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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPluginRegistry;
import org.eclipse.core.runtime.Platform;

import org.eclipse.jdt.internal.ui.JavaPlugin;


public class PropertyInterface implements IPropertyTester {
	
	private static final String EXT_POINT= "propertyTesters"; //$NON-NLS-1$
	private static final String TYPE= "type"; //$NON-NLS-1$
	private static final PropertyInterface[] EMPTY_ARRAY= new PropertyInterface[0];
	
	private static final Map fInterfaceMap= new HashMap();
	
	private static final PropertyInterface FIX_POINT= new PropertyInterface(null) {
		public int test(Object o, String name, String value) {
			return UNKNOWN;
		}	
	};
	
	private Class fType;
	private IPropertyTester[] fTesters;
	
	private PropertyInterface fExtends;
	private PropertyInterface[] fImplements;
	
	private PropertyInterface(Class type) {
		fType= type;
		fInterfaceMap.put(fType, this);
	}
	
	public static PropertyInterface get(Class clazz) {
		PropertyInterface result= (PropertyInterface)fInterfaceMap.get(clazz);
		if (result == null) {
			result= new PropertyInterface(clazz);
		}
		return result;
	}
	
	public int test(Object o, String name, String value) {
		if (fTesters == null)
			initialize();
		int result;
		for (int i= 0; i < fTesters.length; i++) {
			IPropertyTester evaluator= fTesters[i];
			if (evaluator == null)
				continue;
			result= evaluator.test(o, name, value);
			if (result <= NOT_LOADED)
				return result;
			if (result == PropertyTesterDescriptor.EXCHANGE) {
				try {
					fTesters[i]= evaluator= ((PropertyTesterDescriptor)evaluator).create();
					return evaluator.test(o, name, value);
				} catch (CoreException e) {
					fTesters[i]= null;
				}
			}
		}
		if (fExtends == null) {
			Class superClass= fType.getSuperclass();
			if (superClass != null) {
				fExtends= PropertyInterface.get(superClass);
			} else {
				fExtends= FIX_POINT;
			}
		}
		result= fExtends.test(o, name, value);
		if (result != UNKNOWN)
			return result;		
		if (fImplements == null) {
			Class[] interfaces= fType.getInterfaces();
			if (interfaces.length == 0) {
				fImplements= EMPTY_ARRAY;
			} else {
				fImplements= new PropertyInterface[interfaces.length];
				for (int i= 0; i < interfaces.length; i++) {
					fImplements[i]= PropertyInterface.get(interfaces[i]);
				}				
			}
		}
		for (int i= 0; i < fImplements.length; i++) {
			result= fImplements[i].test(o, name, value);
			if (result != UNKNOWN)
				return result;
		}
		return UNKNOWN;
	}
	
	private void initialize() {
		IPluginRegistry registry= Platform.getPluginRegistry();
		IConfigurationElement[] ces= registry.getConfigurationElementsFor(
			JavaPlugin.getPluginId(), 
			EXT_POINT); 
		String fTypeName= fType.getName();
		List result= new ArrayList(2);
		for (int i= 0; i < ces.length; i++) {
			IConfigurationElement config= ces[i];
			if (fTypeName.equals(config.getAttribute(TYPE)))
				result.add(new PropertyTesterDescriptor(config));
		}
		if (result.size() == 0)
			fTesters= EMPTY_ARRAY;
		else
			fTesters= (IPropertyTester[])result.toArray(new IPropertyTester[result.size()]);
	}
}
