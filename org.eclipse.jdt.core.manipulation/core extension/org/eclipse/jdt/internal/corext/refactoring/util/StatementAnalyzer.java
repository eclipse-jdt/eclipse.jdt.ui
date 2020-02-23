/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.SourceRange;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchExpression;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

import org.eclipse.jdt.internal.core.manipulation.JavaManipulationMessages;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.dom.SelectionAnalyzer;
import org.eclipse.jdt.internal.corext.dom.TokenScanner;

/**
 * Analyzer to check if a selection covers a valid set of statements of an abstract syntax
 * tree. The selection is valid iff
 * <ul>
 * 	<li>it does not start or end in the middle of a comment.</li>
 * 	<li>no extract characters except the empty statement ";" is included in the selection.</li>
 * </ul>
 */
public class StatementAnalyzer extends SelectionAnalyzer {

	protected ICompilationUnit fCUnit;
	private TokenScanner fScanner;
	private RefactoringStatus fStatus;

	public StatementAnalyzer(ICompilationUnit cunit, Selection selection, boolean traverseSelectedNode) throws CoreException {
		super(selection, traverseSelectedNode);
		Assert.isNotNull(cunit);
		fCUnit= cunit;
		fStatus= new RefactoringStatus();
		fScanner= new TokenScanner(fCUnit);
	}

	protected void checkSelectedNodes() {
		ASTNode[] nodes= getSelectedNodes();
		if (nodes.length == 0)
			return;

		ASTNode node= nodes[0];
		int selectionOffset= getSelection().getOffset();
		try {
			int start= fScanner.getNextStartOffset(selectionOffset, true);
			if (start == node.getStartPosition()) {
				int lastNodeEnd= ASTNodes.getExclusiveEnd(nodes[nodes.length - 1]);
				int pos= fScanner.getNextStartOffset(lastNodeEnd, true);
				int selectionEnd= getSelection().getInclusiveEnd();
				if (pos <= selectionEnd) {
					IScanner scanner= fScanner.getScanner();
					char[] token= scanner.getCurrentTokenSource(); //see https://bugs.eclipse.org/324237
					if (start < lastNodeEnd && token.length == 1 && (token[0] == ';' || token[0] == ',')) {
						setSelection(Selection.createFromStartEnd(start, lastNodeEnd - 1));
					} else {
						ISourceRange range= new SourceRange(lastNodeEnd, pos - lastNodeEnd);
						invalidSelection(JavaManipulationMessages.StatementAnalyzer_end_of_selection, JavaStatusContext.create(fCUnit, range));
					}
				}
				return; // success
			}
		} catch (CoreException e) {
			// fall through
		}
		ISourceRange range= new SourceRange(selectionOffset, node.getStartPosition() - selectionOffset + 1);
		invalidSelection(JavaManipulationMessages.StatementAnalyzer_beginning_of_selection, JavaStatusContext.create(fCUnit, range));
	}

	public RefactoringStatus getStatus() {
		return fStatus;
	}

	protected ICompilationUnit getCompilationUnit() {
		return fCUnit;
	}

	protected TokenScanner getTokenScanner() {
		return fScanner;
	}

	@Override
	public void endVisit(CompilationUnit node) {
		if (!hasSelectedNodes()) {
			super.endVisit(node);
			return;
		}
		ASTNode selectedNode= getFirstSelectedNode();
		Selection selection= getSelection();
		if (node != selectedNode) {
			ASTNode parent= selectedNode.getParent();
			fStatus.merge(CommentAnalyzer.perform(selection, fScanner.getScanner(), parent.getStartPosition(), parent.getLength()));
		}
		if (!fStatus.hasFatalError())
			checkSelectedNodes();
		super.endVisit(node);
	}

	@Override
	public void endVisit(DoStatement node) {
		ASTNode[] selectedNodes= getSelectedNodes();
		if (doAfterValidation(node, selectedNodes)) {
			if (contains(selectedNodes, node.getBody()) && contains(selectedNodes, node.getExpression())) {
				invalidSelection(JavaManipulationMessages.StatementAnalyzer_do_body_expression);
			}
		}
		super.endVisit(node);
	}

	@Override
	public void endVisit(ForStatement node) {
		ASTNode[] selectedNodes= getSelectedNodes();
		if (doAfterValidation(node, selectedNodes)) {
			boolean containsExpression= contains(selectedNodes, node.getExpression());
			boolean containsUpdaters= contains(selectedNodes, node.updaters());
			if (contains(selectedNodes, node.initializers()) && containsExpression) {
				invalidSelection(JavaManipulationMessages.StatementAnalyzer_for_initializer_expression);
			} else if (containsExpression && containsUpdaters) {
				invalidSelection(JavaManipulationMessages.StatementAnalyzer_for_expression_updater);
			} else if (containsUpdaters && contains(selectedNodes, node.getBody())) {
				invalidSelection(JavaManipulationMessages.StatementAnalyzer_for_updater_body);
			}
		}
		super.endVisit(node);
	}

