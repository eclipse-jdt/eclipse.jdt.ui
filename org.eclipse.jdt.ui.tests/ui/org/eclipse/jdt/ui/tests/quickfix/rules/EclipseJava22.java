/*******************************************************************************
 * Copyright (c) 2025 Carsten Hammer and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix.rules;

import org.eclipse.jdt.core.JavaCore;

public class EclipseJava22 extends AbstractEclipseJava {
	private static final String TESTRESOURCES_RTSTUBS_22_JAR= "testresources/rtstubs_22.jar"; //$NON-NLS-1$

	public EclipseJava22() {
		super(TESTRESOURCES_RTSTUBS_22_JAR, JavaCore.VERSION_22);
	}
}
