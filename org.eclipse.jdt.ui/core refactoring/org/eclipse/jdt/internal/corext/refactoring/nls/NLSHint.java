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
package org.eclipse.jdt.internal.corext.refactoring.nls;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.core.dom.ITypeBinding;

/**
 * calculates hints for the nls-refactoring out of a compilation unit.
 * - package fragments of the accessor class and the resource bundle
 * - accessor class name, resource bundle name
 */
public class NLSHint {
	private ITypeBinding fAccessorClassBinding;
	private IPackageFragment fPackage;
	private String fResourceBundle;
	private IPackageFragment fResourceBundlePackage;

	public NLSHint(NLSSubstitution[] nlsSubstitution, ICompilationUnit cu, NLSInfo nlsInfo) {
		IPackageFragment defaultPackage= (IPackageFragment) cu.getAncestor(IJavaElement.PACKAGE_FRAGMENT);
		fPackage= defaultPackage;
		fResourceBundlePackage= defaultPackage;

		NLSElement nlsElement= findFirstNLSElementForHint(nlsSubstitution);

		if (nlsElement != null) {
			fAccessorClassBinding= nlsInfo.getAccessorClass(nlsElement);
			if (fAccessorClassBinding != null) {
				try {
					fPackage= nlsInfo.getPackageOfAccessorClass(fAccessorClassBinding);
					fResourceBundle= nlsInfo.getResourceBundle(fAccessorClassBinding, fPackage);
					fResourceBundlePackage= nlsInfo.getResourceBundlePackage(fResourceBundle);
				} catch (JavaModelException e) {
				}
			}
		}
	}

	public String getMessageClass() {
		if (fAccessorClassBinding != null) {
			return fAccessorClassBinding.getName();
		}
		return NLSRefactoring.DEFAULT_ACCESSOR_CLASSNAME;
	}

	public IPackageFragment getMessageClassPackage() {
		return fPackage;
	}

	public String getResourceBundle() {
		if (fResourceBundle != null) {
			return NLSInfo.getResourceNamePartHelper(fResourceBundle);
		}
		return NLSRefactoring.getDefaultPropertiesFilename();
	}

	public IPackageFragment getResourceBundlePackage() {
		return fResourceBundlePackage;
	}

	private NLSElement findFirstNLSElementForHint(NLSSubstitution[] nlsSubstitutions) {
		NLSSubstitution substitution;
		for (int i= 0; i < nlsSubstitutions.length; i++) {
			substitution= nlsSubstitutions[i];
			if ((substitution.getState() == NLSSubstitution.EXTERNALIZED) && !substitution.hasStateChanged()) {
				return substitution.getNLSElement();
			}
		}
		return null;
	}
}