/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.participants;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IProject;

public class RefactoringProcessors {

	public static String[] getNatures(IProject[] projects) throws CoreException {
		Set<String> result= new HashSet<>();
		for (int i= 0; i < projects.length; i++) {
			String[] pns= projects[i].getDescription().getNatureIds();
			for (int p = 0; p < pns.length; p++) {
				result.add(pns[p]);
			}
		}
		return result.toArray(new String[result.size()]);
	}
}
