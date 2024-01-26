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
 *     Red Hat Inc - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.util;

import org.eclipse.jdt.ui.cleanup.ICleanUp;

import org.eclipse.jdt.internal.ui.fix.IMultiFix;
import org.eclipse.jdt.internal.ui.fix.IMultiFixCore;

/**
 * A utility class for wrapping ICleanUp objects to be used by
 * classes expecting an ICleanUpCore
 *
 * @since 3.31
 */
public class CleanUpCoreWrapper {
	public static ICleanUp wrap(final IMultiFix var) {
		return var;
	}


	public static ICleanUp wrap(final ICleanUp var) {
		return var;
	}

	public static ICleanUp wrap(final IMultiFixCore var) {
		return var;
	}
}
