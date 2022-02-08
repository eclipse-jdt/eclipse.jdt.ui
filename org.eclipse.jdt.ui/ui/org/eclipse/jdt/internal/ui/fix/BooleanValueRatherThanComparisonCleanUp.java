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
 * A fix that directly checks boolean values instead of comparing them with <code>true</code>/<code>false</code>:
 * <ul>
 * <li>The operator can be equals, not equals or XOR,</li>
 * <li>The constants can be a literal or a <code>java.lang.Boolean</code> constant,</li>
 * <li>One operand should be primitive so no new null pointer exceptions may occur,</li>
 * <li>We should never fix NPE as it may trigger zombie code.
 * A zombie code is a dead code that is dead because an error occurs before.
 * The day someone fixes the error, the zombie code comes back to life and alters the behavior.</li>
 * </ul>
 */
public class BooleanValueRatherThanComparisonCleanUp extends AbstractCleanUpCoreWrapper<BooleanValueRatherThanComparisonCleanUpCore> {

	public BooleanValueRatherThanComparisonCleanUp(final Map<String, String> options) {
		super(options, new BooleanValueRatherThanComparisonCleanUpCore());
	}

	public BooleanValueRatherThanComparisonCleanUp() {
		this(Collections.EMPTY_MAP);
	}
}
