/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.corext.refactoring.generics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.Platform;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Type;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.CompilationUnitRange;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.types.GenericType;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.types.ParameterizedType;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.types.TType;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.types.TypeEnvironment;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.types.TypeVariable;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.CastVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.CollectionElementVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ConstraintVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ITypeConstraint2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ImmutableTypeVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.IndependentTypeVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ParameterTypeVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ParameterizedTypeVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ReturnTypeVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.SubTypeConstraint2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.TypeEquivalenceSet;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.TypeVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.VariableVariable2;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;

import org.eclipse.jdt.internal.ui.JavaPlugin;


public class InferTypeArgumentsTCModel {
	
	protected static final boolean DEBUG= Boolean.valueOf(Platform.getDebugOption("org.eclipse.jdt.ui/debug/TypeConstraints")).booleanValue(); //$NON-NLS-1$

	private static final String INDEXED_COLLECTION_ELEMENTS= "IndexedCollectionElements"; //$NON-NLS-1$
	private static final String USED_IN= "UsedIn"; //$NON-NLS-1$
	private static final Map EMPTY_COLLECTION_ELEMENT_VARIABLES_MAP= Collections.EMPTY_MAP;
	
	protected static boolean fStoreToString= DEBUG;
	
	/**
	 * Map from a {@link ConstraintVariable2} to itself.
	 */
	private HashMap/*<ConstraintVariable2, ConstraintVariable2>*/ fConstraintVariables;
	/**
	 * Map from a {@link ITypeConstraint2} to itself.
	 */
	private HashMap/*<ITypeConstraint2, ITypeConstraint2>*/ fTypeConstraints;
	private Collection/*CastVariable2*/ fCastVariables;
	
	private HashSet fCuScopedConstraintVariables;
	
	private TypeEnvironment fTypeEnvironment;
	
	public InferTypeArgumentsTCModel() {
		fTypeConstraints= new HashMap();
		fConstraintVariables= new HashMap();
		fCastVariables= new ArrayList();
		
		fCuScopedConstraintVariables= new HashSet();
		
		fTypeEnvironment= new TypeEnvironment(true);
	}
	
