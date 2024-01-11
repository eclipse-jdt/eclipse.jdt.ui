/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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
package org.eclipse.jdt.internal.corext.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.manipulation.JavaManipulation;
import org.eclipse.jdt.core.search.TypeNameMatch;

import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;

import org.eclipse.jdt.internal.ui.PreferenceConstantsCore;
import org.eclipse.jdt.internal.ui.util.StringMatcher;

public class TypeFilter implements IPreferenceChangeListener {

	public static TypeFilter getDefault() {
		return JavaManipulationPlugin.getDefault().getTypeFilter();
	}

	public static boolean isFiltered(String fullTypeName) {
		return getDefault().filter(fullTypeName);
	}

	public static boolean isFiltered(char[] fullTypeName) {
		return getDefault().filter(new String(fullTypeName));
	}

	public static boolean isFiltered(char[] packageName, char[] typeName) {
		return getDefault().filter(JavaModelUtil.concatenateName(packageName, typeName));
	}

	public static boolean isFiltered(IType type) {
		TypeFilter typeFilter = getDefault();
		if (typeFilter.hasFilters()) {
			return typeFilter.filter(type.getFullyQualifiedName('.'));
		}
		return false;
	}

	public static boolean isFiltered(TypeNameMatch match) {
		boolean filteredByPattern= getDefault().filter(match.getFullyQualifiedName());
		if (filteredByPattern)
			return true;

		int accessibility= match.getAccessibility();
		switch (accessibility) {
			case IAccessRule.K_NON_ACCESSIBLE:
				return JavaCore.ENABLED.equals(JavaCore.getOption(JavaCore.CODEASSIST_FORBIDDEN_REFERENCE_CHECK));
			case IAccessRule.K_DISCOURAGED:
				return JavaCore.ENABLED.equals(JavaCore.getOption(JavaCore.CODEASSIST_DISCOURAGED_REFERENCE_CHECK));
			default:
				return false;
		}
	}

	/**
	 * Remove the type filter if any of the imported element matches.
	 */
	public synchronized void removeFilterIfMatched(Collection<String> importedElements) {
		if (importedElements == null || importedElements.isEmpty()) {
			return;
		}

		StringMatcher[] matchers = getStringMatchers();
		this.fStringMatchers = Arrays.stream(matchers).filter(m -> {
			for (String importedElement : importedElements) {
				if (m.match(importedElement)) {
					return false;
				}
			}
			return true;
		}).toArray(size -> new StringMatcher[size]);
	}


	private StringMatcher[] fStringMatchers;

	public TypeFilter() {
		fStringMatchers= null;
		getPreferenceStore().addPreferenceChangeListener(this);
	}

	private IEclipsePreferences getPreferenceStore() {
		return InstanceScope.INSTANCE.getNode(JavaManipulation.getPreferenceNodeId());
	}

	private IEclipsePreferences getDefaultPreferenceStore() {
		return DefaultScope.INSTANCE.getNode(JavaManipulation.getPreferenceNodeId());
	}

	private String getPreference(String key, String def) {
		String str= getPreferenceStore().get(key, null);
		if( str == null ) {
			str= getDefaultPreferenceStore().get(key, null);
		}
		return str == null ? def : str;
	}

	private synchronized StringMatcher[] getStringMatchers() {
		if (fStringMatchers == null) {
			String str= getPreference(PreferenceConstantsCore.TYPEFILTER_ENABLED, ""); //$NON-NLS-1$
			StringTokenizer tok= new StringTokenizer(str, ";"); //$NON-NLS-1$
			int nTokens= tok.countTokens();

			fStringMatchers= new StringMatcher[nTokens];
			for (int i= 0; i < nTokens; i++) {
				String curr= tok.nextToken();
				if (curr.length() > 0) {
					fStringMatchers[i]= new StringMatcher(curr, false, false);
				}
			}
		}
		return fStringMatchers;
	}

	public void dispose() {
		getPreferenceStore().removePreferenceChangeListener(this);
		fStringMatchers= null;
	}


	public boolean hasFilters() {
		return getStringMatchers().length > 0;
	}

	/**
	 * @param fullTypeName fully-qualified type name
	 * @return <code>true</code> iff the given type is filtered out
	 */
	public boolean filter(String fullTypeName) {
		for (StringMatcher curr : getStringMatchers()) {
			if (curr.match(fullTypeName)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void preferenceChange(PreferenceChangeEvent event) {
		if( PreferenceConstantsCore.TYPEFILTER_ENABLED.equals(event.getKey())) {
			fStringMatchers= null;
		}
	}
}
