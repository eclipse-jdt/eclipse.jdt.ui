/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.junit.util;

public interface IJUnitStatusConstants {

	// JUnit UI status constants start at 10000 to make sure that we don't
	// collide with resource and java model constants.
	
	public static final int INTERNAL_ERROR= 10001;
	
	/**
	 * Status constant indicating that an validateEdit call has changed the
	 * content of a file on disk.
	 */
	public static final int VALIDATE_EDIT_CHANGED_CONTENT= 10003;
	
}
