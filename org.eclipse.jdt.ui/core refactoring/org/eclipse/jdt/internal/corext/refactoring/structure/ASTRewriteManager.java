package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.util.HashMap;
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
		fRewrites.clear();
		//do not clear the mapper
	}
}
