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
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.core.dom.ASTNode;


/**
 * Stores all rewrite events, descriptions of events and knows which nodes
 * are copy or move sources or tracked.
 */
public final class RewriteEventStore {
	
	/**
	 * Interface that allows to override the way how children are accessed from
	 * a parent. Use this interface when the rewriter is set up on an already
	 * modified AST's (as it is the case in the old ASTRewrite infrastructure)
	 */
	public static interface INodePropertyMapper {
		/**
		 * Returns the node attribute for a given property name. 
		 * @param parent The parent node
		 * @param childProperty The child property to access 
		 * @return The child node at the given property location.
		 */
		Object getOriginalValue(ASTNode parent, int childProperty);
	}
	
	/*
	 * Store element to associate event and node position/
	 */
	private static class EventHolder {
		public ASTNode parent;
		public int childProperty;
		public RewriteEvent event;
		
		public EventHolder(ASTNode parent, int childProperty, RewriteEvent change) {
			this.parent= parent;
			this.childProperty= childProperty;
			this.event= change;
		}
		
		public String toString() {
			StringBuffer buf= new StringBuffer();
			buf.append(parent).append(" - "); //$NON-NLS-1$
			buf.append(ASTNodeConstants.getPropertyName(childProperty)).append(": "); //$NON-NLS-1$
			buf.append(event).append('\n');
			return buf.toString();
		}
	}
	
	/*
	 * Store element to remember if a node is the source of a copy or move
	 */
	private static class NodeSourceData {
		boolean isMoveSource= false; // true if this node is moved
		int copyCount= 0; // number of times this node is copied
	}
	
	/**
	 * Iterates over all event parent nodes, tracked nodes and all copy/move sources 
	 */
	private class ParentIterator implements Iterator {
		
		private Iterator fEventIter;
		private Iterator fSourceNodeIter;
		private Iterator fTrackedNodeIter;
		
		public ParentIterator() {
			fEventIter= fEvents.iterator();
			fSourceNodeIter= fNodeSourceDatas.keySet().iterator();
			if (fTrackedNodes != null) {
				fTrackedNodeIter= fTrackedNodes.keySet().iterator();
			} else {
				fTrackedNodeIter= Collections.EMPTY_LIST.iterator();
			}
		}

		/* (non-Javadoc)
		 * @see java.util.Iterator#hasNext()
		 */
		public boolean hasNext() {
			return fEventIter.hasNext() || fSourceNodeIter.hasNext() || fTrackedNodeIter.hasNext();
		}

		/* (non-Javadoc)
		 * @see java.util.Iterator#next()
		 */
		public Object next() {
			if (fEventIter.hasNext()) {
				return ((EventHolder) fEventIter.next()).parent;
			}
			if (fSourceNodeIter.hasNext()) {
				return fSourceNodeIter.next();
			}
			return fTrackedNodeIter.next();
		}

		/* (non-Javadoc)
		 * @see java.util.Iterator#remove()
		 */
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
		
	
	/** all events */
	private final List fEvents;
	
	/** cache for last accessed event */
	private EventHolder fLastEvent;
	
	/** Maps events to group descriptions */
	private Map fEditGroups;
	
	/** Stores which nodes are source of a copy or move */
	private Map fNodeSourceDatas;
	
	/** Stores which nodes are tracked and the corresponding edit group*/
	private Map fTrackedNodes;
	
	/** Stores which inserted nodes bound to the previous node. If not, a node is
	 * always bound to the next node */
	private Set fInsertBoundToPrevious;
	
	/** optional mapper to allow fix already modified AST trees */
	private INodePropertyMapper fNodePropertyMapper;
		
	public RewriteEventStore() {
		fEvents= new ArrayList();
		fLastEvent= null;
		
		fNodeSourceDatas= new IdentityHashMap();
		fEditGroups= null; // lazy initialization
		
		fTrackedNodes= null;
		fInsertBoundToPrevious= null;
		
		fNodePropertyMapper= null;
	}
	
	/**
	 * Override the default way how to access children from a parent node.
	 * @param nodePropertyMapper The new <code>INodePropertyMapper</code> or
	 * <code>null</code>. to use the default.
	 */
	public void setNodePropertyMapper(INodePropertyMapper nodePropertyMapper) {
		fNodePropertyMapper= nodePropertyMapper;
	}
	
	public void clear() {
		fEvents.clear();
		fLastEvent= null;
		fTrackedNodes= null;
		
		fNodeSourceDatas.clear();
		fEditGroups= null; // lazy initialization
		fInsertBoundToPrevious= null;
	}
	
