/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javaeditor.saveparticipant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import org.eclipse.jdt.internal.corext.fix.CleanUpPostSaveListener;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.fix.CleanUpSaveParticipantPreferenceConfiguration;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitDocumentProvider;

/**
 * A registry for save participants. The registry contains
 * {@link SaveParticipantDescriptor}s. The registry keeps track
 * of enabled save participants. Save participants can be enabled
 * and disabled on the Java &gt; Editor &gt; Save Participants
 * preference page. An instance of this registry can be received 
 * through a call to {@link JavaPlugin#getSaveParticipantRegistry()}.
 * Enabled save participants are notified through a call to 
 * {@link IPostSaveListener#saved(org.eclipse.jdt.core.ICompilationUnit, org.eclipse.core.runtime.IProgressMonitor)}
 * whenever the {@link CompilationUnitDocumentProvider} does save
 * a compilation unit.
 * <p>
 * This class is not intended to be subclassed.
 * </p>
 * 
 * @since 3.3
 */
public final class SaveParticipantRegistry {
	
	/**
	 * Preference prefix that is appended to the id of {@link SaveParticipantDescriptor save participants}.
	 * 
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 * 
	 * XXX: Move to {@link PreferenceConstants}
	 *
	 * @see SaveParticipantDescriptor
	 * @since 3.3
	 */
	public static final String EDITOR_SAVE_PARTICIPANT_PREFIX= "editor_save_participant_";  //$NON-NLS-1$

	/** The map of descriptors, indexed by their identifiers. */
	private Map fDescriptors;
	/** Listen for enabling, disabling save participants */
	private IPropertyChangeListener fPropertyListener;
	/** Array of enabled post save listeners */
	private IPostSaveListener[] fEnabledPostSaveListeners;

	/**
	 * Creates a new instance.
	 */
	public SaveParticipantRegistry() {}

	/**
	 * Returns an array of <code>SaveParticipantDescriptor</code> describing
	 * all registered save participants.
	 *
	 * @return the array of registered save participant descriptors
	 */
	public synchronized SaveParticipantDescriptor[] getSaveParticipantDescriptors() {
		ensureRegistered();
		return (SaveParticipantDescriptor[]) fDescriptors.values().toArray(new SaveParticipantDescriptor[fDescriptors.size()]);
	}

	/**
	 * Returns the save participant descriptor for the given <code>id</code> or
	 * <code>null</code> if no such listener is registered.
	 *
	 * @param id the identifier of the requested save participant
	 * @return the corresponding descriptor, or <code>null</code> if none can be found
	 */
	public synchronized SaveParticipantDescriptor getSaveParticipantDescriptor(String id) {
		ensureRegistered();
		return (SaveParticipantDescriptor) fDescriptors.get(id);
	}
	
	/**
	 * Disables the given listener. 
	 * 
	 * @param listener the listener to disable, not null
	 */
	public synchronized void disablePostSaveListener(IPostSaveListener listener) {
		Assert.isNotNull(listener);
		
		final IPreferenceStore preferenceStore= JavaPlugin.getDefault().getPreferenceStore();
		if (fPropertyListener != null)
			preferenceStore.removePropertyChangeListener(fPropertyListener);
		preferenceStore.setValue(EDITOR_SAVE_PARTICIPANT_PREFIX + listener.getId(), false);
    	fEnabledPostSaveListeners= null;
	}

	/**
	 * Returns an array of <code>IPostSaveListener</code> which are 
	 * enabled.
	 *
	 * @return the current enabled post save listeners according to the preferences
	 */
	public synchronized IPostSaveListener[] getEnabledPostSaveListeners() {
		if (fEnabledPostSaveListeners == null)
			updateActiveListeners();
		return fEnabledPostSaveListeners;
	}

	/**
	 * Ensures that all descriptors are created and stored in
	 * <code>fDescriptors</code>.
	 */
	private void ensureRegistered() {
		if (fDescriptors == null)
			reloadDescriptors();
		
		if (fPropertyListener == null) {
	        fPropertyListener= new IPropertyChangeListener() {
		        public void propertyChange(PropertyChangeEvent event) {
		        	if (event.getProperty() != null && event.getProperty().startsWith(EDITOR_SAVE_PARTICIPANT_PREFIX))
				        updateActiveListeners();
		        }
	        };
	        JavaPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(fPropertyListener);
        }
	}

	/**
	 * Loads the save participants.
	 * <p>
	 * This method can be called more than once in
	 * order to reload from a changed extension registry.
	 * </p>
	 */
	private void reloadDescriptors() {
		Map map= new HashMap();
		SaveParticipantDescriptor desc= new SaveParticipantDescriptor(
				new CleanUpPostSaveListener(), 
				new CleanUpSaveParticipantPreferenceConfiguration());
		map.put(desc.getId(), desc);
		fDescriptors= map;
	}

	public void dispose() {
		if (fPropertyListener != null) {
			JavaPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(fPropertyListener);
			fPropertyListener= null;
		}
	}

	private synchronized void updateActiveListeners() {
		ensureRegistered();

		ArrayList result= new ArrayList();
		IPreferenceStore preferenceStore= JavaPlugin.getDefault().getPreferenceStore();
		for (Iterator iterator= fDescriptors.values().iterator(); iterator.hasNext();) {
			SaveParticipantDescriptor desc= (SaveParticipantDescriptor)iterator.next();
			boolean enabled= preferenceStore.getBoolean(getPreferenceKey(desc));
			if (enabled)
				result.add(desc.getPostSaveListener());
		}

		fEnabledPostSaveListeners= (IPostSaveListener[])result.toArray(new IPostSaveListener[result.size()]);
    }

	private String getPreferenceKey(SaveParticipantDescriptor descriptor) {
		return EDITOR_SAVE_PARTICIPANT_PREFIX + descriptor.getId();
    }

}
