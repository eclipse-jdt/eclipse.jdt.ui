/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
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
import org.eclipse.jdt.core.compiler.IProblem;

import org.eclipse.jdt.core.dom.*;

import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
  */
public class ModifierCorrectionSubProcessor {
	
	public static final int TO_STATIC= 1;
	public static final int TO_VISIBLE= 2;
	public static final int TO_NON_PRIVATE= 3;
	public static final int TO_NON_STATIC= 4;
	public static final int TO_NON_FINAL= 5;
	
	public static void addNonAccessibleMemberProposal(IInvocationContext context, IProblemLocation problem, Collection proposals, int kind, int relevance) throws CoreException {
		ICompilationUnit cu= context.getCompilationUnit();

		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		if (selectedNode == null) {
			return;
		}
		
		IBinding binding=null;
		switch (selectedNode.getNodeType()) {
			case ASTNode.SIMPLE_NAME:
				binding= ((SimpleName) selectedNode).resolveBinding();
				break;
			case ASTNode.QUALIFIED_NAME:
				binding= ((QualifiedName) selectedNode).resolveBinding();
				break;
			case ASTNode.SIMPLE_TYPE:
				binding= ((SimpleType) selectedNode).resolveBinding();
				break;
			case ASTNode.METHOD_INVOCATION:
				binding= ((MethodInvocation) selectedNode).getName().resolveBinding();
				break;
			case ASTNode.SUPER_METHOD_INVOCATION:
				binding= ((SuperMethodInvocation) selectedNode).getName().resolveBinding();
				break;
			case ASTNode.FIELD_ACCESS:
				binding= ((FieldAccess) selectedNode).getName().resolveBinding();
				break;								
			case ASTNode.SUPER_FIELD_ACCESS:
				binding= ((SuperFieldAccess) selectedNode).getName().resolveBinding();
				break;				
			case ASTNode.CLASS_INSTANCE_CREATION:
				binding= ((ClassInstanceCreation) selectedNode).resolveConstructorBinding();
				break;
			case ASTNode.SUPER_CONSTRUCTOR_INVOCATION:
				binding= ((SuperConstructorInvocation) selectedNode).resolveConstructorBinding();
				break;
			default:
				return;
		}
		ITypeBinding typeBinding= null;
		String name;
		boolean isLocalVar= false;
		if (binding instanceof IMethodBinding) {
			typeBinding= ((IMethodBinding) binding).getDeclaringClass();
			name= binding.getName() + "()"; //$NON-NLS-1$
		} else if (binding instanceof IVariableBinding) {
			typeBinding= ((IVariableBinding) binding).getDeclaringClass();
			name= binding.getName();
			isLocalVar= !((IVariableBinding) binding).isField();
		} else if (binding instanceof ITypeBinding) {
			typeBinding= (ITypeBinding) binding;
			name= binding.getName();
		} else {
			return;
		}
		if (typeBinding != null && typeBinding.isFromSource() || isLocalVar) {
			int includedModifiers= 0;
			int excludedModifiers= 0;
			String label;
			switch (kind) {
				case TO_VISIBLE:
					excludedModifiers= Modifier.PRIVATE | Modifier.PROTECTED | Modifier.PUBLIC;
					includedModifiers= getNeededVisibility(selectedNode, typeBinding);
					label= CorrectionMessages.getFormattedString("ModifierCorrectionSubProcessor.changevisibility.description", new String[] { name, getVisibilityString(includedModifiers) }); //$NON-NLS-1$
					break;
				case TO_STATIC:
					label= CorrectionMessages.getFormattedString("ModifierCorrectionSubProcessor.changemodifiertostatic.description", name); //$NON-NLS-1$
					includedModifiers= Modifier.STATIC;
					break;
				case TO_NON_STATIC:
					label= CorrectionMessages.getFormattedString("ModifierCorrectionSubProcessor.changemodifiertononstatic.description", name); //$NON-NLS-1$
					excludedModifiers= Modifier.STATIC;
					break;
				case TO_NON_PRIVATE:			
					label= CorrectionMessages.getFormattedString("ModifierCorrectionSubProcessor.changemodifiertodefault.description", name); //$NON-NLS-1$
					excludedModifiers= Modifier.PRIVATE;
					break;
				case TO_NON_FINAL:	
					label= CorrectionMessages.getFormattedString("ModifierCorrectionSubProcessor.changemodifiertononfinal.description", name); //$NON-NLS-1$
					excludedModifiers= Modifier.FINAL;
					break;
				default:
					return;
			}
			ICompilationUnit targetCU= isLocalVar ? cu : ASTResolving.findCompilationUnitForBinding(cu, context.getASTRoot(), typeBinding);
			if (targetCU != null) {
				Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
				proposals.add(new ModifierChangeCompletionProposal(label, targetCU, binding, selectedNode, includedModifiers, excludedModifiers, relevance, image));
			}
		}
		if (kind == TO_VISIBLE && binding.getKind() == IBinding.VARIABLE && Modifier.isPrivate(binding.getModifiers())) {
			if (selectedNode instanceof SimpleName || selectedNode instanceof FieldAccess && ((FieldAccess) selectedNode).getExpression() instanceof ThisExpression) {
				UnresolvedElementsSubProcessor.getVariableProposals(context, problem, proposals);
			}
		}
	}
	
