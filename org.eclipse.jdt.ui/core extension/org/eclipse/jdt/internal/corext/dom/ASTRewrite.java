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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.text.edits.TextEdit;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Statement;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.textmanipulation.GroupDescription;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;

/**
 * Example:
 * <code>
 * void foo(int i, boolean b) {
 *     doSomething(x + 7 - y);
 * }
 *   MethodDeclaration existingDecl
 *   ASTRewrite rewrite= ASTRewrite(existingDecl);
 *   AST ast= existingDecl.getAST();
 * 
 *   // change return type to array of float
 *   ArrayType newReturnType= ast.newArrayType(ast.newPrimitiveType(PrimitiveType.FLOAT), 1);
 *   rewrite.markAsReplaced(existingDecl.getReturnType(), newReturnType);
 * 
 *   // change name
 *   SimpleName newName= ast.newSimpleName("work");
 *   rewrite.markAsReplaced(existingDecl.getName(), newName);
 *  
 *   // remove first parameter
 *   List parameters= existingDecl.parameters();
 *   rewrite.markAsRemoved((ASTNode) parameters.get(0));
 * 
 *   // add new throws declaration
 *   List thrownExceptions= existingDecl.thrownExceptions();
 *   SimpleName newException= ast.newSimpleName("IOException");
 *   thrownExceptions.add(newException);
 *   rewrite.markAsInserted(newException);
 * 
 *   // move statement inside if
 *   List statements= existingDecl.getBody().statements();
 *   
 *   Statement movedNode= (Statement) statements.get(0);
 *   Statement copyTarget= (Statement) rewrite.createCopy(movedNode);
 *   
 *   IfStatement newIfStatement= ast.newIfStatement();
 *   newIfStatement.setExpression(ast.newSimpleName("b"));
 *   newIfStatement.setThenStatement(copyTarget);
 * 
 *   rewrite.markAsReplaced(movedNode, newIfStatement);
 * 
 *   TextEdit resultingEdits= new MultiTextEdit();
 *   rewrite.rewriteNode(textBuffer, resultingEdits, null);
 *   </code>
 */

public final class ASTRewrite extends NewASTRewrite {
	
	public static final int UNKNOWN= NewASTRewrite.UNKNOWN;
	public static final int BLOCK= NewASTRewrite.BLOCK;
	public static final int EXPRESSION= NewASTRewrite.EXPRESSION;
	public static final int STATEMENT= NewASTRewrite.STATEMENT;
	public static final int SINGLEVAR_DECLARATION= NewASTRewrite.SINGLEVAR_DECLARATION;
	public static final int TYPE= NewASTRewrite.TYPE;
	public static final int NAME= NewASTRewrite.NAME;
	public static final int JAVADOC= NewASTRewrite.JAVADOC;
	public static final int VAR_DECLARATION_FRAGMENT= NewASTRewrite.VAR_DECLARATION_FRAGMENT;
	public static final int TYPE_DECLARATION= NewASTRewrite.TYPE_DECLARATION;
	public static final int FIELD_DECLARATION= NewASTRewrite.FIELD_DECLARATION;
	public static final int METHOD_DECLARATION= NewASTRewrite.METHOD_DECLARATION;
	public static final int INITIALIZER= NewASTRewrite.INITIALIZER;
	public static final int PACKAGE_DECLARATION= NewASTRewrite.PACKAGE_DECLARATION;
	public static final int IMPORT_DECLARATION= NewASTRewrite.IMPORT_DECLARATION;
	
	private HashMap fChangedProperties;

	private boolean fHasASTModifications;
	
	/**
	 * Creates the <code>ASTRewrite</code> object.
	 * @param node A node which is parent to all modified, changed or tracked nodes.
	 */
	public ASTRewrite(ASTNode node) {
		super(node);
		fChangedProperties= new HashMap();


		fHasASTModifications= false;	
	}
		
	/**
	 * Perform rewriting: Analyses AST modifications and creates text edits that describe changes to the
	 * underlying code. Edits do only change code when the corresponding node has changed. New code
	 * is formatted using the standard code formatter.
	 * @param textBuffer Text buffer which is describing the code of the AST passed in in the
	 * constructor. This buffer is accessed read-only.
	 * @param rootEdit
	 */
	public final void rewriteNode(TextBuffer textBuffer, TextEdit rootEdit) {
		convertOldToNewEvents();
		TextEdit res= super.rewriteNode(textBuffer.getDocument());
		rootEdit.addChildren(res.removeChildren());
	}
	
