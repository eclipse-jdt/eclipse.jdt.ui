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
package org.eclipse.jdt.internal.corext.refactoring.participants;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;

import org.eclipse.jdt.internal.corext.refactoring.participants.xml.CompositeExpression;
import org.eclipse.jdt.internal.corext.refactoring.participants.xml.Expression;
import org.eclipse.jdt.internal.corext.refactoring.participants.xml.ExpressionParser;
import org.eclipse.jdt.internal.corext.refactoring.participants.xml.IElementHandler;
import org.eclipse.jdt.internal.corext.refactoring.participants.xml.IScope;
import org.eclipse.jdt.internal.corext.refactoring.participants.xml.TestResult;


public class ScopeStateExpression extends CompositeExpression {
	
	public static class XMLHandler implements IElementHandler {
		public Expression create(IConfigurationElement config, ExpressionParser creator) {
			String name= config.getName();
			if (NatureExpression.NAME.equals(name)) {
				return new NatureExpression(config);
			} else if (ScopeStateExpression.NAME.equals(name)) {
				ScopeStateExpression result= new ScopeStateExpression(config);
				creator.processChildren(result, config);
				return result;
			}
			return null;
		}
	}
	
	public static final String NAME= "scopeState"; //$NON-NLS-1$

	public ScopeStateExpression(IConfigurationElement element) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.participants.Expression#evaluate(java.lang.Object)
	 */
	public TestResult evaluate(IScope scope) throws CoreException {
		if (fExpressions == null || fExpressions.size() == 0)
			return TestResult.FALSE;
		return evaluateAnd(scope);
	}	
}
