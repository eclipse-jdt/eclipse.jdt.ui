/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.actions;

import java.util.Iterator;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import org.eclipse.ui.PartInitException;

import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.JavaUIMessages;

/**
 * Open a source reference in an editor
 */
 
public class OpenSourceReferenceAction extends OpenJavaElementAction implements ISelectionChangedListener{
	
	
	protected ISelectionProvider fSelectionProvider;
	
	
	public OpenSourceReferenceAction() {
		super(JavaUIMessages.getString("OpenSourceReferenceAction.label")); //$NON-NLS-1$
		setDescription(JavaUIMessages.getString("OpenSourceReferenceAction.description")); //$NON-NLS-1$
		setToolTipText(JavaUIMessages.getString("OpenSourceReferenceAction.tooltip")); //$NON-NLS-1$
	}
	
	public void run() {
		ISelection selection= fSelectionProvider.getSelection();
		if ( !(selection instanceof IStructuredSelection) || selection.isEmpty())
			return;
			
		Object o= ((IStructuredSelection) selection).getFirstElement();
		if (o instanceof ISourceReference) {
			try {
				open((ISourceReference) o);
			} catch (JavaModelException x) {
				ExceptionHandler.handle(x, JavaUIMessages.getString("OpenSourceReferenceAction.errorTitle"), JavaUIMessages.getString("OpenSourceReferenceAction.errorMessage")); //$NON-NLS-2$ //$NON-NLS-1$
			} catch (PartInitException x) {
			}
		}
	}
		
	public void selectionChanged(SelectionChangedEvent event) {
		
		if (fSelectionProvider.equals(event.getSelectionProvider())) {
			ISelection selection= fSelectionProvider.getSelection();
			if ( !(selection instanceof IStructuredSelection) || selection.isEmpty()) {
				setEnabled(false);
				return;
			}
			
			Iterator iter= ((IStructuredSelection) selection).iterator();
			while (iter.hasNext()) {
				Object o= iter.next();
				if (o instanceof ISourceReference) {
					setEnabled(true);
				} else {
					setEnabled(false); 
					return;
				}
			}
		}
	}
	
	public void setSelectionProvider(ISelectionProvider provider) {
		if (fSelectionProvider != null)
			fSelectionProvider.removeSelectionChangedListener(this);
			
		fSelectionProvider= provider;
		
		if (fSelectionProvider != null)
			fSelectionProvider.addSelectionChangedListener(this);
	}
}