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

import java.util.Collection;
import java.util.HashMap;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;

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
public final class ASTRewrite {
	
	/** Constant used to create place holder nodes */
	public static final int UNKNOWN= -1;
	public static final int BODY_DECLARATION= 1;
	public static final int BLOCK= 2;
	public static final int EXPRESSION= 3;
	public static final int STATEMENT= 4;
	public static final int SINGLEVAR_DECLARATION= 5;
	public static final int TYPE= 6;
	public static final int JAVADOC= 7;
	public static final int VAR_DECLARATION_FRAGMENT= 8;
	
	private ASTNode fRootNode;
	
	private HashMap fChangedProperties;
	private HashMap fCopiedProperties;
	private boolean fHasInserts;
	
	/**
	 * Creates the <code>ASTRewrite</code> object.	 * @param node A node which is parent to all modified or changed nodes.	 */
	public ASTRewrite(ASTNode node) {
		fRootNode= node;
		fChangedProperties= new HashMap();
		fCopiedProperties= new HashMap();
		fHasInserts= false;
	}
	
	public ASTNode getRootNode() {
		return fRootNode;
	}
	
	
	/**
	 * Perform rewriting: Analyses AST modifications and creates text edits that describe changes to the
	 * underlying code. Edits do only change code when the corresponding node has changed. New code
	 * is formatted using the standard code formatter.
	 * @param textBuffer Text buffer which is describing the code of the AST passed in in the
	 * constructor. This buffer is accessed read-only.
	 * @param groupDescription All resulting GroupDescription will be added to this collection.
	 * <code>null</code> can be passed, if no descriptions should be collected.
	 */
	public void rewriteNode(TextBuffer textBuffer, TextEdit rootEdit, Collection resultingGroupDescription) {
		HashMap descriptions= resultingGroupDescription == null ? null : new HashMap(5);
		ASTRewriteAnalyzer visitor= new ASTRewriteAnalyzer(textBuffer, rootEdit, this, descriptions);
		fRootNode.accept(visitor); 
		if (resultingGroupDescription != null) {
			resultingGroupDescription.addAll(descriptions.values());
		}
	}
	
	/**
	 * Removes all modifications applied to the given AST.	 */
	public void removeModifications() {
		if (fHasInserts) {
			fRootNode.accept(new ASTRewriteClear(this));
			fHasInserts= false;
		}
		fChangedProperties.clear();
		fCopiedProperties.clear();
	}
	
	/**
	 * Marks a node as inserted. The node must not exist. To insert an existing node (move or copy),
	 * create a copy target first and insert this target node. ({@link createCopy})
	 * @param node The node to be marked as inserted.
	 * @param node Description of the change.
	 */
	public final void markAsInserted(ASTNode node, String description) {
		ASTInsert insert= new ASTInsert();
		insert.description= description;
		setChangeProperty(node, insert);
		fHasInserts= true;
	}

	/**
	 * Marks a node as inserted. The node must not exist. To insert an existing node (move or copy),
	 * create a copy target first and insert this target node. ({@link createCopy})
	 */
	public final void markAsInserted(ASTNode node) {
		markAsInserted(node, null);
	}
	
	public boolean hasInserts() {
		return fHasInserts;
	}

	/**
	 * Marks an existing node as removed.
	 * @param node The node to be marked as removed.
	 * @param node Description of the change.
	 */
	public final void markAsRemoved(ASTNode node, String description) {
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
		markAsRemoved(node, null);
	}

