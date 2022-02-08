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
 * A fix that replaces the <code>compareTo()</code> method by a comparison on primitive:
 * <ul>
 * <li>It improves the space and time performance,</li>
 * <li>The compared value must be a primitive.</li>
 * </ul>
 */
public class PrimitiveComparisonCleanUp extends AbstractCleanUpCoreWrapper<PrimitiveComparisonCleanUpCore> {

	public PrimitiveComparisonCleanUp(final Map<String, String> options) {
		super(options, new PrimitiveComparisonCleanUpCore());
	}

	public PrimitiveComparisonCleanUp() {
		this(Collections.EMPTY_MAP);
	}
}
