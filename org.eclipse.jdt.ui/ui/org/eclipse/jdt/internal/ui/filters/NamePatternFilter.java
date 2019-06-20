/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.filters;

import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.model.IWorkbenchAdapter;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.internal.ui.util.StringMatcher;

/**
 * The NamePatternFilter selects the elements which
 * match the given string patterns.
 * <p>
 * The following characters have special meaning:
 *   ? => any character
 *   * => any string
 * </p>
 *
 * @since 2.0
 */
public class NamePatternFilter extends ViewerFilter {
	private String[] fPatterns;
	private StringMatcher[] fMatchers;

	/**
	 * Return the currently configured StringMatchers.
	 * @return returns the matchers
	 */
	private StringMatcher[] getMatchers() {
		return fMatchers;
	}

	/**
	 * Gets the patterns for the receiver.
	 * @return returns the patterns to be filtered for
	 */
	public String[] getPatterns() {
		return fPatterns;
	}


	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (fMatchers.length == 0) {
			return true;
		}
		String matchName= null;
		if (element instanceof IJavaElement) {
			matchName= ((IJavaElement) element).getElementName();
		} else if (element instanceof IResource) {
			matchName= ((IResource) element).getName();
		} else if (element instanceof IStorage) {
			matchName= ((IStorage) element).getName();
		} else if (element instanceof IWorkingSet) {
			matchName= ((IWorkingSet) element).getLabel();
		} else if (element instanceof IAdaptable) {
			IWorkbenchAdapter wbadapter= ((IAdaptable)element).getAdapter(IWorkbenchAdapter.class);
			if (wbadapter != null) {
				matchName= wbadapter.getLabel(element);
			}
		}
		if (matchName != null && matchName.length() > 0) {
			for (StringMatcher testMatcher : getMatchers()) {
				if (testMatcher.match(matchName)) {
					return false;
				}
			}
			return true;
		}
		return true;
	}

	/**
	 * Sets the patterns to filter out for the receiver.
	 * <p>
	 * The following characters have special meaning:
	 *   ? => any character
	 *   * => any string
	 * </p>
	 * @param newPatterns the new patterns
	 */
	public void setPatterns(String[] newPatterns) {
		fPatterns = newPatterns;
		fMatchers = new StringMatcher[newPatterns.length];
		for (int i = 0; i < newPatterns.length; i++) {
			//Reset the matchers to prevent constructor overhead
			fMatchers[i]= new StringMatcher(newPatterns[i], true, false);
		}
	}
}