	/*(non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.dom.NewASTRewrite#accessOriginalValue(org.eclipse.jdt.core.dom.ASTNode, int)
	 */
	protected Object accessOriginalValue(ASTNode parent, int childProperty) {
		Object originalValue= ASTNodeConstants.getNodeChild(parent, childProperty);
		if (originalValue instanceof List) {
			List originalList= (List) originalValue;
			ArrayList fixedList= new ArrayList(originalList.size());
			for (int i= 0; i < originalList.size(); i++) {
				ASTNode curr= (ASTNode) originalList.get(i);
				if (!isInserted(curr)) {
					fixedList.add(curr);
				}
			}
			return fixedList;
		} else if (originalValue instanceof ASTNode) {
			if (isInserted((ASTNode) originalValue)) {
				return null;
			}
		}
		return originalValue;
	}
	
	/**
	 * Convert the old to the new events. Can only be done when rewrite is started
	 * (inserted node must not yet be added to the AST when marked)
	 */
	private void convertOldToNewEvents() {
		Set processedListEvents= new HashSet();
		
		for (Iterator iter= fChangedProperties.keySet().iterator(); iter.hasNext(); ) {
			ASTNode node= (ASTNode) iter.next();
			ASTChange object= getChangeProperty(node);
			if (object instanceof ASTInsert) {
				if (node.getParent().getStartPosition() != -1) {
					processChange(node, null, node, object.description, processedListEvents);
					if (((ASTInsert) object).isBoundToPrevious) {
						setInsertBoundToPrevious(node);
					}
				}
			}
		}
	}
	
	private void processChange(ASTNode nodeInAST, ASTNode originalNode, ASTNode newNode, GroupDescription desc, Set processedListEvents) {
		ASTNode parent= nodeInAST.getParent();
		int childProperty= ASTNodeConstants.getPropertyOfNode(nodeInAST);
		if (ASTNodeConstants.isListProperty(childProperty)) {
			ListRewriteEvent event= getListEvent(parent, childProperty, true); // create
			if (processedListEvents.add(event)) {
				convertListChange(event, (List) ASTNodeConstants.getNodeChild(parent, childProperty));
			}
		} else {
			NodeRewriteEvent event= getNodeEvent(parent, childProperty, true);
			event.setNewValue(newNode);
			setDescription(event, desc);
		}		
	}
	
	
	private void convertListChange(ListRewriteEvent listEvent, List modifiedList) {
		int insertIndex= 0;
		for (int i= 0; i < modifiedList.size(); i++) {
			ASTNode curr= (ASTNode) modifiedList.get(i);
			ASTChange object= getChangeProperty(curr);
			if (object instanceof ASTInsert) {
				RewriteEvent change= listEvent.insertEntry(insertIndex, curr);
				setDescription(change, object.description);
				if (((ASTInsert) object).isBoundToPrevious) {
					setInsertBoundToPrevious(curr);
				}
			} else {
				insertIndex++;
			}
		}
	}
	
	/**
	 * Removes all modifications applied to the given AST.
	 */
	public final void removeModifications() {
		if (fHasASTModifications) {
			getRootNode().accept(new ASTRewriteClear(this));
			fHasASTModifications= false;
		}
		fChangedProperties.clear();

		clearRewrite();
	}
	
	/**
	 * Marke a node to insert.
	 * @param parent The node to change
	 * @param childProperty The propert of the child to be inserted.
	 * @param insertedNode the node or attribute to insert.
	 * @param description Description of the change.
	 */
	public final void markAsInsert(ASTNode parent, int childProperty, ASTNode insertedNode, GroupDescription description) {
		validateIsInsideRoot(parent);
		validateIsNodeProperty(childProperty);
		NodeRewriteEvent nodeEvent= getNodeEvent(parent, childProperty, true);
		nodeEvent.setNewValue(insertedNode);
		if (description != null) {
			setDescription(nodeEvent, description);
		}
	}
	
			
	/**
	 * Marks a node as inserted. The node must not exist. To insert an existing node (move or copy),
	 * create a copy target first and insert this target node. ({@link #createCopy})
	 * @param node The node to be marked as inserted.
	 * @param description Description of the change.
	 */
	public final void markAsInserted(ASTNode node, GroupDescription description) {
		Assert.isTrue(!isCollapsed(node), "Tries to insert a collapsed node"); //$NON-NLS-1$
		ASTInsert insert= new ASTInsert();
		insert.isBoundToPrevious= getDefaultBoundBehaviour(node);
		insert.description= description;
		setChangeProperty(node, insert);
		fHasASTModifications= true;
	}

