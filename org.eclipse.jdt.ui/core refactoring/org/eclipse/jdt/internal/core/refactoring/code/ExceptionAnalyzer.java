/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.code;

import java.util.ArrayList;import java.util.HashMap;import java.util.Iterator;import java.util.List;import java.util.Stack;import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;import org.eclipse.jdt.internal.compiler.ast.Argument;import org.eclipse.jdt.internal.compiler.ast.MessageSend;import org.eclipse.jdt.internal.compiler.ast.TryStatement;import org.eclipse.jdt.internal.compiler.ast.TypeReference;import org.eclipse.jdt.internal.compiler.lookup.BlockScope;import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;import org.eclipse.jdt.internal.compiler.lookup.Scope;import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
/* package */ class ExceptionAnalyzer {

	private List fCurrentExceptions;
	private Stack fTryStack;
	private HashMap fTypeNames;
	
	public ExceptionAnalyzer() {
		fTryStack= new Stack();
		fCurrentExceptions= new ArrayList(1);
		fTryStack.push(fCurrentExceptions);
		fTypeNames= new HashMap(10);
	}

	public void visitAbstractMethodDeclaration(AbstractMethodDeclaration declaration, Scope scope) {
		if (declaration.thrownExceptions == null)
			return;
		TypeReference[] thrownExceptions= declaration.thrownExceptions;	
		for (int i= 0; i < thrownExceptions.length; i++) {
			TypeReference reference= thrownExceptions[i];
			TypeBinding binding= reference.binding;
			if (binding != null)
				fTypeNames.put(binding, reference);
		}
	}
	
	public void visitMessageSend(MessageSend statement, BlockScope scope, int mode) {
		if (mode != StatementAnalyzer.SELECTED)
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
	
	public void visitTryStatement(TryStatement statement, BlockScope scope, int mode) {
		if (mode != StatementAnalyzer.SELECTED)
			return;
			
		fCurrentExceptions= new ArrayList(1);
		fTryStack.push(fCurrentExceptions);
	}
	
	public void visitCatchArguments(Argument[] arguments, BlockScope scope, int mode) {
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
	
	public void visitEndTryStatement(TryStatement statement, BlockScope scope, int mode) {
		if (mode != StatementAnalyzer.SELECTED)
			return;
			
		List current= fCurrentExceptions;
		fCurrentExceptions= (List)fTryStack.pop();
		for (Iterator iter= current.iterator(); iter.hasNext();) {
			Object exception= iter.next();
			if (!fCurrentExceptions.contains(exception)) {
				fCurrentExceptions.add(exception);
			}
		}
	}
	
	public String getThrowSignature() {
		if (fCurrentExceptions.size() == 0)
			return "";
			
		StringBuffer result= new StringBuffer(" throws ");
		int i= 0;
		for (Iterator iter= fCurrentExceptions.iterator(); iter.hasNext(); i++) {
			TypeBinding typeBinding= (TypeBinding)iter.next();
			if (i > 0)
				result.append(", ");
			TypeReference reference= (TypeReference)fTypeNames.get(typeBinding);
			if (reference == null) {
				result.append(typeBinding.qualifiedPackageName());
				result.append(".");
				result.append(typeBinding.qualifiedSourceName());
			} else {
				result.append(reference.toStringExpression(0));
			}
		}
		return result.toString();
	}
}