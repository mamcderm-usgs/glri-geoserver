package gov.usgs.cida.prms;

/**
 *
 * @author tkunicki
 */
public interface IRange<N extends Comparable<N>> {

	public N getMinimum();

	public N getMaximum();
	
}