	/**
	 * Marks a node as inserted. The node must not exist. To insert an existing node (move or copy),
	 * create a copy target first and insert this target node. ({@link #createCopy})
	 * @param node The node to be marked as inserted.
	 */
	public final void markAsInserted(ASTNode node) {
		markAsInserted(node, (GroupDescription) null);
	}
	
	private boolean getDefaultBoundBehaviour(ASTNode node) {
		return (node instanceof Statement || node instanceof FieldDeclaration);
	}
	
	
	public boolean hasASTModifications() {
		return fHasASTModifications;
	}
	
	/**
	 * Marks a node or attribute as removed.  
	 * @param parent The node's parent node.
	 * @param childProperty The node's child property in the parent. 
	 * @param description The group description to collect the generated text edits or <code>null</code> if
	 * no edits should be collected.
	 * @throws IllegalArgumentException An <code>IllegalArgumentException</code> is either the parent node is
	 * not inside the rewriters parent or the property is not a node property.
	 */
	public final void markAsRemoved(ASTNode parent, int childProperty, GroupDescription description) {
		validateIsInsideRoot(parent);
		validateIsNodeProperty(childProperty);
		NodeRewriteEvent nodeEvent= getNodeEvent(parent, childProperty, true);
		nodeEvent.setNewValue(null);
		if (description != null) {
			setDescription(nodeEvent, description);
		}
	}

	/**
	 * Marks a node in a list as removed.  
	 * @param parent The parent node of the list.
	 * @param childProperty The child property of the list. 
	 * @param nodeToRemove The node to remove.
	 * @param description The group description to collect the generated text edit's or <code>null</code> if
	 * no edits should be collected.
	 * @throws IllegalArgumentException An <code>IllegalArgumentException</code> is either the parent node is
	 * not inside the rewriters parent, the property is not a list property or the given node is not in the specified list.
	 */
	public final void markAsRemoved(ASTNode parent, int childProperty, ASTNode nodeToRemove, GroupDescription description) {
		validateIsInsideRoot(parent);
		ListRewriteEvent listEvent= getListEvent(parent, childProperty, true);
		RewriteEvent res= listEvent.removeEntry(nodeToRemove);
		if (res == null) {
			throw new IllegalArgumentException("Node to remove is not member of list"); //$NON-NLS-1$
		}
		if (description != null) {
			setDescription(res, description);
		}
	}
	
	/**
	 * Marks an existing node as removed.
	 * @param node The node to be marked as removed.
	 * @param description Description of the change.
	 */
	public final void markAsRemoved(ASTNode node, GroupDescription description) {
		int property= ASTNodeConstants.getPropertyOfNode(node);
		if (ASTNodeConstants.isListProperty(property)) {
			markAsRemoved(node.getParent(), property, node, description);
		} else {
			markAsRemoved(node.getParent(), property, description);
		}
	}

	/**
	 * Marks an existing node as removed.
	 * @param node The node to be marked as removed.
	 */	
	public final void markAsRemoved(ASTNode node) {
		markAsRemoved(node, (GroupDescription) null);
	}

	/**
	 * Marks a node or attribute as replaced.  
	 * @param parent The node's parent node.
	 * @param childProperty The node's child property in the parent. 
	 * @param replacingNode The node that replaces the original node.
	 * @param description The group description to collect the generated text edits or <code>null</code> if
	 * no edits should be collected.
	 * @throws IllegalArgumentException An <code>IllegalArgumentException</code> is either the parent node is
	 * not inside the rewriters parent or the property is not a node property.
	 */
	public final void markAsReplaced(ASTNode parent, int childProperty, Object replacingNode, GroupDescription description) {
		validateIsInsideRoot(parent);
		validateIsNodeProperty(childProperty);
		NodeRewriteEvent nodeEvent= getNodeEvent(parent, childProperty, true);
		nodeEvent.setNewValue(replacingNode);
		if (description != null) {
			setDescription(nodeEvent, description);
		}
	}

	/**
	 * Marks a node in a list as replaced.  
	 * @param parent The parent node of the list.
	 * @param childProperty The child property of the list. 
	 * @param nodeToReplace The node to replaced.
	 * @param replacingNode The node that replaces the original node.
	 * @param description The group description to collect the generated text edit's or <code>null</code> if
	 * no edits should be collected.
	 * @throws IllegalArgumentException An <code>IllegalArgumentException</code> is either the parent node is
	 * not inside the rewriters parent, the property is not a list property or the given node is not in the specified list.
	 */
	public final void markAsReplaced(ASTNode parent, int childProperty, ASTNode nodeToReplace, ASTNode replacingNode, GroupDescription description) {
		validateIsInsideRoot(parent);
		ListRewriteEvent listEvent= getListEvent(parent, childProperty, true);
		RewriteEvent res= listEvent.replaceEntry(nodeToReplace, replacingNode);
		if (res == null) {
			throw new IllegalArgumentException("Node to replace is not member of list"); //$NON-NLS-1$
		}
		if (description != null) {
			setDescription(res, description);
		}
	}
	
