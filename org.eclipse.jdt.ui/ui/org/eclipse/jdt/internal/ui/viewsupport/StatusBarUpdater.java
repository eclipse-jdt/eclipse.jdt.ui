package org.eclipse.jdt.internal.ui.viewsupport;

/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */


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


public class StatusBarUpdater implements ISelectionChangedListener {
	
	private final static String N_ELEMENTS_SELECTED= "StatusBarUpdater.num_elements_selected";
	
	private JavaTextLabelProvider fJavaTextLabelProvider;
	private IStatusLineManager fStatusLineManager;
	
	public StatusBarUpdater(IStatusLineManager statusLineManager) {
		fStatusLineManager= statusLineManager;
		
		int options= JavaElementLabelProvider.SHOW_CONTAINER_QUALIFICATION
		 +  JavaElementLabelProvider.SHOW_CONTAINER
		 +  JavaElementLabelProvider.SHOW_ROOT
		 +  JavaElementLabelProvider.SHOW_PARAMETERS;
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
			return "";
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
				return JavaPlugin.getFormattedString(N_ELEMENTS_SELECTED, Integer.toString(nElement));
			} else { 
				if (element instanceof IJavaElement) {
					return fJavaTextLabelProvider.getTextLabel((IJavaElement)element);
				} else if (element instanceof IResource) {
					IResource resource= (IResource)element;
					StringBuffer buf= new StringBuffer();
					buf.append(resource.getName());
					if (resource.getType() != IResource.PROJECT) {
						if (resource.getParent() != null) {
							buf.append(" - ");
							buf.append(resource.getParent().getFullPath());
						}
					}			
					return buf.toString();
				}
			}
		}
		return "";
	}
}