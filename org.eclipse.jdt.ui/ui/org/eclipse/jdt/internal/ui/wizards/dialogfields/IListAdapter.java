/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards.dialogfields;

public interface IListAdapter {
	
	void customButtonPressed(DialogField field, int index);
	void selectionChanged(DialogField field);

}