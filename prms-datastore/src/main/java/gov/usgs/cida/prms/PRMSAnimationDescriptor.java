/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gov.usgs.cida.prms;

/**
 *
 * @author tkunicki
 */
public class PRMSAnimationDescriptor {

	private PRMSAnimationFileMetaData fileMetaData;
	private RecordEntryDescriptor recordEntryDescriptor;
	private RecordEntryRange recordEntryRange;

	// TEST ONLY
	public PRMSAnimationDescriptor() {
	}

	public PRMSAnimationDescriptor(PRMSAnimationFileMetaData fileMetaData, RecordEntryDescriptor recordEntryDescriptor) {
		this.recordEntryDescriptor = recordEntryDescriptor;

		this.fileMetaData = fileMetaData;

		// TODO: this hints of maybe a need to refactor...
		recordEntryRange = null;
	}

	public RecordEntryDescriptor getRecordEntryDescriptor() {
		return recordEntryDescriptor;
	}

	public RecordEntryRange getRecordEntryRange() {
		return recordEntryRange;
	}

	public PRMSAnimationFileMetaData getFileMetaData() {
		return fileMetaData;
	}

}
