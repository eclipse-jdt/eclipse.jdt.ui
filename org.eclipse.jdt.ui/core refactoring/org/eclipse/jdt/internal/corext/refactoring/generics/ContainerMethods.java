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

package org.eclipse.jdt.internal.corext.refactoring.generics;

import java.util.HashMap;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;

import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.AugmentRawContainerClientsTCModel;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.CollectionElementVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ConstraintVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.TypeBindings;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.TypeConstraintVariable2;

public class ContainerMethods {
	//TODO: check argument / return types
	
	
	private final AugmentRawContainerClientsTCModel fTCFactory;
	private final HashMap/*<String, SpecialMethod>*/ fContainerMethods;
	
	public ContainerMethods(AugmentRawContainerClientsTCModel factory) {
		fTCFactory= factory;
		fContainerMethods= new HashMap();
		initialize();
	}

	public SpecialMethod getSpecialMethodFor(IMethodBinding methodBinding) {
		String key= getKey(methodBinding);
		SpecialMethod specialMethod= (SpecialMethod) fContainerMethods.get(key);
		//TODO: can be multiple...
		if (specialMethod != null && isTargetOf(specialMethod, methodBinding))
			return specialMethod;
		else
			return null;
	}

	private boolean isTargetOf(SpecialMethod specialMethod, IMethodBinding methodBinding) {
		//TODO: check parameter types (resolve type variables)
		ITypeBinding declaringType= methodBinding.getDeclaringClass();
		return TypeBindings.isSuperType(specialMethod.fTypeBinding, declaringType);
	}

	private void initialize() {
		initCollectionAdd();
		initCollectionIterator();
		initIteratorNext();
		initListGet();
	}
	
	private void initCollectionAdd() {
		addSpecialMethod(new SpecialMethod(fTCFactory.getCollectionType(), "add", null, new ITypeBinding[] {fTCFactory.getObjectType()}) {
			public void generateConstraintsFor(MethodInvocation invocation, AugmentRawContClConstraintCreator constraintCreator) {
				AugmentRawContainerClientsTCModel tcModel= constraintCreator.getTCFactory();
				
				Expression receiver= invocation.getExpression();
				//TODO: expression can be null when visiting a non-special method in a subclass of a container type.
				TypeConstraintVariable2 expressionCv= (TypeConstraintVariable2) constraintCreator.getConstraintVariable(receiver);
				Expression arg0= (Expression) invocation.arguments().get(0);
				ConstraintVariable2 arg0Cv= constraintCreator.getConstraintVariable(arg0);
				ConstraintVariable2 elementCv= tcModel.makeElementVariable(expressionCv);
				// [arg0] <= Elem[receiver]
				tcModel.createSubtypeConstraint(arg0Cv, elementCv);
			}
		});
	}
	
	private void initCollectionIterator() {
		addSpecialMethod(new SpecialMethod(fTCFactory.getCollectionType(), "iterator", fTCFactory.getObjectType(), new ITypeBinding[0]) {
			public void generateConstraintsFor(MethodInvocation invocation, AugmentRawContClConstraintCreator constraintCreator) {
				AugmentRawContainerClientsTCModel tcModel= constraintCreator.getTCFactory();
				
				Expression receiver= invocation.getExpression();
				//TODO: expression can be null when visiting a non-special method in a subclass of a container type.
				TypeConstraintVariable2 expressionCv= (TypeConstraintVariable2) constraintCreator.getConstraintVariable(receiver);
				CollectionElementVariable2 elementCv= tcModel.makeElementVariable(expressionCv);
				
				ITypeBinding retValType= invocation.resolveMethodBinding().getReturnType();
				//TODO: is wrong type, but not used. Should avoid creating in the first place!
				TypeConstraintVariable2 retValCv= expressionCv;
				// Elem[retVal] =^= Elem[receiver]
				constraintCreator.setConstraintVariable(invocation, retValCv);
			}
		});
	}

	private void initIteratorNext() {
		addSpecialMethod(new SpecialMethod(fTCFactory.getIteratorType(), "next", fTCFactory.getObjectType(), new ITypeBinding[0]) {
			public void generateConstraintsFor(MethodInvocation invocation, AugmentRawContClConstraintCreator constraintCreator) {
				generateReaderConstraints(invocation, constraintCreator);
			}
		});
	}

	private void initListGet() {
		addSpecialMethod(new SpecialMethod(fTCFactory.getCollectionType(), "get", fTCFactory.getObjectType(), new ITypeBinding[] {fTCFactory.getPrimitiveIntType()}) {
			public void generateConstraintsFor(MethodInvocation invocation, AugmentRawContClConstraintCreator constraintCreator) {
				generateReaderConstraints(invocation, constraintCreator);
			}
		});
	}

	private void addSpecialMethod(SpecialMethod specialMethod) {
		String key= specialMethod.fName + '.' + specialMethod.fParameterTypes.length;
		fContainerMethods.put(key, specialMethod);
	}

	private String getKey(IMethodBinding methodBinding) {
		return methodBinding.getName() + '.' + methodBinding.getParameterTypes().length;
	}

	private void generateReaderConstraints(MethodInvocation invocation, AugmentRawContClConstraintCreator constraintCreator) {
		AugmentRawContainerClientsTCModel tcModel= constraintCreator.getTCFactory();
		
		Expression receiver= invocation.getExpression();
		//TODO: expression can be null when visiting a non-special method in a subclass of a container type.
		TypeConstraintVariable2 expressionCv= (TypeConstraintVariable2) constraintCreator.getConstraintVariable(receiver);
		ConstraintVariable2 elementCv= tcModel.makeElementVariable(expressionCv);
		// [retVal] =^= Elem(receiver)
		constraintCreator.setConstraintVariable(invocation, elementCv);
	}

}
