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

import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.jface.text.IDocument;

import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Statement;

import org.eclipse.jdt.internal.corext.textmanipulation.GroupDescription;

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
		
	/** all events */
	private ArrayList fEvents;
	
	/** Maps events to group descriptions */
	protected HashMap fGroupDescriptions;
	
	/** root node for the rewrite: Only nodes under this root are accepted */
	private ASTNode fRootNode;
		
	/** cache for last access */
	private EventHolder fLastEvent;
	
	private HashMap fPlaceholderNodes;
	private HashMap fNodeSourceDatas;
	private HashSet fCollapsedNodes;
	
	private HashSet fInsertBoundToPrevious;
	
	public NewASTRewrite(ASTNode node) {
		fRootNode= node;
		fEvents= new ArrayList();
		fNodeSourceDatas= new HashMap();
		
		fLastEvent= null;
		fPlaceholderNodes= null;
		fInsertBoundToPrevious= null;
	}
	
	/**
	 * Perform rewriting: Analyses the rewrite events and creates text edits that describe changes to the
	 * underlying code. Edits do only change code when the corresponding node has changed. New code
	 * is formatted using the standard code formatter.
	 * @param document Document which describes the code of the AST that is passed in in the
	 * constructor. This document is accessed read-only.
	 * @return Returns the edit describing the text changes.
	 */
	public TextEdit rewriteNode(IDocument document) {
		TextEdit result= new MultiTextEdit();
		ASTRewriteAnalyzer visitor= new ASTRewriteAnalyzer(document, result, this);
		fRootNode.accept(visitor);
		return result;
	}
	
	/**
	 * Clears all events and other internal structures.
	 */
	protected final void clearRewrite() {
		fEvents.clear();
		fNodeSourceDatas.clear();
		fPlaceholderNodes= null;
		fInsertBoundToPrevious= null;
	}
	
	/**
	 * Returns the root node of the rewrite. All modifications must lie inside the root.
	 * @return Returns the root node
	 */
	public ASTNode getRootNode() {
		return fRootNode;
	}
	
	protected void addEvent(ASTNode parent, int childProperty, RewriteEvent event) {
		validateIsInsideRoot(parent);
		validateHasChildProperty(parent, childProperty);
		
		if (event.isListRewrite()) {
			validateIsListProperty(childProperty);
		}
		
		EventHolder holder= new EventHolder(parent, childProperty, event);
		
		// check if already in list
		for (int i= 0; i < fEvents.size(); i++) {
			EventHolder curr= (EventHolder) fEvents.get(i);
			if (curr.parent == parent && curr.childProperty == childProperty) {
				fEvents.set(i, holder);
				return;
			}
		}
		fEvents.add(holder);
	}
	
	
	protected RewriteEvent getEvent(ASTNode parent, int property) {
		validateHasChildProperty(parent, property);
		
		if (fLastEvent != null && fLastEvent.parent == parent && fLastEvent.childProperty == property) {
			return fLastEvent.event;
		}
		
		// TODO: To optimize
		for (int i= 0; i < fEvents.size(); i++) {
			EventHolder holder= (EventHolder) fEvents.get(i);
			if (holder.parent == parent && holder.childProperty == property) {
				fLastEvent= holder;
				return holder.event;
			}
		}
		return null;
	}
		
	protected final NodeRewriteEvent getNodeEvent(ASTNode parent, int childProperty, boolean forceCreation) {
		validateIsNodeProperty(childProperty);
		NodeRewriteEvent event= (NodeRewriteEvent) getEvent(parent, childProperty);
		if (event == null && forceCreation) {
			Object originalValue= accessOriginalValue(parent, childProperty);
			event= new NodeRewriteEvent(originalValue, originalValue);
			addEvent(parent, childProperty, event);
		}
		return event;		
	}
		
	protected final ListRewriteEvent getListEvent(ASTNode parent, int childProperty, boolean forceCreation) {
		validateIsListProperty(childProperty);
		ListRewriteEvent event= (ListRewriteEvent) getEvent(parent, childProperty);
		if (event == null && forceCreation) {
			List originalValue= (List) accessOriginalValue(parent, childProperty);
			event= new ListRewriteEvent(originalValue);
			addEvent(parent, childProperty, event);
		}
		return event;
	}
	
	protected RewriteEvent findEventByOriginal(Object original) {
		for (int i= 0; i < fEvents.size(); i++) {
			RewriteEvent event= ((EventHolder) fEvents.get(i)).event;
			if (event.getOriginalValue() == original) {
				return event;
			}
			if (event.isListRewrite()) {
				RewriteEvent[] children= event.getChildren();
				for (int k= 0; k < children.length; k++) {
					if (children[k].getOriginalValue() == original) {
						return children[k];
					}
				}
			}
		}
		return null;
	}
	
	public boolean isRemoved(ASTNode node) {
		RewriteEvent event= findEventByOriginal(node);
		return event != null && event.getChangeKind() == RewriteEvent.REMOVED;
	}	
	
	public boolean isReplaced(ASTNode node) {
		RewriteEvent event= findEventByOriginal(node);
		return event != null && event.getChangeKind() == RewriteEvent.REPLACED;
	}
	
	
	/*
	 * Gets an original child from the AST. Override this method if the AST can be modified when accessing that.
	 * Temporarily overridden to port. All rewriters should prevent AST modification without their control.
	 */
	protected Object accessOriginalValue(ASTNode parent, int childProperty) {
		return ASTNodeConstants.getNodeChild(parent, childProperty);
	}
	
	
	protected final void validateIsNodeProperty(int property) {
		if (ASTNodeConstants.isListProperty(property)) {
			String message= ASTNodeConstants.getPropertyName(property) + " is not a node property"; //$NON-NLS-1$
			throw new IllegalArgumentException(message);
		}
	}
	
	protected final void validateIsListProperty(int property) {
		if (!ASTNodeConstants.isListProperty(property)) {
			String message= ASTNodeConstants.getPropertyName(property) + " is not a list property"; //$NON-NLS-1$
			throw new IllegalArgumentException(message);
		}
	}
	
	protected final void validateHasChildProperty(ASTNode parent, int property) {
		if (!ASTNodeConstants.hasChildProperty(parent, property)) {
			String message= Signature.getSimpleName(parent.getClass().getName()) + " has no property " + ASTNodeConstants.getPropertyName(property); //$NON-NLS-1$
			throw new IllegalArgumentException(message);
		}
	}
	
	protected final void validateIsInsideRoot(ASTNode node) {
		if (node.getStartPosition() == -1) {
			throw new IllegalArgumentException("Node is not an existing node"); //$NON-NLS-1$
		}
	
		int endPos= node.getStartPosition() + node.getLength();
		ASTNode rootNode= getRootNode();
		if (rootNode.getStartPosition() > node.getStartPosition() || rootNode.getStartPosition() + rootNode.getLength() < endPos) {
			throw new IllegalArgumentException("Node is not inside of ASTRewrite root"); //$NON-NLS-1$
		}
	}
		
		
	public Object getOriginalValue(ASTNode parent, int property) {
		RewriteEvent event= getEvent(parent, property);
		if (event != null) {
			return event.getOriginalValue();
		}
		return accessOriginalValue(parent, property);
	}
	
	public Object getNewValue(ASTNode parent, int property) {
		RewriteEvent event= getEvent(parent, property);
		if (event != null) {
			return event.getNewValue();
		}
		return accessOriginalValue(parent, property);
	}
	
	public boolean hasChildrenChanges(ASTNode parent) {
		for (int i= 0; i < fEvents.size(); i++) {
			EventHolder holder= (EventHolder) fEvents.get(i);
			if (holder.parent == parent) {
				return true;
			}
		}
		return false;
	}
	
	
	public final GroupDescription getDescription(RewriteEvent event) {
		if (fGroupDescriptions == null) {
			return null;
		}
		return (GroupDescription) fGroupDescriptions.get(event);
	}
	
	protected final void setDescription(RewriteEvent event, GroupDescription desc) {
		if (desc != null) {
			if (fGroupDescriptions == null) {
				fGroupDescriptions= new HashMap(5);
			}	
			fGroupDescriptions.put(event, desc);
		}
	}
	
	public final NodeSourceData getNodeSourceData(ASTNode node) {
		return (NodeSourceData) fNodeSourceDatas.get(node);
	}
	
	private final NodeSourceData createNodeSourceData(ASTNode node) {
		NodeSourceData data= (NodeSourceData) fNodeSourceDatas.get(node);
		if (data == null) {
			data= new NodeSourceData();
			fNodeSourceDatas.put(node, data);
		}
		return data;
	}

	public final boolean isMoveSource(ASTNode node) {
		NodeSourceData data= getNodeSourceData(node);
		if (data != null) {
			return data.isMoveSource;
		}
		return false;
	}

	protected final void setAsMoveSource(ASTNode node) {
		NodeSourceData data= createNodeSourceData(node);
		data.isMoveSource= true;
	}
	
	public final int getCopyCount(ASTNode node) {
		NodeSourceData data= getNodeSourceData(node);
		if (data != null) {
			return data.copyCount;
		}
		return 0;
	}
	
	
	public final GroupDescription getTrackedNodeData(ASTNode node) {
		NodeSourceData data= getNodeSourceData(node);
		if (data != null) {
			return data.trackData;
		}
		return null;	
	}
	
	protected void setTrackedNodeData(ASTNode node, GroupDescription description) {
		NodeSourceData data= createNodeSourceData(node);
		data.trackData= description;
	}
	
	public final boolean isInsertBoundToPrevious(ASTNode node) {	
		if (fInsertBoundToPrevious != null) {
			return fInsertBoundToPrevious.contains(node);
		}
		return false;
	}

	protected final void setInsertBoundToPrevious(ASTNode node) {
		if (fInsertBoundToPrevious == null) {
			fInsertBoundToPrevious= new HashSet();
		}
		fInsertBoundToPrevious.add(node);
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
	public final ASTNode createPlaceholder(String code, int nodeType) {
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
		validateIsInsideRoot(node);
		
		createNodeSourceData(node).copyCount++;
		
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
		validateIsInsideRoot(node);

		int placeHolderType= getPlaceholderType(node);
		if (placeHolderType == UNKNOWN) {
			throw new IllegalArgumentException("Move placeholders are not supported for nodes of type " + node.getClass().getName()); //$NON-NLS-1$
		}
		
		createNodeSourceData(node).isMoveSource= true;
		
		MovePlaceholderData data= new MovePlaceholderData();
		data.node= node;
		return createPlaceholder(data, placeHolderType);
	}	
	
	protected final Block createCollapsePlaceholder() {
		Block placeHolder= getRootNode().getAST().newBlock();
		if (fCollapsedNodes == null) {
			fCollapsedNodes= new HashSet();
		}
		fCollapsedNodes.add(placeHolder);
		return placeHolder;
	}
		
	private final ASTNode createPlaceholder(PlaceholderData data, int nodeType) {
		AST ast= getRootNode().getAST();
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
	
	public final boolean isCollapsed(ASTNode node) {
		if (fCollapsedNodes != null) {
			return fCollapsedNodes.contains(node);
		}
		return false;	
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
	
	private static class EventHolder {
		public ASTNode parent;
		public int childProperty;
		public RewriteEvent event;
		
		public EventHolder(ASTNode parent, int childProperty, RewriteEvent change) {
			this.parent= parent;
			this.childProperty= childProperty;
			this.event= change;
		}
	}
	
	public static class NodeSourceData {
		boolean isMoveSource= false;
		int copyCount= 0;
		GroupDescription trackData= null;
	}
	
	
	private static class PlaceholderData {
	}
		
	public static final class MovePlaceholderData extends PlaceholderData {
		public ASTNode node;
		public String toString() {
			return "[placeholder move: " + node +"]"; //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	public static final class CopyPlaceholderData extends PlaceholderData {
		public ASTNode node;
		public String toString() {
			return "[placeholder copy: " + node +"]";  //$NON-NLS-1$//$NON-NLS-2$
		}
	}	
	
	public static final class StringPlaceholderData extends PlaceholderData {
		public String code;
		public String toString() {
			return "[placeholder string: " + code +"]"; //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	public String toString() {
		StringBuffer buf= new StringBuffer();
		buf.append("Events:\n"); //$NON-NLS-1$
		for (int i= 0; i < fEvents.size(); i++) {
			EventHolder holder= (EventHolder) fEvents.get(i);
			buf.append(holder.parent).append(" - "); //$NON-NLS-1$
			buf.append(ASTNodeConstants.getPropertyName(holder.childProperty)).append(": "); //$NON-NLS-1$
			buf.append(holder.event).append('\n');
		}
		return buf.toString();
	}
	

}
