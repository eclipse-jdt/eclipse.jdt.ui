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
package org.eclipse.jdt.ui.tests.refactoring;

import java.util.Hashtable;

import junit.framework.Test;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

public class Java15Setup extends RefactoringTestSetup {

	public Java15Setup(Test test) {
		super(test);
	}

	protected void setUp() throws Exception {
		super.setUp();

		Hashtable options= JavaCore.getOptions();
		JavaProjectHelper.set15CompilerOptions(options);
		JavaCore.setOptions(options);
	}
}