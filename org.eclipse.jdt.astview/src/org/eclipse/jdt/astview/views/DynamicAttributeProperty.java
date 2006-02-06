/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.astview.views;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.IBinding;

import org.eclipse.swt.graphics.Image;


public abstract class DynamicAttributeProperty extends ExceptionAttribute {

	protected static final String N_A= "N/A"; //$NON-NLS-1$
	private final Object fParent;
	
	private Object fViewerElement;
	private String fLabel= "<unknown>";
	
	public DynamicAttributeProperty(Object parentAttribute) {
		fParent= parentAttribute;
	}

	public Object getParent() {
		return fParent;
	}

	public Object[] getChildren() {
		return EMPTY;
	}
	
	public void setViewerElement(Object viewerAttribute) {
		if (fViewerElement == viewerAttribute)
			return;
		
		fViewerElement= viewerAttribute;
		fException= null;
		Object trayObject= unwrapAttribute(fParent);
		StringBuffer buf= new StringBuffer(getName());
		if (viewerAttribute != null) {
			Object viewerObject= unwrapAttribute(viewerAttribute);
			try {
				String queryResult= executeQuery(viewerObject, trayObject);
				buf.append(queryResult);
			} catch (RuntimeException e) {
				fException= e;
				buf.append(e.getClass().getName());
				buf.append(" for \""); //$NON-NLS-1$
				if (viewerObject == null)
					buf.append("null"); //$NON-NLS-1$
				else
					buf.append('"').append(objectToString(viewerObject));
				buf.append("\" and "); //$NON-NLS-1$
				buf.append(objectToString(trayObject)).append('"');
			}
		} else {
			buf.append(N_A);
		}
		fLabel= buf.toString();
	}

	//TODO: make complete
	private String objectToString(Object object) {
		if (object instanceof IBinding) {
			return ((IBinding) object).getKey();
		} else {
			return String.valueOf(object);
		}
	}

	public static Object unwrapAttribute(Object attribute) {
		if (attribute instanceof Binding) {
			return ((Binding) attribute).getBinding();
		} else if (attribute instanceof JavaElement) {
			return ((JavaElement) attribute).getJavaElement();
		} else if (attribute instanceof ASTNode) {
			return attribute;
		} else if (attribute instanceof ResolvedAnnotation) {
			return ((ResolvedAnnotation) attribute).getAnnotation();
		} else if (attribute instanceof ResolvedMemberValuePair) {
			return ((ResolvedMemberValuePair) attribute).getPair();
		} else {
			return null;
		}
	}
	
	/**
	 * Executes this dynamic attribute property's query in a protected environment.
	 * A {@link RuntimeException} thrown by this method is made available via
	 * {@link #getException()}. 
	 * 
	 * @param viewerObject the object of the element selected in the AST viewer, or <code>null</code> iff none
	 * @param trayObject the object of the element selected in the comparison tray, or <code>null</code> iff none
	 * @return this property's result
	 */
	protected abstract String executeQuery(Object viewerObject, Object trayObject);

	/**
	 * @return a description of the dynamic property
	 */
	protected abstract String getName();

	public String getLabel() {
		return fLabel;
	}

	public Image getImage() {
		return null;
	}
}
