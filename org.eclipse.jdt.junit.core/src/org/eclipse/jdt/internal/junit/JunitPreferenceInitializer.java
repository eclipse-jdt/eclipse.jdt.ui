/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Sebastian Davids <sdavids@gmx.de> - initial API and implementation
 *     Achim Demelt <a.demelt@exxcellent.de> - [junit] Separate UI from non-UI code - https://bugs.eclipse.org/bugs/show_bug.cgi?id=278844
 *******************************************************************************/
package org.eclipse.jdt.internal.junit;

import java.util.List;

import org.osgi.service.prefs.BackingStoreException;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

/**
 * Default preference value initialization for the
 * <code>org.eclipse.jdt.junit</code> plug-in.
 */
public class JunitPreferenceInitializer extends AbstractPreferenceInitializer {

	/** {@inheritDoc} */
	public void initializeDefaultPreferences() {
		IEclipsePreferences prefs= DefaultScope.INSTANCE.getNode(JUnitCorePlugin.CORE_PLUGIN_ID);

		prefs.putBoolean(JUnitPreferencesConstants.DO_FILTER_STACK, true);

		prefs.putBoolean(JUnitPreferencesConstants.SHOW_ON_ERROR_ONLY, false);
		prefs.putBoolean(JUnitPreferencesConstants.ENABLE_ASSERTIONS, false);

		List defaults= JUnitPreferencesConstants.createDefaultStackFiltersList();
		String[] filters= (String[]) defaults.toArray(new String[defaults.size()]);
		String active= JUnitPreferencesConstants.serializeList(filters);
		prefs.put(JUnitPreferencesConstants.PREF_ACTIVE_FILTERS_LIST, active);
		prefs.put(JUnitPreferencesConstants.PREF_INACTIVE_FILTERS_LIST, ""); //$NON-NLS-1$
		prefs.putInt(JUnitPreferencesConstants.MAX_TEST_RUNS, 10);

		// see https://github.com/junit-team/junit/issues/570
		prefs.put(JUnitPreferencesConstants.JUNIT3_JAVADOC, "http://junit.sourceforge.net/junit3.8.1/javadoc/"); //$NON-NLS-1$
		prefs.put(JUnitPreferencesConstants.JUNIT4_JAVADOC, "http://junit-team.github.io/junit/javadoc/latest/"); //$NON-NLS-1$
		prefs.put(JUnitPreferencesConstants.HAMCREST_CORE_JAVADOC, "http://hamcrest.org/JavaHamcrest/javadoc/1.3/"); //$NON-NLS-1$
		
		// migrate old instance scope prefs
		try {
			IEclipsePreferences newInstancePrefs= InstanceScope.INSTANCE.getNode(JUnitCorePlugin.CORE_PLUGIN_ID);
			
			if (newInstancePrefs.keys().length == 0) {
				IEclipsePreferences oldInstancePrefs= InstanceScope.INSTANCE.getNode(JUnitCorePlugin.PLUGIN_ID);
				
				String[] oldKeys= oldInstancePrefs.keys();
				for (int i= 0; i < oldKeys.length; i++ ) {
					String key= oldKeys[i];
					newInstancePrefs.put(key, oldInstancePrefs.get(key, null));
					oldInstancePrefs.remove(key);
				}
				newInstancePrefs.flush();
				oldInstancePrefs.flush();
			}
		} catch (BackingStoreException e) {
			JUnitCorePlugin.log(e);
		}
	}
}
