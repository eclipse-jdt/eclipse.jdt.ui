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
			} else if (object instanceof ASTReplace) {
				processChange(node, node, ((ASTReplace) object).replacingNode, object.description, processedListEvents);
			} else if (object instanceof ASTRemove) {
				processChange(node, node, null, object.description, processedListEvents);
			} else if (object instanceof ASTModify) {
				ASTNode modifiedNode= ((ASTModify) object).modifiedNode;
				int[] properties= ASTNodeConstants.getNodeChildProperties(node);
				for (int i= 0; i < properties.length; i++) {
					int property= properties[i];
					if (ASTNodeConstants.isAttributeProperty(property)) {
						NodeRewriteEvent event= getNodeEvent(node, property, true);
						Object newChild= ASTNodeConstants.getNodeChild(modifiedNode, property);
						event.setNewValue(newChild);
						setDescription(event, object.description);
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
				if (object instanceof ASTRemove) {
					RewriteEvent change= listEvent.removeEntry(curr);
					setDescription(change, object.description);
				} else if (object instanceof ASTReplace) {
					RewriteEvent change= listEvent.replaceEntry(curr, ((ASTReplace) object).replacingNode);
					setDescription(change, object.description);
				}
			}
		}
		testSame(modifiedList, listEvent.getChildren());
	}
	
	private void testSame(List list, RewriteEvent[] changes) {
		Assert.isTrue(list.size() == changes.length);
		for (int i= 0; i < changes.length; i++) {
			ASTNode curr= (ASTNode) list.get(i);
			Assert.isTrue(getChangeKind(curr) == changes[i].getChangeKind());
		}
	}
	
	private final int getChangeKind(ASTNode node) {
		ASTChange change= getChangeProperty(node);
		if (change == null) {
			return RewriteEvent.UNCHANGED;
		}
		if (change instanceof ASTInsert) {
			return RewriteEvent.INSERTED;
		} else if (change instanceof ASTReplace) {
			return RewriteEvent.REPLACED;
		} else if (change instanceof ASTRemove) {
			return RewriteEvent.REMOVED;
		}
		return RewriteEvent.UNCHANGED;
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
	 * Marks a node as inserted. The node must not exist. To insert an existing node (move or copy),
	 * create a copy target first and insert this target node. ({@link #createCopy})
	 * @param node The node to be marked as inserted.
	 * @param boundToPrevious If set, the inserted node is bound to the previous node, that means
	 * it is inserted directly after the previous node. If set to false the node is inserted before the next.
	 * This option is only used for nodes in lists.
	 * @param description Description of the change.
	 */
	public final void markAsInserted(ASTNode node, boolean boundToPrevious, GroupDescription description) {
		Assert.isTrue(!isCollapsed(node), "Tries to insert a collapsed node"); //$NON-NLS-1$
		ASTInsert insert= new ASTInsert();
		insert.isBoundToPrevious= boundToPrevious;
		insert.description= description;
		setChangeProperty(node, insert);
		fHasASTModifications= true;
	}

	/**
	 * Marks a node as inserted. The node must not exist. To insert an existing node (move or copy),
	 * create a copy target first and insert this target node. ({@link #createCopy})
	 * @param node The node to be marked as inserted.
	 * @param boundToPrevious If set, the inserted node is bound to the previous node, that means
	 * it is inserted directly after the previous node. If set to false the node is inserted before the next.
	 * This option is only used for nodes in lists.
	 */
	public final void markAsInserted(ASTNode node, boolean boundToPrevious) {
		markAsInserted(node, boundToPrevious, (GroupDescription) null);
	}
			
	/**
	 * Marks a node as inserted. The node must not exist. To insert an existing node (move or copy),
	 * create a copy target first and insert this target node. ({@link #createCopy})
	 * @param node The node to be marked as inserted.
	 * @param description Description of the change.
	 */
	public final void markAsInserted(ASTNode node, GroupDescription description) {
		markAsInserted(node, getDefaultBoundBehaviour(node), description);
	}

	/**
	 * Marks a node as inserted. The node must not exist. To insert an existing node (move or copy),
	 * create a copy target first and insert this target node. ({@link #createCopy})
	 * @param node The node to be marked as inserted.
	 */
	public final void markAsInserted(ASTNode node) {
		markAsInserted(node, getDefaultBoundBehaviour(node), (GroupDescription) null);
	}
	
	private boolean getDefaultBoundBehaviour(ASTNode node) {
		return (node instanceof Statement || node instanceof FieldDeclaration);
	}
	
	
	public boolean hasASTModifications() {
		return fHasASTModifications;
	}
	
	/**
	 * Marks an existing node as removed.
	 * @param node The node to be marked as removed.
	 * @param description Description of the change.
	 */
	public final void markAsRemoved(ASTNode node, GroupDescription description) {
		assertIsInside(node);
		ASTRemove remove= new ASTRemove();
		remove.description= description;
		setChangeProperty(node, remove);
	}

	/**
	 * Marks an existing node as removed.
	 * @param node The node to be marked as removed.
	 */	
	public final void markAsRemoved(ASTNode node) {
		markAsRemoved(node, (GroupDescription) null);
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
		Assert.isTrue(replacingNode != null, "Tries to replace with null (use remove instead)"); //$NON-NLS-1$
		Assert.isTrue(replacingNode.getStartPosition() == -1, "Tries to replace with existing node"); //$NON-NLS-1$
		assertIsInside(node);
		ASTReplace replace= new ASTReplace();	
		replace.replacingNode= replacingNode;
		replace.description= description;
		setChangeProperty(node, replace);
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
	 * Marks the node <code>node</code> as replaced with the nodes provided in <code>
	 * replacements</code>. The given node must be a member of the passed list <code>
	 * container</code>.
	 * 
	 * @param node the node to be replaced
	 * @param container the list <code>node</code> is a member of
	 * @param replacements the replacing nodes
	 */	
	public final void markAsReplaced(ASTNode node, List container, ASTNode[] replacements) {
		markAsReplaced(node, container, replacements, (GroupDescription) null);
	}

	/**
	 * Marks an node as modified. The modifiued node describes changes like changed modifiers,
	 * or operators: This is only for properties that are not children nodes of type ASTNode.
	 * @param node The node to be marked as modified.
	 * @param modifiedNode The node of the same type as the modified node but with the new properties.
	 * @param description Description of the change. 
	 */			
	public final void markAsModified(ASTNode node, ASTNode modifiedNode, GroupDescription description) {
		Assert.isTrue(node.getClass().equals(modifiedNode.getClass()), "Tries to modify with a node of different type"); //$NON-NLS-1$
		assertIsInside(node);
		ASTModify modify= new ASTModify();
		modify.modifiedNode= modifiedNode;
		modify.description= description;
		setChangeProperty(node, modify);
	}

	/**
	 * Marks the node <code>node</code> as replaced with the nodes provided in <code>
	 * replacements</code>. The given node must be a member of the passed list <code>
	 * container</code>.
	 * 
	 * @param node the node to be replaced
	 * @param container the list <code>node</code> is a member of
	 * @param replacements the replacing nodes
	 * @param description the description of the change
	 */	
	public final void markAsReplaced(ASTNode node, List container, ASTNode[] replacements, GroupDescription description) {
		if (replacements == null || replacements.length == 0) {
			markAsRemoved(node, description);
			return;
		}
		Assert.isNotNull(container, "Replacing a node with a list of nodes requires access to container"); //$NON-NLS-1$
		int index= container.indexOf(node);
		Assert.isTrue(index != -1, "Node must be a member of the given list container"); //$NON-NLS-1$
		markAsReplaced(node, replacements[0], description);
		for (int i= 1; i < replacements.length; i++) {
			ASTNode replacement= replacements[i];
			markAsInserted(replacement, description);
			container.add(++index, replacement);
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
		Assert.isTrue(getTrackedNodeData(node) == null, "Node is already marked as tracked"); //$NON-NLS-1$

		setTrackedNodeData(node, description);
	}	
	
	/**
	 * Creates a target node for a node to be copied. A target node can be inserted or used
	 * to replace at the target position. 
	 * @param node
	 * @return
	 */
	public final ASTNode createCopy(ASTNode node) {
		assertIsInside(node);
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
		assertIsInside(node);
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
		assertIsInside(firstNode);
		assertIsInside(lastNode);
		
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
	
	public final boolean isReplaced(ASTNode node) {
		return getChangeProperty(node) instanceof ASTReplace;
	}
	
	public final boolean isRemoved(ASTNode node) {
		return getChangeProperty(node) instanceof ASTRemove;
	}	
		
	
	public final ASTNode getReplacingNode(ASTNode node) {
		Object info= getChangeProperty(node);
		if (info instanceof ASTReplace) {
			return ((ASTReplace) info).replacingNode;
		}
		return null;
	}
						
	private final void setChangeProperty(ASTNode node, ASTChange change) {
		fChangedProperties.put(node, change);
	}
	
	private final ASTChange getChangeProperty(ASTNode node) {
		return (ASTChange) fChangedProperties.get(node);
	}
		
	private void assertIsInside(ASTNode node) {
		int endPos= node.getStartPosition() + node.getLength();
		ASTNode rootNode= getRootNode();
		if (rootNode.getStartPosition() > node.getStartPosition() || rootNode.getStartPosition() + rootNode.getLength() < endPos) {
			Assert.isTrue(false, "Node that is changed is not located inside of ASTRewrite root"); //$NON-NLS-1$
		}
	}
	private static class ASTChange {
		public GroupDescription description;
	}
	
		
	private static final class ASTInsert extends ASTChange {
		public boolean isBoundToPrevious;	
	}
	
	private static final class ASTRemove extends ASTChange {
	}	
		
	private static final class ASTReplace extends ASTChange {
		public ASTNode replacingNode;
	}
	
	private static final class ASTModify extends ASTChange {
		public ASTNode modifiedNode;
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
		} else if (change instanceof ASTReplace) {
			buf.append(" [replaced "); //$NON-NLS-1$
			buf.append(getNodeString(getReplacingNode(node)));
			buf.append(']');
		} else if (change instanceof ASTModify) {
			buf.append(" [modified]"); //$NON-NLS-1$
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
