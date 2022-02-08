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
 * A fix that removes the comparator declaration if it is the default one:
 * <ul>
 * <li>The declared comparator should be an equivalent to the natural order,</li>
 * <li>Apply on anonymous class, lambda, <code>Comparator.comparing()</code>, <code>Comparator.naturalOrder()</code> and null,</li>
 * <li>Apply on <code>List.sort(Comparator)</code>, <code>Collections.sort(List, Comparator)</code>, <code>Collections.max(Collection, Comparator)</code> and <code>Collections.min(Collection, Comparator)</code>,</li>
 * <li>If the comparator is used in the method <code>List.sort(Comparator)</code>, the method is converted into <code>Collections.sort(List)</code>.</li>
 * </ul>
 */
public class RedundantComparatorCleanUp extends AbstractCleanUpCoreWrapper<RedundantComparatorCleanUpCore> {

	public RedundantComparatorCleanUp(final Map<String, String> options) {
		super(options, new RedundantComparatorCleanUpCore());
	}

	public RedundantComparatorCleanUp() {
		this(Collections.EMPTY_MAP);
	}
}