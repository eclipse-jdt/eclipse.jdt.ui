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
	
	private IConfigurationElement fElement;
	
	public static final String NAME= "objectState"; //$NON-NLS-1$
	
	private static final String ADAPTABLE= "adaptable"; //$NON-NLS-1$
	
	public ObjectStateExpression(IConfigurationElement element) {
		fElement= element;
	}

	public int evaluate(Object element) throws CoreException {
		String adaptable= fElement.getAttribute(ADAPTABLE);
		if (adaptable != null && element instanceof IAdaptable) {
			IPluginDescriptor pd= fElement.getDeclaringExtension().getDeclaringPluginDescriptor();
			ClassLoader loader= pd.getPluginClassLoader();
			try {
				Class clazz= loader.loadClass(adaptable);
				element= ((IAdaptable)element).getAdapter(clazz);
			} catch (ClassNotFoundException e) {
				element= null;
			}
		}
		if (element == null)
			return ITestResult.FALSE;
		return evaluateAnd(element);
	}
}
