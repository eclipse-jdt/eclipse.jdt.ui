/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.preferences.cleanup;

import org.eclipse.swt.widgets.Composite;

import org.eclipse.jdt.internal.ui.fix.CleanUpOptions;
import org.eclipse.jdt.internal.ui.fix.ICleanUp;

/**
 * @since 3.4
 */
public interface ICleanUpTabPage {

	/**
	 * The kind of clean up options this page will
	 * modify.
	 *
	 * @param kind the kind
	 *
	 * @see ICleanUp#DEFAULT_CLEAN_UP_OPTIONS
	 * @see ICleanUp#DEFAULT_SAVE_ACTION_OPTIONS
	 */
	public void setOptionsKind(int kind);

	/**
	 * The options to modify on this page.
	 * 
	 * @param options the options to modify
	 */
	public void setOptions(CleanUpOptions options);

	/**
	 * Create the contents of this tab page.
	 * 
	 * @param parent the parent composite
	 * @return created content control
	 */
	public Composite createContents(Composite parent);

	/**
	 * @return the maximum number of clean ups the user can select on this page
	 */
	public int getCleanUpCount();

	/**
	 * @return the number of selected clean ups at the moment
	 */
	public int getSelectedCleanUpCount();

	/**
	 * A code snippet which complies to the current settings.
	 * 
	 * @return A code snippet
	 */
	public String getPreview();
}