package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.refactoring.util.WorkingCopyUtil;

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
		CompilationUnit cuNode= (CompilationUnit)ASTNodes.getParent(node, CompilationUnit.class);
		Assert.isTrue(fCuNodesToCus.containsKey(cuNode)); //the cu node must've been created before
		return (ICompilationUnit)fCuNodesToCus.get(cuNode);
	}

	public ICompilationUnit[] getAllCompilationUnits(){
		return (ICompilationUnit[]) fCUsToCuNodes.keySet().toArray(new ICompilationUnit[fCUsToCuNodes.keySet().size()]);
	}
	
	public void clear(){
		fCuNodesToCus.clear();
		fCUsToCuNodes.clear();
	}
}
