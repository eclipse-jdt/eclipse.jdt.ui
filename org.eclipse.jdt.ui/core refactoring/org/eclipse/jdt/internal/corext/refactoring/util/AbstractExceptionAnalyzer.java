/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import org.eclipse.jdt.internal.compiler.AbstractSyntaxTreeVisitorAdapter;
import org.eclipse.jdt.internal.compiler.ast.AllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.AnonymousLocalTypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.LocalTypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.ast.ThrowStatement;
import org.eclipse.jdt.internal.compiler.ast.TryStatement;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

public abstract class AbstractExceptionAnalyzer extends AbstractSyntaxTreeVisitorAdapter {
	
	private List fCurrentExceptions;	// Elements in this list are of type TypeBinding
	private Stack fTryStack;

	protected AbstractExceptionAnalyzer() {
		fTryStack= new Stack();
		fCurrentExceptions= new ArrayList(1);
		fTryStack.push(fCurrentExceptions);
	}

	public boolean visit(LocalTypeDeclaration node, BlockScope scope) {
		// Don't dive into a local type.
		return false;
	}	

	public boolean visit(AnonymousLocalTypeDeclaration node, BlockScope scope) {
		// Don't dive into a anonymous type.
		return false;
	}
	
	public abstract boolean visit(ThrowStatement node, BlockScope scope);
	
	public abstract boolean visit(MessageSend node, BlockScope scope);
	
	public abstract boolean visit(AllocationExpression node, BlockScope scope);
	
	public boolean visit(TryStatement node, BlockScope scope) {
		fCurrentExceptions= new ArrayList(1);
		fTryStack.push(fCurrentExceptions);
		// Actually this is the wrong scope. But since tryBlock.scope is not visible we can't do any better. 
		// The scope is only used to call getJavaLangRuntimeException so it doesn't matter.
		node.tryBlock.traverse(this, scope);
		// do not dive into. We have to do this in endVisit.
		return false;
	}
	
	public void endVisit(TryStatement node, BlockScope scope) {
		if (node.catchArguments != null)
			handleCatchArguments(node.catchArguments, scope);
		List current= (List)fTryStack.pop();
		fCurrentExceptions= (List)fTryStack.peek();
		for (Iterator iter= current.iterator(); iter.hasNext();) {
			addException(iter.next());
		}
		// Actually we are using the wrong scope. But since tryBlock.scope is not visible we can't do any better. 
		// The scope is only used to call getJavaLangRuntimeException so it doesn't matter.
		if (node.catchBlocks != null) {
			for (int i= 0; i < node.catchBlocks.length; i++) {
				node.catchBlocks[i].traverse(this, scope);
			}
		}
		if (node.finallyBlock != null)
			node.finallyBlock.traverse(this, scope);
	}
	
	protected boolean handleExceptions(MethodBinding binding) {
		if (binding == null)
			return false;
			
		ReferenceBinding[] thrownExceptions= binding.thrownExceptions;
		if (thrownExceptions != null) {
			for (int i= 0; i < thrownExceptions.length;i++) {
				addException(thrownExceptions[i]);
			}
		}
		return true;
	}
	
	protected void addException(Object exception) {
		if (!fCurrentExceptions.contains(exception))
			fCurrentExceptions.add(exception);
	}
	
	protected boolean isRuntimeException(TypeBinding binding, Scope scope) {
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
	
	protected List getCurrentExceptions() {
		return fCurrentExceptions;
	}
	
	private void handleCatchArguments(Argument[] arguments, BlockScope scope) {
		for (int i= 0; i < arguments.length; i++) {
			Argument argument= arguments[i];
			TypeBinding catchTypeBinding= argument.type.binding;
			List exceptions= new ArrayList(fCurrentExceptions);
			for (Iterator iter= exceptions.iterator(); iter.hasNext(); ) {
				ReferenceBinding throwTypeBinding= (ReferenceBinding)iter.next();
				if (catches(catchTypeBinding, throwTypeBinding))
					fCurrentExceptions.remove(throwTypeBinding);
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
}
