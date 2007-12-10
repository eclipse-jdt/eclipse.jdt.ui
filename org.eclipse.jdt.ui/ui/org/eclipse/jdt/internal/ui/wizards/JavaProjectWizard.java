/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageOne;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageTwo;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.workingsets.JavaWorkingSetUpdater;

public class JavaProjectWizard extends NewElementWizard implements IExecutableExtension {

	private NewJavaProjectWizardPageOne fFirstPage;
	private NewJavaProjectWizardPageTwo fSecondPage;

	private IConfigurationElement fConfigElement;

	public JavaProjectWizard() {
		this(null, null);
	}

	public JavaProjectWizard(NewJavaProjectWizardPageOne pageOne, NewJavaProjectWizardPageTwo pageTwo) {
		setDefaultPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_NEWJPRJ);
		setDialogSettings(JavaPlugin.getDefault().getDialogSettings());
		setWindowTitle(NewWizardMessages.JavaProjectWizard_title); 

		fFirstPage= pageOne;
		fSecondPage= pageTwo;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	public void addPages() {
		if (fFirstPage == null)
			fFirstPage= new NewJavaProjectWizardPageOne();
		addPage(fFirstPage);

		if (fSecondPage == null)
			fSecondPage= new NewJavaProjectWizardPageTwo(fFirstPage);
		addPage(fSecondPage);

		fFirstPage.setWorkingSets(getWorkingSets(getSelection()));
	}		

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.wizards.NewElementWizard#finishPage(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void finishPage(IProgressMonitor monitor) throws InterruptedException, CoreException {
		fSecondPage.performFinish(monitor); // use the full progress monitor
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizard#performFinish()
	 */
	public boolean performFinish() {
		boolean res= super.performFinish();
		if (res) {
			final IJavaElement newElement= getCreatedElement();

			IWorkingSet[] workingSets= fFirstPage.getWorkingSets();
			if (workingSets.length > 0) {
				PlatformUI.getWorkbench().getWorkingSetManager().addToWorkingSets(newElement, workingSets);
			}

			BasicNewProjectResourceWizard.updatePerspective(fConfigElement);
			selectAndReveal(fSecondPage.getJavaProject().getProject());				

			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					PackageExplorerPart activePackageExplorer= getActivePackageExplorer();
					if (activePackageExplorer != null) {
						activePackageExplorer.tryToReveal(newElement);
					}
				}
			});
		}
		return res;
	}

	protected void handleFinishException(Shell shell, InvocationTargetException e) {
		String title= NewWizardMessages.JavaProjectWizard_op_error_title; 
		String message= NewWizardMessages.JavaProjectWizard_op_error_create_message;			 
		ExceptionHandler.handle(e, getShell(), title, message);
	}	

	/*
	 * Stores the configuration element for the wizard.  The config element will be used
	 * in <code>performFinish</code> to set the result perspective.
	 */
	public void setInitializationData(IConfigurationElement cfig, String propertyName, Object data) {
		fConfigElement= cfig;
	}

	/* (non-Javadoc)
	 * @see IWizard#performCancel()
	 */
	public boolean performCancel() {
		fSecondPage.performCancel();
		return super.performCancel();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.wizards.NewElementWizard#getCreatedElement()
	 */
	public IJavaElement getCreatedElement() {
		return fSecondPage.getJavaProject();
	}

	private static final IWorkingSet[] EMPTY_WORKING_SET_ARRAY = new IWorkingSet[0];

	private IWorkingSet[] getWorkingSets(IStructuredSelection selection) {
		IWorkingSet[] selected= getSelectedWorkingSet(selection);
		if (selected != null && selected.length > 0) {
			for (int i= 0; i < selected.length; i++) {
				if (!isValidWorkingSet(selected[i]))
					return EMPTY_WORKING_SET_ARRAY;
			}
			return selected;
		}

		PackageExplorerPart explorerPart= getActivePackageExplorer();
		if (explorerPart == null)
			return EMPTY_WORKING_SET_ARRAY;

		if (explorerPart.getRootMode() == PackageExplorerPart.PROJECTS_AS_ROOTS) {				
			//Get active filter
			IWorkingSet filterWorkingSet= explorerPart.getFilterWorkingSet();
			if (filterWorkingSet == null)
				return EMPTY_WORKING_SET_ARRAY;

			if (!isValidWorkingSet(filterWorkingSet))
				return EMPTY_WORKING_SET_ARRAY;

			return new IWorkingSet[] {filterWorkingSet};
		} else {
			//If we have been gone into a working set return the working set
			Object input= explorerPart.getViewPartInput();
			if (!(input instanceof IWorkingSet))
				return EMPTY_WORKING_SET_ARRAY;

			IWorkingSet workingSet= (IWorkingSet)input;
			if (!isValidWorkingSet(workingSet))
				return EMPTY_WORKING_SET_ARRAY;

			return new IWorkingSet[] {workingSet};
		}
	}

	private IWorkingSet[] getSelectedWorkingSet(IStructuredSelection selection) {
		if (!(selection instanceof ITreeSelection))
			return EMPTY_WORKING_SET_ARRAY;

		ITreeSelection treeSelection= (ITreeSelection) selection;
		if (treeSelection.isEmpty())
			return EMPTY_WORKING_SET_ARRAY;

		List elements= treeSelection.toList();
		if (elements.size() == 1) {
			Object element= elements.get(0);
			TreePath[] paths= treeSelection.getPathsFor(element);
			if (paths.length != 1)
				return EMPTY_WORKING_SET_ARRAY;

			TreePath path= paths[0];
			if (path.getSegmentCount() == 0)
				return EMPTY_WORKING_SET_ARRAY;

			Object candidate= path.getSegment(0);
			if (!(candidate instanceof IWorkingSet))
				return EMPTY_WORKING_SET_ARRAY;

			IWorkingSet workingSetCandidate= (IWorkingSet) candidate;
			if (isValidWorkingSet(workingSetCandidate))
				return new IWorkingSet[] { workingSetCandidate };

			return EMPTY_WORKING_SET_ARRAY;
		}

		ArrayList result= new ArrayList();
		for (Iterator iterator= elements.iterator(); iterator.hasNext();) {
			Object element= iterator.next();
			if (element instanceof IWorkingSet && isValidWorkingSet((IWorkingSet) element)) {
				result.add(element);
			}
		}
		return (IWorkingSet[]) result.toArray(new IWorkingSet[result.size()]);
	}

	private PackageExplorerPart getActivePackageExplorer() {
		PackageExplorerPart explorerPart= PackageExplorerPart.getFromActivePerspective();
		if (explorerPart == null)
			return null;

		IWorkbenchPage activePage= explorerPart.getViewSite().getWorkbenchWindow().getActivePage();
		if (activePage == null)
			return null;

		if (activePage.getActivePart() != explorerPart)
			return null;

		return explorerPart;
	}

	private static boolean isValidWorkingSet(IWorkingSet workingSet) {
		String id= workingSet.getId();	
		if (!JavaWorkingSetUpdater.ID.equals(id) && !"org.eclipse.ui.resourceWorkingSetPage".equals(id)) //$NON-NLS-1$
			return false;

		if (workingSet.isAggregateWorkingSet())
			return false;

		return true;
	}

}
