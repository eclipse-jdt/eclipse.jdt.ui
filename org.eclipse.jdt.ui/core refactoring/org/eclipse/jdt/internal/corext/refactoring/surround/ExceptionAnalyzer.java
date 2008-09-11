/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.surround;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.ThrowStatement;

import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.refactoring.util.AbstractExceptionAnalyzer;

public class ExceptionAnalyzer extends AbstractExceptionAnalyzer {

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
		if (enclosingNode.getNodeType() == ASTNode.METHOD_DECLARATION) {
			List thrownExceptions= ((MethodDeclaration)enclosingNode).thrownExceptions();
			for (Iterator thrown= thrownExceptions.iterator(); thrown.hasNext();) {
				ITypeBinding thrownException= ((Name)thrown.next()).resolveTypeBinding();
				if (thrownException != null) {
					for (Iterator excep= exceptions.iterator(); excep.hasNext();) {
						ITypeBinding exception= (ITypeBinding) excep.next();
						if (exception.isAssignmentCompatible(thrownException))
							excep.remove();
					}
				}
			}
		}
		Collections.sort(exceptions, new ExceptionComparator());
		return (ITypeBinding[]) exceptions.toArray(new ITypeBinding[exceptions.size()]);
	}

	public boolean visit(ThrowStatement node) {
		ITypeBinding exception= node.getExpression().resolveTypeBinding();
		if (!isSelected(node) || exception == null || Bindings.isRuntimeException(exception)) // Safety net for null bindings when compiling fails.
			return true;

		addException(exception);
		return true;
	}

	public boolean visit(MethodInvocation node) {
		if (!isSelected(node))
			return false;
		return handleExceptions(node.resolveMethodBinding());
	}

	public boolean visit(SuperMethodInvocation node) {
		if (!isSelected(node))
			return false;
		return handleExceptions(node.resolveMethodBinding());
	}

	public boolean visit(ClassInstanceCreation node) {
		if (!isSelected(node))
			return false;
		return handleExceptions(node.resolveConstructorBinding());
	}

	public boolean visit(ConstructorInvocation node) {
		if (!isSelected(node))
			return false;
		return handleExceptions(node.resolveConstructorBinding());
	}

	public boolean visit(SuperConstructorInvocation node) {
		if (!isSelected(node))
			return false;
		return handleExceptions(node.resolveConstructorBinding());
	}

	private boolean handleExceptions(IMethodBinding binding) {
		if (binding == null)
			return true;
		ITypeBinding[] exceptions= binding.getExceptionTypes();
		for (int i= 0; i < exceptions.length; i++) {
			addException(exceptions[i]);
		}
		return true;
	}

	private boolean isSelected(ASTNode node) {
		return fSelection.getVisitSelectionMode(node) == Selection.SELECTED;
	}
}
