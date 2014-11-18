package gov.usgs.cida.geotools.datastore;

import com.vividsolutions.jts.geom.Envelope;
import static gov.usgs.cida.geotools.datastore.PRMSAnimationShapefileDataStore.NHRU_FILE_ATTRIB_COLUMN;
import gov.usgs.cida.prms.PRMSAnimationFileMetaData;
import gov.usgs.cida.prms.PRMSAnimationRecord;
import gov.usgs.cida.prms.PRMSAnimationRecordBuffer;
import gov.usgs.cida.prms.RecordEntryDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.data.shapefile.ShapefileAttributeReader;
import org.geotools.renderer.ScreenMap;
import org.opengis.feature.type.AttributeDescriptor;

/**
 *
 * @author tkunicki
 */
public class PRMSAnimationShapefileAttributeJoiningReader extends ShapefileAttributeReader {
    
	protected static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.geotools.data");
	
    private final ShapefileAttributeReader delegate;
    private final int shapefileJoinAttributeIndex;
    private final int animationJoinValueOffset;
    private final int animationTimeStepRecordOffset;
    
    private PRMSAnimationRecordBuffer animationRecordBuffer;
    private PRMSAnimationRecord animationRecord;
	final private RecordEntryDescriptor[] recordEntryDescriptors;

    public PRMSAnimationShapefileAttributeJoiningReader(ShapefileAttributeReader delegate, PRMSAnimationFileMetaData animationFileMetaData, int shapefileJoinAttributeIndex, int animationJoinValueOffset) throws IOException {
        this(delegate, animationFileMetaData, shapefileJoinAttributeIndex, animationJoinValueOffset, 0);
    }

    public PRMSAnimationShapefileAttributeJoiningReader(ShapefileAttributeReader delegate, PRMSAnimationFileMetaData animationFileMetaData, int shapefileJoinAttributeIndex, int animationJoinValueOffset, int timeStepIndex) throws IOException {
        super(hack(delegate), null, null); // lame duck
        this.delegate = delegate;
        this.shapefileJoinAttributeIndex = shapefileJoinAttributeIndex;
        this.animationJoinValueOffset = animationJoinValueOffset;
        this.animationTimeStepRecordOffset = timeStepIndex * animationFileMetaData.getTimeStepRecordCount();
        animationRecordBuffer = new PRMSAnimationRecordBuffer(animationFileMetaData, animationTimeStepRecordOffset, animationTimeStepRecordOffset + animationFileMetaData.getTimeStepRecordCount());
		this.recordEntryDescriptors = animationFileMetaData.getRecordEntryDescriptors().toArray(new RecordEntryDescriptor[0]);
    }

    @Override
    public void close() throws IOException {
        try {
            delegate.close();
        } catch (IOException e) {
            /* don't care */
        }
        try {
            animationRecordBuffer.close();
        } catch (IOException e) {
            /* don't care */
        }
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
        animationRecord = animationRecordBuffer.getRecord(animationTimeStepRecordOffset + ((Number) delegate.read(shapefileJoinAttributeIndex)).intValue() - animationJoinValueOffset);
		_isRecordDebuged = false;
    }
	
	//true if we have already printed debug info for this row
	private volatile boolean _isRecordDebuged = false;
	
	/**
	 * Hack method to display all the attributes within the various files
	 * @throws IOException 
	 */
	public void logCurrentRecord() throws IOException {
		LOGGER.log(Level.FINE, "###################################################");
		LOGGER.log(Level.FINE, "Debug record info for record #" + getRecordNumber());
		LOGGER.log(Level.FINE, "###################################################");
		
		//Log new shapefile record info
		LOGGER.log(Level.FINE, "# Shapefile Record for #" + getRecordNumber() + " : ");
		for (int i=0; i < delegate.getAttributeCount(); i++) {
			
			LOGGER.log(Level.FINE, "" + i + ": " + delegate.getAttributeType(i).getLocalName() + " : " + delegate.read(i).toString());
		}
		
		//Log new animation record info
		LOGGER.log(Level.FINE, "# Animation Record for #" + getRecordNumber() + ", NHRU: " + animationRecord.getNHRU());
		LOGGER.log(Level.FINE, "0: " + recordEntryDescriptors[0].getName() + " : " + animationRecord.getTimeStamp());
		LOGGER.log(Level.FINE, "1: " + recordEntryDescriptors[1].getName() + " : " + animationRecord.getNHRU());
		for (int i=2; i < animationRecord.getColumnCount(); i++) {
			
			LOGGER.log(Level.FINE, "" + i + ": " + recordEntryDescriptors[i].getName() + " : " + animationRecord.getValue(i));
		}
		LOGGER.log(Level.FINE, "###################################################");
	}

    @Override
    public Object read(int attributeIndex) throws IOException, ArrayIndexOutOfBoundsException {
		
		if (LOGGER.isLoggable(Level.FINE) && ! _isRecordDebuged) {
			_isRecordDebuged = true;
			logCurrentRecord();
		}
		
		Object ret = null;
		
        if (isNhruData(attributeIndex)) {
			
			int animationRecordIndex = mapToNhruColumn(attributeIndex);
			
            switch (animationRecordIndex) {
                case 0:
                    ret = animationRecord.getTimeStamp().toDate();
					break;
                case 1:
                    ret = animationRecord.getNHRU();
					break;
                default:
                    ret = animationRecord.getValue(animationRecordIndex);
            }
			

        } else {
			ret = delegate.read(attributeIndex);
        }
		

		return ret;
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
	
	/**
	 * Returns true to indicate that this attribute needs to be read from the NHRU
	 * file and not from the shapefile.
	 * 
	 * @param attributeIndex
	 * @return 
	 */
	protected boolean isNhruData(int attributeIndex) {
		return super.getAttributeType(attributeIndex).getUserData().containsKey(NHRU_FILE_ATTRIB_COLUMN);
	}
	
	/**
	 * For attributes that need to be read from the Nhru file, this method will
	 * map the attribute index to the nhru column index.
	 * 
	 * Return -1 if the column does not map to the nhru data.
	 * 
	 * @param attributeIndex
	 * @return 
	 */
	protected int mapToNhruColumn(int attributeIndex) {
		Integer idx = (Integer) super.getAttributeType(attributeIndex).getUserData().get(NHRU_FILE_ATTRIB_COLUMN);
		
		if (idx != null) {
			return idx;
		} else {
			return -1;
		}
		
	}
    
}
