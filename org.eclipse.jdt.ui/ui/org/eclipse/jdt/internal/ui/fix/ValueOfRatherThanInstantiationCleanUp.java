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
 * A fix that replaces unnecessary primitive wrappers instance creations by using static factory <code>valueOf()</code> method:
 * <ul>
 * <li>It dramatically improves the space performance.</li>
 * </ul>
 */
public class ValueOfRatherThanInstantiationCleanUp extends AbstractCleanUpCoreWrapper<ValueOfRatherThanInstantiationCleanUpCore> {

	public ValueOfRatherThanInstantiationCleanUp(final Map<String, String> options) {
		super(options, new ValueOfRatherThanInstantiationCleanUpCore());
	}

	public ValueOfRatherThanInstantiationCleanUp() {
		this(Collections.EMPTY_MAP);
	}
}