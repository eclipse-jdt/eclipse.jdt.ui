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

public class EnablementExpression extends CompositeExpression {

	public static final String NAME= "enablement"; //$NON-NLS-1$
	
	public EnablementExpression(IConfigurationElement configElement) {
	}
	
	/* (non-Javadoc)
	 * @see Expression#evaluate(IVariablePool)
	 */
	public TestResult evaluate(IVariablePool pool) throws CoreException {
		return evaluateAnd(pool);
	}
}
