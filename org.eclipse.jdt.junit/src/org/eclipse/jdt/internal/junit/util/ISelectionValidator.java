/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.junit.util;

import org.eclipse.core.runtime.IStatus;

/**
 * Used in selection dialogs to validate selections
 */
public interface ISelectionValidator {
	
	/**
 	 * Validates an array of elements and returns the resulting status.
 	 * @param selection The elements to validate
 	 * @return The resulting status
	 */	
	IStatus validate(Object[] selection);
	
}