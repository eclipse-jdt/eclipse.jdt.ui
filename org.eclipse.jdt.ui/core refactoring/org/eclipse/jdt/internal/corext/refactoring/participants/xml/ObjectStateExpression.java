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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;


public class ObjectStateExpression extends CompositeExpression {
	
	private String fAdaptable;
	
	public static final String NAME= "objectState"; //$NON-NLS-1$
	
	private static final String ADAPTABLE= "adaptable"; //$NON-NLS-1$
	
	public ObjectStateExpression(IConfigurationElement element) {
		fAdaptable= element.getAttribute(ADAPTABLE);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.participants.Expression#evaluate(java.lang.Object)
	 */
	public int evaluate(Object element) throws CoreException {
//		if (fAdaptable != null) {
//			if (("*".equals(fAdaptable) || isInstanceOf(element, fAdaptable)) && (element instanceof IAdaptable)) //$NON-NLS-1$
//				element= ((IAdaptable)element).getAdapter(IResource.class); 
//		}
		if (element == null)
			return ITestResult.FALSE;
		return evaluateAnd(element);
	}
}
