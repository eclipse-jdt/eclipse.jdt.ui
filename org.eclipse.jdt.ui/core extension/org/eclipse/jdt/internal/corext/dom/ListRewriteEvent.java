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
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;

/**
 *
 */
public class ListRewriteEvent extends RewriteEvent {
	
	private List fOriginalNodes;
	private List fListEntries;
	
	public ListRewriteEvent(List originalNodes) {
		fOriginalNodes= originalNodes;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.dom.ASTRewriteChange#getChangeKind()
	 */
	public int getChangeKind() {
		if (fListEntries != null) {
			for (int i= 0; i < fListEntries.size(); i++) {
				RewriteEvent curr= (RewriteEvent) fListEntries.get(i);
				if (curr.getChangeKind() != UNCHANGED) {
					return CHILDREN_CHANGED;
				}
			}
		}
		return UNCHANGED;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.dom.ASTRewriteChange#isListChange()
	 */
	public boolean isListRewrite() {
		return true;
	}	
	
	public RewriteEvent removeEntry(ASTNode originalEntry) {
		return replaceEntry(originalEntry, null);
	}
	
	public RewriteEvent replaceEntry(ASTNode originalEntry, ASTNode newEntry) {
		if (originalEntry == null) {
			throw new IllegalArgumentException();
		}
		
		List listEntries= getEntries();
		int nEntries= listEntries.size();
		for (int i= 0; i < nEntries; i++) {
			NodeRewriteEvent curr= (NodeRewriteEvent) listEntries.get(i);
			if (curr.getOriginalValue() == originalEntry) {
				curr.setNewValue(newEntry);
				return curr;
			}
		}
		throw new IllegalArgumentException();
	}
	
	private List getEntries() {
		if (fListEntries == null) {
			int nNodes= fOriginalNodes.size();
			fListEntries= new ArrayList(nNodes * 2);
			for (int i= 0; i < nNodes; i++) {
				ASTNode node= (ASTNode) fOriginalNodes.get(i);
				fListEntries.add(new NodeRewriteEvent(node, node));
			}
		}
		return fListEntries;
	}

	public RewriteEvent insertEntry(int insertIndex, ASTNode newEntry) {
		int currIndex= 0;
		
		List listEntries= getEntries();
		int nEntries= listEntries.size();
		for (int i= 0; i < nEntries; i++) {
			RewriteEvent curr= (RewriteEvent) listEntries.get(i);
			if (curr.getOriginalValue() != null) {
				if (insertIndex == currIndex) {
					NodeRewriteEvent change= new NodeRewriteEvent(null, newEntry);
					listEntries.add(i, change);
					return change;
				}
				currIndex++;
			}
		}
		if (insertIndex == currIndex) {
			NodeRewriteEvent change= new NodeRewriteEvent(null, newEntry);
			listEntries.add(change);
			return change;
		}
		throw new IndexOutOfBoundsException();
	}
		
	public RewriteEvent[] getChildren() {
		List listEntries= getEntries();
		return (RewriteEvent[]) listEntries.toArray(new RewriteEvent[listEntries.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.dom.RewriteEvent#getOriginalNode()
	 */
	public Object getOriginalValue() {
		return fOriginalNodes;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.dom.RewriteEvent#getNewValue()
	 */
	public Object getNewValue() {
		List listEntries= getEntries();
		ArrayList res= new ArrayList(listEntries.size());
		for (int i= 0; i < listEntries.size(); i++) {
			RewriteEvent curr= (RewriteEvent) listEntries.get(i);
			Object newVal= curr.getNewValue();
			if (newVal != null) {
				res.add(newVal);
			}
		}
		return res;
	}
	
}
