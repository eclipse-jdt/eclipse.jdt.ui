/*******************************************************************************
 * Copyright (c) 2004 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.junit.ui;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.participants.xml.PropertyTester;
import org.eclipse.jdt.internal.junit.util.TestSearchEngine;

/**
 * Contributes an "isTest" property for ITypes.
 */
public class JavaTypeExtender extends PropertyTester  {
	private static final String IS_TEST= "isTest"; //$NON-NLS-1$
	/**
	 * @inheritDoc
	 */
	public boolean test(Object receiver, String method, Object[] args, Object expectedValue) {
		IType type= (IType)receiver;
		try {
			if (IS_TEST.equals(method)) 
				return TestSearchEngine.isTestOrTestSuite(type);
		} catch (JavaModelException e) {
			return false;
		}
		return false;
	}
}
