/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.actions;

import java.util.Iterator;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * Open a java element in an editor
 * 
 * @deprecated Use org.eclipse.jdt.ui.actions.OpenAction instead
 */
public class OpenJavaElementAction extends AbstractOpenJavaElementAction {
	
	private ISelectionProvider fSelectionProvider;
	
	public OpenJavaElementAction(ISelectionProvider provider) {
		super(JavaUIMessages.getString("OpenJavaElementAction.label")); //$NON-NLS-1$
		setDescription(JavaUIMessages.getString("OpenJavaElementAction.description")); //$NON-NLS-1$
		setToolTipText(JavaUIMessages.getString("OpenJavaElementAction.tooltip")); //$NON-NLS-1$
		fSelectionProvider= provider;
	}
	
	public void run() {
		ISelection selection= fSelectionProvider.getSelection();
		if ( !(selection instanceof IStructuredSelection) || selection.isEmpty())
			return;
			
		try {
			Iterator iter= ((IStructuredSelection) selection).iterator();
			while (iter.hasNext()) {
				Object current= iter.next();
				if (current instanceof IJavaElement)
					open((IJavaElement) current);
			}
		} catch (JavaModelException x) {
			String title= JavaUIMessages.getString("OpenJavaElementAction.errorTitle"); //$NON-NLS-1$
			String message= JavaUIMessages.getString("OpenJavaElementAction.errorMessage"); //$NON-NLS-1$
			ExceptionHandler.handle(x, title, message);
		} catch (PartInitException x) {
			String title= JavaUIMessages.getString("OpenJavaElementAction.errorTitle"); //$NON-NLS-1$
			String message= JavaUIMessages.getString("OpenJavaElementAction.errorMessage"); //$NON-NLS-1$
			MessageDialog.openError(JavaPlugin.getActiveWorkbenchShell(), title, message);
			JavaPlugin.log(x);
		}
	}
	
	public boolean canActionBeAdded() {
		ISelection selection= fSelectionProvider.getSelection();
		return (selection instanceof IStructuredSelection) && !selection.isEmpty();
	}		
	
}