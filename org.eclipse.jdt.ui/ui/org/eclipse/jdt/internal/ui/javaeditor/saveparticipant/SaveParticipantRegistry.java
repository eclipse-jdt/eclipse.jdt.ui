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

import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;

import org.eclipse.jdt.internal.corext.fix.CleanUpPostSaveListener;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.fix.CleanUpSaveParticipantPreferenceConfiguration;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitDocumentProvider;

/**
 * A registry for save participants. This registry manages
 * {@link SaveParticipantDescriptor}s and keeps track of enabled save
 * participants.
 * <p>
 * Save participants can be enabled and disabled on the Java &gt; Editor &gt;
 * Save Participants preference page. Enabled save participants are notified
 * through a call to
 * {@link IPostSaveListener#saved(org.eclipse.jdt.core.ICompilationUnit, org.eclipse.core.runtime.IProgressMonitor)}
 * whenever the {@link CompilationUnitDocumentProvider} saves a compilation unit
 * that is in the workspace.</p>
 * <p>
 * An instance of this registry can be received through a call to {@link JavaPlugin#getSaveParticipantRegistry()}.</p>
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
    private static final String EDITOR_SAVE_PARTICIPANT_PREFIX= "editor_save_participant_";  //$NON-NLS-1$
    
	/** The map of descriptors, indexed by their identifiers. */
	private Map fDescriptors;
	/** Maps IProject to array of enabled post save listeners */
	private HashMap fEnabledPostSaveListeners;
	/** Maps IScopeContext to IPreferenceChangeListeners */
	private HashMap fPreferenceChangeListeners;

	/**
	 * Creates a new instance.
	 */
	public SaveParticipantRegistry() {
	}

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
	 * Ensures that all descriptors are created and stored in
	 * <code>fDescriptors</code>.
	 */
	private void ensureRegistered() {
		if (fDescriptors == null)
			reloadDescriptors();
			
		if (fEnabledPostSaveListeners == null) {
			fEnabledPostSaveListeners= new HashMap();
			fPreferenceChangeListeners= new HashMap();
			addPreferenceChangeListener(new InstanceScope());	        
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
		if (fPreferenceChangeListeners != null) {
			for (Iterator iterator= fPreferenceChangeListeners.keySet().iterator(); iterator.hasNext();) {
	            IScopeContext context= (IScopeContext)iterator.next();
	            context.getNode(JavaUI.ID_PLUGIN).removePreferenceChangeListener((IPreferenceChangeListener)fPreferenceChangeListeners.get(context));
            }
		}
	}

	/**
	 * The key that is used to store the enable state of the given save participant
	 * descriptor.
	 * 
	 * @param descriptor the descriptor to generate a key from, not null
	 * @return the key, not null
	 */
	public static String getPreferenceKey(SaveParticipantDescriptor descriptor) {
    	return SaveParticipantRegistry.EDITOR_SAVE_PARTICIPANT_PREFIX + descriptor.getId();
    }

	/**
	 * Checks weather there are enabled or disabled post save listener in the given context.
	 * 
	 * @param context to context to check, not null
	 * @return true if there are settings in context
	 */
	public synchronized boolean hasSettingsInScope(IScopeContext context) {		
		ensureRegistered();
		
    	IEclipsePreferences node= context.getNode(JavaUI.ID_PLUGIN);
    	for (Iterator iterator= fDescriptors.values().iterator(); iterator.hasNext();) {
	        SaveParticipantDescriptor descriptor= (SaveParticipantDescriptor)iterator.next();
    		if (node.get(getPreferenceKey(descriptor), null) != null)
    			return true;
    	}
    	
    	return false;
    }
	
	/**
	 * Returns an array of <code>IPostSaveListener</code> which are 
	 * enabled for the given project.
	 *
	 * @param project the project to retrieve the settings for, not null
	 * @return the current enabled post save listeners according to the preferences
	 */
	public synchronized IPostSaveListener[] getEnabledPostSaveListeners(IProject project) {
		ensureRegistered();
		
		IPostSaveListener[] result= (IPostSaveListener[])fEnabledPostSaveListeners.get(project);
		if (result == null) {
			ProjectScope projectScope= new ProjectScope(project);
			result= getEnabledPostSaveListeners(projectScope);
			fEnabledPostSaveListeners.put(project, result);
			addPreferenceChangeListener(projectScope);
		}
		
		return result;
	}

	/**
	 * Returns an array of <code>IPostSaveListener</code> which are 
	 * enabled in the given context.
	 *
	 * @param context the context from which to retrive the settings from, not null
	 * @return the current enabled post save listeners according to the preferences
	 */
	public synchronized IPostSaveListener[] getEnabledPostSaveListeners(IScopeContext context) {
		ensureRegistered();
		
		IEclipsePreferences node;
		if (hasSettingsInScope(context)) {
			node= context.getNode(JavaUI.ID_PLUGIN);
		} else {
			node= new InstanceScope().getNode(JavaUI.ID_PLUGIN);
		}
		IEclipsePreferences defaultNode= new DefaultScope().getNode(JavaUI.ID_PLUGIN);
		
		ArrayList result= new ArrayList();
		for (Iterator iterator= fDescriptors.values().iterator(); iterator.hasNext();) {
			SaveParticipantDescriptor descriptor= (SaveParticipantDescriptor)iterator.next();
			String key= SaveParticipantRegistry.getPreferenceKey(descriptor);
			if (node.getBoolean(key, defaultNode.getBoolean(key, false)))
				result.add(descriptor.getPostSaveListener());
		}
		
		return (IPostSaveListener[])result.toArray(new IPostSaveListener[result.size()]);
	}
	
	private void updateEnabledPostSaveListeners() {
		for (Iterator iterator= fEnabledPostSaveListeners.keySet().iterator(); iterator.hasNext();) {
	        IProject project= (IProject)iterator.next();
	        IPostSaveListener[] listeners= getEnabledPostSaveListeners(new ProjectScope(project));
	        fEnabledPostSaveListeners.put(project, listeners);
        }
	}
	
	private void addPreferenceChangeListener(IScopeContext context) {
	    IPreferenceChangeListener listener= new IPreferenceChangeListener() {
	    	public void preferenceChange(PreferenceChangeEvent event) {
	    		if (event.getKey().startsWith(EDITOR_SAVE_PARTICIPANT_PREFIX))
	    	        updateEnabledPostSaveListeners();
	    	}
	    };
	    context.getNode(JavaUI.ID_PLUGIN).addPreferenceChangeListener(listener);
	    fPreferenceChangeListeners.put(context, listener);
    }
}
