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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.core.dom.*;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.textmanipulation.GroupDescription;
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
	public static final int BLOCK= 2;
	public static final int EXPRESSION= 3;
	public static final int STATEMENT= 4;
	public static final int SINGLEVAR_DECLARATION= 5;
	public static final int TYPE= 6;
	public static final int JAVADOC= 7;
	public static final int VAR_DECLARATION_FRAGMENT= 8;
	public static final int TYPE_DECLARATION= 9;
	public static final int FIELD_DECLARATION= 10;
	public static final int METHOD_DECLARATION= 11;
	public static final int INITIALIZER= 12;
	
	/** Constant used to describe the kind of the change */
	public static final int INSERTED= 1;
	public static final int REMOVED= 2;
	public static final int REPLACED= 3;
	public static final int UNCHANGED= 4;
	
	// properties used on nodes
	private static final String COMPOUND_CHILDREN= "collapsed"; //$NON-NLS-1$
	
	private ASTNode fRootNode;
	
	private HashMap fChangedProperties;
	private HashMap fCopyCounts;
	private HashSet fMoveSources;
	
	private HashMap fTrackedNodes;
	private HashMap fPlaceholderNodes;
	
	private HashMap fGroupDescriptions;
	private boolean fHasASTModifications;
	
	/**
	 * Creates the <code>ASTRewrite</code> object.
	 * @param node A node which is parent to all modified, changed or tracked nodes.
	 */
	public ASTRewrite(ASTNode node) {
		fRootNode= node;
		fChangedProperties= new HashMap();
		fCopyCounts= null;
		fMoveSources= null;
		fTrackedNodes= null;
		fPlaceholderNodes= null;
		fHasASTModifications= false;
	}
	
	public ASTNode getRootNode() {
		return fRootNode;
	}
	
	/**
	 * @deprecated use rewriteNode(TextBuffer, TextEdit)
	 */
	public final void rewriteNode(TextBuffer textBuffer, TextEdit rootEdit, Collection resultingGroupDescription) {
		ASTRewriteAnalyzer visitor= new ASTRewriteAnalyzer(textBuffer, rootEdit, this);
		fRootNode.accept(visitor);
		if (resultingGroupDescription != null && fGroupDescriptions != null) {
			resultingGroupDescription.addAll(fGroupDescriptions.values());
		}
	}
	
	/**
	 * Perform rewriting: Analyses AST modifications and creates text edits that describe changes to the
	 * underlying code. Edits do only change code when the corresponding node has changed. New code
	 * is formatted using the standard code formatter.
	 * @param textBuffer Text buffer which is describing the code of the AST passed in in the
	 * constructor. This buffer is accessed read-only.
	 */
	public final void rewriteNode(TextBuffer textBuffer, TextEdit rootEdit) {
		ASTRewriteAnalyzer visitor= new ASTRewriteAnalyzer(textBuffer, rootEdit, this);
		fRootNode.accept(visitor); 
	}
	
	/**
	 * Removes all modifications applied to the given AST.
	 */
	public final void removeModifications() {
		if (fHasASTModifications) {
			fRootNode.accept(new ASTRewriteClear(this));
			fHasASTModifications= false;
		}
		fChangedProperties.clear();
		fCopyCounts= null;
		fMoveSources= null;
		fPlaceholderNodes= null;
		fTrackedNodes= null;
		fGroupDescriptions= null;
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
		Assert.isTrue(getCollapsedNodes(node) == null, "Tries to insert a collapsed node"); //$NON-NLS-1$
		ASTInsert insert= new ASTInsert();
		insert.isBoundToPrevious= boundToPrevious;
		setChangeProperty(node, insert);
		fHasASTModifications= true;
		setDescription(node, description);
	}

	/**
	 * @deprecated use markAsInserted(node, boolean, GroupDescription)
	 */
	public final void markAsInserted(ASTNode node, boolean boundToPrevious, String description) {
		markAsInserted(node, boundToPrevious, getGroupDescriptionForName(description));
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
	 * @deprecated
	 */
	public final void markAsInserted(ASTNode node, String description) {
		markAsInserted(node, getGroupDescriptionForName(description));
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
	 * @deprecated Use markAsRemoved(node, GroupDescription)
	 */
	public final void markAsRemoved(ASTNode node, String description) {
		markAsRemoved(node, getGroupDescriptionForName(description));
	}
	
	/**
	 * Marks an existing node as removed.
	 * @param node The node to be marked as removed.
	 * @param node Description of the change.
	 */
	public final void markAsRemoved(ASTNode node, GroupDescription description) {
		assertIsInside(node);
		ASTRemove remove= new ASTRemove();
		setChangeProperty(node, remove);
		setDescription(node, description);
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
	 * @param node The node replacing the node.
	 * @param node Description of the change. 
	 */		
	public final void markAsReplaced(ASTNode node, ASTNode replacingNode, GroupDescription description) {
		Assert.isTrue(replacingNode != null, "Tries to replace with null (use remove instead)"); //$NON-NLS-1$
		Assert.isTrue(replacingNode.getStartPosition() == -1, "Tries to replace with existing node"); //$NON-NLS-1$
		assertIsInside(node);
		ASTReplace replace= new ASTReplace();	
		replace.replacingNode= replacingNode;
		setChangeProperty(node, replace);
		setDescription(node, description);
	}
	
	/**
	 * @deprecated use markAsReplaced(ASTNode, ASTNode, GroupDescription)
	 */
	public final void markAsReplaced(ASTNode node, ASTNode replacingNode, String description) {
		markAsReplaced(node, replacingNode, getGroupDescriptionForName(description));
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
	 * @deprecated
	 */
	public final void markAsModified(ASTNode node, ASTNode modifiedNode, String description) {
		markAsModified(node, modifiedNode, getGroupDescriptionForName(description));
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
		setChangeProperty(node, modify);
		setDescription(node, description);
	}

	/**
	 * @deprecated use markAsReplaced(ASTNode, List, ASTNode[], GroupDescription)
	 */
	public final void markAsReplaced(ASTNode node, List container, ASTNode[] replacements, String description) {
		markAsReplaced(node, container, replacements, getGroupDescriptionForName(description));
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
	 */
	public final void markAsTracked(ASTNode node, GroupDescription description) {
		Assert.isTrue(getTrackedNodeData(node) == null, "Node is already marked as tracked"); //$NON-NLS-1$

		setTrackedNodeData(node, description);
	}	
	
	/**
	 * Creates a target node for a node to be copied. A target node can be inserted or used
	 * to replace at the target position. 
	 */
	public final ASTNode createCopy(ASTNode node) {
		Assert.isTrue(node.getStartPosition() != -1, "Tries to copy a non-existing node"); //$NON-NLS-1$
		assertIsInside(node);
		
		incrementCopyCount(node);
		
		int placeHolderType= getPlaceholderType(node);
		if (placeHolderType == UNKNOWN) {
			Assert.isTrue(false, "Can not create copy for elements of type " + node.getClass().getName()); //$NON-NLS-1$
		}
		CopyPlaceholderData data= new CopyPlaceholderData();
		data.node= node;
		return createPlaceholder(data, placeHolderType);
	}
	
	/**
	 * Creates a target node for a node to be moved. A target node can be inserted or used
	 * to replace at the target position. The source node will be marked as removed, but the user can also
	 * override this by marking it as replaced.
	 */
	public final ASTNode createMove(ASTNode node) {
		Assert.isTrue(node.getStartPosition() != -1, "Tries to move a non-existing node"); //$NON-NLS-1$
		Assert.isTrue(!isMoveSource(node), "Node already marked as moved"); //$NON-NLS-1$
		assertIsInside(node);
		int placeHolderType= getPlaceholderType(node);
		if (placeHolderType == UNKNOWN) {
			Assert.isTrue(false, "Can not create move for elements of type " + node.getClass().getName()); //$NON-NLS-1$
		}
		
		setAsMoveSource(node);
		if (getChangeProperty(node) == null) {
			markAsRemoved(node);
		}
		
		MovePlaceholderData data= new MovePlaceholderData();
		data.node= node;
		return createPlaceholder(data, placeHolderType);
	}	
	
	/**
	 * Succeeding nodes in a list are collapsed and represented by a new 'compound' node. The new compound node is inserted in the list
	 * and replaces the collapsed node. The compound node can be used for rewriting, e.g. a copy can be created to move
	 * a whole range of statements. This operation modifies the AST.
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
		
		ArrayList children= new ArrayList(length);
		
		ASTNode compoundNode= fRootNode.getAST().newBlock();
		compoundNode.setProperty(COMPOUND_CHILDREN, children);
		compoundNode.setSourceRange(startPos, endPos - startPos);		
		
		for (int i= 0; i < length; i++) {
			children.add(list.get(index));
			list.remove(index);
		}
		list.add(index, compoundNode);
		
		fHasASTModifications= true;
		
		return compoundNode;
	}
	
	/**
	 * Returns the nodes that are collapsed by this compound node. If the node is not a compound node <code>null</code>
	 * is returned.
	 */
	public List getCollapsedNodes(ASTNode compoundNode) {
		return (List) compoundNode.getProperty(COMPOUND_CHILDREN);
	}
	
	
	/**
	 * Creates a target node for a source string to be inserted without being formatted. A target node can
	 * be inserted or used to replace at the target position.
	 * @param code String that will be inserted. The string must not have extra indent.
	 * @param nodeType the type of the place holder. Valid values are <code>METHOD_DECLARATION</code>,
	 * <code>FIELD_DECLARATION</code>, <code>INITIALIZER</code>,
	 * <code>TYPE_DECLARATION</code>, <code>BLOCK</code>, <code>STATEMENT</code>,
	 *  <code>SINGLEVAR_DECLARATION</code>,<code> VAR_DECLARATION_FRAGMENT</code>,
	 * <code>TYPE</code>, <code>EXPRESSION</code> and <code>JAVADOC</code> .
	 * @return the place holder node
	 */
	public final ASTNode createPlaceholder(String code, int nodeType) {
		StringPlaceholderData data= new StringPlaceholderData();
		data.code= code;
		return createPlaceholder(data, nodeType);
	}
	
	private final ASTNode createPlaceholder(PlaceholderData data, int nodeType) {
		AST ast= fRootNode.getAST();
		ASTNode placeHolder;
		switch (nodeType) {
			case ASTRewrite.EXPRESSION:
				placeHolder= ast.newSimpleName("z"); //$NON-NLS-1$
				break;
			case ASTRewrite.TYPE:
				placeHolder= ast.newSimpleType(ast.newSimpleName("X")); //$NON-NLS-1$
				break;				
			case ASTRewrite.STATEMENT:
				placeHolder= ast.newReturnStatement();
				break;
			case ASTRewrite.BLOCK:
				placeHolder= ast.newBlock();
				break;
			case ASTRewrite.METHOD_DECLARATION:
				placeHolder= ast.newMethodDeclaration();
				break;
			case ASTRewrite.FIELD_DECLARATION:
				placeHolder= ast.newFieldDeclaration(ast.newVariableDeclarationFragment());
				break;
			case ASTRewrite.INITIALIZER:
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
			case ASTRewrite.TYPE_DECLARATION:
				placeHolder= ast.newTypeDeclaration();
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
		if (existingNode instanceof Expression) {
			return EXPRESSION;
		} else if (existingNode instanceof Statement) {
			if (existingNode.getNodeType() == ASTNode.BLOCK) {
				return BLOCK;
			} else {
				return STATEMENT;
			}
		} else if (existingNode instanceof TypeDeclaration) {
			return TYPE_DECLARATION;
		} else if (existingNode instanceof MethodDeclaration) {
			return METHOD_DECLARATION;
		} else if (existingNode instanceof FieldDeclaration) {
			return FIELD_DECLARATION;
		} else if (existingNode instanceof Initializer) {
			return INITIALIZER;					
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
	
	// to be removed
	private GroupDescription getGroupDescriptionForName(String name) {
		if (name == null) {
			return null;
		}
		if (fGroupDescriptions != null) {
			Iterator iter= fGroupDescriptions.values().iterator();
			while (iter.hasNext()) {
				GroupDescription curr= (GroupDescription) iter.next();
				if (name.equals(curr.getName())) {
					return curr;
				}
			}
		}
		return new GroupDescription(name);
	}
	
	
	public final int getChangeKind(ASTNode node) {
		Object object= getChangeProperty(node);
		if (object == null) {
			return UNCHANGED;
		}
		if (object instanceof ASTInsert) {
			return INSERTED;
		} else if (object instanceof ASTReplace) {
			return REPLACED;
		} else if (object instanceof ASTRemove) {
			return REMOVED;
		}
		return UNCHANGED;
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
	
	public final boolean isCollapsed(ASTNode node) {
		return node.getProperty(COMPOUND_CHILDREN) instanceof List;
	}
	
	public final int getCopyCount(ASTNode node) {
		if (fCopyCounts != null) {
			Integer n= (Integer) fCopyCounts.get(node);
			if (n != null) {
				return n.intValue();
			}
		}
		return 0;
	}
	
	public final GroupDescription getDescription(ASTNode node) {
		if (fGroupDescriptions == null) {
			return null;
		}
		return (GroupDescription) fGroupDescriptions.get(node);
	}
	
	private final void setDescription(ASTNode node, GroupDescription desc) {
		if (desc != null) {
			if (fGroupDescriptions == null) {
				fGroupDescriptions= new HashMap(5);
			}	
			fGroupDescriptions.put(node, desc);
		}
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
	
	public final boolean isInsertBoundToPrevious(ASTNode node) {
		Object info= getChangeProperty(node);
		if (info instanceof ASTInsert) {
			return ((ASTInsert) info).isBoundToPrevious;
		}
		return false;
	}
	
	public final boolean isMoveSource(ASTNode node) {
		if (fMoveSources != null) {
			return fMoveSources.contains(node);
		}
		return false;
	}
	
	private final void setAsMoveSource(ASTNode node) {
		if (fMoveSources == null) {
			fMoveSources= new HashSet();
		}
		fMoveSources.add(node);
	}
	
	private final void incrementCopyCount(ASTNode node) {
		int count= getCopyCount(node);
		if (fCopyCounts == null) {
			fCopyCounts= new HashMap();
		}
		fCopyCounts.put(node, new Integer(count + 1));
	}
	
	public final Object getPlaceholderData(ASTNode node) {
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
	
	public final GroupDescription getTrackedNodeData(ASTNode node) {
		if (fTrackedNodes != null) {
			return (GroupDescription) fTrackedNodes.get(node);
		}
		return null;	
	}
	
	private void setTrackedNodeData(ASTNode node, GroupDescription data) {
		if (fTrackedNodes == null) {
			fTrackedNodes= new HashMap();
		}
		fTrackedNodes.put(node, data);		
	}	
	
	
	private final void setChangeProperty(ASTNode node, Object change) {
		fChangedProperties.put(node, change);
	}
	
	private final Object getChangeProperty(ASTNode node) {
		return fChangedProperties.get(node);
	}
		
	private void assertIsInside(ASTNode node) {
		int endPos= node.getStartPosition() + node.getLength();
		if (fRootNode.getStartPosition() > node.getStartPosition() || fRootNode.getStartPosition() + fRootNode.getLength() < endPos) {
			Assert.isTrue(false, "Node that is changed is not located inside of ASTRewrite root"); //$NON-NLS-1$
		}
	}
		
	private static final class ASTInsert {
		public boolean isBoundToPrevious;
	}
	
	private static final class ASTRemove {
		public boolean isMoveSource;
	}	
		
	private static final class ASTReplace {
		public ASTNode replacingNode;
	}
	
	private static final class ASTModify {
		public ASTNode modifiedNode;
	}
	
	private static class PlaceholderData {
	}
	
	public static final class MovePlaceholderData extends PlaceholderData {
		public ASTNode node;
	}
	
	public static final class CopyPlaceholderData extends PlaceholderData {
		public ASTNode node;
	}	
	
	public static final class StringPlaceholderData extends PlaceholderData {
		public String code;
	}
		
}
