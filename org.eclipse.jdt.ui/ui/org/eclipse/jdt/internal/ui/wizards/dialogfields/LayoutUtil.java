/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui.wizards.dialogfields;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jdt.internal.ui.wizards.swt.MGridData;
import org.eclipse.jdt.internal.ui.wizards.swt.MGridLayout;

public class LayoutUtil {
	
	public static int getNumberOfColumns(DialogField[] editors) {
		int nCulumns= 0;
		for (int i= 0; i < editors.length; i++) {
			nCulumns= Math.max(editors[i].getNumberOfControls(), nCulumns);
		}
		return nCulumns;
	}
	
	public static void doDefaultLayout(Composite parent, DialogField[] editors, boolean labelOnTop) {
		doDefaultLayout(parent, editors, labelOnTop, 0, 0, 0, 0);
	}

	public static void doDefaultLayout(Composite parent, DialogField[] editors, boolean labelOnTop, int minWidth, int minHeight) {
		doDefaultLayout(parent, editors, labelOnTop, minWidth, minHeight, 0, 0);
	}
	
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
	

	
	public static void modifyLabelSpans(Control[][] controls, int nCulumns) {
		for (int i= 0; i < controls.length; i++) {
			setHorizontalSpan(controls[i][0], nCulumns);
		}
	}
			
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