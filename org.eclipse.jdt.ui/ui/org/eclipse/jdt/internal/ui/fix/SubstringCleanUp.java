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
 *     Red Hat - moved implementation to jdt.core.manipulation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.Collections;
import java.util.Map;

/**
 * A fix that removes the second <code>substring()</code> parameter if this parameter is the length of the string:
 * <ul>
 * <li>It must reference the same expression,</li>
 * <li>The expression must be passive.</li>
 * </ul>
 */
public class SubstringCleanUp extends AbstractMultiFixCoreWrapper<SubstringCleanUpCore> {

	public SubstringCleanUp(final Map<String, String> options) {
		super(options, new SubstringCleanUpCore());
	}

	public SubstringCleanUp() {
		this(Collections.EMPTY_MAP);
	}
}
