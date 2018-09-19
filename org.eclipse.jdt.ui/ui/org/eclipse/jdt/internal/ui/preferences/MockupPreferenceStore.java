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

package org.eclipse.jdt.internal.ui.preferences;

import org.eclipse.core.runtime.ListenerList;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

/**
 * Mockup preference store, for registering listeners and firing events,
 * without being an actual store.
 * <p>
 * All methods except firing, adding and removing listeners throw
 * an {@link java.lang.UnsupportedOperationException}.
 * </p>
 *
 * @since 3.0
 */
public class MockupPreferenceStore implements IPreferenceStore {

	/** Listeners on this store */
	private ListenerList<IPropertyChangeListener> fListeners= new ListenerList<>(ListenerList.IDENTITY);

	@Override
	public void addPropertyChangeListener(IPropertyChangeListener listener) {
		fListeners.add(listener);
	}

	@Override
	public void removePropertyChangeListener(IPropertyChangeListener listener) {
		fListeners.remove(listener);
	}

	@Override
	public boolean contains(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void firePropertyChangeEvent(String name, Object oldValue, Object newValue) {
		firePropertyChangeEvent(this, name, oldValue, newValue);
	}

	/**
	 * Fires a property change event with the given source, property name, old and new value. Used
	 * when the event source should be different from this mockup preference store.
	 * @param source The event source
	 * @param name The property name
	 * @param oldValue The property's old value
	 * @param newValue The property's new value
	 */
	public void firePropertyChangeEvent(Object source, String name, Object oldValue, Object newValue) {
		PropertyChangeEvent event= new PropertyChangeEvent(source, name, oldValue, newValue);
		for (IPropertyChangeListener listener : fListeners) {
			listener.propertyChange(event);
		}
	}

	@Override
	public boolean getBoolean(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean getDefaultBoolean(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getDefaultDouble(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public float getDefaultFloat(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getDefaultInt(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public long getDefaultLong(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getDefaultString(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getDouble(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public float getFloat(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getInt(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public long getLong(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getString(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isDefault(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean needsSaving() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void putValue(String name, String value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setDefault(String name, double value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setDefault(String name, float value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setDefault(String name, int value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setDefault(String name, long value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setDefault(String name, String defaultObject) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setDefault(String name, boolean value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setToDefault(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setValue(String name, double value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setValue(String name, float value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setValue(String name, int value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setValue(String name, long value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setValue(String name, String value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setValue(String name, boolean value) {
		throw new UnsupportedOperationException();
	}

}
