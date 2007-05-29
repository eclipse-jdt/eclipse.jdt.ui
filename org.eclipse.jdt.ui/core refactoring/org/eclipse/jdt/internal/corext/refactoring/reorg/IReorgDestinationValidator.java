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
package org.eclipse.jdt.internal.corext.refactoring.reorg;

import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.IJavaElement;

public interface IReorgDestinationValidator {

	public boolean canChildrenBeDestinations(IResource resource);
	public boolean canChildrenBeDestinations(IJavaElement javaElement);
	public boolean canElementBeDestination(IResource resource);
	public boolean canElementBeDestination(IJavaElement javaElement);
}
