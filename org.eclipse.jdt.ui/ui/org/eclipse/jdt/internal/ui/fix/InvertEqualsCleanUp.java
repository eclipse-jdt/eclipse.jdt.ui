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
 * A fix that inverts calls to <code>Object.equals(Object)</code> and <code>String.equalsIgnoreCase(String)</code>:
 * <ul>
 * <li>It avoids useless null pointer exception,</li>
 * <li>The caller must be nullable,</li>
 * <li>The parameter must not be nullable,</li>
 * <li>Beware! By avoiding null pointer exception, the behavior may change!</li>
 * </ul>
 */
public class InvertEqualsCleanUp extends AbstractCleanUpCoreWrapper<InvertEqualsCleanUpCore> {

	public InvertEqualsCleanUp(final Map<String, String> options) {
		super(options, new InvertEqualsCleanUpCore());
	}

	public InvertEqualsCleanUp() {
		this(Collections.EMPTY_MAP);
	}

}
