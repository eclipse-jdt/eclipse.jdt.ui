/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.junit;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.expressions.PropertyTester;

import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.junit.util.TestSearchEngine;

/**
 * JUnitPropertyTester provides propertyTester(s) for IResource types
 * for use in XML Expression Language syntax.
 */
public class JUnitPropertyTester extends PropertyTester {

	private static final String PROPERTY_IS_TEST= "isTest";	 //$NON-NLS-1$
	
	private static final String PROPERTY_CAN_LAUNCH_AS_JUNIT_TEST= "canLaunchAsJUnit"; //$NON-NLS-1$

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.participants.properties.IPropertyEvaluator#test(java.lang.Object, java.lang.String, java.lang.String)
	 */
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		IJavaElement element;
		if (receiver instanceof IJavaElement) {
			element= (IJavaElement) receiver;
		} else if (receiver instanceof IResource) {
			element = JavaCore.create((IResource) receiver);
			if (element == null) {
				return false;
			}
		} else {
			throw new IllegalArgumentException("Element must be of type 'IJavaElement' or 'IResource'"); //$NON-NLS-1$
		}
		if (PROPERTY_IS_TEST.equals(property)) { 
			return isJUnitTest(element);
		} else if (PROPERTY_CAN_LAUNCH_AS_JUNIT_TEST.equals(property)) {
			return canLaunchAsJUnitTest(element);
		}
		throw new IllegalArgumentException("Method must be 'isTest' method"); //$NON-NLS-1$
	}
	
	private boolean canLaunchAsJUnitTest(IJavaElement element) {
		switch (element.getElementType()) {
			case IJavaElement.JAVA_PROJECT:
			case IJavaElement.PACKAGE_FRAGMENT_ROOT:
			case IJavaElement.PACKAGE_FRAGMENT:
				return true; // can run, let test runner detect if there are tests
			case IJavaElement.COMPILATION_UNIT:
			case IJavaElement.CLASS_FILE:
			case IJavaElement.TYPE:
			case IJavaElement.METHOD:
				return isJUnitTest(element);
			default:
				return false;
		}
	}

	/*
	 * Return whether the target resource is a JUnit test.
	 */
	private boolean isJUnitTest(IJavaElement element) {
		try {
			IType testType= null;
			if (element instanceof ICompilationUnit) {
				testType= (((ICompilationUnit) element)).findPrimaryType();
			} else if (element instanceof IClassFile) {
				testType= (((IClassFile) element)).getType();
			} else if (element instanceof IType) {
				testType= (IType) element;
			} else if (element instanceof IMember) {
				testType= ((IMember) element).getDeclaringType();
			}
			if (testType != null && testType.exists()) {
				return TestSearchEngine.isTestOrTestSuite(testType);
			}
		} catch (CoreException e) {
			// ignore, return false
		}
		return false;
	}
}
