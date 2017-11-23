/*******************************************************************************
 * Copyright (c) 2017 Igor Fedorenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Igor Fedorenko - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.junit.launcher;

/**
 * Launch configuration delegate for a JUnit test as a Java application with advanced source lookup
 * support.
 *
 * @since 3.10
 * @provisional This is part of work in progress and can be changed, moved or removed without notice
 */
public class AdvancedJUnitLaunchConfigurationDelegate extends JUnitLaunchConfigurationDelegate {

	public AdvancedJUnitLaunchConfigurationDelegate() {
		super();
		allowAdvancedSourcelookup();
	}

}
