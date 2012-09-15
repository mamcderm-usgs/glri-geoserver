package org.geotools.data.shapefile.dbf;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.resources.NIOUtilities;

public class FieldIndexedDbaseFileReader extends DbaseFileReader {
    
    public FieldIndexedDbaseFileReader(FileChannel fileChannel) throws IOException {
        super(fileChannel, true, ShapefileDataStore.DEFAULT_STRING_CHARSET);
    }
    
    public FieldIndexedDbaseFileReader(FileChannel fileChannel, boolean useMemoryMappedBuffer) throws IOException {
        super(fileChannel, useMemoryMappedBuffer, ShapefileDataStore.DEFAULT_STRING_CHARSET, TimeZone.getDefault());
    }
    
    public FieldIndexedDbaseFileReader(FileChannel fileChannel, boolean useMemoryMappedBuffer, Charset stringCharset)
            throws IOException {
        super(fileChannel, useMemoryMappedBuffer, stringCharset, TimeZone.getDefault());
    }

    public FieldIndexedDbaseFileReader(FileChannel fileChannel, boolean useMemoryMappedBuffer, Charset stringCharset, TimeZone timeZone)
            throws IOException {
        super(fileChannel, useMemoryMappedBuffer, stringCharset, timeZone);
    }

    public void setCurrentRecordByIndex(int recordIndex) throws IOException, UnsupportedOperationException {
        if (recordIndex > header.getNumRecords() - 1) {
            throw new IllegalArgumentException("recordIndex > recordCount");
        }
        
        long newPosition = this.header.getHeaderLength()
                + this.header.getRecordLength() * (long) (recordIndex - 1);

        if (this.useMemoryMappedBuffer) {
            if(newPosition < this.currentOffset || (this.currentOffset + buffer.limit()) < (newPosition + header.getRecordLength())) {
                NIOUtilities.clean(buffer);
                FileChannel fc = (FileChannel) channel;
                if(fc.size() > newPosition + Integer.MAX_VALUE) {
                    currentOffset = newPosition;
                } else {
                    currentOffset = fc.size() - Integer.MAX_VALUE;
                }
                buffer = fc.map(FileChannel.MapMode.READ_ONLY, currentOffset, Integer.MAX_VALUE);
                buffer.position((int) (newPosition - currentOffset));
            } else {
                buffer.position((int) (newPosition - currentOffset));
            }
        } else {
            if (this.currentOffset <= newPosition
                    && this.currentOffset + buffer.limit() >= newPosition) {
                buffer.position((int) (newPosition - this.currentOffset));
                //System.out.println("Hit");
            } else {
                //System.out.println("Jump");
                FileChannel fc = (FileChannel) this.channel;
                fc.position(newPosition);
                this.currentOffset = newPosition;
                buffer.limit(buffer.capacity());
                buffer.position(0);
                fill(buffer, fc);
                buffer.position(0);
            }
        }
    }
    
    public boolean setCurrentRecordByValue(Object value) throws IOException {
        if (indexMap == null || indexMap.isEmpty()) {
            throw new IllegalArgumentException("index not created");
        }
        if (!indexMap.containsKey(value)) {
            return false;
        }
        setCurrentRecordByIndex(indexMap.get(value));
        return true;
    }
    
    Map<Object, Integer> indexMap = new HashMap<Object, Integer>();
    public void buildFieldIndex(int fieldIndex) throws IOException {
        int fieldCount = header.getNumFields();
        if (!(fieldIndex < fieldCount)) {
            throw new IllegalArgumentException("fieldIndex " + fieldIndex +  " >= " + fieldCount);
        }
        setCurrentRecordByIndex(0);
        indexMap.clear();
        Object[] values = new Object[0];
        for (int recordIndex = 0; hasNext(); ++recordIndex) {
            read(); // required when using readField
            Object value = readField(fieldIndex);
            if (indexMap.put(value, recordIndex) != null) {
                throw new IllegalStateException("record values at for field " + header.getFieldName(fieldIndex) + " are not unique, " + value + " already indexed");
            }
        }
    }
    
    public void buildFieldIndex(String fieldName) throws IOException {
        for (int fieldIndex = 0, fieldCount = header.getNumFields(); fieldIndex < fieldCount; ++fieldIndex) {
            if (header.getFieldName(fieldIndex).equalsIgnoreCase(fieldName)) {
                buildFieldIndex(fieldIndex);
                return;
            }
        }
        throw new IllegalArgumentException("field " + fieldName + " not found in dbf");
    }
    
    public Map<Object, Integer> getFieldIndex() {
        return indexMap;
    }
    
    public void setFieldIndex(Map<Object, Integer> index) {
        if (index == null || index.isEmpty()) {
            throw new IllegalArgumentException("index is null or empty");
        }
        if (index.size() != header.getNumRecords()) {
            throw new IllegalArgumentException("index size doesn't match record count");
        }
        this.indexMap = index;
    }
}
