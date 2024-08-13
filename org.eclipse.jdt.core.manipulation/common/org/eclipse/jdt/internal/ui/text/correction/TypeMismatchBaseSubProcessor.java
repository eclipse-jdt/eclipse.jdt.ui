/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Benjamin Muskalla <bmuskalla@eclipsesource.com> - [quick fix] proposes wrong cast from Object to primitive int - https://bugs.eclipse.org/bugs/show_bug.cgi?id=100593
 *     Benjamin Muskalla <bmuskalla@eclipsesource.com> - [quick fix] "Add exceptions to..." quickfix does nothing - https://bugs.eclipse.org/bugs/show_bug.cgi?id=107924
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NameQualifiedType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.core.manipulation.BindingLabelProviderCore;
import org.eclipse.jdt.internal.core.manipulation.JavaElementLabelsCore;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.core.manipulation.dom.ASTResolving;
import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.text.correction.proposals.ChangeMethodSignatureProposalCore.ChangeDescription;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ChangeMethodSignatureProposalCore.InsertDescription;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ChangeMethodSignatureProposalCore.RemoveDescription;
import org.eclipse.jdt.internal.ui.text.correction.proposals.NewVariableCorrectionProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.OptionalCorrectionProposalCore;
import org.eclipse.jdt.internal.ui.util.ASTHelper;


public abstract class TypeMismatchBaseSubProcessor<T> {

	protected TypeMismatchBaseSubProcessor() {
	}

