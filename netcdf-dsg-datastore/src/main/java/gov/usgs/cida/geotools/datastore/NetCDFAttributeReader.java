package gov.usgs.cida.geotools.datastore;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import org.geotools.data.AttributeReader;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.PointFeatureCollection;
import ucar.nc2.ft.StationTimeSeriesFeatureCollection;
import ucar.nc2.time.CalendarDateRange;

/**
 *
 * @author tkunicki
 */
public class NetCDFAttributeReader implements AttributeReader {
    
    private final SimpleFeatureType featureType;
    private final int attributeCount;
    
    private final PointFeatureCollection pointFeatureCollection;
    private final NetCDFPointFeatureExtractor<?>[] pointFeatureExtractors;
    
    private PointFeature pointFeature;

    NetCDFAttributeReader(FeatureDataset featureDataset, SimpleFeatureType featureType) throws IOException {
        this.featureType = featureType;
        this.attributeCount = featureType.getAttributeCount();
        
        StationTimeSeriesFeatureCollection stationTimeSeriesFeatureCollection = NetCDFUtil.extractStationTimeSeriesFeatureCollection(featureDataset);
        
        pointFeatureCollection = stationTimeSeriesFeatureCollection != null ?
                stationTimeSeriesFeatureCollection.flatten(null, (CalendarDateRange)null) :
                null;
                
        pointFeatureExtractors = new NetCDFPointFeatureExtractor<?>[attributeCount];
        
        List<AttributeDescriptor> desctiptors = featureType.getAttributeDescriptors();
        for (int attributeIndex = 0; attributeIndex < attributeCount; ++attributeIndex) {
            Object o = desctiptors.get(attributeIndex).getUserData().get(NetCDFShapefileDataStore.EXTRACTOR_KEY);
            pointFeatureExtractors[attributeIndex] = o instanceof NetCDFPointFeatureExtractor<?> ?
                    (NetCDFPointFeatureExtractor<?>)o :
                    null;
        }
    }

    @Override
    public int getAttributeCount() {
        return featureType.getAttributeCount();
    }

    @Override
    public AttributeDescriptor getAttributeType(int index) throws ArrayIndexOutOfBoundsException {
        return featureType.getDescriptor(index);
    }

    @Override
    public void close() throws IOException {
        pointFeatureCollection.finish();
    }

    @Override
    public boolean hasNext() throws IOException {
        return pointFeatureCollection.hasNext();
    }

    @Override
    public void next() throws IOException, IllegalArgumentException, NoSuchElementException {
        pointFeature = pointFeatureCollection.next();
        if (pointFeature == null) {
            throw new NoSuchElementException();
        }
    }

    @Override
    public Object read(int index) throws IOException, ArrayIndexOutOfBoundsException {
        if (index < attributeCount) {
            return pointFeatureExtractors[index].extract(pointFeature);
        } else {
            throw new ArrayIndexOutOfBoundsException(index);
        }
    }
}
