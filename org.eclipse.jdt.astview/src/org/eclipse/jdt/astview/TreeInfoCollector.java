/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.astview;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;

/**
 *
 */
public class TreeInfoCollector {

	public static class NodeCounter extends ASTVisitor {

		public int numberOfNodes= 0;

		@Override
		public void preVisit(ASTNode node) {
			numberOfNodes++;
		}
	}


	private final CompilationUnit fRoot;

	public TreeInfoCollector(CompilationUnit root) {
		fRoot= root;
	}

	public int getSize() {
		return fRoot.subtreeBytes();
	}

	public int getNumberOfNodes() {
		NodeCounter counter= new NodeCounter();
		fRoot.accept(counter);
		return counter.numberOfNodes;
	}


}
