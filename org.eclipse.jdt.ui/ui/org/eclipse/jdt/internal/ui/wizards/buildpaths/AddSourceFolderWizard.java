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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.wizards.NewElementWizard;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;

public class AddSourceFolderWizard extends NewElementWizard {
	
	private AddSourceFolderWizardPage fAddFolderPage;
	private SetFilterWizardPage fFilterPage;
	private CPListElement fNewEntry;
	private final List/*<CPListElement>*/ fExistingEntries;
	private IPackageFragmentRoot fPackageFragmentRoot;
	private boolean fDoFlushChange;
	private final IPath fOutputLocation;

	public AddSourceFolderWizard(IJavaProject project, CPListElement[] existingEntries, CPListElement newEntry, IPath outputLocation) {
		super();
		fOutputLocation= outputLocation;
		setDefaultPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_NEWSRCFOLDR);
		setDialogSettings(JavaPlugin.getDefault().getDialogSettings());
		if (newEntry.getPath() == null) {
			setWindowTitle(NewWizardMessages.NewSourceFolderCreationWizard_title);
		} else {
			setWindowTitle(NewWizardMessages.NewSourceFolderCreationWizard_edit_title);
		}

		fNewEntry= newEntry;
		fExistingEntries= new ArrayList(Arrays.asList(existingEntries));
		fDoFlushChange= true;
	}
	
	public void setDoFlushChange(boolean b) {
		fDoFlushChange= b;
	}

	/*
	 * @see Wizard#addPages
	 */	
	public void addPages() {
		super.addPages();
	
		fAddFolderPage= new AddSourceFolderWizardPage(fNewEntry, fExistingEntries, fOutputLocation);
		addPage(fAddFolderPage);
		
		fFilterPage= new SetFilterWizardPage();
		addPage(fFilterPage);
		fFilterPage.init(fNewEntry);
	}			

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.wizards.NewElementWizard#finishPage(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void finishPage(IProgressMonitor monitor) throws InterruptedException, CoreException {	
		if (fDoFlushChange) {
			BuildPathsBlock.flush(fExistingEntries, fAddFolderPage.getOutputLocation(), monitor);
			
			IJavaProject javaProject= fNewEntry.getJavaProject();
			IProject project= javaProject.getProject();
			IPath projPath= project.getFullPath();
			IPath path= fNewEntry.getPath();
			
			if (!projPath.equals(path) && projPath.isPrefixOf(path)) {
				path= path.removeFirstSegments(projPath.segmentCount());
			}
			
			IFolder folder= project.getFolder(path);
			fPackageFragmentRoot= javaProject.getPackageFragmentRoot(folder);
		}
	}
	
	public CPListElement[] getCreatedElements() {
		if (fNewEntry.getOrginalPath() != null) {
			return new CPListElement[] {};
		} else {
			return new CPListElement[] {fNewEntry};
		}
	}
	
	public CPListElement[] getRemovedElements() {
		return fAddFolderPage.getRemovedElements();
	}
	
	public CPListElement[] getModifiedElements() {
		return fAddFolderPage.getModifiedElements();
	}
	
	public IPath getOutputLocation() {
		return fAddFolderPage.getOutputLocation();
	}
	
	public List getCPListElements() {
		return fExistingEntries;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizard#performFinish()
	 */
	public boolean performFinish() {
		fNewEntry.setAttribute(CPListElement.INCLUSION, fFilterPage.getInclusionPattern());
		fNewEntry.setAttribute(CPListElement.EXCLUSION, fFilterPage.getExclusionPattern());
		
		boolean res= super.performFinish();
		if (res) {
			selectAndReveal(fAddFolderPage.getCorrespondingResource());
		}
		return res;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.wizards.NewElementWizard#getCreatedElement()
	 */
	public IJavaElement getCreatedElement() {
		return fPackageFragmentRoot;
	}		

}
