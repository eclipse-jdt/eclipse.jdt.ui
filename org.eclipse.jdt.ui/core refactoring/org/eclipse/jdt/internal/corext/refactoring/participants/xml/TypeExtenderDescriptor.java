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

import org.eclipse.jdt.internal.corext.Assert;

/* package */ class TypeExtenderDescriptor implements ITypeExtender {
	
	private String fProperties;
	private IConfigurationElement fConfigElement;
	
	private static final String METHODS= "methods"; //$NON-NLS-1$
	private static final String CLASS= "class";  //$NON-NLS-1$
	
	public TypeExtenderDescriptor(IConfigurationElement element) {
		fConfigElement= element;
		StringBuffer buffer= new StringBuffer(","); //$NON-NLS-1$
		String properties= element.getAttribute(METHODS);
		for (int i= 0; i < properties.length(); i++) {
			char ch= properties.charAt(i);
			if (!Character.isWhitespace(ch))
				buffer.append(ch);
		}
		buffer.append(',');
		fProperties= buffer.toString();
	}
	
	public String getProperties() {
		return fProperties;
	}
	
	public boolean handles(String property) {
		return fProperties.indexOf("," + property + ",") != -1;  //$NON-NLS-1$//$NON-NLS-2$
	}
	
	public boolean isLoaded() {
		return false;
	}
	
	public boolean canLoad() {
		IPluginDescriptor plugin= fConfigElement.getDeclaringExtension().getDeclaringPluginDescriptor();
		return plugin.isPluginActivated();
	}
	
	public Object perform(Object receiver, String method, Object[] args) {
		Assert.isTrue(false);
		return null;
	}
	
	public TypeExtender create() throws CoreException {
		return (TypeExtender)fConfigElement.createExecutableExtension(CLASS);
	}
}
