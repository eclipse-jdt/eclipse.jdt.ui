/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.participants.xml;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;


public class TestNotExpression extends TestExpression {

	public static final String NAME= "testNot"; //$NON-NLS-1$
	
	public TestNotExpression(IConfigurationElement element) {
		super(element);
	}
	
	/* (non-Javadoc)
	 * @see Expression#evaluate(java.lang.Object)
	 */
	public int evaluate(Object element) throws CoreException {
		return TestResult.not(super.evaluate(element));
	}
}