	public static void addOverridesFinalProposals(IInvocationContext context, IProblemLocation problem, Collection proposals, int kind) throws JavaModelException {
		ICompilationUnit cu= context.getCompilationUnit();

		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		if (!(selectedNode instanceof MethodDeclaration)) {
			return;
		}
		
		IMethodBinding method= ((MethodDeclaration) selectedNode).resolveBinding();
		ITypeBinding curr= method.getDeclaringClass();
		IMethodBinding overriddenClass= null;
		
		
		if (overriddenClass == null && curr.getSuperclass() != null) {
			curr= curr.getSuperclass();
			overriddenClass= Bindings.findMethodInType(curr, method.getName(), method.getParameterTypes());
		}
		if (overriddenClass != null) {
			ICompilationUnit targetCU= ASTResolving.findCompilationUnitForBinding(cu, context.getASTRoot(), curr);
			if (targetCU != null) {
				String methodName= curr.getName() + '.' + overriddenClass.getName();
				String label;
				int excludedModifiers;
				int includedModifiers;			
				switch (kind) {
					case TO_VISIBLE:
						excludedModifiers= Modifier.PRIVATE | Modifier.PROTECTED | Modifier.PUBLIC;
						includedModifiers= method.getModifiers() & (Modifier.PROTECTED | Modifier.PUBLIC);
						label= CorrectionMessages.getFormattedString("ModifierCorrectionSubProcessor.changeoverriddenvisibility.description", new String[] { methodName, getVisibilityString(includedModifiers) }); //$NON-NLS-1$
						break;
					case TO_NON_FINAL:	
						label= CorrectionMessages.getFormattedString("ModifierCorrectionSubProcessor.changemethodtononfinal.description", methodName); //$NON-NLS-1$
						excludedModifiers= Modifier.FINAL;
						includedModifiers= 0;
						break;
					default:
						return;
				}
				Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
				proposals.add(new ModifierChangeCompletionProposal(label, targetCU, overriddenClass, selectedNode, includedModifiers, excludedModifiers, 9, image));
			}
		}
		if (kind == TO_VISIBLE && problem.getProblemId() != IProblem.OverridingNonVisibleMethod) {
			IMethodBinding defining= Bindings.findDefiningMethod(method);
			if (defining != null) {
				int excludedModifiers= Modifier.PRIVATE | Modifier.PROTECTED | Modifier.PUBLIC;
				int includedModifiers= defining.getModifiers() & (Modifier.PROTECTED | Modifier.PUBLIC);
				String label= CorrectionMessages.getFormattedString("ModifierCorrectionSubProcessor.changemethodvisibility.description", new String[] { getVisibilityString(includedModifiers) }); //$NON-NLS-1$
				Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
				proposals.add(new ModifierChangeCompletionProposal(label, cu, method, selectedNode, includedModifiers, excludedModifiers, 8, image));
			}
		}
		
	}
	
	
	public static void addNonFinalLocalProposal(IInvocationContext context, IProblemLocation problem, Collection proposals) {
		ICompilationUnit cu= context.getCompilationUnit();

		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		if (!(selectedNode instanceof SimpleName)) {
			return;
		}
		
		IBinding binding= ((SimpleName) selectedNode).resolveBinding();
		if (binding instanceof IVariableBinding) {
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			String label= CorrectionMessages.getFormattedString("ModifierCorrectionSubProcessor.changemodifiertofinal.description", binding.getName()); //$NON-NLS-1$
			proposals.add(new ModifierChangeCompletionProposal(label, cu, binding, selectedNode, Modifier.FINAL, 0, 5, image));
		}
	}
		
