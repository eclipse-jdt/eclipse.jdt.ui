/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.dom;

import java.util.ArrayList;

import org.eclipse.jdt.core.ICodeFormatter;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;

public class ASTWithExistingFlattener extends ASTFlattener {

	private static final String KEY= "ExistingASTNode";

	public static class NodeMarker {
		public Object data;
		public int offset;
		public int length;		
	}
	
	/* package */ static ASTNode createPlaceholder(AST ast, Object data, int nodeType) {
		ASTNode placeHolder;
		switch (nodeType) {
			case ASTRewrite.EXPRESSION:
				placeHolder= ast.newSimpleName("z");
				break;
			case ASTRewrite.TYPE:
				placeHolder= ast.newSimpleType(ast.newSimpleName("X"));
				break;				
			case ASTRewrite.STATEMENT:
				placeHolder= ast.newReturnStatement();
				break;
			case ASTRewrite.BLOCK:
				placeHolder= ast.newBlock();
				break;
			case ASTRewrite.BODY_DECLARATION:
				placeHolder= ast.newInitializer();
				break;
			case ASTRewrite.SINGLEVAR_DECLARATION:
				placeHolder= ast.newSingleVariableDeclaration();
				break;
			case ASTRewrite.VAR_DECLARATION_FRAGMENT:
				placeHolder= ast.newVariableDeclarationFragment();
				break;
			case ASTRewrite.JAVADOC:
				placeHolder= ast.newJavadoc();
				break;				
			default:
				return null;
		}
		
		NodeMarker marker= new NodeMarker();
		marker.data= data;
		marker.offset= -1;
		marker.length= 0;
		
		placeHolder.setProperty(KEY, marker);
		return placeHolder;
	}

	private static NodeMarker getMarker(ASTNode node) {
		return (NodeMarker) node.getProperty(KEY);
	} 
	
	private ArrayList fExistingNodes;

	public ASTWithExistingFlattener() {
		super();
		fExistingNodes= new ArrayList(10);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.dom.ASTFlattener#reset()
	 */
	public void reset() {
		super.reset();
		fExistingNodes.clear();
	}
	
	public NodeMarker[] getNodeMarkers() {
		return (NodeMarker[]) fExistingNodes.toArray(new NodeMarker[fExistingNodes.size()]);
	}
	
	/**
	 * Returns the string accumulated in the visit formatted using the default formatter.
	 * Updates the existing node's positions.
	 *
	 * @return the serialized and formatted code.
	 */	
	public String getFormattedResult(int initialIndentationLevel, String lineDelimiter) {
		NodeMarker[] markers= getNodeMarkers();
		int nExistingNodes= markers.length;

		int[] positions= new int[nExistingNodes*2];
		for (int i= 0, k= 0; i < nExistingNodes; i++) {
			int startPos= markers[i].offset;
			int length= markers[i].length;
			positions[k++]= startPos;
			positions[k++]= startPos + length - 1;
		}		
		
		ICodeFormatter formatter= ToolFactory.createDefaultCodeFormatter(null);
		String formatted= formatter.format(getResult(), initialIndentationLevel, positions, lineDelimiter);
		
		for (int i= 0, k= 0; i < nExistingNodes; i++) {
			int startPos= positions[k++];
			int endPos= positions[k++] + 1;
			markers[i].offset= startPos;
			markers[i].length= endPos - startPos;
		}
		return formatted;
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#preVisit(ASTNode)
	 */
	public void preVisit(ASTNode node) {
		NodeMarker marker= getMarker(node);
		if (marker != null) {
			marker.offset= fResult.length();
			fExistingNodes.add(marker);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#postVisit(ASTNode)
	 */
	public void postVisit(ASTNode node) {
		NodeMarker marker= getMarker(node);
		if (marker != null) {
			marker.length= fResult.length() - marker.offset;
		}
	}
}
