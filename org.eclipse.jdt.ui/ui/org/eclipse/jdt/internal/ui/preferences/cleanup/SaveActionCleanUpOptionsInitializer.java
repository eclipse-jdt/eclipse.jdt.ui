/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.preferences.cleanup;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstantsOptions;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;

import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.cleanup.ICleanUpOptionsInitializer;


/**
 * The clean up initializer for save actions.
 *
 * @since 3.5
 */
public class SaveActionCleanUpOptionsInitializer implements ICleanUpOptionsInitializer {


	/*
	 * @see org.eclipse.jdt.ui.cleanup.ICleanUpOptionsInitializer#setDefaultOptions(org.eclipse.jdt.ui.cleanup.CleanUpOptions)
	 * @since 3.5
	 */
	@Override
	public void setDefaultOptions(CleanUpOptions options) {
		CleanUpConstantsOptions.setDefaultOptions(CleanUpConstants.DEFAULT_SAVE_ACTION_OPTIONS, options);
	}

}
