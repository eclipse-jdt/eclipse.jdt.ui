/*******************************************************************************
 * Copyright (c) 2024 Carsten Hammer and others.
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

public class EclipseJava17 extends AbstractEclipseJava {
	private static final String TESTRESOURCES_RTSTUBS_17_JAR= "testresources/rtstubs_17.jar"; //$NON-NLS-1$

	public EclipseJava17() {
		super(TESTRESOURCES_RTSTUBS_17_JAR, JavaCore.VERSION_17);
	}
}