	public void collectTypeMismatchProposals(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) throws CoreException {
		String[] args= problem.getProblemArguments();
		if (args.length != 2) {
			return;
		}

		ICompilationUnit cu= context.getCompilationUnit();

		CompilationUnit astRoot= context.getASTRoot();
		AST ast= astRoot.getAST();

		ASTNode selectedNode= problem.getCoveredNode(astRoot);
		if (!(selectedNode instanceof Expression nodeToCast)) {
			return;
		}
		Name receiverNode= null;
		ITypeBinding castTypeBinding= null;

		int parentNodeType= selectedNode.getParent().getNodeType();
		switch (parentNodeType) {
			case ASTNode.ASSIGNMENT:
				Assignment assign= (Assignment) selectedNode.getParent();
				Expression leftHandSide= assign.getLeftHandSide();
				if (selectedNode.equals(leftHandSide)) {
					nodeToCast= assign.getRightHandSide();
				}
				castTypeBinding= assign.getLeftHandSide().resolveTypeBinding();
				if (leftHandSide instanceof Name name2) {
					receiverNode= name2;
				} else if (leftHandSide instanceof FieldAccess fieldAccess) {
					receiverNode= fieldAccess.getName();
				}
				break;
			case ASTNode.VARIABLE_DECLARATION_FRAGMENT:
				VariableDeclarationFragment frag= (VariableDeclarationFragment) selectedNode.getParent();
				if (selectedNode.equals(frag.getName()) || selectedNode.equals(frag.getInitializer())) {
					nodeToCast= frag.getInitializer();
					castTypeBinding= ASTNodes.getType(frag).resolveBinding();
					receiverNode= frag.getName();
				}
				break;
			case ASTNode.MEMBER_VALUE_PAIR:
				receiverNode= ((MemberValuePair) selectedNode.getParent()).getName();
				castTypeBinding= ASTResolving.guessBindingForReference(nodeToCast);
				break;
			case ASTNode.SINGLE_MEMBER_ANNOTATION:
				receiverNode= ((SingleMemberAnnotation) selectedNode.getParent()).getTypeName(); // use the type name
				castTypeBinding= ASTResolving.guessBindingForReference(nodeToCast);
				break;
			default:
				// try to find the binding corresponding to 'castTypeName'
				castTypeBinding= ASTResolving.guessBindingForReference(nodeToCast);
				break;
		}
		if (castTypeBinding == null) {
			return;
		}

		ITypeBinding currBinding= nodeToCast.resolveTypeBinding();
		if (currBinding == null && nodeToCast instanceof MethodInvocation methodInvoc) {
			IMethodBinding methodBinding= methodInvoc.resolveMethodBinding();
			if (methodBinding != null) {
				currBinding= methodBinding.getReturnType();
			}
		}

		if (!(nodeToCast instanceof ArrayInitializer)) {
			String castTypeName= castTypeBinding.getErasure().getQualifiedName();
			if ("java.util.Optional".equals(castTypeName) && ast.apiLevel() >= ASTHelper.JLS8) { //$NON-NLS-1$
				ITypeBinding nodeToCastTypeBinding= nodeToCast.resolveTypeBinding();
				String label0= Messages.format(CorrectionMessages.TypeMismatchSubProcessor_changetooptionalempty_description, nodeToCast.toString());
				T prop1= createOptionalProposal(label0, cu, nodeToCast, IProposalRelevance.CREATE_EMPTY_OPTIONAL, OptionalCorrectionProposalCore.OPTIONAL_EMPTY);
				if (prop1 != null)
					proposals.add(prop1);
				ITypeBinding[] typeArguments= castTypeBinding.getTypeArguments();
				boolean wrapAll= false;
				for (ITypeBinding typeArgument : typeArguments) {
					if (typeArgument.isCastCompatible(nodeToCastTypeBinding)) {
						wrapAll= true;
						break;
					}
				}
				if (wrapAll) {
					String label1= Messages.format(CorrectionMessages.TypeMismatchSubProcessor_changetooptionalof_description, nodeToCast.toString());
					T prop2= createOptionalProposal(label1, cu, nodeToCast, IProposalRelevance.CREATE_OPTIONAL, OptionalCorrectionProposalCore.OPTIONAL_OF);
					if (prop2 != null)
						proposals.add(prop2);
					if (!nodeToCastTypeBinding.isPrimitive()) {
						String label2= Messages.format(CorrectionMessages.TypeMismatchSubProcessor_changetooptionalofnullable_description, nodeToCast.toString());
						T prop3= createOptionalProposal(label2, cu, nodeToCast, IProposalRelevance.CREATE_OPTIONAL_OF_NULLABLE, OptionalCorrectionProposalCore.OPTIONAL_OF_NULLABLE);
						if (prop3 != null)
							proposals.add(prop3);
					}
				}
			}

			ITypeBinding castFixType= null;
			if (currBinding == null || castTypeBinding.isCastCompatible(currBinding) || nodeToCast instanceof CastExpression) {
				castFixType= castTypeBinding;
			} else if (JavaModelUtil.is50OrHigher(cu.getJavaProject())) {
				ITypeBinding boxUnboxedTypeBinding= boxOrUnboxPrimitives(castTypeBinding, currBinding, ast);
				if (boxUnboxedTypeBinding != castTypeBinding && boxUnboxedTypeBinding.isCastCompatible(currBinding)) {
					castFixType= boxUnboxedTypeBinding;
				}
			}
			if (castFixType != null) {
				proposals.add(collectCastProposals(context, castFixType, nodeToCast, IProposalRelevance.CREATE_CAST));
			}
		}

		boolean nullOrVoid= currBinding == null || "void".equals(currBinding.getName()); //$NON-NLS-1$

		// change method return statement to actual type
		if (!nullOrVoid && isTypeReturned(nodeToCast)) {
			BodyDeclaration decl= ASTResolving.findParentBodyDeclaration(selectedNode);
			if (decl instanceof MethodDeclaration methodDeclaration) {
				currBinding= Bindings.normalizeTypeBinding(currBinding);
				if (currBinding == null) {
					currBinding= ast.resolveWellKnownType("java.lang.Object"); //$NON-NLS-1$
				}
				if (currBinding.isWildcardType()) {
					currBinding= ASTResolving.normalizeWildcardType(currBinding, true, ast);
				}

				ASTRewrite rewrite= ASTRewrite.create(ast);

				String label= Messages.format(CorrectionMessages.TypeMismatchSubProcessor_changereturntype_description, BasicElementLabels.getJavaElementName(currBinding.getName()));
				T proposal= createChangeReturnTypeProposal(label, cu, rewrite, IProposalRelevance.CHANGE_METHOD_RETURN_TYPE, currBinding, ast, astRoot, methodDeclaration, decl);
				proposals.add(proposal);
			}
		}

		if (!nullOrVoid && receiverNode != null) {
			currBinding= Bindings.normalizeTypeBinding(currBinding);
			if (currBinding == null) {
				currBinding= ast.resolveWellKnownType("java.lang.Object"); //$NON-NLS-1$
			}
			if (currBinding.isWildcardType()) {
				currBinding= ASTResolving.normalizeWildcardType(currBinding, true, ast);
			}
			collectChangeSenderTypeProposals(context, receiverNode, currBinding, true, IProposalRelevance.CHANGE_TYPE_OF_RECEIVER_NODE, proposals);
		}

		collectChangeSenderTypeProposals(context, nodeToCast, castTypeBinding, false, IProposalRelevance.CHANGE_TYPE_OF_NODE_TO_CAST, proposals);

		if (castTypeBinding == ast.resolveWellKnownType("boolean") && currBinding != null && !currBinding.isPrimitive() && !Bindings.isVoidType(currBinding)) { //$NON-NLS-1$
			String label= CorrectionMessages.TypeMismatchSubProcessor_insertnullcheck_description;
			ASTRewrite rewrite= ASTRewrite.create(astRoot.getAST());

			InfixExpression expression= ast.newInfixExpression();
			expression.setLeftOperand((Expression) rewrite.createMoveTarget(nodeToCast));
			expression.setRightOperand(ast.newNullLiteral());
			expression.setOperator(InfixExpression.Operator.NOT_EQUALS);
			rewrite.replace(nodeToCast, expression, null);
			T prop= createInsertNullCheckProposal(label, context.getCompilationUnit(), rewrite, IProposalRelevance.INSERT_NULL_CHECK);
			if (prop != null)
				proposals.add(prop);
		}
	}