	/**
	 * Marks an existing node as replace by a new node. The replacing node node must not exist.
	 * To replace with an existing node (move or copy), create a copy target first and replace with the
	 * target node. ({@link createCopy})
	 * @param node The node to be marked as replaced.
	 * @param node The node replacing the node.
	 * @param node Description of the change. 
	 */		
	public final void markAsReplaced(ASTNode node, ASTNode replacingNode, String description) {
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
	 * target node. ({@link createCopy})
	 * @param node The node to be marked as replaced.
	 * @param node The node replacing the node.
	 */		
	public final void markAsReplaced(ASTNode node, ASTNode replacingNode) {
		markAsReplaced(node, replacingNode, null);
	}

	/**
	 * Marks an node as modified. The modifiued node describes changes like changed modifiers,
	 * or operators: This is only for properties that are not children nodes of type ASTNode.
	 * @param node The node to be marked as modified.
	 * @param node The node of the same type as the modified node but with the new properties.
	 * @param node Description of the change. 
	 */			
	public final void markAsModified(ASTNode node, ASTNode modifiedNode, String description) {
		Assert.isTrue(node.getClass().equals(modifiedNode.getClass()), "Tries to modify with a node of different type"); //$NON-NLS-1$
		assertIsInside(node);
		ASTModify modify= new ASTModify();
		modify.modifiedNode= modifiedNode;
		modify.description= description;
		setChangeProperty(node, modify);
	}

	/**
	 * Marks an node as modified. The modifiued node describes changes like changed modifiers,
	 * or operators: This is only for properties that are not children nodes of type ASTNode.
	 * @param node The node to be marked as modified.
	 * @param node The node of the same type as the modified node but with the new properties.
	 */		
	public final void markAsModified(ASTNode node, ASTNode modifiedNode) {
		markAsModified(node, modifiedNode, null);
	}
	
	/**
	 * Creates a target node for a node to be moved or copied. A target node can be inserted or used
	 * to replace at the target position. 
	 */
	public final ASTNode createCopy(ASTNode node) {
		Assert.isTrue(node.getStartPosition() != -1, "Tries to copy a non-existing node"); //$NON-NLS-1$
		Assert.isTrue(getCopySourceEdit(node) == null, "Node used as more than one copy source"); //$NON-NLS-1$
		assertIsInside(node);
		Object copySource= ASTRewriteAnalyzer.createSourceCopy(node.getStartPosition(), node.getLength());
		setCopySourceEdit(node, copySource);
		
		int placeHolderType= getPlaceholderType(node);
		if (placeHolderType == UNKNOWN) {
			Assert.isTrue(false, "Can not create copy for elements of type " + node.getClass().getName()); //$NON-NLS-1$
		}
		return ASTWithExistingFlattener.createPlaceholder(node.getAST(), node, placeHolderType);
	}
	
	/**
	 * Creates a target node for a node to be moved or copied. A target node can be inserted or used
	 * to replace at the target position. 
	 */
	public final ASTNode createCopy(ASTNode startNode, ASTNode endNode) {
		Assert.isTrue(startNode.getStartPosition() != -1, "Tries to copy a non-existing node"); //$NON-NLS-1$
		Assert.isTrue(endNode.getStartPosition() != -1, "Tries to copy a non-existing node"); //$NON-NLS-1$
		Assert.isTrue(getCopySourceEdit(startNode) == null, "Start node used as more than one copy source "); //$NON-NLS-1$
		Assert.isTrue(getCopySourceEdit(endNode) == null, "End node used as more than one copy source "); //$NON-NLS-1$
		Assert.isTrue(startNode.getParent() == endNode.getParent(), "Nodes must have same parent"); //$NON-NLS-1$
		assertIsInside(startNode);
		assertIsInside(endNode);
		int start= startNode.getStartPosition();
		int end= endNode.getStartPosition() + endNode.getLength();
		Assert.isTrue(start < end, "Start node must have smaller offset than end node"); //$NON-NLS-1$

		Object copySource= ASTRewriteAnalyzer.createSourceCopy(start, end - start);
		setCopySourceEdit(startNode, copySource);
		setCopySourceEdit(endNode, copySource);
		int placeHolderType= getPlaceholderType(startNode);
		if (placeHolderType == UNKNOWN) {
			Assert.isTrue(false, "Can not create copy for elements of type " + startNode.getClass().getName()); //$NON-NLS-1$
		}		
		return ASTWithExistingFlattener.createPlaceholder(startNode.getAST(), startNode, placeHolderType);
	}	
	
	/**
	 * Creates a target node for a source string to be inserted without being formatted. A target node can
	 * be inserted or used to replace at the target position.
	 * @param code String that will be inserted. The string must have no extra indent.
	 * @param nodeType the type of the place holder. Valid values are <code>BODY_DECLARATION</code>,
	 * <code>BLOCK</code>, <code>STATEMENT</code>, <code>SINGLEVAR_DECLARATION</code>,
	 * <code>TYPE</code>, <code>EXPRESSION</code> and <code>JAVADOC</code> .
	 * @return the place holder node
	 */
	public final ASTNode createPlaceholder(String code, int nodeType) {
		return ASTWithExistingFlattener.createPlaceholder(fRootNode.getAST(), code, nodeType);
	}
	
	/**
	 * Returns the node type that should be used to create a place holder for the given node
	 * <code>existingNode</code>.
	 * 	 * @param existingNode an existing node for which a place holder is to be created	 * @return the node type of a potential place holder	 */
	public static int getPlaceholderType(ASTNode existingNode) {
		if (existingNode instanceof Expression) {
			return EXPRESSION;
		} else if (existingNode instanceof Statement) {
			if (existingNode.getNodeType() == ASTNode.BLOCK) {
				return BLOCK;
			} else {
				return STATEMENT;
			}
		} else if (existingNode instanceof BodyDeclaration) {
			return BODY_DECLARATION;
		} else if (existingNode instanceof SingleVariableDeclaration) {
			return SINGLEVAR_DECLARATION;
		} else if (existingNode instanceof VariableDeclarationFragment) {
			return VAR_DECLARATION_FRAGMENT;
		} else if (existingNode instanceof Type) {
			return TYPE;
		} else if (existingNode instanceof Javadoc) {
			return JAVADOC;				
		} else {
			return UNKNOWN;
		}
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
	
	public final boolean isModified(ASTNode node) {
		return getChangeProperty(node) instanceof ASTModify;
	}
	
	public final String getDescription(ASTNode node) {
		Object info= getChangeProperty(node);
		if (info instanceof ASTChange) {
			return ((ASTChange) info).description;
		}
		return null;
	}
	
	public final ASTNode getModifiedNode(ASTNode node) {
		Object info= getChangeProperty(node);
		if (info instanceof ASTModify) {
			return ((ASTModify) info).modifiedNode;
		}
		return null;
	}

	public final ASTNode getReplacingNode(ASTNode node) {
		Object info= getChangeProperty(node);
		if (info instanceof ASTReplace) {
			return ((ASTReplace) info).replacingNode;
		}
		return null;
	}
	
	public void clearMark(ASTNode node) {
		setCopySourceEdit(node, null);
		setChangeProperty(node, null);
	}

	private static final String CHANGEKEY= "ASTChangeData"; //$NON-NLS-1$
	private static final String COPYSOURCEKEY= "ASTCopySource"; //$NON-NLS-1$
	
	private final void setChangeProperty(ASTNode node, ASTChange change) {
		fChangedProperties.put(node, change);
	}
	
	private final Object getChangeProperty(ASTNode node) {
		return fChangedProperties.get(node);
	}
	
	private final void setCopySourceEdit(ASTNode node, Object copySource) {
		fCopiedProperties.put(node, copySource);
	}
	
	/* package */ final Object getCopySourceEdit(ASTNode node) {
		return fCopiedProperties.get(node);
	}
	
	private void assertIsInside(ASTNode node) {
		int endPos= node.getStartPosition() + node.getLength();
		if (fRootNode.getStartPosition() > node.getStartPosition() || fRootNode.getStartPosition() + fRootNode.getLength() < endPos) {
			Assert.isTrue(false, "Node that is changed is not located inside of ASTRewrite root"); //$NON-NLS-1$
		}
	}
	

	private static class ASTChange {
		String description;
	}		
	
	private static final class ASTInsert extends ASTChange {
	}
	
	private static final class ASTRemove extends ASTChange {
	}	
		
	private static final class ASTReplace extends ASTChange {
		public ASTNode replacingNode;
	}
	
	private static final class ASTModify extends ASTChange {
		public ASTNode modifiedNode;
	}
}
