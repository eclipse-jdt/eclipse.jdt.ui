/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.dialogs;


import java.util.List;

public interface ISelectionValidator {
	void isValid(Object[] selection, StatusInfo res);
}