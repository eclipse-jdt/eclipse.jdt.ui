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
 * A fix that replaces a String concatenation to a Java 15 Text Block.
 */
public class StringConcatToTextBlockCleanUp extends AbstractCleanUpCoreWrapper<StringConcatToTextBlockCleanUpCore> {

	public StringConcatToTextBlockCleanUp(final Map<String, String> options) {
		super(options, new StringConcatToTextBlockCleanUpCore());
	}

	public StringConcatToTextBlockCleanUp() {
		this(Collections.EMPTY_MAP);
	}
}
