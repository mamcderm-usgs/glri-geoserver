
var flowlineStyle = "FlowlineStreamOrder";
var flowlineLayer = "glri:NHDFlowline";
var gageStyle = "GageLocStreamOrder";
var gageLayer = "glri:GageLoc";
//var geoserverBaseURL = "http://localhost:18080/glri-geoserver/";
var geoserverBaseURL = "http://localhost:8080/geoserver/";
//var geoserverBaseURL = "http://cida-wiwsc-gdp2qa.er.usgs.gov:8084/lkm-geoserver/";
//var geoserverBaseURL = "http://internal.cida.usgs.gov/lkm-geoserver/";

var streamOrderClipValues = [
    7, // 0
    7,
    7,
    6,
    6,
    6, // 5
    5,
    5,
    5,
    4,
    4, // 10
    4,
    3,
    3,
    3,
    2, // 15
    2,
    2,
    1,
    1,
    1  // 20
];

var streamOrderClipValue = 0;

// START - Gage Feature
var gageDefaultStyle = new OpenLayers.Style({
  'pointRadius': 6,
  'fillColor' : '#ee9900',
  'fillOpacity' : 0.4,
  'strokeColor' : '#ee9900',
  'strokeOpacity' : 1,
  'strokeWidth' : 1
});

var gageSelectedStyle = new OpenLayers.Style({
  'pointRadius': 6,
  'fillColor' : '#ee9900',
  'fillOpacity' : 0.4,
  'strokeColor' : '#ffffff',
  'strokeOpacity' : 1,
  'strokeWidth' : 1
});
var gageStyleMap = new OpenLayers.StyleMap({
    'default': gageDefaultStyle,
    'select': gageSelectedStyle});
var gageStreamOrderFilter = new OpenLayers.Filter.Comparison({
    type: OpenLayers.Filter.Comparison.GREATER_THAN_OR_EQUAL_TO,
    property: "StreamOrde",
    value: streamOrderClipValue
});
var gageStreamOrderFilterStrategy = new OpenLayers.Strategy.Filter({filter: gageStreamOrderFilter});
var gageFeature = new OpenLayers.Layer.Vector("GageLoc (WFS)", {
    minScale: 15000000,
    strategies: [new OpenLayers.Strategy.BBOX(), gageStreamOrderFilterStrategy],
    protocol: new OpenLayers.Protocol.WFS({
        url: geoserverBaseURL + "wfs",
        featureType: "GageLoc",
        featureNS: "http://cida.usgs.gov/glri"
    }),
    styleMap: gageStyleMap,
    renderers: ['DeclusterCanvas']
});
var updateGageStreamOrderFilter = function() {
    gageStreamOrderFilter.value = streamOrderClipValue;
    gageStreamOrderFilterStrategy.setFilter(gageStreamOrderFilter);
};
// End Gage Feature

// START - Flowline Raster
var flowlinesWMSData = new OpenLayers.Layer.WMS(
    "Flowline WMS (Data)",
    geoserverBaseURL + "wms",
    { layers: flowlineLayer, styles: flowlineStyle, format: "image/png", tiled: "true" },
    { isBaseLayer: false, opacity: 0, displayInLayerSwitcher: false, tileOptions: { crossOriginKeyword: 'anonymous' } }
);
var flowlineRaster = new OpenLayers.Layer.Raster({
    name: "NHD Flowlines",
    data: flowlineClipOperationData,
    isBaseLayer: false
});

var flowlineAboveClipPixelR = 255;
var flowlineAboveClipPixelG = 255;
var flowlineAboveClipPixelB = 255;
var flowlineAboveClipPixelA = 128;
var flowlineAboveClipPixel;
var createFlowlineAboveClipPixel = function() {
    flowlineAboveClipPixel =
        (flowlineAboveClipPixelA & 0xff) << 24 |
        (flowlineAboveClipPixelB & 0xff) << 16 |
        (flowlineAboveClipPixelG & 0xff) << 8  |
        (flowlineAboveClipPixelR & 0xff);
};
createFlowlineAboveClipPixel();

// define per-pixel operation
var flowlineClipOperation = OpenLayers.Raster.Operation.create(function(pixel) {
    if (pixel >> 24 === 0) {
        return 0;
    }
    var value = pixel & 0x00ffffff;
    if (value >= streamOrderClipValue && value < 0x00ffffff) {
        return flowlineAboveClipPixel;
    } else {
        return 0;
    }
});

