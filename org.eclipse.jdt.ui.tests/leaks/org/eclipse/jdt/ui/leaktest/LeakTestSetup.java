/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.leaktest;

import junit.extensions.TestSetup;
import junit.framework.Test;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.intro.IIntroManager;

/**
 *
 */
public class LeakTestSetup extends TestSetup {

	private static LeakTestSetup fgCurrentSetup= null;

	public LeakTestSetup(Test test) {
		super(test);
		if (fgCurrentSetup == null) {
			fgCurrentSetup= this;
		}
	}

	protected void setUp() throws Exception {
		if (fgCurrentSetup != this) {
			return;
		}
		IIntroManager introManager= PlatformUI.getWorkbench().getIntroManager();
		introManager.closeIntro(introManager.getIntro());
	}

	protected void tearDown() throws Exception {
		if (fgCurrentSetup != this) {
			return;
		}
		// nothing done at the moment
	}
}
