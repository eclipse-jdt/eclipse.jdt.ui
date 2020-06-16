/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
 *     Red Hat Inc. - split out non-UI logic to MemebersOrderPreferenceCacheCommon
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.preferences;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import org.eclipse.jdt.internal.core.manipulation.MembersOrderPreferenceCacheCommon;

import org.eclipse.jdt.ui.PreferenceConstants;

/**
 */
public class MembersOrderPreferenceCache extends MembersOrderPreferenceCacheCommon implements IPropertyChangeListener {

	private IPreferenceStore fPreferenceStore;

	public MembersOrderPreferenceCache() {
		fPreferenceStore= null;
	}

	public void install(IPreferenceStore store) {
		fPreferenceStore= store;
		store.addPropertyChangeListener(this);
		super.install();
		fSortByVisibility= store.getBoolean(PreferenceConstants.APPEARANCE_ENABLE_VISIBILITY_SORT_ORDER);
	}

	@Override
	public void dispose() {
		super.dispose();
		fPreferenceStore.removePropertyChangeListener(this);
		fPreferenceStore= null;
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		String property= event.getProperty();

		if (property != null) {
			switch (property) {
				case PreferenceConstants.APPEARANCE_MEMBER_SORT_ORDER:
					fCategoryOffsets= null;
					break;
				case PreferenceConstants.APPEARANCE_VISIBILITY_SORT_ORDER:
					fVisibilityOffsets= null;
					break;
				case PreferenceConstants.APPEARANCE_ENABLE_VISIBILITY_SORT_ORDER:
					fSortByVisibility= fPreferenceStore.getBoolean(PreferenceConstants.APPEARANCE_ENABLE_VISIBILITY_SORT_ORDER);
					break;
				default:
					break;
			}
		}
	}

	@Override
	protected int[] getCategoryOffsets() {
		int[] offsets= new int[N_CATEGORIES];
		IPreferenceStore store= fPreferenceStore;
		String key= PreferenceConstants.APPEARANCE_MEMBER_SORT_ORDER;
		boolean success= fillCategoryOffsetsFromPreferenceString(store.getString(key), offsets);
		if (!success) {
			store.setToDefault(key);
			fillCategoryOffsetsFromPreferenceString(store.getDefaultString(key), offsets);
		}
		return offsets;
	}

	@Override
	protected int[] getVisibilityOffsets() {
		int[] offsets= new int[N_VISIBILITIES];
		IPreferenceStore store= fPreferenceStore;
		String key= PreferenceConstants.APPEARANCE_VISIBILITY_SORT_ORDER;
		boolean success= fillVisibilityOffsetsFromPreferenceString(store.getString(key), offsets);
		if (!success) {
			store.setToDefault(key);
			fillVisibilityOffsetsFromPreferenceString(store.getDefaultString(key), offsets);
		}
		return offsets;
	}

}