	private static boolean isTypeReturned(final Expression nodeToCast) {
		int parentNodeType= nodeToCast.getParent().getNodeType();

		if (parentNodeType == ASTNode.RETURN_STATEMENT) {
			return true;
		}

		if (parentNodeType == ASTNode.PARENTHESIZED_EXPRESSION
				|| parentNodeType == ASTNode.CONDITIONAL_EXPRESSION && (nodeToCast.getLocationInParent() == ConditionalExpression.THEN_EXPRESSION_PROPERTY || nodeToCast.getLocationInParent() == ConditionalExpression.ELSE_EXPRESSION_PROPERTY)) {
			return isTypeReturned((Expression) nodeToCast.getParent());
		}

		return false;
	}

	public static ITypeBinding boxOrUnboxPrimitives(ITypeBinding castType, ITypeBinding toCast, AST ast) {
		/*
		 * e.g:
		 * 	void m(toCast var) {
		 * 		castType i= var;
		 * 	}
		 */
		if (castType.isPrimitive() && !toCast.isPrimitive()) {
			return Bindings.getBoxedTypeBinding(castType, ast);
		} else if (!castType.isPrimitive() && toCast.isPrimitive()) {
			return Bindings.getUnboxedTypeBinding(castType, ast);
		} else {
			return castType;
		}
	}

