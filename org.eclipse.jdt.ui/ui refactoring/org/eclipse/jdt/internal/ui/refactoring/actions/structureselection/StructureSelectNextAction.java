package org.eclipse.jdt.internal.ui.refactoring.actions.structureselection;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.compiler.ast.AstNode;
import org.eclipse.jdt.internal.compiler.ast.Statement;
import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.util.ASTUtil;

public class StructureSelectNextAction extends StructureSelectionAction{
	
	public StructureSelectNextAction() {
		super("Structure Select Next"); 
		setText("Next Element@Alt+ARROW_RIGHT");
	}
	
	/* non java doc
	 * @see StructureSelectionAction#internalGetNewSelectionRange(ISourceRange, ICompilationUnit, StructureSelectionAnalyzer)
	 */
	ISourceRange internalGetNewSelectionRange(ISourceRange oldSourceRange, ICompilationUnit cu, StructureSelectionAnalyzer selAnalyzer) throws JavaModelException{
		AstNode[] parents= selAnalyzer.getParents();
		if (parents == null || parents.length == 0)
			return super.internalGetNewSelectionRange(oldSourceRange, cu, selAnalyzer);
			
		AstNode parent= parents[parents.length - 1];
		AstNode lastSelectedNode= selAnalyzer.getSelectedNodes()[selAnalyzer.getSelectedNodes().length - 1];
		AstNode nextNode= getNextNode(parent, lastSelectedNode);
		
		if (nextNode == parent)
			return super.internalGetNewSelectionRange(oldSourceRange, cu, selAnalyzer);

		int offset= oldSourceRange.getOffset();
		int end= Math.min(cu.getSourceRange().getLength(), ASTUtil.getSourceEnd(nextNode));
		return createSourceRange(offset, end);			
	}
	
	private static AstNode getNextNode(AstNode parent, AstNode node){
		if (! containsStatements(parent))
			return parent;
		Statement[] statements= getStatements(parent);
		Assert.isNotNull(statements);
		Assert.isTrue(statements.length > 0);
		if (node == statements[statements.length -1 ])
			return parent;
		else
			return statements[findIndex(statements, node) + 1];
	}
}

