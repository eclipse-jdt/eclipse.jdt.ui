/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.dom;

import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Statement;

/**
 *
 */
public final class NodeInfoStore {
	
	/** Constant used to create place holder nodes */
	public static final int UNKNOWN= -1;
	public static final int BLOCK= 2;
	public static final int EXPRESSION= 3;
	public static final int STATEMENT= 4;
	public static final int SINGLEVAR_DECLARATION= 5;
	public static final int TYPE= 6;
	public static final int NAME= 7;
	public static final int JAVADOC= 8;
	public static final int VAR_DECLARATION_FRAGMENT= 9;
	public static final int TYPE_DECLARATION= 10;
	public static final int FIELD_DECLARATION= 11;
	public static final int METHOD_DECLARATION= 12;
	public static final int INITIALIZER= 13;
	public static final int PACKAGE_DECLARATION= 14;
	public static final int IMPORT_DECLARATION= 15;
	
	private AST fAst;
	
	private HashMap fPlaceholderNodes;
	private HashSet fCollapsedNodes;

	private HashMap fTrackedNodes;
	
	public NodeInfoStore(AST ast) {
		super();
		fAst= ast;
		fPlaceholderNodes= null;
		fCollapsedNodes= null;
		
		fTrackedNodes= null;
	}
	
	/**
	 * Returns the node type that should be used to create a place holder for the given node
	 * <code>existingNode</code>.
	 * 
	 * @param existingNode an existing node for which a place holder is to be created
	 * @return the node type of a potential place holder
	 */
	public static int getPlaceholderType(ASTNode existingNode) {
		switch (existingNode.getNodeType()) {
			case ASTNode.SIMPLE_NAME:
			case ASTNode.QUALIFIED_NAME:
				return NAME;
			case ASTNode.SIMPLE_TYPE:
			case ASTNode.PRIMITIVE_TYPE:
			case ASTNode.ARRAY_TYPE:
				return TYPE;				
			case ASTNode.BLOCK:
				return BLOCK;
			case ASTNode.TYPE_DECLARATION:
				return TYPE_DECLARATION;
			case ASTNode.METHOD_DECLARATION:
				return METHOD_DECLARATION;
			case ASTNode.FIELD_DECLARATION:
				return FIELD_DECLARATION;
			case ASTNode.INITIALIZER:
				return INITIALIZER;
			case ASTNode.SINGLE_VARIABLE_DECLARATION:
				return SINGLEVAR_DECLARATION;			
			case ASTNode.VARIABLE_DECLARATION_FRAGMENT:
				return VAR_DECLARATION_FRAGMENT;
			case ASTNode.JAVADOC:
				return JAVADOC;
			case ASTNode.PACKAGE_DECLARATION:
				return PACKAGE_DECLARATION;
			case ASTNode.IMPORT_DECLARATION:
				return IMPORT_DECLARATION;
			default:
				if (existingNode instanceof Expression) {
					return EXPRESSION;
				} else if (existingNode instanceof Statement) {
					// is not Block: special case statement for block
					return STATEMENT;
				}
		}
		return UNKNOWN;
	}
	
	/**
	 * Creates a target node for a source string to be inserted without being formatted. A target node can
	 * be inserted or used to replace at the target position.
	 * @param code String that will be inserted. The string must not have extra indent.
	 * @param nodeType the type of the place holder. Valid values are <code>METHOD_DECLARATION</code>,
	 * <code>FIELD_DECLARATION</code>, <code>INITIALIZER</code>,
	 * <code>TYPE_DECLARATION</code>, <code>BLOCK</code>, <code>STATEMENT</code>,
	 *  <code>SINGLEVAR_DECLARATION</code>,<code> VAR_DECLARATION_FRAGMENT</code>,
	 * <code>TYPE</code>, <code>EXPRESSION</code>, <code>NAME</code>
	 * <code>PACKAGE_DECLARATION</code>, <code>IMPORT_DECLARATION</code> and <code>JAVADOC</code>.
	 * @return Returns the place holder node
	 */
	public final ASTNode createStringPlaceholder(String code, int nodeType) {
		StringPlaceholderData data= new StringPlaceholderData();
		data.code= code;
		return createPlaceholder(data, nodeType);
	}

	/**
	 * Creates a target node for a node to be copied. A target node can be inserted or used
	 * to replace at the target position.
	 * @param node The node to create a copy placeholder for.
	 * @return The placeholder to be used at the copy destination.
	 */
	public final ASTNode createCopyPlaceholder(ASTNode node) {
		
		int placeHolderType= getPlaceholderType(node);
		if (placeHolderType == UNKNOWN) {
			throw new IllegalArgumentException("Copy placeholders are not supported for nodes of type " + node.getClass().getName()); //$NON-NLS-1$
		}
		CopyPlaceholderData data= new CopyPlaceholderData();
		data.node= node;
		return createPlaceholder(data, placeHolderType);
	}
	
