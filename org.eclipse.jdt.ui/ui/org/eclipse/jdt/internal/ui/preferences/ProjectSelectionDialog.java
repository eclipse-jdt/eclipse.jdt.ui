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
package org.eclipse.jdt.internal.ui.preferences;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.ui.dialogs.SelectionStatusDialog;

import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaElementSorter;
import org.eclipse.jdt.ui.StandardJavaElementContentProvider;

import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;

public class ProjectSelectionDialog extends SelectionStatusDialog {

	// the visual selection widget group
	private TableViewer fTableViewer;

	// sizing constants
	private final static int SIZING_SELECTION_WIDGET_HEIGHT= 250;
	private final static int SIZING_SELECTION_WIDGET_WIDTH= 300;

	private ViewerFilter fFilter;

	public ProjectSelectionDialog(Shell parentShell, ViewerFilter hasProjectSettingsFilter) {
		super(parentShell);
		setTitle(PreferencesMessages.getString("ProjectSelectionDialog.title"));  //$NON-NLS-1$
		setMessage(PreferencesMessages.getString("ProjectSelectionDialog.desciption")); //$NON-NLS-1$
		fFilter= hasProjectSettingsFilter;
		
        int shellStyle = getShellStyle();
        setShellStyle(shellStyle | SWT.MAX | SWT.RESIZE);
	}

	/* (non-Javadoc)
	 * Method declared on Dialog.
	 */
	protected Control createDialogArea(Composite parent) {
		// page group
		Composite composite= (Composite) super.createDialogArea(parent);

		Font font= parent.getFont();
		composite.setFont(font);

		createMessageArea(composite);

		fTableViewer= new TableViewer(composite, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		fTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				doSelectionChanged(((IStructuredSelection) event.getSelection()).toArray());
			}
		});
		fTableViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
                okPressed();
			}
		});
		GridData data= new GridData(SWT.FILL, SWT.FILL, true, true);
		data.heightHint= SIZING_SELECTION_WIDGET_HEIGHT;
		data.widthHint= SIZING_SELECTION_WIDGET_WIDTH;
		fTableViewer.getTable().setLayoutData(data);

		fTableViewer.setLabelProvider(new JavaElementLabelProvider());
		fTableViewer.setContentProvider(new StandardJavaElementContentProvider());
		fTableViewer.setSorter(new JavaElementSorter());
		fTableViewer.getControl().setFont(font);

		Button checkbox= new Button(composite, SWT.CHECK);
		checkbox.setText(PreferencesMessages.getString("ProjectSelectionDialog.filter")); //$NON-NLS-1$
		checkbox.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false));
		checkbox.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				updateFilter(((Button) e.widget).getSelection());
			}
			public void widgetDefaultSelected(SelectionEvent e) {
				updateFilter(((Button) e.widget).getSelection());
			}
		});
		
		IJavaModel input= JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());
		fTableViewer.setInput(input);
		
		doSelectionChanged(new Object[0]);

		return composite;
	}
	
	protected void updateFilter(boolean selected) {
		if (selected) {
			fTableViewer.addFilter(fFilter);
		} else {
			fTableViewer.removeFilter(fFilter);
		}
	}

	private void doSelectionChanged(Object[] objects) {
		if (objects.length != 1) {
			updateStatus(new StatusInfo(IStatus.ERROR, "")); //$NON-NLS-1$
			setSelectionResult(null);
		} else {
			updateStatus(new StatusInfo()); //$NON-NLS-1$
			setSelectionResult(objects);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.SelectionStatusDialog#computeResult()
	 */
	protected void computeResult() {
	}
}