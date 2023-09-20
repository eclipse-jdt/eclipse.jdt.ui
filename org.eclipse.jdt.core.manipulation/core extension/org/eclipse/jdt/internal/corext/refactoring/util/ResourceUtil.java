/*******************************************************************************
 * Copyright (c) 2000, 2023 IBM Corporation and others.
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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.internal.ui.util.ResourcesUtility;


public class ResourceUtil {

	private ResourceUtil(){
	}

	public static IFile[] getFiles(ICompilationUnit[] cus) {
		return ResourcesUtility.getFiles(cus);
	}

	public static IFile getFile(ICompilationUnit cu) {
		return ResourcesUtility.getFile(cu);
	}

	//----- other ------------------------------

	public static IResource getResource(Object o){
		return ResourcesUtility.getResource(o);
	}
}
