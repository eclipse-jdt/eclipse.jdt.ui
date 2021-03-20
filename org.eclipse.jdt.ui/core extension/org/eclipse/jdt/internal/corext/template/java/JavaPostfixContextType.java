/*******************************************************************************
 * Copyright (c) 2019 Nicolaj Hoess.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Nicolaj Hoess - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.template.java;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;

import org.eclipse.jdt.core.CompletionContext;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;

public class JavaPostfixContextType extends AbstractJavaContextType {

	public static final String ID_ALL= "postfix"; //$NON-NLS-1$

	public JavaPostfixContextType() {
		this.addResolver(new InnerExpressionResolver());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.template.java.AbstractJavaContextType#initializeContext(org.eclipse.jdt.internal.corext.template.java.JavaContext)
	 */
	@Override
	protected void initializeContext(IJavaContext context) {
		if (!JavaPostfixContextType.ID_ALL.equals(getId())) { // a specific context must also allow the templates that work everywhere
			context.addCompatibleContextType(JavaPostfixContextType.ID_ALL);
		}
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.template.java.CompilationUnitContextType#createContext(org.eclipse.jface.text.IDocument, int, int, org.eclipse.jdt.core.ICompilationUnit)
	 */
	@Override
	public CompilationUnitContext createContext(IDocument document, int offset, int length, ICompilationUnit compilationUnit) {
		return createContext(document, offset, length, compilationUnit, null, null, null);
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.template.java.CompilationUnitContextType#createContext(org.eclipse.jface.text.IDocument, org.eclipse.jface.text.Position, org.eclipse.jdt.core.ICompilationUnit)
	 */
	@Override
	public CompilationUnitContext createContext(IDocument document, Position completionPosition, ICompilationUnit compilationUnit) {
		return createContext(document, completionPosition.getOffset(), completionPosition.getLength(), compilationUnit);
	}

	public JavaPostfixContext createContext(IDocument document, int offset, int length, ICompilationUnit compilationUnit, ASTNode currentNode, ASTNode parentNode, CompletionContext context) {
		JavaPostfixContext javaContext= new JavaPostfixContext(this, document, offset, length, compilationUnit, currentNode, parentNode, context);
		initializeContext(javaContext);
		return javaContext;
	}

}
