/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring.infra;

import junit.extensions.TestSetup;
import junit.framework.Test;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.testplugin.JavaProjectHelper;

public class AbstractRefactoringTestSetup extends TestSetup {

	private boolean fWasAutobuild;
	
	public AbstractRefactoringTestSetup(Test test) {
		super(test);
	}

	protected void setUp() throws Exception {
		super.setUp();
		fWasAutobuild= JavaProjectHelper.setAutoBuilding(false);
		if (JavaPlugin.getActivePage() != null)
			JavaPlugin.getActivePage().close();
	}
	
	protected void tearDown() throws Exception {
		JavaProjectHelper.setAutoBuilding(fWasAutobuild);
		super.tearDown();
	}
}
