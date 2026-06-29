/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.wizards;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IPath;

import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;

public class NewTestSourceFolderWizardPage extends AbstractNewFolderWizardPage {

	private static final String PAGE_NAME= "NewTestSourceFolderWizardPage"; //$NON-NLS-1$

	public NewTestSourceFolderWizardPage() {
		super(PAGE_NAME);
		setTitle(NewWizardMessages.NewTestSourceFolderWizardPage_title);
		setDescription(NewWizardMessages.NewTestSourceFolderWizardPage_description);
	}

	@Override
	protected IClasspathEntry createNewClassPathEntry(IPath path, IPath[] inclusionPatterns, IPath[] exclusionPatterns,IPath newOutputPath, IClasspathAttribute[] attributes) {
		List<IClasspathAttribute> new_attributes = new ArrayList<>(Arrays.asList(attributes));
		new_attributes.add(JavaCore.newClasspathAttribute(IClasspathAttribute.TEST, "true")); //$NON-NLS-1$
		IClasspathAttribute[] new_attributes_array = new IClasspathAttribute[new_attributes.size()];
		new_attributes.toArray(new_attributes_array);
		return JavaCore.newSourceEntry(path, inclusionPatterns, exclusionPatterns, newOutputPath, new_attributes_array);
	}

	@Override
	protected IPath createNewOutputPath() {
		return fCurrJProject.getProject().getFullPath().append("bin_test"); //$NON-NLS-1$
	}

}
