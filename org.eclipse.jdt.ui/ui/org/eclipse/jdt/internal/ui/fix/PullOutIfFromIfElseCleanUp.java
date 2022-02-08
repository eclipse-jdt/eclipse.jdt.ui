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
 * A fix that moves an inner <code>if</code> condition around the outer <code>if</code> condition:
 * <ul>
 * <li>The inner <code>if</code> condition should be common to both <code>if</code>/<code>else</code> clauses of the outer <code>if</code> statement,</li>
 * <li>The <code>if</code> conditions should be passive.</li>
 * </ul>
 */
public class PullOutIfFromIfElseCleanUp extends AbstractCleanUpCoreWrapper<PullOutIfFromIfElseCleanUpCore> {

	public PullOutIfFromIfElseCleanUp(final Map<String, String> options) {
		super(options, new PullOutIfFromIfElseCleanUpCore());
	}

	public PullOutIfFromIfElseCleanUp() {
		this(Collections.EMPTY_MAP);
	}
}