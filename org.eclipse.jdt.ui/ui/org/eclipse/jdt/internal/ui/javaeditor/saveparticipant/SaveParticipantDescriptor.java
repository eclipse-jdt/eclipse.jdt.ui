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

import org.eclipse.core.runtime.Assert;

/**
 * Describes a save participant contribution.
 *
 * @since 3.3
 */
public final class SaveParticipantDescriptor {

	/** The listener */
	private final IPostSaveListener fPostSaveListener;
	/** The preference configuration block, if any */
	private final ISaveParticipantPreferenceConfiguration fPreferenceConfiguration;
	
	/**
	 * Creates a new descriptor which connects a {@link IPostSaveListener}
	 * with a default implementation of a {@link ISaveParticipantPreferenceConfiguration}.
	 * @param listener the listener
	 */
	SaveParticipantDescriptor(final IPostSaveListener listener) {
		this(listener, new AbstractSaveParticipantPreferenceConfiguration() {

			protected String getPostSaveListenerId() {
	            return listener.getId();
            }

			protected String getPostSaveListenerName() {
	            return listener.getName();
            }
			
		});
	}

	/**
	 * Creates a new descriptor which connects a {@link IPostSaveListener}
	 * with an {@link ISaveParticipantPreferenceConfiguration}.
	 * 
	 * @param listener the listener
	 * @param preferenceConfiguration preference configuration
	 */
	SaveParticipantDescriptor(IPostSaveListener listener, ISaveParticipantPreferenceConfiguration preferenceConfiguration) {
		Assert.isNotNull(listener);
		Assert.isNotNull(preferenceConfiguration);

		fPostSaveListener= listener;
		fPreferenceConfiguration= preferenceConfiguration;
	}

	/**
	 * Returns the post save listener of the described
	 * save participant
	 * 
	 * @return the listener
	 */
	public IPostSaveListener getPostSaveListener()  {
		return fPostSaveListener;
	}

	/**
	 * Returns the preference configuration of the described
	 * save participant.
	 * 
	 * @return the preference configuration
	 */
	public ISaveParticipantPreferenceConfiguration getPreferenceConfiguration() {
		return fPreferenceConfiguration;
	}

	/**
	 * Returns the identifier of the described save participant.
	 *
	 * @return the non-empty id of this descriptor
	 */
	public String getId() {
		return fPostSaveListener.getId();
	}
}
