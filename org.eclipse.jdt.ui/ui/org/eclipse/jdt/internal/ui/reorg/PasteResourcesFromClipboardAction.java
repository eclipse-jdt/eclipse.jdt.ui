/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.reorg;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.actions.CopyFilesAndFoldersOperation;
import org.eclipse.ui.actions.CopyProjectAction;
import org.eclipse.ui.part.ResourceTransfer;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.reorg.CopyRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.reorg.IPackageFragmentRootManipulationQuery;

public class PasteResourcesFromClipboardAction extends SelectionDispatchAction {

	private Clipboard fClipboard;

	protected PasteResourcesFromClipboardAction(IWorkbenchSite site, Clipboard clipboard) {
		super(site);
		Assert.isNotNull(clipboard);
		fClipboard= clipboard;
	}
	
    /*
     * @see SelectionDispatchAction#selectionChanged(IStructuredSelection)
     */
	public void selectionChanged(IStructuredSelection selection) {
		setEnabled(canOperateOn(selection));
	}
	
    /*
     * @see SelectionDispatchAction#run(IStructuredSelection)
     */
	public void run(IStructuredSelection selection) {
		IResource[] resourceData = getClipboardResources();		
		if (resourceData == null || resourceData.length == 0){
			if (canPasteFiles(selection))
				pasteFiles(selection.getFirstElement());
			return;
		}	
			 
		pasteResources(selection, resourceData);
	}

    private void pasteFiles(Object target) {
    	String[] fileData= getClipboardFiles();
    	if (fileData == null)
    		return;
		IContainer container= convertToContainer(target);
		if (container == null)
			return;
				
		new CopyFilesAndFoldersOperation(getShell()).copyFiles(fileData, container);
    }
    
    private IContainer convertToContainer(Object target){
		if (target instanceof IContainer) 
			return (IContainer)target;
		try {
			return (IContainer)((IJavaElement)target).getCorrespondingResource();	
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, ReorgMessages.getString("PasteResourcesFromClipboardAction.error.title"), ReorgMessages.getString("PasteResourcesFromClipboardAction.error.message")); //$NON-NLS-1$ //$NON-NLS-2$
			return null;
		}
    }

	private void pasteResources(IStructuredSelection selection, IResource[] resourceData) {
		if (resourceData[0].getType() == IResource.PROJECT)
			pasteProject((IProject) resourceData[0]);
		else
			ReorgActionFactory.createDnDCopyAction(resourceData, getFirstSelectedResource(selection)).run();
	}
	
	private void pasteProject(IProject project){
		CopyProjectAction cpa= new CopyProjectAction(getShell());
		cpa.selectionChanged(new StructuredSelection(project));
		if (cpa.isEnabled())
			cpa.run();
	}

	//- enablement ---
	private boolean canOperateOn(IStructuredSelection selection){		
		IResource[] resourceData= getClipboardResources();
		if (resourceData == null || resourceData.length == 0)
			return canPasteFiles(selection);
			
		if (ClipboardActionUtil.isOneOpenProject(resourceData))
			return true;
			
		if (selection.size() != 1) //only after we checked the 'one project' case
			return false;
				
		/*
		 * special case: if both source references and resources are in clipboard - disable this action
		 * if a compilation unit is selected.
		 * this is important in the case when a type is copied to the clipboard - we put also its compilation unit
		 */
		TypedSource[] typedSource= getClipboardSourceReference();
		Object firstElement= selection.getFirstElement();
		if (firstElement instanceof IProject && !((IProject)firstElement).isOpen())
			return false;
		if (firstElement instanceof ICompilationUnit && typedSource != null && typedSource.length != 0)
			return false;
		
		if (StructuredSelectionUtil.getResources(selection).length != 1)
			return false;
		
		if (resourceData == null)
			return ClipboardActionUtil.getFirstResource(selection) instanceof IContainer;	
		
		if (! allResourcesExist(resourceData))
			return false;
			
		return canActivateCopyRefactoring(getShell(), resourceData, ClipboardActionUtil.getFirstResource(selection));
	}
	
    private static boolean allResourcesExist(IResource[] resourceData) {
    	for (int i= 0; i < resourceData.length; i++) {
            if (! resourceData[i].exists())
       	        return false;
        }
        return true;
    }
    
    private boolean canPasteFiles(IStructuredSelection selection) {
		String[] clipboardFiles= getClipboardFiles();
        return clipboardFiles != null
        		&& canPasteFilesOn(selection.getFirstElement());
    }

    private static boolean canPasteFilesOn(Object target) {
    	boolean isPackageFragment= target instanceof IPackageFragment;
		boolean isJavaProject= target instanceof IJavaProject;
		boolean isPackageFragmentRoot= target instanceof IPackageFragmentRoot;
		boolean isContainer= target instanceof IContainer;
		
		if (!(isPackageFragment || isJavaProject || isPackageFragmentRoot || isContainer)) 
			return false;
			
		if (isContainer) {
			IContainer container= (IContainer)target;
			if (!container.isReadOnly())
				return true;
		} else {
			IJavaElement element= (IJavaElement)target;
			if (!element.isReadOnly()) 
				return true;
		}
		return false;	
    }

	private static boolean canActivateCopyRefactoring(Shell shell, IResource[] resourceData, IResource selectedResource) {
		try{
			CopyRefactoring ref= createCopyRefactoring(shell, resourceData);
			if (ref == null)
				return false;

			return ref.isValidDestination(ClipboardActionUtil.tryConvertingToJava(selectedResource));
			
		} catch (JavaModelException e){
			return false;
		}	
	}
	
	//-- helpers
	
	private IResource getFirstSelectedResource(IStructuredSelection selection){
		return ClipboardActionUtil.getFirstResource(selection);
	}

	private String[] getClipboardFiles() {
		return ((String[])fClipboard.getContents(FileTransfer.getInstance()));
	}
	
	private IResource[] getClipboardResources() {
		return ((IResource[])fClipboard.getContents(ResourceTransfer.getInstance()));
	}
	
	private TypedSource[] getClipboardSourceReference() {
		return ((TypedSource[])fClipboard.getContents(TypedSourceTransfer.getInstance()));
	}

	private static CopyRefactoring createCopyRefactoring(Shell shell, IResource[] resourceData) throws JavaModelException {
		IPackageFragmentRootManipulationQuery query= JdtCopyAction.createUpdateClasspathQuery(shell);
		return CopyRefactoring.create(ClipboardActionUtil.getConvertedResources(resourceData), new ReorgQueries(), query);
	}
}
