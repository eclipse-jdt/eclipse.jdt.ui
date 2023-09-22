/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
 *     Red Hat Inc. - copied from AbstractSaveParticipantPreferenceConfiguration and CleanUpPostSaveListener
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javaeditor.saveparticipant;

/**
 * Constant important to proposals
 *
 * @since 1.20
 */
public interface SaveParticipantPreferenceConfigurationConstants {

	/**
     * Preference prefix that is prepended to the id of {@link SaveParticipantDescriptor save participants}.
     *
     * <p>
     * Value is of type <code>Boolean</code>.
     * </p>
     *
     * @see SaveParticipantDescriptor
     * @since 1.20
     */
    public static final String EDITOR_SAVE_PARTICIPANT_PREFIX= "editor_save_participant_";  //$NON-NLS-1$

    /**
     * Constant originally found in CleanUpPostSaveListener
     *
     * @since 1.20
     */
	public static final String POSTSAVELISTENER_ID= "org.eclipse.jdt.ui.postsavelistener.cleanup"; //$NON-NLS-1$

}
