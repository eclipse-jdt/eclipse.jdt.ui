/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.corext.refactoring.typeconstraints2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Type;

import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.CompilationUnitRange;


public class TypeConstraintFactory2 {
	
	private static class TypeConstraintComparer implements IElementComparer/*<TypeConstraint2>*/ {

		public boolean equals(Object a, Object b) {
			// TODO Auto-generated method stub
			return false;
		}

		public int hashCode(Object element) {
			// TODO Auto-generated method stub
			return 0;
		}
	}
	
	private static class ConstraintVariableComparer implements IElementComparer/*<ConstraintVariable2>*/ {
		public boolean equals(Object a, Object b) {
			return ((ConstraintVariable2) a).isSameAs((ConstraintVariable2) b);
		}

		public int hashCode(Object element) {
			return ((ConstraintVariable2) element).getHash();
		}
	}
	
	private static final Object NULL= new Object();
	
	CustomHashtable/*<TypeConstraint2>*/ fTypeConstraints;
	CustomHashtable/*<ConstraintVariable2, Object>*/ fConstraintVariables;
	
	public TypeConstraintFactory2() {
		fTypeConstraints= new CustomHashtable(new TypeConstraintComparer());
		fConstraintVariables= new CustomHashtable(new ConstraintVariableComparer());
	}
	
	public ITypeConstraint2[] createSubtypeConstraint(ConstraintVariable2 v1, ConstraintVariable2 v2){
		return createSimpleTypeConstraint(v1, v2, ConstraintOperator2.createSubTypeOperator());
	}
	
	public ITypeConstraint2[] createStrictSubtypeConstraint(ConstraintVariable2 v1, ConstraintVariable2 v2){
		return createSimpleTypeConstraint(v1, v2, ConstraintOperator2.createStrictSubtypeOperator());
	}
	
	public ITypeConstraint2[] createEqualsConstraint(ConstraintVariable2 v1, ConstraintVariable2 v2){
		return createSimpleTypeConstraint(v1, v2, ConstraintOperator2.createEqualsOperator());
	}
	
	private ITypeConstraint2[] createSimpleTypeConstraint(ConstraintVariable2 cv1, ConstraintVariable2 cv2, ConstraintOperator2 operator) {
		if (filter(cv1, cv2, operator)){
			return new ITypeConstraint2[0];
		} else {
			ConstraintVariable2 storedCv1= (ConstraintVariable2) fConstraintVariables.getKey(cv1);
			ConstraintVariable2 storedCv2= (ConstraintVariable2) fConstraintVariables.getKey(cv1);
			SimpleTypeConstraint2 typeConstraint= new SimpleTypeConstraint2(
					storedCv1 == null ? cv1 : storedCv1,
					storedCv2 == null ? cv2 : storedCv2,
					operator);
			
			Object storedTc= fTypeConstraints.getKey(typeConstraint);
			if (storedTc == null) {
			} else {
				fTypeConstraints.put(typeConstraint, NULL); //TODO: should be a CustomHashset?
				typeConstraint= (SimpleTypeConstraint2) storedTc;
			}
			
			registerCvInTc(storedCv1, cv1, typeConstraint);
			registerCvInTc(storedCv2, cv2, typeConstraint);
			return new ITypeConstraint2[]{ typeConstraint };
		}
	}

	private void registerCvInTc(ConstraintVariable2 storedCv, ConstraintVariable2 cv, SimpleTypeConstraint2 typeConstraint) {
		if (storedCv == null) {
			// new CV -> directly store TC:
			fConstraintVariables.put(cv, typeConstraint);
		} else {
			// existing CV:
			//TODO: could avoid call to get() if there was a method HashMapEntry getEntry(Object key) 
			Object storedTcs= fConstraintVariables.get(storedCv);
			ArrayList/*<ITypeConstraint2>*/ typeConstraintList;
			if (storedTcs instanceof ITypeConstraint2) {
				// CV only used in one TC so far:
				typeConstraintList= new ArrayList(2);
				fConstraintVariables.put(cv, typeConstraintList);
				typeConstraintList.add(storedTcs); // typeConstraintList.add((ITypeConstraint2) tcs);
			} else {
				// CV already used in multiple TCs:
				typeConstraintList= (ArrayList) storedTcs;
			}
			typeConstraintList.add(typeConstraint);
		}
	}
	
	public List getUsedIn(ConstraintVariable2 cv) {
		Object storedTcs= fConstraintVariables.get(cv);
		if (storedTcs instanceof ITypeConstraint2)
			return Collections.singletonList(storedTcs);
		else if (storedTcs instanceof List)
			return (List) storedTcs;
		else
			return Collections.EMPTY_LIST;
	}
	
	public CompositeOrTypeConstraint2 createCompositeOrTypeConstraint(ITypeConstraint2[] constraints) {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * Allows for avoiding the creation of SimpleTypeConstraints based on properties of
	 * their constituent ConstraintVariables and ConstraintOperators. Can be used to e.g. 
	 * avoid creation of constraints for assignments between built-in types.
	 * 
	 * @param v1 
	 * @param v2
	 * @param operator
	 * @return whether the constraint should <em>not</em> be created
	 */
	public boolean filter(ConstraintVariable2 v1, ConstraintVariable2 v2, ConstraintOperator2 operator) {
		// TODO Auto-generated method stub
		return false;
	}
	
	
	public ConstraintVariable2 makeExpressionOrTypeVariable(Expression expression) {
		return null;
//		IBinding binding= ExpressionVariable.resolveBinding(expression);
//
//		if (binding instanceof ITypeBinding) {
//			//TODO: only for qualified names or field access expressions to static fields
//			// => should separate makeExpressionOrTypeVariable() from makeExpressionVariable() !
//			ICompilationUnit cu= RefactoringASTParser.getCompilationUnit(expression);
//			Assert.isNotNull(cu);
//			CompilationUnitRange range= new CompilationUnitRange(cu, expression);
//			return makeTypeVariable((ITypeBinding) getKey(binding), expression.toString(), range);
//		}
//
//		if (ASTNodes.isLiteral(expression)) {
//			Integer nodeType= new Integer(expression.getNodeType());
//			if (! fLiteralMap.containsKey(nodeType)) {
//				fLiteralMap.put(nodeType, new ExpressionVariable(expression));
//			}
//			return (ExpressionVariable) fLiteralMap.get(nodeType);
//		}
//
//		// For ExpressionVariables, there are two cases. If the expression has a
//		// binding
//		// we use that as the key. Otherwise, we use the CompilationUnitRange.
//		// See
//		// also ExpressionVariable.equals()
//		ExpressionVariable ev;
//		Object key;
//		if (binding != null) {
//			key= getKey(binding);
//		} else {
//			key= new CompilationUnitRange(ASTCreator.getCu(expression), expression);
//		}
//		ev= (ExpressionVariable) fExpressionMap.get(key);
//
//		if (ev == null) {
//			ev= new ExpressionVariable(expression);
//			fExpressionMap.put(key, ev);
//		}
//		return ev;
	}

	public TypeVariable2 makeTypeVariable(ITypeBinding binding, String source, CompilationUnitRange range) {
		return null;
//		if (! fTypeVariableMap.containsKey(range))
//			fTypeVariableMap.put(range, new TypeVariable2(binding, source, range));
//		return (TypeVariable) fTypeVariableMap.get(range);
	}

	public TypeVariable2 makeTypeVariable(Type type) {
		// TODO Auto-generated method stub
		return null;
	}

	
}
