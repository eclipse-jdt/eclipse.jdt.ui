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
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.CompilationUnitRange;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;


public class AugmentRawContainerClientsTCModel {
	
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

	private static final String COLLECTION_ELEMENT= "CollectionElement"; //$NON-NLS-1$
	private static final String USED_IN= "UsedIn"; //$NON-NLS-1$
	
	protected static boolean fStoreToString= DEBUG;
	
	/**
	 * Map from {@link ConstraintVariable2} to
	 * <ul>
	 * <li>{@link ITypeConstraint2}, or</li>
	 * <li>{@link List}&lt;{@link ITypeConstraint2}&gt;</li>
	 * </ul>
	 */
	private CustomHashtable/*<ConstraintVariable2, Object>*/ fConstraintVariables;
	private CustomHashtable/*<ITypeConstraint2, NULL>*/ fTypeConstraints;
	private HashSet/*<EquivalenceRepresentative>*/ fEquivalenceRepresentatives;
	private Collection/*CastVariable2*/ fCastVariables;
	
	private HashSet fCuScopedConstraintVariables;
	
	private Collection fNewTypeConstraints;
	private Collection fNewConstraintVariables; //TODO: remove?
	
	protected final ITypeBinding fCollection;
	protected final ITypeBinding fList;
	protected final ITypeBinding fIterator;
	protected final ITypeBinding fObject;
	protected final ITypeBinding fPrimitiveInt;
	protected final ITypeBinding fPrimitiveBoolean;

	public AugmentRawContainerClientsTCModel(IJavaProject project) {
		fTypeConstraints= new CustomHashtable(new TypeConstraintComparer());
		fConstraintVariables= new CustomHashtable(new ConstraintVariableComparer());
		fEquivalenceRepresentatives= new HashSet();
		fCastVariables= new ArrayList();
		
		fCuScopedConstraintVariables= new HashSet();
		fNewTypeConstraints= new ArrayList();
		fNewConstraintVariables= new ArrayList();
		
		ASTParser parser= ASTParser.newParser(AST.JLS3);
		
		String source= "class X {java.util.Collection coll; java.util.Iterator iter; java.util.List list;}"; //$NON-NLS-1$
		parser.setSource(source.toCharArray());
		parser.setUnitName("X.java"); //$NON-NLS-1$
		parser.setProject(project);
		parser.setResolveBindings(true);
		CompilationUnit unit= (CompilationUnit) parser.createAST(null);
		TypeDeclaration type= (TypeDeclaration) unit.types().get(0);
		List typeBodyDeclarations= type.bodyDeclarations();
		//TODO: make sure this is in the compiler loop!
		fCollection= ((FieldDeclaration) typeBodyDeclarations.get(0)).getType().resolveBinding();
		fIterator= ((FieldDeclaration) typeBodyDeclarations.get(1)).getType().resolveBinding();
		fList= ((FieldDeclaration) typeBodyDeclarations.get(2)).getType().resolveBinding();
		fObject= unit.getAST().resolveWellKnownType("java.lang.Object");
		fPrimitiveInt= unit.getAST().resolveWellKnownType("int");
		fPrimitiveBoolean= unit.getAST().resolveWellKnownType("boolean");
	}
	
	/**
	 * @param typeBinding the type binding to check
	 * @return whether the constraint variable should <em>not</em> be created
	 */
	public boolean filterConstraintVariableType(ITypeBinding typeBinding) {
		//TODO: filter makeDeclaringTypeVariable, since that's not used?
		//-> would need to adapt create...Constraint methods to deal with null
		
		return typeBinding.isPrimitive();
//		return TypeRules.canAssign(fCollectionBinding, typeBinding);
	}
	
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
		if ((cv1 == null || cv2 == null))
			return false;
		
		if (cv1 == cv2 || cv1.isSameAs(cv2))
			Assert.isTrue(false);
		
		if (cv1 instanceof CollectionElementVariable2 || cv2 instanceof CollectionElementVariable2)
			return true;
		
		//TODO: who needs these?
		if (cv1 instanceof TypeConstraintVariable2)
			if (TypeBindings.isSuperType(fCollection, ((TypeConstraintVariable2) cv1).getTypeBinding()))
				return true;
				
		if (cv2 instanceof TypeConstraintVariable2)
			if (TypeBindings.isSuperType(fCollection, ((TypeConstraintVariable2) cv2).getTypeBinding()))
				return true;
		
