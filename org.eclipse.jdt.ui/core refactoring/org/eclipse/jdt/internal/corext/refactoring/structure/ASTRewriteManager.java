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
import java.util.Iterator;
import java.util.Map;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;

class ASTRewriteManager {
	private final Map fRewrites; //CompilationUnit -> ASTRewrite
	private final ASTNodeMappingManager fNodeMapper;
	
	ASTRewriteManager(ASTNodeMappingManager nodeMapper){
		Assert.isNotNull(nodeMapper);
		fRewrites= new HashMap(2);
		fNodeMapper= nodeMapper;
	}
	
	ASTRewrite getRewrite(ICompilationUnit cu){
		return getRewrite(fNodeMapper.getAST(WorkingCopyUtil.getWorkingCopyIfExists(cu)));
	}

	ASTRewrite getRewrite(CompilationUnit cuNode){
		if (fRewrites.containsKey(cuNode))
			return (ASTRewrite)fRewrites.get(cuNode);

		ASTRewrite cuRewrite= new ASTRewrite(cuNode);
		fRewrites.put(cuNode, cuRewrite);
		return cuRewrite;	
	}
	
	CompilationUnit[] getAllCompilationUnitNodes(){
		return (CompilationUnit[]) fRewrites.keySet().toArray(new CompilationUnit[fRewrites.keySet().size()]);
	}
	
	public void clear(){
		removeRewriteModifications();
		fRewrites.clear();
		//do not clear the mapper
	}
	
	private void removeRewriteModifications(){
		for (Iterator iter= fRewrites.values().iterator(); iter.hasNext();) {
			ASTRewrite rewrite= (ASTRewrite) iter.next();
			rewrite.removeModifications();
		}
	}
}