// source canvas (writes WMS tiles to canvas for reading
var flowlineComposite = OpenLayers.Raster.Composite.fromLayer(flowlinesWMSData, {int32: true});
// filter source data through per-pixel operation 
var flowlineClipOperationData = flowlineClipOperation(flowlineComposite);
// define layer that writes data to a new cnavas
flowlineRaster.setData(flowlineClipOperationData);
// END - Flowline Raster

// START - Gage Raster
var gageWMSData = new OpenLayers.Layer.WMS(
    "Gage WMS (Data)",
    geoserverBaseURL + "wms",
    { layers: gageLayer, styles: gageStyle, format: "image/png", tiled: "true" },
    { isBaseLayer: false, opacity: 0, displayInLayerSwitcher: false, tileOptions: { crossOriginKeyword: 'anonymous' } }
);
var gageRaster = new OpenLayers.Layer.Raster({
    name: "Gage Location",
    isBaseLayer: false,
    readOnly: true,
    visibility: false
});

var gageStyleR = 0;
var gageStyleG = 255;
var gageStyleB = 0;
var gageStyleA = 255;
var gageRadius = 4;
var gageFill = false;
var gageStyle;

var createGageStyle = function() {
    gageStyle =
        "rgba(" + 
        gageStyleR + "," +
        gageStyleG + "," +
        gageStyleB + "," +
        gageStyleA / 255 + ")";
        
};
createGageStyle();

var gageClipOperation = OpenLayers.Raster.Operation.create(function(pixel, x, y) {
    var value = pixel & 0x00ffffff;
    if (value >= streamOrderClipValue && value < 0x00ffffff) {
        gageRaster.context.beginPath();
        gageRaster.context.fillStyle = gageStyle;
        gageRaster.context.strokeStyle = gageStyle;
        gageRaster.context.arc(x,y,gageRadius,0,2*Math.PI);
        if (gageFill) {
            gageRaster.context.fill();
        } else {
            gageRaster.context.stroke();
        }
    }
});
var gageComposite = OpenLayers.Raster.Composite.fromLayer(gageWMSData, {int32: true});
var gageClipOperationData = gageClipOperation(gageComposite);
gageRaster.setData(gageClipOperationData);
// END - Gage Raster

