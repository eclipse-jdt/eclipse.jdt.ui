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
 *     IBM Corporation - Bug 565447, Bug 570690
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.Collections;
import java.util.Map;

/**
 * A fix that uses pattern matching for the instanceof expression when possible.
 */
public class PatternMatchingForInstanceofCleanUp extends AbstractCleanUpCoreWrapper<PatternMatchingForInstanceofCleanUpCore> {

	public PatternMatchingForInstanceofCleanUp(final Map<String, String> options) {
		super(options, new PatternMatchingForInstanceofCleanUpCore());
	}

	public PatternMatchingForInstanceofCleanUp() {
		this(Collections.EMPTY_MAP);
	}
}
