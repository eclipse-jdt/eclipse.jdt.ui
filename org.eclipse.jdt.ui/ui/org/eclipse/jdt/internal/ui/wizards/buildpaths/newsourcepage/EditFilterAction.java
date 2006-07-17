/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import org.eclipse.core.resources.IProject;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ISetSelectionTarget;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier;
import org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier.IClasspathModifierListener;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.PixelConverter;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElement;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.EditFilterWizard;

public class EditFilterAction extends Action implements ISelectionChangedListener {
	
	private IJavaProject fSelectedProject;
	private IJavaElement fSelectedElement;
	private IWorkbenchSite fSite;
	private IClasspathModifierListener fListener;
	
	public EditFilterAction(IWorkbenchSite site) {
		this(site, PlatformUI.getWorkbench().getProgressService(), null);
	}
	
	protected EditFilterAction(IWorkbenchSite site, IRunnableContext context, IClasspathModifierListener listener) {
		super(NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_Edit_label, JavaPluginImages.DESC_ELCL_CONFIGURE_BUILDPATH_FILTERS);
		
		fSite= site;
		fListener= listener;
		
		setToolTipText(NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_Edit_tooltip); 
		setDescription(NewWizardMessages.PackageExplorerActionGroup_FormText_Edit);
		setDisabledImageDescriptor(JavaPluginImages.DESC_DLCL_CONFIGURE_BUILDPATH_FILTERS);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	public void run() {
		Shell shell= getShell();
	
		try {
			EditFilterWizard wizard= createWizard();
			wizard.init(PlatformUI.getWorkbench(), new StructuredSelection(fSelectedElement));
			
			WizardDialog dialog= new WizardDialog(shell, wizard);
			if (shell != null) {
				PixelConverter converter= new PixelConverter(shell);
				dialog.setMinimumPageSize(converter.convertWidthInCharsToPixels(70), converter.convertHeightInCharsToPixels(20));
			}
			dialog.create();
			int res= dialog.open();
			if (res == Window.OK) {
				if (fListener != null)
					fListener.classpathEntryChanged(wizard.getExistingEntries());
				
				selectAndReveal(new StructuredSelection(wizard.getCreatedElement()));
			}
			
			notifyResult(res == Window.OK);
		} catch (CoreException e) {
			String title= NewWizardMessages.AbstractOpenWizardAction_createerror_title; 
			String message= NewWizardMessages.AbstractOpenWizardAction_createerror_message; 
			ExceptionHandler.handle(e, shell, title, message);
		}
	}
	
	private Shell getShell() {
		if (fSite == null)
			return JavaPlugin.getActiveWorkbenchShell();
		
	    return fSite.getShell() != null ? fSite.getShell() : JavaPlugin.getActiveWorkbenchShell();
    }
	
	private EditFilterWizard createWizard() throws CoreException {
		CPListElement[] existingEntries= CPListElement.createFromExisting(fSelectedProject);
		CPListElement elementToEdit= findElement(fSelectedElement, existingEntries);
		return new EditFilterWizard(existingEntries, elementToEdit, getOutputLocation(fSelectedProject));
	}
	
	private IPath getOutputLocation(IJavaProject javaProject) {
		try {
			return javaProject.getOutputLocation();		
		} catch (CoreException e) {
			IProject project= javaProject.getProject();
			IPath projPath= project.getFullPath();
			return projPath.append(PreferenceConstants.getPreferenceStore().getString(PreferenceConstants.SRCBIN_BINNAME));
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void selectionChanged(SelectionChangedEvent event) {
        ISelection selection = event.getSelection();
        if (selection instanceof IStructuredSelection) {
			setEnabled(canHandle((IStructuredSelection) selection));
        } else {
			setEnabled(canHandle(StructuredSelection.EMPTY));
        }
	}
	
	private static CPListElement findElement(IJavaElement element, CPListElement[] elements) {
		IPath path= element.getPath();
		for (int i= 0; i < elements.length; i++) {
			CPListElement cur= elements[i];
			if (cur.getEntryKind() == IClasspathEntry.CPE_SOURCE && cur.getPath().equals(path)) {
				return cur;
			}
		}
		return null;
	}

	public boolean canHandle(IStructuredSelection selection) {
		if (selection.size() != 1)
			return false;
		
		try {
			Object element= selection.getFirstElement();
			if (element instanceof IJavaProject) {
				IJavaProject project= (IJavaProject)element;	
				if (ClasspathModifier.isSourceFolder(project)) {
					fSelectedProject= project;
					fSelectedElement= (IJavaElement)element;
					return true;
				}
			} else if (element instanceof IPackageFragmentRoot) {
				IPackageFragmentRoot packageFragmentRoot= ((IPackageFragmentRoot) element);
				IJavaProject project= packageFragmentRoot.getJavaProject();
				if (packageFragmentRoot.getKind() == IPackageFragmentRoot.K_SOURCE && project != null) {
					fSelectedProject= project;
					fSelectedElement= (IJavaElement)element;
					return true;
				}
			}
		} catch (JavaModelException e) {
			return false;
		}
		return false;
	}
	
	protected void selectAndReveal(final ISelection selection) {
		// validate the input
		IWorkbenchPage page= fSite.getPage();
		if (page == null)
			return;

		// get all the view and editor parts
		List parts= new ArrayList();
		IWorkbenchPartReference refs[]= page.getViewReferences();
		for (int i= 0; i < refs.length; i++) {
			IWorkbenchPart part= refs[i].getPart(false);
			if (part != null)
				parts.add(part);
		}
		refs= page.getEditorReferences();
		for (int i= 0; i < refs.length; i++) {
			if (refs[i].getPart(false) != null)
				parts.add(refs[i].getPart(false));
		}

		Iterator itr= parts.iterator();
		while (itr.hasNext()) {
			IWorkbenchPart part= (IWorkbenchPart) itr.next();

			// get the part's ISetSelectionTarget implementation
			ISetSelectionTarget target= null;
			if (part instanceof ISetSelectionTarget)
				target= (ISetSelectionTarget) part;
			else
				target= (ISetSelectionTarget) part.getAdapter(ISetSelectionTarget.class);

			if (target != null) {
				// select and reveal resource
				final ISetSelectionTarget finalTarget= target;
				page.getWorkbenchWindow().getShell().getDisplay().asyncExec(new Runnable() {
					public void run() {
						finalTarget.selectReveal(selection);
					}
				});
			}
		}
	}
	
}