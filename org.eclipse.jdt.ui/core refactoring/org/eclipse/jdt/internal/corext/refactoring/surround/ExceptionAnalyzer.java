/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.surround;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.AstNode;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.ast.ThrowStatement;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.util.AbstractExceptionAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.util.Selection;

/* package */ class ExceptionAnalyzer extends AbstractExceptionAnalyzer {

	private AbstractMethodDeclaration fEnclosingMethod;
	private Selection fSelection;
	
	private static class ExceptionComparator implements Comparator {
		public int compare(Object o1, Object o2) {
			int d1= getDepth((ReferenceBinding)o1);
			int d2= getDepth((ReferenceBinding)o2);
			if (d1 < d2)
				return 1;
			if (d1 > d2)
				return -1;
			return 0;
		}
		private int getDepth(ReferenceBinding binding) {
			int result= 0;
			while (binding != null) {
				binding= binding.superclass();
				result++;
			}
			return result;
		}
	}
	
	private ExceptionAnalyzer(AbstractMethodDeclaration enclosingMethod, Selection selection) {
		Assert.isNotNull(enclosingMethod);
		Assert.isNotNull(selection);
		fEnclosingMethod= enclosingMethod;
		fSelection= selection;
	}
	
	public static TypeBinding[] perform(AbstractMethodDeclaration enclosingMethod, Selection selection, ClassScope scope) {
		ExceptionAnalyzer analyzer= new ExceptionAnalyzer(enclosingMethod, selection);
		enclosingMethod.traverse(analyzer, scope);
		List exceptions= analyzer.getCurrentExceptions();
		Collections.sort(exceptions, new ExceptionComparator());
		return (TypeBinding[]) exceptions.toArray(new TypeBinding[exceptions.size()]);
	}

	public boolean visit(ThrowStatement node, BlockScope scope) {
		TypeBinding exception= node.exceptionType;
		if (!isSelected(node) || exception == null) // Safety net for null bindings when compiling fails.
			return false;
		
		addException(exception);
		return true;
	}
	
	public boolean visit(MessageSend node, BlockScope scope) {
		if (!isSelected(node))
			return false;
		return handleExceptions(node.binding);
	}
	
	public boolean visit(AllocationExpression node, BlockScope scope) {
		if (!isSelected(node))
			return false;
		return handleExceptions(node.binding);
	}
	
	private boolean isSelected(AstNode node) {
		return fSelection.getVisitSelectionMode(node) == Selection.SELECTED;
	}
}