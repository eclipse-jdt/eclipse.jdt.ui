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
 *     Red Hat Inc. - modified to use PotentialProgrammingProblemsCleanUpCore
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.Collections;
import java.util.Map;

public class PotentialProgrammingProblemsCleanUp extends AbstractMultiFixCoreWrapper<PotentialProgrammingProblemsCleanUpCore> {

	public PotentialProgrammingProblemsCleanUp(final Map<String, String> options) {
		super(options, new PotentialProgrammingProblemsCleanUpCore());
	}

	public PotentialProgrammingProblemsCleanUp() {
		this(Collections.EMPTY_MAP);
	}
}