	/**
	 * Marks an existing node as replace by a new node. The replacing node node must not exist.
	 * To replace with an existing node (move or copy), create a copy target first and replace with the
	 * target node. ({@link #createCopy})
	 * @param node The node to be marked as replaced.
	 * @param replacingNode The node replacing the node.
	 * @param description Description of the change. 
	 */		
	public final void markAsReplaced(ASTNode node, ASTNode replacingNode, GroupDescription description) {
		int property= ASTNodeConstants.getPropertyOfNode(node);
		if (ASTNodeConstants.isListProperty(property)) {
			markAsReplaced(node.getParent(), property, node, replacingNode, description);
		} else {
			markAsReplaced(node.getParent(), property, replacingNode, description);
		}
	}
	
	/**
	 * Marks an existing node as replace by a new node. The replacing node node must not exist.
	 * To replace with an existing node (move or copy), create a copy target first and replace with the
	 * target node. ({@link #createCopy})
	 * @param node The node to be marked as replaced.
	 * @param replacingNode The node replacing the node.
	 */		
	public final void markAsReplaced(ASTNode node, ASTNode replacingNode) {
		markAsReplaced(node, replacingNode, (GroupDescription) null);
	}
	
	/**
	 * Create a placeholder for a sequence of new statements to be inserted or placed at a single place.
	 * @param children The target nodes to collapse
	 * @return A placeholder node that stands for all of the statements 
	 */
	
	public final Block getCollapseTargetPlaceholder(Statement[] children) {
		Block res= createCollapsePlaceholder();
		List statements= res.statements();
		for (int i= 0; i < children.length; i++) {
			statements.add(children[i]);
		}
		return res;
	}

	/**
	 * Marks an node as modified. The modifiued node describes changes like changed modifiers,
	 * or operators: This is only for properties that are not children nodes of type ASTNode.
	 * @param node The node to be marked as modified.
	 * @param modifiedNode The node of the same type as the modified node but with the new properties.
	 * @param description Description of the change. 
	 */			
	public final void markAsModified(ASTNode node, ASTNode modifiedNode, GroupDescription description) {
		int[] properties= ASTNodeConstants.getNodeChildProperties(node);
		for (int i= 0; i < properties.length; i++) {
			int property= properties[i];
			if (ASTNodeConstants.isAttributeProperty(property)) {
				NodeRewriteEvent event= getNodeEvent(node, property, true);
				Object newChild= ASTNodeConstants.getNodeChild(modifiedNode, property);
				event.setNewValue(newChild);
				setDescription(event, description);
			}
		}		
	}
	
	/**
	 * Marks an node as modified. The modifiied node describes changes like changed modifiers,
	 * or operators: This is only for properties that are not children nodes of type ASTNode.
	 * @param node The node to be marked as modified.
	 * @param modifiedNode The node of the same type as the modified node but with the new properties.
	 */		
	public final void markAsModified(ASTNode node, ASTNode modifiedNode) {
		markAsModified(node, modifiedNode, (GroupDescription) null);
	}
	
	/**
	 * Marks a node as tracked. The edits added to the group description can be used to get the
	 * position of the node after the rewrite operation.
	 * @param node
	 * @param description
	 */
	public final void markAsTracked(ASTNode node, GroupDescription description) {
		if (getTrackedNodeData(node) != null) {
			throw new IllegalArgumentException("Node is already marked as tracked"); //$NON-NLS-1$
		}
		setTrackedNodeData(node, description);
	}	
	
	/**
	 * Creates a target node for a node to be copied. A target node can be inserted or used
	 * to replace at the target position. 
	 * @param node
	 * @return
	 */
	public final ASTNode createCopy(ASTNode node) {
		validateIsInsideRoot(node);
		return createCopyPlaceholder(node);
	}
	
	/**
	 * Creates a target node for a node to be moved. A target node can be inserted or used
	 * to replace at the target position. The source node will be marked as removed, but the user can also
	 * override this by marking it as replaced.
	 * @param node
	 * @return
	 */
	public final ASTNode createMove(ASTNode node) {
		validateIsInsideRoot(node);
		if (getChangeProperty(node) == null) {
			markAsRemoved(node);
		}
		return createMovePlaceholder(node);
	}
	
