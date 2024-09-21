/*******************************************************************************
 * Copyright (c) 2024 Erik Brangs and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Erik Brangs - initial implementation
 *******************************************************************************/

package org.eclipse.jdt.junit.launcher;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({
AdvancedJUnitLaunchConfigurationDelegateTest.class,
})
public class JUnitLauncherTests {

}
