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

import org.eclipse.core.runtime.Platform;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Type;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.CompilationUnitRange;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;


public class TypeConstraintFactory2 {
	
	private static class TypeConstraintComparer implements IElementComparer/*<ITypeConstraint2>*/ {
		public boolean equals(Object a, Object b) {
			return ((ITypeConstraint2) a).isSameAs((ITypeConstraint2) b);
		}
		public int hashCode(Object element) {
			return ((ITypeConstraint2) element).getHash();
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
	protected static final boolean DEBUG= "true".equalsIgnoreCase(Platform.getDebugOption("org.eclipse.jdt.ui/debug/TypeConstraints")); //$NON-NLS-1$//$NON-NLS-2$
	
	protected CustomHashtable/*<TypeConstraint2, NULL>*/ fTypeConstraints;
	protected CustomHashtable/*<ConstraintVariable2, Object>*/ fConstraintVariables;
	protected TypeHandleFactory fTypeHandleFactory;
	protected boolean fStoreToString;
	
	public TypeConstraintFactory2() {
		fTypeConstraints= new CustomHashtable(new TypeConstraintComparer());
		fConstraintVariables= new CustomHashtable(new ConstraintVariableComparer());
		fTypeHandleFactory= new TypeHandleFactory();
		fStoreToString= DEBUG;
	}
	
	public List getUsedIn(ConstraintVariable2 cv) {
		Object storedTcs= fConstraintVariables.get(cv);
		if (storedTcs instanceof ITypeConstraint2)
			return Collections.singletonList(storedTcs);
		else if (storedTcs instanceof List)
			return (List) storedTcs;
		else
			throw new IllegalStateException("Unknown constraint variable: " + cv); //$NON-NLS-1$
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
	
	/**
	 * Controls calculation and storage of information for more readable toString() messages.
	 * 
	 * Warning: This method is for testing purposes only and should not be called except from unit tests!
	 * 
	 * @param store <code>true</code> iff information for toString() should be stored 
	 */
	public void setStoreToString(boolean store) {
		fStoreToString= store;
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
	
	protected ITypeConstraint2[] createSimpleTypeConstraint(ConstraintVariable2 cv1, ConstraintVariable2 cv2, ConstraintOperator2 operator) {
		if (filter(cv1, cv2, operator))
			return new ITypeConstraint2[0];
		
		ConstraintVariable2 storedCv1= (ConstraintVariable2) fConstraintVariables.getKey(cv1);
		ConstraintVariable2 storedCv2= (ConstraintVariable2) fConstraintVariables.getKey(cv1);
		SimpleTypeConstraint2 typeConstraint= new SimpleTypeConstraint2(
				storedCv1 == null ? cv1 : storedCv1,
				storedCv2 == null ? cv2 : storedCv2,
				operator);
		
		Object storedTc= fTypeConstraints.getKey(typeConstraint);
		if (storedTc == null) {
			fTypeConstraints.put(typeConstraint, NULL); //TODO: should be a CustomHashset?
		} else {
			typeConstraint= (SimpleTypeConstraint2) storedTc;
		}
		
		registerCvInTc(storedCv1, cv1, typeConstraint);
		registerCvInTc(storedCv2, cv2, typeConstraint);
		return new ITypeConstraint2[]{ typeConstraint };
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
	
	public CompositeOrTypeConstraint2 createCompositeOrTypeConstraint(ITypeConstraint2[] constraints) {
		// TODO Auto-generated method stub
		return null;
	}
	
	//--- Creation of ConstraintVariables (CVs are only stored when really used in a TC) ---
	
//	public ConstraintVariable2 makeExpressionOrTypeVariable(Expression expression) {
//		Assert.isTrue(false);
//		return null;
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
//	}

	public ExpressionVariable2 makeExpressionVariable(Expression expression) {
		TypeHandle typeHandle= fTypeHandleFactory.getTypeHandle(expression.resolveTypeBinding());
		ExpressionVariable2 expressionVariable= new ExpressionVariable2(typeHandle, expression);
		if (fStoreToString)
			expressionVariable.setData(ConstraintVariable2.TO_STRING, "[" + expression.toString() + "]"); //$NON-NLS-1$//$NON-NLS-2$
		return expressionVariable;
	}

	public TypeVariable2 makeTypeVariable(Type type) {
		TypeHandle typeHandle= fTypeHandleFactory.getTypeHandle(type.resolveBinding());
		ICompilationUnit cu= RefactoringASTParser.getCompilationUnit(type);
		CompilationUnitRange range= new CompilationUnitRange(cu, type);
		TypeVariable2 typeVariable= new TypeVariable2(typeHandle, range);
		if (fStoreToString)
			typeVariable.setData(ConstraintVariable2.TO_STRING, type.toString());
		return typeVariable;
	}

	/**
	 * Create a constraint variable for an Expression that is a reference to a type.
	 * @param expression
	 * @param typeBinding
	 * @return
	 * @exception RuntimeException
	 */
	public TypeVariable2 makeTypeVariable(Expression expression, ITypeBinding typeBinding) {
		TypeHandle typeHandle= fTypeHandleFactory.getTypeHandle(typeBinding);
		ICompilationUnit cu= RefactoringASTParser.getCompilationUnit(expression);
		CompilationUnitRange range= new CompilationUnitRange(cu, expression);
		TypeVariable2 typeVariable= new TypeVariable2(typeHandle, range);
		if (fStoreToString)
			typeVariable.setData(ConstraintVariable2.TO_STRING, expression.toString());
		return typeVariable;
	}
	
	public ParameterTypeVariable2 makeParameterTypeVariable(IMethodBinding methodBinding, int parameterIndex) {
		TypeHandle typeHandle= fTypeHandleFactory.getTypeHandle(methodBinding.getParameterTypes() [parameterIndex]);
		ParameterTypeVariable2 cv= new ParameterTypeVariable2(typeHandle, parameterIndex, methodBinding);
		if (fStoreToString)
			cv.setData(ConstraintVariable2.TO_STRING, "[Parameter(" + parameterIndex + "," + Bindings.asString(methodBinding) + ")]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return cv;
	}

	public DeclaringTypeVariable2 makeDeclaringTypeVariable(IMethodBinding methodBinding) {
		TypeHandle declaringTypeHandle= fTypeHandleFactory.getTypeHandle(methodBinding.getDeclaringClass());
		return makeDeclaringTypeVariable(declaringTypeHandle, methodBinding);
	}

	public DeclaringTypeVariable2 makeDeclaringTypeVariable(IVariableBinding fieldBinding) {
		Assert.isTrue(fieldBinding.isField());
		TypeHandle declaringTypeHandle= fTypeHandleFactory.getTypeHandle(fieldBinding.getDeclaringClass());
		return makeDeclaringTypeVariable(declaringTypeHandle, fieldBinding);
	}
	
	private DeclaringTypeVariable2 makeDeclaringTypeVariable(TypeHandle declaringTypeHandle, IBinding memberBinding) {
		DeclaringTypeVariable2 cv= new DeclaringTypeVariable2(declaringTypeHandle, memberBinding);
		return cv;
	}

	public PlainTypeVariable2 makePlainTypeVariable(ITypeBinding typeBinding) {
		TypeHandle typeHandle= fTypeHandleFactory.getTypeHandle(typeBinding);
		PlainTypeVariable2 cv= new PlainTypeVariable2(typeHandle);
		return cv;
	}

	public ReturnTypeVariable2 makeReturnTypeVariable(IMethodBinding methodBinding) {
		TypeHandle returnTypeHandle= fTypeHandleFactory.getTypeHandle(methodBinding.getReturnType());
		ReturnTypeVariable2 cv= new ReturnTypeVariable2(returnTypeHandle);
		return cv;
	}

}
