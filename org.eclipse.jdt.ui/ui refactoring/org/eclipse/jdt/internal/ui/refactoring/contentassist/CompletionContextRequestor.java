/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
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

package org.eclipse.jdt.internal.ui.refactoring.contentassist;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.internal.corext.refactoring.StubTypeContext;


public abstract class CompletionContextRequestor {

	public abstract StubTypeContext getStubTypeContext();

	public ICompilationUnit getOriginalCu() {
		return getStubTypeContext().getCuHandle();
	}

	public String getBeforeString() {
		return getStubTypeContext().getBeforeString();
	}

	public String getAfterString() {
		return getStubTypeContext().getAfterString();
	}

}
