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
package org.eclipse.jdt.internal.corext.refactoring.participants;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

public class ResourceProcessors {

	public static IProject[] computeScope(IResource resource) {
		IProject project= resource.getProject();
		Set result= new HashSet();
		computeScope(result, project);
		return (IProject[])result.toArray(new IProject[result.size()]);
	}
	
	public static IProject[] computeScope(IResource[] resources) {
		Set result= new HashSet();
		for (int i= 0; i < resources.length; i++) {
			computeScope(result, resources[i].getProject());
		}
		return (IProject[])result.toArray(new IProject[result.size()]);
	}
	
	private static void computeScope(Set result, IProject focus) {
		if (result.contains(focus))
			return;
		result.add(focus);
		IProject[] referencing= focus.getReferencingProjects();
		for (int i= 0; i < referencing.length; i++) {
			computeScope(result, referencing[i]);
		}
	}
}