	/**
	 * Allows for avoiding the creation of SimpleTypeConstraints based on properties of
	 * their constituent ConstraintVariables and ConstraintOperators. Can be used to e.g. 
	 * avoid creation of constraints for assignments between built-in types.
	 * 
	 * @param cv1 
	 * @param cv2
	 * @return <code>true</code> iff the type constraint should really be created
	 */
	protected boolean keep(ConstraintVariable2 cv1, ConstraintVariable2 cv2) {
		if ((cv1 == null || cv2 == null))
			return false;
		
		if (cv1.equals(cv2)) {
			if (cv1 == cv2)
				return false;
			else
				Assert.isTrue(false);
		}
		
		if (cv1 instanceof CollectionElementVariable2 || cv2 instanceof CollectionElementVariable2)
			return true;
		
		if (cv1 instanceof IndependentTypeVariable2 || cv2 instanceof IndependentTypeVariable2)
			return true;

		if (isAGenericType(cv1.getType()))
			return true;

		if (isAGenericType(cv2.getType()))
			return true;
		
		return false;
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
	
	public void newCu() {
		pruneUnusedCuScopedCvs();
		fCuScopedConstraintVariables.clear();
	}
	
	private void pruneUnusedCuScopedCvs() {
		for (Iterator iter= fCuScopedConstraintVariables.iterator(); iter.hasNext();) {
			ConstraintVariable2 cv= (ConstraintVariable2) iter.next();
			//TODO: also prune if all element variables are unused; also prune element variables then
			if (getUsedIn(cv).size() == 0 && getElementVariables(cv).size() == 0)
				fConstraintVariables.remove(cv);
		}
	}

	public ConstraintVariable2[] getAllConstraintVariables() {
		ConstraintVariable2[] result= new ConstraintVariable2[fConstraintVariables.size()];
		int i= 0;
		for (Iterator iter= fConstraintVariables.keySet().iterator(); iter.hasNext(); i++)
			result[i]= (ConstraintVariable2) iter.next();
		return result;
	}
	
	public ITypeConstraint2[] getAllTypeConstraints() {
		Set typeConstraints= fTypeConstraints.keySet();
		return (ITypeConstraint2[]) typeConstraints.toArray(new ITypeConstraint2[typeConstraints.size()]);
	}
	
	public CastVariable2[] getCastVariables() {
		return (CastVariable2[]) fCastVariables.toArray(new CastVariable2[fCastVariables.size()]);
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
	
	public void createSubtypeConstraint(ConstraintVariable2 cv1, ConstraintVariable2 cv2) {
		if (! keep(cv1, cv2))
			return;
		
		ConstraintVariable2 storedCv1= storedCv(cv1);
		ConstraintVariable2 storedCv2= storedCv(cv2);
		ITypeConstraint2 typeConstraint= new SubTypeConstraint2(storedCv1, storedCv2);
		
		Object storedTc= fTypeConstraints.get(typeConstraint);
		if (storedTc == null) {
			fTypeConstraints.put(typeConstraint, typeConstraint);
		} else {
			typeConstraint= (ITypeConstraint2) storedTc;
		}
		
		registerCvWithTc(storedCv1, typeConstraint);
		registerCvWithTc(storedCv2, typeConstraint);
	}

	private ConstraintVariable2 storedCv(ConstraintVariable2 cv) {
		Object stored= fConstraintVariables.get(cv);
		if (stored == null) {
			fConstraintVariables.put(cv, cv);
			return cv;
		} else {
			return (ConstraintVariable2) stored;
		}
	}
	
	private void registerCvWithTc(ConstraintVariable2 storedCv, ITypeConstraint2 typeConstraint) {
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
	
	public void createEqualsConstraint(ConstraintVariable2 leftElement, ConstraintVariable2 rightElement) {
		if (leftElement == null || rightElement == null)
			return;
		
		TypeEquivalenceSet leftSet= leftElement.getTypeEquivalenceSet();
		TypeEquivalenceSet rightSet= rightElement.getTypeEquivalenceSet();
		if (leftSet == null) {
			if (rightSet == null) {
				TypeEquivalenceSet set= new TypeEquivalenceSet(leftElement, rightElement);
				leftElement.setTypeEquivalenceSet(set);
				rightElement.setTypeEquivalenceSet(set);
			} else {
				rightSet.add(leftElement);
				leftElement.setTypeEquivalenceSet(rightSet);
			}
		} else {
			if (rightSet == null) {
				leftSet.add(rightElement);
				rightElement.setTypeEquivalenceSet(leftSet);
			} else if (leftSet == rightSet) {
				return;
			} else {
				ConstraintVariable2[] cvs= rightSet.getContributingVariables();
				leftSet.addAll(cvs);
				for (int i= 0; i < cvs.length; i++)
					cvs[i].setTypeEquivalenceSet(leftSet);
			}
		}
	}
	
	private ITypeBinding getBoxedType(ITypeBinding typeBinding, ICompilationUnit cu) {
		if (! typeBinding.isPrimitive())
			return typeBinding;
		
		String primitiveName= typeBinding.getName();
		if ("void".equals(primitiveName)) //$NON-NLS-1$ //$NON-NLS-2$
			return null;
		
		//FIXME: workaround for bug 86779:
		// return unit.getAST().resolveWellKnownType(problem.getArguments()[1]);
		try {
			IJavaProject javaProject= cu.getJavaProject();
			String wrapperType= getBoxedTypeName(primitiveName);
			IType type= javaProject.findType(wrapperType);
			ASTParser parser= ASTParser.newParser(AST.JLS3);
			parser.setProject(javaProject);
			IBinding[] bindings= parser.createBindings(new IJavaElement[] {type} , null);
			return (ITypeBinding) bindings[0];
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
			return typeBinding;
		}
	}

	private String getBoxedTypeName(String primitiveName) {
		if ("long".equals(primitiveName)) //$NON-NLS-1$
			return "java.lang.Long"; //$NON-NLS-1$
		
		else if ("int".equals(primitiveName)) //$NON-NLS-1$
			return "java.lang.Integer"; //$NON-NLS-1$
		
		else if ("short".equals(primitiveName)) //$NON-NLS-1$
			return "java.lang.Short"; //$NON-NLS-1$
		
		else if ("char".equals(primitiveName)) //$NON-NLS-1$
			return "java.lang.Character"; //$NON-NLS-1$
		
		else if ("byte".equals(primitiveName)) //$NON-NLS-1$
			return "java.lang.Byte"; //$NON-NLS-1$
		
		else if ("boolean".equals(primitiveName)) //$NON-NLS-1$
			return "java.lang.Boolean"; //$NON-NLS-1$
		
		else if ("float".equals(primitiveName)) //$NON-NLS-1$
			return "java.lang.Float"; //$NON-NLS-1$
		
		else if ("double".equals(primitiveName)) //$NON-NLS-1$
			return "java.lang.Double"; //$NON-NLS-1$
		
		else 
			return null;
	}
	
	public VariableVariable2 makeVariableVariable(IVariableBinding variableBinding, ICompilationUnit cu) {
		ITypeBinding typeBinding= getBoxedType(variableBinding.getType(), cu);
		if (typeBinding == null)
			return null;
		VariableVariable2 cv= new VariableVariable2(fTypeEnvironment.create(typeBinding), variableBinding);
		VariableVariable2 storedCv= (VariableVariable2) storedCv(cv);
		if (storedCv == cv) {
			if (! variableBinding.isField())
				fCuScopedConstraintVariables.add(storedCv);
			makeElementVariables(storedCv, typeBinding);
			if (fStoreToString)
				storedCv.setData(ConstraintVariable2.TO_STRING, '[' + variableBinding.getName() + ']');
		}
		return storedCv;
	}

	public VariableVariable2 makeDeclaredVariableVariable(IVariableBinding variableBinding, ICompilationUnit cu) {
		VariableVariable2 cv= makeVariableVariable(variableBinding, cu);
		if (cv == null)
			return null;
		cv.setCompilationUnit(cu);
		return cv;
	}
	
	public TypeVariable2 makeTypeVariable(Type type) {
		ICompilationUnit cu= RefactoringASTParser.getCompilationUnit(type);
		ITypeBinding typeBinding= getBoxedType(type.resolveBinding(), cu);
		if (typeBinding == null)
			return null;
		CompilationUnitRange range= new CompilationUnitRange(cu, type);
		TypeVariable2 typeVariable= new TypeVariable2(fTypeEnvironment.create(typeBinding), range);
		TypeVariable2 storedCv= (TypeVariable2) storedCv(typeVariable);
		if (storedCv == typeVariable) {
			fCuScopedConstraintVariables.add(storedCv);
			if (isAGenericType(typeBinding))
				makeElementVariables(storedCv, typeBinding);
			if (fStoreToString)
				storedCv.setData(ConstraintVariable2.TO_STRING, type.toString());
		}
		return storedCv;
	}

	public IndependentTypeVariable2 makeIndependentTypeVariable(ITypeBinding typeBinding) {
		//TODO: prune if unused!
		Assert.isTrue(! typeBinding.isPrimitive());
		IndependentTypeVariable2 cv= new IndependentTypeVariable2(fTypeEnvironment.create(typeBinding));
		IndependentTypeVariable2 storedCv= (IndependentTypeVariable2) storedCv(cv);
		if (cv == storedCv) {
//			if (isAGenericType(typeBinding)) // would lead to infinite recursion!
//				makeElementVariables(storedCv, typeBinding);
			if (fStoreToString)
				storedCv.setData(ConstraintVariable2.TO_STRING, "IndependentType(" + Bindings.asString(typeBinding) + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return storedCv;
	}
		
	public IndependentTypeVariable2 makeIndependentTypeVariable(TypeVariable type) {
		//TODO: prune if unused!
		IndependentTypeVariable2 cv= new IndependentTypeVariable2(type);
		IndependentTypeVariable2 storedCv= (IndependentTypeVariable2) storedCv(cv);
		if (cv == storedCv) {
//			if (isAGenericType(typeBinding)) // would lead to infinite recursion!
//				makeElementVariables(storedCv, typeBinding);
			if (fStoreToString)
				storedCv.setData(ConstraintVariable2.TO_STRING, "IndependentType(" + type.getPrettySignature() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return storedCv;
	}
		
	public ParameterizedTypeVariable2 makeParameterizedTypeVariable(ITypeBinding typeBinding) {
		//TODO: prune if unused!
		ParameterizedTypeVariable2 cv= new ParameterizedTypeVariable2(fTypeEnvironment.create(typeBinding));
		ParameterizedTypeVariable2 storedCv= (ParameterizedTypeVariable2) storedCv(cv);
		if (cv == storedCv) {
			makeElementVariables(storedCv, typeBinding);
			if (fStoreToString)
				storedCv.setData(ConstraintVariable2.TO_STRING, "ParameterizedType(" + Bindings.asString(typeBinding) + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return storedCv;
	}
		
	public ParameterTypeVariable2 makeParameterTypeVariable(IMethodBinding methodBinding, int parameterIndex, ICompilationUnit cu) {
		ITypeBinding typeBinding= getBoxedType(methodBinding.getParameterTypes() [parameterIndex], cu);
		if (typeBinding == null)
			return null;
		
		ParameterTypeVariable2 cv= new ParameterTypeVariable2(
			fTypeEnvironment.create(typeBinding), parameterIndex, methodBinding);
		ParameterTypeVariable2 storedCv= (ParameterTypeVariable2) storedCv(cv);
		if (storedCv == cv) {
			if (methodBinding.getDeclaringClass().isLocal() || Modifier.isPrivate(methodBinding.getModifiers()))
				fCuScopedConstraintVariables.add(cv);
			makeElementVariables(storedCv, typeBinding);
			if (fStoreToString)
				storedCv.setData(ConstraintVariable2.TO_STRING, "[Parameter(" + parameterIndex + "," + Bindings.asString(methodBinding) + ")]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		return storedCv;
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
		ParameterTypeVariable2 cv= makeParameterTypeVariable(methodBinding, parameterIndex, cu);
		if (cv == null)
			return null;
		cv.setCompilationUnit(cu);
		return cv;
	}

	public ReturnTypeVariable2 makeReturnTypeVariable(IMethodBinding methodBinding, ICompilationUnit cu) {
		ITypeBinding returnTypeBinding= getBoxedType(methodBinding.getReturnType(), cu);
		if (returnTypeBinding == null)
			return null;
		
		ReturnTypeVariable2 cv= new ReturnTypeVariable2(fTypeEnvironment.create(returnTypeBinding), methodBinding);
		ReturnTypeVariable2 storedCv= (ReturnTypeVariable2) storedCv(cv);
		if (cv == storedCv) {
			makeElementVariables(storedCv, returnTypeBinding);
			if (fStoreToString)
				storedCv.setData(ConstraintVariable2.TO_STRING, "[ReturnType(" + Bindings.asString(methodBinding) + ")]"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return storedCv;
	}

	public ReturnTypeVariable2 makeDeclaredReturnTypeVariable(IMethodBinding methodBinding, ICompilationUnit unit) {
		ReturnTypeVariable2 cv= makeReturnTypeVariable(methodBinding, unit);
		if (cv == null)
			return null;
		
		cv.setCompilationUnit(unit);
		if (methodBinding.getDeclaringClass().isLocal())
			fCuScopedConstraintVariables.add(cv);
		return cv;
	}
	
	public ImmutableTypeVariable2 makeImmutableTypeVariable(ITypeBinding typeBinding, ICompilationUnit cu) {
		typeBinding= getBoxedType(typeBinding, cu);
		if (typeBinding == null)
			return null;
		
		ImmutableTypeVariable2 cv= new ImmutableTypeVariable2(fTypeEnvironment.create(typeBinding));
		cv= (ImmutableTypeVariable2) storedCv(cv);
		return cv;
	}
	
	public boolean isAGenericType(TType type) {
		return type.isGenericType()
				|| type.isParameterizedType()
				|| type.isRawType();
	}

	public boolean isAGenericType(ITypeBinding type) {
		return type.isGenericType()
				|| type.isParameterizedType()
				|| type.isRawType();
	}

	public CastVariable2 makeCastVariable(CastExpression castExpression, ConstraintVariable2 expressionCv) {
		ITypeBinding typeBinding= castExpression.resolveTypeBinding();
		ICompilationUnit cu= RefactoringASTParser.getCompilationUnit(castExpression);
		CompilationUnitRange range= new CompilationUnitRange(cu, castExpression);
		CastVariable2 castCv= new CastVariable2(fTypeEnvironment.create(typeBinding), range, expressionCv);
		fCastVariables.add(castCv);
		return castCv;
	}
	
	public TypeEnvironment getTypeEnvironment() {
		return fTypeEnvironment;
	}
	
	private void makeElementVariables(ConstraintVariable2 expressionCv, ITypeBinding typeBinding) {
		makeElementVariables(expressionCv, typeBinding, true);
	}
	
	/**
	 * Make element variables for type variables declared in typeBinding.
	 * 
	 * @param expressionCv the type constraint variable
	 * @param typeBinding the type binding to fetch type variables from
	 * @param isDeclaration <code>true</code> iff typeBinding is the base type of expressionCv
	 */
	private void makeElementVariables(ConstraintVariable2 expressionCv, ITypeBinding typeBinding, boolean isDeclaration) {
		//TODO: element variables for type variables of enclosing types and methods
		if (isAGenericType(typeBinding)) {
			typeBinding= typeBinding.getTypeDeclaration();
			ITypeBinding[] typeParameters= typeBinding.getTypeParameters();
			for (int i= 0; i < typeParameters.length; i++) {
				makeElementVariable(expressionCv, typeParameters[i], isDeclaration ? i : CollectionElementVariable2.NOT_DECLARED_TYPE_VARIABLE_INDEX);
				if (typeParameters[i].getTypeBounds().length != 0) {
					//TODO: create subtype constraints for bounds
				}
			}
		}
		
		ITypeBinding superclass= typeBinding.getSuperclass();
		if (superclass != null) {
			//TODO: don't create new CollectionElementVariables. Instead, reuse existing (add to map with new key)
			makeElementVariables(expressionCv, superclass, false);
			createTypeVariablesEqualityConstraints(expressionCv, superclass);
		}
		
		ITypeBinding[] interfaces= typeBinding.getInterfaces();
		for (int i= 0; i < interfaces.length; i++) {
			makeElementVariables(expressionCv, interfaces[i], false);
			createTypeVariablesEqualityConstraints(expressionCv, interfaces[i]);
		}
	}

	private void createTypeVariablesEqualityConstraints(ConstraintVariable2 expressionCv, ITypeBinding reference) {
		createTypeVariablesEqualityConstraints(expressionCv, EMPTY_COLLECTION_ELEMENT_VARIABLES_MAP, expressionCv, reference);
	}

	/**
	 * Create equality constraints between generic type variables of expressionCv and referenceCv.
	 * For example, the generic interface <code>java.lang.Iterable&lt;E&gt;</code> defines a method
	 * <code>Iterator&lt;E&gt; iterator()</code>. Given
	 * <ul>
	 *   <li>an expressionCv of a subtype of <code>Iterable</code>,</li>
	 *   <li>a referenceCv of a subtype of <code>Iterator</code>, and</li>
	 *   <li>a reference binding of the Iterable#iterator()'s return type (the parameterized type <code>Iterator&lt;E&gt;</code>),</li>
	 * </ul>
	 * this method creates an equality constraint between the type variable E in expressionCV and
	 * the type variable E in referenceCV.
	 * 
	 * @param expressionCv the type constraint variable of an expression
	 * @param methodTypeVariables 
	 * @param referenceCv the type constraint variable of a type reference
	 * @param reference the declared type reference
	 */
	public void createTypeVariablesEqualityConstraints(ConstraintVariable2 expressionCv, Map/*<String, IndependentTypeVariable2>*/ methodTypeVariables, ConstraintVariable2 referenceCv, ITypeBinding reference) {
		if (reference.isParameterizedType() || reference.isRawType()) {
			ITypeBinding[] referenceTypeArguments= reference.getTypeArguments();
			ITypeBinding[] referenceTypeParameters= reference.getTypeDeclaration().getTypeParameters();
			for (int i= 0; i < referenceTypeParameters.length; i++) {
				ITypeBinding referenceTypeParameter= referenceTypeParameters[i];
				ITypeBinding referenceTypeArgument;
				if (reference.isRawType())
					referenceTypeArgument= referenceTypeParameter.getErasure();
				else
					referenceTypeArgument= referenceTypeArguments[i];
				ConstraintVariable2 referenceTypeArgumentCv;
				if (referenceTypeArgument.isTypeVariable()) {
					referenceTypeArgumentCv= (ConstraintVariable2) methodTypeVariables.get(referenceTypeArgument.getKey());
					if (referenceTypeArgumentCv == null)
						referenceTypeArgumentCv= getElementVariable(expressionCv, referenceTypeArgument);
				} else if (referenceTypeArgument.isWildcardType()) {
					referenceTypeArgumentCv= null; //TODO: make new WildcardTypeVariable, which is compatible to nothing 
				} else {
					referenceTypeArgumentCv= makeIndependentTypeVariable(referenceTypeParameter);
				}
				CollectionElementVariable2 referenceTypeParametersCv= getElementVariable(referenceCv, referenceTypeParameter);
				createEqualsConstraint(referenceTypeArgumentCv, referenceTypeParametersCv);
			}
		}
	}

	private CollectionElementVariable2 makeElementVariable(ConstraintVariable2 expressionCv, ITypeBinding typeVariable, int declarationTypeVariableIndex) {
		if (expressionCv == null)
			return null;
		
		CollectionElementVariable2 storedElementVariable= getElementVariable(expressionCv, typeVariable);
		if (storedElementVariable != null)
			return storedElementVariable;
		
		if (isAGenericType(expressionCv.getType())) {
			CollectionElementVariable2 cv= new CollectionElementVariable2(expressionCv, typeVariable, declarationTypeVariableIndex);
			cv= (CollectionElementVariable2) storedCv(cv);
			setElementVariable(expressionCv, cv, typeVariable);
//			if (fStoreToString)
//				cv.setData(ConstraintVariable2.TO_STRING, "Elem[" + expressionCv.toString() + "]"); //$NON-NLS-1$ //$NON-NLS-2$
			return cv;
		} else {
			return null;
		}
	}
	
	private void setElementVariable(ConstraintVariable2 typeConstraintVariable, CollectionElementVariable2 elementVariable, ITypeBinding typeVariable) {
		HashMap keyToElementVar= (HashMap) typeConstraintVariable.getData(INDEXED_COLLECTION_ELEMENTS);
		String key= typeVariable.getKey();
		if (keyToElementVar == null) {
			keyToElementVar= new HashMap();
			typeConstraintVariable.setData(INDEXED_COLLECTION_ELEMENTS, keyToElementVar);
		} else {
			Assert.isTrue(! keyToElementVar.containsKey(key));
		}
		keyToElementVar.put(key, elementVariable);
	}
	
	public CollectionElementVariable2 getElementVariable(ConstraintVariable2 constraintVariable, ITypeBinding typeVariable) {
		Assert.isTrue(typeVariable.isTypeVariable()); // includes null check
		HashMap typeVarToElementVars= (HashMap) constraintVariable.getData(INDEXED_COLLECTION_ELEMENTS);
		if (typeVarToElementVars == null)
			return null;
		return (CollectionElementVariable2) typeVarToElementVars.get(typeVariable.getKey());
	}

	public Map/*<String typeVariableKey, CollectionElementVariable2>*/ getElementVariables(ConstraintVariable2 constraintVariable) {
		//TODO: null check should be done on client side!
//		if (constraintVariable == null)
//			return null;
		Map elementVariables= (Map) constraintVariable.getData(INDEXED_COLLECTION_ELEMENTS);
		if (elementVariables == null)
			return EMPTY_COLLECTION_ELEMENT_VARIABLES_MAP;
		else
			return elementVariables;
	}
	
// ----------------------------- TODO: duplicated from above, but using TTypes instead of ITypeBindings -------------------
	
	public void makeElementVariables(ConstraintVariable2 expressionCv, GenericType type) {
		makeElementVariables(expressionCv, type, true);
	}
	
	/**
	 * Make element variables for type variables declared in typeBinding.
	 * 
	 * @param expressionCv the type constraint variable
	 * @param type the type binding to fetch type variables from
	 * @param isDeclaration <code>true</code> iff typeBinding is the base type of expressionCv
	 */
	private void makeElementVariables(ConstraintVariable2 expressionCv, TType type, boolean isDeclaration) {
		//TODO: element variables for type variables of enclosing types and methods
		if (isAGenericType(type)) {
			GenericType genericType= (GenericType) type.getTypeDeclaration();
			TType[] typeParameters= genericType.getTypeParameters();
			for (int i= 0; i < typeParameters.length; i++) {
				TypeVariable typeVariable= (TypeVariable) typeParameters[i];
				makeElementVariable(expressionCv, typeVariable, isDeclaration ? i : CollectionElementVariable2.NOT_DECLARED_TYPE_VARIABLE_INDEX);
				if (typeVariable.getBounds().length != 0) {
					//TODO: create subtype constraints for bounds
				}
			}
		}
		
		TType superclass= type.getSuperclass();
		if (superclass != null) {
			//TODO: don't create new CollectionElementVariables. Instead, reuse existing (add to map with new key)
			makeElementVariables(expressionCv, superclass, false);
			createTypeVariablesEqualityConstraints(expressionCv, superclass);
		}
		
		TType[] interfaces= type.getInterfaces();
		for (int i= 0; i < interfaces.length; i++) {
			makeElementVariables(expressionCv, interfaces[i], false);
			createTypeVariablesEqualityConstraints(expressionCv, interfaces[i]);
		}
	}

	private void createTypeVariablesEqualityConstraints(ConstraintVariable2 expressionCv, TType reference) {
		createTypeVariablesEqualityConstraints(expressionCv, EMPTY_COLLECTION_ELEMENT_VARIABLES_MAP, expressionCv, reference);
	}

	/**
	 * Create equality constraints between generic type variables of expressionCv and referenceCv.
	 * For example, the generic interface <code>java.lang.Iterable&lt;E&gt;</code> defines a method
	 * <code>Iterator&lt;E&gt; iterator()</code>. Given
	 * <ul>
	 *   <li>an expressionCv of a subtype of <code>Iterable</code>,</li>
	 *   <li>a referenceCv of a subtype of <code>Iterator</code>, and</li>
	 *   <li>a reference binding of the Iterable#iterator()'s return type (the parameterized type <code>Iterator&lt;E&gt;</code>),</li>
	 * </ul>
	 * this method creates an equality constraint between the type variable E in expressionCV and
	 * the type variable E in referenceCV.
	 * 
	 * @param expressionCv the type constraint variable of an expression
	 * @param methodTypeVariables 
	 * @param referenceCv the type constraint variable of a type reference
	 * @param reference the declared type reference
	 */
	public void createTypeVariablesEqualityConstraints(ConstraintVariable2 expressionCv, Map/*<String, IndependentTypeVariable2>*/ methodTypeVariables, ConstraintVariable2 referenceCv, TType reference) {
		if (reference.isParameterizedType() || reference.isRawType()) {
			TType[] referenceTypeArguments= null;
			if (reference.isParameterizedType()) {
				referenceTypeArguments= ((ParameterizedType) reference).getTypeArguments();
			}
			TType[] referenceTypeParameters= ((GenericType) reference.getTypeDeclaration()).getTypeParameters();
			for (int i= 0; i < referenceTypeParameters.length; i++) {
				TypeVariable referenceTypeParameter= (TypeVariable) referenceTypeParameters[i];
				TType referenceTypeArgument;
				if (referenceTypeArguments == null)
					referenceTypeArgument= referenceTypeParameter.getErasure();
				else
					referenceTypeArgument= referenceTypeArguments[i];
				ConstraintVariable2 referenceTypeArgumentCv;
				if (referenceTypeArgument.isTypeVariable()) {
					referenceTypeArgumentCv= (ConstraintVariable2) methodTypeVariables.get(referenceTypeArgument.getBindingKey());
					if (referenceTypeArgumentCv == null)
						referenceTypeArgumentCv= getElementVariable(expressionCv, (TypeVariable) referenceTypeArgument);
				} else if (referenceTypeArgument.isWildcardType()) {
					referenceTypeArgumentCv= null; //TODO: make new WildcardTypeVariable, which is compatible to nothing 
				} else {
					referenceTypeArgumentCv= makeIndependentTypeVariable(referenceTypeParameter);
				}
				CollectionElementVariable2 referenceTypeParametersCv= getElementVariable(referenceCv, referenceTypeParameter);
				createEqualsConstraint(referenceTypeArgumentCv, referenceTypeParametersCv);
			}
		}
	}

	private CollectionElementVariable2 makeElementVariable(ConstraintVariable2 expressionCv, TypeVariable typeVariable, int declarationTypeVariableIndex) {
		if (expressionCv == null)
			return null;
		
		CollectionElementVariable2 storedElementVariable= getElementVariable(expressionCv, typeVariable);
		if (storedElementVariable != null)
			return storedElementVariable;
		
		CollectionElementVariable2 cv= new CollectionElementVariable2(expressionCv, typeVariable, declarationTypeVariableIndex);
		cv= (CollectionElementVariable2) storedCv(cv);
		setElementVariable(expressionCv, cv, typeVariable);
		return cv;
	}
	
	private void setElementVariable(ConstraintVariable2 typeConstraintVariable, CollectionElementVariable2 elementVariable, TypeVariable typeVariable) {
		HashMap keyToElementVar= (HashMap) typeConstraintVariable.getData(INDEXED_COLLECTION_ELEMENTS);
		String key= typeVariable.getBindingKey();
		if (keyToElementVar == null) {
			keyToElementVar= new HashMap();
			typeConstraintVariable.setData(INDEXED_COLLECTION_ELEMENTS, keyToElementVar);
		} else {
			Assert.isTrue(! keyToElementVar.containsKey(key));
		}
		keyToElementVar.put(key, elementVariable);
	}
	
	public CollectionElementVariable2 getElementVariable(ConstraintVariable2 constraintVariable, TypeVariable typeVariable) {
		Assert.isTrue(typeVariable.isTypeVariable()); // includes null check
		HashMap typeVarToElementVars= (HashMap) constraintVariable.getData(INDEXED_COLLECTION_ELEMENTS);
		if (typeVarToElementVars == null)
			return null;
		return (CollectionElementVariable2) typeVarToElementVars.get(typeVariable.getBindingKey());
	}

	public void createElementEqualsConstraints(ConstraintVariable2 cv, ConstraintVariable2 initializerCv) {
		Map leftElements= getElementVariables(cv);
		Map rightElements= getElementVariables(initializerCv);
		for (Iterator leftIter= leftElements.entrySet().iterator(); leftIter.hasNext();) {
			Map.Entry leftEntry= (Map.Entry) leftIter.next();
			String leftTypeVariableKey= (String) leftEntry.getKey();
			CollectionElementVariable2 rightElementVariable= (CollectionElementVariable2) rightElements.get(leftTypeVariableKey);
			if (rightElementVariable != null) {
				CollectionElementVariable2 leftElementVariable= (CollectionElementVariable2) leftEntry.getValue();
				createEqualsConstraint(leftElementVariable, rightElementVariable);
				createElementEqualsConstraints(leftElementVariable, rightElementVariable); // recursive
			}
		}
	}

//	public void createElementSubtypeConstraints(ConstraintVariable2 cv, ConstraintVariable2 initializerCv) {
//		Map leftElements= getElementVariables(cv);
//		Map rightElements= getElementVariables(initializerCv);
//		for (Iterator leftIter= leftElements.entrySet().iterator(); leftIter.hasNext();) {
//			Map.Entry leftEntry= (Map.Entry) leftIter.next();
//			String leftTypeVariableKey= (String) leftEntry.getKey();
//			CollectionElementVariable2 rightElementVariable= (CollectionElementVariable2) rightElements.get(leftTypeVariableKey);
//			if (rightElementVariable != null) {
//				CollectionElementVariable2 leftElementVariable= (CollectionElementVariable2) leftEntry.getValue();
//				createSubtypeConstraint(leftElementVariable, rightElementVariable);
//			}
//		}
//	}
	
}
