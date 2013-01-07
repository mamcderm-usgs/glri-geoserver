package gov.usgs.cida.geoserver.wps;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.CompositeContext;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.UUID;
import org.geoserver.wps.gs.GeoServerProcess;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.jts.Geometries;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

/**
 *
 * @author tkunicki
 */
@DescribeProcess(
        title = "Flowline Raster",
        description = "Rasterize flowline by attribute",
        version = "1.0.0")
public class FlowlineRasterProcess implements GeoServerProcess {

    private static final int COORD_GRID_CHUNK_SIZE = 256;

    @DescribeResult(name = "coverage", description = "coverage")
    public GridCoverage2D execute(
            @DescribeParameter(name = "data", min = 1, max = 1) SimpleFeatureCollection data,
            @DescribeParameter(name = "attribute", min = 1, max = 1) String attribute,
            @DescribeParameter(name = "stroke-width", min = 1, max = 1) Float strokewidth,
            @DescribeParameter(name = "bbox", min = 1, max = 1) ReferencedEnvelope bbox,
            @DescribeParameter(name = "width", min = 1, max = 1) Integer width,
            @DescribeParameter(name = "height", min = 1, max = 1) Integer height) throws Exception {

        return new Process(data, attribute, strokewidth, bbox, width, height).execute();

    }

    private class Process {

        private final SimpleFeatureCollection featureCollection;
        private final String attributeName;
        private final float strokeWidth;
        private final ReferencedEnvelope coverageEnvelope;
        private final int coverageWidth;
        private final int coverageHeight;
        private ReferencedEnvelope extent;
        private GridGeometry2D gridGeometry;
        private MathTransform featureToRasterTransform;
        private int[] coordGridX = new int[COORD_GRID_CHUNK_SIZE];
        private int[] coordGridY = new int[COORD_GRID_CHUNK_SIZE];
        private BufferedImage image;
        private Graphics2D graphics;

        private Process(SimpleFeatureCollection featureCollection,
                String className,
                float strokeWidth,
                ReferencedEnvelope coverageEnvelope,
                int coverageWidth,
                int coverageHeight) {
            this.featureCollection = featureCollection;
            this.attributeName = className;
            this.strokeWidth = strokeWidth;
            this.coverageEnvelope = coverageEnvelope;
            this.coverageWidth = coverageWidth;
            this.coverageHeight = coverageHeight;
        }

        private GridCoverage2D execute() throws Exception {

            initialize();

            SimpleFeatureIterator featureIterator = featureCollection.features();
            try {
                while (featureIterator.hasNext()) {
                    processFeature(featureIterator.next(), attributeName);
                }
            } finally {
                featureIterator.close();
            }

            GridCoverageFactory gcf = new GridCoverageFactory();
            return gcf.create(
                    FlowlineRasterProcess.class.getSimpleName() + "-" + UUID.randomUUID().toString(),
                    image, extent);
        }

        private void initialize() {

            AttributeDescriptor attributeDescriptor = featureCollection.getSchema().getDescriptor(attributeName);
            if (attributeDescriptor == null) {
                throw new RuntimeException(attributeName + " not found");
            }

            Class<?> attClass = attributeDescriptor.getType().getBinding();
            if (!Number.class.isAssignableFrom(attClass)) {
                throw new RuntimeException(attributeName + " is not numeric type");
            }

            if (Float.class.isAssignableFrom(attClass) || Double.class.isAssignableFrom(attClass)) {
                throw new RuntimeException(attributeName + "is not integral type");
            }

            try {
                setBounds(featureCollection, coverageEnvelope);
            } catch (TransformException ex) {
                throw new RuntimeException(ex);
            }

            createImage();

            gridGeometry = new GridGeometry2D(new GridEnvelope2D(0, 0, coverageWidth, coverageHeight), extent);
        }

        private void processFeature(SimpleFeature feature, String attributeName) throws Exception {

            Geometry geometry = (Geometry) feature.getDefaultGeometry();
            Object attributeValue = feature.getAttribute(attributeName);
            if (!(attributeValue instanceof Number)) {
                // TODO: Log!
                return;
            }

            if (extent.intersects(feature.getBounds().toBounds(extent.getCoordinateReferenceSystem()))) {

                graphics.setColor(valueToColor(((Number) attributeValue).intValue()));

                Geometries geomType = Geometries.get(geometry);
                switch (geomType) {
                    case MULTIPOLYGON:
                    case MULTILINESTRING:
                    case MULTIPOINT:
                        final int numGeom = geometry.getNumGeometries();
                        for (int i = 0; i < numGeom; i++) {
                            Geometry geomN = geometry.getGeometryN(i);
                            drawGeometry(Geometries.get(geomN), geomN);
                        }
                        break;

                    case POLYGON:
                    case LINESTRING:
                    case POINT:
                        drawGeometry(geomType, geometry);
                        break;

                    default:
                    // TODO:  Log!
                }
            }
        }

