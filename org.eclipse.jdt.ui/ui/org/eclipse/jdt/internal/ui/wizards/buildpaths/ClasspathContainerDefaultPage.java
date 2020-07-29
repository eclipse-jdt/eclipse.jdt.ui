/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.jface.dialogs.Dialog;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.wizards.IClasspathContainerPage;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPageExtension;
import org.eclipse.jdt.ui.wizards.NewElementWizardPage;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;

/**
  */
public class ClasspathContainerDefaultPage extends NewElementWizardPage implements IClasspathContainerPage, IClasspathContainerPageExtension {

	private StringDialogField fEntryField;
	private ArrayList<IPath> fUsedPaths;

	/**
	 * Constructor for ClasspathContainerDefaultPage.
	 */
	public ClasspathContainerDefaultPage() {
		super("ClasspathContainerDefaultPage"); //$NON-NLS-1$
		setTitle(NewWizardMessages.ClasspathContainerDefaultPage_title);
		setDescription(NewWizardMessages.ClasspathContainerDefaultPage_description);
		setImageDescriptor(JavaPluginImages.DESC_WIZBAN_ADD_LIBRARY);

		fUsedPaths= new ArrayList<>();

		fEntryField= new StringDialogField();
		fEntryField.setLabelText(NewWizardMessages.ClasspathContainerDefaultPage_path_label);
		fEntryField.setDialogFieldListener(field -> validatePath());
		validatePath();
	}

	private void validatePath() {
		StatusInfo status= new StatusInfo();
		String str= fEntryField.getText();
		if (str.length() == 0) {
			status.setError(NewWizardMessages.ClasspathContainerDefaultPage_path_error_enterpath);
		} else if (!Path.ROOT.isValidPath(str)) {
			status.setError(NewWizardMessages.ClasspathContainerDefaultPage_path_error_invalidpath);
		} else {
			IPath path= new Path(str);
			if (path.segmentCount() == 0) {
				status.setError(NewWizardMessages.ClasspathContainerDefaultPage_path_error_needssegment);
			} else if (fUsedPaths.contains(path)) {
				status.setError(NewWizardMessages.ClasspathContainerDefaultPage_path_error_alreadyexists);
			}
		}
		updateStatus(status);
	}

	@Override
	public void createControl(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.numColumns= 1;
		composite.setLayout(layout);

		fEntryField.doFillIntoGrid(composite, 2);
		LayoutUtil.setHorizontalGrabbing(fEntryField.getTextControl(null));

		fEntryField.setFocus();

		setControl(composite);
		Dialog.applyDialogFont(composite);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IJavaHelpContextIds.CLASSPATH_CONTAINER_DEFAULT_PAGE);
	}

	@Override
	public boolean finish() {
		return true;
	}

	@Override
	public IClasspathEntry getSelection() {
		return JavaCore.newContainerEntry(new Path(fEntryField.getText()));
	}

	@Override
	public void initialize(IJavaProject project, IClasspathEntry[] currentEntries) {
		for (IClasspathEntry curr : currentEntries) {
			if (curr.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
				fUsedPaths.add(curr.getPath());
			}
		}
	}

	@Override
	public void setSelection(IClasspathEntry containerEntry) {
		if (containerEntry != null) {
			fUsedPaths.remove(containerEntry.getPath());
			fEntryField.setText(containerEntry.getPath().toString());
		} else {
			fEntryField.setText(""); //$NON-NLS-1$
		}
	}



}
