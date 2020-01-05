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

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;

import org.eclipse.jdt.internal.corext.util.JdtFlags;

/**
 * Utility class to deal with Java element processors.
 */
public class JavaProcessors {

	public static String[] computeAffectedNatures(IJavaElement element) throws CoreException {
		if (element instanceof IMember) {
			IMember member= (IMember)element;
			if (JdtFlags.isPrivate(member)) {
				return element.getJavaProject().getProject().getDescription().getNatureIds();
			}
		}
		IJavaProject project= element.getJavaProject();
		return ResourceProcessors.computeAffectedNatures(project.getProject());
	}

	public static String[] computeAffectedNaturs(IJavaElement[] elements) throws CoreException {
		Set<String> result= new HashSet<>();
		for (IJavaElement element : elements) {
			String[] natures= computeAffectedNatures(element);
			result.addAll(Arrays.asList(natures));
		}
		return result.toArray(new String[result.size()]);
	}

	private JavaProcessors() {
	}
}
