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

import java.util.Collection;

import org.eclipse.core.runtime.IConfigurationElement;


public class CountExpression extends Expression {

	public static final String NAME= "count"; //$NON-NLS-1$
	
	private static final int ANY_NUMBER=	5;
	private static final int EXACT=			4;
	private static final int ONE_OR_MORE=	3;
	private static final int NONE_OR_ONE= 	2;
	private static final int NONE= 			1;
	private static final int UNKNOWN= 		0;
	
	private int fMode;
	private int fSize;
	
	public CountExpression(IConfigurationElement configElement) {
		String size = configElement.getAttribute(ATT_VALUE);
		if (size == null)
			size = "*"; //$NON-NLS-1$
		if (size.equals("*")) //$NON-NLS-1$
			fMode= ANY_NUMBER;
		else if (size.equals("?")) //$NON-NLS-1$
			fMode= NONE_OR_ONE;
		else if (size.equals("!")) //$NON-NLS-1$
			fMode= NONE;
		else if (size.equals("+")) //$NON-NLS-1$
			fMode= ONE_OR_MORE;
		else {
			try {
				fSize= Integer.parseInt(size);
				fMode= EXACT;
			} catch (NumberFormatException e) {
				fMode= UNKNOWN;
			}
		}
	}
	
	public TestResult evaluate(IVariablePool pool) throws ExpressionException {
		Object var= pool.getDefaultVariable();
		Expressions.checkCollection(var, this);
		Collection collection= (Collection)var;
		int size= collection.size();
		switch (fMode) {
			case UNKNOWN:
				return TestResult.FALSE;
			case NONE:
				return TestResult.valueOf(size == 0);
			case ONE_OR_MORE:
				return TestResult.valueOf(size >= 1);
			case EXACT:
				return TestResult.valueOf(fSize == size);
			case ANY_NUMBER:
				return TestResult.TRUE;
		}
		return TestResult.FALSE;
	}
}
