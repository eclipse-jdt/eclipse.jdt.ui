/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
package org.eclipse.jdt.core.manipulation;

import java.util.Collection;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.search.TypeNameMatch;
import org.eclipse.jdt.core.search.TypeNameMatchRequestor;

/**
 * @since 1.10
 */
public class TypeNameMatchCollector extends TypeNameMatchRequestor {

	private final Collection<TypeNameMatch> fCollection;
	private Pattern[] fStringMatchers;

	public TypeNameMatchCollector(Collection<TypeNameMatch> collection) {
		Assert.isNotNull(collection);
		fCollection= collection;
	}

	private boolean inScope(TypeNameMatch match) {
		return ! isFiltered(match);
	}

	private boolean isFiltered(TypeNameMatch match) {
		boolean filteredByPattern= filter(match.getFullyQualifiedName());
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

	private boolean filter(String fullTypeName) {
		for (Pattern curr : getStringMatchers()) {
			if (curr.matcher(fullTypeName).matches()) {
				return true;
			}
		}
		return false;
	}

	private synchronized Pattern[] getStringMatchers() {
		if (fStringMatchers == null) {
			String str= getPreference("org.eclipse.jdt.ui.typefilter.enabled"); //$NON-NLS-1$
			StringTokenizer tok= new StringTokenizer(str, ";"); //$NON-NLS-1$
			int nTokens= tok.countTokens();

			fStringMatchers= new Pattern[nTokens];
			for (int i= 0; i < nTokens; i++) {
				String curr= tok.nextToken();
				if (curr.length() > 0) {
					// Simulate '*', and '?' wildcards using '.*' and '.'
					curr = curr.replace(".", "\\."); //$NON-NLS-1$ //$NON-NLS-2$
					curr = curr.replace("*", ".*"); //$NON-NLS-1$ //$NON-NLS-2$
					curr = curr.replace("?", "."); //$NON-NLS-1$ //$NON-NLS-2$
					fStringMatchers[i]= Pattern.compile(curr);
				}
			}
		}
		return fStringMatchers;
	}

	@Override
	public void acceptTypeNameMatch(TypeNameMatch match) {
		if (inScope(match)) {
			fCollection.add(match);
		}
	}

	private static String getPreference(String key) {
		String val;
		IEclipsePreferences node= InstanceScope.INSTANCE.getNode(JavaManipulation.getPreferenceNodeId());
		if (node != null) {
			val= node.get(key, null);
			if (val != null) {
				return val;
			}
		}
		node= DefaultScope.INSTANCE.getNode(JavaManipulation.getPreferenceNodeId());
		if (node != null) {
			return node.get(key, null);
		}
	    return null;
	}

}
