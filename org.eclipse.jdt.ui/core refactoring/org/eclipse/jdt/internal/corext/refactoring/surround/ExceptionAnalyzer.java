/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.surround;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.ThrowStatement;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.refactoring.util.AbstractExceptionAnalyzer;

/* package */ class ExceptionAnalyzer extends AbstractExceptionAnalyzer {

	private Selection fSelection;
	
	private static class ExceptionComparator implements Comparator {
		public int compare(Object o1, Object o2) {
			int d1= getDepth((ITypeBinding)o1);
			int d2= getDepth((ITypeBinding)o2);
			if (d1 < d2)
				return 1;
			if (d1 > d2)
				return -1;
			return 0;
		}
		private int getDepth(ITypeBinding binding) {
			int result= 0;
			while (binding != null) {
				binding= binding.getSuperclass();
				result++;
			}
			return result;
		}
	}
	
	private ExceptionAnalyzer(Selection selection) {
		Assert.isNotNull(selection);
		fSelection= selection;
	}
	
	public static ITypeBinding[] perform(BodyDeclaration enclosingNode, Selection selection) {
		ExceptionAnalyzer analyzer= new ExceptionAnalyzer(selection);
		enclosingNode.accept(analyzer);
		List exceptions= analyzer.getCurrentExceptions();
		Collections.sort(exceptions, new ExceptionComparator());
		return (ITypeBinding[]) exceptions.toArray(new ITypeBinding[exceptions.size()]);
	}

	public boolean visit(ThrowStatement node) {
		ITypeBinding exception= node.getExpression().resolveTypeBinding();
		if (!isSelected(node) || exception == null || Bindings.isRuntimeException(exception, node.getAST())) // Safety net for null bindings when compiling fails.
			return true;
		
		addException(exception);
		return true;
	}
	
	public boolean visit(MethodInvocation node) {
		if (!isSelected(node))
			return false;
		return handleExceptions(ASTNodes.getMethodBinding(node.getName()), node.getAST());
	}
	
	public boolean visit(SuperMethodInvocation node) {
		if (!isSelected(node))
			return false;
		return handleExceptions(ASTNodes.getMethodBinding(node.getName()), node.getAST());
	}
	
	public boolean visit(ClassInstanceCreation node) {
		if (!isSelected(node))
			return false;
		return handleExceptions(node.resolveConstructorBinding(), node.getAST());
	}
	
	private boolean handleExceptions(IMethodBinding binding, AST ast) {
		if (binding == null)
			return true;
		ITypeBinding[] exceptions= binding.getExceptionTypes();
		for (int i= 0; i < exceptions.length; i++) {
			ITypeBinding exception= exceptions[i];
			if (!Bindings.isRuntimeException(exception, ast))
				addException(exception);
		}
		return true;
	}
		
	private boolean isSelected(ASTNode node) {
		return fSelection.getVisitSelectionMode(node) == Selection.SELECTED;
	}
}