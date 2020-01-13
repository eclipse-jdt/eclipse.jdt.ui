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
package org.eclipse.jdt.astview.views;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.compiler.IProblem;

import org.eclipse.jdt.core.dom.CompilationUnit;

/**
 *
 */
public class ProblemsProperty extends ASTAttribute {

	private final CompilationUnit fRoot;

	public ProblemsProperty(CompilationUnit root) {
		fRoot= root;
	}

	@Override
	public Object getParent() {
		return fRoot;
	}

	@Override
	public Object[] getChildren() {
		IProblem[] problems= fRoot.getProblems();
		Object[] res= new Object[problems.length];
		for (int i= 0; i < res.length; i++) {
			res[i]= new ProblemNode(this, problems[i]);
		}
		return res;
	}

	@Override
	public String getLabel() {
		return "> compiler problems (" +  fRoot.getProblems().length + ")";  //$NON-NLS-1$//$NON-NLS-2$
	}

	@Override
	public Image getImage() {
		return null;
	}

	/*
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || !obj.getClass().equals(getClass())) {
			return false;
		}
		return true;
	}

	/*
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return 18;
	}
}
