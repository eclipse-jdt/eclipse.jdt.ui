/*******************************************************************************
 * Copyright (c) 2020, 2023 Fabrice TIERCELIN and others.
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
 *     Red Hat Inc. - modified to wrapper VarCleanUpCore
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.Collections;
import java.util.Map;

/**
 * A fix that uses the Local variable type inference:
 * <ul>
 * <li>As of Java 10, if a variable is initialized by an explicit type value, it can be declared
 * using the <code>var</code> keyword.</li>
 * </ul>
 * @see org.eclipse.jdt.internal.ui.fix.VarCleanUpCore
 */
public class VarCleanUp extends AbstractMultiFixCoreWrapper<VarCleanUpCore> {

	public VarCleanUp(final Map<String, String> options) {
		super(options, new VarCleanUpCore());
	}

	public VarCleanUp() {
		this(Collections.EMPTY_MAP);
	}
}

