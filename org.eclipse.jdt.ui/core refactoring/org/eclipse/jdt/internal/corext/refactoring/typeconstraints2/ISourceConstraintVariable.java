/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
package org.eclipse.jdt.internal.corext.refactoring.typeconstraints2;

import org.eclipse.jdt.core.ICompilationUnit;

public interface ISourceConstraintVariable {

	public ICompilationUnit getCompilationUnit();

	public Object getData(String name);

	public ITypeSet getTypeEstimate();

	public void setCompilationUnit(ICompilationUnit cu);

	public void setData(String name, Object data);
}