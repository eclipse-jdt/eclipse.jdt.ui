/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards.dialogfields;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jdt.internal.ui.wizards.swt.MGridData;
import org.eclipse.jdt.internal.ui.wizards.swt.MGridLayout;

public class LayoutUtil {
	
	/**
	 * Calculates the number of columns needed by field editors
	 */
	public static int getNumberOfColumns(DialogField[] editors) {
		int nCulumns= 0;
		for (int i= 0; i < editors.length; i++) {
			nCulumns= Math.max(editors[i].getNumberOfControls(), nCulumns);
		}
		return nCulumns;
	}
	
	/**
	 * Creates a composite and fills in the given editors.
	 * @param labelOnTop Defines if the label of all fields should be on top of the fields
	 */	
	public static void doDefaultLayout(Composite parent, DialogField[] editors, boolean labelOnTop) {
		doDefaultLayout(parent, editors, labelOnTop, 0, 0, 0, 0);
	}

	/**
	 * Creates a composite and fills in the given editors.
	 * @param labelOnTop Defines if the label of all fields should be on top of the fields
	 * @param minWidth The minimal width of the composite
	 * @param minHeight The minimal height of the composite 
	 */
	public static void doDefaultLayout(Composite parent, DialogField[] editors, boolean labelOnTop, int minWidth, int minHeight) {
		doDefaultLayout(parent, editors, labelOnTop, minWidth, minHeight, 0, 0);
	}

	/**
	 * Creates a composite and fills in the given editors.
	 * @param labelOnTop Defines if the label of all fields should be on top of the fields
	 * @param minWidth The minimal width of the composite
	 * @param minHeight The minimal height of the composite
	 * @param marginWidth The margin width to be used by the composite
	 * @param marginHeight The margin height to be used by the composite
	 */	
	public static void doDefaultLayout(Composite parent, DialogField[] editors, boolean labelOnTop, int minWidth, int minHeight, int marginWidth, int marginHeight) {
		int nCulumns= getNumberOfColumns(editors);
		Control[][] controls= new Control[editors.length][];
		for (int i= 0; i < editors.length; i++) {
			controls[i]= editors[i].doFillIntoGrid(parent, nCulumns);
		}
		if (labelOnTop) {
			nCulumns--;
			modifyLabelSpans(controls, nCulumns);
		}
		MGridLayout layout= new MGridLayout();
		layout.marginWidth= marginWidth;
		layout.marginHeight= marginHeight;	
		layout.minimumWidth= minWidth;
		layout.minimumHeight= minHeight;
		layout.numColumns= nCulumns;		
		parent.setLayout(layout);
	}
	
	private static void modifyLabelSpans(Control[][] controls, int nCulumns) {
		for (int i= 0; i < controls.length; i++) {
			setHorizontalSpan(controls[i][0], nCulumns);
		}
	}
	
	/**
	 * Sets the span of a control. Assumes that MGriddata is used.
	 */
	public static void setHorizontalSpan(Control control, int span) {
		Object ld= control.getLayoutData();
		if (ld instanceof MGridData) {
			((MGridData)ld).horizontalSpan= span;
		} else if (span != 1) {
			MGridData gd= new MGridData();
			gd.horizontalSpan= span;
			control.setLayoutData(gd);
		}
	}	

}