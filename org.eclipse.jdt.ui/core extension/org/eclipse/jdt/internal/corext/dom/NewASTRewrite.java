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

import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jface.text.IDocument;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Statement;

/**
 * Work in progress.
 */
public class NewASTRewrite {
	
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
		

	/** root node for the rewrite: Only nodes under this root are accepted */
	private ASTNode fRootNode;
		
	private HashMap fPlaceholderNodes;
	private HashSet fCollapsedNodes;
	
	private HashMap fTrackedNodes;
	

	protected final RewriteEventStore fEventStore;
	
	public NewASTRewrite(ASTNode node) {
		fRootNode= node;
		fEventStore= new RewriteEventStore();
		
		fPlaceholderNodes= null;
		fTrackedNodes= null;
	}
	
	/**
	 * @return Returns the AST the rewrite was set up on.
	 */
	public AST getAST() {
		return fRootNode.getAST();
	}
		
	/**
	 * Returns the root node of the rewrite. All modifications or move/copy sources lie
	 * inside the root.
	 * @return Returns the root node or <code>null</code> if no node has been
	 * changed.
	 */
	public ASTNode getRootNode() {
		return fRootNode;
	}
	
	/**
	 * Performs the rewrite: The rewrite events are translated to the corresponding in text changes.
	 * @param document Document which describes the code of the AST that is passed in in the
	 * constructor. This document is accessed read-only.
	 * @return Returns the edit describing the text changes.
	 */
	public TextEdit rewriteAST(IDocument document) {
		TextEdit result= new MultiTextEdit();
		
		ASTNode rootNode= getRootNode();
		if (rootNode != null) {
			ASTRewriteAnalyzer visitor= new ASTRewriteAnalyzer(document, result, this, fEventStore);
		
			// update extra comment ranges
			ASTNodes.annotateExtraRanges(rootNode, visitor.getScanner());

			rootNode.accept(visitor);
		}
		return result;
	}
	
	
	/**
	 * Marks a node to be inserted. The inserted node must be either new or a placeholder.
	 * @param parent The node to change
	 * @param childProperty The propert of the child to be inserted.
	 * @param insertedNode The node or attribute to insert.
	 * @param editGroup Description of the change.
	 */
	public final void markAsInsert(ASTNode parent, int childProperty, ASTNode insertedNode, TextEditGroup editGroup) {
		validateIsInsideAST(parent);
		NodeRewriteEvent nodeEvent= fEventStore.getNodeEvent(parent, childProperty, true);
		nodeEvent.setNewValue(insertedNode);

		if (editGroup != null) {
			fEventStore.setEventEditGroup(nodeEvent, editGroup);
		}
	}
	

	/**
	 * 
	 * @param parent
	 * @param childProperty
	 * @return
	 */
	public ListRewriter getListRewrite(ASTNode parent, int childProperty) {
		validateIsInsideAST(parent);
		return new ListRewriter(this, parent, childProperty);
	}
		
	/**
	 * Marks a node or attribute as removed.  
	 * @param parent The node's parent node.
	 * @param childProperty The node's child property in the parent. 
	 * @param editGroup Collect the generated text edits or <code>null</code> if
	 * no edits should be collected.
	 * @throws IllegalArgumentException An <code>IllegalArgumentException</code> is either the parent node is
	 * not inside the rewriters parent or the property is not a node property.
	 */
	public final void markAsRemoved(ASTNode parent, int childProperty, TextEditGroup editGroup) {
		validateIsInsideAST(parent);
		NodeRewriteEvent nodeEvent= fEventStore.getNodeEvent(parent, childProperty, true);
		nodeEvent.setNewValue(null);
		if (editGroup != null) {
			fEventStore.setEventEditGroup(nodeEvent, editGroup);
		}
	}