	/**
	 * Succeeding nodes in a list are collapsed and represented by a new 'compound' node. The new compound node is inserted in the list
	 * and replaces the collapsed node. The compound node can be used for rewriting, e.g. a copy can be created to move
	 * a whole range of statements. This operation modifies the AST.
	 * @param list
	 * @param index
	 * @param length
	 * @return
	 */	
	public final ASTNode collapseNodes(List list, int index, int length) {
		Assert.isTrue(index >= 0 && length > 0 && list.size() >= (index + length), "Index or length out of bound"); //$NON-NLS-1$
		
		ASTNode firstNode= (ASTNode) list.get(index);
		ASTNode lastNode= (ASTNode) list.get(index + length - 1);
		validateIsInsideRoot(firstNode);
		validateIsInsideRoot(lastNode);
		
		Assert.isTrue(lastNode instanceof Statement, "Can only collapse statements"); //$NON-NLS-1$
		
		int startPos= firstNode.getStartPosition();
		int endPos= lastNode.getStartPosition() + lastNode.getLength();
				
		Block compoundNode= createCollapsePlaceholder();
		List children= compoundNode.statements();
		compoundNode.setSourceRange(startPos, endPos - startPos);
		
		int childProperty= ASTNodeConstants.getPropertyOfNode(firstNode);
		
		ListRewriteEvent existingEvent= getListEvent(firstNode.getParent(), childProperty, false);
		if (existingEvent != null) {
			RewriteEvent[] origChildren= existingEvent.getChildren();
			Assert.isTrue(origChildren.length == list.size());
			RewriteEvent[] newChildren= new RewriteEvent[origChildren.length - length + 1];
			System.arraycopy(origChildren, 0, newChildren, 0, index);
			newChildren[index]= new NodeRewriteEvent(compoundNode, compoundNode);
			System.arraycopy(origChildren, index + length, newChildren, index + 1, origChildren.length - index - length);
			addEvent(firstNode.getParent(), childProperty, new ListRewriteEvent(newChildren)); // replace
			
			RewriteEvent[] newCollapsedChildren= new RewriteEvent[length];
			System.arraycopy(origChildren, index, newCollapsedChildren, 0, length);
			addEvent(compoundNode, ASTNodeConstants.STATEMENTS, new ListRewriteEvent(newCollapsedChildren));
		}
		
		for (int i= 0; i < length; i++) {
			Object curr= list.remove(index);
			children.add(curr);
		}
		list.add(index, compoundNode);
		
		fHasASTModifications= true;
		
		return compoundNode;
	}
		
	public final boolean isInserted(ASTNode node) {
		return getChangeProperty(node) instanceof ASTInsert;
	}
		
	
	public final ASTNode getReplacingNode(ASTNode node) {
		RewriteEvent event= findEventByOriginal(node);
		if (event != null && event.getChangeKind() == RewriteEvent.REPLACED) {
			return (ASTNode) event.getNewValue();
		}
		return null;
	}
						
	private final void setChangeProperty(ASTNode node, ASTChange change) {
		fChangedProperties.put(node, change);
	}
	
	private final ASTChange getChangeProperty(ASTNode node) {
		return (ASTChange) fChangedProperties.get(node);
	}
		

	private static class ASTChange {
		public GroupDescription description;
	}
	
		
	private static final class ASTInsert extends ASTChange {
		public boolean isBoundToPrevious;	
	}
			
	private String getNodeString(ASTNode node) {
		StringBuffer buf= new StringBuffer();
		Object object= getPlaceholderData(node);
		if (object != null) {
			buf.append(object.toString());
		} else {
			buf.append(node);
		}
		if (isMoveSource(node)) {
			buf.append(" [move source]"); //$NON-NLS-1$
		}
		if (isCollapsed(node)) {
			buf.append(" [collapsed]"); //$NON-NLS-1$
		}
		Object change= getChangeProperty(node);
		if (change == null) {
			buf.append(" [unchanged]"); //$NON-NLS-1$
		}
		if (change instanceof ASTInsert) {
			buf.append(" [inserted]"); //$NON-NLS-1$
		}
		return buf.toString();
	}
	
	public String toString() {
		StringBuffer buf= new StringBuffer();
		for (Iterator iter= fChangedProperties.keySet().iterator(); iter.hasNext(); ) {
			ASTNode element= (ASTNode) iter.next();
			buf.append(getNodeString(element));
			buf.append('\n');
		}
		return buf.toString();
	}
	
}