	public void collectChangeSenderTypeProposals(IInvocationContext context, Expression nodeToCast, ITypeBinding castTypeBinding, boolean isAssignedNode, int relevance, Collection<T> proposals) throws JavaModelException {
		IBinding callerBinding= Bindings.resolveExpressionBinding(nodeToCast, false);

		ICompilationUnit cu= context.getCompilationUnit();
		CompilationUnit astRoot= context.getASTRoot();

		ICompilationUnit targetCu= null;
		ITypeBinding declaringType= null;
		IBinding callerBindingDecl= callerBinding;
		if (callerBinding instanceof IVariableBinding variableBinding) {
			if (variableBinding.isEnumConstant()) {
				return;
			}
			if (!variableBinding.isField()) {
				targetCu= cu;
			} else {
				callerBindingDecl= variableBinding.getVariableDeclaration();
				ITypeBinding declaringClass= variableBinding.getDeclaringClass();
				if (declaringClass == null) {
					return; // array length
				}
				declaringType= declaringClass.getTypeDeclaration();
			}
		} else if (callerBinding instanceof IMethodBinding methodBinding) {
			if (!methodBinding.isConstructor()) {
				declaringType= methodBinding.getDeclaringClass().getTypeDeclaration();
				callerBindingDecl= methodBinding.getMethodDeclaration();
			}
		} else if (callerBinding instanceof ITypeBinding typeBinding2 && nodeToCast.getLocationInParent() == SingleMemberAnnotation.TYPE_NAME_PROPERTY) {
			declaringType= typeBinding2;
			callerBindingDecl= Bindings.findMethodInType(declaringType, "value", (String[]) null); //$NON-NLS-1$
			if (callerBindingDecl == null) {
				return;
			}
		}

		if (declaringType != null && declaringType.isFromSource()) {
			targetCu= ASTResolving.findCompilationUnitForBinding(cu, astRoot, declaringType);
		}
		if (targetCu != null && ASTResolving.isUseableTypeInContext(castTypeBinding, callerBindingDecl, false)) {
			T p= createChangeSenderTypeProposal(targetCu, callerBindingDecl, astRoot, castTypeBinding, isAssignedNode, relevance);
			if (p != null)
				proposals.add(p);
		}

		// add interface to resulting type
		if (!isAssignedNode) {
			ITypeBinding nodeType= nodeToCast.resolveTypeBinding();
			if (castTypeBinding.isInterface() && nodeType != null && nodeType.isClass() && !nodeType.isAnonymous() && nodeType.isFromSource()) {
				ITypeBinding typeDecl= nodeType.getTypeDeclaration();
				ICompilationUnit nodeCu= ASTResolving.findCompilationUnitForBinding(cu, astRoot, typeDecl);
				if (nodeCu != null && ASTResolving.isUseableTypeInContext(castTypeBinding, typeDecl, true)) {
					T p2= createImplementInterfaceProposal(nodeCu, typeDecl, astRoot, castTypeBinding, relevance - 1);
					if (p2 != null)
						proposals.add(p2);
				}
			}

			if (nodeToCast.getNodeType() == ASTNode.CLASS_INSTANCE_CREATION) {
				ASTNode constructorNode = context.getCoveringNode();
				if(!(constructorNode instanceof ClassInstanceCreation)) {
					constructorNode = ASTNodes.getParent(constructorNode, ClassInstanceCreation.class);
				}
				T p3= createChangeConstructorTypeProposal(cu, constructorNode, astRoot,
						castTypeBinding, relevance);
				if (p3 != null)
					proposals.add(p3);
			}
		}
	}

	public T collectCastProposals(IInvocationContext context, ITypeBinding castTypeBinding, Expression nodeToCast, int relevance) {
		return collectCastProposals(null, context, castTypeBinding, nodeToCast, relevance);
	}

	public T collectCastProposals(String label, IInvocationContext context, ITypeBinding castTypeBinding, Expression nodeToCast, int relevance) {
		ICompilationUnit cu= context.getCompilationUnit();
		if (label == null ) {
			String castType= BindingLabelProviderCore.getBindingLabel(castTypeBinding, JavaElementLabelsCore.ALL_DEFAULT);
			if (nodeToCast.getNodeType() == ASTNode.CAST_EXPRESSION) {
				label= Messages.format(CorrectionMessages.TypeMismatchSubProcessor_changecast_description, castType);
			} else {
				label= Messages.format(CorrectionMessages.TypeMismatchSubProcessor_addcast_description, castType);
			}
		}
		return createCastCorrectionProposal(label, cu, nodeToCast, castTypeBinding, relevance);
	}

