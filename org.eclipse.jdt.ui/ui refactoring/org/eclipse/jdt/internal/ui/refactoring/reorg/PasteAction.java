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
package org.eclipse.jdt.internal.ui.refactoring.reorg;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.actions.CopyFilesAndFoldersOperation;
import org.eclipse.ui.actions.CopyProjectOperation;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.part.ResourceTransfer;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.reorg.JavaElementTransfer;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ParentChecker;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgUtils;

public class PasteAction extends SelectionDispatchAction{

	private final Clipboard fClipboard;

	public PasteAction(IWorkbenchSite site, Clipboard clipboard) {
		super(site);
		Assert.isNotNull(clipboard);
		fClipboard= clipboard;
		
		setText("&Paste");
		setDescription("Pastes elements from the clipboard");

		ISharedImages workbenchImages= JavaPlugin.getDefault().getWorkbench().getSharedImages();
		setDisabledImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_PASTE_DISABLED));
		setImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_PASTE));
		setHoverImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_PASTE_HOVER));

		update(getSelection());
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.PASTE_ACTION);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#selectionChanged(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void selectionChanged(IStructuredSelection selection) {
		try {
			setEnabled(canOperateOn(selection));
		} catch (JavaModelException e) {
			//no ui here - this happens on selection changes
			JavaPlugin.log(e);
			setEnabled(false);
		}
	}

	private boolean canOperateOn(IStructuredSelection selection) throws JavaModelException {
		TransferData[] availableDataTypes= fClipboard.getAvailableTypes();

		List elements= selection.toList();
		IResource[] resources= ReorgUtils.getResources(elements);
		IJavaElement[] javaElements= ReorgUtils.getJavaElements(elements);
		IPaster paster= createEnabledPaster(availableDataTypes);
		return paster != null && paster.canPasteOn(javaElements, resources);
	}
	
	private IPaster createEnabledPaster(TransferData[] availableDataTypes) throws JavaModelException {
		IPaster paster;
		
		paster= new ProjectPaster();
		if (paster.canEnable(availableDataTypes)) 
			return paster;
		
		paster= new JavaElementAndResourcePaster();
		if (paster.canEnable(availableDataTypes)) 
			return paster;

		paster= new FilePaster();
		if (paster.canEnable(availableDataTypes)) 
			return paster;
			
		return null;
	}

	private IResource[] getClipboardResources(TransferData[] availableDataTypes) {
		Transfer transfer= ResourceTransfer.getInstance();
		if (isAvailable(transfer, availableDataTypes)) {
			return (IResource[])getContents(fClipboard, transfer, getShell());
		}
		return null;
	}

	private IJavaElement[] getClipboardJavaElements(TransferData[] availableDataTypes) {
		Transfer transfer= JavaElementTransfer.getInstance();
		if (isAvailable(transfer, availableDataTypes)) {
			return (IJavaElement[])getContents(fClipboard, transfer, getShell());
		}
		return null;
	}
	
	private static Object getContents(final Clipboard clipboard, final Transfer transfer, Shell shell) {
		//see bug 33028 for explanation why we need this
		final Object[] result= new Object[1];
		shell.getDisplay().syncExec(new Runnable() {
			public void run() {
				result[0]= clipboard.getContents(transfer);
			}
		});
		return result[0];
	}
	
	private static boolean isAvailable(Transfer transfer, TransferData[] availableDataTypes) {
		for (int i= 0; i < availableDataTypes.length; i++) {
			if (transfer.isSupportedType(availableDataTypes[i])) return true;
		}
		return false;
	}

	public void run(IStructuredSelection selection) {
		try {
			TransferData[] availableTypes= fClipboard.getAvailableTypes();
			List elements= selection.toList();
			IResource[] resources= ReorgUtils.getResources(elements);
			IJavaElement[] javaElements= ReorgUtils.getJavaElements(elements);
			IPaster paster= createEnabledPaster(availableTypes);
			if (paster != null && paster.canPasteOn(javaElements, resources))
				paster.paste(javaElements, resources, availableTypes);
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, RefactoringMessages.getString("OpenRefactoringWizardAction.refactoring"), RefactoringMessages.getString("OpenRefactoringWizardAction.exception")); //$NON-NLS-1$ //$NON-NLS-2$
		} catch(InvocationTargetException e) {
			ExceptionHandler.handle(e, RefactoringMessages.getString("OpenRefactoringWizardAction.refactoring"), RefactoringMessages.getString("OpenRefactoringWizardAction.exception")); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (InterruptedException e) {
			//ok
		}
	}

	private interface IPaster{
		public abstract void paste(IJavaElement[] javaElements, IResource[] resources, TransferData[] availableTypes) throws JavaModelException, InterruptedException, InvocationTargetException;
		public abstract boolean canEnable(TransferData[] availableTypes)  throws JavaModelException;
		public abstract boolean canPasteOn(IJavaElement[] selectedJavaElements, IResource[] selectedResources)  throws JavaModelException;
	}
    
    private class ProjectPaster implements IPaster{
    	
    	public boolean canEnable(TransferData[] availableDataTypes) {
			boolean resourceTransfer= isAvailable(ResourceTransfer.getInstance(), availableDataTypes);
			boolean javaElementTransfer= isAvailable(JavaElementTransfer.getInstance(), availableDataTypes);
			if (! javaElementTransfer)
				return canPasteSimpleProjects(availableDataTypes);
			if (! resourceTransfer)
				return canPasteJavaProjects(availableDataTypes);
			return canPasteJavaProjects(availableDataTypes) && canPasteSimpleProjects(availableDataTypes);
    	}
    	
		public void paste(IJavaElement[] javaElements, IResource[] resources, TransferData[] availableTypes) {
			pasteProjects(availableTypes);
		}

		private void pasteProjects(TransferData[] availableTypes) {
			pasteProjects(getProjectsToPaste(availableTypes));
		}
		
		private void pasteProjects(IProject[] projects){
			Shell shell= getShell();
			for (int i = 0; i < projects.length; i++) {
				new CopyProjectOperation(shell).copyProject(projects[i]);
			}
		}
		private IProject[] getProjectsToPaste(TransferData[] availableTypes) {
			IResource[] resources= getClipboardResources(availableTypes);
			IJavaElement[] javaElements= getClipboardJavaElements(availableTypes);
			Set result= new HashSet();
			if (resources != null)
				result.addAll(Arrays.asList(resources));
			if (javaElements != null)
				result.addAll(Arrays.asList(ReorgUtils.getResources(javaElements)));
			Assert.isTrue(result.size() > 0);
			return (IProject[]) result.toArray(new IProject[result.size()]);
		}

		public boolean canPasteOn(IJavaElement[] javaElements, IResource[] resources) {
			return true;//ignores the selected element
		}
		
		private boolean canPasteJavaProjects(TransferData[] availableDataTypes) {
			IJavaElement[] javaElements= getClipboardJavaElements(availableDataTypes);
			return 	javaElements != null && 
					javaElements.length != 0 && 
					! ReorgUtils.hasElementsNotOfType(javaElements, IJavaElement.JAVA_PROJECT);
		}

		private boolean canPasteSimpleProjects(TransferData[] availableDataTypes) {
			IResource[] resources= getClipboardResources(availableDataTypes);
			if (resources == null || resources.length == 0) return false;
			for (int i= 0; i < resources.length; i++) {
				if (resources[i].getType() != IResource.PROJECT || ! ((IProject)resources[i]).isOpen())
					return false;
			}
			return true;
		}
    }
    
    private class FilePaster  implements IPaster{
		public void paste(IJavaElement[] javaElements, IResource[] resources, TransferData[] availableTypes) throws JavaModelException {
			String[] fileData= getClipboardFiles(availableTypes);
			if (fileData == null)
				return;
    		
			IContainer container= getAsContainer(getTarget(javaElements, resources));
			if (container == null)
				return;
				
			new CopyFilesAndFoldersOperation(getShell()).copyFiles(fileData, container);
		}
		
		private Object getTarget(IJavaElement[] javaElements, IResource[] resources) {
			if (javaElements.length + resources.length == 1){
				if (javaElements.length == 1)
					return javaElements[0];
				else
					return resources[0];
			} else				
				return getCommonParent(javaElements, resources);
		}

		public boolean canPasteOn(IJavaElement[] javaElements, IResource[] resources) throws JavaModelException {
			Object target= getTarget(javaElements, resources);
			return target != null && canPasteFilesOn(getAsContainer(target));
		}

		public boolean canEnable(TransferData[] availableDataTypes) throws JavaModelException {
			return isAvailable(FileTransfer.getInstance(), availableDataTypes);
		}
				
		private boolean canPasteFilesOn(Object target) throws JavaModelException {
			boolean isPackageFragment= target instanceof IPackageFragment;
			boolean isJavaProject= target instanceof IJavaProject;
			boolean isPackageFragmentRoot= target instanceof IPackageFragmentRoot;
			boolean isContainer= target instanceof IContainer;
		
			if (!(isPackageFragment || isJavaProject || isPackageFragmentRoot || isContainer)) 
				return false;

			if (isContainer) {
				return true;
			} else {
				IJavaElement element= (IJavaElement)target;
				return !element.isReadOnly();
			}
		}
		
		private IContainer getAsContainer(Object target) throws JavaModelException{
			if (target == null) 
				return null;
			if (target instanceof IContainer) 
				return (IContainer)target;
			if (target instanceof IFile)
				return ((IFile)target).getParent();
			return getAsContainer(((IJavaElement)target).getCorrespondingResource());
		}
		
		private String[] getClipboardFiles(TransferData[] availableDataTypes) {
			Transfer transfer= FileTransfer.getInstance();
			if (isAvailable(transfer, availableDataTypes)) {
				return (String[])getContents(fClipboard, transfer, getShell());
			}
			return null;
		}
		private Object getCommonParent(IJavaElement[] javaElements, IResource[] resources) {
			return new ParentChecker(resources, javaElements).getCommonParent();		
		}
    }
    private class JavaElementAndResourcePaster implements IPaster {

		private TransferData[] fAvailableTypes;

		public void paste(IJavaElement[] javaElements, IResource[] resources, TransferData[] availableTypes) throws JavaModelException, InterruptedException, InvocationTargetException{
			IResource[] clipboardResources= getClipboardResources(availableTypes);
			if (clipboardResources == null) 
				clipboardResources= new IResource[0];
			IJavaElement[] clipboardJavaElements= getClipboardJavaElements(availableTypes);
			if (clipboardJavaElements == null) 
				clipboardJavaElements= new IJavaElement[0];

			Object destination= getTarget(javaElements, resources);
			if (destination instanceof IJavaElement)
				ReorgCopyStarter.create(clipboardJavaElements, clipboardResources, (IJavaElement)destination).run(getShell());
			else if (destination instanceof IResource)
				ReorgCopyStarter.create(clipboardJavaElements, clipboardResources, (IResource)destination).run(getShell());
		}

		private Object getTarget(IJavaElement[] javaElements, IResource[] resources) {
			if (javaElements.length + resources.length == 1){
				if (javaElements.length == 1)
					return javaElements[0];
				else
					return resources[0];
			} else				
				return getCommonParent(javaElements, resources);
		}
		
		private Object getCommonParent(IJavaElement[] javaElements, IResource[] resources) {
			return new ParentChecker(resources, javaElements).getCommonParent();		
		}

		public boolean canPasteOn(IJavaElement[] javaElements, IResource[] resources) throws JavaModelException {
			IResource[] clipboardResources= getClipboardResources(fAvailableTypes);
			if (clipboardResources == null) 
				clipboardResources= new IResource[0];
			IJavaElement[] clipboardJavaElements= getClipboardJavaElements(fAvailableTypes);
			if (clipboardJavaElements == null) 
				clipboardJavaElements= new IJavaElement[0];
			Object destination= getTarget(javaElements, resources);
			if (destination instanceof IJavaElement)
				return ReorgCopyStarter.create(clipboardJavaElements, clipboardResources, (IJavaElement)destination) != null;
			if (destination instanceof IResource)
				return ReorgCopyStarter.create(clipboardJavaElements, clipboardResources, (IResource)destination) != null;
			return false;
		}
		
		public boolean canEnable(TransferData[] availableTypes) {
			fAvailableTypes= availableTypes;
			return isAvailable(JavaElementTransfer.getInstance(), availableTypes) || isAvailable(ResourceTransfer.getInstance(), availableTypes);
		}
    }
}
