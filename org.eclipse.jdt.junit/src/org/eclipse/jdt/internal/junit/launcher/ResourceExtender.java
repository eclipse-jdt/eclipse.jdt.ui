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
package org.eclipse.jdt.internal.junit.launcher;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.junit.util.TestSearchEngine;

/**
 * ResourceExtender provides propertyTester(s) for IResource types
 * for use in XML Expression Language syntax.
 */
public class ResourceExtender extends PropertyTester {

	private static final String PROPERTY_IS_TEST= "isTest";	 //$NON-NLS-1$

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.participants.properties.IPropertyEvaluator#test(java.lang.Object, java.lang.String, java.lang.String)
	 */
	public boolean test(Object receiver, String method, Object[] args, Object expectedValue) {
		IResource resource= (IResource)receiver;
		if (PROPERTY_IS_TEST.equals(method)) { //$NON-NLS-1$
			return isJUnitTest(resource);
		}
		Assert.isTrue(false);
		return false;
	}
	
	/*
	 * Return wether the target resource is a JUnit test.
	 */
	private boolean isJUnitTest(IResource target) {
		if (target != null) {
			IJavaElement element = JavaCore.create(target);
			if (element instanceof ICompilationUnit) {
				ICompilationUnit cu = (ICompilationUnit) element;
				IType mainType= cu.getType(Signature.getQualifier(cu.getElementName()));
				try {
					return TestSearchEngine.isTestOrTestSuite(mainType);
				} catch (JavaModelException e) {
					return false;
				}
			} 
		}
		return false;
	}
}
