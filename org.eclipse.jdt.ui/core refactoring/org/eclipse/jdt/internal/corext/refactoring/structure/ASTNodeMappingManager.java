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
package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;

class ASTNodeMappingManager {

	private final Map fCUsToCuNodes;
	private final Map fCuNodesToCus;
	
	public ASTNodeMappingManager() {
		fCUsToCuNodes= new HashMap();
		fCuNodesToCus= new HashMap();
	}

	public CompilationUnit getAST(ICompilationUnit cu){
		ICompilationUnit wc= WorkingCopyUtil.getWorkingCopyIfExists(cu);
		if (fCUsToCuNodes.containsKey(wc)){
			return (CompilationUnit)fCUsToCuNodes.get(wc);
		}	
		CompilationUnit cuNode= AST.parseCompilationUnit(wc, true);
		fCUsToCuNodes.put(wc, cuNode);
		fCuNodesToCus.put(cuNode, wc);
		return cuNode;	
	}
	
	public ICompilationUnit getCompilationUnit(ASTNode node) {
		CompilationUnit cuNode= getCompilationUnitNode(node);
		Assert.isTrue(fCuNodesToCus.containsKey(cuNode)); //the cu node must've been created before
		return (ICompilationUnit)fCuNodesToCus.get(cuNode);
	}
	
	public static CompilationUnit getCompilationUnitNode(ASTNode node){
		if (node instanceof CompilationUnit)
			return (CompilationUnit)node;
		return (CompilationUnit)ASTNodes.getParent(node, CompilationUnit.class);
	}

	public ICompilationUnit[] getAllCompilationUnits(){
		return (ICompilationUnit[]) fCUsToCuNodes.keySet().toArray(new ICompilationUnit[fCUsToCuNodes.keySet().size()]);
	}
	
	public void clear(){
		fCuNodesToCus.clear();
		fCUsToCuNodes.clear();
	}
}
