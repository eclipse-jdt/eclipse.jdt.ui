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
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;

import org.eclipse.core.resources.IResource;


public class ObjectStateExpression extends CompositeExpression {
	
	private String fAdaptable;
	
	private static final String ADAPTABLE= "adaptable"; //$NON-NLS-1$
	
	public ObjectStateExpression(IConfigurationElement element) {
		fAdaptable= element.getAttribute(ADAPTABLE);
		parse(this, element);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.participants.Expression#evaluate(java.lang.Object)
	 */
	public boolean evaluate(Object element) throws CoreException {
		if (fAdaptable != null) {
			if (("*".equals(fAdaptable) || isInstanceOf(element, fAdaptable)) && (element instanceof IAdaptable)) //$NON-NLS-1$
				element= ((IAdaptable)element).getAdapter(IResource.class); 
		}
		return evaluateAnd(element);
	}

	private static void parse(CompositeExpression result, IConfigurationElement root) {
		IConfigurationElement[] children= root.getChildren();
		for (int i= 0; i < children.length; i++) {
			String name= children[i].getName();
			if (PropertyExpression.NAME.equals(name)) {
				result.add(new PropertyExpression(children[i]));
			} else if (OrExpression.NAME.equals(name)) {
				OrExpression expression= new OrExpression();
				result.add(expression);
				parse(expression, children[i]);
			} else if (AndExpression.NAME.equals(name)) {
				AndExpression expression= new AndExpression();
				result.add(expression);
				parse(expression, children[i]);
			}
		}
	}
}
