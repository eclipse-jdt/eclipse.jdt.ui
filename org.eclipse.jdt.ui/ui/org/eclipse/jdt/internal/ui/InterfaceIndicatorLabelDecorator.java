/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.ListenerList;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;

import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.TypeNameRequestor;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

public class InterfaceIndicatorLabelDecorator implements ILabelDecorator, ILightweightLabelDecorator {
	
	private class IntefaceIndicatorChangeListener implements IElementChangedListener {

		/**
		 * {@inheritDoc}
		 */
		public void elementChanged(ElementChangedEvent event) {
			List changed= new ArrayList();
			processDelta(event.getDelta(), changed);
			if (changed.size() == 0)
				return;
			
			fireChange((IJavaElement[])changed.toArray(new IJavaElement[changed.size()]));
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
			ImageDescriptor overlay= getOverlay(element);
			if (overlay == null)
				return;
			
			decoration.addOverlay(overlay, IDecoration.TOP_RIGHT);
		} catch (JavaModelException e) {
			return;
		}
	}
	
	private ImageDescriptor getOverlay(Object element) throws JavaModelException {
		if (element instanceof ICompilationUnit) {
			ICompilationUnit unit= (ICompilationUnit) element;
			if (unit.isOpen()) {
				IType mainType= unit.findPrimaryType();
				if (mainType != null) {
					return getOverlayFromFlags(mainType.getFlags());
				}
			}
			return getOverlayWithSearchEngine(unit);
		} else if (element instanceof IClassFile) {
			IClassFile classFile= (IClassFile) element;
			if (classFile.isOpen()) {
				return getOverlayFromFlags(classFile.getType().getFlags());
			}
			return getOverlayWithSearchEngine(classFile);
		}
		return null;
	}
	
	private ImageDescriptor getOverlayWithSearchEngine(IJavaElement element) {
		SearchEngine engine= new SearchEngine();
		IJavaSearchScope scope= SearchEngine.createJavaSearchScope(new IJavaElement[] { element });
		
		class Result extends RuntimeException {
			private static final long serialVersionUID= 1L;
			int modifiers;
			public Result(int modifiers) {
				this.modifiers= modifiers;
			}
		}
		
		TypeNameRequestor requestor= new TypeNameRequestor() {
			public void acceptType(int modifiers, char[] packageName, char[] simpleTypeName, char[][] enclosingTypeNames, String path) {
				if (enclosingTypeNames.length == 0 && Flags.isPublic(modifiers)) {
					throw new Result(modifiers);
				}
			}
		};
		
		try {
			engine.searchAllTypeNames(null, null, SearchPattern.R_EXACT_MATCH, IJavaSearchConstants.TYPE, scope, requestor, IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH , null);
		} catch (Result e) {
			return getOverlayFromFlags(e.modifiers);
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
		return null;

	}

	private ImageDescriptor getOverlayFromFlags(int flags)  {
		if (Flags.isAnnotation(flags)) {
			return JavaPluginImages.DESC_OVR_ANNOTATION;
		} else if (Flags.isEnum(flags)) {
			return JavaPluginImages.DESC_OVR_ENUM;
		} else if (Flags.isInterface(flags)) {
			return JavaPluginImages.DESC_OVR_INTERFACE;
		} else if (/* is class */ Flags.isAbstract(flags)) {
			return JavaPluginImages.DESC_OVR_ABSTRACT_CLASS;
		}
		return null;
	}

	private void fireChange(IJavaElement[] elements) {
		if (fListeners != null && !fListeners.isEmpty()) {
			LabelProviderChangedEvent event= new LabelProviderChangedEvent(this, elements);
			Object[] listeners= fListeners.getListeners();
			for (int i= 0; i < listeners.length; i++) {
				((ILabelProviderListener) listeners[i]).labelProviderChanged(event);
			}
		}
	}
	
	private void processDelta(IJavaElementDelta delta, List result) {
		IJavaElement elem= delta.getElement();
		
		boolean isChanged= delta.getKind() == IJavaElementDelta.CHANGED;
		boolean isRemoved= delta.getKind() == IJavaElementDelta.REMOVED;
		int flags= delta.getFlags();
		
		switch (elem.getElementType()) {
			case IJavaElement.JAVA_PROJECT:
				if (isRemoved || (isChanged && 
						(flags & IJavaElementDelta.F_CLOSED) != 0)) {
					return;
				}
				processChildrenDelta(delta, result);
				return;
			case IJavaElement.PACKAGE_FRAGMENT_ROOT:
				if (isRemoved || (isChanged && (
						(flags & IJavaElementDelta.F_ARCHIVE_CONTENT_CHANGED) != 0 ||
						(flags & IJavaElementDelta.F_REMOVED_FROM_CLASSPATH) != 0))) {
					return;
				}
				processChildrenDelta(delta, result);
				return;
			case IJavaElement.PACKAGE_FRAGMENT:
				if (isRemoved)
					return;
				processChildrenDelta(delta, result);
				return;
			case IJavaElement.TYPE:
			case IJavaElement.CLASS_FILE:
				return;
			case IJavaElement.JAVA_MODEL:
				processChildrenDelta(delta, result);
				return;
			case IJavaElement.COMPILATION_UNIT:
				// Not the primary compilation unit. Ignore it 
				if (!JavaModelUtil.isPrimary((ICompilationUnit) elem)) {
					return;
				}

				if (isChanged &&  ((flags & IJavaElementDelta.F_CONTENT) != 0 || (flags & IJavaElementDelta.F_FINE_GRAINED) != 0)) {
					if (delta.getAffectedChildren().length == 0)
						return;
					
					result.add(elem);
				}
				return;
			default:
				// fields, methods, imports ect
				return;
		}	
	}
	
	private boolean processChildrenDelta(IJavaElementDelta delta, List result) {
		IJavaElementDelta[] children= delta.getAffectedChildren();
		for (int i= 0; i < children.length; i++) {
			processDelta(children[i], result);
		}
		return false;
	}

}
