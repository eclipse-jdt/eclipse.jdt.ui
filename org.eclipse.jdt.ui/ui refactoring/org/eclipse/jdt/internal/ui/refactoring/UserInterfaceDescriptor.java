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


public class UserInterfaceDescriptor extends AbstractDescriptor {

	private static final String EXT_ID= "refactoringUserInterfaces"; //$NON-NLS-1$
	
	public UserInterfaceDescriptor(IConfigurationElement element) {
		super(element);
	}

	private static DescriptorManager fgDescriptions= new DescriptorManager(EXT_ID) {
		protected AbstractDescriptor createDescriptor(IConfigurationElement element) {
			return new UserInterfaceDescriptor(element);
		}
	};
	
	public static UserInterfaceDescriptor get(Object element) throws CoreException {
		return (UserInterfaceDescriptor)fgDescriptions.getDescriptor(element);
	}

	public UserInterfaceStarter create() throws CoreException {
		String starter= fConfigurationElement.getAttribute(CLASS);
		UserInterfaceStarter result= null;
		if (starter != null)
			result= (UserInterfaceStarter)fConfigurationElement.createExecutableExtension(CLASS);
		else
			result= new UserInterfaceStarter();
		result.initialize(fConfigurationElement);
		return result;
	}
}
