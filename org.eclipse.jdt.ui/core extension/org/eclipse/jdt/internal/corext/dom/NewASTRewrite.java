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

import org.eclipse.jdt.core.dom.ASTNode;

import org.eclipse.jdt.internal.corext.textmanipulation.GroupDescription;

/**
 * Work in progress.
 */
public class NewASTRewrite {
	
	private static class EventHolder {
		
		public ASTNode parent;
		public int childProperty;
		public RewriteEvent change;
		
		public EventHolder(ASTNode parent, int childProperty, RewriteEvent change) {
			super();
			this.parent= parent;
			this.childProperty= childProperty;
			this.change= change;
		}
	}
	
	
	private ArrayList fEvents;
	protected HashMap fGroupDescriptions;
	protected HashSet fMoveSources;
	
	public NewASTRewrite() {
		fEvents= new ArrayList();
		fMoveSources= null;
	}
	
	
	public void addEvent(ASTNode parent, int childProperty, RewriteEvent change) {
		if (change.isListRewrite() && !ASTNodeConstants.isListProperty(childProperty)) {
			throw new IllegalArgumentException();
		} else {
			if (!ASTNodeConstants.hasChildProperty(parent, childProperty)) {
				throw new IllegalArgumentException();
			}
		}
		fEvents.add(new EventHolder(parent, childProperty, change));
	}
	
	
	public RewriteEvent getEvent(ASTNode parent, int property) {
		if (!ASTNodeConstants.hasChildProperty(parent, property)) {
			throw new IllegalArgumentException();
		}
		
		// TODO: To optimize
		for (int i= 0; i < fEvents.size(); i++) {
			EventHolder event= (EventHolder) fEvents.get(i);
			if (event.parent == parent && event.childProperty == property) {
				return event.change;
			}
		}
		return null;
	}
	
	public Object getOriginalValue(ASTNode parent, int property) {
		RewriteEvent event= getEvent(parent, property);
		if (event != null) {
			return event.getOriginalValue();
		}
		return ASTNodeConstants.getNodeChild(parent, property);
	}
	
	
	public final GroupDescription getDescription(RewriteEvent event) {
		if (fGroupDescriptions == null) {
			return null;
		}
		return (GroupDescription) fGroupDescriptions.get(event);
	}
	
	public final void setDescription(RewriteEvent event, GroupDescription desc) {
		if (desc != null) {
			if (fGroupDescriptions == null) {
				fGroupDescriptions= new HashMap(5);
			}	
			fGroupDescriptions.put(event, desc);
		}
	}	

	public final boolean isMoveSource(ASTNode node) {	
		if (fMoveSources != null) {
			return fMoveSources.contains(node);
		}
		return false;
	}


	protected final void setAsMoveSource(ASTNode node) {
		if (fMoveSources == null) {
			fMoveSources= new HashSet();
		}
		fMoveSources.add(node);
	}	


}
