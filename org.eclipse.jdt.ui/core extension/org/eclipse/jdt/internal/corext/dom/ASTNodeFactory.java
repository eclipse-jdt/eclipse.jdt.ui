/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.dom;

import java.util.StringTokenizer;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;

public class ASTNodeFactory {

	private static final String STATEMENT_HEADER= "class __X__ { void __x__() { "; //$NON-NLS-1$
	private static final String STATEMENT_FOOTER= "}}"; //$NON-NLS-1$
	
	private static class PositionClearer extends GenericVisitor {
		protected boolean visitNode(ASTNode node) {
			node.setSourceRange(-1, 0);
			return true;
		}
	}
	
	private ASTNodeFactory() {
		// no instance;
	}
	
	public static ASTNode newStatement(AST ast, String content) {
		StringBuffer buffer= new StringBuffer(STATEMENT_HEADER);
		buffer.append(content);
		buffer.append(STATEMENT_FOOTER);
		CompilationUnit root= AST.parseCompilationUnit(buffer.toString().toCharArray());
		ASTNode result= ASTNode.copySubtree(ast, NodeFinder.perform(root, STATEMENT_HEADER.length(), content.length()));
		result.accept(new PositionClearer());
		return result;
	}
	
	public static Name newName(AST ast, String name) {
		StringTokenizer tok= new StringTokenizer(name, "."); //$NON-NLS-1$
		Name res= null;
		while (tok.hasMoreTokens()) {
			SimpleName curr= ast.newSimpleName(tok.nextToken());
			if (res == null) {
				res= curr;
			} else {
				res= ast.newQualifiedName(res, curr);
			}
		}
		return res;
	}	

}
