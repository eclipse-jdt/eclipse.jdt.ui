/*******************************************************************************
 * Copyright (c) 2010, 2017 IBM Corporation and others.
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
package org.eclipse.jdt.internal.junit.ui;

import org.osgi.service.prefs.BackingStoreException;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

/**
 * Defines constants which are used to refer to values in the plugin's preference store.
 *
 * @since 3.7
 */
public class JUnitUIPreferencesConstants {
	/**
	 * Boolean preference controlling whether newly launched JUnit tests should be shown in all
	 * JUnit views (in all windows).
	 */
	public static final String SHOW_IN_ALL_VIEWS= JUnitPlugin.PLUGIN_ID + ".show_in_all_views"; //$NON-NLS-1$

	public static final boolean SHOW_IN_ALL_VIEWS_DEFAULT= false; // would need a PreferenceInitializer if this was changed to true!

	private static final String CODEASSIST_FAVORITE_STATIC_MEMBERS_MIGRATED= JUnitPlugin.PLUGIN_ID + ".content_assist_favorite_static_members_migrated"; //$NON-NLS-1$

	private JUnitUIPreferencesConstants() {
		// no instance
	}

	public static boolean getShowInAllViews() {
		return Platform.getPreferencesService().getBoolean(JUnitPlugin.PLUGIN_ID, SHOW_IN_ALL_VIEWS, SHOW_IN_ALL_VIEWS_DEFAULT, null);
	}

	public static void setShowInAllViews(boolean show) {
		IEclipsePreferences preferences= InstanceScope.INSTANCE.getNode(JUnitPlugin.PLUGIN_ID);
		preferences.putBoolean(SHOW_IN_ALL_VIEWS, show);
		try {
			preferences.flush();
		} catch (BackingStoreException e) {
			JUnitPlugin.log(e);
		}
	}

	public static boolean isCodeassistFavoriteStaticMembersMigrated() {
		return Platform.getPreferencesService().getBoolean(JUnitPlugin.PLUGIN_ID, CODEASSIST_FAVORITE_STATIC_MEMBERS_MIGRATED, false, null);
	}

	public static void setCodeassistFavoriteStaticMembersMigrated(boolean migrated) {
		IEclipsePreferences preferences= InstanceScope.INSTANCE.getNode(JUnitPlugin.PLUGIN_ID);
		preferences.putBoolean(CODEASSIST_FAVORITE_STATIC_MEMBERS_MIGRATED, migrated);
		try {
			preferences.flush();
		} catch (BackingStoreException e) {
			JUnitPlugin.log(e);
		}
	}
}
