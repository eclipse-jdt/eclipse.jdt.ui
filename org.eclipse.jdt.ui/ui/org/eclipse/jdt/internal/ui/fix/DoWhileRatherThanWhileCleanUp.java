/*******************************************************************************
 * Copyright (c) 2021, 2022 Fabrice TIERCELIN and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Fabrice TIERCELIN - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.Collections;
import java.util.Map;

/**
 * A fix that replaces <code>while</code> by <code>do</code>/<code>while</code>:
 * <ul>
 * <li>The first evaluation must be always true.</li>
 * <li>The first evaluation must be passive.</li>
 * </ul>
 */
public class DoWhileRatherThanWhileCleanUp extends AbstractCleanUpCoreWrapper<DoWhileRatherThanWhileCleanUpCore> {

	public DoWhileRatherThanWhileCleanUp(final Map<String, String> options) {
		super(options, new DoWhileRatherThanWhileCleanUpCore());
	}

	public DoWhileRatherThanWhileCleanUp() {
		this(Collections.EMPTY_MAP);
	}
}
