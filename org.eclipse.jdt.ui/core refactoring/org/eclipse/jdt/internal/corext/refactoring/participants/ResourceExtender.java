/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.participants;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import org.eclipse.core.expressions.PropertyTester;


import org.eclipse.jdt.internal.ui.util.StringMatcher;


public class ResourceExtender extends PropertyTester {

	private static final String PROPERTY_MATCHES_PATTERN= "matchesPattern";	 //$NON-NLS-1$
	private static final String PROJECT_NATURE = "projectNature";	 //$NON-NLS-1$
	private static final String CAN_DELETE= "canDelete"; //$NON-NLS-1$
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.participants.properties.IPropertyEvaluator#test(java.lang.Object, java.lang.String, java.lang.String)
	 */
	public boolean test(Object receiver, String method, Object[] args, Object expectedValue) {
		IResource resource= (IResource)receiver;
		if (PROPERTY_MATCHES_PATTERN.equals(method)) { 
			String fileName= resource.getName();
			StringMatcher matcher= new StringMatcher((String)expectedValue, false, false);
			return matcher.match(fileName);
		} else if (PROJECT_NATURE.equals(method)) {
			try {
				IProject proj = resource.getProject();
				return proj.isAccessible() && proj.hasNature((String)expectedValue);
			} catch (CoreException e) {
				return false;		
			}
		} else if (CAN_DELETE.equals(method)) {
			return canDelete(resource);
		}
		Assert.isTrue(false);
		return false;
	}
	
	private boolean canDelete(IResource resource) {
		if (!resource.exists() || resource.isPhantom())
			return false;
		if (resource.getType() == IResource.ROOT || resource.getType() == IResource.PROJECT)
			return false;
		return true;
	}
}