	public void collectIncompatibleReturnTypeProposals(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) throws JavaModelException {
		CompilationUnit astRoot= context.getASTRoot();
		ASTNode selectedNode= problem.getCoveringNode(astRoot);
		if (selectedNode == null) {
			return;
		}
		MethodDeclaration decl= ASTResolving.findParentMethodDeclaration(selectedNode);
		if (decl == null) {
			return;
		}
		IMethodBinding methodDeclBinding= decl.resolveBinding();
		if (methodDeclBinding == null) {
			return;
		}

		ITypeBinding returnType= methodDeclBinding.getReturnType();
		IMethodBinding overridden= Bindings.findOverriddenMethod(methodDeclBinding, false);
		if (overridden == null || overridden.getReturnType() == returnType) {
			return;
		}


		ICompilationUnit cu= context.getCompilationUnit();
		IMethodBinding methodDecl= methodDeclBinding.getMethodDeclaration();
		ITypeBinding overriddenReturnType= overridden.getReturnType();
		// propose erasure
		if (decl.typeParameters().isEmpty() || overriddenReturnType.getTypeBounds().length == 0 || Stream.of(overriddenReturnType.getTypeBounds()).allMatch(bound -> bound.getTypeArguments().length == 0)) {
			T p1= createChangeIncompatibleReturnTypeProposal(cu, methodDecl, astRoot, overriddenReturnType.getErasure(), false, IProposalRelevance.CHANGE_RETURN_TYPE);
			if (p1 != null)
				proposals.add(p1);
		}

		// propose using (and potentially introducing) the type variable
		if (overriddenReturnType.isTypeVariable()) {
			T p2 = createChangeIncompatibleReturnTypeProposal(cu, methodDecl, astRoot, overriddenReturnType, false, IProposalRelevance.CHANGE_RETURN_TYPE);
			if (p2 != null) {
				proposals.add(p2);
			}
		}

		ICompilationUnit targetCu= cu;

		IMethodBinding overriddenDecl= overridden.getMethodDeclaration();
		ITypeBinding overridenDeclType= overriddenDecl.getDeclaringClass();

		if (overridenDeclType.isFromSource()) {
			targetCu= ASTResolving.findCompilationUnitForBinding(cu, astRoot, overridenDeclType);
			if (targetCu != null && ASTResolving.isUseableTypeInContext(returnType, overriddenDecl, false)) {
				T proposal= createChangeReturnTypeOfOverridden(targetCu, overriddenDecl, astRoot, returnType, false, IProposalRelevance.CHANGE_RETURN_TYPE_OF_OVERRIDDEN, overridenDeclType);
				if (proposal != null) {
					proposals.add(proposal);
				}
			}
		}
	}

