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
 * A fix that merges consecutive <code>if</code> statements with same code block that end with a jump statement:
 * <ul>
 * <li>It collapses 5 <code>if</code> statements maximum.</li>
 * </ul>
 */
public class OneIfRatherThanDuplicateBlocksThatFallThroughCleanUp extends AbstractCleanUpCoreWrapper<OneIfRatherThanDuplicateBlocksThatFallThroughCleanUpCore> {
	public OneIfRatherThanDuplicateBlocksThatFallThroughCleanUp(final Map<String, String> options) {
		super(options, new OneIfRatherThanDuplicateBlocksThatFallThroughCleanUpCore());
	}

	public OneIfRatherThanDuplicateBlocksThatFallThroughCleanUp() {
		this(Collections.EMPTY_MAP);
	}
}
