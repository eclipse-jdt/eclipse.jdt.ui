/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.reorg;

import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;

public interface ICopyQueries {
	public INewNameQuery createNewCompilationUnitNameQuery(ICompilationUnit cu);
	public INewNameQuery createNewResourceNameQuery(IResource res);
	public INewNameQuery createNewPackageNameQuery(IPackageFragment pack);
	public INewNameQuery createNullQuery();
	public INewNameQuery createStaticQuery(String newName);
}