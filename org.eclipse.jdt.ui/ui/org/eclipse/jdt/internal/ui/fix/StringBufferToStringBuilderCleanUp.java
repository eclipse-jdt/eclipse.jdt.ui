/*******************************************************************************
 * Copyright (c) 2021, 2022 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.Collections;
import java.util.Map;

/**
 * A fix that replaces StringBuffer with StringBuilder.  The user can choose to do this for:
 * <ul>
 * <li>Only local usage where only private fields/methods or local variables are changed</li>
 * <li>All usages</li>
 * </ul>
 */
public class StringBufferToStringBuilderCleanUp extends AbstractCleanUpCoreWrapper<StringBufferToStringBuilderCleanUpCore> {

	public StringBufferToStringBuilderCleanUp(final Map<String, String> options) {
		super(options, new StringBufferToStringBuilderCleanUpCore());
	}

	public StringBufferToStringBuilderCleanUp() {
		this(Collections.EMPTY_MAP);
	}
}