package org.eclipse.jdt.internal.ui;/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */


import org.eclipse.core.runtime.IAdapterFactory;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.views.properties.IPropertySource;

import org.eclipse.search.ui.ISearchPageScoreComputer;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.internal.ui.search.JavaSearchPageScoreComputer;

/**
 * Implements basic UI support for Java elements.
 * Implements handle to persistent support for Java elements.
 */
public class EditorInputAdapterFactory implements IAdapterFactory {
	
	private static Class[] PROPERTIES= new Class[] {
		ISearchPageScoreComputer.class,
	};
	
	private ISearchPageScoreComputer fSearchPageScoreComputer= new JavaSearchPageScoreComputer();

	public Class[] getAdapterList() {
		return PROPERTIES;
	}
	
	public Object getAdapter(Object element, Class key) {
		
		IEditorInput java= (IEditorInput) element;
		
		if (ISearchPageScoreComputer.class.equals(key)) {
			return fSearchPageScoreComputer;
		}
		
		return null;
	}
	
	private IPropertySource getProperties(IJavaElement element) {
		return new JavaElementProperties(element);
	}
}