/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.corext.refactoring.util;

import java.util.HashSet;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.rewrite.TargetSourceRangeComputer;

public class TightSourceRangeComputer extends TargetSourceRangeComputer {
	private HashSet/*<ASTNode>*/ fTightSourceRangeNodes= new HashSet();
	
	public void addTightSourceNode(ASTNode reference) {
		fTightSourceRangeNodes.add(reference);
	}

	public SourceRange computeSourceRange(ASTNode node) {
		if (fTightSourceRangeNodes.contains(node)) {
			return new TargetSourceRangeComputer.SourceRange(node.getStartPosition(), node.getLength());
		} else {
			return super.computeSourceRange(node); // see bug 85850
		}
	}
}