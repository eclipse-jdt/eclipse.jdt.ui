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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.internal.corext.Assert;

import org.eclipse.jdt.internal.ui.util.StringMatcher;


public class ResourceExtender extends TypeExtender {

	private static final String PROPERTY_MATCHES_PATTERN= "matchesPattern";	 //$NON-NLS-1$
	private static final String PROJECT_NATURE = "projectNature";	 //$NON-NLS-1$
	private static final String CAN_DELETE= "canDelete"; //$NON-NLS-1$
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.participants.properties.IPropertyEvaluator#test(java.lang.Object, java.lang.String, java.lang.String)
	 */
	public Object invoke(Object receiver, String method, Object[] args) {
		IResource resource= (IResource)receiver;
		if (PROPERTY_MATCHES_PATTERN.equals(method)) { //$NON-NLS-1$
			String fileName= resource.getName();
			StringMatcher matcher= new StringMatcher((String)args[0], false, false);
			return Boolean.valueOf(matcher.match(fileName));
		} else if (PROJECT_NATURE.equals(method)) {
			try {
				IProject proj = resource.getProject();
				return Boolean.valueOf(proj.isAccessible() && proj.hasNature((String)args[0]));
			} catch (CoreException e) {
				return Boolean.FALSE;		
			}
		} else if (CAN_DELETE.equals(method)) {
			return Boolean.valueOf(canDelete(resource));
		}
		Assert.isTrue(false);
		return null;
	}
	
	private boolean canDelete(IResource resource) {
		if (!resource.exists() || resource.isPhantom())
			return false;
		if (resource.getType() == IResource.ROOT || resource.getType() == IResource.PROJECT)
			return false;
		return true;
	}
}
