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
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPluginDescriptor;


public class ObjectStateExpression extends CompositeExpression {
	
	private IConfigurationElement fConfigElement;
	
	public static final String NAME= "objectState"; //$NON-NLS-1$
	
	private static final String ADAPTABLE= "adaptable"; //$NON-NLS-1$
	
	public ObjectStateExpression(IConfigurationElement element) {
		fConfigElement= element;
	}

	public TestResult evaluate(IScope scope) throws CoreException {
		String adaptable= fConfigElement.getAttribute(ADAPTABLE);
		Object element= scope.getDefaultVariable();
		if (element == null)
			return TestResult.FALSE;
		if (adaptable != null && element instanceof IAdaptable) {
			IPluginDescriptor pd= fConfigElement.getDeclaringExtension().getDeclaringPluginDescriptor();
			ClassLoader loader= pd.getPluginClassLoader();
			try {
				Class clazz= loader.loadClass(adaptable);
				element= ((IAdaptable)element).getAdapter(clazz);
			} catch (ClassNotFoundException e) {
				element= null;
			}
			if (element == null)
				return TestResult.FALSE;
			scope= new Scope(scope, element);
		}
		return evaluateAnd(scope);
	}
}
