/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.dom;

import java.util.Collections;
import java.util.List;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Statement;

/**
 *
 */
public final class ListRewriter {
	
	private ASTNode fParent;
	private int fChildProperty;
	private NewASTRewrite fRewrite;

	/* package*/ ListRewriter(NewASTRewrite rewrite, ASTNode parent, int childProperty) {
		fRewrite= rewrite;
		fParent= parent;
		fChildProperty= childProperty;
	}
	
	private ListRewriteEvent getEvent() {
		return fRewrite.getRewriteEventStore().getListEvent(fParent, fChildProperty, true);
	}
	
	public void remove(ASTNode nodeToRemove, TextEditGroup editGroup) {
		RewriteEvent event= getEvent().removeEntry(nodeToRemove);
		if (editGroup != null) {
			fRewrite.getRewriteEventStore().setEventEditGroup(event, editGroup);
		}
	}
	
	public void replace(ASTNode nodeToReplace, ASTNode replacingNode, TextEditGroup editGroup) {
		RewriteEvent event= getEvent().replaceEntry(nodeToReplace, replacingNode);
		if (editGroup != null) {
			fRewrite.getRewriteEventStore().setEventEditGroup(event, editGroup);
		}
	}
	
	public void insertAfter(ASTNode insert, ASTNode nodeBefore, TextEditGroup editGroup) {
		int index= getEvent().getIndex(nodeBefore, ListRewriteEvent.BOTH);
		if (index == -1) {
			throw new IllegalArgumentException("Node does not exist"); //$NON-NLS-1$
		}
		insertAt(insert, index + 1, editGroup);
	}
	
	public void insertBefore(ASTNode insert, ASTNode nodeAfter, TextEditGroup editGroup) {
		int index= getEvent().getIndex(nodeAfter, ListRewriteEvent.BOTH);
		if (index == -1) {
			throw new IllegalArgumentException("Node does not exist"); //$NON-NLS-1$
		}
		insertAt(insert, index, editGroup);
	}
	
	public void insertFirst(ASTNode insert, TextEditGroup editGroup) {
		insertAt(insert, 0, editGroup);
	}
	
	public void insertLast(ASTNode insert, TextEditGroup editGroup) {
		insertAt(insert, -1, editGroup);
	}
	
	public void insertAt(ASTNode insert, int index, TextEditGroup editGroup) {
		RewriteEvent event= getEvent().insert(insert, index);
		if (isInsertBoundToPreviousByDefault(insert)) {
			fRewrite.getRewriteEventStore().setInsertBoundToPrevious(insert);
		}
		if (editGroup != null) {
			fRewrite.getRewriteEventStore().setEventEditGroup(event, editGroup);
		}
	}
	
	protected boolean isInsertBoundToPreviousByDefault(ASTNode node) {
		return (node instanceof Statement || node instanceof FieldDeclaration);
	}
	
	public List getOriginalList() {
		List list= (List) getEvent().getOriginalValue();
		return Collections.unmodifiableList(list);
	}
	
	public List getRewrittenList() {
		List list= (List) getEvent().getNewValue();
		return Collections.unmodifiableList(list);
	}
	

	
	
}
