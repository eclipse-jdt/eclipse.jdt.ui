/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui.wizards.dialogfields;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jdt.internal.ui.wizards.swt.MGridData;

public class Separator extends DialogField {
	
	private Label fSeparator;
	private int fStyle;
	
	public Separator() {
		this(0);
	}	
	
	public Separator(int style) {
		super();
		fStyle= style;
	}
			
	// ------- layout helpers
	
	public Control[] doFillIntoGrid(Composite parent, int nColumns, int height) {
		assertEnoughColumns(nColumns);
		
		Control separator= getSeparator(parent);
		separator.setLayoutData(gridDataForSeperator(nColumns, height));
		
		return new Control[] { separator };
	}
	
	public Control[] doFillIntoGrid(Composite parent, int nColumns) {
		return doFillIntoGrid(parent, nColumns, 4);
	}
	
	public int getNumberOfControls() {
		return 1;	
	}
	
	protected static MGridData gridDataForSeperator(int span, int height) {
		MGridData gd= new MGridData();
		gd.horizontalAlignment= gd.FILL;
		gd.verticalAlignment= gd.BEGINNING;
		gd.heightHint= height;		
		gd.horizontalSpan= span;
		return gd;
	}
	
	// ------- ui creation	

	public Control getSeparator(Composite parent) {
		if (fSeparator == null) {
			assertCompositeNotNull(parent);
			fSeparator= new Label(parent, fStyle);
		}	
		return fSeparator;
	}

}