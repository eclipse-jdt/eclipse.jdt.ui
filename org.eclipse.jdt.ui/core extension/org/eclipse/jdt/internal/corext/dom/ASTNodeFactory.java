/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.dom;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class ASTNodeFactory {

	private static final String STATEMENT_HEADER= "class __X__ { void __x__() { ";
	private static final String STATEMENT_FOOTER= "}}";
	
	private static class NodeFinder extends GenericVisitor {
		private int start;
		private int length;
		private int end;
		public ASTNode result;
		public NodeFinder(int s, int l) {
			start= s;
			length= l;
			end= s + l;
		}
		protected boolean visitNode(ASTNode node) {
			int nodeStart= node.getStartPosition();
			int nodeLength= node.getLength();
			if (liesOutside(nodeStart, nodeLength)) {
				return false;
			} else if (nodeStart == start && nodeLength == length) {
				clearPosition(node);
				result= node;
				// traverse down to clear positions.
				return true;
			}
			clearPosition(node);
			return true;
		}
		private boolean liesOutside(int nodeStart, int nodeLength) {
			return nodeStart + nodeLength <= start || end <= nodeStart;
		}
		private void clearPosition(ASTNode node) {
			node.setSourceRange(-1, 0);
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
		NodeFinder finder= new NodeFinder(STATEMENT_HEADER.length(), content.length());
		root.accept(finder);
		return ASTNode.copySubtree(ast, finder.result);
	}

}
