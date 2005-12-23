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

import java.util.List;

import org.eclipse.core.runtime.IPath;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;

public class AddSourceFolderWizard extends BuildPathWizard {
	
	private AddSourceFolderWizardPage fAddFolderPage;
	private SetFilterWizardPage fFilterPage;

	public AddSourceFolderWizard(CPListElement[] existingEntries, CPListElement newEntry, IPath outputLocation) {
		super(existingEntries, newEntry, outputLocation, getTitel(newEntry), JavaPluginImages.DESC_WIZBAN_NEWSRCFOLDR);
	}

	private static String getTitel(CPListElement newEntry) {
		if (newEntry.getPath() == null) {
			return NewWizardMessages.NewSourceFolderCreationWizard_title;
		} else {
			return NewWizardMessages.NewSourceFolderCreationWizard_edit_title;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void addPages() {
		super.addPages();
	
		fAddFolderPage= new AddSourceFolderWizardPage(getEntryToEdit(), getExistingEntries(), getOutputLocation());
		addPage(fAddFolderPage);
		
		fFilterPage= new SetFilterWizardPage(getEntryToEdit(), getExistingEntries(), getOutputLocation());
		addPage(fFilterPage);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public List getInsertedElements() {
		List result= super.getInsertedElements();
		if (getEntryToEdit().getOrginalPath() == null)
			result.add(getEntryToEdit());
		
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	public List getRemovedElements() {
		return fAddFolderPage.getRemovedElements();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public List getModifiedElements() {
		return fAddFolderPage.getModifiedElements();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean performFinish() {
		getEntryToEdit().setAttribute(CPListElement.INCLUSION, fFilterPage.getInclusionPattern());
		getEntryToEdit().setAttribute(CPListElement.EXCLUSION, fFilterPage.getExclusionPattern());
		setOutputLocation(fAddFolderPage.getOutputLocation());
		
		boolean res= super.performFinish();
		if (res) {
			selectAndReveal(fAddFolderPage.getCorrespondingResource());
		}
		return res;
	}
}
