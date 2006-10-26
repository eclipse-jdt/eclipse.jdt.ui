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
 * Describes a save participant contribution. Connects an
 * {@link IPostSaveListener} with an {@link ISaveParticipantPreferenceConfiguration}.
 * If the described post save listener does not provide additional
 * preference UI then the preference configuration is null.
 * <p>
 * This class is not intended to be subclassed.
 * </p>
 *
 * @since 3.3
 */
public final class SaveParticipantDescriptor {

	/** The listener, not null */
	private final IPostSaveListener fPostSaveListener;
	/** The preference configuration block, if any */
	private final ISaveParticipantPreferenceConfiguration fPreferenceConfiguration;

	/**
	 * Creates a new descriptor.
	 * 
	 * @param listener the listener, not null
	 * @param preferenceConfiguration additional preference configuration UI or null
	 */
	SaveParticipantDescriptor(IPostSaveListener listener, ISaveParticipantPreferenceConfiguration preferenceConfiguration) {
		Assert.isNotNull(listener);

		fPostSaveListener= listener;
		fPreferenceConfiguration= preferenceConfiguration;
	}
	
	/**
	 * Returns the post save listener of the described
	 * save participant
	 * 
	 * @return the listener, not null
	 */
	public IPostSaveListener getPostSaveListener()  {
		return fPostSaveListener;
	}

	/**
	 * Returns the preference configuration of the described
	 * save participant. If the participant does not provide
	 * additional preference UI the result is null.
	 * 
	 * @return the preference configuration or null
	 */
	public ISaveParticipantPreferenceConfiguration getPreferenceConfiguration() {
		return fPreferenceConfiguration;
	}

	/**
	 * Returns the identifier of the described save participant.
	 *
	 * @return Returns the id, not null, not empty
	 */
	public String getId() {
		return fPostSaveListener.getId();
	}
}
