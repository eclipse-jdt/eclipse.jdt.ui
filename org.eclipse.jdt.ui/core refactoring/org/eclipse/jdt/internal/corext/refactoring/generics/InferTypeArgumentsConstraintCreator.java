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

import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor;
import org.eclipse.jdt.internal.corext.refactoring.rename.MethodChecks;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.InferTypeArgumentsTCModel;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.CollectionElementVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ConstraintVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ParameterTypeVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.PlainTypeVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ReturnTypeVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.TypeBindings;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.TypeConstraintVariable2;
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
	

	public InferTypeArgumentsConstraintCreator(InferTypeArgumentsTCModel model) {
		fTCModel= model;
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
		//TODO: who needs this?
		TypeVariable2 typeVariable= fTCModel.makeTypeVariable(node);
		if (typeVariable == null)
			return;
		
		setConstraintVariable(node, typeVariable);
		if (fTCModel.isAGenericType(typeVariable.getTypeBinding()))
			fTCModel.makeElementVariable(typeVariable);
	}
	
	public boolean visit(SimpleName node) {
		IBinding binding= node.resolveBinding();
		if (binding instanceof IVariableBinding) {
			VariableVariable2 cv= fTCModel.makeVariableVariable((IVariableBinding) binding);
			setConstraintVariable(node, cv);
		}
		// TODO else?
		return super.visit(node);
	}
	
	public void endVisit(Assignment node) {
		Expression lhs= node.getLeftHandSide();
		Expression rhs= node.getRightHandSide();
		
		ConstraintVariable2 left= getConstraintVariable(lhs);
		ConstraintVariable2 right= getConstraintVariable(rhs);
		if (left == null || right == null)
			return;
		
		Assignment.Operator op= node.getOperator();
		if (op == Assignment.Operator.PLUS_ASSIGN && TypeBindings.equals(lhs.resolveTypeBinding(), TypeBindings.getStringBinding(node))) {
			//Special handling for automatic String conversion: do nothing; the RHS can be anything.
		} else {
			CollectionElementVariable2 leftElement= fTCModel.getElementVariable(left);
			CollectionElementVariable2 rightElement= fTCModel.getElementVariable(right);
			fTCModel.createEqualsConstraint(leftElement, rightElement);
			
//			if (left instanceof CollectionElementVariable2 || right instanceof CollectionElementVariable2)
//				fTCModel.createSubtypeConstraint(right, left); // left= right;  -->  [right] <= [left]
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
		if (! (expressionCv instanceof TypeConstraintVariable2))
			return;
		fTCModel.makeCastVariable(node, (TypeConstraintVariable2) expressionCv);
		
		boolean eitherIsIntf= type.resolveBinding().isInterface() || expression.resolveTypeBinding().isInterface();
		if (eitherIsIntf)
			return;
		
		//TODO: preserve up- and down-castedness!
		
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
		PlainTypeVariable2 cv= fTCModel.makePlainTypeVariable(typeBinding);
		setConstraintVariable(node, cv);
	}
	
	public void endVisit(TypeLiteral node) {
//		ITypeBinding typeBinding= node.resolveTypeBinding();
//		PlainTypeVariable2 cv= fTCModel.makePlainTypeVariable(typeBinding);
//		fTCModel.makeElementVariable(cv);
//		setConstraintVariable(node, cv);
	}
	
	public void endVisit(MethodDeclaration node) {
		IMethodBinding methodBinding= node.resolveBinding();

		if (methodBinding == null)
			return; //TODO: emit error?
		
		int count= node.parameters().size();
		CollectionElementVariable2[] parameterElementCvs= new CollectionElementVariable2[count];
		for (int i= 0; i < count; i++) {
			SingleVariableDeclaration paramDecl= (SingleVariableDeclaration) node.parameters().get(i);
			//parameterTypeVariable currently not used, but need to register in order to store source range
			TypeConstraintVariable2 parameterTypeVariable= fTCModel.makeDeclaredParameterTypeVariable(methodBinding, i, fCU);
			//creating equals constraint between parameterTypeVariable's elements and the Type's elements
			//TODO: should maybe avoid creating Type's ConstraintVariables
			CollectionElementVariable2 parameterElementCv= fTCModel.makeElementVariable(parameterTypeVariable);
			if (parameterElementCv == null)
				continue;
			
			parameterElementCvs[i]= parameterElementCv;
			ConstraintVariable2 typeCv= getConstraintVariable(paramDecl.getType());
			CollectionElementVariable2 typeElementCv= fTCModel.getElementVariable(typeCv);
			fTCModel.createEqualsConstraint(parameterElementCv, typeElementCv);
			
			//TODO: should avoid having a VariableVariable as well as a ParameterVariable for a parameter
			ConstraintVariable2 nameCv= getConstraintVariable(paramDecl.getName());
			CollectionElementVariable2 nameElementCv= fTCModel.getElementVariable(nameCv);
			fTCModel.createEqualsConstraint(parameterElementCv, nameElementCv);
		}
		
		CollectionElementVariable2 returnTypeElementCv= null;
		if (! methodBinding.isConstructor()) {
			TypeConstraintVariable2 returnTypeBindingCv= fTCModel.makeDeclaredReturnTypeVariable(methodBinding, fCU);
			if (returnTypeBindingCv != null) {
				TypeConstraintVariable2 returnTypeCv= (TypeConstraintVariable2) getConstraintVariable(node.getReturnType2());
				CollectionElementVariable2 returnTypeBindingElementCv= fTCModel.makeElementVariable(returnTypeBindingCv);
				returnTypeElementCv= fTCModel.getElementVariable(returnTypeCv);
				fTCModel.createEqualsConstraint(returnTypeBindingElementCv, returnTypeElementCv);
			}
		}
		if (MethodChecks.isVirtual(methodBinding)) {
			//TODO: RippleMethod constraints for corner cases: see testCuRippleMethods3, bug 41989
			addConstraintsForOverriding(methodBinding, returnTypeElementCv, parameterElementCvs);
		}
	}
	
	private void addConstraintsForOverriding(IMethodBinding methodBinding, CollectionElementVariable2 returnTypeElementCv, CollectionElementVariable2[] parameterElementCvs) {
		boolean hasParameterElementCvs= false;;
		for (int i= 0; i < parameterElementCvs.length; i++)
			if (parameterElementCvs[i] != null)
				hasParameterElementCvs= true;
		
		if (returnTypeElementCv == null && ! hasParameterElementCvs)
			return;
		
		String name= methodBinding.getName();
		ITypeBinding[] parameterTypes= methodBinding.getParameterTypes();
		
		ITypeBinding[] allSuperTypes= Bindings.getAllSuperTypes(methodBinding.getDeclaringClass());
		for (int i= 0; i < allSuperTypes.length; i++) {
			ITypeBinding superType= allSuperTypes[i];
			IMethodBinding superMethod= Bindings.findMethodInType(superType, name, parameterTypes);
			if (superMethod == null)
				continue;
			
			for (int p= 0; p < parameterElementCvs.length; p++) {
				if (parameterElementCvs[p] == null)
					continue;
				ParameterTypeVariable2 parameterTypeVariable= fTCModel.makeParameterTypeVariable(superMethod, p);
				CollectionElementVariable2 superParameterElementCv= fTCModel.makeElementVariable(parameterTypeVariable);
				fTCModel.createEqualsConstraint(superParameterElementCv, parameterElementCvs[p]);
			}
			
			if (returnTypeElementCv != null) {
				ReturnTypeVariable2 returnTypeVariable= fTCModel.makeReturnTypeVariable(superMethod);
				CollectionElementVariable2 superReturnElementCv= fTCModel.makeElementVariable(returnTypeVariable);
				fTCModel.createEqualsConstraint(superReturnElementCv, returnTypeElementCv);
			}
		}
	}

	public void endVisit(MethodInvocation node) {
		IMethodBinding methodBinding= node.resolveMethodBinding();
		if (methodBinding == null)
			return;
		
		Expression receiver= node.getExpression();
		//TODO: Expression can be null when visiting a non-special method in a subclass of a container type.
		
		doVisitMethodInvocationReturnType(node, methodBinding, receiver);
		List arguments= node.arguments();
		doVisitMethodInvocationArguments(methodBinding, arguments, receiver);
		
	}
	
	private void doVisitMethodInvocationReturnType(MethodInvocation node, IMethodBinding methodBinding, Expression receiver) {
		ITypeBinding genericReturnType= methodBinding.getErasure().getReturnType();
		if (methodBinding.getErasure().getTypeParameters().length == 1 &&
				(genericReturnType.isTypeVariable() || genericReturnType.isWildcardType()) &&
				methodBinding.getParameterTypes().length == 1 &&
				methodBinding.getParameterTypes()[0].getErasure().isGenericType()) {
			// e.g. in Collections: <T ..> T min(Collection<? extends T> coll):
			TypeConstraintVariable2 argCv= (TypeConstraintVariable2) getConstraintVariable((Expression) node.arguments().get(0));
			ConstraintVariable2 elementCv= fTCModel.makeElementVariable(argCv);
			// [retVal] =^= Elem[arg]:
			setConstraintVariable(node, elementCv); //TODO: should be [retVal] <= Elem[arg]
			
		} else if (isOneParameterGenericType(methodBinding.getDeclaringClass()) &&
				(genericReturnType.isTypeVariable() || genericReturnType.isWildcardType())) {
			if (receiver == null) //TODO: ???
				return;
			// e.g. in List<E>: E get(int index):
			TypeConstraintVariable2 expressionCv= (TypeConstraintVariable2) getConstraintVariable(receiver);
			ConstraintVariable2 elementCv= fTCModel.makeElementVariable(expressionCv);
			// [retVal] =^= Elem[receiver]:
			setConstraintVariable(node, elementCv);
			
		} else if (isOneParameterGenericType(methodBinding.getDeclaringClass()) && genericReturnType.isParameterizedType()) {
			if (receiver == null) //TODO: ???
				return;
			//e.g. List<E>: Iterator<E> iterator()
			ITypeBinding[] typeArguments= genericReturnType.getTypeArguments();
			if (typeArguments.length == 1 && 
					(typeArguments[0].isTypeVariable() || typeArguments[0].isWildcardType())) { //TODO: wildcard, more than one type arg, ...
				TypeConstraintVariable2 expressionCv= (TypeConstraintVariable2) getConstraintVariable(receiver);
				fTCModel.makeElementVariable(expressionCv); //TODO: needs index for element
				
				//TODO: retValCv has wrong raw type, but not used (refactoring only considers element types).
				// Should avoid creating in the first place!
				TypeConstraintVariable2 retValCv= expressionCv;
				// Elem[retVal] =^= Elem[receiver]
				setConstraintVariable(node, retValCv);
			}
			
		} else {
			ReturnTypeVariable2 returnTypeCv= fTCModel.makeReturnTypeVariable(methodBinding);
			if (returnTypeCv != null) {
				fTCModel.makeElementVariable(returnTypeCv);
				setConstraintVariable(node, returnTypeCv);
			}
		}
	}

	private boolean isOneParameterGenericType(ITypeBinding type) {
		//TODO: remove this temporary restriction
		return type.getErasure().getTypeParameters().length == 1;
	}

	private void doVisitMethodInvocationArguments(IMethodBinding methodBinding, List arguments, Expression receiver) {
		ITypeBinding[] parameterTypes= methodBinding.getParameterTypes();
		for (int i= 0; i < parameterTypes.length; i++) {
			ITypeBinding genericArgumentType= methodBinding.getErasure().getParameterTypes()[i];
			if (isOneParameterGenericType(methodBinding.getDeclaringClass()) && 
					(genericArgumentType.isTypeVariable() || genericArgumentType.isWildcardType())) {
				if (receiver == null) //TODO: ???
					continue;
				//e.g. Collection<E>: boolean add(E o)
				TypeConstraintVariable2 expressionCv= (TypeConstraintVariable2) getConstraintVariable(receiver);
				Expression arg= (Expression) arguments.get(i);
				ConstraintVariable2 argCv= getConstraintVariable(arg);
				ConstraintVariable2 elementCv= fTCModel.makeElementVariable(expressionCv);
				// [arg] <= Elem[receiver]:
				fTCModel.createSubtypeConstraint(argCv, elementCv);
			
			} else if (isOneParameterGenericType(methodBinding.getDeclaringClass()) && genericArgumentType.isParameterizedType()) {
				if (receiver == null) //TODO: ???
					continue;
				//e.g. Collection<E>: boolean addAll(Collection<? extends E> c)
				TypeConstraintVariable2 expressionCv= (TypeConstraintVariable2) getConstraintVariable(receiver);
				Expression arg= (Expression) arguments.get(i);
				ConstraintVariable2 argCv= getConstraintVariable(arg);
				CollectionElementVariable2 argElementCv= fTCModel.getElementVariable(argCv);
				CollectionElementVariable2 elementCv= fTCModel.makeElementVariable(expressionCv);
				// Elem[arg] <= Elem[receiver]
				fTCModel.createSubtypeConstraint(argElementCv, elementCv);

			} else { //TODO: not else, but always?
				ITypeBinding parameterTypeBinding= parameterTypes[i];
				if (! fTCModel.isAGenericType(parameterTypeBinding))
					continue;
				ParameterTypeVariable2 parameterTypeCv= fTCModel.makeParameterTypeVariable(methodBinding, i);
				ConstraintVariable2 argumentCv= getConstraintVariable((ASTNode) arguments.get(i));
				if (argumentCv == null)
					continue;
				CollectionElementVariable2 parameterElementCv= fTCModel.makeElementVariable(parameterTypeCv);
				CollectionElementVariable2 argumentElementCv= fTCModel.getElementVariable(argumentCv); //TODO: argument(Element)Cv is only visible inside the method body. Prune!
				// Elem[param] =^= Elem[arg]
				fTCModel.createEqualsConstraint(parameterElementCv, argumentElementCv);
			}
		}
	}

	public void endVisit(ClassInstanceCreation node) {
		Expression receiver= node.getExpression();
		
		TypeVariable2 typeCv= (TypeVariable2) getConstraintVariable(node.getType());
		setConstraintVariable(node, typeCv);
		
		IMethodBinding methodBinding= node.resolveConstructorBinding();
		List arguments= node.arguments();
		doVisitMethodInvocationArguments(methodBinding, arguments, receiver);
		//TODO: return type?
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
		ReturnTypeVariable2 returnTypeCv= fTCModel.makeReturnTypeVariable(methodBinding);
		
		CollectionElementVariable2 returnTypeElementCv= fTCModel.makeElementVariable(returnTypeCv);
		CollectionElementVariable2 expressionElementCv= fTCModel.getElementVariable(expressionCv);
		fTCModel.createEqualsConstraint(returnTypeElementCv, expressionElementCv);
	}
	
	public void endVisit(VariableDeclarationExpression node) {
		// Constrain the types of the child VariableDeclarationFragments to be equal to one
		// another, since the initializers in a 'for' statement can only have one type.
		// Pairwise constraints between adjacent variables is enough.
		Type type= node.getType();
		ConstraintVariable2 typeCv= getConstraintVariable(type);
		if (typeCv == null)
			return;
		CollectionElementVariable2 typeElement= fTCModel.getElementVariable(typeCv);
		if (typeElement == null)
			return;
		
		setConstraintVariable(node, typeCv);
		
		List fragments= node.fragments();
		for (Iterator iter= fragments.iterator(); iter.hasNext();) {
			VariableDeclarationFragment fragment= (VariableDeclarationFragment) iter.next();
			ConstraintVariable2 fragmentCv= getConstraintVariable(fragment);
			CollectionElementVariable2 fragmentElement= fTCModel.getElementVariable(fragmentCv);
			fTCModel.createEqualsConstraint(typeElement, fragmentElement); //TODO: batch
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
		CollectionElementVariable2 typeElement= fTCModel.getElementVariable(typeCv);
		if (typeElement == null)
			return;
		
		for (Iterator iter= variableDeclarationFragments.iterator(); iter.hasNext();) {
			VariableDeclarationFragment fragment= (VariableDeclarationFragment) iter.next();
			ConstraintVariable2 fragmentCv= getConstraintVariable(fragment);
			CollectionElementVariable2 fragmentElement= fTCModel.getElementVariable(fragmentCv);
			fTCModel.createEqualsConstraint(typeElement, fragmentElement); //TODO: batch
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
		
		CollectionElementVariable2 leftElement= fTCModel.getElementVariable(cv);
		CollectionElementVariable2 rightElement= fTCModel.getElementVariable(initializerCv);
		fTCModel.createEqualsConstraint(leftElement, rightElement);
		
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
	
	private void unexpectedNode(ASTNode node) {
		ICompilationUnit cu= RefactoringASTParser.getCompilationUnit(node);
		JavaPlugin.logErrorMessage((cu == null ? "" : cu.getElementName() + " - ")  //$NON-NLS-1$//$NON-NLS-2$
				+ node.getNodeType() + ": " + node.toString()); //$NON-NLS-1$
	}

}
