/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.search.internal.core;

import org.eclipse.core.runtime.*;
import org.eclipse.search.core.SearchParticipant;

/**
 * TODO add spec
 */
public class SearchCore extends Plugin implements IExecutableExtension {

	private static Plugin SEARCH_CORE_PLUGIN = null; 
	
	/**
	 * The plug-in identifier of the Search core support
	 * (value <code>"org.eclipse.search.core"</code>).
	 */
	public static final String PLUGIN_ID = "org.eclipse.search.core" ; //$NON-NLS-1$
	
	public static final String SEARCH_PARTICIPANT_EXTPOINT_ID = "searchParticipant" ; //$NON-NLS-1$

	public static boolean VERBOSE = false;
	
	/**
	 * Creates the Search core plug-in.
	 */
	public SearchCore(IPluginDescriptor pluginDescriptor) {
		super(pluginDescriptor);
		SEARCH_CORE_PLUGIN = this;
	}

	/**
	 * Returns all registered search participants
	 */
	/**
	 * Helper method finding the classpath container initializer registered for a given classpath container ID 
	 * or <code>null</code> if none was found while iterating over the contributions to extension point to
	 * the extension point "org.eclipse.jdt.core.classpathContainerInitializer".
	 * <p>
	 * A containerID is the first segment of any container path, used to identify the registered container initializer.
	 * <p>
	 * @param String - a containerID identifying a registered initializer
	 * @return ClasspathContainerInitializer - the registered classpath container initializer or <code>null</code> if 
	 * none was found.
	 */
	public static SearchParticipant[] getParticipants() {
		
		if (getPlugin() == null) return null;
	
		IExtensionPoint extension = getPlugin().getDescriptor().getExtensionPoint(SearchCore.SEARCH_PARTICIPANT_EXTPOINT_ID);
		if (extension != null) {
			IExtension[] extensions =  extension.getExtensions();
			int length = extensions.length;
			SearchParticipant[] participants = new SearchParticipant[length];
			int found = 0;
			for(int i = 0; i < extensions.length; i++){
				IConfigurationElement [] configElements = extensions[i].getConfigurationElements();
				for(int j = 0, configLength = configElements.length; j < configLength; j++){
					try {
						Object execExt = configElements[j].createExecutableExtension("class"); //$NON-NLS-1$
						if (execExt != null && execExt instanceof SearchParticipant){
							participants[found++] = (SearchParticipant)execExt;
						}
					} catch(CoreException e) {
					}
				}
			}	
			if (found == 0) return SearchParticipant.NO_PARTICIPANT;
			if (found < length) {
				System.arraycopy(participants, 0, participants = new SearchParticipant[found], 0, found);
			}
			return participants;
		}
		return SearchParticipant.NO_PARTICIPANT;
	}

	/**
	 * Returns the single instance of the Search core plug-in runtime class.
	 * 
	 * @return the single instance of the Search core plug-in runtime class
	 */
	public static Plugin getPlugin() {
		return SEARCH_CORE_PLUGIN;
	}
	
	/* (non-Javadoc)
	 * Method declared on IExecutableExtension.
	 * Record any necessary initialization data from the plugin.
	 */
	public void setInitializationData(
		IConfigurationElement cfig,
		String propertyName,
		Object data)
		throws CoreException {
	}
}
