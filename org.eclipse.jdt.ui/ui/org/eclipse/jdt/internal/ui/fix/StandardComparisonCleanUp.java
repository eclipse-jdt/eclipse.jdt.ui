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
 * A fix that fixes <code>Comparable.compareTo()</code> usage:
 * <ul>
 * <li>The developer is not supposed to predict the <code>1</code> and <code>-1</code> values; them is supposed to get zero or a value lesser or greater than zero,</li>
 * <li>Beware! The behavior may change if you implement a custom comparator!</li>
 * </ul>
 */
public class StandardComparisonCleanUp extends AbstractCleanUpCoreWrapper<StandardComparisonCleanUpCore> {

	public StandardComparisonCleanUp(final Map<String, String> options) {
		super(options, new StandardComparisonCleanUpCore());
	}

	public StandardComparisonCleanUp() {
		this(Collections.EMPTY_MAP);
	}
}