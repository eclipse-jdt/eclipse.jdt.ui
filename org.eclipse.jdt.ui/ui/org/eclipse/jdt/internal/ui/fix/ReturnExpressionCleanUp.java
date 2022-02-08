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
 * A fix that removes unnecessary local variable declaration or unnecessary variable assignment before a return statement:
 * <ul>
 * <li>An explicit type is added for arrays.</li>
 * </ul>
 */
public class ReturnExpressionCleanUp extends AbstractCleanUpCoreWrapper<ReturnExpressionCleanUpCore> {

	public ReturnExpressionCleanUp(final Map<String, String> options) {
		super(options, new ReturnExpressionCleanUpCore());
	}

	public ReturnExpressionCleanUp() {
		this(Collections.EMPTY_MAP);
	}
}