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
	
	private String fAccessorName;
	private IPackageFragment fAccessorPackage;
	private String fResourceBundleName;
	private IPackageFragment fResourceBundlePackage;

	public NLSHint(NLSSubstitution[] nlsSubstitution, ICompilationUnit cu, NLSInfo nlsInfo) {
		IPackageFragment cuPackage= (IPackageFragment) cu.getAncestor(IJavaElement.PACKAGE_FRAGMENT);

		fAccessorName= NLSRefactoring.DEFAULT_ACCESSOR_CLASSNAME;
		fAccessorPackage= cuPackage;
		fResourceBundleName= NLSRefactoring.getDefaultPropertiesFilename();
		fResourceBundlePackage= cuPackage;
		
		NLSElement nlsElement= findFirstNLSElementForHint(nlsSubstitution);
		if (nlsElement != null) {
			ITypeBinding accessorClassBinding= nlsInfo.getAccessorClass(nlsElement);
			if (accessorClassBinding != null) {
				fAccessorName= accessorClassBinding.getName();
				
				try {
					IPackageFragment accessorPack= nlsInfo.getPackageOfAccessorClass(accessorClassBinding);
					if (accessorPack != null) {
						fAccessorPackage= accessorPack;
					}
					
					String fullBundleName= nlsInfo.getResourceBundle(accessorClassBinding, fAccessorPackage);
					if (fullBundleName != null) {
						fResourceBundleName= NLSInfo.getResourceNamePartHelper(fullBundleName);
						IPackageFragment bundlePack= nlsInfo.getResourceBundlePackage(fullBundleName);
						if (bundlePack != null) {
							fResourceBundlePackage= bundlePack;
						}
					}
				} catch (JavaModelException e) {
				}
			}
		}
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
	

	public String getAccessorClassName() {
		return fAccessorName;
	}

	public IPackageFragment getAccessorClassPackage() {
		return fAccessorPackage;
	}

	public String getResourceBundleName() {
		return fResourceBundleName;
	}

	public IPackageFragment getResourceBundlePackage() {
		return fResourceBundlePackage;
	}


}