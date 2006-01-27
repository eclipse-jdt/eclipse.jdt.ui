/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.dialogs.IDialogSettings;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;

public abstract class AbstractCleanUp implements ICleanUp {

	protected static IDialogSettings getSection(IDialogSettings settings, String sectionName) {
		IDialogSettings section= settings.getSection(sectionName);
		if (section == null)
			section= settings.addNewSection(sectionName);
		return section;
	}

	private static final String SETTINGS_FLAG_NAME= "flag"; //$NON-NLS-1$
	
	private int fFlags;
	
	protected AbstractCleanUp(IDialogSettings settings, int defaultFlag) {

		if (settings.get(SETTINGS_FLAG_NAME) == null)
			settings.put(SETTINGS_FLAG_NAME, defaultFlag);
		
		fFlags= settings.getInt(SETTINGS_FLAG_NAME);
	}

	protected AbstractCleanUp(int flag) {
		fFlags= flag;
	}

	public void saveSettings(IDialogSettings settings) {
		settings.put(SETTINGS_FLAG_NAME, fFlags);
	}

	protected void setFlag(int flag, boolean b) {
		if (!isFlag(flag) && b) {
			fFlags |= flag;
		} else if (isFlag(flag) && !b) {
			fFlags &= ~flag;
		}
	}

	protected boolean isFlag(int flag) {
		return (fFlags & flag) != 0;
	}

	protected Button addCheckBox(Composite parent, final int flag, String label) {
		return addCheckBox(parent, flag, label, new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				setFlag(flag, ((Button)e.getSource()).getSelection());
			}
		});
	}

	protected Button addCheckBox(Composite parent, final int flag, String label, SelectionAdapter adapter) {
		Button button= new Button(parent, SWT.CHECK);
		button.setText(label);
		button.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		button.setSelection(isFlag(flag));
		button.addSelectionListener(adapter);
		return button;
	}
	
	protected String removeMemonic(String s) {
		StringBuffer buf= new StringBuffer(s);
		for (int i=buf.length()-1;i>=0;i--) {
			if (buf.charAt(i) == '&')
				buf.deleteCharAt(i);
		}
		return buf.toString();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean canCleanUp(IJavaProject project) {
		return true;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void beginCleanUp(IJavaProject project, ICompilationUnit[] compilationUnits, IProgressMonitor monitor) throws CoreException {
		if (monitor != null)
			monitor.done();
		//Default do nothing
	}

	/**
	 * {@inheritDoc}
	 */
	public void endCleanUp() throws CoreException {
		//Default do nothing
	}
	
}
