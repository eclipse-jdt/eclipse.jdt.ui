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
 *     Red Hat Inc. - use AbstractCleanUpCoreWrapper to access core cleanup
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.Collections;
import java.util.Map;

/**
 * A fix that removes a String instance from a String literal.
 */
public class NoStringCreationCleanUp extends AbstractCleanUpCoreWrapper<NoStringCreationCleanUpCore> {
	public NoStringCreationCleanUp(final Map<String, String> options) {
		super(options, new NoStringCreationCleanUpCore());
	}

	public NoStringCreationCleanUp() {
		this(Collections.EMPTY_MAP);
	}
}
