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
package org.eclipse.jdt.testplugin;

import junit.extensions.TestSetup;
import junit.framework.Test;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class TestOptionsSetup extends TestSetup {

	public TestOptionsSetup(Test test) {
		super(test);
	}

	protected void setUp() throws Exception {
		super.setUp();
		JavaCore.setOptions(TestOptions.getDefault());
		JavaPlugin.getDefault().getCodeTemplateStore().restoreDefaults();		
	}
}
