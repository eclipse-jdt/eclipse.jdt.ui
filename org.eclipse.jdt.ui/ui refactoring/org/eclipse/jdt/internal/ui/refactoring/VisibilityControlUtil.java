/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jdt.core.dom.Modifier;

class VisibilityControlUtil {
	private VisibilityControlUtil(){}
	
	public static Composite createVisibilityControl(Composite parent, final IVisibilityChangeListener visibilityChangeListener, int[] availableVisibilities, int currectVisibility) {
		List allowedVisibilities= convertToIntegerList(availableVisibilities);
		if (allowedVisibilities.size() == 1)
			return null;
		
		Composite composite= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2; layout.marginWidth= 0;
		composite.setLayout(layout);
		
		
		Label label= new Label(composite, SWT.NONE);
		label.setText("Access modifier:");
		
		Composite group= new Composite(composite, SWT.NONE);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		layout= new GridLayout();
		layout.numColumns= 4; layout.marginWidth= 0;
		group.setLayout(layout);
		
		String[] labels= new String[] {
			"&public",
			"pro&tected",
			"defa&ult",
			"pri&vate"
		};
		Integer[] data= new Integer[] {
					new Integer(Modifier.PUBLIC),
					new Integer(Modifier.PROTECTED),
					new Integer(Modifier.NONE),
					new Integer(Modifier.PRIVATE)};
		Integer initialVisibility= new Integer(currectVisibility);
		for (int i= 0; i < labels.length; i++) {
			Button radio= new Button(group, SWT.RADIO);
			Integer visibilityCode= data[i];
			radio.setText(labels[i]);
			radio.setData(visibilityCode);
			radio.setSelection(visibilityCode.equals(initialVisibility));
			radio.setEnabled(allowedVisibilities.contains(visibilityCode));
			radio.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent event) {
					visibilityChangeListener.visibilityChanged(((Integer)event.widget.getData()).intValue());
				}
			});
		}
		label.setLayoutData((new GridData()));
		group.setLayoutData((new GridData(GridData.FILL_HORIZONTAL)));
		return composite;
	}
	private static List convertToIntegerList(int[] array) {
		List result= new ArrayList(array.length);
		for (int i= 0; i < array.length; i++) {
			result.add(new Integer(array[i]));
		}
		return result;
	}
}
