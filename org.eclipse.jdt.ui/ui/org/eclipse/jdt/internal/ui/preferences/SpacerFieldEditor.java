/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jface.preference.FieldEditor;

/**
 * A field editor to create spaces.
 */
class SpacerFieldEditor extends FieldEditor {
	
	private Label fLabel;
					
	public SpacerFieldEditor(Composite parent) {
		createControl(parent);
	}			
		
	protected void adjustForNumColumns(int numColumns) {
		((GridData) fLabel.getLayoutData()).horizontalSpan = numColumns;
	}

	protected void doFillIntoGrid(Composite parent, int numColumns) {
		fLabel= new Label(parent, SWT.NONE);
		fLabel.setLayoutData(new GridData());
	}

	public int getNumberOfControls() {
		return 1;
	}
	
	protected void doLoad() {}
	protected void doLoadDefault() {}
	protected void doStore() {}

}