	@Override
	public void endVisit(SwitchStatement node) {
		ASTNode[] selectedNodes= getSelectedNodes();
		if (doAfterValidation(node, selectedNodes)) {
			List<SwitchCase> cases= getSwitchCases(node);
			for (ASTNode topNode : selectedNodes) {
				if (cases.contains(topNode)) {
					invalidSelection(JavaManipulationMessages.StatementAnalyzer_switch_statement);
					break;
				}
			}
		}
		super.endVisit(node);
	}

	@Override
	public void endVisit(SwitchExpression node) {
		ASTNode[] selectedNodes= getSelectedNodes();
		if (doAfterValidation(node, selectedNodes)) {
			List<SwitchCase> cases= getSwitchCases(node);
			for (ASTNode topNode : selectedNodes) {
				if (cases.contains(topNode)) {
					invalidSelection(JavaManipulationMessages.StatementAnalyzer_switch_expression);
					break;
				}
			}
		}
		super.endVisit(node);
	}

	@Override
	public void endVisit(SynchronizedStatement node) {
		ASTNode firstSelectedNode= getFirstSelectedNode();
		if (getSelection().getEndVisitSelectionMode(node) == Selection.SELECTED) {
			if (firstSelectedNode == node.getBody()) {
				invalidSelection(JavaManipulationMessages.StatementAnalyzer_synchronized_statement);
			}
		}
		super.endVisit(node);
	}

	@Override
	public void endVisit(TryStatement node) {
		ASTNode firstSelectedNode= getFirstSelectedNode();
		if (getSelection().getEndVisitSelectionMode(node) == Selection.AFTER) {
			if (firstSelectedNode == node.getBody() || firstSelectedNode == node.getFinally()) {
				invalidSelection(JavaManipulationMessages.StatementAnalyzer_try_statement);
			} else {
				List<CatchClause> catchClauses= node.catchClauses();
				for (CatchClause element : catchClauses) {
					if (element == firstSelectedNode || element.getBody() == firstSelectedNode) {
						invalidSelection(JavaManipulationMessages.StatementAnalyzer_try_statement);
					} else if (element.getException() == firstSelectedNode) {
						invalidSelection(JavaManipulationMessages.StatementAnalyzer_catch_argument);
					}
				}
			}
		}
		super.endVisit(node);
	}

	@Override
	public void endVisit(WhileStatement node) {
		ASTNode[] selectedNodes= getSelectedNodes();
		if (doAfterValidation(node, selectedNodes)) {
			if (contains(selectedNodes, node.getExpression()) && contains(selectedNodes, node.getBody())) {
				invalidSelection(JavaManipulationMessages.StatementAnalyzer_while_expression_body);
			}
		}
		super.endVisit(node);
	}

	private boolean doAfterValidation(ASTNode node, ASTNode[] selectedNodes) {
		return selectedNodes.length > 0 && node == selectedNodes[0].getParent() && getSelection().getEndVisitSelectionMode(node) == Selection.AFTER;
	}

	protected void invalidSelection(String message) {
		fStatus.addFatalError(message);
		reset();
	}

	protected void invalidSelection(String message, RefactoringStatusContext context) {
		fStatus.addFatalError(message, context);
		reset();
	}

	private static List<SwitchCase> getSwitchCases(ASTNode node) {
		List<SwitchCase> result= new ArrayList<>();
		List<Statement> statements= new ArrayList<>();
		if (node instanceof SwitchStatement) {
			statements= ((SwitchStatement) node).statements();
		} else if (node instanceof SwitchExpression) {
			statements= ((SwitchExpression) node).statements();
		}
		for (Statement statement : statements) {
			Object element= statement;
			if (element instanceof SwitchCase)
				result.add((SwitchCase) element);
		}
		return result;
	}

	protected static boolean contains(ASTNode[] nodes, ASTNode node) {
		for (ASTNode n : nodes) {
			if (n == node) {
				return true;
			}
		}
		return false;
	}

	protected static boolean contains(ASTNode[] nodes, List<Expression> list) {
		for (ASTNode node : nodes) {
			if (list.contains(node)) {
				return true;
			}
		}
		return false;
	}
}
