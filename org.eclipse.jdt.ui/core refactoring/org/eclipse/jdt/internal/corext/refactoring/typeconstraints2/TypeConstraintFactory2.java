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
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.Platform;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
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
	
	private static final Object NULL= new Object() {
		public String toString() {
			return ""; //$NON-NLS-1$
		}
	};
	protected static final boolean DEBUG= "true".equalsIgnoreCase(Platform.getDebugOption("org.eclipse.jdt.ui/debug/TypeConstraints")); //$NON-NLS-1$//$NON-NLS-2$
	
	protected TypeHandleFactory fTypeHandleFactory;
	protected static boolean fStoreToString= DEBUG;
	
	
	/**
	 * Map from {@link ConstraintVariable2} to
	 * <ul>
	 * <li>{@link ITypeConstraint2}, or</li>
	 * <li>{@link List}&lt;{@link ITypeConstraint2}&gt;</li>
	 * </ul>
	 */
	protected CustomHashtable/*<ConstraintVariable2, Object>*/ fConstraintVariables;
	
	protected CustomHashtable/*<TypeConstraint2, NULL>*/ fTypeConstraints;
	
	private HashSet fCuScopedConstraintVariables;
	
	private Collection fNewTypeConstraints;
	private Collection fNewConstraintVariables;
	
	public TypeConstraintFactory2() {
		fTypeConstraints= new CustomHashtable(new TypeConstraintComparer());
		fConstraintVariables= new CustomHashtable(new ConstraintVariableComparer());
		fTypeHandleFactory= new TypeHandleFactory();
		
		fCuScopedConstraintVariables= new HashSet();
		fNewTypeConstraints= new ArrayList();
		fNewConstraintVariables= new ArrayList();
	}
	
	/**
	 * @param cv
	 * @return a List of ITypeConstraint2s where cv is used
	 */
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
	 * Resets the accumulators for {@link #getNewConstraintVariables()} and
	 * {@link #getNewTypeConstraints()}.
	 */
	public void newCu() {
		fNewTypeConstraints.clear();
		fNewConstraintVariables.clear();
		pruneUnusedCuScopedCvs();
		fCuScopedConstraintVariables.clear();
	}
	
	private void pruneUnusedCuScopedCvs() {
		for (Iterator iter= fCuScopedConstraintVariables.iterator(); iter.hasNext();) {
			ConstraintVariable2 cv= (ConstraintVariable2) iter.next();
			if (getUsedIn(cv).size() == 0)
				fConstraintVariables.remove(cv);
		}
	}

	public ConstraintVariable2[] getAllConstraintVariables() {
		ConstraintVariable2[] result= new ConstraintVariable2[fConstraintVariables.size()];
		int i= 0;
		for (Enumeration e= fConstraintVariables.keys(); e.hasMoreElements(); i++) {
			result[i]= (ConstraintVariable2) e.nextElement();
		}
		return result;
	}
	
	public ConstraintVariable2[] getNewConstraintVariables() {
		return (ConstraintVariable2[]) fNewConstraintVariables.toArray(new ConstraintVariable2[fNewConstraintVariables.size()]);
	}
	
	public ITypeConstraint2[] getNewTypeConstraints() {
		return (ITypeConstraint2[]) fNewTypeConstraints.toArray(new ITypeConstraint2[fNewTypeConstraints.size()]);
	}
	
//	public Set getAllTypeConstraints() {
//		fTypeConstraints.keys();
//	}
	
	/**
	 * Allows for avoiding the creation of SimpleTypeConstraints based on properties of
	 * their constituent ConstraintVariables and ConstraintOperators. Can be used to e.g. 
	 * avoid creation of constraints for assignments between built-in types.
	 * 
	 * @param cv1 
	 * @param cv2
	 * @param operator
	 * @return <code>true</code> iff the type constraint should really be created
	 */
	public boolean keep(ConstraintVariable2 cv1, ConstraintVariable2 cv2, ConstraintOperator2 operator) {
		if (cv1 == cv2) {
			Assert.isTrue(false);
		}
		if (cv1.isSameAs(cv2)) {
			Assert.isTrue(false);
		}
		return cv1 != null && cv2 != null;
	}
	
	/**
	 * @param typeBinding the type binding to check
	 * @return whether the constraint variable should <em>not</em> be created
	 */
	public boolean filterConstraintVariableType(ITypeBinding typeBinding) {
		return false;
	}
	
	/**
	 * Controls calculation and storage of information for more readable toString() messages.
	 * <p><em>Warning: This method is for testing purposes only and should not be called except from unit tests.</em></p>
	 * 
	 * @param store <code>true</code> iff information for toString() should be stored 
	 */
	public static void setStoreToString(boolean store) {
		fStoreToString= store;
	}
	
	public ITypeConstraint2[] createSubtypeConstraint(ConstraintVariable2 v1, ConstraintVariable2 v2){
		return createSimpleTypeConstraint(v1, v2, ConstraintOperator2.createSubTypeOperator());
	}
	
//	public ITypeConstraint2[] createStrictSubtypeConstraint(ConstraintVariable2 v1, ConstraintVariable2 v2){
//		return createSimpleTypeConstraint(v1, v2, ConstraintOperator2.createStrictSubtypeOperator());
//	}
//	
	public TypeHandleFactory getTypeHandleFactory() {
		return fTypeHandleFactory;
	}
	
//	/**
//	 * @param v1
//	 * @param v2
//	 * @return
//	 * @deprecated resolve on creation
//	 */
//	public ITypeConstraint2[] createEqualsConstraint(ConstraintVariable2 v1, ConstraintVariable2 v2){
//		return createSimpleTypeConstraint(v1, v2, ConstraintOperator2.createEqualsOperator());
//	}
//	
	protected ITypeConstraint2[] createSimpleTypeConstraint(ConstraintVariable2 cv1, ConstraintVariable2 cv2, ConstraintOperator2 operator) {
		if (! keep(cv1, cv2, operator))
			return new ITypeConstraint2[0];
		
		ConstraintVariable2 storedCv1= (ConstraintVariable2) fConstraintVariables.getKey(cv1);
		ConstraintVariable2 storedCv2= (ConstraintVariable2) fConstraintVariables.getKey(cv2);
		SimpleTypeConstraint2 typeConstraint= new SimpleTypeConstraint2(
				storedCv1 == null ? cv1 : storedCv1,
				storedCv2 == null ? cv2 : storedCv2,
				operator);
		
		Object storedTc= fTypeConstraints.getKey(typeConstraint);
		if (storedTc == null) {
			fTypeConstraints.put(typeConstraint, NULL); //TODO: should be a CustomHashset?
			fNewTypeConstraints.add(typeConstraint);
		} else {
			typeConstraint= (SimpleTypeConstraint2) storedTc;
		}
		
		registerCvWithTc(storedCv1, cv1, typeConstraint);
		registerCvWithTc(storedCv2, cv2, typeConstraint);
		return new ITypeConstraint2[]{ typeConstraint };
	}

	private void registerCvWithTc(ConstraintVariable2 storedCv, ConstraintVariable2 cv, SimpleTypeConstraint2 typeConstraint) {
		//TODO: special handling for CollectionElementVariable2
		if (storedCv == null) {
			// new CV -> directly store TC:
			fConstraintVariables.put(cv, typeConstraint);
			fNewConstraintVariables.add(cv);
			registerElementCVWithTC(cv, typeConstraint);
		} else {
			// existing stored CV:
			//TODO: could avoid call to get() if there was a method HashMapEntry getEntry(Object key) 
			Object storedTcs= fConstraintVariables.get(storedCv);
			ArrayList/*<ITypeConstraint2>*/ typeConstraintList;
			if (storedTcs instanceof ITypeConstraint2) {
				// CV only used in one TC so far:
				typeConstraintList= new ArrayList(2);
				typeConstraintList.add(storedTcs);
				typeConstraintList.add(typeConstraint);
				fConstraintVariables.put(storedCv, typeConstraintList);
				registerElementCVWithTC(cv, typeConstraint);
			} else {
				// CV already used in multiple or zero TCs:
				typeConstraintList= (ArrayList) storedTcs;
				typeConstraintList.add(typeConstraint);
			}
		}
	}
	
	private void registerElementCVWithTC(ConstraintVariable2 cv, SimpleTypeConstraint2 typeConstraint) {
		if (cv instanceof CollectionElementVariable2) {
			ConstraintVariable2 elementCv= ((CollectionElementVariable2) cv).getElementVariable();
			ConstraintVariable2 storedElementCv= (ConstraintVariable2) fConstraintVariables.getKey(elementCv);
			registerCvWithTc(storedElementCv, elementCv, typeConstraint);
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

//	public ExpressionVariable2 makeExpressionVariable(Expression expression) {
//		ITypeBinding typeBinding= expression.resolveTypeBinding();
//		if (filterConstraintVariableType(typeBinding))
//			return null;
//		TypeHandle typeHandle= fTypeHandleFactory.getTypeHandle(typeBinding);
//		ExpressionVariable2 expressionVariable= new ExpressionVariable2(typeHandle, expression);
//		if (fStoreToString)
//			expressionVariable.setData(ConstraintVariable2.TO_STRING, "[" + expression.toString() + "]"); //$NON-NLS-1$//$NON-NLS-2$
//		return expressionVariable;
//	}
	
	//TODO: pass in the ASTNode, to store a reference to the origin of a CV iff fStoreToString is true.

	public VariableVariable2 makeVariableVariable(IVariableBinding variableBinding) {
		ITypeBinding typeBinding= variableBinding.getType();
		if (filterConstraintVariableType(typeBinding))
			return null;
		TypeHandle typeHandle= fTypeHandleFactory.getTypeHandle(typeBinding);
		VariableVariable2 cv= new VariableVariable2(typeHandle, variableBinding);
		if (fStoreToString)
			cv.setData(ConstraintVariable2.TO_STRING, '[' + variableBinding.getName() + ']');
		return cv;
	}

	public VariableVariable2 makeDeclaredVariableVariable(IVariableBinding variableBinding, ICompilationUnit cu) {
		VariableVariable2 cv= makeVariableVariable(variableBinding);
		VariableVariable2 storedCv= (VariableVariable2) registerDeclaredVariable(cv, cu);
		if (! variableBinding.isField())
			fCuScopedConstraintVariables.add(storedCv);
		return storedCv;
	}
	
	public TypeVariable2 makeTypeVariable(Type type) {
		ITypeBinding typeBinding= type.resolveBinding();
		if (filterConstraintVariableType(typeBinding))
			return null;
		TypeHandle typeHandle= fTypeHandleFactory.getTypeHandle(typeBinding);
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
		if (filterConstraintVariableType(typeBinding))
			return null;
		TypeHandle typeHandle= fTypeHandleFactory.getTypeHandle(typeBinding);
		ICompilationUnit cu= RefactoringASTParser.getCompilationUnit(expression);
		CompilationUnitRange range= new CompilationUnitRange(cu, expression);
		TypeVariable2 typeVariable= new TypeVariable2(typeHandle, range);
		if (fStoreToString)
			typeVariable.setData(ConstraintVariable2.TO_STRING, expression.toString());
		return typeVariable;
	}
	
	public ParameterTypeVariable2 makeParameterTypeVariable(IMethodBinding methodBinding, int parameterIndex) {
		ITypeBinding typeBinding= methodBinding.getParameterTypes() [parameterIndex];
		if (filterConstraintVariableType(typeBinding))
			return null;
		TypeHandle typeHandle= fTypeHandleFactory.getTypeHandle(typeBinding);
		ParameterTypeVariable2 cv= new ParameterTypeVariable2(typeHandle, parameterIndex, methodBinding);
		if (fStoreToString)
			cv.setData(ConstraintVariable2.TO_STRING, "[Parameter(" + parameterIndex + "," + Bindings.asString(methodBinding) + ")]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return cv;
	}
	
	/**
	 * Make a ParameterTypeVariable2 from a method declaration.
	 * The constraint variable is always stored if it passes the type filter.
	 * @param methodBinding
	 * @param parameterIndex
	 * @param cu
	 * @return the ParameterTypeVariable2, or <code>null</code> 
	 */
	public ParameterTypeVariable2 makeDeclaredParameterTypeVariable(IMethodBinding methodBinding, int parameterIndex, ICompilationUnit cu) {
		ParameterTypeVariable2 cv= makeParameterTypeVariable(methodBinding, parameterIndex);
		ParameterTypeVariable2 storedCv= (ParameterTypeVariable2) registerDeclaredVariable(cv, cu);
		if (methodBinding.getDeclaringClass().isLocal())
			fCuScopedConstraintVariables.add(storedCv);
		return storedCv;
	}

	private IDeclaredConstraintVariable registerDeclaredVariable(IDeclaredConstraintVariable cv, ICompilationUnit unit) {
		if (cv == null)
			return null;
		
		IDeclaredConstraintVariable storedCv= (IDeclaredConstraintVariable) fConstraintVariables.getKey(cv);
		if (storedCv == null) {
			storedCv= cv;
			fConstraintVariables.put(cv, new ArrayList(0));
			fNewConstraintVariables.add(cv);
		}
		storedCv.setCompilationUnit(unit);
		return storedCv;
	}

	public ReturnTypeVariable2 makeReturnTypeVariable(IMethodBinding methodBinding) {
		ITypeBinding typeBinding= methodBinding.getReturnType();
		if (filterConstraintVariableType(typeBinding))
			return null;
		TypeHandle returnTypeHandle= fTypeHandleFactory.getTypeHandle(typeBinding);
		ReturnTypeVariable2 cv= new ReturnTypeVariable2(returnTypeHandle, methodBinding);
		if (fStoreToString)
			cv.setData(ConstraintVariable2.TO_STRING, "[ReturnType(" + Bindings.asString(methodBinding) + ")]"); //$NON-NLS-1$ //$NON-NLS-2$
		return cv;
	}

	public ReturnTypeVariable2 makeDeclaredReturnTypeVariable(IMethodBinding methodBinding, ICompilationUnit unit) {
		ReturnTypeVariable2 cv= makeReturnTypeVariable(methodBinding);
		ReturnTypeVariable2 storedCv= (ReturnTypeVariable2) registerDeclaredVariable(cv, unit);
		if (methodBinding.getDeclaringClass().isLocal())
			fCuScopedConstraintVariables.add(storedCv);
		return storedCv;
	}
	
//	public DeclaringTypeVariable2 makeDeclaringTypeVariable(IMethodBinding methodBinding) {
//		TypeHandle declaringTypeHandle= fTypeHandleFactory.getTypeHandle(methodBinding.getDeclaringClass());
//		return makeDeclaringTypeVariable(declaringTypeHandle, methodBinding);
//	}
//	
//	public DeclaringTypeVariable2 makeDeclaringTypeVariable(IVariableBinding fieldBinding) {
//		Assert.isTrue(fieldBinding.isField());
//		TypeHandle declaringTypeHandle= fTypeHandleFactory.getTypeHandle(fieldBinding.getDeclaringClass());
//		return makeDeclaringTypeVariable(declaringTypeHandle, fieldBinding);
//	}
//	
//	private DeclaringTypeVariable2 makeDeclaringTypeVariable(TypeHandle declaringTypeHandle, IBinding memberBinding) {
//		DeclaringTypeVariable2 cv= new DeclaringTypeVariable2(declaringTypeHandle, memberBinding);
//		return cv;
//	}
	
	public PlainTypeVariable2 makePlainTypeVariable(ITypeBinding typeBinding) {
		TypeHandle typeHandle= fTypeHandleFactory.getTypeHandle(typeBinding);
		PlainTypeVariable2 cv= new PlainTypeVariable2(typeHandle);
		return cv;
	}
	
	
	//TODO: move down to AugmentRawContainerClientsTCFactory?
	public ConstraintVariable2 makeElementVariable(ConstraintVariable2 expressionCv) {
		ConstraintVariable2 cv= new CollectionElementVariable2(expressionCv);
		if (fStoreToString)
			cv.setData(ConstraintVariable2.TO_STRING, "Elem[" + expressionCv.toString() + "]"); //$NON-NLS-1$ //$NON-NLS-2$
		return cv;
	}

}
