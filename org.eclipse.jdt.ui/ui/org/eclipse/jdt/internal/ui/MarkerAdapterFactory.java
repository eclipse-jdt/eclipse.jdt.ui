/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui;


import org.eclipse.core.runtime.IAdapterFactory;

import org.eclipse.search.ui.ISearchPageScoreComputer;

import org.eclipse.jdt.internal.ui.search.JavaSearchPageScoreComputer;

/**
 * Implements basic UI support for markers.
 */
public class MarkerAdapterFactory implements IAdapterFactory {
	
	private static Class[] PROPERTIES= new Class[] {
		ISearchPageScoreComputer.class
	};
	
	private ISearchPageScoreComputer fSearchPageScoreComputer= new JavaSearchPageScoreComputer();
	
	public Class[] getAdapterList() {
		return PROPERTIES;
	}
	
	public Object getAdapter(Object element, Class key) {
		if (ISearchPageScoreComputer.class.equals(key))
			return fSearchPageScoreComputer;
		return null;
	}
}