package org.eclipse.jdt.internal.corext.refactoring.rename;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.AstNode;
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.ast.NameReference;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.jdt.internal.corext.refactoring.util.AST;
import org.eclipse.jdt.internal.corext.refactoring.util.SelectionAnalyzer;

public class TempDeclarationFinder {
	
	//no instances
	private TempDeclarationFinder(){}
	
	/**
	 * @return <code>null</code> if the selection is invalid of does not cover a temp
	 * declaration or reference.
	 */
	public static LocalDeclaration findTempDeclaration(AST ast, ICompilationUnit cu, int selectionOffset, int selectionLength) throws JavaModelException{
		SelectionAnalyzer analyzer= new TempSelectionAnalyzer(cu, selectionOffset, selectionLength);
		ast.accept(analyzer.getParentTracker());
		
		AstNode[] selected= analyzer.getSelectedNodes();
		if (selected == null || selected.length != 1){
			return null;
		} else if (selected[0] instanceof LocalDeclaration && (!(selected[0] instanceof Argument))){
			return (LocalDeclaration)selected[0];
		} else if (selected[0] instanceof NameReference){
			NameReference reference= (NameReference)selected[0];
			if (reference.binding instanceof LocalVariableBinding){
				LocalVariableBinding localBinding= (LocalVariableBinding)reference.binding;
				if (! localBinding.isArgument){
					return localBinding.declaration;
				}	
			}
		}
		return null;
	} 
	
	/*
	 * Class used to extract selected nodes from an AST.
	 * Subclassing <code>SelectionAnalyzer</code> is needed to support activation 
	 * when only a part of the LocalDeclaration node is selected
	 */
	private static class TempSelectionAnalyzer extends SelectionAnalyzer {

		private AstNode fNode;

		TempSelectionAnalyzer(ICompilationUnit cu, int selectionOffset, int selectionLength) throws JavaModelException {
			super(cu.getBuffer(), selectionOffset, selectionLength);
		}

		//overridden
		public boolean visit(LocalDeclaration node, BlockScope scope) {
			int start= node.declarationSourceStart;
			int end= node.sourceEnd;
			if (fSelection.start >= start && fSelection.end <= end) {
				fNode= node;
				return true;
			}
			return super.visit(node, scope);
		}
		
		//overridden
		public AstNode[] getSelectedNodes() {
			if (fNode != null)
				return new AstNode[] { fNode };
			return super.getSelectedNodes();
		}
	}
}

