package gov.usgs.cida.geotools.datastore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.ft.FeatureCollection;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.StationTimeSeriesFeatureCollection;

/**
 *
 * @author tkunicki
 */
public class NetCDFUtil {
    
    public static StationTimeSeriesFeatureCollection extractStationTimeSeriesFeatureCollection(FeatureDataset featureDataset) throws IOException {
        if (featureDataset instanceof FeatureDatasetPoint) {
            FeatureDatasetPoint featureDatasetPoint = (FeatureDatasetPoint)featureDataset;
            List<FeatureCollection> featureCollectionList = featureDatasetPoint.getPointFeatureCollectionList();
            if (featureCollectionList != null && featureCollectionList.size() > 0) {
                if (featureCollectionList.size() == 1) {
                    FeatureCollection featureCollection = featureCollectionList.get(0);
                    if (featureCollection instanceof StationTimeSeriesFeatureCollection) {
                        return (StationTimeSeriesFeatureCollection)featureCollection;
                    }
                } else {
                    // multiple collections???
                }
            } else {
                 // error, no data
            }
        } else {
            // error, no data or wrong data type
        }
        return null;
    }
    
    public static List<VariableSimpleIF> getObservationVariables(FeatureDataset dataset) throws IOException {

        List<VariableSimpleIF> variableList = null;

        switch (dataset.getFeatureType()) {
            case POINT:
            case PROFILE:
            case SECTION:
            case STATION:
            case STATION_PROFILE:
            case STATION_RADIAL:
            case TRAJECTORY:

                variableList = new ArrayList<VariableSimpleIF>();

                // Try Unidata Observation Dataset convention where observation
                // dimension is declared as global attribute...
                Attribute convAtt = dataset.getNetcdfFile().findGlobalAttributeIgnoreCase("Conventions");
                if (convAtt != null && convAtt.isString()) {
                    String convName = convAtt.getStringValue();

                    //// Unidata Observation Dataset Convention
                    //   http://www.unidata.ucar.edu/software/netcdf-java/formats/UnidataObsConvention.html
                    if (convName.contains("Unidata Observation Dataset")) {
                        Attribute obsDimAtt = dataset.findGlobalAttributeIgnoreCase("observationDimension");
                        String obsDimName = (obsDimAtt != null && obsDimAtt.isString())
                                ? obsDimAtt.getStringValue() : null;
                        if (obsDimName != null && obsDimName.length() > 0) {
                            String psuedoRecordPrefix = obsDimName + '.';
                            for (VariableSimpleIF var : dataset.getNetcdfFile().getVariables()) {
                                if (var.findAttributeIgnoreCase("_CoordinateAxisType") == null) {
                                    if (var.getName().startsWith(psuedoRecordPrefix)) {
                                        // doesn't appear to be documented, this
                                        // is observed behavior...
                                        variableList.add(var);
                                    } else {
                                        for (Dimension dim : var.getDimensions()) {
                                            if (obsDimName.equalsIgnoreCase(dim.getName())) {
                                                variableList.add(var);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (variableList.isEmpty()) {
                            // no explicit observation dimension found? look for
                            // variables with unlimited dimension
                            for (VariableSimpleIF var : dataset.getNetcdfFile().getVariables()) {
                                for (Dimension dim : var.getDimensions()) {
                                    if (dim.isUnlimited()) {
                                        variableList.add(var);
                                    }
                                }
                            }
                        }
                    }
                }

                //// CF Conventions
                //   https://cf-pcmdi.llnl.gov/trac/wiki/PointObservationConventions
                //
                //  Don't try explicit :Conventions attribute check since this
                //  doesnt seem to be coming through TDS with cdmremote when
                //  CF conventions are used (?!)
                if (variableList.isEmpty()) {
                    // Try CF convention where range variable has coordinate attribute
                    for (Variable variable : dataset.getNetcdfFile().getVariables()) {
                        if (variable.findAttributeIgnoreCase("coordinates") != null) {
                            if (variable instanceof Structure) {
                                for (Variable structureVariable : ((Structure)variable).getVariables()) {
                                    if ( !(structureVariable instanceof CoordinateAxis) &&
                                         structureVariable.findAttributeIgnoreCase(CF.RAGGED_PARENTINDEX) == null) {
                                        variableList.add(structureVariable);
                                    }   
                                }
                            } else {
                                variableList.add(variable);
                            }
                        }
                    }
                }
                break;
            default:
                for (VariableSimpleIF var : dataset.getDataVariables()) {
                    variableList.add(var);
                }
                break;
        }

        if (variableList == null) {
            variableList = Collections.emptyList();
        }
        return variableList;
    }
    
    public static VariableSimpleIF getObservationTimeVariable(FeatureDataset featureDataset) {
        return getObservationTimeVariable(featureDataset.getDataVariables());
    }
    
    public static VariableSimpleIF getObservationTimeVariable(List<? extends VariableSimpleIF> variableList) {
        for (VariableSimpleIF variable : variableList) {
            if (isObservationTimeVariable(variable)) {
                return variable;
            }
        }
        return null;
    }
    
    public static VariableSimpleIF getStationIdVariable(FeatureDataset featureDataset) {
        return getStationIdVariable(featureDataset.getNetcdfFile().getVariables());
    }
    
    public static VariableSimpleIF getStationIdVariable(List<? extends VariableSimpleIF> variableList) {
        for (VariableSimpleIF variable : variableList) {
            if (isStationIdVariable(variable)) {
                return variable;
            }
        }
        return null;
    }
    
    public static boolean isObservationTimeVariable(VariableSimpleIF variable) {
        Attribute attribute = variable.findAttributeIgnoreCase("_CoordinateAxisType");
        return attribute != null && attribute.isString() && "Time".equals(attribute.getStringValue());
    }
    
    public static boolean isStationIdVariable(VariableSimpleIF variable) {
        Attribute standardNameAttribute = variable.findAttributeIgnoreCase("standard_name");
        Attribute cfRoleAttribute = variable.findAttributeIgnoreCase("cf_role");
        return (standardNameAttribute != null && standardNameAttribute.isString() && "station_id".equals(standardNameAttribute.getStringValue())) || 
               (cfRoleAttribute != null && cfRoleAttribute.isString() && "timeseries_id".equals(cfRoleAttribute.getStringValue()));
    }
}
