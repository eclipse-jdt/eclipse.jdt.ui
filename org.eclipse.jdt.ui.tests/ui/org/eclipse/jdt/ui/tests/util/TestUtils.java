/*******************************************************************************
 * Copyright (c) 2026 Simeon Andreev and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Simeon Andreev - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.util;

import org.eclipse.jdt.internal.core.JavaModelManager;

public class TestUtils {


	public static void waitForIndexer() {
		JavaModelManager.getIndexManager().waitForIndex(false, null);
	}
}
