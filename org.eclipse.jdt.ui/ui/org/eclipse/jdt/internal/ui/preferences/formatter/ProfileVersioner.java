/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
 *     Microsoft Corporation - moved some methods and fields to ProfileVersionerCore
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.preferences.formatter;

import java.util.Map;

import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager.CustomProfile;


public class ProfileVersioner implements IProfileVersioner {

	public static final String CODE_FORMATTER_PROFILE_KIND= "CodeFormatterProfile"; //$NON-NLS-1$

	@Override
	public int getFirstVersion() {
	    return ProfileVersionerCore.getFirstVersion();
    }

	@Override
	public int getCurrentVersion() {
	    return ProfileVersionerCore.getCurrentVersion();
    }

    @Override
	public String getProfileKind() {
	    return CODE_FORMATTER_PROFILE_KIND;
    }

	@Override
	public void update(CustomProfile profile) {
		final Map<String, String> oldSettings= profile.getSettings();
		Map<String, String> newSettings= updateAndComplete(oldSettings, profile.getVersion());
		profile.setVersion(getCurrentVersion());
		profile.setSettings(newSettings);
	}

	public static Map<String, String> updateAndComplete(Map<String, String> oldSettings, int version) {
		return ProfileVersionerCore.updateAndComplete(oldSettings, version);
	}

	public static int getVersionStatus(CustomProfile profile) {
		final int version= profile.getVersion();
		if (version < ProfileVersionerCore.getCurrentVersion())
			return -1;
		else if (version > ProfileVersionerCore.getCurrentVersion())
			return 1;
		else
			return 0;
	}

 }
