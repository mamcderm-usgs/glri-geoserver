package gov.usgs.cida.geotools.datastore;

import java.io.IOException;
import java.util.Date;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.ft.PointFeature;

/**
 *
 * @author tkunicki
 */
public abstract class NetCDFPointFeatureExtractor<T> {

    public abstract T extract(PointFeature pointFeature) throws IOException;
    
    public static class TimeStamp extends NetCDFPointFeatureExtractor<Date> {
        @Override public Date extract(PointFeature pointFeature) throws IOException {
            return pointFeature.getObservationTimeAsCalendarDate().toDate();
        }
    }
    
    public static abstract class Scalar<T> extends NetCDFPointFeatureExtractor<T> {
        protected final String variableName;
        public Scalar(String variableName) {
            this.variableName = variableName;
        }
    }
    
    public static class ScalarBoolean extends Scalar<Boolean> {
        public ScalarBoolean(String variableName) { super(variableName); }
        @Override public Boolean extract(PointFeature pointFeature) throws IOException {
            return null;
        }
    }
    
    public static class ScalarString extends Scalar<String> {
        public ScalarString(String variableName) { super(variableName); }
        @Override public String extract(PointFeature pointFeature) throws IOException {
            return pointFeature.getData().getScalarString(variableName);
        }
    }
    
    public static class ScalarByte extends Scalar<Byte> {
        public ScalarByte(String variableName) { super(variableName); }
        @Override public Byte extract(PointFeature pointFeature) throws IOException {
            return pointFeature.getData().getScalarByte(variableName);
        }
    }
    
    public static class ScalarShort extends Scalar<Short> {
        public ScalarShort(String variableName) { super(variableName); }
        @Override public Short extract(PointFeature pointFeature) throws IOException {
            return pointFeature.getData().getScalarShort(variableName);
        }
    }
    
    public static class ScalarInteger extends Scalar<Integer> {
        public ScalarInteger(String variableName) { super(variableName); }
        @Override public Integer extract(PointFeature pointFeature) throws IOException {
            return pointFeature.getData().getScalarInt(variableName);
        }
    }
    
    public static class ScalarLong extends Scalar<Long> {
        public ScalarLong(String variableName) { super(variableName); }
        @Override public Long extract(PointFeature pointFeature) throws IOException {
            return pointFeature.getData().getScalarLong(variableName);
        }
    }
    
    public static class ScalarFloat extends Scalar<Float> {
        public ScalarFloat(String variableName) { super(variableName); }
        @Override public Float extract(PointFeature pointFeature) throws IOException {
            return pointFeature.getData().getScalarFloat(variableName);
        }
    }
    
    public static class ScalarDouble extends Scalar<Double> {
        public ScalarDouble(String variableName) { super(variableName); }
        @Override public Double extract(PointFeature pointFeature) throws IOException {
            return pointFeature.getData().getScalarDouble(variableName);
        }
    }
        
    public static NetCDFPointFeatureExtractor<?> generatePointFeatureExtractor(VariableSimpleIF variable) {
        String variableName = variable.getShortName();
        switch (variable.getDataType()) {
            case BOOLEAN: return new ScalarBoolean(variableName);
            case BYTE:
            case CHAR: return new ScalarByte(variableName);
            case SHORT: return new ScalarShort(variableName);
            case INT: return new ScalarInteger(variableName);
            case LONG: return new ScalarLong(variableName);
            case FLOAT: return new ScalarFloat(variableName);
            case DOUBLE: return new ScalarDouble(variableName);
            case STRING: return new ScalarString(variableName);
            default: throw new UnsupportedOperationException("extractor not implemented");
        }
    }
}
