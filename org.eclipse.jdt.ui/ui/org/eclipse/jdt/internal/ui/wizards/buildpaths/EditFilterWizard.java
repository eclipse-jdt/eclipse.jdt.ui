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
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import org.eclipse.core.runtime.IPath;

import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;

public class EditFilterWizard extends BuildPathWizard {

	private SetFilterWizardPage fFilterPage;
	
	public EditFilterWizard(CPListElement[] existingEntries, CPListElement newEntry, IPath outputLocation) {
		super(existingEntries, newEntry, outputLocation, NewWizardMessages.ExclusionInclusionDialog_title, null);
	}
	
	/*
	 * @see Wizard#addPages
	 */	
	public void addPages() {
		super.addPages();
		
		fFilterPage= new SetFilterWizardPage(getEntryToEdit(), getExistingEntries(), getOutputLocation());
		addPage(fFilterPage);
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizard#performFinish()
	 */
	public boolean performFinish() {
		getEntryToEdit().setAttribute(CPListElement.INCLUSION, fFilterPage.getInclusionPattern());
		getEntryToEdit().setAttribute(CPListElement.EXCLUSION, fFilterPage.getExclusionPattern());
		
		return super.performFinish();
	}
}