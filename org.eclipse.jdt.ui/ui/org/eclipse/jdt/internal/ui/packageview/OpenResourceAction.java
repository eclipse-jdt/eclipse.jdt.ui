/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.packageview;

import java.util.Iterator;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.SelectionProviderAction;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaStatusConstants;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;

 
/**
 * Open a resource (ClassFile, CompilationUnit, or ordinary resource) 
 * from the PackageViewer
 */
public class OpenResourceAction extends SelectionProviderAction implements ISelectionChangedListener {	
	
	public OpenResourceAction(ISelectionProvider provider) {
		super(provider, PackagesMessages.getString("OpenResource.action.label")); //$NON-NLS-1$
		setDescription(PackagesMessages.getString("OpenResource.action.description")); //$NON-NLS-1$
	}
	
	public void run() {
		Iterator iter= getStructuredSelection().iterator();
		Shell shell= JavaPlugin.getActiveWorkbenchShell();
		
		while (iter.hasNext()) {
			
			Object element= iter.next();
			
			try {				
				EditorUtility.openInEditor(element);			
			} catch (JavaModelException x) {
				
				JavaPlugin.log(new Status(IStatus.ERROR, JavaPlugin.getPluginId(),
					JavaStatusConstants.INTERNAL_ERROR, PackagesMessages.getString("OpenResource.error.message"), x)); //$NON-NLS-1$
				
				ErrorDialog.openError(shell, 
					PackagesMessages.getString("OpenResource.error.title"), //$NON-NLS-1$
					PackagesMessages.getString("OpenResource.error.messageProblems"),  //$NON-NLS-1$
					x.getStatus());
			
			} catch (PartInitException x) {
								
				String name= null;
				
				if (element instanceof IClassFile) {
					name= ((IClassFile) element).getElementName();
				} else if (element instanceof ICompilationUnit) {
					name= ((ICompilationUnit) element).getElementName();
				} else if (element instanceof IResource) {
					name= ((IResource) element).getName();
				}
				
				if (name != null) {
					MessageDialog.openError(JavaPlugin.getActiveWorkbenchShell(),
						PackagesMessages.getString("OpenResource.error.messageProblems"),  //$NON-NLS-1$
						PackagesMessages.getFormattedString("OpenResource.error.messageArgs",  //$NON-NLS-1$
							new String[] { name, x.getMessage() } ));			
				}
			}
		}
	}
	
	/**
	 * Set self's enablement based upon the currently-selected Resource elements
	 */
	public void selectionChanged(IStructuredSelection selection) {
		if (selection == null || selection.isEmpty()) {
			setEnabled(false);
			return;
		}		
		Iterator resources= selection.iterator();
		if (resources == null) {
			setEnabled(false);
			return;
		}
		while (resources.hasNext()) {
			Object element= resources.next();
			if (canOpen(element)) {
				setEnabled(true);
			} else {
				setEnabled(false); 
				return;
			}
		}
	}
	
	public void update() {
		selectionChanged(getStructuredSelection());
	}
	
	boolean canOpen(Object o) {
		try {
			return (EditorUtility.getEditorInput(o) != null);
		} catch (JavaModelException x) {
		}
		return false;
	}
}