/*******************************************************************************
 * Copyright (c) 2003 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.participants.xml;

import org.eclipse.core.resources.IFile;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;

public class FilePropertyTester extends PropertyTester {

	private static final String PROPERTY_IS_CU= "isCompilationUnit"; //$NON-NLS-1$
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.participants.properties.IPropertyEvaluator#test(java.lang.Object, java.lang.String, java.lang.String)
	 */
	public int test(Object element, String propertyName, String value) {
		if (!(element instanceof IFile))
			return UNKNOWN;
		IFile file= (IFile)element;
		if (PROPERTY_IS_CU.equals(propertyName)) {
			IJavaElement jElement= JavaCore.create(file);
			return testBoolean(value, jElement != null && jElement.exists() && jElement.getElementType() == IJavaElement.COMPILATION_UNIT);
		}
		return UNKNOWN;
	}
}
