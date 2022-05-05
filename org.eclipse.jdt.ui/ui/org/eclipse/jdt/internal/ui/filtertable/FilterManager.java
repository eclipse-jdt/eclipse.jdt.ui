/*******************************************************************************
 * Copyright (c) 2022 Zsombor Gegesy.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Zsombor Gegesy - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.filtertable;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.jface.preference.IPreferenceStore;

/**
 * Helper class to manage converting between active/inactive Filter lists into preference settings.
 * @since 3.26
 */
public class FilterManager {

	private final String activeListKey;
	private final String inactiveListKey;
	private final String separatorString;

	public FilterManager(String activeListKey, String inactiveListKey, String separatorString) {
		this.activeListKey = activeListKey;
		this.inactiveListKey = inactiveListKey;
		this.separatorString = separatorString;
	}

	public FilterManager(String activeListKey, String inactiveListKey) {
		this(activeListKey, inactiveListKey, ","); //$NON-NLS-1$
	}

	public String[] getActiveList(IPreferenceStore store) {
		return parseList(store.getString(activeListKey));
	}

	public String[] getInactiveList(IPreferenceStore store) {
		return parseList(store.getString(inactiveListKey));
	}

	public Filter[] getAllStoredFilters(IPreferenceStore store, boolean defaults) {
		String[] activefilters, inactivefilters;
		if (defaults) {
			activefilters = getDefaultActiveFilters(store);
			inactivefilters = getDefaultInactiveFilters(store);
		} else {
			activefilters = getActiveList(store);
			inactivefilters = getInactiveList(store);
		}
		Filter[] filters = new Filter[activefilters.length + inactivefilters.length];;
		for (int i = 0; i < activefilters.length; i++) {
			filters[i] = new Filter(activefilters[i], true);
		}
		for (int i = 0; i < inactivefilters.length; i++) {
			filters[i + activefilters.length] = new Filter(inactivefilters[i], false);
		}
		return filters;
	}

	protected String[] getDefaultActiveFilters(IPreferenceStore store) {
		return parseList(store.getDefaultString(activeListKey));
	}

	protected String[] getDefaultInactiveFilters(IPreferenceStore store) {
		return parseList(store.getDefaultString(inactiveListKey));
	}

	public void save(IPreferenceStore store, Filter[] filters) {
		ArrayList<String> active = new ArrayList<>();
		ArrayList<String> inactive = new ArrayList<>();
		String name = ""; //$NON-NLS-1$
		for (int i = 0; i < filters.length; i++) {
			name = filters[i].getName();
			if (filters[i].isChecked()) {
				active.add(name);
			} else {
				inactive.add(name);
			}
		}
		String pref = serializeList(active.toArray(new String[active.size()]));
		store.setValue(activeListKey, pref);
		pref = serializeList(inactive.toArray(new String[inactive.size()]));
		store.setValue(inactiveListKey, pref);
	}


	/**
	 * Parses the comma separated string into an array of strings
	 *
	 * @param listString the comma separated string
	 * @return list
	 */
	public String[] parseList(String listString) {
		List<String> list = new ArrayList<>(10);
		StringTokenizer tokenizer = new StringTokenizer(listString, separatorString);
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			list.add(token);
		}
		return list.toArray(new String[list.size()]);
	}

	/**
	 * Serializes the array of strings into one comma
	 * separated string.
	 *
	 * @param list array of strings
	 * @return a single string composed of the given list
	 */
	public String serializeList(String[] list) {
		if (list == null) {
			return ""; //$NON-NLS-1$
		}
		return String.join(separatorString, list);
	}

}
