/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.util.PixelConverter;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.CheckedListDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;


public class ProjectsWorkbookPage extends BuildPathBasePage {
			
	private ListDialogField fClassPathList;
	private IJavaProject fCurrJProject;
	
	private CheckedListDialogField fProjectsList;
	
	public ProjectsWorkbookPage(ListDialogField classPathList) {
		fClassPathList= classPathList;
				
		ProjectsListListener listener= new ProjectsListListener();
		
		String[] buttonLabels= new String[] {
			/* 0 */ NewWizardMessages.getString("ProjectsWorkbookPage.projects.checkall.button"), //$NON-NLS-1$
			/* 1 */ NewWizardMessages.getString("ProjectsWorkbookPage.projects.uncheckall.button") //$NON-NLS-1$
		};
		
		fProjectsList= new CheckedListDialogField(null, buttonLabels, new CPListLabelProvider());
		fProjectsList.setDialogFieldListener(listener);
		fProjectsList.setLabelText(NewWizardMessages.getString("ProjectsWorkbookPage.projects.label")); //$NON-NLS-1$
		fProjectsList.setCheckAllButtonIndex(0);
		fProjectsList.setUncheckAllButtonIndex(1);
		
		fProjectsList.setViewerSorter(new CPListElementSorter());
	}
	
	public void init(IJavaProject jproject) {
		updateProjectsList(jproject);
	}
		
	private void updateProjectsList(IJavaProject currJProject) {
		try {
			IJavaModel jmodel= currJProject.getJavaModel();
			IJavaProject[] jprojects= jmodel.getJavaProjects();
			
			List projects= new ArrayList(jprojects.length);
			
			// a vector remembering all projects that dont have to be added anymore
			List existingProjects= new ArrayList(jprojects.length);
			existingProjects.add(currJProject.getProject());
			
			final List checkedProjects= new ArrayList(jprojects.length);
			// add the projects-cpentries that are already on the class path
			List cpelements= fClassPathList.getElements();
			for (int i= cpelements.size() - 1 ; i >= 0; i--) {
				CPListElement cpelem= (CPListElement)cpelements.get(i);
				if (isEntryKind(cpelem.getEntryKind())) {
					existingProjects.add(cpelem.getResource());
					projects.add(cpelem);
					checkedProjects.add(cpelem);
				}
			}
			
			for (int i= 0; i < jprojects.length; i++) {
				IProject proj= jprojects[i].getProject();
				if (!existingProjects.contains(proj)) {
					projects.add(new CPListElement(fCurrJProject, IClasspathEntry.CPE_PROJECT, proj.getFullPath(), proj));
				}
			}	
						
			fProjectsList.setElements(projects);
			fProjectsList.setCheckedElements(checkedProjects);
				
		} catch (JavaModelException e) {
			// no solution exists or other problems: create an empty list
			fProjectsList.setElements(new ArrayList(5));
		}
		fCurrJProject= currJProject;
	}		
		
	// -------- UI creation ---------
		
	public Control getControl(Composite parent) {
		PixelConverter converter= new PixelConverter(parent);
		
		Composite composite= new Composite(parent, SWT.NONE);
			
		LayoutUtil.doDefaultLayout(composite, new DialogField[] { fProjectsList }, true, SWT.DEFAULT, SWT.DEFAULT);
		LayoutUtil.setHorizontalGrabbing(fProjectsList.getListControl(null));
		
		int buttonBarWidth= converter.convertWidthInCharsToPixels(24);
		fProjectsList.setButtonsMinWidth(buttonBarWidth);
				
		return composite;
	}
	
	private class ProjectsListListener implements IDialogFieldListener {
			
		// ---------- IDialogFieldListener --------
	
		public void dialogFieldChanged(DialogField field) {
			if (fCurrJProject != null) {
				// already initialized
				updateClasspathList();
			}
		}
	}
	
	private void updateClasspathList() {
		List projelements= fProjectsList.getCheckedElements();
		
		boolean remove= false;
		List cpelements= fClassPathList.getElements();
		// backwards, as entries will be deleted
		for (int i= cpelements.size() -1; i >= 0 ; i--) {
			CPListElement cpe= (CPListElement)cpelements.get(i);
			if (isEntryKind(cpe.getEntryKind())) {
				if (!projelements.remove(cpe)) {
					cpelements.remove(i);
					remove= true;
				}	
			}
		}
		for (int i= 0; i < projelements.size(); i++) {
			cpelements.add(projelements.get(i));
		}
		if (remove || (projelements.size() > 0)) {
			fClassPathList.setElements(cpelements);
		}
	}
	
	/*
	 * @see BuildPathBasePage#getSelection
	 */
	public List getSelection() {
		return fProjectsList.getSelectedElements();
	}

	/*
	 * @see BuildPathBasePage#setSelection
	 */	
	public void setSelection(List selElements) {
		fProjectsList.selectElements(new StructuredSelection(selElements));
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathBasePage#isEntryKind(int)
	 */
	public boolean isEntryKind(int kind) {
		return kind == IClasspathEntry.CPE_PROJECT;
	}


}
