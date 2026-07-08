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

import org.eclipse.core.runtime.IPath;

import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;

public class NewSourceFolderWizardPage extends AbstractNewFolderWizardPage {
	private static final String PAGE_NAME= "NewSourceFolderWizardPage"; //$NON-NLS-1$

	public NewSourceFolderWizardPage() {
		super(PAGE_NAME);
		setTitle(NewWizardMessages.NewSourceFolderWizardPage_title);
		setDescription(NewWizardMessages.NewSourceFolderWizardPage_description);
	}

	@Override
	protected IClasspathEntry createNewClassPathEntry(IPath path, IPath[] inclusionPatterns, IPath[] exclusionPatterns, IPath newOutputPath, IClasspathAttribute[] attributes) {
		return JavaCore.newSourceEntry(path, null, null, newOutputPath, attributes);
	}

	@Override
	protected IPath createNewOutputPath() {
		return null;
	}


}
