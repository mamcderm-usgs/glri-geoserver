package gov.usgs.cida.prms;

public class RecordEntryRange<N extends Comparable<N>> extends Range<N> {

	public RecordEntryRange(N initialValue) {
		super(initialValue, initialValue);
	}

	public void update(N value) {
		if (value.compareTo(minimum) < 0) {
			minimum = value;
		} else if (value.compareTo(maximum) > 0) {
			maximum = value;
		}
	}
}
