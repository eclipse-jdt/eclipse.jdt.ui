/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Microsoft Corporation - copied to jdt.core.manipulation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.participants;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

public class ResourceProcessors {

	public static String[] computeAffectedNatures(IResource resource) throws CoreException {
		IProject project= resource.getProject();
		Set<String> result= new HashSet<>();
		Set<IProject> visitedProjects= new HashSet<>();
		computeNatures(result, visitedProjects, project);
		return result.toArray(new String[result.size()]);
	}

	public static String[] computeAffectedNatures(IResource[] resources) throws CoreException {
		Set<String> result= new HashSet<>();
		Set<IProject> visitedProjects= new HashSet<>();
		for (IResource resource : resources) {
			computeNatures(result, visitedProjects, resource.getProject());
		}
		return result.toArray(new String[result.size()]);
	}

	private static void computeNatures(Set<String> result, Set<IProject> visitedProjects, IProject focus) throws CoreException {
		if (visitedProjects.contains(focus))
			return;
		String[] pns= focus.getDescription().getNatureIds();
		result.addAll(Arrays.asList(pns));
		visitedProjects.add(focus);
		for (IProject element : focus.getReferencingProjects()) {
			computeNatures(result, visitedProjects, element);
		}
	}

	private ResourceProcessors() {
	}
}
