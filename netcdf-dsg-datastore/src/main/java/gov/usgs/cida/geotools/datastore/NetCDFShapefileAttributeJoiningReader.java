package gov.usgs.cida.geotools.datastore;

import com.vividsolutions.jts.geom.Envelope;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.geotools.data.shapefile.ShapefileAttributeReader;
import org.geotools.renderer.ScreenMap;
import org.opengis.feature.type.AttributeDescriptor;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ft.StationTimeSeriesFeatureCollection;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.Station;

/**
 *
 * @author tkunicki
 */
public class NetCDFShapefileAttributeJoiningReader  extends ShapefileAttributeReader {
    
    private final ShapefileAttributeReader delegate;
    private final int shapefileJoinAttributeIndex;
    
    private final FeatureDataset featureDataset;
    private final Date timeStep;
    private final StationTimeSeriesFeatureCollection stationTimeSeriesFeatureCollection;
  
    private final NetCDFPointFeatureExtractor<?>[] pointFeatureExtractors;
    
    private PointFeature pointFeature;
    
    public NetCDFShapefileAttributeJoiningReader(ShapefileAttributeReader delegate, FeatureDataset featureDataset, int shapefileJoinAttributeIndex) throws IOException {
        this(delegate, featureDataset, shapefileJoinAttributeIndex, null);
    }

    public NetCDFShapefileAttributeJoiningReader(ShapefileAttributeReader delegate, FeatureDataset featureDataset, int shapefileJoinAttributeIndex, Date timeStep) throws IOException {
        super(hack(delegate), null, null); // lame duck
        this.delegate = delegate;
        this.featureDataset = featureDataset;
        this.shapefileJoinAttributeIndex = shapefileJoinAttributeIndex;
        this.timeStep = timeStep;
        
        this.stationTimeSeriesFeatureCollection = NetCDFUtil.extractStationTimeSeriesFeatureCollection(featureDataset);
        stationTimeSeriesFeatureCollection.resetIteration();
        
        int attributeCount = getAttributeCount();
        pointFeatureExtractors = new NetCDFPointFeatureExtractor<?>[attributeCount];
        for (int attributeIndex = 0; attributeIndex < attributeCount; ++attributeIndex) {
            Object o = getAttributeType(attributeIndex).getUserData().get(NetCDFShapefileDataStore.EXTRACTOR_KEY);
            pointFeatureExtractors[attributeIndex] = o instanceof NetCDFPointFeatureExtractor<?> ?
                    (NetCDFPointFeatureExtractor<?>)o :
                    null;
        }
    }

    @Override
    public void close() throws IOException {
        try {
            delegate.close();
        } catch (IOException e) {
            /* don't care */
        }
        stationTimeSeriesFeatureCollection.finish();
    }

    @Override
    public int getRecordNumber() {
        return delegate.getRecordNumber();
    }

    @Override
    public boolean hasNext() throws IOException {
        return delegate.hasNext();
    }

    @Override
    public void next() throws IOException {
        delegate.next();
        String stationId = delegate.read(shapefileJoinAttributeIndex).toString();
        Station station = stationTimeSeriesFeatureCollection.getStation(stationId);
        StationTimeSeriesFeature stationTimeSeriesFeature = stationTimeSeriesFeatureCollection.getStationFeature(station);
        if (timeStep != null) {
            stationTimeSeriesFeature = stationTimeSeriesFeature.subset(new CalendarDateRange(CalendarDate.of(timeStep), 0));
        }
        if (stationTimeSeriesFeature.hasNext()) {
            pointFeature = stationTimeSeriesFeature.next();
        }
        stationTimeSeriesFeature.finish();
    }

    @Override
    public Object read(int attributeIndex) throws IOException, ArrayIndexOutOfBoundsException {
        NetCDFPointFeatureExtractor extractor = pointFeatureExtractors[attributeIndex];
        return extractor == null ?
                delegate.read(attributeIndex) :
                extractor.extract(pointFeature);
    }

    @Override
    public void setScreenMap(ScreenMap screenMap) {
        delegate.setScreenMap(screenMap);
    }

    @Override
    public void setSimplificationDistance(double distance) {
        delegate.setSimplificationDistance(distance);
    }

    @Override
    public void setTargetBBox(Envelope envelope) {
        delegate.setTargetBBox(envelope);
    }

    private static List<AttributeDescriptor> hack(ShapefileAttributeReader delegate) {
        int attributeCount = delegate.getAttributeCount();
        List<AttributeDescriptor> descriptors = new ArrayList<AttributeDescriptor>(delegate.getAttributeCount());
        for (int attributeIndex = 0; attributeIndex < attributeCount; ++attributeIndex) {
            descriptors.add(delegate.getAttributeType(attributeIndex));
        }
        return descriptors;
    }
    
}
