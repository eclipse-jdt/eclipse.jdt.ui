/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.viewsupport;


import java.util.Iterator;

import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;


public class StatusBarUpdater implements ISelectionChangedListener {
	
	private JavaTextLabelProvider fJavaTextLabelProvider;
	private IStatusLineManager fStatusLineManager;
	
	public StatusBarUpdater(IStatusLineManager statusLineManager) {
		fStatusLineManager= statusLineManager;
		
		int options= JavaElementLabelProvider.SHOW_CONTAINER_QUALIFICATION
		 +  JavaElementLabelProvider.SHOW_CONTAINER
		 +  JavaElementLabelProvider.SHOW_ROOT
		 +  JavaElementLabelProvider.SHOW_PARAMETERS
		 +  JavaElementLabelProvider.SHOW_RETURN_TYPE;
		fJavaTextLabelProvider= new JavaTextLabelProvider(options);
	}
		
	/**
	 * @see ISelectionChangedListener#selectionChanged
	 */
	public void selectionChanged(SelectionChangedEvent event) {
		String statusBarMessage= formatMessage(event.getSelection());
		fStatusLineManager.setMessage(statusBarMessage);
	}
	
	
	private String formatMessage(ISelection sel) {
		if (sel ==  null || sel.isEmpty()) {
			return ""; //$NON-NLS-1$
		}
		if (sel instanceof IStructuredSelection) {			
			Iterator iter= ((IStructuredSelection)sel).iterator();
			Object element= iter.next();
			if (iter.hasNext()) {
				int nElement= 1;
				do {
					nElement++;
					iter.next();
				} while (iter.hasNext());
				return JavaUIMessages.getFormattedString("StatusBarUpdater.num_elements_selected", Integer.toString(nElement)); //$NON-NLS-1$
			} else { 
				if (element instanceof IJavaElement) {
					return fJavaTextLabelProvider.getTextLabel((IJavaElement)element);
				} else if (element instanceof IResource) {
					IResource resource= (IResource)element;
					StringBuffer buf= new StringBuffer();
					buf.append(resource.getName());
					if (resource.getType() != IResource.PROJECT) {
						if (resource.getParent() != null) {
							buf.append(" - "); //$NON-NLS-1$
							buf.append(resource.getParent().getFullPath());
						}
					}			
					return buf.toString();
				}
			}
		}
		return ""; //$NON-NLS-1$
	}
}