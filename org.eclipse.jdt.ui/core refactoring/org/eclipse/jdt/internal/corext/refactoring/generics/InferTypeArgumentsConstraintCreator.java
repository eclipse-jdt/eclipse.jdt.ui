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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor;
import org.eclipse.jdt.internal.corext.refactoring.rename.MethodChecks;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.CollectionElementVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ConstraintVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ImmutableTypeVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.IndependentTypeVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ParameterTypeVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ReturnTypeVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.TypeVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.VariableVariable2;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class InferTypeArgumentsConstraintCreator extends HierarchicalASTVisitor {

	/**
	 * Property in <code>ASTNode</code>s that holds the node's <code>ConstraintVariable</code>.
	 */
	private static final String CV_PROP= "org.eclipse.jdt.internal.corext.refactoring.typeconstraints.CONSTRAINT_VARIABLE"; //$NON-NLS-1$
	
	private InferTypeArgumentsTCModel fTCModel;
	private ICompilationUnit fCU;

	private final boolean fAssumeCloneReturnsSameType;
	

	public InferTypeArgumentsConstraintCreator(InferTypeArgumentsTCModel model, boolean assumeCloneReturnsSameType) {
		fTCModel= model;
		fAssumeCloneReturnsSameType= assumeCloneReturnsSameType;
	}
	
	public boolean visit(CompilationUnit node) {
		fTCModel.newCu(); //TODO: make sure that accumulators are reset after last CU!
		fCU= RefactoringASTParser.getCompilationUnit(node);
		return super.visit(node);
	}
	
	public boolean visit(Javadoc node) {
		return false;
	}
	
	public boolean visit(Type node) {
		return false; //TODO
	}
	
	/*
	 * @see org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor#endVisit(org.eclipse.jdt.core.dom.Type)
	 */
	public void endVisit(Type node) {
		TypeVariable2 typeVariable= fTCModel.makeTypeVariable(node);
		setConstraintVariable(node, typeVariable);
	}
	
	public void endVisit(SimpleName node) {
		IBinding binding= node.resolveBinding();
		if (binding instanceof IVariableBinding) {
			//TODO: code is similar to handling of method return value
			IVariableBinding variableBinding= (IVariableBinding) binding;
			ITypeBinding declaredVariableType= variableBinding.getVariableDeclaration().getType();
			if (declaredVariableType.isTypeVariable()) {
				Expression receiver= getSimpleNameReceiver(node);
				if (receiver != null) {
					ConstraintVariable2 receiverCv= getConstraintVariable(receiver);
					Assert.isNotNull(receiverCv); // the type variable must come from the receiver!
					
					ConstraintVariable2 elementCv= fTCModel.getElementVariable(receiverCv, declaredVariableType);
					// [retVal] =^= Elem[receiver]:
					setConstraintVariable(node, elementCv);
					return;
				}
				
			} else if (declaredVariableType.isParameterizedType()){
				Expression receiver= getSimpleNameReceiver(node);
				if (receiver != null) {
					ConstraintVariable2 receiverCv= getConstraintVariable(receiver);
					if (receiverCv != null) {
						ITypeBinding genericVariableType= declaredVariableType.getTypeDeclaration();
						ConstraintVariable2 returnTypeCv= fTCModel.makeParameterizedTypeVariable(genericVariableType);
						setConstraintVariable(node, returnTypeCv);
						// Elem[retVal] =^= Elem[receiver]
						fTCModel.createTypeVariablesEqualityConstraints(receiverCv, Collections.EMPTY_MAP, returnTypeCv, declaredVariableType);
						return;
					}
				}
				
			} else {
				//TODO: array...
				int i= -1;
				//logUnexpectedNode(node, null);
			}
			
			// default: 
			VariableVariable2 cv= fTCModel.makeVariableVariable(variableBinding, fCU);
			setConstraintVariable(node, cv);
		}
		// TODO else?
	}

	private Expression getSimpleNameReceiver(SimpleName node) {
		Expression receiver;
		if (node.getParent() instanceof QualifiedName && node.getLocationInParent() == QualifiedName.NAME_PROPERTY) {
			receiver= ((QualifiedName) node.getParent()).getQualifier();
		} else if (node.getParent() instanceof FieldAccess && node.getLocationInParent() == FieldAccess.NAME_PROPERTY) {
			receiver= ((FieldAccess) node.getParent()).getExpression();
		} else {
			//TODO other cases? (ThisExpression, SuperAccessExpression, ...)
			receiver= null;
		}
		if (receiver instanceof ThisExpression)
			return null;
		else
			return receiver;
	}
	
	//TODO: FieldAccess
	
	public void endVisit(QualifiedName node) {
		ConstraintVariable2 cv= getConstraintVariable(node.getName());
		setConstraintVariable(node, cv);
	}
	
	public void endVisit(Assignment node) {
		Expression lhs= node.getLeftHandSide();
		Expression rhs= node.getRightHandSide();
		
		ConstraintVariable2 left= getConstraintVariable(lhs);
		ConstraintVariable2 right= getConstraintVariable(rhs);
		if (left == null || right == null)
			return;
		
		Assignment.Operator op= node.getOperator();
		if (op == Assignment.Operator.PLUS_ASSIGN && (lhs.resolveTypeBinding() == node.getAST().resolveWellKnownType("java.lang.String"))) { //$NON-NLS-1$
			//Special handling for automatic String conversion: do nothing; the RHS can be anything.
		} else {
			createElementEqualsConstraints(left, right);
			fTCModel.createSubtypeConstraint(right, left); // left= right;  -->  [right] <= [left]
		}
		//TODO: other implicit conversions: numeric promotion, autoboxing?
		
		setConstraintVariable(node, left); // type of assignement is type of 'left'
	}
	
	public void endVisit(CastExpression node) {
//		if (! (expressionCv instanceof CollectionElementVariable2))
//			return; //TODO: returns too early when dealing with nested collections.
//		fTCModel.makeCastVariable(node, (CollectionElementVariable2) expressionCv);
		
		Type type= node.getType();
		ConstraintVariable2 typeCv= getConstraintVariable(type);
		
		//TODO: can this be loosened when we remove casts?
		setConstraintVariable(node, typeCv);
		
		Expression expression= node.getExpression();
		ConstraintVariable2 expressionCv= getConstraintVariable(expression);
		if (expressionCv == null)
			return;
		
		createElementEqualsConstraints(expressionCv, typeCv);
		
		if (expression instanceof MethodInvocation) {
			MethodInvocation invoc= (MethodInvocation) expression;
			if (! isSpecialCloneInvocation(invoc.resolveMethodBinding(), invoc.getExpression())) {
				fTCModel.makeCastVariable(node, expressionCv);
			}
		} else {
			fTCModel.makeCastVariable(node, expressionCv);
		}
		
		boolean eitherIsIntf= type.resolveBinding().isInterface() || expression.resolveTypeBinding().isInterface();
		if (eitherIsIntf)
			return;
		
		//TODO: preserve up- and down-castedness!
		
	}
	
	public void endVisit(ParenthesizedExpression node) {
		ConstraintVariable2 expressionCv= getConstraintVariable(node.getExpression());
		setConstraintVariable(node, expressionCv);
	}
	
	public boolean visit(CatchClause node) {
		SingleVariableDeclaration exception= node.getException();
		IVariableBinding variableBinding= exception.resolveBinding();
		VariableVariable2 cv= fTCModel.makeDeclaredVariableVariable(variableBinding, fCU);
		setConstraintVariable(exception, cv);
		return true;
	}
	
	public void endVisit(StringLiteral node) {
		ITypeBinding typeBinding= node.resolveTypeBinding();
		ImmutableTypeVariable2 cv= fTCModel.makeImmutableTypeVariable(typeBinding, fCU);
		setConstraintVariable(node, cv);
	}
	
	public void endVisit(NumberLiteral node) {
		ITypeBinding typeBinding= node.resolveTypeBinding();
		ImmutableTypeVariable2 cv= fTCModel.makeImmutableTypeVariable(typeBinding, fCU);
		setConstraintVariable(node, cv);
	}

	public void endVisit(TypeLiteral node) {
//		ITypeBinding typeBinding= node.resolveTypeBinding();
//		ImmutableTypeVariable2 cv= fTCModel.makePlainTypeVariable(typeBinding);
//		fTCModel.makeElementVariable(cv);
//		setConstraintVariable(node, cv);
	}
	
	public void endVisit(MethodDeclaration node) {
		IMethodBinding methodBinding= node.resolveBinding();
	
		if (methodBinding == null)
			return; //TODO: emit error?
		
		int parameterCount= node.parameters().size();
		ConstraintVariable2[] parameterTypeCvs= new ConstraintVariable2[parameterCount];
		for (int i= 0; i < parameterCount; i++) {
			SingleVariableDeclaration paramDecl= (SingleVariableDeclaration) node.parameters().get(i);
			//parameterTypeVariable currently not used, but need to register in order to store source range
			ConstraintVariable2 parameterTypeCv= fTCModel.makeDeclaredParameterTypeVariable(methodBinding, i, fCU);
			parameterTypeCvs[i]= parameterTypeCv;
			if (parameterTypeCv == null)
				continue;
			
			//creating equals constraint between parameterTypeVariable's elements and the Type's elements
			ConstraintVariable2 typeCv= getConstraintVariable(paramDecl.getType());
			createElementEqualsConstraints(parameterTypeCv, typeCv);
			
			//TODO: should avoid having a VariableVariable as well as a ParameterVariable for a parameter
			ConstraintVariable2 nameCv= getConstraintVariable(paramDecl.getName());
			createElementEqualsConstraints(parameterTypeCv, nameCv);
		}
		
		ConstraintVariable2 returnTypeCv= null;
		if (! methodBinding.isConstructor()) {
			//TODO: should only create return type variable if type is generic?
			ConstraintVariable2 returnTypeBindingCv= fTCModel.makeDeclaredReturnTypeVariable(methodBinding, fCU);
			if (returnTypeBindingCv != null) {
				returnTypeCv= getConstraintVariable(node.getReturnType2());
				createElementEqualsConstraints(returnTypeBindingCv, returnTypeCv);
			}
		}
		if (MethodChecks.isVirtual(methodBinding)) {
			//TODO: RippleMethod constraints for corner cases: see testCuRippleMethods3, bug 41989
			addConstraintsForOverriding(methodBinding, returnTypeCv, parameterTypeCvs);
		}
	}
	
//TODO:
	private void addConstraintsForOverriding(IMethodBinding methodBinding, ConstraintVariable2 returnTypeCv, ConstraintVariable2[] parameterTypeCvs) {
		boolean hasParameterElementCvs= false;
		for (int i= 0; i < parameterTypeCvs.length; i++)
			if (parameterTypeCvs[i] != null)
				hasParameterElementCvs= true;
		
		if (returnTypeCv == null && ! hasParameterElementCvs)
			return;
		
		String name= methodBinding.getName();
		ITypeBinding[] parameterTypes= methodBinding.getParameterTypes();
		
		ITypeBinding[] allSuperTypes= Bindings.getAllSuperTypes(methodBinding.getDeclaringClass());
		for (int i= 0; i < allSuperTypes.length; i++) {
			ITypeBinding superType= allSuperTypes[i];
			IMethodBinding superMethod= Bindings.findMethodInType(superType, name, parameterTypes);
			if (superMethod == null)
				continue;
			
			for (int p= 0; p < parameterTypeCvs.length; p++) {
				if (parameterTypeCvs[p] == null)
					continue;
				ParameterTypeVariable2 parameterTypeCv= fTCModel.makeParameterTypeVariable(superMethod, p, fCU);
				createElementEqualsConstraints(parameterTypeCv, parameterTypeCvs[p]);
			}
			
			if (returnTypeCv != null) {
				ReturnTypeVariable2 superMethodReturnTypeCv= fTCModel.makeReturnTypeVariable(superMethod, fCU);
				createElementEqualsConstraints(superMethodReturnTypeCv, returnTypeCv);
			}
		}
	}
//	private void addConstraintsForOverriding(IMethodBinding methodBinding, CollectionElementVariable2 returnTypeElementCv, CollectionElementVariable2[] parameterElementCvs) {
//		boolean hasParameterElementCvs= false;;
//		for (int i= 0; i < parameterElementCvs.length; i++)
//			if (parameterElementCvs[i] != null)
//				hasParameterElementCvs= true;
//		
//		if (returnTypeElementCv == null && ! hasParameterElementCvs)
//			return;
//		
//		String name= methodBinding.getName();
//		ITypeBinding[] parameterTypes= methodBinding.getParameterTypes();
//		
//		ITypeBinding[] allSuperTypes= Bindings.getAllSuperTypes(methodBinding.getDeclaringClass());
//		for (int i= 0; i < allSuperTypes.length; i++) {
//			ITypeBinding superType= allSuperTypes[i];
//			IMethodBinding superMethod= Bindings.findMethodInType(superType, name, parameterTypes);
//			if (superMethod == null)
//				continue;
//			
//			for (int p= 0; p < parameterElementCvs.length; p++) {
//				if (parameterElementCvs[p] == null)
//					continue;
//				ParameterTypeVariable2 parameterTypeVariable= fTCModel.makeParameterTypeVariable(superMethod, p);
//				CollectionElementVariable2 superParameterElementCv= fTCModel.makeElementVariable(parameterTypeVariable);
//				fTCModel.createEqualsConstraint(superParameterElementCv, parameterElementCvs[p]);
//			}
//			
//			if (returnTypeElementCv != null) {
//				ReturnTypeVariable2 returnTypeVariable= fTCModel.makeReturnTypeVariable(superMethod);
//				CollectionElementVariable2 superReturnElementCv= fTCModel.makeElementVariable(returnTypeVariable);
//				fTCModel.createEqualsConstraint(superReturnElementCv, returnTypeElementCv);
//			}
//		}
//	}

	public void endVisit(MethodInvocation node) {
		IMethodBinding methodBinding= node.resolveMethodBinding();
		if (methodBinding == null)
			return;
		
		Expression receiver= node.getExpression();
		//TODO: Expression can be null when visiting a non-special method in a subclass of a container type.
		
		Map/*<String, IndependentTypeVariable2>*/ methodTypeVariables= createMethodTypeParameters(methodBinding);
		
		if (isSpecialCloneInvocation(methodBinding, receiver)) {
			ConstraintVariable2 expressionCv= getConstraintVariable(receiver);
			// [retVal] =^= [receiver]:
			setConstraintVariable(node, expressionCv);
		} else {
			doVisitMethodInvocationReturnType(node, methodBinding, receiver, methodTypeVariables);
		}
		List arguments= node.arguments();
		doVisitMethodInvocationArguments(methodBinding, arguments, receiver, methodTypeVariables, null);
		
	}

	/**
	 * @return a map from type variable key to type variable constraint variable
	 */
	private Map/*<String, IndependentTypeVariable2>*/ createMethodTypeParameters(IMethodBinding methodBinding) {
		ITypeBinding[] methodTypeParameters= methodBinding.getMethodDeclaration().getTypeParameters();
		Map methodTypeVariables;
		if (methodTypeParameters.length == 0) {
			methodTypeVariables= Collections.EMPTY_MAP;
		} else {
			methodTypeVariables= new HashMap();
			for (int i= 0; i < methodTypeParameters.length; i++) {
				ITypeBinding methodTypeParameter= methodTypeParameters[i];
				//TODO: typeVariable does not need a type binding - only used in equality constraints
				IndependentTypeVariable2 typeVariableCv= fTCModel.makeIndependentTypeVariable(methodTypeParameter);
				methodTypeVariables.put(methodTypeParameter.getKey(), typeVariableCv);
			}
		}
		return methodTypeVariables;
	}
	
	private void doVisitMethodInvocationReturnType(/*MethodInvocation*/ASTNode node, IMethodBinding methodBinding, Expression receiver, Map/*<String, IndependentTypeVariable2>*/ methodTypeVariables) {
		ITypeBinding declaredReturnType= methodBinding.getMethodDeclaration().getReturnType();
		
		if (declaredReturnType.isTypeVariable()) {
			ConstraintVariable2 methodTypeVariableCv= (ConstraintVariable2) methodTypeVariables.get(declaredReturnType.getKey());
			if (methodTypeVariableCv != null) {
				// e.g. in Collections: <T ..> T min(Collection<? extends T> coll):
				setConstraintVariable(node, methodTypeVariableCv); //TODO: should be [retVal] <= Elem[arg] in this case?
				
	//			TODO:
	//			} else if (methodBinding.getErasure().getTypeParameters().length == 1 &&
	//					(genericReturnType.isTypeVariable() || genericReturnType.isWildcardType()) &&
	//					methodBinding.getParameterTypes().length == 1 &&
	//					methodBinding.getParameterTypes()[0].getErasure().isGenericType()) {
	//				// e.g. in Collections: <T ..> T min(Collection<? extends T> coll):
	//				TypeConstraintVariable2 argCv= (TypeConstraintVariable2) getConstraintVariable((Expression) node.arguments().get(0));
	//				ConstraintVariable2 elementCv= fTCModel.makeElementVariable(argCv);
	//				// [retVal] =^= Elem[arg]:
	//				setConstraintVariable(node, elementCv); //TODO: should be [retVal] <= Elem[arg]
				
			} else {
				//TODO: nested generic classes and methods?
				
				if (receiver == null) //TODO: deal with methods inside generic types
					return;
				// e.g. in List<E>: E get(int index):
				ConstraintVariable2 expressionCv= getConstraintVariable(receiver);
				ConstraintVariable2 elementCv= fTCModel.getElementVariable(expressionCv, declaredReturnType);
				// [retVal] =^= Elem[receiver]:
				setConstraintVariable(node, elementCv);
			}
		
		} else if (declaredReturnType.isParameterizedType()) {
			if (receiver == null) //TODO: deal with methods inside generic types
				return;
			//e.g. List<E>: Iterator<E> iterator()
			ConstraintVariable2 receiverCv= getConstraintVariable(receiver);
			ITypeBinding genericReturnType= declaredReturnType.getTypeDeclaration();
			ConstraintVariable2 returnTypeCv= fTCModel.makeParameterizedTypeVariable(genericReturnType);
			setConstraintVariable(node, returnTypeCv);
			// Elem[retVal] =^= Elem[receiver]
			fTCModel.createTypeVariablesEqualityConstraints(receiverCv, methodTypeVariables, returnTypeCv, declaredReturnType);
		
//		} else if (genericMethodReturnType.isArray()) {
//			//TODO: See bug 84422. Need an ArrayTypeVariable2 and handling similar to the isParameterizedType() case.
//			
		} else {
			ReturnTypeVariable2 returnTypeCv= fTCModel.makeReturnTypeVariable(methodBinding, fCU);
			setConstraintVariable(node, returnTypeCv);
		}
	}

	private boolean isSpecialCloneInvocation(IMethodBinding methodBinding, Expression receiver) {
		return fAssumeCloneReturnsSameType
				&& "clone".equals(methodBinding.getName()) //$NON-NLS-1$
				&& methodBinding.getParameterTypes().length == 0
				&& receiver != null
				&& receiver.resolveTypeBinding() != methodBinding.getMethodDeclaration().getReturnType();
	}

	private void doVisitMethodInvocationArguments(IMethodBinding methodBinding, List arguments, Expression receiver, Map methodTypeVariables, Type createdType) {
		//TODO: connect generic method type parameters, e.g. <T> void take(T t, List<T> ts)
		ITypeBinding[] declaredParameterTypes= methodBinding.getMethodDeclaration().getParameterTypes();
		for (int i= 0; i < declaredParameterTypes.length; i++) {
			ITypeBinding declaredParameterType= declaredParameterTypes[i];
			if (declaredParameterType.isTypeVariable()) {
				Expression arg= (Expression) arguments.get(i);
				ConstraintVariable2 argCv= getConstraintVariable(arg);
				
				ConstraintVariable2 methodTypeVariableCv= (ConstraintVariable2) methodTypeVariables.get(declaredParameterType.getKey());
				if (methodTypeVariableCv != null) {
					// e.g. t in "<T> void take(T t, List<T> ts)"
					fTCModel.createSubtypeConstraint(argCv, methodTypeVariableCv);
					
				} else {
					if (createdType != null) {
						//e.g. Tuple<T1, T2>: constructor Tuple(T1 t1, T2 t2)
						ConstraintVariable2 createdTypeCv= getConstraintVariable(createdType);
						ConstraintVariable2 elementCv= fTCModel.getElementVariable(createdTypeCv, declaredParameterType);
						// [arg] <= Elem[createdType]:
						fTCModel.createSubtypeConstraint(argCv, elementCv);
					}
					if (receiver != null) {
						//e.g. "Collection<E>: boolean add(E o)"
						ConstraintVariable2 expressionCv= getConstraintVariable(receiver);
						ConstraintVariable2 elementCv= fTCModel.getElementVariable(expressionCv, declaredParameterType);
	
						//	//TypeVariableConstraintVariable2 typeVariableCv= fTCModel.makeTypeVariableVariable(declaredParameterType);
						//				ConstraintVariable2 elementCv= fTCModel.makeElementVariable(expressionCv, typeVariableCv);
						//TODO: Somebody must connect typeVariableCv to corresponding typeVariableCVs of supertypes.
						//- Do only once for binaries.
						//- Do when passing for sources.
						//- Keep a flag in CV whether done?
						//- Do in one pass over all TypeVarCvs at the end?
	
						// [arg] <= Elem[receiver]:
						fTCModel.createSubtypeConstraint(argCv, elementCv);
					} else {
						//TODO: ???
					}
				}
			} else if (declaredParameterType.isParameterizedType()) {
				Expression arg= (Expression) arguments.get(i);
				ConstraintVariable2 argCv= getConstraintVariable(arg);
				ITypeBinding[] typeArguments= declaredParameterType.getTypeArguments();
				ITypeBinding[] typeParameters= declaredParameterType.getErasure().getTypeParameters();
				for (int ta= 0; ta < typeArguments.length; ta++) {
					ITypeBinding typeArgument= typeArguments[ta];
					if (typeArgument.isWildcardType() && typeArgument.isUpperbound()) {
						// Elem[arg] <= Elem[receiver]
						ITypeBinding bound= typeArgument.getBound();
						if (bound.isTypeVariable()) {
							ConstraintVariable2 methodTypeVariableCv= (ConstraintVariable2) methodTypeVariables.get(bound.getKey());
							if (methodTypeVariableCv != null) {
								CollectionElementVariable2 argElementCv= fTCModel.getElementVariable(argCv, typeParameters[ta]);
								//e.g. in Collections: <T ..> T min(Collection<? extends T> coll):
								//TODO:
								fTCModel.createEqualsConstraint(argElementCv, methodTypeVariableCv);
//								fTCModel.createSubtypeConstraint(argElementCv, methodTypeVariableCv);
//								fTCModel.createSubtypeConstraint(methodTypeVariableCv, argElementCv);
								
							} else {
								if (createdType != null) {
									CollectionElementVariable2 argElementCv= fTCModel.getElementVariable(argCv, typeParameters[ta]);
									ConstraintVariable2 createdTypeCv= getConstraintVariable(createdType);
									ConstraintVariable2 elementCv= fTCModel.getElementVariable(createdTypeCv, declaredParameterType);
									fTCModel.createEqualsConstraint(argElementCv, elementCv);
								}
								if (receiver != null) {
									//e.g. Collection<E>: boolean addAll(Collection<? extends E> c)
									CollectionElementVariable2 argElementCv= fTCModel.getElementVariable(argCv, typeParameters[ta]);
									ConstraintVariable2 expressionCv= getConstraintVariable(receiver);
									ConstraintVariable2 elementCv= fTCModel.getElementVariable(expressionCv, declaredParameterType);
									fTCModel.createSubtypeConstraint(argElementCv, elementCv);
								} else {
									//TODO: ???
								}
							}
						
						} else {
							//TODO
						}
						
					} else if (typeArgument.isTypeVariable()) {
						if (createdType != null) {
							CollectionElementVariable2 argElementCv= fTCModel.getElementVariable(argCv, typeParameters[ta]);
							ConstraintVariable2 createdTypeCv= getConstraintVariable(createdType);
							ConstraintVariable2 elementCv= fTCModel.getElementVariable(createdTypeCv, typeArgument);
							fTCModel.createEqualsConstraint(argElementCv, elementCv);
						}
						if (receiver != null) {
							CollectionElementVariable2 argElementCv= fTCModel.getElementVariable(argCv, typeParameters[ta]);
							ConstraintVariable2 expressionCv= getConstraintVariable(receiver);
							ConstraintVariable2 elementCv= fTCModel.getElementVariable(expressionCv, typeArgument);
							fTCModel.createEqualsConstraint(argElementCv, elementCv);
						} else {
							//TODO: ???
						}
						
						
					} else {
						//TODO
					}
				}
//				// Elem[arg] <= Elem[receiver]
//				fTCModel.createSubtypeConstraint(argElementCv, elementCv);
				
			//TODO:
//			} else if (/*isOneParameterGenericType(methodBinding.getDeclaringClass()) && */declaredParameterType.isParameterizedType()) {
//				if (receiver == null) //TODO: ???
//					continue;
//				//e.g. Collection<E>: boolean addAll(Collection<? extends E> c)
//				TypeConstraintVariable2 expressionCv= (TypeConstraintVariable2) getConstraintVariable(receiver);
//				Expression arg= (Expression) arguments.get(i);
//				ConstraintVariable2 argCv= getConstraintVariable(arg);
//				CollectionElementVariable2 argElementCv= fTCModel.getElementVariable(argCv);
//				CollectionElementVariable2 elementCv= fTCModel.makeElementVariable(expressionCv);
//				// Elem[arg] <= Elem[receiver]
//				fTCModel.createSubtypeConstraint(argElementCv, elementCv);

			} else { //TODO: not else, but always? Other kinds of type references?
				ITypeBinding parameterTypeBinding= declaredParameterTypes[i];
				if (! fTCModel.isAGenericType(parameterTypeBinding))
					continue;
				ParameterTypeVariable2 parameterTypeCv= fTCModel.makeParameterTypeVariable(methodBinding, i, fCU);
				ConstraintVariable2 argumentCv= getConstraintVariable((ASTNode) arguments.get(i));
				if (argumentCv == null)
					continue;
				// Elem[param] =^= Elem[arg]
				createElementEqualsConstraints(parameterTypeCv, argumentCv);
			}
		}
	}

	public void endVisit(ClassInstanceCreation node) {
		Expression receiver= node.getExpression();
		Type createdType= node.getType();
		
		TypeVariable2 typeCv= (TypeVariable2) getConstraintVariable(createdType);
		setConstraintVariable(node, typeCv);
		
		IMethodBinding methodBinding= node.resolveConstructorBinding();
		Map methodTypeVariables= createMethodTypeParameters(methodBinding);
		List arguments= node.arguments();
		doVisitMethodInvocationArguments(methodBinding, arguments, receiver, methodTypeVariables, createdType);
	}
	
	public void endVisit(ReturnStatement node) {
		Expression expression= node.getExpression();
		if (expression == null)
			return;
		ConstraintVariable2 expressionCv= getConstraintVariable(expression);
		if (expressionCv == null)
			return;
		
		MethodDeclaration methodDeclaration= (MethodDeclaration) ASTNodes.getParent(node, ASTNode.METHOD_DECLARATION);
		if (methodDeclaration == null)
			return;
		IMethodBinding methodBinding= methodDeclaration.resolveBinding();
		if (methodBinding == null)
			return;
		ReturnTypeVariable2 returnTypeCv= fTCModel.makeReturnTypeVariable(methodBinding, fCU);
		
		createElementEqualsConstraints(returnTypeCv, expressionCv);
	}
	
	public void endVisit(VariableDeclarationExpression node) {
		// Constrain the types of the child VariableDeclarationFragments to be equal to one
		// another, since the initializers in a 'for' statement can only have one type.
		// Pairwise constraints between adjacent variables is enough.
		Type type= node.getType();
		ConstraintVariable2 typeCv= getConstraintVariable(type);
		if (typeCv == null)
			return;
		
		setConstraintVariable(node, typeCv);
		
		List fragments= node.fragments();
		for (Iterator iter= fragments.iterator(); iter.hasNext();) {
			VariableDeclarationFragment fragment= (VariableDeclarationFragment) iter.next();
			ConstraintVariable2 fragmentCv= getConstraintVariable(fragment);
			createElementEqualsConstraints(typeCv, fragmentCv);
		}
	}
	
	public void endVisit(VariableDeclarationStatement node) {
		// TODO: in principle, no need to tie the VariableDeclarationFragments together.
		// The VariableDeclarationExpression can be split up when fragments get different types.
		// Warning: still need to connect fragments with type!
		endVisitFieldVariableDeclaration(node.getType(), node.fragments());
	}

	public void endVisit(FieldDeclaration node) {
		// TODO: in principle, no need to tie the VariableDeclarationFragments together.
		// The FieldDeclaration can be split up when fragments get different types.
		// Warning: still need to connect fragments with type!
		endVisitFieldVariableDeclaration(node.getType(), node.fragments());
	}
	
	private void endVisitFieldVariableDeclaration(Type type, List variableDeclarationFragments) {
		ConstraintVariable2 typeCv= getConstraintVariable(type);
		if (typeCv == null)
			return;
		
		for (Iterator iter= variableDeclarationFragments.iterator(); iter.hasNext();) {
			VariableDeclarationFragment fragment= (VariableDeclarationFragment) iter.next();
			ConstraintVariable2 fragmentCv= getConstraintVariable(fragment);
			createElementEqualsConstraints(typeCv, fragmentCv);
		}
	}

	public void endVisit(SingleVariableDeclaration node) {
		// used for formal method parameters and catch clauses
		//TODO: extra dimensions?
		
//		ConstraintVariable2 typeCv= getConstraintVariable(node.getType()); //TODO: who needs this?
		
//		ConstraintVariable2 nameCv;
//		switch (node.getParent().getNodeType()) {
//			case ASTNode.METHOD_DECLARATION :
//				MethodDeclaration parent= (MethodDeclaration) node.getParent();
//				int index= parent.parameters().indexOf(node);
//				nameCv= fTCFactory.makeParameterTypeVariable(parent.resolveBinding(), index, node.getType());
//				//store source range even if variable not used in constraint here. TODO: move to visit(MethodDeclaration)?
//				break;
//			case ASTNode.CATCH_CLAUSE :
//				nameCv= fTCFactory.makeVariableVariable(node.resolveBinding());
//			
//				break;
//			default:
//				unexpectedNode(node.getParent());
//		}
//		setConstraintVariable(node, nameCv);
		
		//TODO: Move this into visit(SimpleName) or leave it here?
//		ExpressionVariable2 name= fTCFactory.makeExpressionVariable(node.getName());
//		TypeVariable2 type= fTCFactory.makeTypeVariable(node.getType());
//		ITypeConstraint2[] nameEqualsType= fTCFactory.createEqualsConstraint(name, type);
//		addConstraints(nameEqualsType);
		
		//TODO: When can a SingleVariableDeclaration have an initializer? Never up to Java 1.5?
		Expression initializer= node.getInitializer();
		if (initializer == null)
			return;
		
		ConstraintVariable2 initializerCv= getConstraintVariable(initializer);
		ConstraintVariable2 nameCv= getConstraintVariable(node);
		//TODO: check: property has been set in visit(CatchClause), visit(MethodDeclaration), visit(EnhancedForStatament)
		//fTCFactory.createSubtypeConstraint(initializerCv, nameCv); //TODO: not for augment raw container clients
	}
	
	public void endVisit(VariableDeclarationFragment node) {
		VariableVariable2 cv= fTCModel.makeDeclaredVariableVariable(node.resolveBinding(), fCU);
		setConstraintVariable(node, cv);
		
		//TODO: prune unused CV for local variables (but not fields)
		
		Expression initializer= node.getInitializer();
		if (initializer == null)
			return;
		
		ConstraintVariable2 initializerCv= getConstraintVariable(initializer);
		if (initializerCv == null)
			return;
		
		createElementEqualsConstraints(cv, initializerCv);
		
		// name= initializer  -->  [initializer] <= [name]
//		if (initializerCv instanceof CollectionElementVariable2)
//			fTCModel.createSubtypeConstraint(initializerCv, cv);
	}
	
	//--------- private helpers ----------------//
	
	public InferTypeArgumentsTCModel getTCModel() {
		return fTCModel;
	}
	
	/**
	 * @param node the ASTNode
	 * @return the {@link ConstraintVariable2} associated with the node, or <code>null</code>
	 */
	protected ConstraintVariable2 getConstraintVariable(ASTNode node) {
		//TODO: make static?
		return (ConstraintVariable2) node.getProperty(CV_PROP);
	}
	
	/**
	 * @param node the ASTNode
	 * @param constraintVariable the {@link ConstraintVariable2} to be associated with node
	 */
	protected void setConstraintVariable(ASTNode node, ConstraintVariable2 constraintVariable) {
		node.setProperty(CV_PROP, constraintVariable);
	}
	
	private void createElementEqualsConstraints(ConstraintVariable2 cv, ConstraintVariable2 initializerCv) {
		Map leftElements= fTCModel.getElementVariables(cv);
		Map rightElements= fTCModel.getElementVariables(initializerCv);
		for (Iterator leftIter= leftElements.entrySet().iterator(); leftIter.hasNext();) {
			Map.Entry leftEntry= (Map.Entry) leftIter.next();
			String leftTypeVariableKey= (String) leftEntry.getKey();
			CollectionElementVariable2 rightElementVariable= (CollectionElementVariable2) rightElements.get(leftTypeVariableKey);
			if (rightElementVariable != null) {
				CollectionElementVariable2 leftElementVariable= (CollectionElementVariable2) leftEntry.getValue();
				fTCModel.createEqualsConstraint(leftElementVariable, rightElementVariable);
			}
		}
	}
	
	private void createElementSubtypeConstraints(ConstraintVariable2 cv, ConstraintVariable2 initializerCv) {
		Map leftElements= fTCModel.getElementVariables(cv);
		Map rightElements= fTCModel.getElementVariables(initializerCv);
		for (Iterator leftIter= leftElements.entrySet().iterator(); leftIter.hasNext();) {
			Map.Entry leftEntry= (Map.Entry) leftIter.next();
			String leftTypeVariableKey= (String) leftEntry.getKey();
			CollectionElementVariable2 rightElementVariable= (CollectionElementVariable2) rightElements.get(leftTypeVariableKey);
			if (rightElementVariable != null) {
				CollectionElementVariable2 leftElementVariable= (CollectionElementVariable2) leftEntry.getValue();
				fTCModel.createSubtypeConstraint(leftElementVariable, rightElementVariable);
			}
		}
	}

	private void logUnexpectedNode(ASTNode node, String msg) {
		String message= msg == null ? "" : msg + ":\n";  //$NON-NLS-1$//$NON-NLS-2$
		if (node == null) {
			message+= "ASTNode was not expected to be null"; //$NON-NLS-1$
		} else {
			message+= "Found unexpected node (type: " + node.getNodeType() + "):\n" + node.toString(); //$NON-NLS-1$ //$NON-NLS-2$
		}
		JavaPlugin.log(new Exception(message).fillInStackTrace());
	}

}
