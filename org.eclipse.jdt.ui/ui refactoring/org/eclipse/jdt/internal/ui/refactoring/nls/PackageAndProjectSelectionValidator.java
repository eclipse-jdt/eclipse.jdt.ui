/*****************************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others. All rights reserved. This program
 * and the accompanying materials are made available under the terms of the Common Public
 * License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ****************************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring.nls;

import org.eclipse.core.runtime.IPath;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.wizards.TypedElementSelectionValidator;

public class PackageAndProjectSelectionValidator extends TypedElementSelectionValidator {

	public PackageAndProjectSelectionValidator() {
		super(new Class[]{IPackageFragmentRoot.class}, false);
	}

	public boolean isSelectedValid(Object element) {
		try {
			if (element instanceof IJavaProject) {
				IJavaProject jproject= (IJavaProject)element;
				IPath path= jproject.getProject().getFullPath();
				return (jproject.findPackageFragmentRoot(path) != null);
			} else if (element instanceof IPackageFragmentRoot) {
				return (((IPackageFragmentRoot)element).getKind() == IPackageFragmentRoot.K_SOURCE);
			}
			return true;
		} catch (JavaModelException e) {
			// fall through returning false
		}
		return false;
	}
}