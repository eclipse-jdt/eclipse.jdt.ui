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

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.textmanipulation.CopySourceEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.MultiTextEdit;
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
 *   Statement copyTarget= (Statement) rewrite.createCopyTarget(movedNode);
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
public class ASTRewrite {
	
	private static final String CHANGEKEY= "ASTChangeData";
	private static final String COPYSOURCEKEY= "ASTCopySource";
	
	private ASTNode fRootNode;
		
	public ASTRewrite(ASTNode node) {
		fRootNode= node;
	}
	
	/**
	 * Perform rewriting: Analyses AST modifications and creates text edits that describe changes to the
	 * underlying code. Edits do only change code when the corresponding node has changed. New code
	 * is formatted using the standard code formatter.
	 * @param textBuffer Text buffer which is describing the code of the AST passed in in the
	 * constructor.
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
	 * Marks a node as inserted. The node must not exist. To insert an existing node (move or copy),
	 * create a copy target first and insert this target node. ({@link createCopyTarget})
	 * @param node The node to be marked as inserted.
	 * @param node Description of the change.
	 */
	public final void markAsInserted(ASTNode node, String description) {
		Assert.isTrue(node.getStartPosition() == -1, "Tries to insert an existing node");
		ASTInsert insert= new ASTInsert();
		insert.description= description;
		node.setProperty(CHANGEKEY, insert);		
	}

	/**
	 * Marks a node as inserted. The node must not exist. To insert an existing node (move or copy),
	 * create a copy target first and insert this target node. ({@link createCopyTarget})
	 */
	public final void markAsInserted(ASTNode node) {
		markAsInserted(node, null);
	}

	/**
	 * Marks an existing node as removed.
	 * @param node The node to be marked as removed.
	 * @param node Description of the change.
	 */
	public final void markAsRemoved(ASTNode node, String description) {
		ASTRemove remove= new ASTRemove();
		remove.description= description;
		node.setProperty(CHANGEKEY, remove);	
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
	 * target node. ({@link createCopyTarget})
	 * @param node The node to be marked as replaced.
	 * @param node The node replacing the node.
	 * @param node Description of the change. 
	 */		
	public final void markAsReplaced(ASTNode node, ASTNode replacingNode, String description) {
		Assert.isTrue(replacingNode != null, "Tries to replace with null (use remove instead)");
		Assert.isTrue(replacingNode.getStartPosition() == -1, "Tries to replace with existing node");
		ASTReplace replace= new ASTReplace();
		replace.replacingNode= replacingNode;
		replace.description= description;
		node.setProperty(CHANGEKEY, replace);
	}


	/**
	 * Marks an existing node as replace by a new node. The replacing node node must not exist.
	 * To replace with an existing node (move or copy), create a copy target first and replace with the
	 * target node. ({@link createCopyTarget})
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
		Assert.isTrue(node.getClass().equals(modifiedNode.getClass()), "Tries to modify with a node of different type");
		ASTModify modify= new ASTModify();
		modify.modifiedNode= modifiedNode;
		modify.description= description;		
		node.setProperty(CHANGEKEY, modify);
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
		Assert.isTrue(node.getProperty(COPYSOURCEKEY) == null, "Node used as more than one copy source");
		Object copySource= ASTRewriteAnalyzer.createSourceCopy(node);
		node.setProperty(COPYSOURCEKEY, copySource);
		return ASTWithExistingFlattener.getPlaceholder(node.getAST(), node, node);
	}
	
	/**
	 * Creates a target node for a source string to be inserted without being formatted. A target node can
	 * be inserted or used to replace at the target position.
	 * @param code String that will be inserted. The string must have no extra indent.
	 * @param similarNode A node that is similar to the code which is inserted. This node is only used to
	 * find a similar placeholder (Expression, Statement, BodyDeclaration...). No operations are performed
	 * on this node.
	 */
	public final ASTNode createPlaceholder(String code, ASTNode similarNode) {
		return ASTWithExistingFlattener.getPlaceholder(fRootNode.getAST(), code, similarNode);
	}	
	public final boolean isInserted(ASTNode node) {
		return node.getProperty(CHANGEKEY) instanceof ASTInsert;
	}
	
	public final boolean isReplaced(ASTNode node) {
		return node.getProperty(CHANGEKEY) instanceof ASTReplace;
	}
	
	public final boolean isRemoved(ASTNode node) {
		return node.getProperty(CHANGEKEY) instanceof ASTRemove;
	}	
	
	public final boolean isModified(ASTNode node) {
		return node.getProperty(CHANGEKEY) instanceof ASTModify;
	}
	
	public final String getDescription(ASTNode node) {
		Object info= node.getProperty(CHANGEKEY);
		if (info instanceof ASTChange) {
			return ((ASTChange) info).description;
		}
		return null;
	}
	
	public final ASTNode getModifiedNode(ASTNode node) {
		Object info= node.getProperty(CHANGEKEY);
		if (info instanceof ASTModify) {
			return ((ASTModify) info).modifiedNode;
		}
		return null;
	}

	public final ASTNode getReplacingNode(ASTNode node) {
		Object info= node.getProperty(CHANGEKEY);
		if (info instanceof ASTReplace) {
			return ((ASTReplace) info).replacingNode;
		}
		return null;
	}
	
	public final CopySourceEdit getCopySourceEdit(ASTNode node) {
		return (CopySourceEdit) node.getProperty(COPYSOURCEKEY);
	}

	private static final class ASTCopySource {
		public CopySourceEdit copySource;
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
