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

import junit.framework.Test;

import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

public class SefTestSetup extends RefactoringTestSetup {
	
	private IPackageFragment fBaseTypes;
	private IPackageFragment fObjectTypes;
	private IPackageFragment fInvalid;
	
	public SefTestSetup(Test test) {
		super(test);
	}	
	
	protected void setUp() throws Exception {
		super.setUp();

		IPackageFragmentRoot root= getDefaultSourceFolder();
		
		fBaseTypes= root.createPackageFragment("base_in", true, null);
		fObjectTypes= root.createPackageFragment("object_in", true, null);
		fInvalid= root.createPackageFragment("invalid", true, null);
	}

	public IPackageFragment getBasePackage() {
		return fBaseTypes;
	}	

	public IPackageFragment getObjectPackage() {
		return fObjectTypes;
	}	

	public IPackageFragment getInvalidPackage() {
		return fInvalid;
	}	
}

