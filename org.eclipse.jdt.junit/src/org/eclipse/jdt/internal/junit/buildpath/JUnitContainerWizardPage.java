/*******************************************************************************
 * Copyright (c) 2006, 2025 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.buildpath;

import org.eclipse.jdt.junit.JUnitCore;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;

import org.eclipse.core.runtime.IPath;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.layout.PixelConverter;

import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.junit.BasicElementLabels;
import org.eclipse.jdt.internal.junit.ui.JUnitMessages;
import org.eclipse.jdt.internal.junit.util.ExceptionHandler;
import org.eclipse.jdt.internal.junit.util.JUnitStatus;

import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPage;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPageExtension;
import org.eclipse.jdt.ui.wizards.NewElementWizardPage;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.util.SWTUtil;


public class JUnitContainerWizardPage extends NewElementWizardPage implements IClasspathContainerPage, IClasspathContainerPageExtension {

	private IClasspathEntry fContainerEntryResult;
	private Combo fVersionCombo;
	private Text fResolvedPath;
	private Text fResolvedSourcePath;
	private Button fVintage;
	private Link fLink;

	public JUnitContainerWizardPage() {
		super("JUnitContainerPage"); //$NON-NLS-1$
		setTitle(JUnitMessages.JUnitContainerWizardPage_wizard_title);
		setDescription(JUnitMessages.JUnitContainerWizardPage_wizard_description);
		setImageDescriptor(JavaPluginImages.DESC_WIZBAN_ADD_LIBRARY);

		fContainerEntryResult= JavaCore.newContainerEntry(JUnitCore.JUNIT3_CONTAINER_PATH);
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

	@Override
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

	@Override
	public IClasspathEntry getSelection() {
		return fContainerEntryResult;
	}

	@Override
	public void setSelection(IClasspathEntry containerEntry) {
		fContainerEntryResult= containerEntry;
	}

	@Override
	public void createControl(Composite parent) {
		PixelConverter converter= new PixelConverter(parent);

		Composite composite= new Composite(parent, SWT.NONE);
		composite.setFont(parent.getFont());

		composite.setLayout(new GridLayout(2, false));

		Label label= new Label(composite, SWT.NONE);
		label.setFont(composite.getFont());
		label.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, false, false, 1, 1));
		label.setText(JUnitMessages.JUnitContainerWizardPage_combo_label);

		Composite comboComposite= new Composite(composite, SWT.NONE);
		comboComposite.setLayout(new GridLayout(3, false));
		fVersionCombo= new Combo(comboComposite, SWT.READ_ONLY);
		fVersionCombo.setItems(JUnitMessages.JUnitContainerWizardPage_option_junit3, JUnitMessages.JUnitContainerWizardPage_option_junit4, JUnitMessages.JUnitContainerWizardPage_option_junit5);
		fVersionCombo.setFont(comboComposite.getFont());
		fVintage = new Button(comboComposite, SWT.CHECK);
		fLink= new Link(comboComposite, SWT.NONE);
		fLink.setText(JUnitMessages.JUnitContainerWizardPage_enableVintage);
		fLink.setToolTipText(JUnitMessages.JUnitContainerWizardPage_enableVintage_tooltip);
		fVintage.setToolTipText(JUnitMessages.JUnitContainerWizardPage_enableVintage_tooltip);
		fLink.addSelectionListener(SelectionListener.widgetSelectedAdapter(e->Program.launch("https://junit.org/junit5/docs/current/user-guide/#overview-what-is-junit-5"))); //$NON-NLS-1$

		GridData data= new GridData(GridData.BEGINNING, GridData.CENTER, false, false, 1, 1);
		data.widthHint= converter.convertWidthInCharsToPixels(15);
		fVersionCombo.setLayoutData(data);

		if (fContainerEntryResult != null && JUnitCore.JUNIT3_CONTAINER_PATH.equals(fContainerEntryResult.getPath())) {
			fVersionCombo.select(0);
		} else if (fContainerEntryResult != null && JUnitCore.JUNIT4_CONTAINER_PATH.equals(fContainerEntryResult.getPath())) {
			fVersionCombo.select(1);
		} else {
			fVersionCombo.select(2);
			fVintage.setSelection(fContainerEntryResult!=null && JUnitCore.isVintage(fContainerEntryResult.getExtraAttributes()));
		}
		fVersionCombo.addModifyListener(e -> doSelectionChanged());
		fVintage.addSelectionListener(SelectionListener.widgetSelectedAdapter(e->doSelectionChanged()));

		label= new Label(composite, SWT.NONE);
		label.setFont(composite.getFont());
		label.setText(JUnitMessages.JUnitContainerWizardPage_resolved_label);
		label.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, false, false, 1, 1));

		fResolvedPath= new Text(composite, SWT.READ_ONLY | SWT.WRAP);
		SWTUtil.fixReadonlyTextBackground(fResolvedPath);
		data= new GridData(GridData.FILL, GridData.FILL, true, false, 1, 1);
		data.widthHint= converter.convertWidthInCharsToPixels(60);
		fResolvedPath.setFont(composite.getFont());
		fResolvedPath.setLayoutData(data);

		label= new Label(composite, SWT.NONE);
		label.setFont(composite.getFont());
		label.setText(JUnitMessages.JUnitContainerWizardPage_source_location_label);
		label.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, false, false, 1, 1));

		fResolvedSourcePath= new Text(composite,  SWT.READ_ONLY | SWT.WRAP);
		SWTUtil.fixReadonlyTextBackground(fResolvedSourcePath);
		data= new GridData(GridData.FILL, GridData.FILL, true, false, 1, 1);
		data.widthHint= converter.convertWidthInCharsToPixels(60);
		fResolvedSourcePath.setFont(composite.getFont());
		fResolvedSourcePath.setLayoutData(data);

		doSelectionChanged();

		setControl(composite);
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			fVersionCombo.setFocus();
		}
	}

	protected void doSelectionChanged() {
		JUnitStatus status= new JUnitStatus();

		IClasspathEntry libEntry;
		IPath containerPath;
		IClasspathAttribute[] extraAttributes;
		if (fVersionCombo != null && fVersionCombo.getSelectionIndex() == 2) {
			containerPath= JUnitCore.JUNIT5_CONTAINER_PATH;
			libEntry= BuildPathSupport.getJUnitJupiterApiLibraryEntry();
			fVintage.setVisible(true);
			fLink.setVisible(true);
			extraAttributes= new IClasspathAttribute[] {JavaCore.newClasspathAttribute(JUnitCore.VINTAGE_ATTRIBUTE, Boolean.toString(fVintage.getSelection()))};
		} else if (fVersionCombo != null && fVersionCombo.getSelectionIndex() == 1) {
			containerPath= JUnitCore.JUNIT4_CONTAINER_PATH;
			libEntry= BuildPathSupport.getJUnit4LibraryEntry();
			fVintage.setVisible(false);
			fLink.setVisible(false);
			extraAttributes= new IClasspathAttribute[0];
		} else {
			containerPath= JUnitCore.JUNIT3_CONTAINER_PATH;
			libEntry= BuildPathSupport.getJUnit3LibraryEntry();
			if (libEntry == null)
				libEntry= BuildPathSupport.getJUnit4as3LibraryEntry(); // JUnit 4 includes most of JUnit 3, so let's cheat
			fVintage.setVisible(false);
			fLink.setVisible(false);
			extraAttributes= new IClasspathAttribute[0];
		}

		if (libEntry == null) {
			status.setError(JUnitMessages.JUnitContainerWizardPage_error_version_not_available);
		}
		fContainerEntryResult= JavaCore.newContainerEntry(containerPath, new IAccessRule[0],extraAttributes,
				false);

		if (fResolvedPath != null && !fResolvedPath.isDisposed()) {
			if (libEntry != null) {
				fResolvedPath.setText(getPathLabel(libEntry.getPath()));
			} else {
				fResolvedPath.setText(JUnitMessages.JUnitContainerWizardPage_lib_not_found);
			}
		}
		if (fResolvedSourcePath != null && !fResolvedSourcePath.isDisposed()) {
			if (libEntry != null && libEntry.getSourceAttachmentPath() != null) {
				fResolvedSourcePath.setText(getPathLabel(libEntry.getSourceAttachmentPath()));
			} else {
				fResolvedSourcePath.setText(JUnitMessages.JUnitContainerWizardPage_source_not_found);
			}
		}

		updateStatus(status);
	}

	private String getPathLabel(IPath path) {
		StringBuilder buf= new StringBuilder(BasicElementLabels.getResourceName(path.lastSegment()));
		buf.append(JavaElementLabels.CONCAT_STRING);
		buf.append(BasicElementLabels.getPathLabel(path.removeLastSegments(1), true));
		return buf.toString();
	}

	@Override
	public void initialize(IJavaProject project, IClasspathEntry[] currentEntries) {
	}

}