        private void setBounds(SimpleFeatureCollection features, Envelope bounds) throws TransformException {

            ReferencedEnvelope featureBounds = features.getBounds();

            if (bounds == null) {
                extent = featureBounds;
            } else {
                extent = new ReferencedEnvelope(bounds);
            }

            CoordinateReferenceSystem featuresCRS = featureBounds.getCoordinateReferenceSystem();
            CoordinateReferenceSystem boundsCRS = bounds.getCoordinateReferenceSystem();

            if (featuresCRS != null && boundsCRS != null && !CRS.equalsIgnoreMetadata(boundsCRS, featuresCRS)) {
                try {
                    featureToRasterTransform = CRS.findMathTransform(featuresCRS, boundsCRS, true);
                } catch (Exception ex) {
                    throw new TransformException("Unable to transform features into output coordinate reference system", ex);
                }
            }
        }

        private void createImage() {

            if (GraphicsEnvironment.isHeadless()) {
                image = new BufferedImage(coverageWidth, coverageHeight, BufferedImage.TYPE_4BYTE_ABGR);
            } else {
                image = GraphicsEnvironment.
                        getLocalGraphicsEnvironment().
                        getDefaultScreenDevice().
                        getDefaultConfiguration().
                        createCompatibleImage(coverageWidth, coverageHeight, Transparency.TRANSLUCENT);
            }

            image.setAccelerationPriority(1f);
            graphics = image.createGraphics();
            graphics.setStroke(new BasicStroke(strokeWidth));
            graphics.setComposite(new ClassComposite());
        }

        private void drawGeometry(Geometries geomType, Geometry geometry) throws TransformException {
            if (featureToRasterTransform != null) {
                try {
                    geometry = JTS.transform(geometry, featureToRasterTransform);
                } catch (TransformException ex) {
                    throw ex;
                } catch (MismatchedDimensionException ex) {
                    throw new RuntimeException(ex);
                }
            }

            Coordinate[] coords = geometry.getCoordinates();

            // enlarge if needed
            if (coords.length > coordGridX.length) {
                int n = coords.length / COORD_GRID_CHUNK_SIZE + 1;
                coordGridX = new int[n * COORD_GRID_CHUNK_SIZE];
                coordGridY = new int[n * COORD_GRID_CHUNK_SIZE];
            }

            // Go through coordinate array in order received
            DirectPosition2D worldPos = new DirectPosition2D();
            for (int n = 0; n < coords.length; n++) {
                worldPos.setLocation(coords[n].x, coords[n].y);
                GridCoordinates2D gridPos = gridGeometry.worldToGrid(worldPos);
                coordGridX[n] = gridPos.x;
                coordGridY[n] = gridPos.y;
            }

            switch (geomType) {
                case POLYGON:
                    graphics.fillPolygon(coordGridX, coordGridY, coords.length);
                    break;
                case LINESTRING:
                    graphics.drawPolyline(coordGridX, coordGridY, coords.length);
                    break;
                case POINT:
                    graphics.fillRect(coordGridX[0], coordGridY[0], 1, 1);
                    break;
                default:
                // TODO:  Log!
            }
        }

        private Color valueToColor(Number value) {
            int valueAsInt = value.intValue();
            return new Color(
                    (valueAsInt      ) & 0xff,
                    (valueAsInt >> 8 ) & 0xff,
                    (valueAsInt >> 16) & 0xff);
        }
    }

    private static class ClassComposite implements Composite, CompositeContext {

        public ClassComposite() {
        }

        @Override
        public CompositeContext createContext(ColorModel sourceColorModel, ColorModel destinationColorModel, RenderingHints hints) {
            return this;
        }

        @Override
        public void dispose() {
            // nothing to do...
        }

        @Override
        public void compose(Raster sourceRaster, Raster destinationInRaster, WritableRaster destinationOutRaster) {
            int xCount = destinationOutRaster.getWidth();
            int yCount = destinationOutRaster.getHeight();

            int[] sourcePixel = new int[4];
            int[] destinationPixel = new int[4];
            for (int x = 0; xCount > x; x++) {
                for (int y = 0; yCount > y; y++) {
                    sourceRaster.getPixel(x, y, sourcePixel);
                    destinationInRaster.getPixel(x, y, destinationPixel);
                    // TODO:  Only allows classes of up to 1 byte (255)
                    int sC = sourcePixel[0];
                    int dC = destinationPixel[0];
                    int sA = sourcePixel[3];
                    int dA = destinationPixel[3];
                    if (sC > dC || sC == dC && sA > dA ) {
                        destinationOutRaster.setPixel(x, y, sourcePixel);
                    }
                }
            }
        }
    }
}
