/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.viewsupport;


import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;

import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.internal.ui.JavaUIMessages;

/**
 * Add the <code>StatusBarUpdater</code> to your ViewPart to have the statusbar
 * describing the selected elements.
 */
public class StatusBarUpdater implements ISelectionChangedListener {
	
	private final long LABEL_FLAGS= JavaElementLabels.DEFAULT_QUALIFIED | JavaElementLabels.ROOT_POST_QUALIFIED | JavaElementLabels.APPEND_ROOT_PATH |
			JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.M_PARAMETER_NAMES | JavaElementLabels.M_APP_RETURNTYPE | JavaElementLabels.M_EXCEPTIONS | 
		 	JavaElementLabels.F_APP_TYPE_SIGNATURE;
		 	
	private IStatusLineManager fStatusLineManager;
	
	public StatusBarUpdater(IStatusLineManager statusLineManager) {
		fStatusLineManager= statusLineManager;
	}
		
	/*
	 * @see ISelectionChangedListener#selectionChanged
	 */
	public void selectionChanged(SelectionChangedEvent event) {
		String statusBarMessage= formatMessage(event.getSelection());
		fStatusLineManager.setMessage(statusBarMessage);
	}
	
	
	protected String formatMessage(ISelection sel) {
		if (sel instanceof IStructuredSelection && !sel.isEmpty()) {
			IStructuredSelection selection= (IStructuredSelection) sel;
			
			int nElements= selection.size();
			if (nElements > 1) {
				return JavaUIMessages.getFormattedString("StatusBarUpdater.num_elements_selected", String.valueOf(nElements)); //$NON-NLS-1$
			} else { 
				Object elem= selection.getFirstElement();
				if (elem instanceof IJavaElement) {
					return formatJavaElementMessage((IJavaElement) elem);
				} else if (elem instanceof IResource) {
					return formatResourceMessage((IResource) elem);
				}
			}
		}
		return "";  //$NON-NLS-1$
	}
		
	private String formatJavaElementMessage(IJavaElement element) {
		return JavaElementLabels.getElementLabel(element, LABEL_FLAGS);
	}
		
	private String formatResourceMessage(IResource element) {
		IContainer parent= element.getParent();
		if (parent != null && parent.getType() != IResource.ROOT)
			return element.getName() + JavaElementLabels.CONCAT_STRING + parent.getFullPath().makeRelative().toString();
		else
			return element.getName();
	}	

}
