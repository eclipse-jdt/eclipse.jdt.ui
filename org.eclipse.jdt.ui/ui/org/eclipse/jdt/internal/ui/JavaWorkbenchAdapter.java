/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui;


import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.ui.model.IWorkbenchAdapter;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.JavaModelException;

/**
 * An imlementation of the IWorkbenchAdapter for IJavaElements.
 */
  
public class JavaWorkbenchAdapter implements IWorkbenchAdapter {
	protected static final Object[] NO_CHILDREN= new Object[0];

	public Object[] getChildren(Object o) {
		if (o instanceof IParent) {
			try {
				return ((IParent)o).getChildren();
			} catch(JavaModelException e) {
				JavaPlugin.logErrorMessage(getClass().getName() + ": Error getting children for: " + o); //$NON-NLS-1$
			}
		}
		return NO_CHILDREN;
	}

	public ImageDescriptor getImageDescriptor(Object object) {
		return null;
	}

	public String getLabel(Object o) {
		if (o instanceof IJavaElement)
			return ((IJavaElement)o).getElementName();
		return null;
	}

	public Object getParent(Object o) {
		if (o instanceof IJavaElement)
			return ((IJavaElement)o).getParent();
		return null;
	}
}