/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.util;

import org.eclipse.jdt.internal.compiler.ast.AstNode;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.ast.SwitchStatement;
import org.eclipse.jdt.internal.compiler.ast.SynchronizedStatement;
import org.eclipse.jdt.internal.compiler.ast.TryStatement;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.jdt.internal.corext.refactoring.ExtendedBuffer;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.code.CommentAnalyzer;

/**
 * Analyzer to check if a selection covers a valid set of nodes of an abstract syntax
 * tree. The selection is valid iff
 * <ul>
 * 	<li>it does not start or end in the middle of a comment.</li>
 * 	<li>it fully covers a set of nodes or a single node.</li>
 * 	<li>no extract characters except the empty statement ";" is included in the selection.</li>
 * </ul>
 * Examples of valid selections. The selection is denoted by []:
 * <ul>
 * 	<li><code>[foo()]</code></li>
 * 	<li><code>[foo();]</code></li>
 * 	<li><code>if ([i == 10])</code></li>
 * </ul>
 */
public class NewStatementAnalyzer extends NewSelectionAnalyzer{

	public NewStatementAnalyzer(ExtendedBuffer buffer, Selection selection) {
		super(buffer, selection);
	}
	
	protected void checkSelectedNodes() {
		// We don't have a selected node;
		int pos= fBuffer.indexOfStatementCharacter(fSelection.start);
		AstNode node= getFirstSelectedNode();
		if (ASTUtil.getSourceStart(node) != pos) {
			invalidSelection("Selection starts in the middle of a statement.");
			return;
		}	
		
		for (int i= 0; i < fSelectedNodes.size() - 1; i++) {
			AstNode first= (AstNode)fSelectedNodes.get(i);
			AstNode second= (AstNode)fSelectedNodes.get(i + 1);
			if (isMultiLocalDeclaration(first, second))
				continue;
			pos= fBuffer.indexOfStatementCharacter(ASTUtil.getSourceEnd(first) + 1);
			if (pos != ASTUtil.getSourceStart(second)) {
				invalidSelection("Selected statements do not belong to the same category. For example, a while statement's expression and action are selected.");
				return;
			}
		}		
		
		node= getLastSelectedNode();	
		pos= fBuffer.indexOfStatementCharacter(ASTUtil.getSourceEnd(node) + 1);
		if (pos != -1 && pos <= fSelection.end)
			invalidSelection("End of selection contains characters that do not belong to a statement.");
	}

	private boolean isMultiLocalDeclaration(AstNode first, AstNode second) {
		// Not working: see http://dev.eclipse.org/bugs/show_bug.cgi?id=7106
		return first instanceof LocalDeclaration && second instanceof LocalDeclaration && 
			((LocalDeclaration)first).declarationSourceStart == ((LocalDeclaration)second).declarationSourceStart;
	}
			
	public void endVisit(CompilationUnitDeclaration node, CompilationUnitScope scope) {
		AstNode[] parents= getParents();
		if (parents != null && parents.length > 0) {
			AstNode parent= parents[parents.length - 1];
			fStatus.merge(CommentAnalyzer.perform(fSelection, fBuffer.getCharacters(), 
				ASTUtil.getSourceStart(parent), ASTUtil.getSourceEnd(parent)));
		}
		if (!fStatus.hasFatalError() && hasSelectedNodes())
			checkSelectedNodes();
		super.endVisit(node, scope);
	}
	
	public void endVisit(SwitchStatement node, BlockScope scope) {
		AstNode[] selectedNodes= getSelectedNodes();
		if (getSelection().getEndVisitSelectionMode(node) == Selection.AFTER && selectedNodes != null) {
			for (int i= 0; i < selectedNodes.length; i++) {
				AstNode topNode= selectedNodes[i];
				if (topNode == node.defaultCase || contains(node.cases, topNode)) {
					invalidSelection(RefactoringCoreMessages.getString("StatementAnalyzer.switch_statement")); //$NON-NLS-1$
					break;
				}
			}
		}
		super.endVisit(node, scope);
	}

	public void endVisit(SynchronizedStatement node, BlockScope scope) {
		AstNode firstSelectedNode= getFirstSelectedNode();
		if (getSelection().getEndVisitSelectionMode(node) == Selection.SELECTED) {
			if (firstSelectedNode == node.block) {
				invalidSelection(RefactoringCoreMessages.getString("StatementAnalyzer.synchronized_statement")); //$NON-NLS-1$
			}
		}
		super.endVisit(node, scope);
	}

	public void endVisit(TryStatement node, BlockScope scope) {
		AstNode firstSelectedNode= getFirstSelectedNode();
		if (getSelection().getEndVisitSelectionMode(node) == Selection.AFTER) {
			if (firstSelectedNode == node.tryBlock || firstSelectedNode == node.finallyBlock ||
					contains(node.catchBlocks, firstSelectedNode))
				invalidSelection(RefactoringCoreMessages.getString("StatementAnalyzer.try_statement"));
		}
		super.endVisit(node, scope);
	}
}
