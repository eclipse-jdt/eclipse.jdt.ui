package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.refactoring.util.WorkingCopyUtil;

public class ASTNodeMappingManager {

	private final Map fCUsToCuNodes;
	
	public ASTNodeMappingManager() {
		fCUsToCuNodes= new HashMap();
	}

	public CompilationUnit getAST(ICompilationUnit cu){
		ICompilationUnit wc= WorkingCopyUtil.getWorkingCopyIfExists(cu);
		if (fCUsToCuNodes.containsKey(wc)){
			return (CompilationUnit)fCUsToCuNodes.get(wc);
		}	
		CompilationUnit cuNode= AST.parseCompilationUnit(wc, true);
		fCUsToCuNodes.put(wc, cuNode);
		return cuNode;	
	}
	
	public ICompilationUnit getCompilationUnit(ASTNode node) {
		CompilationUnit cuNode= (CompilationUnit)ASTNodes.getParent(node, CompilationUnit.class);
		Assert.isTrue(fCUsToCuNodes.containsValue(cuNode)); //the cu node must've been created before
		
		for (Iterator iter= fCUsToCuNodes.keySet().iterator(); iter.hasNext();) {
			ICompilationUnit cu= (ICompilationUnit) iter.next();
			if (fCUsToCuNodes.get(cu) == cuNode)
				return cu;
		}	
		Assert.isTrue(false); //checked before
		return null;
	}

	public ICompilationUnit[] getAllCompilationUnits(){
		return (ICompilationUnit[]) fCUsToCuNodes.keySet().toArray(new ICompilationUnit[fCUsToCuNodes.keySet().size()]);
	}
}