	/**
	 * Creates a target node for a node to be moved. A target node can be inserted or used
	 * to replace at the target position. The source node has to be marked as removed or replaced.
	 * @param node The node to create a move placeholder for.
	 * @return The placeholder to be used at the move destination.
	 */
	public final ASTNode createMovePlaceholder(ASTNode node) {
		int placeHolderType= getPlaceholderType(node);
		if (placeHolderType == UNKNOWN) {
			throw new IllegalArgumentException("Move placeholders are not supported for nodes of type " + node.getClass().getName()); //$NON-NLS-1$
		}
		
		MovePlaceholderData data= new MovePlaceholderData();
		data.node= node;
		return createPlaceholder(data, placeHolderType);
	}	
	
	private final ASTNode createPlaceholder(PlaceholderData data, int nodeType) {
		AST ast= fAst;
		ASTNode placeHolder;
		switch (nodeType) {
			case NAME:
				placeHolder= ast.newSimpleName("z"); //$NON-NLS-1$
				break;
			case EXPRESSION:
				MethodInvocation expression = ast.newMethodInvocation(); 
				expression.setName(ast.newSimpleName("z")); //$NON-NLS-1$
				placeHolder = expression;
				break;			
			case TYPE:
				placeHolder= ast.newSimpleType(ast.newSimpleName("X")); //$NON-NLS-1$
				break;				
			case STATEMENT:
				placeHolder= ast.newReturnStatement();
				break;
			case BLOCK:
				placeHolder= ast.newBlock();
				break;
			case METHOD_DECLARATION:
				placeHolder= ast.newMethodDeclaration();
				break;
			case FIELD_DECLARATION:
				placeHolder= ast.newFieldDeclaration(ast.newVariableDeclarationFragment());
				break;
			case INITIALIZER:
				placeHolder= ast.newInitializer();
				break;								
			case SINGLEVAR_DECLARATION:
				placeHolder= ast.newSingleVariableDeclaration();
				break;
			case VAR_DECLARATION_FRAGMENT:
				placeHolder= ast.newVariableDeclarationFragment();
				break;
			case JAVADOC:
				placeHolder= ast.newJavadoc();
				break;				
			case TYPE_DECLARATION:
				placeHolder= ast.newTypeDeclaration();
				break;
			case PACKAGE_DECLARATION:
				placeHolder= ast.newPackageDeclaration();
				break;
			case IMPORT_DECLARATION:
				placeHolder= ast.newImportDeclaration();
				break;
			default:
				return null;
		}
		setPlaceholderData(placeHolder, data);
		return placeHolder;
	}	


	
	
	// collapsed nodes: in source: use one node that represents many; to be used as
	// copy/move source or to replace at once.
	// in the target: one block node that is not flattened.
	
	public Block createCollapsePlaceholder() {
		Block placeHolder= fAst.newBlock();
		if (fCollapsedNodes == null) {
			fCollapsedNodes= new HashSet();
		}
		fCollapsedNodes.add(placeHolder);
		return placeHolder;
	}
	
	public boolean isCollapsed(ASTNode node) {
		if (fCollapsedNodes != null) {
			return fCollapsedNodes.contains(node);
		}
		return false;	
	}
	
	public Object getPlaceholderData(ASTNode node) {
		if (fPlaceholderNodes != null) {
			return fPlaceholderNodes.get(node);
		}
		return null;	
	}
	
	public void setPlaceholderData(ASTNode node, PlaceholderData data) {
		if (fPlaceholderNodes == null) {
			fPlaceholderNodes= new HashMap();
		}
		fPlaceholderNodes.put(node, data);		
	}
	
	private static class PlaceholderData {
	}
		
	protected static final class MovePlaceholderData extends PlaceholderData {
		public ASTNode node;
		public String toString() {
			return "[placeholder move: " + node +"]"; //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	protected static final class CopyPlaceholderData extends PlaceholderData {
		public ASTNode node;
		public String toString() {
			return "[placeholder copy: " + node +"]";  //$NON-NLS-1$//$NON-NLS-2$
		}
	}	
	
	protected static final class StringPlaceholderData extends PlaceholderData {
		public String code;
		public String toString() {
			return "[placeholder string: " + code +"]"; //$NON-NLS-1$ //$NON-NLS-2$
		}
	}


	public final TextEditGroup getTrackedNodeData(ASTNode node) {
		if (fTrackedNodes != null) {
			return (TextEditGroup) fTrackedNodes.get(node);
		}
		return null;	
	}
	
	public void setTrackedNodeData(ASTNode node, TextEditGroup editGroup) {
		if (fTrackedNodes == null) {
			fTrackedNodes= new HashMap();
		}
		fTrackedNodes.put(node, editGroup);
	}
	
	/**
	 * Marks a node as tracked. The edits added to the group editGroup can be used to get the
	 * position of the node after the rewrite operation.
	 * @param node The node to track
	 * @param editGroup Collects the range markers describing the node position.
	 */
	public final void markAsTracked(ASTNode node, TextEditGroup editGroup) {
		if (getTrackedNodeData(node) != null) {
			throw new IllegalArgumentException("Node is already marked as tracked"); //$NON-NLS-1$
		}
		setTrackedNodeData(node, editGroup);
	}	
	
	/**
	 * 
	 */
	public void clear() {
		fPlaceholderNodes= null;
		fTrackedNodes= null;
		fCollapsedNodes= null;
	}
}