var mapExtent = new OpenLayers.Bounds(-93.18993823245728, 40.398554803028716, -73.65211352945056, 48.11264392438207);
var mapCenterStart = mapExtent.getCenterLonLat();
var mapOptions = {
    div: "map",
    projection: "EPSG:4326",
    restrictedExtent: mapExtent,
    layers: [
        new OpenLayers.Layer.WMS(
            "Blue Marble",
            "http://maps.opengeo.org/geowebcache/service/wms",
            { layers: "bluemarble" }, { isBaseLayer: true }),
        flowlinesWMSData,
        gageWMSData,
        flowlineRaster,
        gageRaster,
        gageFeature
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


var streamOrderSlider;
var zoomLevelTextField;
var streamOrderLock = true;

var getClipValueForZoom = function(zoom) {
    return streamOrderTable[zoom].getValue();
};
var getClipValue = function() {
    return getClipValueForZoom(map.zoom);
};
var setClipValueForZoom = function(zoom, value) {
    if (streamOrderLock === true) {
       for (var zoomIndex = 0; zoomIndex < streamOrderTable.length; ++zoomIndex) {
           if (zoomIndex < zoom) {
               if (streamOrderTable[zoomIndex].getValue() < value) {
                   streamOrderTable[zoomIndex].setValue(value);
               }
           } else if (zoomIndex > zoom) {
               if (streamOrderTable[zoomIndex].getValue() > value) {
                   streamOrderTable[zoomIndex].setValue(value);
               }
           } else {
               streamOrderTable[zoomIndex].setValue(value);
           }
       }
    } else {
        streamOrderTable[zoom].setValue(value);
    }
};
var setClipValue = function(value) {
    setClipValueForZoom(map.zoom, value);
};

var updateFromClipValue = function() {
    if (flowlineRaster.getVisibility()) {
        flowlineRaster.onDataUpdate();
    }
    if (gageRaster.getVisibility()) {
        gageRaster.onDataUpdate();
    }
    if (gageFeature.getVisibility()) {
        updateGageStreamOrderFilter();
    }
};

streamOrderTable = new Array(21);
for (var zoomIndex = 0; zoomIndex < streamOrderTable.length; ++zoomIndex) {
    streamOrderTable[zoomIndex] = new Ext.slider.SingleSlider({
        fieldLabel: '' + zoomIndex,
        value: streamOrderClipValues[zoomIndex],
        minValue: 1,
        maxValue: 7,
        zoom: zoomIndex,
        listeners: {
            change: function(element, newValue) {
                setClipValueForZoom(element.zoom, newValue);
                if (element.zoom === map.zoom) {
                    streamOrderSlider.setValue(streamOrderClipValue);
                    if (newValue !== streamOrderClipValue) {
                        streamOrderClipValue = newValue;
                        updateFromClipValue();
                    }
                }
            }
        }
    });
                
}

var flowlineRasterWindow;
var gageRasterWindow;



Ext.onReady(function() {
    
    streamOrderClipValue = streamOrderClipValues[map.zoom];
    
    // openlayers controls
    var select = new OpenLayers.Control.SelectFeature(
            gageFeature,
            { hover: false } );
    map.addControl(select);
    select.activate();
    
    // openlayers events
    gageFeature.events.on({
        featureselected: function(event) {
            var feature = event.feature;
            var id = feature.attributes.key;
            console.log("selected " + id);
        }
    });
    
    updateFromFlowlineRasterVisibility = function() {
        if (flowlineRasterWindow) {
            flowlineRasterWindow.setVisible(flowlineRaster.getVisibility());
        }
    };
    flowlineRaster.events.on({
        visibilitychanged: function(event) {
            updateFromFlowlineRasterVisibility();
        }
    });

    updateFromGageRasterVisibility = function() {
        if (gageRasterWindow) {
            gageRasterWindow.setVisible(gageRaster.getVisibility());
        }
    };
    gageRaster.events.on({
        visibilitychanged: function(event) {
            updateFromGageRasterVisibility();
        }
    });
    
    
    Ext.override(Ext.dd.DragTracker, {
        onMouseMove: function (e, target) {
            var isIE9 = Ext.isIE && (/msie 9/.test(navigator.userAgent.toLowerCase())) && document.documentMode != 6;
            if (this.active && Ext.isIE && !isIE9 && !e.browserEvent.button) {
                e.preventDefault();
                this.onMouseUp(e);
                return;
            }
            e.preventDefault();
            var xy = e.getXY(), s = this.startXY;
            this.lastXY = xy;
            if (!this.active) {
                if (Math.abs(s[0] - xy[0]) > this.tolerance || Math.abs(s[1] - xy[1]) > this.tolerance) {
                    this.triggerStart(e);
                } else {
                    return;
                }
            }
            this.fireEvent('mousemove', this, e);
            this.onDrag(e);
            this.fireEvent('drag', this, e);
        }
    });
    
    streamOrderSlider = new Ext.slider.SingleSlider({
        fieldLabel: "Stream Order",
        width: 120,
        value: streamOrderClipValue,
        increment: 1,
        minValue: 1,
        maxValue: 7,
        listeners: {
            change: function(element, newValue) {
                if (newValue !== streamOrderClipValue) {
                    streamOrderClipValue = newValue;
                    //setClipValue(newValue);
                    updateFromClipValue();
                }
            }
        }
    });
    zoomLevelTextField = new Ext.form.TextField({
        fieldLabel: 'Zoom Level',
        value: map.zoom,
        editable: false
    });
    map.events.register(
        'zoomend',
        map, 
        function() {
            var zoom = map.zoom;
            zoomLevelTextField.setValue(zoom);
            streamOrderClipValue = getClipValueForZoom(zoom);
            streamOrderSlider.setValue(streamOrderClipValue);
            updateGageStreamOrderFilter();
        },
        true);
    
    new Ext.Window({
        title: "Stream Order",
        renderTo: Ext.getBody(),
        autoHeight: true,
        autoWidth: true,
        x: 50,
        y: 10,
        closable: false,
        collapsible: true,
        titleCollapse: true,
        border: false,
        layout: 'form',
        labelAlign: 'right',
        items:[
            streamOrderSlider,
            zoomLevelTextField,
            {
                xtype: 'panel',
                layout: 'form',
                items: streamOrderTable
            }
        ]
    }).show();
        
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
            new Ext.slider.SingleSlider({
                fieldLabel: "Red Channel",
                width: 255,
                value: flowlineAboveClipPixelR,
                increment: 1,
                minValue: 0,
                maxValue: 255,
                listeners: {
                    change: function(element, newValue) {
                        flowlineAboveClipPixelR = newValue;
                        createFlowlineAboveClipPixel();
                        flowlineRaster.onDataUpdate();
                    }
                }
            }),
            new Ext.slider.SingleSlider({
                fieldLabel: "Green Channel",
                width: 255,
                value: flowlineAboveClipPixelG,
                increment: 1,
                minValue: 0,
                maxValue: 255,
                listeners: {
                    change: function(element, newValue) {
                        flowlineAboveClipPixelG = newValue;
                        createFlowlineAboveClipPixel();
                        flowlineRaster.onDataUpdate();
                    }
                }
            }),
            new Ext.slider.SingleSlider({
                fieldLabel: "Blue Channel",
                width: 255,
                value: flowlineAboveClipPixelB,
                increment: 1,
                minValue: 0,
                maxValue: 255,
                listeners: {
                    change: function(element, newValue) {
                        flowlineAboveClipPixelB = newValue;
                        createFlowlineAboveClipPixel();
                        flowlineRaster.onDataUpdate();
                    }
                }
            }),
            new Ext.slider.SingleSlider({
                fieldLabel: "Alpha Channel",
                width: 255,
                value: flowlineAboveClipPixelA,
                increment: 1,
                minValue: 0,
                maxValue: 255,
                listeners: {
                    change: function(element, newValue) {
                        flowlineAboveClipPixelA = newValue;
                        createFlowlineAboveClipPixel();
                        flowlineRaster.onDataUpdate();
                    }
                }
            })
        ]
    }).show();
    
    gageRasterWindow = new Ext.Window({
        title: "Gage Properties",
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
            new Ext.slider.SingleSlider({
                fieldLabel: "Red Channel",
                width: 255,
                value: gageStyleR,
                increment: 1,
                minValue: 0,
                maxValue: 255,
                listeners: {
                    change: function(element, newValue) {
                        gageStyleR = newValue;
                        createGageStyle();
                        gageRaster.onDataUpdate();
                    }
                }
            }),
            new Ext.slider.SingleSlider({
                fieldLabel: "Green Channel",
                width: 255,
                value: gageStyleG,
                increment: 1,
                minValue: 0,
                maxValue: 255,
                listeners: {
                    change: function(element, newValue) {
                        gageStyleG = newValue;
                        createGageStyle();
                        gageRaster.onDataUpdate();
                    }
                }
            }),
            new Ext.slider.SingleSlider({
                fieldLabel: "Blue Channel",
                width: 255,
                value: gageStyleB,
                increment: 1,
                minValue: 0,
                maxValue: 255,
                listeners: {
                    change: function(element, newValue) {
                        gageStyleB = newValue;
                        createGageStyle();
                        gageRaster.onDataUpdate();
                    }
                }
            }),
            new Ext.slider.SingleSlider({
                fieldLabel: "Alpha Channel",
                width: 255,
                value: gageStyleA,
                increment: 1,
                minValue: 0,
                maxValue: 255,
                listeners: {
                    change: function(element, newValue) {
                        gageStyleA = newValue;
                        createGageStyle();
                        gageRaster.onDataUpdate();
                    }
                }
            }),
            new Ext.slider.SingleSlider({
                fieldLabel: "Radius (px)",
                width: 255,
                value: gageRadius,
                increment: 1,
                minValue: 0,
                maxValue: 10,
                listeners: {
                    change: function(element, newValue) {
                        gageRadius = newValue;
                        createGageStyle();
                        gageRaster.onDataUpdate();
                    }
                }
            }),
            new Ext.form.Checkbox({
                fieldLabel: "Fill",
                checked: gageFill,
                listeners: {
                    check: function(element, newValue) {
                        gageFill = newValue;
                        createGageStyle();
                        gageRaster.onDataUpdate();
                    }
                }
            })
        ]
    }).show();

    updateFromFlowlineRasterVisibility();
    updateFromGageRasterVisibility();
    updateGageStreamOrderFilter();
});
