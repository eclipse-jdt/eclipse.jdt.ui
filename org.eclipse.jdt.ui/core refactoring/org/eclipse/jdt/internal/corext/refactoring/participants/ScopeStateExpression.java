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


public class ScopeStateExpression extends CompositeExpression {

	public ScopeStateExpression(IConfigurationElement element) {
		parse(element);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.participants.Expression#evaluate(java.lang.Object)
	 */
	public boolean evaluate(Object element) throws CoreException {
		if (fExpressions == null || fExpressions.size() == 0)
			return false;
		return evaluateAnd(element);
	}
	
	private void parse(IConfigurationElement root) {
		IConfigurationElement[] children= root.getChildren();
		for (int i= 0; i < children.length; i++) {
			String name= children[i].getName();
			if (NatureExpression.NAME.equals(name)) {
				add(new NatureExpression(children[i]));
			}
		}
	}
}
