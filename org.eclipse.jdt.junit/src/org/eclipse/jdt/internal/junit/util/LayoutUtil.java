/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.util;

import org.eclipse.jdt.internal.junit.wizards.MethodStubsSelectionButtonGroup;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

public class LayoutUtil {
	
	/**
	 * Calculates the number of columns needed by field editors
	 */
	public static int getNumberOfColumns(MethodStubsSelectionButtonGroup[] editors) {
		int columnCount= 0;
		for (int i= 0; i < editors.length; i++) {
			columnCount= Math.max(editors[i].getNumberOfControls(), columnCount);
		}
		return columnCount;
	}
	
	/**
	 * Creates a composite and fills in the given editors.
	 * @param labelOnTop Defines if the label of all fields should be on top of the fields
	 */	
	public static void doDefaultLayout(Composite parent, MethodStubsSelectionButtonGroup[] editors, boolean labelOnTop) {
		doDefaultLayout(parent, editors, labelOnTop, 0, 0, 0, 0);
	}

	/**
	 * Creates a composite and fills in the given editors.
	 * @param labelOnTop Defines if the label of all fields should be on top of the fields
	 * @param minWidth The minimal width of the composite
	 * @param minHeight The minimal height of the composite 
	 */
	public static void doDefaultLayout(Composite parent, MethodStubsSelectionButtonGroup[] editors, boolean labelOnTop, int minWidth, int minHeight) {
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
	public static void doDefaultLayout(Composite parent, MethodStubsSelectionButtonGroup[] editors, boolean labelOnTop, int minWidth, int minHeight, int marginWidth, int marginHeight) {
		int nCulumns= getNumberOfColumns(editors);
		Control[][] controls= new Control[editors.length][];
		for (int i= 0; i < editors.length; i++) {
			controls[i]= editors[i].doFillIntoGrid(parent, nCulumns);
		}
		if (labelOnTop) {
			nCulumns--;
			modifyLabelSpans(controls, nCulumns);
		}
		GridLayout layout= new GridLayout();
		if (marginWidth != SWT.DEFAULT) {
			layout.marginWidth= marginWidth;
		}
		if (marginHeight != SWT.DEFAULT) {
			layout.marginHeight= marginHeight;
		}
//		layout.minimumWidth= minWidth;
//		layout.minimumHeight= minHeight;
		layout.numColumns= nCulumns;		
		parent.setLayout(layout);
	}
	
	private static void modifyLabelSpans(Control[][] controls, int nCulumns) {
		for (int i= 0; i < controls.length; i++) {
			setHorizontalSpan(controls[i][0], nCulumns);
		}
	}
	
	/**
	 * Sets the span of a control. Assumes that MGridData is used.
	 */
	public static void setHorizontalSpan(Control control, int span) {
		Object ld= control.getLayoutData();
		if (ld instanceof GridData) {
			((GridData)ld).horizontalSpan= span;
		} else if (span != 1) {
			GridData gd= new GridData();
			gd.horizontalSpan= span;
			control.setLayoutData(gd);
		}
	}	

	/**
	 * Sets the width hint of a control. Assumes that MGridData is used.
	 */
	public static void setWidthHint(Control control, int widthHint) {
		Object ld= control.getLayoutData();
		if (ld instanceof GridData) {
			((GridData)ld).widthHint= widthHint;
		}
	}
	
	/**
	 * Sets the horizontal indent of a control. Assumes that MGridData is used.
	 */
	public static void setHorizontalIndent(Control control, int horizontalIndent) {
		Object ld= control.getLayoutData();
		if (ld instanceof GridData) {
			((GridData)ld).horizontalIndent= horizontalIndent;
		}
	}

	/**
	 * Creates a spacer control with the given span.
	 * The composite is assumed to have <code>MGridLayout</code> as
	 * layout.
	 * @param parent The parent composite
	 */			
	public static Control createEmptySpace(Composite parent, int span) {
		Label label= new Label(parent, SWT.LEFT);
		GridData gd= new GridData();
		gd.horizontalAlignment= GridData.BEGINNING;
		gd.grabExcessHorizontalSpace= false;
		gd.horizontalSpan= span;
		gd.horizontalIndent= 0;
		gd.widthHint= 0;
		gd.heightHint= 0;
		label.setLayoutData(gd);
		return label;
	}
	

}