	/**
	 * Marks a node in a list as removed.  
	 * @param parent The parent node of the list.
	 * @param childProperty The child property of the list. 
	 * @param nodeToRemove The node to remove.
	 * @param editGroup Collect the generated text edit's or <code>null</code> if
	 * no edits should be collected.
	 * @throws IllegalArgumentException An <code>IllegalArgumentException</code> is either the parent node is
	 * not inside the rewriters parent, the property is not a list property or the given node is not in the specified list.
	 */
	public final void markAsRemoved(ASTNode parent, int childProperty, ASTNode nodeToRemove, TextEditGroup editGroup) {
		validateIsInsideAST(parent);
		ListRewriteEvent listEvent= fEventStore.getListEvent(parent, childProperty, true);
		RewriteEvent res= listEvent.removeEntry(nodeToRemove);
		if (res == null) {
			throw new IllegalArgumentException("Node to remove is not member of list"); //$NON-NLS-1$
		}
		if (editGroup != null) {
			fEventStore.setEventEditGroup(res, editGroup);
		}
	}
	
	/**
	 * Marks an existing node as removed.
	 * @param node The node to be marked as removed.
	 * @param editGroup Description of the change.
	 */
	public final void markAsRemoved(ASTNode node, TextEditGroup editGroup) {
		int property= ASTNodeConstants.getPropertyOfNode(node);
		if (ASTNodeConstants.isListProperty(property)) {
			markAsRemoved(node.getParent(), property, node, editGroup);
		} else {
			markAsRemoved(node.getParent(), property, editGroup);
		}
	}

	/**
	 * Marks an existing node as removed.
	 * @param node The node to be marked as removed.
	 */	
	public final void markAsRemoved(ASTNode node) {
		markAsRemoved(node, (TextEditGroup) null);
	}

	/**
	 * Marks a node or attribute as replaced.  The replacing node must be new or
	 * a placeholder.
	 * @param parent The node's parent node.
	 * @param childProperty The node's child property in the parent. 
	 * @param replacingNode The node that replaces the original node.
	 * @param editGroup Collects the generated text edits or <code>null</code> if
	 * no edits should be collected.
	 * @throws IllegalArgumentException An <code>IllegalArgumentException</code> is either the parent node is
	 * not inside the rewriters parent or the property is not a node property.
	 */
	public final void markAsReplaced(ASTNode parent, int childProperty, Object replacingNode, TextEditGroup editGroup) {
		validateIsInsideAST(parent);
		NodeRewriteEvent nodeEvent= fEventStore.getNodeEvent(parent, childProperty, true);
		nodeEvent.setNewValue(replacingNode);
		if (editGroup != null) {
			fEventStore.setEventEditGroup(nodeEvent, editGroup);
		}
	}

	/**
	 * Marks a node in a list as replaced. The replacing node must be new or a placeholder.
	 * @param parent The parent node of the list.
	 * @param childProperty The child property of the list. 
	 * @param nodeToReplace The node to replaced.
	 * @param replacingNode The node that replaces the original node.
	 * @param editGroup Collect the generated text edit's or <code>null</code> if
	 * no edits should be collected.
	 * @throws IllegalArgumentException An <code>IllegalArgumentException</code> is either the parent node is
	 * not inside the rewriters parent, the property is not a list property or the given node is not in the specified list.
	 */
	public final void markAsReplaced(ASTNode parent, int childProperty, ASTNode nodeToReplace, ASTNode replacingNode, TextEditGroup editGroup) {
		validateIsInsideAST(parent);
		ListRewriteEvent listEvent= fEventStore.getListEvent(parent, childProperty, true);
		RewriteEvent res= listEvent.replaceEntry(nodeToReplace, replacingNode);
		if (res == null) {
			throw new IllegalArgumentException("Node to replace is not member of list"); //$NON-NLS-1$
		}
		if (editGroup != null) {
			fEventStore.setEventEditGroup(res, editGroup);
		}
	}
	
