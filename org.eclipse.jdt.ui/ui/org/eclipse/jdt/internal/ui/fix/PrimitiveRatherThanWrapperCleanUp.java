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
 * A fix that replaces a primitive wrapper object by the primitive type when an object is not necessary:
 * <ul>
 * <li>The variable must be not null,</li>
 * <li>The result should not make more autoboxing/unboxing than the original code.</li>
 * </ul>
 */
public class PrimitiveRatherThanWrapperCleanUp extends AbstractCleanUpCoreWrapper<PrimitiveRatherThanWrapperCleanUpCore> {

	public PrimitiveRatherThanWrapperCleanUp(final Map<String, String> options) {
		super(options, new PrimitiveRatherThanWrapperCleanUpCore());
	}

	public PrimitiveRatherThanWrapperCleanUp() {
		this(Collections.EMPTY_MAP);
	}
}