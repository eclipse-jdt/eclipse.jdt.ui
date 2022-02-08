/*******************************************************************************
 * Copyright (c) 2020, 2022 Fabrice TIERCELIN and others.
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
 * A fix that replaces <code>if</code>/<code>else if</code>/<code>else</code> blocks to use <code>switch</code> where possible:
 * <ul>
 * <li>Convert to switch when there are more than two cases,</li>
 * <li>Do not convert if the discriminant can be null, that is to say only primitive and enum,</li>
 * <li>Do a variable conflict analyze.</li>
 * </ul>
 */
public class SwitchCleanUp extends AbstractCleanUpCoreWrapper<SwitchCleanUpCore> {

	public SwitchCleanUp(final Map<String, String> options) {
		super(options, new SwitchCleanUpCore());
	}

	public SwitchCleanUp() {
		this(Collections.EMPTY_MAP);
	}
}
