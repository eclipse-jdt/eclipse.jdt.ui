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
	
	private static class ReaderMethod extends SpecialMethod {
		private ReaderMethod(ITypeBinding declaringType, String name, ITypeBinding returnType, ITypeBinding[] argumentTypes) {
			super(declaringType, name, returnType, argumentTypes);
		}

		public void generateConstraintsFor(MethodInvocation invocation, AugmentRawContClConstraintCreator constraintCreator) {
			AugmentRawContainerClientsTCModel tcModel= constraintCreator.getTCModel();
			
			Expression receiver= invocation.getExpression();
			//TODO: Expression can be null when visiting a non-special method in a subclass of a container type.
			TypeConstraintVariable2 expressionCv= (TypeConstraintVariable2) constraintCreator.getConstraintVariable(receiver);
			ConstraintVariable2 elementCv= tcModel.makeElementVariable(expressionCv);
			// [retVal] =^= Elem[receiver]:
			constraintCreator.setConstraintVariable(invocation, elementCv);
		}
	}

	private static class WriterMethod extends SpecialMethod {
		private final int fElementArgumentIndex;

		private WriterMethod(ITypeBinding declaringType, String name, ITypeBinding returnType, ITypeBinding[] argumentTypes, int elementArgumentIndex) {
			super(declaringType, name, returnType, argumentTypes);
			fElementArgumentIndex= elementArgumentIndex;
		}

		public void generateConstraintsFor(MethodInvocation invocation, AugmentRawContClConstraintCreator constraintCreator) {
			AugmentRawContainerClientsTCModel tcModel= constraintCreator.getTCModel();
			
			Expression receiver= invocation.getExpression();
			//TODO: expression can be null when visiting a non-special method in a subclass of a container type.
			TypeConstraintVariable2 expressionCv= (TypeConstraintVariable2) constraintCreator.getConstraintVariable(receiver);
			Expression arg= (Expression) invocation.arguments().get(fElementArgumentIndex);
			ConstraintVariable2 argCv= constraintCreator.getConstraintVariable(arg);
			ConstraintVariable2 elementCv= tcModel.makeElementVariable(expressionCv);
			// [arg] <= Elem[receiver]:
			tcModel.createSubtypeConstraint(argCv, elementCv);
		}
	}

	/**
	 * Argument type of elementArgumentIndex is Collection<? extends E>
	 */
	private static class CollectionWriterMethod extends SpecialMethod {
		private final int fElementArgumentIndex;

		private CollectionWriterMethod(ITypeBinding declaringType, String name, ITypeBinding returnType, ITypeBinding[] argumentTypes, int elementArgumentIndex) {
			super(declaringType, name, returnType, argumentTypes);
			fElementArgumentIndex= elementArgumentIndex;
		}

		public void generateConstraintsFor(MethodInvocation invocation, AugmentRawContClConstraintCreator constraintCreator) {
			AugmentRawContainerClientsTCModel tcModel= constraintCreator.getTCModel();
			
			Expression receiver= invocation.getExpression();
			//TODO: expression can be null when visiting a non-special method in a subclass of a container type.
			TypeConstraintVariable2 expressionCv= (TypeConstraintVariable2) constraintCreator.getConstraintVariable(receiver);
			Expression arg= (Expression) invocation.arguments().get(fElementArgumentIndex);
			ConstraintVariable2 argCv= constraintCreator.getConstraintVariable(arg);
			CollectionElementVariable2 argElementCv= tcModel.getElementVariable(argCv);
			CollectionElementVariable2 elementCv= tcModel.makeElementVariable(expressionCv);
			// Elem[arg] <= Elem[receiver]
			tcModel.createSubtypeConstraint(argElementCv, elementCv);
		}
	}

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
		return TypeBindings.isSuperType(specialMethod.fDeclaringType, declaringType);
	}

	private void initialize() {
		initCollection();
		initIterator();
		initList();
		//TODO: complete ...
	}
	
	private void initCollection() {
		ITypeBinding collectionType= fTCFactory.getCollectionType();
		ITypeBinding objectType= fTCFactory.getObjectType();
		ITypeBinding primitiveBooleanType= fTCFactory.getPrimitiveBooleanType();
		addSpecialMethod(new WriterMethod(collectionType, "add", null, new ITypeBinding[] {objectType}, 0));
		addSpecialMethod(new CollectionWriterMethod(collectionType, "addAll", primitiveBooleanType, new ITypeBinding[] {collectionType}, 0));
		//clear
		//contains
		//containsAll
		//equals
		//hashCode
		//isEmpty
		initCollectionIterator();
		//remove
		//remove
		//retainAll
		//size
		//toArray
		//TODO: toArray(T[])<T>
	}
	
	private void initCollectionIterator() {
		addSpecialMethod(new SpecialMethod(fTCFactory.getCollectionType(), "iterator", fTCFactory.getObjectType(), new ITypeBinding[0]) {
			public void generateConstraintsFor(MethodInvocation invocation, AugmentRawContClConstraintCreator constraintCreator) {
				AugmentRawContainerClientsTCModel tcModel= constraintCreator.getTCModel();
				
				Expression receiver= invocation.getExpression();
				//TODO: expression can be null when visiting a non-special method in a subclass of a container type.
				TypeConstraintVariable2 expressionCv= (TypeConstraintVariable2) constraintCreator.getConstraintVariable(receiver);
				CollectionElementVariable2 elementCv= tcModel.makeElementVariable(expressionCv);
				
				//TODO: retValCv has wrong type, but not used (refactoring only considers element types).
				// Should avoid creating in the first place!
				TypeConstraintVariable2 retValCv= expressionCv;
				// Elem[retVal] =^= Elem[receiver]
				constraintCreator.setConstraintVariable(invocation, retValCv);
			}
		});
	}

	private void initIterator() {
		addSpecialMethod(new ReaderMethod(fTCFactory.getIteratorType(), "next", fTCFactory.getObjectType(), new ITypeBinding[0]));
	}

	private void initList() {
		ITypeBinding listType= fTCFactory.getListType();
		ITypeBinding primitiveIntType= fTCFactory.getPrimitiveIntType();
		ITypeBinding objectType= fTCFactory.getObjectType();
		addSpecialMethod(new ReaderMethod(listType, "get", fTCFactory.getObjectType(), new ITypeBinding[] {primitiveIntType}));
		addSpecialMethod(new WriterMethod(listType, "add", null, new ITypeBinding[] {primitiveIntType, objectType}, 0));
	}

	private void addSpecialMethod(SpecialMethod specialMethod) {
		String key= specialMethod.fName + '.' + specialMethod.fParameterTypes.length;
		fContainerMethods.put(key, specialMethod);
	}

	private String getKey(IMethodBinding methodBinding) {
		return methodBinding.getName() + '.' + methodBinding.getParameterTypes().length;
	}

}
