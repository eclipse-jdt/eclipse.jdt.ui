/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
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

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class SettingsProperty extends ASTAttribute {

	private final CompilationUnit fRoot;

	public SettingsProperty(CompilationUnit root) {
		fRoot= root;
	}

	@Override
	public Object getParent() {
		return fRoot;
	}

	@Override
	public Object[] getChildren() {
		AST ast= fRoot.getAST();
		Object[] res= {
				new GeneralAttribute(this, "apiLevel", String.valueOf(ast.apiLevel())),
				new GeneralAttribute(this, "hasResolvedBindings", String.valueOf(ast.hasResolvedBindings())),
				new GeneralAttribute(this, "hasStatementsRecovery", String.valueOf(ast.hasStatementsRecovery())),
				new GeneralAttribute(this, "hasBindingsRecovery", String.valueOf(ast.hasBindingsRecovery())),
		};
		return res;
	}

	@Override
	public String getLabel() {
		return "> AST settings";  //$NON-NLS-1$
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
		return 19;
	}
}
