/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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

import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

public interface INewNameQueries {
	INewNameQuery createNewCompilationUnitNameQuery(ICompilationUnit cu, String initialSuggestedName) throws OperationCanceledException;
	INewNameQuery createNewResourceNameQuery(IResource res, String initialSuggestedName) throws OperationCanceledException;
	INewNameQuery createNewPackageNameQuery(IPackageFragment pack, String initialSuggestedName) throws OperationCanceledException;
	INewNameQuery createNewPackageFragmentRootNameQuery(IPackageFragmentRoot root, String initialSuggestedName) throws OperationCanceledException;
	INewNameQuery createNullQuery();
	INewNameQuery createStaticQuery(String newName) throws OperationCanceledException;
}
