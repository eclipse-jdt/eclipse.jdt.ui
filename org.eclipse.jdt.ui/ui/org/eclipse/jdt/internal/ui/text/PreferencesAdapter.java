/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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

package org.eclipse.jdt.internal.ui.text;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Preferences;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Adapts {@link org.eclipse.core.runtime.Preferences} to
 * {@link org.eclipse.jface.preference.IPreferenceStore}
 *
 * @since 3.0
 */
public class PreferencesAdapter implements IPreferenceStore {

	/**
	 * Property change listener. Listens for events of type
	 * {@link org.eclipse.core.runtime.Preferences.PropertyChangeEvent} and fires
	 * a {@link org.eclipse.jface.util.PropertyChangeEvent} on the
	 * adapter with arguments from the received event.
	 */
	private class PropertyChangeListener implements Preferences.IPropertyChangeListener {

		/*
		 * @see org.eclipse.core.runtime.Preferences.IPropertyChangeListener#propertyChange(org.eclipse.core.runtime.Preferences.PropertyChangeEvent)
		 */
		@Override
		public void propertyChange(Preferences.PropertyChangeEvent event) {
			firePropertyChangeEvent(event.getProperty(), event.getOldValue(), event.getNewValue());
		}
	}

	/** Listeners on the adapter */
	private ListenerList<IPropertyChangeListener> fListeners= new ListenerList<>(ListenerList.IDENTITY);

	/** Listener on the adapted Preferences */
	private PropertyChangeListener fListener= new PropertyChangeListener();

	/** Adapted Preferences */
	private Preferences fPreferences;

	/** True iff no events should be forwarded */
	private boolean fSilent;

	/**
	 * Initialize with empty Preferences.
	 */
	public PreferencesAdapter() {
		this(new Preferences());
	}
	/**
	 * Initialize with the given Preferences.
	 *
	 * @param preferences The preferences to wrap.
	 */
	public PreferencesAdapter(Preferences preferences) {
		fPreferences= preferences;
	}

	@Override
	public void addPropertyChangeListener(IPropertyChangeListener listener) {
		if (fListeners.size() == 0)
			fPreferences.addPropertyChangeListener(fListener);
		fListeners.add(listener);
	}

	@Override
	public void removePropertyChangeListener(IPropertyChangeListener listener) {
		fListeners.remove(listener);
		if (fListeners.size() == 0)
			fPreferences.removePropertyChangeListener(fListener);
	}

	@Override
	public boolean contains(String name) {
		return fPreferences.contains(name);
	}

	@Override
	public void firePropertyChangeEvent(String name, Object oldValue, Object newValue) {
		if (!fSilent) {
			final PropertyChangeEvent event= new PropertyChangeEvent(this, name, oldValue, newValue);
			for (final IPropertyChangeListener listener : fListeners) {
				Runnable runnable= () -> listener.propertyChange(event);

				if (Display.getCurrent() != null)
					runnable.run();
				else {
					// Post runnable into UI thread
					Shell shell= JavaPlugin.getActiveWorkbenchShell();
					Display display;
					if (shell != null)
						display= shell.getDisplay();
					else
						display= Display.getDefault();
					display.asyncExec(runnable);
				}
			}
		}
	}

	@Override
	public boolean getBoolean(String name) {
		return fPreferences.getBoolean(name);
	}

	@Override
	public boolean getDefaultBoolean(String name) {
		return fPreferences.getDefaultBoolean(name);
	}

	@Override
	public double getDefaultDouble(String name) {
		return fPreferences.getDefaultDouble(name);
	}

	@Override
	public float getDefaultFloat(String name) {
		return fPreferences.getDefaultFloat(name);
	}

	@Override
	public int getDefaultInt(String name) {
		return fPreferences.getDefaultInt(name);
	}

	@Override
	public long getDefaultLong(String name) {
		return fPreferences.getDefaultLong(name);
	}

	@Override
	public String getDefaultString(String name) {
		return fPreferences.getDefaultString(name);
	}

	@Override
	public double getDouble(String name) {
		return fPreferences.getDouble(name);
	}

	@Override
	public float getFloat(String name) {
		return fPreferences.getFloat(name);
	}

	@Override
	public int getInt(String name) {
		return fPreferences.getInt(name);
	}

	@Override
	public long getLong(String name) {
		return fPreferences.getLong(name);
	}

	@Override
	public String getString(String name) {
		return fPreferences.getString(name);
	}

	@Override
	public boolean isDefault(String name) {
		return fPreferences.isDefault(name);
	}

	@Override
	public boolean needsSaving() {
		return fPreferences.needsSaving();
	}

	@Override
	public void putValue(String name, String value) {
		try {
			fSilent= true;
			fPreferences.setValue(name, value);
		} finally {
			fSilent= false;
		}
	}

	@Override
	public void setDefault(String name, double value) {
		fPreferences.setDefault(name, value);
	}

	@Override
	public void setDefault(String name, float value) {
		fPreferences.setDefault(name, value);
	}

	@Override
	public void setDefault(String name, int value) {
		fPreferences.setDefault(name, value);
	}

	@Override
	public void setDefault(String name, long value) {
		fPreferences.setDefault(name, value);
	}

	@Override
	public void setDefault(String name, String defaultObject) {
		fPreferences.setDefault(name, defaultObject);
	}

	@Override
	public void setDefault(String name, boolean value) {
		fPreferences.setDefault(name, value);
	}

	@Override
	public void setToDefault(String name) {
		fPreferences.setToDefault(name);
	}

	@Override
	public void setValue(String name, double value) {
		fPreferences.setValue(name, value);
	}

	@Override
	public void setValue(String name, float value) {
		fPreferences.setValue(name, value);
	}

	@Override
	public void setValue(String name, int value) {
		fPreferences.setValue(name, value);
	}

	@Override
	public void setValue(String name, long value) {
		fPreferences.setValue(name, value);
	}

	@Override
	public void setValue(String name, String value) {
		fPreferences.setValue(name, value);
	}

	@Override
	public void setValue(String name, boolean value) {
		fPreferences.setValue(name, value);
	}
}
