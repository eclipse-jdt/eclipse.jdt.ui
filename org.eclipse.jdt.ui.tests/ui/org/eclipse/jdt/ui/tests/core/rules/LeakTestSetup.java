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
package org.eclipse.jdt.ui.tests.core.rules;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.intro.IIntroManager;

/**
 *
 */
public class LeakTestSetup extends JUnitSourceSetup {

	private static LeakTestSetup fgCurrentSetup= null;

	public LeakTestSetup() {
		super();
		if (fgCurrentSetup == null) {
			fgCurrentSetup= this;
		}
	}

	@Override
	public void before() throws Throwable {
		super.before();
		if (fgCurrentSetup != this) {
			return;
		}
		IIntroManager introManager= PlatformUI.getWorkbench().getIntroManager();
		introManager.closeIntro(introManager.getIntro());
	}

	@Override
	public void after() {
		super.after();
		if (fgCurrentSetup != this) {
			return;
		}
		// nothing done at the moment
	}
}