	/*
	 				if (proposal != null) {
					if (overridenDeclType.isInterface()) {
						proposal.setDisplayName(Messages.format(CorrectionMessages.TypeMismatchSubProcessor_changereturnofimplemented_description, BasicElementLabels.getJavaElementName(overriddenDecl.getName())));
					} else {
						proposal.setDisplayName(Messages.format(CorrectionMessages.TypeMismatchSubProcessor_changereturnofoverridden_description, BasicElementLabels.getJavaElementName(overriddenDecl.getName())));
					}
	 */
	public void collectIncompatibleThrowsProposals(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) throws JavaModelException {
		CompilationUnit astRoot= context.getASTRoot();
		ASTNode selectedNode= problem.getCoveringNode(astRoot);
		if (!(selectedNode instanceof MethodDeclaration decl)) {
			return;
		}
		IMethodBinding methodDeclBinding= decl.resolveBinding();
		if (methodDeclBinding == null) {
			return;
		}

		IMethodBinding overridden= Bindings.findOverriddenMethod(methodDeclBinding, false);
		if (overridden == null) {
			return;
		}

		ICompilationUnit cu= context.getCompilationUnit();

		ITypeBinding[] methodExceptions= methodDeclBinding.getExceptionTypes();
		ITypeBinding[] definedExceptions= overridden.getExceptionTypes();

		ArrayList<ITypeBinding> undeclaredExceptions= new ArrayList<>();
		{
			ChangeDescription[] changes= new ChangeDescription[methodExceptions.length];

			for (int i= 0; i < methodExceptions.length; i++) {
				if (!isDeclaredException(methodExceptions[i], definedExceptions)) {
					changes[i]= new RemoveDescription();
					undeclaredExceptions.add(methodExceptions[i]);
				}
			}
			if (undeclaredExceptions.isEmpty()) {
				return;
			}
			String label= Messages.format(CorrectionMessages.TypeMismatchSubProcessor_removeexceptions_description, BasicElementLabels.getJavaElementName(methodDeclBinding.getName()));
			T p1= createChangeMethodSignatureProposal(label, cu, astRoot, methodDeclBinding, null, changes, IProposalRelevance.REMOVE_EXCEPTIONS);
			if (p1 != null)
				proposals.add(p1);
		}

		ITypeBinding declaringType= overridden.getDeclaringClass();
		ICompilationUnit targetCu= null;
		if (declaringType.isFromSource()) {
			targetCu= ASTResolving.findCompilationUnitForBinding(cu, astRoot, declaringType);
		}
		if (targetCu != null) {
			ChangeDescription[] changes= new ChangeDescription[definedExceptions.length + undeclaredExceptions.size()];

			for (int i= 0; i < undeclaredExceptions.size(); i++) {
				changes[i + definedExceptions.length]= new InsertDescription(undeclaredExceptions.get(i), ""); //$NON-NLS-1$
			}
			IMethodBinding overriddenDecl= overridden.getMethodDeclaration();
			String[] args= {  BasicElementLabels.getJavaElementName(declaringType.getName()), BasicElementLabels.getJavaElementName(overridden.getName()) };
			String label= Messages.format(CorrectionMessages.TypeMismatchSubProcessor_addexceptions_description, args);
			T p2= createChangeMethodSignatureProposal(label, targetCu, astRoot, overriddenDecl, null, changes, IProposalRelevance.ADD_EXCEPTIONS);
			if (p2 != null)
				proposals.add(p2);
		}
	}

	private static boolean isDeclaredException(ITypeBinding curr, ITypeBinding[] declared) {
		for (ITypeBinding d : declared) {
			if (Bindings.isSuperType(d, curr)) {
				return true;
			}
		}
		return false;
	}

	public void collectTypeMismatchInForEachProposals(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) {
		CompilationUnit astRoot= context.getASTRoot();
		ASTNode selectedNode= problem.getCoveringNode(astRoot);
		if (selectedNode == null || selectedNode.getLocationInParent() != EnhancedForStatement.EXPRESSION_PROPERTY) {
			return;
		}
		EnhancedForStatement forStatement= (EnhancedForStatement) selectedNode.getParent();


		ITypeBinding expressionBinding= forStatement.getExpression().resolveTypeBinding();
		if (expressionBinding == null) {
			return;
		}

		ITypeBinding expectedBinding;
		if (expressionBinding.isArray()) {
			expectedBinding= expressionBinding.getComponentType();
		} else {
			IMethodBinding iteratorMethod= Bindings.findMethodInHierarchy(expressionBinding, "iterator", new String[0]); //$NON-NLS-1$
			if (iteratorMethod == null) {
				return;
			}
			ITypeBinding[] typeArguments= iteratorMethod.getReturnType().getTypeArguments();
			if (typeArguments.length != 1) {
				return;
			}
			expectedBinding= typeArguments[0];
		}
		AST ast= astRoot.getAST();
		expectedBinding= Bindings.normalizeForDeclarationUse(expectedBinding, ast);

		SingleVariableDeclaration parameter= forStatement.getParameter();

		ICompilationUnit cu= context.getCompilationUnit();
		if (parameter.getName().getLength() == 0) {
			SimpleName simpleName= null;
			if (parameter.getType() instanceof SimpleType simpleType) {
				SimpleType type= simpleType;
				if (type.getName() instanceof SimpleName simpleName2) {
					simpleName= simpleName2;
				}
			} else if (parameter.getType() instanceof NameQualifiedType nameQualifiedType) {
				simpleName= nameQualifiedType.getName();
			}
			if (simpleName != null) {
				String name= simpleName.getIdentifier();
				int relevance= StubUtility.hasLocalVariableName(cu.getJavaProject(), name) ? 10 : 7;
				String label= Messages.format(CorrectionMessages.TypeMismatchSubProcessor_create_loop_variable_description, BasicElementLabels.getJavaElementName(name));
				T p1= createNewVariableCorrectionProposal(label, cu, NewVariableCorrectionProposalCore.LOCAL, simpleName, null, relevance);
				if (p1 != null)
					proposals.add(p1);
				return;
			}
		}

		String label= Messages.format(CorrectionMessages.TypeMismatchSubProcessor_incompatible_for_each_type_description, new String[] { BasicElementLabels.getJavaElementName(parameter.getName().getIdentifier()), BindingLabelProviderCore.getBindingLabel(expectedBinding, BindingLabelProviderCore.DEFAULT_TEXTFLAGS) });
		ASTRewrite rewrite= ASTRewrite.create(ast);
		T p2= createIncompatibleForEachTypeProposal(label, cu, rewrite, IProposalRelevance.INCOMPATIBLE_FOREACH_TYPE, astRoot, ast, expectedBinding, selectedNode, parameter);
		if (p2 != null)
			proposals.add(p2);
	}

