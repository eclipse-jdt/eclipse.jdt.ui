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

import java.util.ArrayList;
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
	//TODO: java.util.Collections? -> done for Collections.min(..)
	//TODO: simplify this business by assuming that all E in arguments are write accesses and all
	// E in return type are read accesses. Generate special methods out of ITypes in Collections hierarchy.
	// Note: that approach only works when an 1.5 JRE / collections library is available.
	//TODO: What about constructors taking a collection?
	
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

	private static class ReaderWriterMethod extends SpecialMethod {
		private final int fElementArgumentIndex;

		private ReaderWriterMethod(ITypeBinding declaringType, String name, ITypeBinding returnType, ITypeBinding[] argumentTypes, int elementArgumentIndex) {
			super(declaringType, name, returnType, argumentTypes);
			fElementArgumentIndex= elementArgumentIndex;
		}

		public void generateConstraintsFor(MethodInvocation invocation, AugmentRawContClConstraintCreator constraintCreator) {
			AugmentRawContainerClientsTCModel tcModel= constraintCreator.getTCModel();
			
			Expression receiver= invocation.getExpression();
			//TODO: Expression can be null when visiting a non-special method in a subclass of a container type.
			TypeConstraintVariable2 expressionCv= (TypeConstraintVariable2) constraintCreator.getConstraintVariable(receiver);
			ConstraintVariable2 elementCv= tcModel.makeElementVariable(expressionCv);
			
			//reader:
			// [retVal] =^= Elem[receiver]:
			constraintCreator.setConstraintVariable(invocation, elementCv);
			
			//writer:
			Expression arg= (Expression) invocation.arguments().get(fElementArgumentIndex);
			ConstraintVariable2 argCv= constraintCreator.getConstraintVariable(arg);
			// [arg] <= Elem[receiver]:
			tcModel.createSubtypeConstraint(argCv, elementCv);
		}
	}

	/**
	 * Argument type of argument elementArgumentIndex is {@code Collection<? extends E>}.
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

	/**
	 * Return type is {@code returnType<E>}.
	 */
	private static class CollectionReaderMethod extends SpecialMethod {
		private CollectionReaderMethod(ITypeBinding declaringType, String name, ITypeBinding returnType, ITypeBinding[] argumentTypes) {
			super(declaringType, name, returnType, argumentTypes);
		}

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
	}

	private final AugmentRawContainerClientsTCModel fTCModel;
	private final HashMap/*<String, ArrayList<SpecialMethod>>*/ fContainerMethods;
	
	public ContainerMethods(AugmentRawContainerClientsTCModel factory) {
		fTCModel= factory;
		fContainerMethods= new HashMap();
		initialize();
	}

	public SpecialMethod getSpecialMethodFor(IMethodBinding methodBinding) {
		String key= getKey(methodBinding);
		ArrayList/*<SpecialMethod>*/ specialMethods= (ArrayList) fContainerMethods.get(key);
		if (specialMethods == null)
			return null;
		for (int i= 0; i < specialMethods.size(); i++) {
			SpecialMethod specialMethod= (SpecialMethod) specialMethods.get(i);
			if (isTargetOf(specialMethod, methodBinding))
				return specialMethod;
		}
		return null;
	}

	private boolean isTargetOf(SpecialMethod specialMethod, IMethodBinding methodBinding) {
		//TODO: check parameter types (resolve type variables)
		ITypeBinding declaringType= methodBinding.getDeclaringClass();
		return TypeBindings.isSuperType(specialMethod.fDeclaringType, declaringType);
	}

	private void addSpecialMethod(SpecialMethod specialMethod) {
		String key= specialMethod.fName + '.' + specialMethod.fParameterTypes.length;
		ArrayList methods= (ArrayList) fContainerMethods.get(key);
		if (methods == null) {
			methods= new ArrayList(1);
			fContainerMethods.put(key, methods);
		}
		methods.add(specialMethod);
	}

	private static String getKey(IMethodBinding methodBinding) {
		return methodBinding.getName() + '.' + methodBinding.getParameterTypes().length;
	}

	private void initialize() {
		initCollection();
		initList();
		initLinkedList();
		initVector();
		initIterator();
		initListIterator();
		initEnumeration();
		//TODO: complete ...
		// missing branches: Queue, Set
		
		initCollections();
	}
	
	private void initCollection() {
		ITypeBinding primitiveBooleanType= fTCModel.getPrimitiveBooleanType();
		ITypeBinding voidType= fTCModel.getVoidType();
		ITypeBinding objectType= fTCModel.getObjectType();
		ITypeBinding collectionType= fTCModel.getCollectionType();
		ITypeBinding iteratorType= fTCModel.getIteratorType();
		
		addSpecialMethod(new WriterMethod(collectionType, "add", primitiveBooleanType, new ITypeBinding[] {objectType}, 0));
		addSpecialMethod(new CollectionWriterMethod(collectionType, "addAll", primitiveBooleanType, new ITypeBinding[] {collectionType}, 0));
		addSpecialMethod(new CollectionReaderMethod(collectionType, "iterator", iteratorType, new ITypeBinding[0]));
		//TODO: toArray(T[])<T>
	}
	
	private void initList() {
		ITypeBinding primitiveIntType= fTCModel.getPrimitiveIntType();
		ITypeBinding primitiveBooleanType= fTCModel.getPrimitiveBooleanType();
		ITypeBinding voidType= fTCModel.getVoidType();
		ITypeBinding objectType= fTCModel.getObjectType();
		ITypeBinding collectionType= fTCModel.getCollectionType();
		ITypeBinding listType= fTCModel.getListType();
		ITypeBinding listIteratorType= fTCModel.getListIteratorType();
		
		addSpecialMethod(new WriterMethod(listType, "add", voidType, new ITypeBinding[] {primitiveIntType, objectType}, 1));
		addSpecialMethod(new CollectionWriterMethod(collectionType, "addAll", primitiveBooleanType,
				new ITypeBinding[] {primitiveIntType, collectionType}, 1));
		addSpecialMethod(new ReaderMethod(listType, "get", fTCModel.getObjectType(), new ITypeBinding[] {primitiveIntType}));
		addSpecialMethod(new CollectionReaderMethod(listType, "listIterator", listIteratorType, new ITypeBinding[0]));
		addSpecialMethod(new CollectionReaderMethod(listType, "listIterator", listIteratorType, new ITypeBinding[] {primitiveIntType}));
		addSpecialMethod(new ReaderMethod(listType, "remove", fTCModel.getObjectType(), new ITypeBinding[] {primitiveIntType}));
		addSpecialMethod(new ReaderWriterMethod(listType, "set", objectType, new ITypeBinding[] {primitiveIntType, objectType}, 1));
		addSpecialMethod(new CollectionReaderMethod(listType, "subList", listType, new ITypeBinding[] {primitiveIntType, primitiveIntType}));
	}
	
	private void initLinkedList() {
		ITypeBinding voidType= fTCModel.getVoidType();
		ITypeBinding objectType= fTCModel.getObjectType();
		ITypeBinding linkedListType= fTCModel.getLinkedListType();
		
		addSpecialMethod(new WriterMethod(linkedListType, "addFirst", voidType, new ITypeBinding[] {objectType}, 0));
		addSpecialMethod(new WriterMethod(linkedListType, "addLast", voidType, new ITypeBinding[] {objectType}, 0));
		addSpecialMethod(new ReaderMethod(linkedListType, "getFirst", objectType, new ITypeBinding[0]));
		addSpecialMethod(new ReaderMethod(linkedListType, "getLast", objectType, new ITypeBinding[0]));
		addSpecialMethod(new ReaderMethod(linkedListType, "removeFirst", objectType, new ITypeBinding[0]));
		addSpecialMethod(new ReaderMethod(linkedListType, "removeLast", objectType, new ITypeBinding[0]));
	}
	
	private void initVector() {
		ITypeBinding primitiveIntType= fTCModel.getPrimitiveIntType();
		ITypeBinding voidType= fTCModel.getVoidType();
		ITypeBinding objectType= fTCModel.getObjectType();
		ITypeBinding vectorType= fTCModel.getVectorType();
		ITypeBinding enumerationType= fTCModel.getEnumerationType();
		
		addSpecialMethod(new WriterMethod(vectorType, "addElement", voidType, new ITypeBinding[] {objectType}, 0));
		addSpecialMethod(new ReaderMethod(vectorType, "elementAt", objectType, new ITypeBinding[] { primitiveIntType }));
		addSpecialMethod(new CollectionReaderMethod(vectorType, "elements", enumerationType, new ITypeBinding[0]));
		addSpecialMethod(new ReaderMethod(vectorType, "firstElement", objectType, new ITypeBinding[0]));
		addSpecialMethod(new WriterMethod(vectorType, "insertElementAt", voidType,
				new ITypeBinding[] {objectType, primitiveIntType}, 0));
		addSpecialMethod(new ReaderMethod(vectorType, "lastElement", objectType, new ITypeBinding[0]));
		addSpecialMethod(new WriterMethod(vectorType, "setElementAt", voidType,
				new ITypeBinding[] {objectType, primitiveIntType}, 0));
	}
	
	private void initIterator() {
		ITypeBinding objectType= fTCModel.getObjectType();
		ITypeBinding iteratorType= fTCModel.getIteratorType();
		
		addSpecialMethod(new ReaderMethod(iteratorType, "next", objectType, new ITypeBinding[0]));
	}

	private void initListIterator() {
		ITypeBinding voidType= fTCModel.getVoidType();
		ITypeBinding objectType= fTCModel.getObjectType();
		ITypeBinding listIteratorType= fTCModel.getListIteratorType();
		
		addSpecialMethod(new ReaderMethod(listIteratorType, "previous", objectType, new ITypeBinding[0]));
		addSpecialMethod(new WriterMethod(listIteratorType, "set", voidType, new ITypeBinding[] { objectType }, 0));
		addSpecialMethod(new WriterMethod(listIteratorType, "add", voidType, new ITypeBinding[] { objectType }, 0));
	}

	private void initEnumeration() {
		ITypeBinding objectType= fTCModel.getObjectType();
		ITypeBinding enumerationType= fTCModel.getEnumerationType();
		
		addSpecialMethod(new ReaderMethod(enumerationType, "nextElement", objectType, new ITypeBinding[0]));
	}

	private void initCollections() {
		ITypeBinding objectType= fTCModel.getObjectType();
		ITypeBinding collectionType= fTCModel.getCollectionType();
		ITypeBinding collectionsType= fTCModel.getCollectionsType();
		addSpecialMethod(new SpecialMethod(collectionsType, "min", objectType, new ITypeBinding[] {collectionType}) {
			// <T extends Object & Comparable<? super T>> T min(Collection<? extends T> coll)
			public void generateConstraintsFor(MethodInvocation invocation, AugmentRawContClConstraintCreator constraintCreator) {
				AugmentRawContainerClientsTCModel tcModel= constraintCreator.getTCModel();
				
				Expression arg= (Expression) invocation.arguments().get(0);
				TypeConstraintVariable2 argCv= (TypeConstraintVariable2) constraintCreator.getConstraintVariable(arg);
				if (argCv == null)
					return;
				CollectionElementVariable2 argElementCv= tcModel.getElementVariable(argCv);
				
				// [retVal] =^= Elem[arg0] //TODO: correct?
				constraintCreator.setConstraintVariable(invocation, argElementCv);
			}
		});
	}
}
