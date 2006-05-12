/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.buildpath;

import org.eclipse.core.runtime.IPath;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.wizards.IClasspathContainerPage;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPageExtension;
import org.eclipse.jdt.ui.wizards.NewElementWizardPage;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

import org.eclipse.jdt.internal.junit.ui.JUnitMessages;
import org.eclipse.jdt.internal.junit.util.ExceptionHandler;
import org.eclipse.jdt.internal.junit.util.JUnitStatus;
import org.eclipse.jdt.internal.junit.util.JUnitStubUtility;
import org.eclipse.jdt.internal.junit.util.PixelConverter;

public class JUnitContainerWizardPage extends NewElementWizardPage implements IClasspathContainerPage, IClasspathContainerPageExtension {

	private IJavaProject fProject;
	private IClasspathEntry fContainerEntryResult;
	private Combo fVersionCombo;
	private Label fResolvedPath;

	public JUnitContainerWizardPage() {
		super("JUnitContainerPage"); //$NON-NLS-1$
		setTitle(JUnitMessages.JUnitContainerWizardPage_wizard_title);
		setDescription(JUnitMessages.JUnitContainerWizardPage_wizard_description);
		setImageDescriptor(JavaPluginImages.DESC_WIZBAN_ADD_LIBRARY);
		
		fContainerEntryResult= JavaCore.newContainerEntry(JUnitContainerInitializer.JUNIT3_PATH);
	}

	public static IJavaProject getPlaceholderProject() {
		String name= "####internal"; //$NON-NLS-1$
		IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
		while (true) {
			IProject project= root.getProject(name);
			if (!project.exists()) {
				return JavaCore.create(project);
			}
			name += '1';
		}
	}
	
	public boolean finish() {
		try {
			IJavaProject[] javaProjects= new IJavaProject[] { getPlaceholderProject() };
			IClasspathContainer[] containers= { null };
			JavaCore.setClasspathContainer(fContainerEntryResult.getPath(), javaProjects, containers, null);
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, getShell(), JUnitMessages.JUnitContainerWizardPage_error_title, JUnitMessages.JUnitContainerWizardPage_error_problem_configuring_container);
			return false;
		}
		return true;
	}

	public IClasspathEntry getSelection() {
		return fContainerEntryResult;
	}

	public void setSelection(IClasspathEntry containerEntry) {
		fContainerEntryResult= containerEntry;
	}

	public void createControl(Composite parent) {
		PixelConverter converter= new PixelConverter(parent);
		
		Composite composite= new Composite(parent, SWT.NONE);
		composite.setFont(parent.getFont());
		
		composite.setLayout(new GridLayout(2, false));
		
		Label label= new Label(composite, SWT.NONE);
		label.setFont(composite.getFont());
		label.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, false, false, 1, 1));
		label.setText(JUnitMessages.JUnitContainerWizardPage_combo_label);
		
		fVersionCombo= new Combo(composite, SWT.READ_ONLY);
		fVersionCombo.setItems(new String[] {
				JUnitMessages.JUnitContainerWizardPage_option_junit3,
				JUnitMessages.JUnitContainerWizardPage_option_junit4
		});
		fVersionCombo.setFont(composite.getFont());
		
		GridData data= new GridData(GridData.BEGINNING, GridData.CENTER, false, false, 1, 1);
		data.widthHint= converter.convertWidthInCharsToPixels(15);
		fVersionCombo.setLayoutData(data);
		
		if (fContainerEntryResult != null && JUnitContainerInitializer.JUNIT4_PATH.equals(fContainerEntryResult.getPath())) {
			fVersionCombo.select(1);
		} else {
			fVersionCombo.select(0);
		}
		fVersionCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				doSelectionChanged();
			}
		});
		
		label= new Label(composite, SWT.NONE);
		label.setFont(composite.getFont());
		label.setText(JUnitMessages.JUnitContainerWizardPage_resolved_label);
		label.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, false, false, 1, 1));
		
		fResolvedPath= new Label(composite, SWT.WRAP);
		data= new GridData(GridData.FILL, GridData.FILL, true, false, 1, 1);
		data.widthHint= converter.convertWidthInCharsToPixels(60);
		fResolvedPath.setFont(composite.getFont());
		fResolvedPath.setLayoutData(data);
		
		doSelectionChanged();
		
		setControl(composite);
	}

	protected void doSelectionChanged() {
		JUnitStatus status= new JUnitStatus();
		
		IClasspathEntry libEntry;
		IPath containerPath;
		if (fVersionCombo != null && fVersionCombo.getSelectionIndex() == 1) {
			containerPath= JUnitContainerInitializer.JUNIT4_PATH;
			libEntry= BuildPathSupport.getJUnit4LibraryEntry();
		} else {
			containerPath= JUnitContainerInitializer.JUNIT3_PATH;
			libEntry= BuildPathSupport.getJUnit3LibraryEntry();
		}
		
		if (libEntry == null) {
			status.setError(JUnitMessages.JUnitContainerWizardPage_error_version_not_available);
		} else if (JUnitContainerInitializer.JUNIT4_PATH.equals(containerPath)) {
			if (!JUnitStubUtility.is50OrHigher(fProject)) {
				status.setWarning(JUnitMessages.JUnitContainerWizardPage_warning_java5_required);
			}
		}
		fContainerEntryResult= JavaCore.newContainerEntry(containerPath);
		
		if (fResolvedPath != null && !fResolvedPath.isDisposed()) {
			if (libEntry != null) {
				fResolvedPath.setText(libEntry.getPath().toOSString());
			} else {
				fResolvedPath.setText(new String());
			}
		}
		updateStatus(status);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.wizards.IClasspathContainerPageExtension#initialize(org.eclipse.jdt.core.IJavaProject, org.eclipse.jdt.core.IClasspathEntry[])
	 */
	public void initialize(IJavaProject project, IClasspathEntry[] currentEntries) {
		fProject= project;
	}

}
