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

import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * Open a source reference in an editor
 */
public class OpenSourceReferenceAction extends Action {
	
	private ISelectionProvider fSelectionProvider;
	
	public OpenSourceReferenceAction(ISelectionProvider provider) {
		super(JavaUIMessages.getString("OpenSourceReferenceAction.label")); //$NON-NLS-1$
		setDescription(JavaUIMessages.getString("OpenSourceReferenceAction.description")); //$NON-NLS-1$
		setToolTipText(JavaUIMessages.getString("OpenSourceReferenceAction.tooltip")); //$NON-NLS-1$
	
		fSelectionProvider= provider;
	}
	
	public void run() {
		ISelection selection= fSelectionProvider.getSelection();
		if ( !(selection instanceof IStructuredSelection) || selection.isEmpty())
			return;
			
		try {
			Iterator iter= ((IStructuredSelection) selection).iterator();
			while (iter.hasNext()) {
				Object curr= iter.next();
				if (curr instanceof ISourceReference) {
					ISourceReference ref= (ISourceReference)curr;
					IEditorPart part= EditorUtility.openInEditor(ref);
					EditorUtility.revealInEditor(part, ref);
				}
			}
		} catch (JavaModelException x) {
			String title= JavaUIMessages.getString("OpenSourceReferenceAction.errorTitle"); //$NON-NLS-1$
			String message= JavaUIMessages.getString("OpenSourceReferenceAction.errorMessage"); //$NON-NLS-1$
			ExceptionHandler.handle(x, title, message);
		} catch (PartInitException x) {
			String title= JavaUIMessages.getString("OpenSourceReferenceAction.errorTitle"); //$NON-NLS-1$
			String message= JavaUIMessages.getString("OpenSourceReferenceAction.errorMessage"); //$NON-NLS-1$
			MessageDialog.openError(JavaPlugin.getActiveWorkbenchShell(), title, message);
			JavaPlugin.log(x);
		}
	}
}