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
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPluginDescriptor;

public class AdaptExpression extends CompositeExpression {

	public static final String NAME= "adapt"; //$NON-NLS-1$
	
	private static final String ATT_TYPE= "type"; //$NON-NLS-1$
	
	private String fTypeName;
	private Class fType;

	private IPluginDescriptor fPluginDescriptor;
	
	public AdaptExpression(IConfigurationElement configElement) throws CoreException {
		fTypeName= configElement.getAttribute(ATT_TYPE);
		checkAttribute(ATT_TYPE, fTypeName);
		fPluginDescriptor= configElement.getDeclaringExtension().getDeclaringPluginDescriptor();
	}
	
	/* (non-Javadoc)
	 * @see Expression#evaluate(IVariablePool)
	 */
	public TestResult evaluate(IVariablePool pool) throws CoreException {
		if (fTypeName == null)
			return TestResult.FALSE;
		Object var= pool.getDefaultVariable();
		if (!(var instanceof IAdaptable))
			return TestResult.FALSE;
		
		if (fType == null) {
			ClassLoader loader= fPluginDescriptor.getPluginClassLoader();
			try {
				fType= loader.loadClass(fTypeName);
			} catch (ClassNotFoundException e) {
				fTypeName= null;
				return TestResult.FALSE;
			}
		}
		Object adapted= ((IAdaptable)var).getAdapter(fType);
		if (adapted == null)
			return TestResult.FALSE;
		return evaluateAnd(new DefaultVariable(pool, adapted));
	}
}
