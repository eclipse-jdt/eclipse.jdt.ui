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
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.Collection;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.PrimitiveType.Code;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.corext.codemanipulation.ImportRewrite;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.TypeRules;
import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
 *
 */
public class TypeMismatchSubProcessor {

	private TypeMismatchSubProcessor() {
	}
	
	public static void addTypeMismatchProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) throws CoreException {
		String[] args= problem.getProblemArguments();
		if (args.length != 2) {
			return;
		}
			
		ICompilationUnit cu= context.getCompilationUnit();
		String castTypeName= args[1];

		CompilationUnit astRoot= context.getASTRoot();
		ASTNode selectedNode= problem.getCoveredNode(astRoot);
		if (!(selectedNode instanceof Expression)) {
			return;
		}
		Expression nodeToCast= (Expression) selectedNode;
		Name receiverNode= null;
		ITypeBinding castTypeBinding= null;
		
		int parentNodeType= selectedNode.getParent().getNodeType();
		if (parentNodeType == ASTNode.ASSIGNMENT) {
			Assignment assign= (Assignment) selectedNode.getParent();
			Expression leftHandSide= assign.getLeftHandSide();
			if (selectedNode.equals(leftHandSide)) {
				nodeToCast= assign.getRightHandSide();
			}
			castTypeBinding= assign.getLeftHandSide().resolveTypeBinding();
			if (leftHandSide instanceof Name) {
				receiverNode= (Name) leftHandSide;
			}
		} else if (parentNodeType == ASTNode.VARIABLE_DECLARATION_FRAGMENT) {
			VariableDeclarationFragment frag= (VariableDeclarationFragment) selectedNode.getParent();
			if (selectedNode.equals(frag.getName())) {
				nodeToCast= frag.getInitializer();
				IVariableBinding varBinding= frag.resolveBinding();
				if (varBinding != null) {
					castTypeBinding= varBinding.getType();
				}
				receiverNode= frag.getName();
			}
		} else {
			// try to find the binding corresponding to 'castTypeName'
			ITypeBinding guessedCastTypeBinding= ASTResolving.guessBindingForReference(nodeToCast);
			if (guessedCastTypeBinding != null && castTypeName.equals(guessedCastTypeBinding.getQualifiedName())) {
				castTypeBinding= guessedCastTypeBinding;
			}
		}

		ITypeBinding binding= nodeToCast.resolveTypeBinding();
		if (binding == null || canCast(castTypeName, castTypeBinding, binding)) {
			proposals.add(createCastProposal(context, castTypeName, nodeToCast, 5));
		}
		
		ITypeBinding currBinding= nodeToCast.resolveTypeBinding();
		
		if (currBinding == null || "void".equals(currBinding.getName())) { //$NON-NLS-1$
			return;
		}
		
		// change method return statement to actual type
		if (parentNodeType == ASTNode.RETURN_STATEMENT) {
			BodyDeclaration decl= ASTResolving.findParentBodyDeclaration(selectedNode);
			if (decl instanceof MethodDeclaration) {
				MethodDeclaration methodDeclaration= (MethodDeclaration) decl;
	
				currBinding= Bindings.normalizeTypeBinding(currBinding);
				if (currBinding == null) {
					currBinding= astRoot.getAST().resolveWellKnownType("java.lang.Object"); //$NON-NLS-1$
				}
	
				ASTRewrite rewrite= ASTRewrite.create(methodDeclaration.getAST());
				ImportRewrite imports= new ImportRewrite(cu);

				String returnTypeName= imports.addImport(currBinding);
				
				Type newReturnType= ASTNodeFactory.newType(astRoot.getAST(), returnTypeName);
				rewrite.replace(methodDeclaration.getReturnType(), newReturnType, null);
				
				String label= CorrectionMessages.getFormattedString("TypeMismatchSubProcessor.changereturntype.description", currBinding.getName()); //$NON-NLS-1$
				Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
				LinkedCorrectionProposal proposal= new LinkedCorrectionProposal(label, cu, rewrite, 6, image);
				proposal.setImportRewrite(imports);
				
				String returnKey= "return"; //$NON-NLS-1$
				proposal.addLinkedPosition(rewrite.track(newReturnType), true, returnKey);
				ITypeBinding[] typeSuggestions= ASTResolving.getRelaxingTypes(astRoot.getAST(), currBinding);
				for (int i= 0; i < typeSuggestions.length; i++) {
					proposal.addLinkedPositionProposal(returnKey, typeSuggestions[i]);
				}
				proposals.add(proposal);
			}
		}

		if (receiverNode != null) {
			currBinding= Bindings.normalizeTypeBinding(currBinding);
			if (currBinding == null) {
				currBinding= astRoot.getAST().resolveWellKnownType("java.lang.Object"); //$NON-NLS-1$
			}
			addChangeSenderTypeProposals(context, receiverNode, currBinding, true, 4, proposals);
		}
		
		if (castTypeBinding != null) {
			addChangeSenderTypeProposals(context, nodeToCast, castTypeBinding, false, 5, proposals);
		}
	}
	
	public static void addChangeSenderTypeProposals(IInvocationContext context, Expression nodeToCast, ITypeBinding castTypeBinding, boolean isAssingedNode, int relevance, Collection proposals) throws JavaModelException {
		IBinding callerBinding= null;
		switch (nodeToCast.getNodeType()) {
			case ASTNode.METHOD_INVOCATION:
				callerBinding= ((MethodInvocation) nodeToCast).resolveMethodBinding();
				break;
			case ASTNode.SUPER_METHOD_INVOCATION:
				callerBinding= ((SuperMethodInvocation) nodeToCast).resolveMethodBinding();
				break;			
			case ASTNode.FIELD_ACCESS:
				callerBinding= ((FieldAccess) nodeToCast).resolveFieldBinding();
				break;
			case ASTNode.SUPER_FIELD_ACCESS:
				callerBinding= ((SuperFieldAccess) nodeToCast).resolveFieldBinding();
				break;
			case ASTNode.SIMPLE_NAME:
			case ASTNode.QUALIFIED_NAME:
				callerBinding= ((Name) nodeToCast).resolveBinding();
				break;
		}
		
		ICompilationUnit cu= context.getCompilationUnit();
		CompilationUnit astRoot= context.getASTRoot();
		
		ICompilationUnit targetCu= null;
		ITypeBinding declaringType= null;
		if (callerBinding instanceof IVariableBinding) {
			IVariableBinding variableBinding= (IVariableBinding) callerBinding;
			if (!variableBinding.isField()) {
				targetCu= cu;
			} else {
				declaringType= variableBinding.getDeclaringClass();
			}
		} else if (callerBinding instanceof IMethodBinding) {
			IMethodBinding methodBinding= (IMethodBinding) callerBinding;
			if (!methodBinding.isConstructor()) {
				declaringType= methodBinding.getDeclaringClass();
			}
		}
		if (declaringType != null && declaringType.isFromSource()) {
			targetCu= ASTResolving.findCompilationUnitForBinding(cu, astRoot, declaringType);
		}
		if (targetCu != null) {
			proposals.add(new TypeChangeCompletionProposal(targetCu, callerBinding, astRoot, castTypeBinding, isAssingedNode, relevance));
		}
		
		// add interface to resulting type
		if (!isAssingedNode) {
			ITypeBinding nodeType= nodeToCast.resolveTypeBinding();
			if (castTypeBinding.isInterface() && nodeType != null && nodeType.isClass() && !nodeType.isAnonymous() && nodeType.isFromSource()) {
				ICompilationUnit nodeCu= ASTResolving.findCompilationUnitForBinding(cu, astRoot, nodeType);
				if (nodeCu != null) {
					proposals.add(new ImplementInterfaceProposal(nodeCu, nodeType, astRoot, castTypeBinding, relevance - 1));
				}
			}
		}
	}
	
	private static boolean canCast(String castTarget, ITypeBinding castTypeBinding, ITypeBinding bindingToCast) {
		if (castTypeBinding != null) {
			return TypeRules.canCast(castTypeBinding, bindingToCast);
		}
		
		bindingToCast= Bindings.normalizeTypeBinding(bindingToCast);
		if (bindingToCast == null) {
			return false;
		}
		
		int arrStart= castTarget.indexOf('[');
		if (arrStart != -1) {
			if (!bindingToCast.isArray()) {
				return "java.lang.Object".equals(bindingToCast.getQualifiedName()); //$NON-NLS-1$
			}
			castTarget= castTarget.substring(0, arrStart);
			bindingToCast= bindingToCast.getElementType();
			if (bindingToCast.isPrimitive() && !castTarget.equals(bindingToCast.getName())) {
				return false; // can't cast arrays of primitive types into each other
			}
		}
		
		Code targetCode= PrimitiveType.toCode(castTarget);
		if (bindingToCast.isPrimitive()) {
			Code castCode= PrimitiveType.toCode(bindingToCast.getName());
			if (castCode == targetCode) {
				return true;
			}
			return (targetCode != null && targetCode != PrimitiveType.BOOLEAN && castCode != PrimitiveType.BOOLEAN);
		} else {
			return targetCode == null; // can not check
		}
	}
	
	public static ASTRewriteCorrectionProposal createCastProposal(IInvocationContext context, String castType, Expression nodeToCast, int relevance) throws CoreException {
		ICompilationUnit cu= context.getCompilationUnit();
		CompilationUnit astRoot= context.getASTRoot();
		
		ASTRewrite rewrite= ASTRewrite.create(nodeToCast.getAST());
		ImportRewrite imports= new ImportRewrite(cu);
		
		String label;
		String simpleCastType= imports.addImport(castType);
		
		if (nodeToCast.getNodeType() == ASTNode.CAST_EXPRESSION) {
			label= CorrectionMessages.getFormattedString("TypeMismatchSubProcessor.changecast.description", castType); //$NON-NLS-1$
			CastExpression expression= (CastExpression) nodeToCast;
			rewrite.replace(expression.getType(), rewrite.createStringPlaceholder(simpleCastType, ASTNode.SIMPLE_TYPE), null);
		} else {
			label= CorrectionMessages.getFormattedString("TypeMismatchSubProcessor.addcast.description", castType); //$NON-NLS-1$
			
			Expression expressionCopy= (Expression) rewrite.createCopyTarget(nodeToCast);
			int nodeType= nodeToCast.getNodeType();
			
			if (nodeType == ASTNode.INFIX_EXPRESSION || nodeType == ASTNode.CONDITIONAL_EXPRESSION 
				|| nodeType == ASTNode.ASSIGNMENT || nodeType == ASTNode.INSTANCEOF_EXPRESSION) {
				// nodes have weaker precedence than cast
				ParenthesizedExpression parenthesizedExpression= astRoot.getAST().newParenthesizedExpression();
				parenthesizedExpression.setExpression(expressionCopy);
				expressionCopy= parenthesizedExpression;
			}
			
			Type typeCopy= (Type) rewrite.createStringPlaceholder(simpleCastType, ASTNode.SIMPLE_TYPE);
			CastExpression castExpression= astRoot.getAST().newCastExpression();
			castExpression.setExpression(expressionCopy);
			castExpression.setType(typeCopy);
			
			rewrite.replace(nodeToCast, castExpression, null);
		}
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, relevance, image); //$NON-NLS-1$
		
		proposal.setImportRewrite(imports);
		
		return proposal;
	}

	public static void addIncompatibleReturnTypeProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) throws JavaModelException {
		CompilationUnit astRoot= context.getASTRoot();
		ASTNode selectedNode= problem.getCoveredNode(astRoot);
		if (selectedNode instanceof SimpleName) {
			selectedNode= selectedNode.getParent();
		}
		if (!(selectedNode instanceof MethodDeclaration)) {
			return;
		}
		MethodDeclaration decl= (MethodDeclaration) selectedNode;
		IMethodBinding binding= decl.resolveBinding();
		if (binding == null) {
			return;
		}
		
		IMethodBinding overridden= Bindings.findMethodDefininition(binding);
		if (overridden == null || overridden.getReturnType() == binding.getReturnType()) {
			return;
		}
		ITypeBinding declaringType= overridden.getDeclaringClass();
		
		ICompilationUnit cu= context.getCompilationUnit();
		proposals.add(new TypeChangeCompletionProposal(cu, binding, astRoot, overridden.getReturnType(), false, 8));
		
		ICompilationUnit targetCu= cu;
		if (declaringType != null && declaringType.isFromSource()) {
			targetCu= ASTResolving.findCompilationUnitForBinding(cu, astRoot, declaringType);
		}
		if (targetCu != null) {
			proposals.add(new TypeChangeCompletionProposal(targetCu, overridden, astRoot, binding.getReturnType(), false, 7));
		}

	
	}


}
