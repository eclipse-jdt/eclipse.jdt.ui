/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.surround;

import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AstNode;
import org.eclipse.jdt.internal.compiler.ast.Block;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.DoStatement;
import org.eclipse.jdt.internal.compiler.ast.ForStatement;
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.ast.WhileStatement;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.corext.refactoring.ExtendedBuffer;
import org.eclipse.jdt.internal.corext.refactoring.util.CodeAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.util.Selection;

public class SurroundWithTryCatchAnalyzer extends CodeAnalyzer {

	private TypeBinding[] fExceptions;
	private LocalDeclaration[] fLocals;

	public SurroundWithTryCatchAnalyzer(ExtendedBuffer buffer, Selection selection) {
		super(buffer, selection);
	}
	
	public TypeBinding[] getExceptions() {
		return fExceptions;
	}
	
	public LocalDeclaration[] getAffectedLocals() {
		return fLocals;
	}
	
	public void endVisit(CompilationUnitDeclaration node, CompilationUnitScope scope) {
		superCall: {
			if (fStatus.hasFatalError())
				break superCall;
			if (fSelectedNodes == null || fSelectedNodes.size() == 0) {
				invalidSelection("Cannot surround selection with try/catch block. Selection does not cover a set of statements.");
				break superCall;
			}
			initialize();
			if (fEnclosingMethod == null) {
				invalidSelection("Cannot sorround selection with try/catch block. Selection does not contain statements from a method body."); 
				break superCall;
			}
			fExceptions= ExceptionAnalyzer.perform(fEnclosingMethod, fSelection, fClassScope);
			if (fExceptions == null || fExceptions.length == 0) {
				invalidSelection("No uncaught exceptions are thrown by the selected statement(s).");
				break superCall;
			}
			if (!isValidParent()) {
				invalidSelection("Can only surround statements with a try/catch block.");
			}
			fLocals= LocalDeclarationAnalyzer.perform(fEnclosingMethod, fSelection, fClassScope);
		}
		super.endVisit(node, scope);
	}
	
	private boolean isValidParent() {
		AstNode[] parents= getParents();
		AstNode parent= parents[parents.length - 1];
		if (parent instanceof Block || parent instanceof AbstractMethodDeclaration || parent instanceof ForStatement ||
				parent instanceof DoStatement || parent instanceof WhileStatement)
			return true;
		return false;
	}	
}
