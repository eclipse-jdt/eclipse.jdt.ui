/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.code;

import java.util.ArrayList;import java.util.HashMap;import java.util.Iterator;import java.util.List;import java.util.Stack;import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;import org.eclipse.jdt.internal.compiler.ast.AllocationExpression;import org.eclipse.jdt.internal.compiler.ast.Argument;import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;import org.eclipse.jdt.internal.compiler.ast.NameReference;
import org.eclipse.jdt.internal.compiler.ast.ThrowStatement;import org.eclipse.jdt.internal.compiler.ast.TryStatement;import org.eclipse.jdt.internal.compiler.ast.TypeReference;import org.eclipse.jdt.internal.compiler.lookup.BlockScope;import org.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;import org.eclipse.jdt.internal.compiler.lookup.Scope;import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
/* package */ class ExceptionAnalyzer {

	private List fCurrentExceptions;	// Elements in this list are of type TypeBinding
	private Stack fTryStack;
	private HashMap fTypeNames;
	private StatementAnalyzer fStatementAnalyzer;
	
	public ExceptionAnalyzer(StatementAnalyzer statementAnalyzer) {
		fTryStack= new Stack();
		fCurrentExceptions= new ArrayList(1);
		fTryStack.push(fCurrentExceptions);
		fTypeNames= new HashMap(10);
		fStatementAnalyzer= statementAnalyzer;
	}
	public String getThrowSignature() {
		if (fCurrentExceptions.size() == 0)
			return ""; //$NON-NLS-1$
			
		StringBuffer result= new StringBuffer(" throws "); //$NON-NLS-1$
		int i= 0;
		for (Iterator iter= fCurrentExceptions.iterator(); iter.hasNext(); i++) {
			TypeBinding typeBinding= (TypeBinding)iter.next();
			if (i > 0)
				result.append(", "); //$NON-NLS-1$
			TypeReference reference= (TypeReference)fTypeNames.get(typeBinding);
			if (reference == null) {
				result.append(typeBinding.qualifiedPackageName());
				result.append("."); //$NON-NLS-1$
				result.append(typeBinding.qualifiedSourceName());
			} else {
				result.append(reference.toStringExpression(0));
			}
		}
		return result.toString();
	}
	
	public void visitAbstractMethodDeclaration(AbstractMethodDeclaration declaration, Scope scope) {
		if (declaration.thrownExceptions == null || !fStatementAnalyzer.processesEnclosingMethod())
			return;
		TypeReference[] thrownExceptions= declaration.thrownExceptions;	
		for (int i= 0; i < thrownExceptions.length; i++) {
			TypeReference reference= thrownExceptions[i];
			TypeBinding binding= reference.binding;
			if (binding != null)
				fTypeNames.put(binding, reference);
		}
	}
	
	public void visit(ThrowStatement statement, BlockScope scope, int mode) {
		if (skipNode(mode))
			return;
			
		TypeBinding exceptionType= statement.exceptionType;
		if (exceptionType == null)		// Safety net for null bindings when compiling fails.
			return;
		
		if (isRuntimeException(exceptionType, scope) && !methodThrowsException(exceptionType))
			return;
			
		if (!fCurrentExceptions.contains(exceptionType))
			fCurrentExceptions.add(exceptionType);
		
		// Try to use the same type name (qualified or unqualified) for the method signature
		// as used for the new of local variable declaration.	
		Expression exception= statement.exception;
		if (exception instanceof AllocationExpression) {
			AllocationExpression allocation= (AllocationExpression)exception;
			if (allocation.type != null)
				fTypeNames.put(exceptionType, allocation.type);
		} else if (exception instanceof NameReference) {
			NameReference reference= (NameReference)exception;
			if (reference.binding instanceof LocalVariableBinding) {
				LocalVariableBinding binding= (LocalVariableBinding)reference.binding;
				fTypeNames.put(exceptionType, binding.declaration.type);
			}
		}
	}
	
	public void visit(MessageSend statement, BlockScope scope, int mode) {
		if (skipNode(mode))
			return;
		
		if (statement.binding == null)
			return;
				
		ReferenceBinding[] thrownExceptions= statement.binding.thrownExceptions;
		if (thrownExceptions != null) {
			for (int i= 0; i < thrownExceptions.length;i++) {
				ReferenceBinding exception= thrownExceptions[i];
				if (!fCurrentExceptions.contains(exception)) {
					fCurrentExceptions.add(exception);
				}
			}
		}
	}
	
	public void visit(TryStatement statement, BlockScope scope, int mode) {
		fCurrentExceptions= new ArrayList(1);
		fTryStack.push(fCurrentExceptions);
	}
	
	public void visitCatchArguments(Argument[] arguments, BlockScope scope, int mode) {
		if (!fStatementAnalyzer.processesEnclosingMethod())
			return;
			
		for (int i= 0; i < arguments.length; i++) {
			Argument argument= arguments[i];
			TypeBinding catchTypeBinding= argument.type.binding;
			if (mode == StatementAnalyzer.SELECTED) {
				List exceptions= new ArrayList(fCurrentExceptions);
				for (Iterator iter= exceptions.iterator(); iter.hasNext(); ) {
					ReferenceBinding throwTypeBinding= (ReferenceBinding)iter.next();
					if (catches(catchTypeBinding, throwTypeBinding))
						fCurrentExceptions.remove(throwTypeBinding);
				}
			} else {
				fTypeNames.put(catchTypeBinding, argument.type);
			}
		}
	}
	
	private boolean catches(TypeBinding catchTypeBinding, ReferenceBinding throwTypeBinding) {
		while(throwTypeBinding != null) {
			if (throwTypeBinding == catchTypeBinding)
				return true;
			throwTypeBinding= throwTypeBinding.superclass();	
		}
		return false;
	}
	
	public void endVisit(TryStatement statement, BlockScope scope, int mode) {
		List current= (List)fTryStack.pop();
		fCurrentExceptions= (List)fTryStack.peek();
		for (Iterator iter= current.iterator(); iter.hasNext();) {
			Object exception= iter.next();
			if (!fCurrentExceptions.contains(exception)) {
				fCurrentExceptions.add(exception);
			}
		}
	}
	
	private boolean skipNode(int mode) {
		return mode != StatementAnalyzer.SELECTED || !fStatementAnalyzer.processesEnclosingMethod();
	}
	
	private boolean isRuntimeException(TypeBinding binding, Scope scope) {
		if (!(binding instanceof ReferenceBinding))
			return false;
		
		ReferenceBinding thrownException= (ReferenceBinding)binding;	
		TypeBinding runTimeException= scope.getJavaLangRuntimeException();
		while (thrownException != null) {
			if (runTimeException == thrownException)
				return true;
			thrownException= thrownException.superclass();
		}
		return false;
	}
	
	private boolean methodThrowsException(TypeBinding exception) {
		ReferenceBinding[] methodExceptions = fStatementAnalyzer.getEnclosingMethod().binding.thrownExceptions;
		for (int i= 0; i < methodExceptions.length; i++) {
			if (exception == methodExceptions[i])
				return true;
		}
		return false;
	}
}