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

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Preference UI for contributed the Java &gt; Editor &gt; Save Participants preference
 * page. A {@link SaveParticipantDescriptor} does connect an {@link IPostSaveListener}
 * with an {@link ISaveParticipantPreferenceConfiguration}.
 * <p>
 * Clients may implement this interface.
 * </p>
 * 
 * @since 3.3
 */
public interface ISaveParticipantPreferenceConfiguration {

	/**
	 * Creates the control that will be displayed on the Java &gt; Editor &gt; Save Participants 
	 * preference page.
	 *
	 * @param parent the parent composite to which to add the preferences control, not null
	 * @return the control that was added to <code>parent</code>, not null
	 */
	Control createControl(Composite parent);

	/**
	 * Called after creating the control. Implementations should load the
	 * preferences values and update the controls accordingly.
	 */
	void initialize();

	/**
	 * Called when the <code>OK</code> button is pressed on the preference
	 * page. Implementations should commit the configured preference settings
	 * into their form of preference storage.
	 */
	void performOk();

	/**
	 * Called when the <code>Defaults</code> button is pressed on the
	 * preference page. Implementation should reset any preference settings to
	 * their default values and adjust the controls accordingly.
	 */
	void performDefaults();

	/**
	 * Called when the preference page is being disposed. Implementations should
	 * free any resources they are holding on to.
	 */
	void dispose();

}
