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
 * A fix that replaces the new instance syntax by curly brackets to create an array when possible:
 * <ul>
 * <li>It must be an initialization of a declaration,</li>
 * <li>The declaration must have the same type.</li>
 * </ul>
 */
public class ArrayWithCurlyCleanUp extends AbstractCleanUpCoreWrapper<ArrayWithCurlyCleanUpCore> {
	public ArrayWithCurlyCleanUp(final Map<String, String> options) {
		super(options, new ArrayWithCurlyCleanUpCore());
	}

	public ArrayWithCurlyCleanUp() {
		this(Collections.EMPTY_MAP);
	}
}
