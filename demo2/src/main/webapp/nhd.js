
var flowlineDecileStyle = "FlowlineDecileData";
var flowlineDecileLayer = "glri:NHDFlowlineAFINCH";
var geoserverBaseURL = "http://130.11.177.182:8080/geoserver/";

// START - Flowline Raster
var flowlinesDecileData = new OpenLayers.Layer.WMS(
    "Flowline Percentil WMS (Data)",
    geoserverBaseURL + "wms",
    { layers: flowlineDecileLayer, styles: flowlineDecileStyle, format: "image/png", tiled: "true", time: "1993-07-01T00:00:00.000Z" },
    { isBaseLayer: false, opacity: 0, displayInLayerSwitcher: false, tileOptions: { crossOriginKeyword: 'anonymous' } }
);
var flowlineDecileRaster = new OpenLayers.Layer.Raster({
    name: "NHD Flowline Percentiles",
    data: flowlineDecileOperationData,
    isBaseLayer: false
});

// define per-pixel operation
var flowlineDecileColorMapOperation = OpenLayers.Raster.Operation.create(function(pixel) {
    if (pixel >> 24 === 0) {
        return 0;
    }
    var value = pixel & 0x00ffffff;
    if (value < 0x00ffffff) {
		
		if (value < minValue) return 0xffff0000;
		if (value > maxValue) return 0xff0000ff;
		
		var coef = (value - minValue) / (maxValue - minValue) * 4;
		if (invert) coef = 4 - coef;
		var r = Math.min(coef - 1.5, -coef + 4.5);
		if (r < 0) r = 0; else if (r > 1) r = 1;
		
		var g = Math.min(coef - 0.5, -coef + 3.5);
		if (g < 0) g = 0; else if (g > 1) g = 1;
		
		var b = Math.min(coef + 0.5, -coef + 2.5);
		if (b < 0) b = 0; else if (b > 1) b = 1;
		
		return 0xff000000 | (r * 0xff) << 16 | (g * 0xff) << 8  | (b * 0xff);
    } else {
        return 0;
    }
});

// source canvas (writes WMS tiles to canvas for reading
var flowlineDecileComposite = OpenLayers.Raster.Composite.fromLayer(flowlinesDecileData, {int32: true});
// filter source data through per-pixel operation 
var flowlineDecileOperationData = flowlineDecileColorMapOperation(flowlineDecileComposite);
// define layer that writes data to a new cnavas
flowlineDecileRaster.setData(flowlineDecileOperationData);
// END - Flowline Raster

var mapProj = new OpenLayers.Projection("EPSG:900913");
var wgs84Proj = new OpenLayers.Projection("EPSG:4326");

var mapExtent = new OpenLayers.Bounds(-93.18993823245728, 40.398554803028716, -73.65211352945056, 48.11264392438207).transform(wgs84Proj, mapProj);
var mapCenterStart = mapExtent.getCenterLonLat();
var mapOptions = {
    div: "map",
    projection: mapProj,
//    units: "m",
    restrictedExtent: mapExtent,
    layers: [
        new OpenLayers.Layer.XYZ(
            "World Imagery",
            "http://services.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/${z}/${y}/${x}",
            { isBaseLayer: true, units: "m" } ),
        new OpenLayers.Layer.XYZ(
            "World Light Gray Base",
            "http://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Light_Gray_Base/MapServer/tile/${z}/${y}/${x}",
            { isBaseLayer: true,  units: "m" } ),
        new OpenLayers.Layer.XYZ(
            "World Topo Map",
            "http://services.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer/tile/${z}/${y}/${x}",
            { isBaseLayer: true, units: "m" } ),
        new OpenLayers.Layer.XYZ(
            "World Street Map",
            "http://services.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/${z}/${y}/${x}",
            { isBaseLayer: true, units: "m" } ),
        new OpenLayers.Layer.XYZ(
            "World Terrain Base",
            "http://server.arcgisonline.com/ArcGIS/rest/services/World_Shaded_Relief/MapServer/tile/${z}/${y}/${x}",
            { isBaseLayer: true, units: "m" } ),
        flowlinesDecileData,
        flowlineDecileRaster,
    ],
    controls: [
        new OpenLayers.Control.Navigation({ dragPanOptions: { enableKinetic: true } } ),
        new OpenLayers.Control.Zoom(),
        new OpenLayers.Control.LayerSwitcher(),
        new OpenLayers.Control.Scale(),
        new OpenLayers.Control.MousePosition()
    ]
};
var map = new OpenLayers.Map(mapOptions);

// START - TODO RECALC ON RESIZE
var mapZoomForExtent = map.getZoomForExtent(mapExtent);
map.isValidZoomLevel = function(zoomLevel) {
  return zoomLevel && zoomLevel >= mapZoomForExtent && zoomLevel < this.getNumZoomLevels();  
};
// END - TODO RECALC ON RESIZE

map.setCenter(mapCenterStart, mapZoomForExtent);


var minSlider;
var maxSlider;
var minValue = 0;
var maxValue = 100;
var invert = true;

var updateFromMinOrMaxValue = function() {
    if (flowlineDecileRaster.getVisibility()) {
        flowlineDecileRaster.onDataUpdate();
    }
};

var flowlineRasterWindow;

Ext.onReady(function() {
    
    updateFromFlowlineRasterVisibility = function() {
        if (flowlineRasterWindow) {
            flowlineRasterWindow.setVisible(flowlineDecileRaster.getVisibility());
        }
    };
    flowlineDecileRaster.events.on({
        visibilitychanged: function(event) {
            updateFromFlowlineRasterVisibility();
        }
    });
    
  
    minSlider = new Ext.slider.SingleSlider({
        fieldLabel: "Min Value",
        width: 255,
        value: minValue,
        increment: 1,
        minValue: 0,
        maxValue: 100,
        listeners: {
            change: function(element, newValue) {
                if (newValue !== minValue) {
                    minValue = newValue;
                    //setClipValue(newValue);
                    updateFromMinOrMaxValue();
                }
            }
        }
    });
	maxSlider = new Ext.slider.SingleSlider({
        fieldLabel: "Max Value",
        width: 255,
        value: maxValue,
        increment: 1,
        minValue: 0,
        maxValue: 100,
        listeners: {
            change: function(element, newValue) {
                if (newValue !== maxValue) {
                    maxValue = newValue;
                    //setClipValue(newValue);
                    updateFromMinOrMaxValue();
                }
            }
        }
    });
	       
    flowlineRasterWindow = new Ext.Window({
        title: "Flowline Properties",
        renderTo: Ext.getBody(),
        autoHeight: true,
        autoWidth: true,
        x: 10,
        y: 10,
        closable: false,
        collapsible: true,
        titleCollapse: true,
        border: false,
        layout: 'form',
        labelAlign: 'right',
        items:[
            minSlider,
            maxSlider
        ]
    }).show();

    updateFromFlowlineRasterVisibility();
});
