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
package org.eclipse.jdt.internal.corext.refactoring.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.core.resources.IFile;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;


public class Changes {

	public static IFile[] getModifiedFiles(Change[] changes) {
		List<IFile> result= new ArrayList<>();
		getModifiedFiles(result, changes);
		return result.toArray(new IFile[result.size()]);
	}

	private static void getModifiedFiles(List<IFile> result, Change[] changes) {
		for (Change change : changes) {
			Object modifiedElement= change.getModifiedElement();
			if (modifiedElement instanceof IAdaptable) {
				IFile file= ((IAdaptable)modifiedElement).getAdapter(IFile.class);
				if (file != null)
					result.add(file);
			}
			if (change instanceof CompositeChange) {
				getModifiedFiles(result, ((CompositeChange)change).getChildren());
			}
		}
	}

	private Changes() {
	}
}
