/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.typeconstraints;

import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.dom.Bindings;


public final class Expressions {

	private Expressions() {
	}
	
	public static boolean equalBindings(Expression expression1, Expression expression2){
		IBinding binding1= resolveBinding(expression1);
		IBinding binding2= resolveBinding(expression2);
		return binding1 != null && binding2 != null && Bindings.equals(binding1, binding2);
	}
	
	public static int hashCode(Expression expression){
		Assert.isNotNull(expression);
		IBinding binding= resolveBinding(expression);
		if (binding == null)
			return expression.hashCode();
		else if (binding.getKey() == null)
			return binding.hashCode();
		else
			return resolveBinding(expression).getKey().hashCode();
	}
	
	private static IBinding resolveBinding(Expression expression){
		if (expression instanceof Name)
			return ((Name)expression).resolveBinding();
		else if (expression instanceof Assignment)
			return resolveBinding(((Assignment)expression).getLeftHandSide());//TODO ???
		else if (expression instanceof MethodInvocation)
			return ((MethodInvocation)expression).resolveMethodBinding();
		else if (expression instanceof SuperMethodInvocation)
			return ((SuperMethodInvocation)expression).resolveMethodBinding();
		else if (expression instanceof FieldAccess)
			return ((FieldAccess)expression).getName().resolveBinding();
		else if (expression instanceof SuperFieldAccess)
			return ((SuperFieldAccess)expression).getName().resolveBinding();
		return null;
	}
}