		return false;
	}
	
	public ITypeBinding getCollectionType() {
		return fCollection;
	}
	
	public ITypeBinding getIteratorType() {
		return fIterator;
	}
	
	public ITypeBinding getListType() {
		return fList;
	}
	
	public ITypeBinding getObjectType() {
		return fObject;
	}
	
	public ITypeBinding getPrimitiveBooleanType() {
		return fPrimitiveBoolean;
	}

	public ITypeBinding getPrimitiveIntType() {
		return fPrimitiveInt;
	}

	/**
	 * @param cv
	 * @return a List of ITypeConstraint2s where cv is used
	 */
	public List/*<ITypeConstraint2>*/ getUsedIn(ConstraintVariable2 cv) {
		Object usedIn= cv.getData(USED_IN);
		if (usedIn == null)
			return Collections.EMPTY_LIST;
		else if (usedIn instanceof ArrayList)
			return Collections.unmodifiableList((ArrayList) usedIn);
		else
			return Collections.singletonList(usedIn);
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
			if (getUsedIn(cv).size() == 0 && getElementVariable(cv) == null)
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
	
	public EquivalenceRepresentative[] getEquivalenceRepresentatives() {
		return (EquivalenceRepresentative[]) fEquivalenceRepresentatives.toArray(new EquivalenceRepresentative[fEquivalenceRepresentatives.size()]);
	}
	
	public CastVariable2[] getCastVariables() {
		return (CastVariable2[]) fCastVariables.toArray(new CastVariable2[fCastVariables.size()]);
	}
	
//	public ConstraintVariable2[] getNewConstraintVariables() {
//		return (ConstraintVariable2[]) fNewConstraintVariables.toArray(new ConstraintVariable2[fNewConstraintVariables.size()]);
//	}
//	
	public ITypeConstraint2[] getNewTypeConstraints() {
		return (ITypeConstraint2[]) fNewTypeConstraints.toArray(new ITypeConstraint2[fNewTypeConstraints.size()]);
	}
	
//	public Set getAllTypeConstraints() {
//		fTypeConstraints.keys();
//	}
	
	
	/**
	 * Controls calculation and storage of information for more readable toString() messages.
	 * <p><em>Warning: This method is for testing purposes only and should not be called except from unit tests.</em></p>
	 * 
	 * @param store <code>true</code> iff information for toString() should be stored 
	 */
	public static void setStoreToString(boolean store) {
		fStoreToString= store;
	}
	
	public void createSubtypeConstraint(ConstraintVariable2 v1, ConstraintVariable2 v2){
		createSimpleTypeConstraint(v1, v2, ConstraintOperator2.createSubTypeOperator());
	}
	
//	public ITypeConstraint2[] createStrictSubtypeConstraint(ConstraintVariable2 v1, ConstraintVariable2 v2){
//		return createSimpleTypeConstraint(v1, v2, ConstraintOperator2.createStrictSubtypeOperator());
//	}
//	
	
	protected void createSimpleTypeConstraint(ConstraintVariable2 cv1, ConstraintVariable2 cv2, ConstraintOperator2 operator) {
		if (! keep(cv1, cv2, operator))
			return;
		
		ConstraintVariable2 storedCv1= storedCv(cv1);
		ConstraintVariable2 storedCv2= storedCv(cv2);
		SimpleTypeConstraint2 typeConstraint= new SimpleTypeConstraint2(storedCv1, storedCv2, operator);
		
		Object storedTc= fTypeConstraints.getKey(typeConstraint);
		if (storedTc == null) {
			fTypeConstraints.put(typeConstraint, NULL);
			fNewTypeConstraints.add(typeConstraint);
		} else {
			typeConstraint= (SimpleTypeConstraint2) storedTc;
		}
		
		registerCvWithTc(storedCv1, typeConstraint);
		registerCvWithTc(storedCv2, typeConstraint);
	}

	private ConstraintVariable2 storedCv(ConstraintVariable2 cv) {
		//TODO: should optimize 'stored()' in better CustomHashSet
		Object stored= fConstraintVariables.getKey(cv);
		if (stored == null) {
			fConstraintVariables.put(cv, NULL);
			fNewConstraintVariables.add(cv);
			return cv;
		} else {
			return (ConstraintVariable2) stored;
		}
	}

//	private void registerCvWithTc(ConstraintVariable2 storedCv, ConstraintVariable2 cv, ITypeConstraint2 typeConstraint) {
//		//TODO: special handling for CollectionElementVariable2
//		if (storedCv == null) {
//			// new CV -> directly store TC:
//			fConstraintVariables.put(cv, typeConstraint);
//			fNewConstraintVariables.add(cv);
//			registerElementCVWithTC(cv, typeConstraint);
//		} else {
//			// existing stored CV:
//			//TODO: could avoid call to get() if there was a method HashMapEntry getEntry(Object key) 
//			Object storedTcs= fConstraintVariables.get(storedCv);
//			ArrayList/*<ITypeConstraint2>*/ typeConstraintList;
//			if (storedTcs instanceof ITypeConstraint2) {
//				// CV only used in one TC so far:
//				typeConstraintList= new ArrayList(2);
//				typeConstraintList.add(storedTcs);
//				typeConstraintList.add(typeConstraint);
//				fConstraintVariables.put(storedCv, typeConstraintList);
//				registerElementCVWithTC(cv, typeConstraint);
//			} else {
//				// CV already used in multiple or zero TCs:
//				typeConstraintList= (ArrayList) storedTcs;
//				typeConstraintList.add(typeConstraint);
//			}
//		}
//	}
	
	private void registerCvWithTc(ConstraintVariable2 storedCv, ITypeConstraint2 typeConstraint) {
		//TODO: special handling for CollectionElementVariable2?
		Object usedIn= storedCv.getData(USED_IN);
		if (usedIn == null) {
			storedCv.setData(USED_IN, typeConstraint);
		} else if (usedIn instanceof ArrayList) {
			ArrayList usedInList= (ArrayList) usedIn;
			usedInList.add(typeConstraint);
		} else {
			ArrayList usedInList= new ArrayList(2);
			usedInList.add(usedIn);
			usedInList.add(typeConstraint);
			storedCv.setData(USED_IN, usedInList);
		}
	}
	
	public void createEqualsConstraint(CollectionElementVariable2 leftElement, CollectionElementVariable2 rightElement) {
		if (leftElement == null || rightElement == null)
			return;
		
		EquivalenceRepresentative leftRep= leftElement.getRepresentative();
		EquivalenceRepresentative rightRep= rightElement.getRepresentative();
		if (leftRep == null) {
			if (rightRep == null) {
				EquivalenceRepresentative rep= new EquivalenceRepresentative(leftElement, rightElement);
				fEquivalenceRepresentatives.add(rep);
				leftElement.setRepresentative(rep);
				rightElement.setRepresentative(rep);
			} else {
				rightRep.add(leftElement);
				leftElement.setRepresentative(rightRep);
			}
		} else {
			if (rightRep == null) {
				leftRep.add(rightElement);
				rightElement.setRepresentative(leftRep);
			} else {
				CollectionElementVariable2[] rightElements= rightRep.getElements();
				leftRep.addAll(rightElements);
				for (int i= 0; i < rightElements.length; i++)
					rightElements[i].setRepresentative(leftRep);
				fEquivalenceRepresentatives.remove(rightRep);
			}
		}
	}
	
	public VariableVariable2 makeVariableVariable(IVariableBinding variableBinding) {
		ITypeBinding typeBinding= variableBinding.getType();
		if (filterConstraintVariableType(typeBinding))
			return null;
		VariableVariable2 cv= new VariableVariable2(typeBinding, variableBinding);
		cv= (VariableVariable2) storedCv(cv);
		CollectionElementVariable2 elementVariable= makeElementVariable(cv);
		if (fStoreToString)
			cv.setData(ConstraintVariable2.TO_STRING, '[' + variableBinding.getName() + ']');
		return cv;
	}

	public VariableVariable2 makeDeclaredVariableVariable(IVariableBinding variableBinding, ICompilationUnit cu) {
		VariableVariable2 cv= makeVariableVariable(variableBinding);
		if (cv == null)
			return null;
		VariableVariable2 storedCv= (VariableVariable2) registerDeclaredVariable(cv, cu);
		if (! variableBinding.isField())
			fCuScopedConstraintVariables.add(storedCv);
		return storedCv;
	}
	
	public TypeVariable2 makeTypeVariable(Type type) {
		ITypeBinding typeBinding= type.resolveBinding();
		if (filterConstraintVariableType(typeBinding))
			return null;
		ICompilationUnit cu= RefactoringASTParser.getCompilationUnit(type);
		CompilationUnitRange range= new CompilationUnitRange(cu, type);
		TypeVariable2 typeVariable= new TypeVariable2(typeBinding, range);
		typeVariable= (TypeVariable2) storedCv(typeVariable); //TODO: Should not use storedCv(..) here!
		fCuScopedConstraintVariables.add(typeVariable);
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
		ICompilationUnit cu= RefactoringASTParser.getCompilationUnit(expression);
		CompilationUnitRange range= new CompilationUnitRange(cu, expression);
		TypeVariable2 typeVariable= new TypeVariable2(typeBinding, range);
		fCuScopedConstraintVariables.add(typeVariable);
		if (fStoreToString)
			typeVariable.setData(ConstraintVariable2.TO_STRING, expression.toString());
		return typeVariable;
	}
	
	public ParameterTypeVariable2 makeParameterTypeVariable(IMethodBinding methodBinding, int parameterIndex) {
		ITypeBinding typeBinding= methodBinding.getParameterTypes() [parameterIndex];
		if (filterConstraintVariableType(typeBinding))
			return null;
		ParameterTypeVariable2 cv= new ParameterTypeVariable2(typeBinding, parameterIndex, methodBinding);
		cv= (ParameterTypeVariable2) storedCv(cv); //TODO: Should not use storedCv(..) here!
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
		//TODO: spread such checks:
		if (methodBinding.getDeclaringClass().isLocal() || Modifier.isPrivate(methodBinding.getModifiers()))
			fCuScopedConstraintVariables.add(storedCv);
		return storedCv;
	}

	private IDeclaredConstraintVariable registerDeclaredVariable(IDeclaredConstraintVariable cv, ICompilationUnit unit) {
		if (cv == null)
			return null;
		
		IDeclaredConstraintVariable storedCv= (IDeclaredConstraintVariable) fConstraintVariables.getKey(cv);
		if (storedCv == null) {
			//TODO: should always be the case now
			storedCv= cv;
			fNewConstraintVariables.add(cv);
		}
		storedCv.setCompilationUnit(unit);
		return storedCv;
	}

	public ReturnTypeVariable2 makeReturnTypeVariable(IMethodBinding methodBinding) {
		ITypeBinding returnTypeBinding= methodBinding.getReturnType();
		if (filterConstraintVariableType(returnTypeBinding))
			return null;
		ReturnTypeVariable2 cv= new ReturnTypeVariable2(returnTypeBinding, methodBinding);
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
		if (filterConstraintVariableType(typeBinding))
			return null;
		PlainTypeVariable2 cv= new PlainTypeVariable2(typeBinding);
		cv= (PlainTypeVariable2) storedCv(cv);
		return cv;
	}
	
	public CollectionElementVariable2 makeElementVariable(TypeConstraintVariable2 expressionCv) {
		//TODO: unhack!!!
		CollectionElementVariable2 storedElementVariable= getElementVariable(expressionCv);
		if (storedElementVariable != null)
			return storedElementVariable;
		
		if (isACollectionType(expressionCv.getTypeBinding())) {
			CollectionElementVariable2 cv= new CollectionElementVariable2(expressionCv);
			cv= (CollectionElementVariable2) storedCv(cv); //TODO: Should not use storedCv(..) here!
			setElementVariable(expressionCv, cv);
//			if (fStoreToString)
//				cv.setData(ConstraintVariable2.TO_STRING, "Elem[" + expressionCv.toString() + "]"); //$NON-NLS-1$ //$NON-NLS-2$
			return cv;
		} else {
			return null;
		}
	}

	public boolean isACollectionType(ITypeBinding typeBinding) {
		return TypeBindings.isSuperType(getCollectionType(), typeBinding)
				|| TypeBindings.isSuperType(getIteratorType(), typeBinding);
		//TODO: Enumeration, ...
	}

	public void makeCastVariable(CastExpression castExpression, CollectionElementVariable2 expressionCv) {
		ITypeBinding typeBinding= castExpression.resolveTypeBinding();
		ICompilationUnit cu= RefactoringASTParser.getCompilationUnit(castExpression);
		CompilationUnitRange range= new CompilationUnitRange(cu, castExpression);
		CastVariable2 castCv= new CastVariable2(typeBinding, range, expressionCv);
		fCastVariables.add(castCv);
	}
	
	private void setElementVariable(ConstraintVariable2 typeVariable, CollectionElementVariable2 elementVariable) {
		typeVariable.setData(COLLECTION_ELEMENT, elementVariable);
	}
	
	public CollectionElementVariable2 getElementVariable(ConstraintVariable2 constraintVariable) {
		return (CollectionElementVariable2) constraintVariable.getData(COLLECTION_ELEMENT);
	}

}
