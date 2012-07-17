/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gov.usgs.cida.prms;

/**
 *
 * @author tkunicki
 */
public class RecordEntryDescriptor {

	public enum Type {
		STRING,
		INT,
		FLOAT
	}

	private String name;
	private int index;
	private Type type;
	private int offset;
	private int length;
	private boolean trimRequired;

	public RecordEntryDescriptor(String name, int index, Type type, int offset, int length, boolean trimRequired) {
		this.name = name;
		this.index = index;
		this.type = type;
		this.offset = offset;
		this.length = length;
		this.trimRequired = trimRequired;
	}

	public String getName() {
		return name;
	}

	public int getIndex() {
		return index;
	}

	public int getOffset() {
		return offset;
	}

	public int getLength() {
		return length;
	}

	public boolean isTrimRequired() {
		return trimRequired;
	}

	public Type getType() {
		return type;
	}

	@Override
	public String toString() {
		return name;
	}

}
