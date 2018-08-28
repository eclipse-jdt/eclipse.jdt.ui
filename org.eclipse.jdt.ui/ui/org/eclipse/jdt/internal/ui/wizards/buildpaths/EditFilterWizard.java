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

import org.eclipse.core.runtime.IPath;

import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;

public class EditFilterWizard extends BuildPathWizard {

	private SetFilterWizardPage fFilterPage;
	private final IPath[] fOrginalInclusion, fOriginalExclusion;

	public EditFilterWizard(CPListElement[] existingEntries, CPListElement newEntry, IPath outputLocation) {
		super(existingEntries, newEntry, outputLocation, NewWizardMessages.ExclusionInclusionDialog_title, null);

		IPath[] inc= (IPath[])newEntry.getAttribute(CPListElement.INCLUSION);
		fOrginalInclusion= new IPath[inc.length];
		System.arraycopy(inc, 0, fOrginalInclusion, 0, inc.length);

		IPath[] excl= (IPath[])newEntry.getAttribute(CPListElement.EXCLUSION);
		fOriginalExclusion= new IPath[excl.length];
		System.arraycopy(excl, 0, fOriginalExclusion, 0, excl.length);
	}

	/*
	 * @see Wizard#addPages
	 */
	@Override
	public void addPages() {
		super.addPages();

		fFilterPage= new SetFilterWizardPage(getEntryToEdit(), getExistingEntries(), getOutputLocation());
		addPage(fFilterPage);
	}

	@Override
	public boolean performFinish() {
		CPListElement entryToEdit= getEntryToEdit();
		entryToEdit.setAttribute(CPListElement.INCLUSION, fFilterPage.getInclusionPattern());
		entryToEdit.setAttribute(CPListElement.EXCLUSION, fFilterPage.getExclusionPattern());

		return super.performFinish();
	}

	@Override
	public void cancel() {
		CPListElement entryToEdit= getEntryToEdit();
		entryToEdit.setAttribute(CPListElement.INCLUSION, fOrginalInclusion);
		entryToEdit.setAttribute(CPListElement.EXCLUSION, fOriginalExclusion);
	}
}
