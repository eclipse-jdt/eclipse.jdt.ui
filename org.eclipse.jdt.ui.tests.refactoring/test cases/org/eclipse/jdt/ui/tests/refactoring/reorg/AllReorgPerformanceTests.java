/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring.reorg;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	RenamePackagePerfTests1.class,
	RenamePackagePerfTests2.class,

	RenameTypePerfAcceptanceTests.class,
	RenameTypePerfTests1.class,
	RenameTypePerfTests2.class,

	RenameMethodPerfTests1.class,
	RenameMethodPerfTests2.class,
	RenameMethodWithOverloadPerfTests.class,

	MoveCompilationUnitPerfTests1.class,
	MoveCompilationUnitPerfTests2.class,

	MoveStaticMembersPerfTests1.class,
	MoveStaticMembersPerfTests2.class,

	IntroduceIndirectionPerfAcceptanceTests.class
})
public class AllReorgPerformanceTests {
}