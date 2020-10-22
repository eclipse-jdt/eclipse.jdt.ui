/*******************************************************************************
 * Copyright (c) 2017, 2020 Igor Fedorenko.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Igor Fedorenko - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.unittest.junit.launcher;

/**
 * Launch configuration delegate for a JUnit test as a Java application with
 * advanced source lookup support.
 *
 * @provisional This is part of work in progress and can be changed, moved or
 *              removed without notice
 */
public class AdvancedJUnitLaunchConfigurationDelegate extends JUnitLaunchConfigurationDelegate {

	public AdvancedJUnitLaunchConfigurationDelegate() {
		super();
		allowAdvancedSourcelookup();
	}

}
