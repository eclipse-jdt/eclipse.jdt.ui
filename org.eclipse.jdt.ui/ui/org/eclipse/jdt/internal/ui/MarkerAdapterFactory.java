/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui;

import org.eclipse.core.runtime.IAdapterFactory;

import org.eclipse.search.ui.ISearchPageScoreComputer;

import org.eclipse.jdt.internal.ui.search.JavaSearchPageScoreComputer;

/**
 * Adapter factory to support basic UI operations for markers.
 */
public class MarkerAdapterFactory implements IAdapterFactory {
	
	private static Class[] PROPERTIES= new Class[] {
		ISearchPageScoreComputer.class
	};
	
	private ISearchPageScoreComputer fSearchPageScoreComputer;
	
	public Class[] getAdapterList() {
		return PROPERTIES;
	}
	
	public Object getAdapter(Object element, Class key) {
		if (ISearchPageScoreComputer.class.equals(key))
			return getSearchPageScoreComputer();
		return null;
	}
	
	private ISearchPageScoreComputer getSearchPageScoreComputer() {
		if (fSearchPageScoreComputer == null)
			fSearchPageScoreComputer= new JavaSearchPageScoreComputer();
		return fSearchPageScoreComputer;
	}
}