package org.eclipse.jdt.internal.ui.refactoring.actions.structureselection;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.compiler.ast.AstNode;
import org.eclipse.jdt.internal.compiler.ast.Statement;
import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.util.ASTUtil;

public class StructureSelectPreviousAction extends StructureSelectionAction{
	
	public StructureSelectPreviousAction() {
		super("Structure Select Previous"); 
	}
	
	/* non java doc
	 * @see StructureSelectionAction#internalGetNewSelectionRange(ISourceRange, ICompilationUnit, StructureSelectionAnalyzer)
	 */	
	ISourceRange internalGetNewSelectionRange(ISourceRange oldSourceRange, ICompilationUnit cu, StructureSelectionAnalyzer selAnalyzer) throws JavaModelException{
		AstNode[] parents= selAnalyzer.getParents();
		if (parents == null || parents.length == 0)
			return super.internalGetNewSelectionRange(oldSourceRange, cu, selAnalyzer);
			
		AstNode parent= parents[parents.length - 1];
		AstNode nextNode= getNextNode(parent, selAnalyzer.getSelectedNodes()[0]);

		if (nextNode == parent)	
			return super.internalGetNewSelectionRange(oldSourceRange, cu, selAnalyzer);
			
		int offset= ASTUtil.getSourceStart(nextNode)	;
		int end= oldSourceRange.getOffset() + oldSourceRange.getLength() - 1;
		return createSourceRange(offset, end);
	}
	
	private static AstNode getNextNode(AstNode parent, AstNode node){
		if (! containsStatements(parent))
			return parent;
		Statement[] statements= getStatements(parent);
		Assert.isNotNull(statements);
		Assert.isTrue(statements.length > 0);
		if (node == statements[0])
			return parent;
		else
			return statements[findIndex(statements, node) - 1];
	}
}

