/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

public class VisibilityControlUtil {
	private VisibilityControlUtil(){}
	
	public static Composite createVisibilityControl(Composite parent, final IVisibilityChangeListener visibilityChangeListener, int[] availableVisibilities, int correctVisibility) {
		List allowedVisibilities= convertToIntegerList(availableVisibilities);
		if (allowedVisibilities.size() == 1)
			return null;
		
		Group group= new Group(parent, SWT.NONE);
		group.setText(RefactoringMessages.getString("VisibilityControlUtil.Access_modifier")); //$NON-NLS-1$
		GridData gd= new GridData(GridData.FILL_HORIZONTAL);
		group.setLayoutData(gd);
		GridLayout layout= new GridLayout();
		layout.makeColumnsEqualWidth= true;
		layout.numColumns= 4; 
		group.setLayout(layout);
		
		String[] labels= new String[] {
			"&public", //$NON-NLS-1$
			"pro&tected", //$NON-NLS-1$
			RefactoringMessages.getString("VisibilityControlUtil.defa&ult_4"), //$NON-NLS-1$
			"pri&vate" //$NON-NLS-1$
		};
		Integer[] data= new Integer[] {
					new Integer(Modifier.PUBLIC),
					new Integer(Modifier.PROTECTED),
					new Integer(Modifier.NONE),
					new Integer(Modifier.PRIVATE)};
		Integer initialVisibility= new Integer(correctVisibility);
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
		group.setLayoutData((new GridData(GridData.FILL_HORIZONTAL)));
		return group;
	}

	public static Composite createVisibilityControlAndModifiers(Composite parent, final IVisibilityChangeListener visibilityChangeListener, int[] availableVisibilities, int correctVisibility, int[] availableModifiers) {
		Composite visibilityComposite= createVisibilityControl(parent, visibilityChangeListener, availableVisibilities, correctVisibility);

		List allowedModifiers= convertToIntegerList(availableModifiers);
		if (allowedModifiers.size() == 1)
			return null;
			
		String[] labels= new String[] {
			RefactoringMessages.getString("VisibilityControlUtil.abstract"), //$NON-NLS-1$
			RefactoringMessages.getString("VisibilityControlUtil.static"), //$NON-NLS-1$
			RefactoringMessages.getString("VisibilityControlUtil.final"), //$NON-NLS-1$
			RefactoringMessages.getString("VisibilityControlUtil.synchronized"), //$NON-NLS-1$
			RefactoringMessages.getString("VisibilityControlUtil.native"), //$NON-NLS-1$
		};		
		Integer[] data= new Integer[] {
					new Integer(Modifier.ABSTRACT),
					new Integer(Modifier.STATIC),
					new Integer(Modifier.FINAL),
					new Integer(Modifier.SYNCHRONIZED),
					new Integer(Modifier.NATIVE)};
		
		for (int i=0; i < labels.length; i++) {
			if (allowedModifiers.contains(data[i])) {
				Button checkboxButton= new Button(visibilityComposite, SWT.CHECK);
				checkboxButton.setText(labels[i]);
				GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
				checkboxButton.setLayoutData(gd);
				checkboxButton.setData(data[i]);
				checkboxButton.setEnabled(true);
				checkboxButton.setSelection(false);
				checkboxButton.addSelectionListener(new SelectionListener() {
					private Object modifiers;
	
					public void widgetSelected(SelectionEvent event) {
						visibilityChangeListener.modifierChanged(((Integer)event.widget.getData()).intValue(), ((Button) event.widget).getSelection());
					}
		
					public void widgetDefaultSelected(SelectionEvent event) {
						widgetSelected(event);
					}
				});				
			}
		}				
		return visibilityComposite;			
	}

	private static List convertToIntegerList(int[] array) {
		List result= new ArrayList(array.length);
		for (int i= 0; i < array.length; i++) {
			result.add(new Integer(array[i]));
		}
		return result;
	}
}