	public void addEvent(ASTNode parent, int childProperty, RewriteEvent event) {
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
				fLastEvent= null;
				return;
			}
		}
		fEvents.add(holder);
	}
	
	public RewriteEvent getEvent(ASTNode parent, int property) {
		validateHasChildProperty(parent, property);
		
		if (fLastEvent != null && fLastEvent.parent == parent && fLastEvent.childProperty == property) {
			return fLastEvent.event;
		}
		
		for (int i= 0; i < fEvents.size(); i++) {
			EventHolder holder= (EventHolder) fEvents.get(i);
			if (holder.parent == parent && holder.childProperty == property) {
				fLastEvent= holder;
				return holder.event;
			}
		}
		return null;
	}
	
	public NodeRewriteEvent getNodeEvent(ASTNode parent, int childProperty, boolean forceCreation) {
		validateIsNodeProperty(childProperty);
		NodeRewriteEvent event= (NodeRewriteEvent) getEvent(parent, childProperty);
		if (event == null && forceCreation) {
			Object originalValue= accessOriginalValue(parent, childProperty);
			event= new NodeRewriteEvent(originalValue, originalValue);
			addEvent(parent, childProperty, event);
		}
		return event;		
	}
	
	public ListRewriteEvent getListEvent(ASTNode parent, int childProperty, boolean forceCreation) {
		validateIsListProperty(childProperty);
		ListRewriteEvent event= (ListRewriteEvent) getEvent(parent, childProperty);
		if (event == null && forceCreation) {
			List originalValue= (List) accessOriginalValue(parent, childProperty);
			event= new ListRewriteEvent(originalValue);
			addEvent(parent, childProperty, event);
		}
		return event;
	}
	
	public Iterator getChangeRootIterator() {
		return new ParentIterator();
	}
	
	
	public boolean hasChangedProperties(ASTNode parent) {
		for (int i= 0; i < fEvents.size(); i++) {
			EventHolder holder= (EventHolder) fEvents.get(i);
			if (holder.parent == parent) {
				if (holder.event.getChangeKind() != RewriteEvent.UNCHANGED) {
					return true;
				}
			}
		}
		return false;
	}
	
	
	public RewriteEvent findEventByOriginal(Object original) {
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
	
	public RewriteEvent findEventByNew(Object original) {
		for (int i= 0; i < fEvents.size(); i++) {
			RewriteEvent event= ((EventHolder) fEvents.get(i)).event;
			if (event.getNewValue() == original) {
				return event;
			}
			if (event.isListRewrite()) {
				RewriteEvent[] children= event.getChildren();
				for (int k= 0; k < children.length; k++) {
					if (children[k].getNewValue() == original) {
						return children[k];
					}
				}
			}
		}
		return null;
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
	
	public int getChangeKind(ASTNode node) {
		RewriteEvent event= findEventByOriginal(node);
		if (event != null) {
			return event.getChangeKind();
		}
		return RewriteEvent.UNCHANGED;
	}
	
	/*
	 * Gets an original child from the AST. The behav
	 * Temporarily overridden to port. All rewriters should prevent AST modification without their control.
	 */
	private Object accessOriginalValue(ASTNode parent, int childProperty) {
		if (fNodePropertyMapper != null) {
			return fNodePropertyMapper.getOriginalValue(parent, childProperty);
		}
		
		return ASTNodeConstants.getNodeChild(parent, childProperty);
	}	
	
	public TextEditGroup getEventEditGroup(RewriteEvent event) {
		if (fEditGroups == null) {
			return null;
		}
		return (TextEditGroup) fEditGroups.get(event);
	}
	
	public void setEventEditGroup(RewriteEvent event, TextEditGroup editGroup) {
		if (editGroup != null) {
			if (fEditGroups == null) {
				fEditGroups= new IdentityHashMap(5);
			}	
			fEditGroups.put(event, editGroup);
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
			fTrackedNodes= new IdentityHashMap();
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
	
	
	public NodeSourceData getNodeSourceData(ASTNode node) {
		return (NodeSourceData) fNodeSourceDatas.get(node);
	}
	
	public NodeSourceData createNodeSourceData(ASTNode node) {
		NodeSourceData data= (NodeSourceData) fNodeSourceDatas.get(node);
		if (data == null) {
			data= new NodeSourceData();
			fNodeSourceDatas.put(node, data);
		}
		return data;
	}

	public boolean isMoveSource(ASTNode node) {
		NodeSourceData data= getNodeSourceData(node);
		if (data != null) {
			return data.isMoveSource;
		}
		return false;
	}

	public void setAsMoveSource(ASTNode node) {
		NodeSourceData data= createNodeSourceData(node);
		data.isMoveSource= true;
	}
	
	public int getCopyCount(ASTNode node) {
		NodeSourceData data= getNodeSourceData(node);
		if (data != null) {
			return data.copyCount;
		}
		return 0;
	}
	
	public void increaseCopyCount(ASTNode node) {
		createNodeSourceData(node).copyCount++;
	}
	
	
	public boolean isInsertBoundToPrevious(ASTNode node) {	
		if (fInsertBoundToPrevious != null) {
			return fInsertBoundToPrevious.contains(node);
		}
		return false;
	}

	public void setInsertBoundToPrevious(ASTNode node) {
		if (fInsertBoundToPrevious == null) {
			fInsertBoundToPrevious= new HashSet();
		}
		fInsertBoundToPrevious.add(node);
	}
	
	private void validateIsListProperty(int property) {
		if (!ASTNodeConstants.isListProperty(property)) {
			String message= ASTNodeConstants.getPropertyName(property) + " is not a list property"; //$NON-NLS-1$
			throw new IllegalArgumentException(message);
		}
	}
	
	private void validateHasChildProperty(ASTNode parent, int property) {
		if (!ASTNodeConstants.hasChildProperty(parent, property)) {
			String message= Signature.getSimpleName(parent.getClass().getName()) + " has no property " + ASTNodeConstants.getPropertyName(property); //$NON-NLS-1$
			throw new IllegalArgumentException(message);
		}
	}
	
	private void validateIsNodeProperty(int property) {
		if (ASTNodeConstants.isListProperty(property)) {
			String message= ASTNodeConstants.getPropertyName(property) + " is not a node property"; //$NON-NLS-1$
			throw new IllegalArgumentException(message);
		}
	}	
	
	public String toString() {
		StringBuffer buf= new StringBuffer();
		for (int i= 0; i < fEvents.size(); i++) {
			buf.append(fEvents.get(i).toString()).append('\n');
		}
		return buf.toString();
	}
	
	public static boolean isNewNode(ASTNode node) {
		return node.getStartPosition() == -1; // should be changed with new API
	}


}
