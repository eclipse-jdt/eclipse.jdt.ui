/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.typeconstraints;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.dom.SelectionAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeMappingManager;

public final class CompilationUnitRange {
	
	private final ICompilationUnit fCompilationUnit;
	private final ISourceRange fSourceRange;

	public CompilationUnitRange(ICompilationUnit unit, ISourceRange range) {
		Assert.isNotNull(unit);
		Assert.isNotNull(range);
		fCompilationUnit= unit;
		fSourceRange= range;
	}
	
	public CompilationUnitRange(ICompilationUnit unit, ASTNode node) {
		this(unit, new SourceRange(node));
	}
	
	public ICompilationUnit getCompilationUnit() {
		return fCompilationUnit;
	}

	public ISourceRange getSourceRange() {
		return fSourceRange;
	}
	
	//rootNode must be the ast root for fCompilationUnit
	public ASTNode getNode(CompilationUnit rootNode){
		Selection selection= Selection.createFromStartLength(fSourceRange.getOffset(), fSourceRange.getLength());
		SelectionAnalyzer analyzer= new SelectionAnalyzer(selection, true);
		rootNode.accept(analyzer);
		if (analyzer.getFirstSelectedNode() != null)
			return analyzer.getFirstSelectedNode();
		return analyzer.getLastCoveringNode();
	}
	
	public ASTNode getNode(ASTNodeMappingManager astManager){
		return getNode(astManager.getAST(fCompilationUnit));
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "(" + fSourceRange.toString() + " in " + fCompilationUnit.getElementName() + ")";
	}

}
