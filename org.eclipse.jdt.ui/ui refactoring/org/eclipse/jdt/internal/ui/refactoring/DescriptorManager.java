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
package org.eclipse.jdt.internal.ui.refactoring;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPluginRegistry;
import org.eclipse.core.runtime.Platform;

import org.eclipse.jdt.internal.corext.refactoring.participants.xml.CompositeExpression;
import org.eclipse.jdt.internal.corext.refactoring.participants.xml.Expression;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public abstract class DescriptorManager {
	
	private String fExtensionPoint;
	private AbstractDescriptor[] fExtensions;

	public DescriptorManager(String extensionPoint) {
		fExtensionPoint= extensionPoint;
	}
	
	public AbstractDescriptor getDescriptor(Object element) throws CoreException {
		if (fExtensions == null)
			init();
			
		List candidates= new ArrayList(1);
		for (int i= 0; i < fExtensions.length; i++) {
			AbstractDescriptor descriptor= fExtensions[i];
			if (descriptor.matches(element)) {
				candidates.add(descriptor);
			} else {
				descriptor.clear();
			}
		}
		if (candidates.size() == 0)
			return null;
		if (candidates.size() == 1)
			return (AbstractDescriptor)candidates.get(0);
		AbstractDescriptor result= resolveConflict(candidates, element);
		for (Iterator iter= candidates.iterator(); iter.hasNext();) {
			((AbstractDescriptor)iter.next()).clear();
		}
		return result;
		
	}
	
	protected abstract AbstractDescriptor createDescriptor(IConfigurationElement element);
	
	//---- conflict resolving ---------------------------------------------
	
	protected static AbstractDescriptor resolveConflict(List candidates, Object element) {
		List instaceOfs= collectPropertyValues(candidates);
		int index= resolveConflict(instaceOfs, element.getClass());
		if (index == -1)
			return null;
		return (AbstractDescriptor)candidates.get(index);
	}
	
	private static int resolveConflict(List instanceOfs, Class clazz) {
		int length= instanceOfs.size();
		for (int i= 0; i < length; i++) {
			if (matches(clazz.getName(), (String[])instanceOfs.get(i)))
				return i;
		}
		int result= -1;
		Class superClass= clazz.getSuperclass();
		if (superClass != null && (result= resolveConflict(instanceOfs, superClass)) != -1)
			return result;
		Class[] interfaces= clazz.getInterfaces();
		for (int i= 0; i < interfaces.length; i++) {
			if ((result= resolveConflict(instanceOfs, interfaces[i])) != -1)
				return result;
		} 
		return result;
		
	}
	
	private static boolean matches(String type, String[] values) {
		for (int i= 0; i < values.length; i++) {
			if (type.equals(values[i]))
				return true;
		}
		return false;
	}
	
	private static List collectPropertyValues(List candidate) {
		List result= new ArrayList(candidate.size());
		for (Iterator iter= candidate.iterator(); iter.hasNext();) {
			AbstractDescriptor element= (AbstractDescriptor)iter.next();
			Expression exp= element.getExpression();
			if (exp instanceof CompositeExpression)
				result.add(((CompositeExpression)exp).getPropertyValues("instanceof")); //$NON-NLS-1$
		}
		return result;
	}
	
	// ---- extension point reading -----------------------------------
	
	private void init() {
		IPluginRegistry registry= Platform.getPluginRegistry();
		IConfigurationElement[] ces= registry.getConfigurationElementsFor(
			JavaPlugin.getPluginId(), 
			fExtensionPoint);
		fExtensions= new AbstractDescriptor[ces.length];
		for (int i= 0; i < ces.length; i++) {
			fExtensions[i]= createDescriptor(ces[i]);
		}
	}
}