	private static String getVisibilityString(int code) {
		if (Modifier.isPublic(code)) {
			return "public"; //$NON-NLS-1$
		}else if (Modifier.isProtected(code)) {
			return "protected"; //$NON-NLS-1$
		}
		return "default"; //$NON-NLS-1$
	}
	
	
	private static int getNeededVisibility(ASTNode currNode, ITypeBinding targetType) {
		ITypeBinding currNodeBinding= Bindings.getBindingOfParentType(currNode);
		if (currNodeBinding == null) { // import
			return Modifier.PUBLIC;
		}
		
		if (Bindings.isSuperType(targetType, currNodeBinding)) {
			return Modifier.PROTECTED;
		}

		if (currNodeBinding.getPackage().getKey().equals(targetType.getPackage().getKey())) {
			return 0;
		}
		return Modifier.PUBLIC;
	}

	public static void addAbstractMethodProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) {
		ICompilationUnit cu= context.getCompilationUnit();

		CompilationUnit astRoot= context.getASTRoot();

		ASTNode selectedNode= problem.getCoveringNode(astRoot);
		if (selectedNode == null) {
			return;
		}
		MethodDeclaration decl;
		if (selectedNode instanceof SimpleName) {
			decl= (MethodDeclaration) selectedNode.getParent();
		} else if (selectedNode instanceof MethodDeclaration) {
			decl= (MethodDeclaration) selectedNode;
		} else {
			return;
		}
	
		ASTNode parentType= ASTResolving.findParentType(decl);
		TypeDeclaration parentTypeDecl= null;
		boolean parentIsAbstractClass= false;
		if (parentType instanceof TypeDeclaration) {
			parentTypeDecl= (TypeDeclaration) parentType;
			parentIsAbstractClass= !parentTypeDecl.isInterface() && Modifier.isAbstract(parentTypeDecl.getModifiers());
		}
		boolean hasNoBody= (decl.getBody() == null);
		
		if (problem.getProblemId() == IProblem.AbstractMethodInAbstractClass || parentIsAbstractClass) {
			AST ast= astRoot.getAST();
			ASTRewrite rewrite= new ASTRewrite(ast);

			int newModifiers= decl.getModifiers() & ~Modifier.ABSTRACT;
			rewrite.set(decl, MethodDeclaration.MODIFIERS_PROPERTY, new Integer(newModifiers), null);

			if (hasNoBody) {
				Block newBody= ast.newBlock();
				rewrite.set(decl, MethodDeclaration.BODY_PROPERTY, newBody, null);
				
				Expression expr= ASTNodeFactory.newDefaultExpression(ast, decl.getReturnType(), decl.getExtraDimensions());
				if (expr != null) {
					ReturnStatement returnStatement= ast.newReturnStatement();
					returnStatement.setExpression(expr);
					newBody.statements().add(returnStatement);
				}
			}
	
			String label= CorrectionMessages.getString("ModifierCorrectionSubProcessor.removeabstract.description"); //$NON-NLS-1$
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, 6, image);
			proposals.add(proposal);
		}
		
		if (!hasNoBody && problem.getProblemId() == IProblem.BodyForAbstractMethod) {
			ASTRewrite rewrite= new ASTRewrite(decl.getAST());
			rewrite.remove(decl.getBody(), null);
			
			String label= CorrectionMessages.getString("ModifierCorrectionSubProcessor.removebody.description"); //$NON-NLS-1$
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			ASTRewriteCorrectionProposal proposal2= new ASTRewriteCorrectionProposal(label, cu, rewrite, 5, image);
			proposals.add(proposal2);
		}
		
		if (problem.getProblemId() == IProblem.AbstractMethodInAbstractClass && (parentTypeDecl != null)) {
			ASTRewriteCorrectionProposal proposal= getMakeTypeAbstractProposal(cu, parentTypeDecl, 5);
			proposals.add(proposal);
		}		
		
	}
	
	public static void addNativeMethodProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) {
		ICompilationUnit cu= context.getCompilationUnit();

		CompilationUnit astRoot= context.getASTRoot();

		ASTNode selectedNode= problem.getCoveringNode(astRoot);
		if (selectedNode == null) {
			return;
		}
		MethodDeclaration decl;
		if (selectedNode instanceof SimpleName) {
			decl= (MethodDeclaration) selectedNode.getParent();
		} else if (selectedNode instanceof MethodDeclaration) {
			decl= (MethodDeclaration) selectedNode;
		} else {
			return;
		}
	
		{
			AST ast= astRoot.getAST();
			ASTRewrite rewrite= new ASTRewrite(ast);
			
			int newModifiers= decl.getModifiers() & ~Modifier.NATIVE;
			rewrite.set(decl, MethodDeclaration.MODIFIERS_PROPERTY, new Integer(newModifiers), null);

			Block newBody= ast.newBlock();
			rewrite.set(decl, MethodDeclaration.BODY_PROPERTY, newBody, null);
			
			Expression expr= ASTNodeFactory.newDefaultExpression(ast, decl.getReturnType(), decl.getExtraDimensions());
			if (expr != null) {
				ReturnStatement returnStatement= ast.newReturnStatement();
				returnStatement.setExpression(expr);
				newBody.statements().add(returnStatement);
			}
	
			String label= CorrectionMessages.getString("ModifierCorrectionSubProcessor.removenative.description"); //$NON-NLS-1$
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, 6, image);
			proposals.add(proposal);
		}
		
		if (decl.getBody() != null) {
			ASTRewrite rewrite= new ASTRewrite(decl.getAST());
			rewrite.remove(decl.getBody(), null);
			
			String label= CorrectionMessages.getString("ModifierCorrectionSubProcessor.removebody.description"); //$NON-NLS-1$
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			ASTRewriteCorrectionProposal proposal2= new ASTRewriteCorrectionProposal(label, cu, rewrite, 5, image);
			proposals.add(proposal2);
		}
		
	}
	
	
	
	public static ASTRewriteCorrectionProposal getMakeTypeAbstractProposal(ICompilationUnit cu, TypeDeclaration typeDeclaration, int relevance) {
		ASTRewrite rewrite= new ASTRewrite(typeDeclaration.getAST());
				
		int newModifiers= typeDeclaration.getModifiers() | Modifier.ABSTRACT;
		rewrite.set(typeDeclaration, TypeDeclaration.MODIFIERS_PROPERTY, new Integer(newModifiers), null);

		String label= CorrectionMessages.getFormattedString("ModifierCorrectionSubProcessor.addabstract.description", typeDeclaration.getName().getIdentifier()); //$NON-NLS-1$
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, relevance, image);
		return proposal;
	}

	public static void addMethodRequiresBodyProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) {
		ICompilationUnit cu= context.getCompilationUnit();
		AST ast= context.getASTRoot().getAST();
		
		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		if (!(selectedNode instanceof MethodDeclaration)) {
			return;
		}
		MethodDeclaration decl=  (MethodDeclaration) selectedNode;
		{
			ASTRewrite rewrite= new ASTRewrite(ast);
			
			int newModifiers= decl.getModifiers() & ~Modifier.ABSTRACT;
			rewrite.set(decl, MethodDeclaration.MODIFIERS_PROPERTY, new Integer(newModifiers), null);		
			
			Block body= ast.newBlock();
			rewrite.set(decl, MethodDeclaration.BODY_PROPERTY, body, null);
			
			Type returnType= decl.getReturnType();
			if (!decl.isConstructor()) {
				Expression expression= ASTNodeFactory.newDefaultExpression(ast, returnType, decl.getExtraDimensions());
				if (expression != null) {
					ReturnStatement returnStatement= ast.newReturnStatement();
					returnStatement.setExpression(expression);
					body.statements().add(returnStatement);				
				}
			}
	
			String label= CorrectionMessages.getString("ModifierCorrectionSubProcessor.addmissingbody.description"); //$NON-NLS-1$
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, 9, image);
	
			proposals.add(proposal);
		}
		{
			ASTRewrite rewrite= new ASTRewrite(ast);
			
			int newModifiers= decl.getModifiers() | Modifier.ABSTRACT;
			rewrite.set(decl, MethodDeclaration.MODIFIERS_PROPERTY, new Integer(newModifiers), null);
			
			String label= CorrectionMessages.getString("ModifierCorrectionSubProcessor.setmethodabstract.description"); //$NON-NLS-1$
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, 8, image);
			
			proposals.add(proposal);
		}

	}
	

	public static void addNeedToEmulateProposal(IInvocationContext context, IProblemLocation problem, Collection proposals) {
		ICompilationUnit cu= context.getCompilationUnit();

		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		if (!(selectedNode instanceof SimpleName)) {
			return;
		}
		
		IBinding binding= ((SimpleName) selectedNode).resolveBinding();
		if (binding instanceof IVariableBinding) {
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			String label= CorrectionMessages.getFormattedString("ModifierCorrectionSubProcessor.changemodifiertofinal.description", binding.getName()); //$NON-NLS-1$
			proposals.add(new ModifierChangeCompletionProposal(label, cu, binding, selectedNode, Modifier.FINAL, 0, 5, image));
		}
	}

}
