/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation and others.
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
 *     Red Hat Inc. - created by modifying LambdaExpressionsCleanUp
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.Collections;
import java.util.Map;

public class SwitchExpressionsCleanUp extends AbstractCleanUpCoreWrapper<SwitchExpressionsCleanUpCore> {

	public SwitchExpressionsCleanUp(final Map<String, String> options) {
		super(options, new SwitchExpressionsCleanUpCore());
	}

	public SwitchExpressionsCleanUp() {
		this(Collections.EMPTY_MAP);
	}
}
