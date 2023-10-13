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
package org.eclipse.jdt.internal.corext.refactoring.reorg;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

public class ArrayTypeConverter {

	private ArrayTypeConverter() {
	}

	public static IFile[] toFileArray(Object[] objects){
		List<?> l= Arrays.asList(objects);
		return l.toArray(new IFile[l.size()]);
	}

	public static IFolder[] toFolderArray(Object[] objects){
		List<?> l= Arrays.asList(objects);
		return l.toArray(new IFolder[l.size()]);
	}

	public static ICompilationUnit[] toCuArray(Object[] objects){
		List<?> l= Arrays.asList(objects);
		return l.toArray(new ICompilationUnit[l.size()]);
	}

	public static IPackageFragmentRoot[] toPackageFragmentRootArray(Object[] objects){
		List<?> l= Arrays.asList(objects);
		return l.toArray(new IPackageFragmentRoot[l.size()]);
	}

	public static IPackageFragment[] toPackageArray(Object[] objects){
		List<?> l= Arrays.asList(objects);
		return l.toArray(new IPackageFragment[l.size()]);
	}
}
