/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.dom;

import org.eclipse.jdt.core.dom.ASTNode;

import org.eclipse.jdt.internal.corext.Assert;

/**
 * A visitor that maps a selection to a given ASTNode. The result node is
 * determined as follows:
 * <ul>
 *   <li>first the visitor tries to find a node with the exact start and length</li>
 * 	 <li>if no such node exists than the node that encloses the range defined by
 *       start and end is returned.</li>
 *   <li>if the length is zero than also nodes are considered where the node's
 *       start or end position matches <code>fStart</code>.</li>
 *   <li>otherwise <code>null</code> is returned.</li>
 * </ul>
 * 
 * @since		2.1
 */
public class NodeFinder extends GenericVisitor {

	private int fStart;
	private int fLength;
	private int fEnd;
	private ASTNode fResult;
	private ASTNode fCandidate;

	private NodeFinder(int start, int length) {
		super();
		fStart= start;
		fLength= length;
		fEnd= fStart + fLength;
	}
	
	public static ASTNode perform(ASTNode root, int start, int length) {
		NodeFinder finder= new NodeFinder(start, length);
		root.accept(finder);
		ASTNode result= finder.fResult;
		if (result == null)
			result= finder.fCandidate;
		return result;
	}
	
	protected boolean visitNode(ASTNode node) {
		int nodeStart= node.getStartPosition();
		int nodeLength= node.getLength();
		int nodeEnd= nodeStart + nodeLength;
		if (nodeEnd < fStart || fEnd < nodeStart) {
			return false;
		} else if (fStart < nodeStart && nodeEnd < fEnd) {
			return false;
		} else if (nodeStart == fStart && fEnd == nodeEnd) {
			fResult= node;
			return false;
		} else if (nodeStart <= fStart && fEnd <= nodeEnd) {
			fResult= node;
			// traverse down because there could be a more preceise match.
			return true;
		} else if (nodeEnd == fStart && fLength == 0) {
			fCandidate= node;
		} 
		Assert.isTrue((nodeStart <= fStart && nodeEnd < fEnd) || (nodeStart > fStart && nodeEnd >= fEnd));
		return false;
	}
}
