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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.eclipse.jface.text.Assert;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.compiler.InvalidInputException;

import org.eclipse.jdt.core.dom.CompilationUnit;
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
	private NLSSubstitution[] fSubstitutions;

	public NLSHint(ICompilationUnit cu, CompilationUnit astRoot) {
		Assert.isNotNull(cu);
		Assert.isNotNull(astRoot);
		
		IPackageFragment cuPackage= (IPackageFragment) cu.getAncestor(IJavaElement.PACKAGE_FRAGMENT);

		fAccessorName= NLSRefactoring.DEFAULT_ACCESSOR_CLASSNAME;
		fAccessorPackage= cuPackage;
		fResourceBundleName= NLSRefactoring.getDefaultPropertiesFilename();
		fResourceBundlePackage= cuPackage;
		
		IJavaProject project= cu.getJavaProject();
		NLSLine[] lines= createRawLines(cu);
		
		AccessorClassReference firstAccessInfo= findFirstAccessorReference(lines, astRoot);
		
		Properties props= null;
		if (firstAccessInfo != null)
			props= NLSHintHelper.getProperties(project, firstAccessInfo.getBinding());
		
		if (props == null)
			props= new Properties();
		
		fSubstitutions= createSubstitutions(lines, props, astRoot);
		
		if (firstAccessInfo != null) {
			fAccessorName= firstAccessInfo.getName();
			ITypeBinding accessorClassBinding= firstAccessInfo.getBinding();
			
			try {
				IPackageFragment accessorPack= NLSHintHelper.getPackageOfAccessorClass(project, accessorClassBinding);
				if (accessorPack != null) {
					fAccessorPackage= accessorPack;
				}
				
				String fullBundleName= NLSHintHelper.getResourceBundleName(project, accessorClassBinding);
				if (fullBundleName != null) {
					fResourceBundleName= Signature.getSimpleName(fullBundleName) + NLSRefactoring.PROPERTY_FILE_EXT;
					String packName= Signature.getQualifier(fullBundleName);
					
					IPackageFragment pack= NLSHintHelper.getResourceBundlePackage(project, packName, fResourceBundleName);
					if (pack != null) {
						fResourceBundlePackage= pack;
					}
				}
			} catch (JavaModelException e) {
			}
		}
	}
	
	private NLSSubstitution[] createSubstitutions(NLSLine[] lines, Properties props, CompilationUnit astRoot) {
		List result= new ArrayList();
		
		for (int i= 0; i < lines.length; i++) {
			NLSElement[] elements= lines[i].getElements();
			for (int j= 0; j < elements.length; j++) {
				NLSElement nlsElement= elements[j];
				if (nlsElement.hasTag()) {
					AccessorClassReference accessorClassReference= NLSHintHelper.getAccessorClassReference(astRoot, nlsElement);
					if (accessorClassReference == null) {
						// no accessor class => not translated				        
						result.add(new NLSSubstitution(NLSSubstitution.IGNORED, stripQuotes(nlsElement.getValue()), nlsElement));
					} else {
						String key= stripQuotes(nlsElement.getValue());
						String value= props.getProperty(key);
						result.add(new NLSSubstitution(NLSSubstitution.EXTERNALIZED, key, value, nlsElement, accessorClassReference));
					}
				} else {
					result.add(new NLSSubstitution(NLSSubstitution.INTERNALIZED, stripQuotes(nlsElement.getValue()), nlsElement));
				}
			}
		}
		return (NLSSubstitution[]) result.toArray(new NLSSubstitution[result.size()]);
	}
	
	private static AccessorClassReference findFirstAccessorReference(NLSLine[] lines, CompilationUnit astRoot) {
		for (int i= 0; i < lines.length; i++) {
			NLSElement[] elements= lines[i].getElements();
			for (int j= 0; j < elements.length; j++) {
				NLSElement nlsElement= elements[j];
				if (nlsElement.hasTag()) {
					AccessorClassReference accessorClassReference= NLSHintHelper.getAccessorClassReference(astRoot, nlsElement);
					if (accessorClassReference != null) {
						return accessorClassReference;
					}
				}
			}
		}
		
		// try to find a access with missing //non-nls tag (bug 75155)
		for (int i= 0; i < lines.length; i++) {
			NLSElement[] elements= lines[i].getElements();
			for (int j= 0; j < elements.length; j++) {
				NLSElement nlsElement= elements[j];
				if (!nlsElement.hasTag()) {
					AccessorClassReference accessorClassReference= NLSHintHelper.getAccessorClassReference(astRoot, nlsElement);
					if (accessorClassReference != null) {
						return accessorClassReference;
					}
				}
			}
		}
		return null;
	}

	private static String stripQuotes(String str) {
		return str.substring(1, str.length() - 1);
	}

	private static NLSLine[] createRawLines(ICompilationUnit cu) {
		try {
			return NLSScanner.scan(cu);
		} catch (JavaModelException x) {
			return new NLSLine[0];
		} catch (InvalidInputException x) {
			return new NLSLine[0];
		}
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

	public NLSSubstitution[] getSubstitutions() {
		return fSubstitutions;
	}


}
