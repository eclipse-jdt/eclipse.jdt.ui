/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.participants.xml;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;

import org.eclipse.jdt.internal.corext.Assert;


public class IterateExpression extends CompositeExpression {
	
	public static final String NAME= "iterate";  //$NON-NLS-1$
	
	private static class IteratePool implements IVariablePool {
		
		private Iterator fIterator;
		private Object fDefaultVariable;
		private IVariablePool fParent;
		
		public IteratePool(IVariablePool parent, Iterator iterator) {
			Assert.isNotNull(parent);
			Assert.isNotNull(iterator);
			fParent= parent;
			fIterator= iterator;
		}
		public IVariablePool getParent() {
			return fParent;
		}
		public IVariablePool getRoot() {
			return fParent.getRoot();
		}
		public Object getDefaultVariable() {
			return fDefaultVariable;
		}
		public void addVariable(String name, Object value) {
			fParent.addVariable(name, value);
		}
		public Object removeVariable(String name) {
			return fParent.removeVariable(name);
		}
		public Object getVariable(String name) {
			return fParent.getVariable(name);
		}
		public Object resolveVariable(String name, Object[] args) throws CoreException {
			return fParent.resolveVariable(name, args);
		}
		public Object next() {
			fDefaultVariable= fIterator.next();
			return fDefaultVariable;
		}
		public boolean hasNext() {
			return fIterator.hasNext();
		}
	}
	
	private static final String ATT_OPERATOR= "operator"; //$NON-NLS-1$
	private static final int OR= 1;
	private static final int AND= 2;
	
	private int fOperator;
	
	public IterateExpression(IConfigurationElement configElement) throws CoreException {
		String opValue= configElement.getAttribute(ATT_OPERATOR);
		if (opValue == null) {
			fOperator= AND;
		} else {
			checkAttribute(ATT_OPERATOR, opValue, new String[] {"and", "or"});  //$NON-NLS-1$//$NON-NLS-2$
			if ("and".equals(opValue)) { //$NON-NLS-1$
				fOperator= AND;
			} else {
				fOperator= OR;
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see Expression#evaluate(IVariablePool)
	 */
	public TestResult evaluate(IVariablePool pool) throws CoreException {
		Object var= pool.getDefaultVariable();
		Expressions.checkCollection(var);
		Collection col= (Collection)var;
		switch (col.size()) {
			case 0:
				return fOperator == AND ? TestResult.TRUE : TestResult.FALSE;
			case 1:
				if (col instanceof List)
					return evaluateAnd(new DefaultVariable(pool, ((List)col).get(0)));
				// fall through
			default:
				IteratePool iter= new IteratePool(pool, col.iterator());
				TestResult result= fOperator == AND ? TestResult.TRUE : TestResult.FALSE;
				while (iter.hasNext()) {
					iter.next();
					switch(fOperator) {
						case OR:
							result= result.or(evaluateAnd(iter));
							if (result == TestResult.TRUE)
								return result;
							break;
						case AND:
							result= result.and(evaluateAnd(iter));
							if (result != TestResult.TRUE)
								return result;
							break;
					}
				}
				return result;
		}
	}
}
