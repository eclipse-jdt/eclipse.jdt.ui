/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.code;

import java.util.List;

import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.AstNode;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.ast.ThrowStatement;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.corext.refactoring.util.AbstractExceptionAnalyzer;

/* package */ class ExceptionAnalyzer extends AbstractExceptionAnalyzer {

	private AbstractMethodDeclaration fEnclosingMethod;
	
	private ExceptionAnalyzer(AbstractMethodDeclaration enclosingMethod) {
		fEnclosingMethod= enclosingMethod;
	}
	
	public static TypeBinding[] perform(AbstractMethodDeclaration enclosingMethod, AstNode[] statements, BlockScope scope) {
		ExceptionAnalyzer analyzer= new ExceptionAnalyzer(enclosingMethod);
		for (int i= 0; i < statements.length; i++) {
			AstNode node= statements[i];
			if (scope instanceof MethodScope)
				node.traverse(analyzer, (MethodScope)scope);
			else
				node.traverse(analyzer, scope);
		}
		List exceptions= analyzer.getCurrentExceptions();
		return (TypeBinding[]) exceptions.toArray(new TypeBinding[exceptions.size()]);
	}

	public boolean visit(ThrowStatement node, BlockScope scope) {
		TypeBinding exception= node.exceptionType;
		if (exception == null)		// Safety net for null bindings when compiling fails.
			return false;
		
		if (isRuntimeException(exception, scope) && !methodThrowsException(exception))
			return false;
			
		addException(exception);
		return true;
	}
	
	public boolean visit(MessageSend statement, BlockScope scope) {
		return handleExceptions(statement.binding);
	}
	
	public boolean visit(AllocationExpression node, BlockScope scope) {
		return handleExceptions(node.binding);
	}
	
	private boolean methodThrowsException(TypeBinding exception) {
		ReferenceBinding[] methodExceptions = fEnclosingMethod.binding.thrownExceptions;
		for (int i= 0; i < methodExceptions.length; i++) {
			if (exception == methodExceptions[i])
				return true;
		}
		return false;
	}
}