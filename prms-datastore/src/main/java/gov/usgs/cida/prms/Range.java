package gov.usgs.cida.prms;

/**
 *
 * @author tkunicki
 */
public class Range<N extends Comparable<N>> implements IRange<N> {

	protected N minimum;
	protected N maximum;

	protected Range() {

	}

	public Range(N minimum, N maximum) {
		this.minimum = minimum;
		this.maximum = maximum;
	}

    @Override
	public N getMinimum() {
		return minimum;
	}

    @Override
	public N getMaximum() {
		return maximum;
	}

}
