/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000
 */
package org.eclipse.jdt.internal.ui;


import org.eclipse.core.runtime.IStatus;

/**
 * Status code constants for Java UI plug-in.
 *
 * @see org.eclipse.core.runtime.IStatus
 */
public interface IJavaUIStatus extends IStatus {

	/*
	 * Status code definitions
	 *
	 * Information Only [0-32]
	 * General constants [0-98]
	 */

	/** 
	 * Status code constant (value 3) indicating the operation was  canceled.
	 * Severity: info.
	 */
	public static final int OPERATION_CANCELED= 3;

	/** 
	 * Status code constant (value 76) indicating that an operation failed.
	 * Severity: error. Category: general.
	 */
	public static final int OPERATION_FAILED= 76;

	/** 
	 * Status code constant (value 77) indicating an invalid value.
	 * Severity: error. Category: general.
	 */
	public static final int INVALID_VALUE= 77;

	// Local file system constants [200-298]
	// Information Only [200-232]

	// Warnings [233-265]

	// Errors [266-298]


	// Workspace constants [300-398]
	// Information Only [300-332]

	// Warnings [333-365]

	// Errors [366-398]


	/*
	 * Internal constants [500-598]
	 * Ranges:
	 *  - Information Only [500-532]
	 *  - Warnings [533-565]
	 *  - Errors [566-598]
	 */

	/** 
	 * Status code constant (value 566) indicating an error internal to the
	 * platform has occurred.
	 * Severity: error. Category: internal.
	 */
	public static final int INTERNAL_ERROR= 566;
}