	/**
	 * Marks an existing node as replaced by a new node. The replacing node must be new or
	 * a placeholder.
	 * @param node The node to be marked as replaced.
	 * @param replacingNode The node replacing the node.
	 * @param editGroup Description of the change. 
	 */		
	public final void markAsReplaced(ASTNode node, ASTNode replacingNode, TextEditGroup editGroup) {
		int property= ASTNodeConstants.getPropertyOfNode(node);
		if (ASTNodeConstants.isListProperty(property)) {
			markAsReplaced(node.getParent(), property, node, replacingNode, editGroup);
		} else {
			markAsReplaced(node.getParent(), property, replacingNode, editGroup);
		}
	}
	
	/**
	 * Marks an existing node as replaced by a new node. The replacing node must be new or
	 * a placeholder.
	 * @param node The node to be marked as replaced.
	 * @param replacingNode The node replacing the node.
	 */		
	public final void markAsReplaced(ASTNode node, ASTNode replacingNode) {
		markAsReplaced(node, replacingNode, (TextEditGroup) null);
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
	 * Clears all events and other internal structures.
	 */
	protected final void clearRewrite() {
		fEventStore.clear();

		fPlaceholderNodes= null;
	}
		
	protected final void validateIsInsideAST(ASTNode node) {
		if (node.getStartPosition() == -1) {
			throw new IllegalArgumentException("Node is not an existing node"); //$NON-NLS-1$
		}
	
		if (node.getAST() != getAST()) {
			throw new IllegalArgumentException("Node is not inside the AST"); //$NON-NLS-1$
			
		}
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
		validateIsInsideAST(node);
		
		fEventStore.increaseCopyCount(node);
		
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
		validateIsInsideAST(node);

		int placeHolderType= getPlaceholderType(node);
		if (placeHolderType == UNKNOWN) {
			throw new IllegalArgumentException("Move placeholders are not supported for nodes of type " + node.getClass().getName()); //$NON-NLS-1$
		}
		
		fEventStore.setAsMoveSource(node);
		
		MovePlaceholderData data= new MovePlaceholderData();
		data.node= node;
		return createPlaceholder(data, placeHolderType);
	}	
	
	// collapsed nodes: in source: use one node that represents many; to be used as
	// copy/move source or to replace at once.
	// in the target: one block node that is not flattened.
	
	protected final Block createCollapsePlaceholder() {
		Block placeHolder= getAST().newBlock();
		if (fCollapsedNodes == null) {
			fCollapsedNodes= new HashSet();
		}
		fCollapsedNodes.add(placeHolder);
		return placeHolder;
	}
	
	public final boolean isCollapsed(ASTNode node) {
		if (fCollapsedNodes != null) {
			return fCollapsedNodes.contains(node);
		}
		return false;	
	}
	
	protected RewriteEventStore getRewriteEventStore() {
		return fEventStore;
	}
	
	public final TextEditGroup getTrackedNodeData(ASTNode node) {
		if (fTrackedNodes != null) {
			return (TextEditGroup) fTrackedNodes.get(node);
		}
		return null;	
	}
	
	protected void setTrackedNodeData(ASTNode node, TextEditGroup editGroup) {
		if (fTrackedNodes == null) {
			fTrackedNodes= new HashMap();
		}
		fTrackedNodes.put(node, editGroup);
	}	
		
	private final ASTNode createPlaceholder(PlaceholderData data, int nodeType) {
		AST ast= getAST();
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
	
	protected final Object getPlaceholderData(ASTNode node) {
		if (fPlaceholderNodes != null) {
			return fPlaceholderNodes.get(node);
		}
		return null;	
	}
	
	private void setPlaceholderData(ASTNode node, PlaceholderData data) {
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
	
	public String toString() {
		StringBuffer buf= new StringBuffer();
		buf.append("Events:\n"); //$NON-NLS-1$
		buf.append(fEventStore.toString());
		return buf.toString();
	}
	

}
