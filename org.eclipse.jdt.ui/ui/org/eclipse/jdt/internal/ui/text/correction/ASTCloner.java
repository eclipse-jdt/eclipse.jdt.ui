package org.eclipse.jdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;

import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;

/**
  */
public class ASTCloner {
	
	private static class ASTToList extends ASTVisitor {
		
		private Collection fResult;
		
		public ASTToList(Collection res) {
			fResult= res;
		}
		
		public void preVisit(ASTNode curr) {
			fResult.add(curr);
		}
	}	
	
	private ASTNode fClonedRoot;
	private HashMap fMappings;
	
	public ASTCloner(AST ast, ASTNode origAST) {
		ArrayList origNodes= new ArrayList(100);
		origAST.accept(new ASTToList(origNodes));
		
		int nNodes= origNodes.size();
		fClonedRoot= ASTNode.copySubtree(ast, origAST);
		
		ArrayList clonedNodes= new ArrayList(nNodes);
		fClonedRoot.accept(new ASTToList(clonedNodes));
		
		fMappings= new HashMap(nNodes);
		for (int i= 0; i < nNodes; i++) {
			ASTNode orig= (ASTNode) origNodes.get(i);
			ASTNode cloned= (ASTNode) clonedNodes.get(i);
			
			fMappings.put(orig, cloned);
			
			cloned.setSourceRange(orig.getStartPosition(), orig.getLength());
		}
	}
	
	public ASTNode getClonedRoot() {
		return fClonedRoot;
	}
	
	public ASTNode getCloned(ASTNode orig) {
		return (ASTNode) fMappings.get(orig);
	}
}
