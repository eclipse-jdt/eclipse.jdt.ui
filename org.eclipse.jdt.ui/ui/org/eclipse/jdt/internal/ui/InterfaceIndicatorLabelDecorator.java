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
package org.eclipse.jdt.internal.ui;

import org.eclipse.core.runtime.ListenerList;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;

import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;

public class InterfaceIndicatorLabelDecorator implements ILabelDecorator, ILightweightLabelDecorator {
	
	private class IntefaceIndicatorChangeListener implements IElementChangedListener {

		/**
		 * {@inheritDoc}
		 */
		public void elementChanged(ElementChangedEvent event) {
			handleDelta(event.getDelta());
		}
		
	}
	
	private ListenerList fListeners;
	private IElementChangedListener fChangeListener;

	public InterfaceIndicatorLabelDecorator() {
	}

	/**
	 * {@inheritDoc}
	 */
	public Image decorateImage(Image image, Object element) {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public String decorateText(String text, Object element) {
		return text;
	}

	/**
	 * {@inheritDoc}
	 */
	public void addListener(ILabelProviderListener listener) {
		if (fChangeListener == null) {
			fChangeListener= new IntefaceIndicatorChangeListener();
			JavaCore.addElementChangedListener(fChangeListener);
		}
		
		if (fListeners == null) {
			fListeners= new ListenerList();
		}
		
		fListeners.add(listener);
	}

	/**
	 * {@inheritDoc}
	 */
	public void dispose() {
		if (fChangeListener != null) {
			JavaCore.removeElementChangedListener(fChangeListener);
			fChangeListener= null;
		}
		if (fListeners != null) {
			Object[] listeners= fListeners.getListeners();
			for (int i= 0; i < listeners.length; i++) {
				fListeners.remove(listeners[i]);
			}
			fListeners= null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeListener(ILabelProviderListener listener) {
		if (fListeners == null)
			return;
		
		fListeners.remove(listener);
		
		if (fListeners.isEmpty() && fChangeListener != null) {
			JavaCore.removeElementChangedListener(fChangeListener);
			fChangeListener= null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void decorate(Object element, IDecoration decoration) {
		try {
			IType type= getMainType(element);
	
			if (type == null)
				return;
			
			ImageDescriptor overlay= getOverlay(type);
			if (overlay == null)
				return;
			
			decoration.addOverlay(overlay, IDecoration.TOP_LEFT);
		} catch (JavaModelException e) {
			return;
		}
	}
	
	private IType getMainType(Object element) throws JavaModelException {
		if (element instanceof ICompilationUnit)
			return JavaElementUtil.getMainType((ICompilationUnit)element);
		
		if (element instanceof IClassFile)
			return ((IClassFile)element).getType();
		
		return null;
	}

	private ImageDescriptor getOverlay(IType type) throws JavaModelException {
		if (type.isAnnotation()) {
			return JavaPluginImages.DESC_OVR_ANNOTATION;
		} else if (type.isInterface()) {
			return JavaPluginImages.DESC_OVR_INTERFACE;
		} else if (type.isEnum()) {
			return JavaPluginImages.DESC_OVR_ENUM;
		}
		return null;
	}

	private void handleDelta(IJavaElementDelta delta) {
		IJavaElement element= delta.getElement();
		if (!(element instanceof ICompilationUnit))
			return;
		
		if (delta.getKind() != IJavaElementDelta.CHANGED)
			return;
		
		if (fListeners != null && !fListeners.isEmpty()) {
			LabelProviderChangedEvent event1= new LabelProviderChangedEvent(this, element);
			Object[] listeners= fListeners.getListeners();
			for (int i= 0; i < listeners.length; i++) {
				((ILabelProviderListener) listeners[i]).labelProviderChanged(event1);
			}
		}
		
	}

}
