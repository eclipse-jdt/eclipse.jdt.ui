/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.reorg;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;

import org.eclipse.ui.actions.WorkspaceModifyOperation;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragmentRoot;import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIException;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
  * Action for renaming file and folder resource elements in the resource explorer.
  */
public class RenameAction extends ReorgAction {
	
	private IRefactoringRenameSupport fRefactoringSupport;
	
	/**
	 *Create an instance of this class
	 *
	 *@param viewer org.eclipse.jface.viewer.ContentViewer
	 */
	public RenameAction(ISelectionProvider viewer) {
		super(viewer, ReorgMessages.getString("renameAction.label")); //$NON-NLS-1$
		setDescription(ReorgMessages.getString("renameAction.description")); //$NON-NLS-1$
	}

	public void update() {
		setEnabled(canExecute((IStructuredSelection)getSelectionProvider().getSelection()));
	}
	
	/**
	 *The user has invoked this action
	 *
	 *@param context org.eclipse.jface.parts.Window
	 */
	public void doActionPerformed() {
		Object element= getStructuredSelection().getFirstElement();
		if (fRefactoringSupport != null) {
			fRefactoringSupport.rename(element);
			fRefactoringSupport= null;
			return;
		}	
		if (isReadOnlyResource(element)) {
			if (!confirmReadOnly(ReorgMessages.getString("renameAction.checkRename.title"),  //$NON-NLS-1$
					ReorgMessages.getString("renameAction.checkRename.message"))) //$NON-NLS-1$
					return;
		}
		renameElement(element);
	}
	

	protected void renameElement(final Object element) {
		String title= ReorgMessages.getString("renameAction.rename.title"); //$NON-NLS-1$
		String message= ReorgMessages.getString("renameAction.newName"); //$NON-NLS-1$
		final INamingPolicy policy= ReorgSupportFactory.createNamingPolicy(element);
		IInputValidator validator= new IInputValidator() {
			int k= 0;
			public String isValid(String name) {
				Object parent= getJavaParent(element);
				return policy.isValidNewName(element, parent, name);
			}
		};
		Image img= Dialog.getImage(Dialog.DLG_IMG_QUESTION);
		Shell activeShell= JavaPlugin.getActiveWorkbenchShell();
		InputDialog dialog= new InputDialog(activeShell, title, message, getElementName(element), validator);
		if (dialog.open() != dialog.OK)
			return;
			
		String id= JavaPlugin.getDefault().getDescriptor().getUniqueIdentifier();
		String statusString= ReorgMessages.getString("renameAction.status.exceptions"); //$NON-NLS-1$
		final MultiStatus status= new MultiStatus(id, IStatus.OK, statusString, null);
		final String newName= dialog.getValue();
		final List renamedElements= new ArrayList();
		if (newName != null && !newName.equals("")) { //$NON-NLS-1$
			WorkspaceModifyOperation op= new WorkspaceModifyOperation() {
				public void execute(IProgressMonitor pm) {
					IRenameSupport support= ReorgSupportFactory.createRenameSupport(element);
					try {
						Object renamedElement= support.rename(element, newName, pm);
						if (renamedElement != null)
							renamedElements.add(renamedElement);
					} catch (CoreException e) {
						status.merge(e.getStatus());
					}
				}
			};
			try {
				new ProgressMonitorDialog(activeShell).run(true, true, op);
			} catch (InvocationTargetException e) {
				// this will never happen
			} catch (InterruptedException e) {
				return;
			}
			if (!status.isOK()) {
				Throwable t= new JavaUIException(status);
				ExceptionHandler.handle(t, activeShell, ReorgMessages.getString("renameAction.error.title"), ReorgMessages.getString("renameAction.error.message")); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				select(activeShell, renamedElements);
			}
		}
	}
	
	protected boolean canExecute(IStructuredSelection selection) {
		if (selection.size() != 1)
			return false;
		
		Object element= selection.getFirstElement();
		fRefactoringSupport= RefactoringSupportFactory.createRenameSupport(element);
		if (fRefactoringSupport != null) {
			return fRefactoringSupport.canRename(element);
		}
		
		IRenameSupport support= ReorgSupportFactory.createRenameSupport(element);
		return support.canRename(element);			
	}
	
	private Object getJavaParent(Object element) {
		if (element instanceof IJavaElement) 
			return ((IJavaElement)element).getParent();
		if (element instanceof IResource) {
			IResource file= (IResource)element;
			IResource parent= file.getParent();
			IJavaElement javaParent= JavaCore.create(parent);
			if (javaParent != null)
				return javaParent;
			return parent;
		}
		return null;
	}
	
	protected String getElementName(Object o) {
		if (o instanceof IJavaElement)
			return ((IJavaElement)o).getElementName();
		if (o instanceof IResource) {
			return ((IResource)o).getName();
		}
		return o.toString();
	}
	
	protected String getActionName() {
		return ReorgMessages.getString("renameAction.name"); //$NON-NLS-1$
	}
	
	protected String getConfirmDiscardChangesMessage() {
		// FIXME NLS");
		return ReorgMessages.getString("renameAction.confirmDiscard"); //$NON-NLS-1$
	}
	
	protected boolean isReadOnlyResource(Object element) {
		if (element instanceof IJavaElement) {
			try {
				if ((element instanceof IPackageFragmentRoot) && ReorgSupport.isClasspathDelete((IPackageFragmentRoot)element)) {
					return false;
				}
				element= ((IJavaElement)element).getCorrespondingResource();
			} catch (JavaModelException e) {
				// we catch this, we're only interested in knowing
				// whether to pop up the read-only dialog.
			}
		}
		
		if (element instanceof IResource) {
			return ((IResource)element).isReadOnly();
		}
		return false;
	}

}
