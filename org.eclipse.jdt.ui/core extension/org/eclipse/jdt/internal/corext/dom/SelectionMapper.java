/*
 * (c) Copyright 2002 IBM Corporation.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.dom;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.*;

import org.eclipse.jdt.internal.corext.Assert;

/**
 * Maps a selection to a set of AST nodes.
 */
public class SelectionMapper extends GenericVisitor {
	
	protected Selection fSelection;
	protected boolean fTraverseSelectedNode;
	
	// Selected nodes
	protected List fSelectedNodes;
	
	public SelectionMapper(Selection selection, boolean traverseSelectedNode) {
		Assert.isNotNull(selection);
		fSelection= selection;
		fTraverseSelectedNode= traverseSelectedNode;
	}
	
	public ASTNode[] getSelectedNodes() {
		if (fSelectedNodes == null || fSelectedNodes.isEmpty())
			return new ASTNode[0];
		return (ASTNode[]) fSelectedNodes.toArray(new ASTNode[fSelectedNodes.size()]);
	}
	
	public ASTNode getFirstSelectedNode() {
		if (fSelectedNodes == null || fSelectedNodes.isEmpty())
			return null;
		return (ASTNode)fSelectedNodes.get(0);
	}
	
	//--- node management ---------------------------------------------------------
	
	protected boolean visitNode(ASTNode node) {
		// The selection lies behind the node.
		if (fSelection.liesOutside(node)) {
			return false;
		} else if (fSelection.covers(node)) {
			if (isFirstNode()) {
				handleFirstSelectedNode(node);
			} else {
				handleNextSelectedNode(node);
			}
			return fTraverseSelectedNode;
		} else if (fSelection.coveredBy(node)) {
			return true;
		} else if (fSelection.endsIn(node)) {
			handleSelectionEndsIn(node);
			return false;
		}
		// There is a possibility that the user has selected trailing semicolons that don't belong
		// to the statement. So dive into it to check if sub nodes are fully covered.
		return true;
	}
	
	protected void reset() {
		fSelectedNodes= null;
	}
	
	protected void handleFirstSelectedNode(ASTNode node) {
		fSelectedNodes= new ArrayList(5);
		fSelectedNodes.add(node);
	}
	
	protected void handleNextSelectedNode(ASTNode node) {
		if (getFirstSelectedNode().getParent() == node.getParent()) {
			fSelectedNodes.add(node);
		}
	}

	protected void handleSelectionEndsIn(ASTNode node) {
	}
	
	private boolean isFirstNode() {
		return fSelectedNodes == null;
	}	
}