/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
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
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIException;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.JdtHackFinder;

/**
  * Action for renaming file and folder resource elements in the resource explorer.
  */
public class RenameAction extends ReorgAction {
	private final static String PREFIX= "action.rename.";
	private final static String ACTION_NAME= PREFIX+"name";
	private final static String LABEL= PREFIX+"label";
	private final static String DESCRIPTION= PREFIX+"description";
	private final static String INPUT_PREFIX= PREFIX+"new_name.";
	private final static String ERROR_PREFIX= PREFIX+"error.";
	
	/**
	 *Create an instance of this class
	 *
	 *@param viewer org.eclipse.jface.viewer.ContentViewer
	 */
	public RenameAction(ISelectionProvider viewer) {
		super(viewer, JavaPlugin.getResourceString(LABEL));
		setDescription(JavaPlugin.getResourceString(DESCRIPTION));
	}

	/**
	 *The user has invoked this action
	 *
	 *@param context org.eclipse.jface.parts.Window
	 */
	public void doActionPerformed() {
		Iterator elements= getStructuredSelection().iterator();
		if (elements.hasNext()) 
			renameElement(elements.next());
	}
	
	protected boolean confirmIfUnsaved(Object element) {
		ArrayList unsavedEditors= new ArrayList();
		ArrayList unsavedElements= new ArrayList();
		
		ArrayList elements= new ArrayList();
		elements.add(element);
		collectUnsavedEditors(elements, unsavedEditors, unsavedElements);
		if (unsavedEditors.size() == 0)
			return true;
		return MessageDialog.openConfirm(JavaPlugin.getActiveWorkbenchShell(), getActionName(), getConfirmDiscardChangesMessage());
	}
	
	protected void renameElement(final Object element) {
		if (!confirmIfUnsaved(element))
			return;
			
		String title= JavaPlugin.getResourceString(INPUT_PREFIX+"title");
		String message= JavaPlugin.getResourceString(INPUT_PREFIX+"message");
		final INamingPolicy policy= ReorgSupportFactory.createNamingPolicy(element);
		IInputValidator validator= new IInputValidator() {
			int k= 0;
			public String isValid(String name) {
				IJavaElement parent= getJavaParen(element);
				return policy.isValidNewName(element, parent, name);
			}
		};
		Image img= Dialog.getImage(Dialog.DLG_IMG_QUESTION);
		Shell activeShell= JavaPlugin.getActiveWorkbenchShell();
		InputDialog dialog= new InputDialog(activeShell, title, message, getElementName(element), validator);
		if (dialog.open() != dialog.OK)
			return;
			
		String id= JavaPlugin.getDefault().getDescriptor().getUniqueIdentifier();
		String statusString= JavaPlugin.getResourceString(ERROR_PREFIX+"status");
		final MultiStatus status= new MultiStatus(id, IStatus.OK, statusString, null);
		final String newName= dialog.getValue();
		final List renamedElements= new ArrayList();
		if (newName != null && !newName.equals("")) {
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
				ResourceBundle bundle= JavaPlugin.getResourceBundle();
				ExceptionHandler.handle(t, activeShell, bundle, ERROR_PREFIX);
			} else {
				select(activeShell, renamedElements);
			}
		}
	}
	
	protected boolean canExecute(IStructuredSelection sel) {
		Iterator iter= sel.iterator();
		if (iter.hasNext()) {
			Object o= iter.next();
			IRenameSupport support= ReorgSupportFactory.createRenameSupport(o);
			if (!support.canRename(o))
				return false;
			return !iter.hasNext();
		} else {
			return false;
		}
	}
	
	private IJavaElement getJavaParen(Object element) {
		if (element instanceof IJavaElement) 
			return ((IJavaElement)element).getParent();
		if (element instanceof IResource) {
			IResource file= (IResource)element;
			return JavaCore.create(file.getParent());
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
		return JavaPlugin.getResourceString(ACTION_NAME);
	}
	protected String getConfirmDiscardChangesMessage() {
		JdtHackFinder.fixme("NLS");
		return "The selected element has unsaved changes that will be discarded if you proceed";
	}
}
