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

import org.eclipse.core.runtime.IConfigurationElement;

public class WithExpression extends CompositeExpression {

	public static final String NAME= "with"; //$NON-NLS-1$
	
	private String fVariable;
	private static final String ATT_VARIABLE= "variable";  //$NON-NLS-1$
	
	public WithExpression(IConfigurationElement configElement) throws ExpressionException {
		fVariable= configElement.getAttribute(ATT_VARIABLE);
		Expressions.checkAttribute(ATT_VARIABLE, fVariable);
	}
	
	public TestResult evaluate(IVariablePool pool) throws ExpressionException {
		return evaluateAnd(new VariablePool(pool, pool.getVariable(fVariable)));
	}
}
