package org.eclipse.jdt.internal.corext.refactoring.code;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AstNode;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.corext.refactoring.util.AST;
import org.eclipse.jdt.internal.corext.refactoring.util.GenericVisitor;

class AstMatchingNodeFinder extends GenericVisitor{
	
	private List fMatchingNodeList;
	private AstNode[] fFlattenedNode;
	
	private AstMatchingNodeFinder(AstNode node){
		fFlattenedNode= AstFlattener.flattenNode(node);
		fMatchingNodeList= new ArrayList();
	}
	
	public static AstNode[] findMatchingNodes(AbstractMethodDeclaration methodDeclaration, AstNode node){
		AstMatchingNodeFinder instance= new AstMatchingNodeFinder(node);
		methodDeclaration.traverse(instance, (ClassScope)null);
		return (AstNode[]) instance.fMatchingNodeList.toArray(new AstNode[instance.fMatchingNodeList.size()]);
	}

	public static AstNode[] findMatchingNodes(AST ast, AstNode node){
		AstMatchingNodeFinder instance= new AstMatchingNodeFinder(node);
		ast.accept(instance);
		return (AstNode[]) instance.fMatchingNodeList.toArray(new AstNode[instance.fMatchingNodeList.size()]);
	}
	
	//--- visit methods
	
	protected boolean visitRange(int start, int end, AstNode node, Scope scope) {
		boolean matches= AstNodeMatcher.nodeMatches(node, fFlattenedNode);
		if (matches){
			fMatchingNodeList.add(node);
			return false; //not possible to match a node and its subnode
		}
			
		return true;
	}

	private static  class AstFlattener extends GenericVisitor{
	
		private List fFlattenedNodeList;
		
		private AstFlattener(){
			fFlattenedNodeList= new ArrayList();
		}
		
		public static AstNode[] flattenNode(AstNode node){
			AstFlattener instance= new AstFlattener();
			node.traverse(instance, null);
			return (AstNode[]) instance.fFlattenedNodeList.toArray(new AstNode[instance.fFlattenedNodeList.size()]);
		}
		
		//--- visit methods
		
		protected boolean visitRange(int start, int end, AstNode node, Scope scope) {
			fFlattenedNodeList.add(node);
			return true;
		}
	}

	
}
