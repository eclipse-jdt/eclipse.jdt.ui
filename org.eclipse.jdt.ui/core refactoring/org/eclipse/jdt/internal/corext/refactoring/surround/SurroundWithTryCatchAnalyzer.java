/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.surround;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.WhileStatement;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaSourceContext;
import org.eclipse.jdt.internal.corext.refactoring.util.CodeAnalyzer;

public class SurroundWithTryCatchAnalyzer extends CodeAnalyzer {

	private ITypeBinding[] fExceptions;
	private VariableDeclaration[] fLocals;

	public SurroundWithTryCatchAnalyzer(ICompilationUnit unit, Selection selection) throws JavaModelException {
		super(unit, selection, false);
	}
	
	public ITypeBinding[] getExceptions() {
		return fExceptions;
	}
	
	public VariableDeclaration[] getAffectedLocals() {
		return fLocals;
	}
	
	public void endVisit(CompilationUnit node) {
		superCall: {
			if (getStatus().hasFatalError())
				break superCall;
			if (!hasSelectedNodes()) {
				invalidSelection("Selection does not cover a set of statements.");
				break superCall;
			}
			MethodDeclaration enclosingMethod= (MethodDeclaration)ASTNodes.getParent(getFirstSelectedNode(), MethodDeclaration.class);
			if (enclosingMethod == null) {
				invalidSelection("Selection does not contain statements from a method body."); 
				break superCall;
			}
			fExceptions= ExceptionAnalyzer.perform(enclosingMethod, getSelection());
			if (fExceptions == null || fExceptions.length == 0) {
				invalidSelection("No uncaught exceptions are thrown by the selected code.");
				break superCall;
			}
			if (!onlyStatements()) {
				invalidSelection("Can only surround statements with a try/catch block.");
			}
			fLocals= LocalDeclarationAnalyzer.perform(enclosingMethod, getSelection());
		}
		super.endVisit(node);
	}
	
	public void endVisit(SuperConstructorInvocation node) {
		if (getSelection().getEndVisitSelectionMode(node) == Selection.SELECTED) {
			invalidSelection("Cannot surround a super constructor call.", JavaSourceContext.create(fCUnit, node));
		}
		super.endVisit(node);
	}
	
	private boolean onlyStatements() {
		ASTNode[] nodes= getSelectedNodes();
		for (int i= 0; i < nodes.length; i++) {
			if (!(nodes[i] instanceof Statement))
				return false;
		}
		return true;
	}	
}
