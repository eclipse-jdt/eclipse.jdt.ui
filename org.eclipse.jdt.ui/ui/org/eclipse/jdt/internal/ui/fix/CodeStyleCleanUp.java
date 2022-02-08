/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat Inc. - modified to use CodeStyleCleanUpCore
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.Collections;
import java.util.Map;

/**
 * Creates fixes which can resolve code style issues
 * @see org.eclipse.jdt.internal.corext.fix.CodeStyleFix
 */
public class CodeStyleCleanUp extends AbstractMultiFixCoreWrapper<CodeStyleCleanUpCore> {

	public CodeStyleCleanUp(final Map<String, String> options) {
		super(options, new CodeStyleCleanUpCore());
	}

	public CodeStyleCleanUp() {
		this(Collections.EMPTY_MAP);
	}
}
