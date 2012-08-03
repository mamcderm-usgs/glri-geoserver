package gov.usgs.cida.prms;

import org.joda.time.DateTime;

/**
 *
 * @author tkunicki
 */
public interface PRMSAnimationRecord<N extends Comparable> {

    public int getIndex();

    public DateTime getTimeStamp();

    public Integer getNHRU();

    // from 2 to columnCount (i.e. don't use this for timestamp and nhru)
    public Float getValue(int columnIndex);

    // count timestamp, nhru and all others
    public int getColumnCount();
    
}
