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
import org.eclipse.core.runtime.IPluginDescriptor;

public class PropertyTesterDescriptor implements IPropertyTester {
	
	public static final int EXCHANGE= UNKNOWN + 1;
	
	private String fProperties;
	private IConfigurationElement fConfigElement;
	
	private static final String PROPERTIES= "properties"; //$NON-NLS-1$
	private static final String CLASS= "class";  //$NON-NLS-1$
	
	public PropertyTesterDescriptor(IConfigurationElement element) {
		fConfigElement= element;
		StringBuffer buffer= new StringBuffer(","); //$NON-NLS-1$
		String properties= element.getAttribute(PROPERTIES);
		for (int i= 0; i < properties.length(); i++) {
			char ch= properties.charAt(i);
			if (!Character.isWhitespace(ch))
				buffer.append(ch);
		}
		buffer.append(',');
		fProperties= buffer.toString();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.participants.properties.IPropertyEvaluator#test(java.lang.Object, java.lang.String, java.lang.String)
	 */
	public int test(Object o, String name, String value) {
		if (fProperties.indexOf("," + name + ",") == -1)  //$NON-NLS-1$//$NON-NLS-2$
			return UNKNOWN;
		IPluginDescriptor plugin= fConfigElement.getDeclaringExtension().getDeclaringPluginDescriptor();
		if (!plugin.isPluginActivated())
			return NOT_LOADED;
		return EXCHANGE;
	}
	
	public IPropertyTester create() throws CoreException {
		return (IPropertyTester)fConfigElement.createExecutableExtension(CLASS);
	}
}
