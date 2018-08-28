/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui;

import org.eclipse.core.runtime.IAdapterFactory;

import org.eclipse.core.resources.mapping.ResourceMapping;

import org.eclipse.search.ui.ISearchPageScoreComputer;

import org.eclipse.jdt.internal.corext.util.JavaElementResourceMapping;

import org.eclipse.jdt.internal.ui.browsing.LogicalPackage;
import org.eclipse.jdt.internal.ui.search.JavaSearchPageScoreComputer;
import org.eclipse.jdt.internal.ui.search.SearchUtil;

/**
 * Implements basic UI support for LogicalPackage.
 */
public class LogicalPackageAdapterFactory implements IAdapterFactory {

	private static Class<?>[] PROPERTIES= new Class[] {
		ResourceMapping.class
	};

	// Must be Object to allow lazy loading
	private Object fSearchPageScoreComputer;

	@Override
	public Class<?>[] getAdapterList() {
		updateLazyLoadedAdapters();
		return PROPERTIES;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getAdapter(Object element, Class<T> key) {
		updateLazyLoadedAdapters();

		if (fSearchPageScoreComputer != null && ISearchPageScoreComputer.class.equals(key)) {
			return (T) fSearchPageScoreComputer;
		} else if (ResourceMapping.class.equals(key)) {
			if (!(element instanceof LogicalPackage))
				return null;
			return (T) JavaElementResourceMapping.create((LogicalPackage) element);
		}
		return null;
	}

	private void updateLazyLoadedAdapters() {
		if (fSearchPageScoreComputer == null && SearchUtil.isSearchPlugInActivated())
			createSearchPageScoreComputer();
	}

	private void createSearchPageScoreComputer() {
		fSearchPageScoreComputer= new JavaSearchPageScoreComputer();
		PROPERTIES= new Class[] {
			ISearchPageScoreComputer.class,
			ResourceMapping.class
		};
	}
}
