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
package org.eclipse.jdt.internal.corext.refactoring.participants.properties;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;

public class PropertyInterfaceManager {
	private Map fInterfaces;

	private static PropertyInterfaceManager fInstance;

	public PropertyInterfaceManager() {
		fInterfaces= new HashMap(50);
	}
	
	public static PropertyInterface getInterface(Object object) {
		if (fInstance == null)
			fInstance= new PropertyInterfaceManager();
		return fInstance.internalGetInterface(object.getClass());
	}

	public static PropertyInterface getInterface(Class clazz) {
		if (fInstance == null)
			fInstance= new PropertyInterfaceManager();
		return fInstance.internalGetInterface(clazz);
	}

	private PropertyInterface internalGetInterface(Class clazz) {
		String className= clazz.getName();
		Object o= fInterfaces.get(className);
		if (o == null) {
			// we can optimize this. This is only needed if we
			// have supertypes for which a property evaluator is
			// registered.
			PropertyInterface result= new PropertyInterface();
			fInterfaces.put(className, result);
			return result;
		}
		if (o instanceof PropertyInterface)
			return (PropertyInterface)o;
		IConfigurationElement element= (IConfigurationElement)o;
		if (!element.getDeclaringExtension().getDeclaringPluginDescriptor().isPluginActivated())
			return PropertyInterface.LAZY;
		try {
			IPropertyEvaluator evaluator= (IPropertyEvaluator)element.createExecutableExtension("class"); //$NON-NLS-1$
			PropertyInterface result= new PropertyInterface(evaluator);
			fInterfaces.put(className, result);
			return result;
		} catch (CoreException e) {
			fInterfaces.put(className, null);
		}
		return null;
	}
}
