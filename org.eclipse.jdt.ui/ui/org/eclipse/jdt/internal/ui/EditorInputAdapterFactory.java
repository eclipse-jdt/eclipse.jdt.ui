/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui;

import org.eclipse.core.runtime.IAdapterFactory;

import org.eclipse.search.ui.ISearchPageScoreComputer;

import org.eclipse.jdt.internal.ui.search.JavaSearchPageScoreComputer;
import org.eclipse.jdt.internal.ui.search.SearchUtil;

/**
 * Adapter factory to support basic UI operations for for editor inputs.
 */
public class EditorInputAdapterFactory implements IAdapterFactory {
	
	private static Class[] PROPERTIES= new Class[0];
	
	private Object fSearchPageScoreComputer;

	public Class[] getAdapterList() {
		updateLazyLoadedAdapters();
		return PROPERTIES;
	}
	
	public Object getAdapter(Object element, Class key) {
		updateLazyLoadedAdapters();
		if (fSearchPageScoreComputer != null && ISearchPageScoreComputer.class.equals(key))
			return fSearchPageScoreComputer;
		return null;
	}

	private void updateLazyLoadedAdapters() {
		if (fSearchPageScoreComputer == null && SearchUtil.isSearchPlugInActivated())
			createSearchPageScoreComputer();
	}
	
	private void createSearchPageScoreComputer() {
		fSearchPageScoreComputer= new JavaSearchPageScoreComputer();
		PROPERTIES= new Class[] {ISearchPageScoreComputer.class};
	}
}