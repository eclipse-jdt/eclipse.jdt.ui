/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.preferences.cleanup;

import org.eclipse.jdt.internal.ui.fix.ICleanUp;
import org.eclipse.jdt.internal.ui.preferences.formatter.IModifyDialogTabPage;

public interface ICleanUpTabPage extends IModifyDialogTabPage {

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
	 * @return the maximum number of clean ups the user can select on this page
	 */
	public int getCleanUpCount();

	/**
	 * @return the number of selected clean ups at the moment
	 */
	public int getSelectedCleanUpCount();

}