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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPluginRegistry;
import org.eclipse.core.runtime.Platform;

import org.eclipse.jdt.internal.corext.refactoring.participants.Expression;
import org.eclipse.jdt.internal.corext.refactoring.participants.ObjectStateExpression;

import org.eclipse.jdt.internal.ui.JavaPlugin;


public class StatusContextViewerDescriptor {
	
	private static StatusContextViewerDescriptor[] fgExtensions;
	private static final String EXT_ID= "statusContextViewers"; //$NON-NLS-1$
	
	private IConfigurationElement fConfigurationElement;

	private static final String ID= "id"; //$NON-NLS-1$
	private static final String CLASS= "class"; //$NON-NLS-1$
	
	private static final String OBJECT_STATE= "objectState"; //$NON-NLS-1$

	public static StatusContextViewerDescriptor get(Object element) throws CoreException {
		if (fgExtensions == null)
			initExtensions();
		for (int i= 0; i < fgExtensions.length; i++) {
			StatusContextViewerDescriptor descriptor= fgExtensions[i];
			if (descriptor.matches(element)) {
				return descriptor;
			}
		}
		return null;
	}

	public StatusContextViewerDescriptor(IConfigurationElement element) {
		fConfigurationElement= element;
	}
	
	public String getId() {
		return fConfigurationElement.getAttribute(ID);
	}
	
	public boolean matches(Object element) throws CoreException {
		IConfigurationElement objectState= fConfigurationElement.getChildren(OBJECT_STATE)[0];
		if (objectState != null) {
			Expression exp= new ObjectStateExpression(objectState);
			return exp.evaluate(element);
		}
		return true;
	}

	public IStatusContextViewer createViewer() throws CoreException {
		return (IStatusContextViewer)fConfigurationElement.createExecutableExtension(CLASS);
	}
	
	private static void initExtensions() {
		IPluginRegistry registry= Platform.getPluginRegistry();
		IConfigurationElement[] ces= registry.getConfigurationElementsFor(
			JavaPlugin.getPluginId(), 
			EXT_ID);
		fgExtensions= new StatusContextViewerDescriptor[ces.length];
		for (int i= 0; i < ces.length; i++) {
			fgExtensions[i]= new StatusContextViewerDescriptor(ces[i]);
		}
	}	
}