	protected abstract T createInsertNullCheckProposal(String label, ICompilationUnit compilationUnit, ASTRewrite rewrite, int relevance);

	protected abstract T createChangeReturnTypeProposal(String label, ICompilationUnit cu, ASTRewrite rewrite, int relevance, ITypeBinding currBinding, AST ast, CompilationUnit astRoot, MethodDeclaration methodDeclaration, BodyDeclaration decl);

	protected abstract T createOptionalProposal(String label, ICompilationUnit cu, Expression nodeToCast, int relevance, int optionalType);

	protected abstract T createImplementInterfaceProposal(ICompilationUnit nodeCu, ITypeBinding typeDecl, CompilationUnit astRoot, ITypeBinding castTypeBinding, int relevance);

	protected abstract T createChangeSenderTypeProposal(ICompilationUnit targetCu, IBinding callerBindingDecl, CompilationUnit astRoot, ITypeBinding castTypeBinding, boolean isAssignedNode,
			int relevance);

	protected abstract T createChangeConstructorTypeProposal(ICompilationUnit targetCu, ASTNode callerNode, CompilationUnit astRoot, ITypeBinding castTypeBinding, int relevance);

	protected abstract T createCastCorrectionProposal(String label, ICompilationUnit cu, Expression nodeToCast, ITypeBinding castTypeBinding, int relevance);

	protected abstract T createChangeReturnTypeOfOverridden(ICompilationUnit targetCu, IMethodBinding overriddenDecl, CompilationUnit astRoot, ITypeBinding returnType, boolean offerSuperTypeProposals,
			int relevance, ITypeBinding overridenDeclType);

	protected abstract T createChangeIncompatibleReturnTypeProposal(ICompilationUnit cu, IMethodBinding methodDecl, CompilationUnit astRoot, ITypeBinding overriddenReturnType, boolean offerSuperTypeProposals,
			int relevance);

	protected abstract T createChangeMethodSignatureProposal(String label, ICompilationUnit cu, CompilationUnit astRoot, IMethodBinding methodDeclBinding, ChangeDescription[] paramChanges,
			ChangeDescription[] changes, int relevance);

	protected abstract T createNewVariableCorrectionProposal(String label, ICompilationUnit cu, int local, SimpleName simpleName, ITypeBinding senderBinding, int relevance);

	protected abstract T createIncompatibleForEachTypeProposal(String label, ICompilationUnit cu, ASTRewrite rewrite, int incompatibleForeachType, CompilationUnit astRoot, AST ast, ITypeBinding expectedBinding, ASTNode selectedNode, SingleVariableDeclaration parameter);
}
