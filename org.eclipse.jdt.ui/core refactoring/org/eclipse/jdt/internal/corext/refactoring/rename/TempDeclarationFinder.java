package org.eclipse.jdt.internal.corext.refactoring.rename;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.dom.SelectionAnalyzer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;

public class TempDeclarationFinder {

	//no instances
	private TempDeclarationFinder(){}
	
	/**
	 * @return <code>null</code> if the selection is invalid of does not cover a temp
	 * declaration or reference.
	 */
	public static VariableDeclaration findTempDeclaration(CompilationUnit cu, int selectionOffset, int selectionLength) {
		TempSelectionAnalyzer analyzer= new TempSelectionAnalyzer(selectionOffset, selectionLength);
		cu.accept(analyzer);
		
		ASTNode[] selected= analyzer.getSelectedNodes();
		if (selected == null || selected.length != 1)
			return null;
			
		ASTNode selectedNode= selected[0];
		if (selectedNode instanceof VariableDeclaration)
			return (VariableDeclaration)selectedNode;
		
		if (selectedNode instanceof Name){
			Name reference= (Name)selectedNode;
			IBinding binding= reference.resolveBinding();
			if (binding == null)
				return null;
			ASTNode declaringNode= cu.findDeclaringNode(binding);
			if (declaringNode instanceof VariableDeclaration)
				return (VariableDeclaration)declaringNode;
			else
				return null;	
		} else if (selectedNode instanceof VariableDeclarationStatement){
			VariableDeclarationStatement vds= (VariableDeclarationStatement)selectedNode;
			if (vds.fragments().size() != 1)
				return null;
			return (VariableDeclaration)vds.fragments().get(0);
		}
		return null;
	} 
	
	/*
	 * Class used to extract selected nodes from an AST.
	 * Subclassing <code>SelectionAnalyzer</code> is needed to support activation 
	 * when only a part of the <code>VariableDeclaration</code> node is selected
	 */
	private static class TempSelectionAnalyzer extends SelectionAnalyzer {

		private ASTNode fNode;

		TempSelectionAnalyzer(int selectionOffset, int selectionLength){
			super(Selection.createFromStartLength(selectionOffset, selectionLength), true);
		}

		//overridden
		public boolean visitNode(ASTNode node) {
			if (!(node instanceof VariableDeclaration))
				return super.visitNode(node);
			VariableDeclaration vd= (VariableDeclaration)node;
			if (vd.getInitializer() != null){
				TextRange declarationRange= TextRange.createFromStartAndExclusiveEnd(node.getStartPosition(), vd.getInitializer().getStartPosition());
				if (getSelection().coveredBy(declarationRange)){
					fNode= node;
					return false;
				} else 
					return super.visitNode(node);
			} else {
				if (getSelection().coveredBy(node)){
					fNode= node;
					return false;
				} else 
					return super.visitNode(node);
			}
		}
		
		//overridden
		public ASTNode[] getSelectedNodes() {
			if (fNode != null)
				return new ASTNode[] { fNode };
			return super.getSelectedNodes();
		}
	}

}
