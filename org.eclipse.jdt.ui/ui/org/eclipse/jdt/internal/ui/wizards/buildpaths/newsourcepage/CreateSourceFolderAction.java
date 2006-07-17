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
import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier.IClasspathModifierListener;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.PixelConverter;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.AddSourceFolderWizard;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElement;

public class CreateSourceFolderAction extends Action implements ISelectionChangedListener {
	
	private IJavaProject fSelectedProject;
	private final IWorkbenchSite fSite;
	private final IClasspathModifierListener fListener;

	public CreateSourceFolderAction(IWorkbenchSite site) {
		this(site, PlatformUI.getWorkbench().getProgressService(), null);
	}
	
	public CreateSourceFolderAction(IWorkbenchSite site, IRunnableContext context, IClasspathModifierListener listener) {
		
		fSite= site;
		fListener= listener;
		
		setText(ActionMessages.OpenNewSourceFolderWizardAction_text2); 
		setDescription(ActionMessages.OpenNewSourceFolderWizardAction_description); 
		setToolTipText(ActionMessages.OpenNewSourceFolderWizardAction_tooltip); 
		setImageDescriptor(JavaPluginImages.DESC_TOOL_NEWPACKROOT);
		
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.OPEN_SOURCEFOLDER_WIZARD_ACTION);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	public void run() {
		Shell shell= getShell();
	
		try {
			AddSourceFolderWizard wizard= createWizard();
			wizard.init(PlatformUI.getWorkbench(), new StructuredSelection(fSelectedProject));
			
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

    private IPath getOutputLocation(IJavaProject javaProject) {
    	try {
			return javaProject.getOutputLocation();		
		} catch (CoreException e) {
			IProject project= javaProject.getProject();
			IPath projPath= project.getFullPath();
			return projPath.append(PreferenceConstants.getPreferenceStore().getString(PreferenceConstants.SRCBIN_BINNAME));
		}
    }

    private AddSourceFolderWizard createWizard() throws CoreException {
    	CPListElement newEntrie= new CPListElement(fSelectedProject, IClasspathEntry.CPE_SOURCE);
    	CPListElement[] existing= CPListElement.createFromExisting(fSelectedProject);
    	boolean isProjectSrcFolder= CPListElement.isProjectSourceFolder(existing, fSelectedProject);
    	return new AddSourceFolderWizard(existing, newEntrie, getOutputLocation(fSelectedProject), false, false, false, isProjectSrcFolder, isProjectSrcFolder);
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
	
    private boolean canHandle(IStructuredSelection selection) {
    	if (selection.size() == 1 && selection.getFirstElement() instanceof IJavaProject) {
    		fSelectedProject= (IJavaProject)selection.getFirstElement();
    		return true;
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