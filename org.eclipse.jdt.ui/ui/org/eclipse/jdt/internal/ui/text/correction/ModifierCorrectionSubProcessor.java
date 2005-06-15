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
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;

import org.eclipse.jdt.core.CorrectionEngine;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.Messages;
import org.eclipse.jdt.internal.corext.util.Strings;

import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.correction.LinkedCorrectionProposal.ILinkedModeProposal;

/**
  */
public class ModifierCorrectionSubProcessor {

	private static final String ADD_SUPPRESSWARNINGS_ID= "org.eclipse.jdt.ui.correction.addSuppressWarnings"; //$NON-NLS-1$

	public static final int TO_STATIC= 1;
	public static final int TO_VISIBLE= 2;
	public static final int TO_NON_PRIVATE= 3;
	public static final int TO_NON_STATIC= 4;
	public static final int TO_NON_FINAL= 5;

	public static void addNonAccessibleReferenceProposal(IInvocationContext context, IProblemLocation problem, Collection proposals, int kind, int relevance) throws CoreException {
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
		IBinding bindingDecl;
		boolean isLocalVar= false;
		if (binding instanceof IMethodBinding) {
			IMethodBinding methodDecl= (IMethodBinding) binding;
			bindingDecl= methodDecl.getMethodDeclaration();
			typeBinding= methodDecl.getDeclaringClass();
			name= methodDecl.getName() + "()"; //$NON-NLS-1$
		} else if (binding instanceof IVariableBinding) {
			IVariableBinding varDecl= (IVariableBinding) binding;
			typeBinding= varDecl.getDeclaringClass();
			name= binding.getName();
			isLocalVar= !varDecl.isField();
			bindingDecl= Bindings.getVariableDeclaration(varDecl);
		} else if (binding instanceof ITypeBinding) {
			typeBinding= (ITypeBinding) binding;
			bindingDecl= typeBinding.getTypeDeclaration();
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
					label= Messages.format(CorrectionMessages.ModifierCorrectionSubProcessor_changevisibility_description, new String[] { name, getVisibilityString(includedModifiers) });
					break;
				case TO_STATIC:
					label= Messages.format(CorrectionMessages.ModifierCorrectionSubProcessor_changemodifiertostatic_description, name);
					includedModifiers= Modifier.STATIC;
					break;
				case TO_NON_STATIC:
					label= Messages.format(CorrectionMessages.ModifierCorrectionSubProcessor_changemodifiertononstatic_description, name);
					excludedModifiers= Modifier.STATIC;
					break;
				case TO_NON_PRIVATE:
					label= Messages.format(CorrectionMessages.ModifierCorrectionSubProcessor_changemodifiertodefault_description, name);
					excludedModifiers= Modifier.PRIVATE;
					break;
				case TO_NON_FINAL:
					label= Messages.format(CorrectionMessages.ModifierCorrectionSubProcessor_changemodifiertononfinal_description, name);
					excludedModifiers= Modifier.FINAL;
					break;
				default:
					Assert.isTrue(false, "not supported"); //$NON-NLS-1$
					return;
			}
			ICompilationUnit targetCU= isLocalVar ? cu : ASTResolving.findCompilationUnitForBinding(cu, context.getASTRoot(), typeBinding.getTypeDeclaration());
			if (targetCU != null) {
				Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
				proposals.add(new ModifierChangeCompletionProposal(label, targetCU, bindingDecl, selectedNode, includedModifiers, excludedModifiers, relevance, image));
			}
		}
		if (kind == TO_VISIBLE && bindingDecl.getKind() == IBinding.VARIABLE /* &&Modifier.isPrivate(bindingDecl.getModifiers())*/) {
			if (selectedNode instanceof SimpleName || selectedNode instanceof FieldAccess && ((FieldAccess) selectedNode).getExpression() instanceof ThisExpression) {
				UnresolvedElementsSubProcessor.getVariableProposals(context, problem, proposals);
			}
		}
	}

	public static void addChangeOverriddenModfierProposal(IInvocationContext context, IProblemLocation problem, Collection proposals, int kind) throws JavaModelException {
		ICompilationUnit cu= context.getCompilationUnit();

		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		if (!(selectedNode instanceof MethodDeclaration)) {
			return;
		}

		IMethodBinding method= ((MethodDeclaration) selectedNode).resolveBinding();
		ITypeBinding curr= method.getDeclaringClass();


		if (kind == TO_VISIBLE && problem.getProblemId() != IProblem.OverridingNonVisibleMethod) {
			IMethodBinding defining= Bindings.findMethodDefininition(method, false);
			if (defining != null) {
				int excludedModifiers= Modifier.PRIVATE | Modifier.PROTECTED | Modifier.PUBLIC;
				int includedModifiers= JdtFlags.getVisibilityCode(defining);
				String label= Messages.format(CorrectionMessages.ModifierCorrectionSubProcessor_changemethodvisibility_description, new String[] { getVisibilityString(includedModifiers) });
				Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
				proposals.add(new ModifierChangeCompletionProposal(label, cu, method, selectedNode, includedModifiers, excludedModifiers, 8, image));
			}
		}

		IMethodBinding overriddenInClass= null;
		while (overriddenInClass == null && curr.getSuperclass() != null) {
			curr= curr.getSuperclass();
			overriddenInClass= Bindings.findMethodInType(curr, method.getName(), method.getParameterTypes());
		}
		if (overriddenInClass != null) {
			IMethodBinding overriddenDecl= overriddenInClass.getMethodDeclaration();
			ICompilationUnit targetCU= ASTResolving.findCompilationUnitForBinding(cu, context.getASTRoot(), overriddenDecl.getDeclaringClass());
			if (targetCU != null) {
				String methodName= curr.getName() + '.' + overriddenInClass.getName();
				String label;
				int excludedModifiers;
				int includedModifiers;
				switch (kind) {
					case TO_VISIBLE:
						excludedModifiers= Modifier.PRIVATE | Modifier.PROTECTED | Modifier.PUBLIC;
						includedModifiers= JdtFlags.getVisibilityCode(method);
						label= Messages.format(CorrectionMessages.ModifierCorrectionSubProcessor_changeoverriddenvisibility_description, new String[] { methodName, getVisibilityString(includedModifiers) });
						break;
					case TO_NON_FINAL:
						label= Messages.format(CorrectionMessages.ModifierCorrectionSubProcessor_changemethodtononfinal_description, methodName);
						excludedModifiers= Modifier.FINAL;
						includedModifiers= 0;
						break;
					case TO_NON_STATIC:
						label= Messages.format(CorrectionMessages.ModifierCorrectionSubProcessor_changemethodtononstatic_description, methodName);
						excludedModifiers= Modifier.STATIC;
						includedModifiers= 0;
						break;
					default:
						Assert.isTrue(false, "not supported"); //$NON-NLS-1$
						return;
				}
				Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
				proposals.add(new ModifierChangeCompletionProposal(label, targetCU, overriddenDecl, selectedNode, includedModifiers, excludedModifiers, 7, image));
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
			binding= Bindings.getVariableDeclaration((IVariableBinding) binding);
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			String label= Messages.format(CorrectionMessages.ModifierCorrectionSubProcessor_changemodifiertofinal_description, binding.getName());
			proposals.add(new ModifierChangeCompletionProposal(label, cu, binding, selectedNode, Modifier.FINAL, 0, 5, image));
		}
	}



	public static void addRemoveInvalidModfiersProposal(IInvocationContext context, IProblemLocation problem, Collection proposals, int relevance) {
		ICompilationUnit cu= context.getCompilationUnit();

		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		if (selectedNode instanceof MethodDeclaration) {
			selectedNode= ((MethodDeclaration) selectedNode).getName();
		}

		if (!(selectedNode instanceof SimpleName)) {
			return;
		}

		IBinding binding= ((SimpleName) selectedNode).resolveBinding();
		if (binding != null) {
			String methodName= binding.getName();
			String label;
			int problemId= problem.getProblemId();
			if (problemId == IProblem.CannotHideAnInstanceMethodWithAStaticMethod || problemId == IProblem.UnexpectedStaticModifierForMethod) {
				label= Messages.format(CorrectionMessages.ModifierCorrectionSubProcessor_changemethodtononstatic_description, methodName);
			} else {
				label= Messages.format(CorrectionMessages.ModifierCorrectionSubProcessor_removeinvalidmodifiers_description, methodName);
			}

			int excludedModifiers= 0;
			int includedModifiers= 0;

			switch (problemId) {
				case IProblem.CannotHideAnInstanceMethodWithAStaticMethod:
				case IProblem.UnexpectedStaticModifierForMethod:
					excludedModifiers= Modifier.STATIC;
					break;
				case IProblem.IllegalModifierForInterfaceMethod:
					excludedModifiers= ~(Modifier.PUBLIC | Modifier.ABSTRACT);
					break;
				case IProblem.IllegalModifierForInterface:
					excludedModifiers= ~(Modifier.PUBLIC | Modifier.ABSTRACT | Modifier.STRICTFP);
					break;
				case IProblem.IllegalModifierForClass:
					excludedModifiers= ~(Modifier.PUBLIC | Modifier.ABSTRACT | Modifier.FINAL | Modifier.STRICTFP);
					break;
				case IProblem.IllegalModifierForInterfaceField:
					excludedModifiers= ~(Modifier.PUBLIC | Modifier.ABSTRACT | Modifier.FINAL);
					break;
				case IProblem.IllegalModifierForMemberInterface:
				case IProblem.IllegalVisibilityModifierForInterfaceMemberType:
					excludedModifiers= ~(Modifier.PUBLIC | Modifier.STATIC | Modifier.STRICTFP);
					break;
				case IProblem.IllegalModifierForMemberClass:
					excludedModifiers= ~(Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE | Modifier.STATIC | Modifier.ABSTRACT | Modifier.FINAL | Modifier.STRICTFP);
					break;
				case IProblem.IllegalModifierForLocalClass:
					excludedModifiers= ~(Modifier.ABSTRACT | Modifier.FINAL | Modifier.STRICTFP);
					break;
				case IProblem.IllegalModifierForArgument:
					excludedModifiers= ~Modifier.FINAL;
					break;
				case IProblem.IllegalModifierForField:
					excludedModifiers= ~(Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE | Modifier.STATIC |  Modifier.FINAL | Modifier.VOLATILE | Modifier.TRANSIENT);
					break;
				case IProblem.IllegalModifierForMethod:
					excludedModifiers= ~(Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE | Modifier.STATIC | Modifier.ABSTRACT | Modifier.FINAL | Modifier.NATIVE | Modifier.STRICTFP);
					break;
				case IProblem.IllegalModifierForVariable:
					excludedModifiers= ~Modifier.FINAL;
					break;
				default:
					Assert.isTrue(false, "not supported"); //$NON-NLS-1$
					return;
			}


			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			proposals.add(new ModifierChangeCompletionProposal(label, cu, binding, selectedNode, includedModifiers, excludedModifiers, relevance, image));
		}
	}

	private static String getVisibilityString(int code) {
		if (Modifier.isPublic(code)) {
			return "public"; //$NON-NLS-1$
		} else if (Modifier.isProtected(code)) {
			return "protected"; //$NON-NLS-1$
		} else if (Modifier.isPrivate(code)) {
			return "private"; //$NON-NLS-1$
		}
		return CorrectionMessages.ModifierCorrectionSubProcessor_default;
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
			ASTRewrite rewrite= ASTRewrite.create(ast);

			Modifier modifierNode= ASTNodes.findModifierNode(Modifier.ABSTRACT, decl.modifiers());
			if (modifierNode != null) {
				rewrite.remove(modifierNode, null);
			}

			if (hasNoBody) {
				Block newBody= ast.newBlock();
				rewrite.set(decl, MethodDeclaration.BODY_PROPERTY, newBody, null);

				Expression expr= ASTNodeFactory.newDefaultExpression(ast, decl.getReturnType2(), decl.getExtraDimensions());
				if (expr != null) {
					ReturnStatement returnStatement= ast.newReturnStatement();
					returnStatement.setExpression(expr);
					newBody.statements().add(returnStatement);
				}
			}

			String label= CorrectionMessages.ModifierCorrectionSubProcessor_removeabstract_description;
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, 6, image);
			proposals.add(proposal);
		}

		if (!hasNoBody && problem.getProblemId() == IProblem.BodyForAbstractMethod) {
			ASTRewrite rewrite= ASTRewrite.create(decl.getAST());
			rewrite.remove(decl.getBody(), null);

			String label= CorrectionMessages.ModifierCorrectionSubProcessor_removebody_description;
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
			ASTRewrite rewrite= ASTRewrite.create(ast);

			Modifier modifierNode= ASTNodes.findModifierNode(Modifier.NATIVE, decl.modifiers());
			if (modifierNode != null) {
				rewrite.remove(modifierNode, null);
			}

			Block newBody= ast.newBlock();
			rewrite.set(decl, MethodDeclaration.BODY_PROPERTY, newBody, null);

			Expression expr= ASTNodeFactory.newDefaultExpression(ast, decl.getReturnType2(), decl.getExtraDimensions());
			if (expr != null) {
				ReturnStatement returnStatement= ast.newReturnStatement();
				returnStatement.setExpression(expr);
				newBody.statements().add(returnStatement);
			}

			String label= CorrectionMessages.ModifierCorrectionSubProcessor_removenative_description;
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, 6, image);
			proposals.add(proposal);
		}

		if (decl.getBody() != null) {
			ASTRewrite rewrite= ASTRewrite.create(decl.getAST());
			rewrite.remove(decl.getBody(), null);

			String label= CorrectionMessages.ModifierCorrectionSubProcessor_removebody_description;
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			ASTRewriteCorrectionProposal proposal2= new ASTRewriteCorrectionProposal(label, cu, rewrite, 5, image);
			proposals.add(proposal2);
		}

	}



	public static ASTRewriteCorrectionProposal getMakeTypeAbstractProposal(ICompilationUnit cu, TypeDeclaration typeDeclaration, int relevance) {
		AST ast= typeDeclaration.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);
		Modifier newModifier= ast.newModifier(Modifier.ModifierKeyword.ABSTRACT_KEYWORD);
		rewrite.getListRewrite(typeDeclaration, TypeDeclaration.MODIFIERS2_PROPERTY).insertLast(newModifier, null);

		String label= Messages.format(CorrectionMessages.ModifierCorrectionSubProcessor_addabstract_description, typeDeclaration.getName().getIdentifier());
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		LinkedCorrectionProposal proposal= new LinkedCorrectionProposal(label, cu, rewrite, relevance, image);
		proposal.addLinkedPosition(rewrite.track(newModifier), true, "modifier"); //$NON-NLS-1$
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
			ASTRewrite rewrite= ASTRewrite.create(ast);

			Modifier modifierNode= ASTNodes.findModifierNode(Modifier.ABSTRACT, decl.modifiers());
			if (modifierNode != null) {
				rewrite.remove(modifierNode, null);
			}

			Block body= ast.newBlock();
			rewrite.set(decl, MethodDeclaration.BODY_PROPERTY, body, null);


			if (!decl.isConstructor()) {
				Type returnType= decl.getReturnType2();
				Expression expression= ASTNodeFactory.newDefaultExpression(ast, returnType, decl.getExtraDimensions());
				if (expression != null) {
					ReturnStatement returnStatement= ast.newReturnStatement();
					returnStatement.setExpression(expression);
					body.statements().add(returnStatement);
				}
			}

			String label= CorrectionMessages.ModifierCorrectionSubProcessor_addmissingbody_description;
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, 9, image);

			proposals.add(proposal);
		}
		{
			ASTRewrite rewrite= ASTRewrite.create(ast);

			Modifier newModifier= ast.newModifier(Modifier.ModifierKeyword.ABSTRACT_KEYWORD);
			rewrite.getListRewrite(decl, MethodDeclaration.MODIFIERS2_PROPERTY).insertLast(newModifier, null);

			String label= CorrectionMessages.ModifierCorrectionSubProcessor_setmethodabstract_description;
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			LinkedCorrectionProposal proposal= new LinkedCorrectionProposal(label, cu, rewrite, 8, image);
			proposal.addLinkedPosition(rewrite.track(newModifier), true, "modifier"); //$NON-NLS-1$

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
			binding= Bindings.getVariableDeclaration((IVariableBinding) binding);
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			String label= Messages.format(CorrectionMessages.ModifierCorrectionSubProcessor_changemodifiertofinal_description, binding.getName());
			proposals.add(new ModifierChangeCompletionProposal(label, cu, binding, selectedNode, Modifier.FINAL, 0, 5, image));
		}
	}
	
	public static void addOverrideAnnotationProposal(IInvocationContext context, IProblemLocation problem, Collection proposals) {
		ASTRewriteCorrectionProposal proposal= getMissingAnnotationsProposal(context, problem, "Override"); //$NON-NLS-1$
		if (proposal != null) {
			proposal.setDisplayName(CorrectionMessages.ModifierCorrectionSubProcessor_addoverrideannotation);
			proposal.setImage(JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE));
			proposals.add(proposal);
		}
	}
	
	public static void addDeprecatedAnnotationProposal(IInvocationContext context, IProblemLocation problem, Collection proposals) {
		ASTRewriteCorrectionProposal proposal= getMissingAnnotationsProposal(context, problem, "Deprecated"); //$NON-NLS-1$
		if (proposal != null) {
			proposal.setDisplayName(CorrectionMessages.ModifierCorrectionSubProcessor_adddeprecatedannotation);
			proposal.setImage(JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE));
			proposals.add(proposal);
		}
	}
		
	private static ASTRewriteCorrectionProposal getMissingAnnotationsProposal(IInvocationContext context, IProblemLocation problem, String annotationName) {
		ICompilationUnit cu= context.getCompilationUnit();

		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		ASTNode declaringNode= null;		
		if (selectedNode instanceof MethodDeclaration) {
			declaringNode= selectedNode;
		} else if (selectedNode instanceof SimpleName) {
			StructuralPropertyDescriptor locationInParent= selectedNode.getLocationInParent();
			if (locationInParent == MethodDeclaration.NAME_PROPERTY || locationInParent == TypeDeclaration.NAME_PROPERTY) {
				declaringNode= selectedNode.getParent();
			} else if (locationInParent == VariableDeclarationFragment.NAME_PROPERTY) {
				declaringNode= selectedNode.getParent().getParent();
			}
		}
		if (declaringNode instanceof BodyDeclaration) {
			BodyDeclaration declaration= (BodyDeclaration) declaringNode;
			AST ast= declaration.getAST();
			ASTRewrite rewrite= ASTRewrite.create(ast);
			ListRewrite listRewrite= rewrite.getListRewrite(declaration, declaration.getModifiersProperty());

			Annotation newAnnotation= ast.newMarkerAnnotation();
			newAnnotation.setTypeName(ast.newSimpleName(annotationName));
			listRewrite.insertFirst(newAnnotation, null);
			
			return new ASTRewriteCorrectionProposal("", cu, rewrite, 5, null); //$NON-NLS-1$
		}
		return null;
	}

	private static final String KEY_MODIFIER= "modifier"; //$NON-NLS-1$
	
	private static class ModifierLinkedModeProposal implements ILinkedModeProposal, ICompletionProposalExtension2 {

		private LinkedPositionGroup fLinkedPositionGroup;
		private final int fModifier;

		public ModifierLinkedModeProposal(int modifier) {
			fModifier= modifier;
		}
		
		public void setLinkedPositionGroup(LinkedPositionGroup group) {
			fLinkedPositionGroup= group;
		}

		public String getAdditionalProposalInfo() {
			return getDisplayString();
		}

		public String getDisplayString() {
			if (fModifier == 0) {
				return CorrectionMessages.ModifierCorrectionSubProcessor_default_visibility_label;
			} else {
				return ModifierKeyword.fromFlagValue(fModifier).toString();
			}
		}

		public Image getImage() {
			return null;
		}

		private Position getCurrentPosition(int offset) {
			if (fLinkedPositionGroup != null) {
				LinkedPosition[] positions= fLinkedPositionGroup.getPositions();
				for (int i= 0; i < positions.length; i++) {
					Position position= positions[i];
					if (position.overlapsWith(offset, 0)) {
						return position;
					}
				}
			}
			return null;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#apply(org.eclipse.jface.text.ITextViewer, char, int, int)
		 */
		public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
			Position currentPosition= getCurrentPosition(offset);
			if (currentPosition == null)
				return;
			try {
				IDocument document= viewer.getDocument();
				int documentLen= document.getLength();

				if (fModifier == 0) {

					int end= currentPosition.offset + currentPosition.length; // current end position
					int k= end;
					while (k < documentLen && Strings.isIndentChar(document.getChar(k))) {
						k++;
					}
					// first remove space then replace range (remove space can destoy empty positon)
					document.replace(end, k - end, new String()); // remove extra spaces
					document.replace(currentPosition.offset, currentPosition.length, new String());
				} else {
					// first then replace range the insert space (insert space can destoy empty positon)
					document.replace(currentPosition.offset, currentPosition.length, ModifierKeyword.fromFlagValue(fModifier).toString());
					int end= currentPosition.offset + currentPosition.length; // current end position
					if (end < documentLen && !Character.isWhitespace(document.getChar(end))) {
						document.replace(end, 0, String.valueOf(' ')); // insert extra space
					}
				}
				
			} catch (BadLocationException e) {
				JavaPlugin.log(e);
			}
		}

		public int getRelevance() { return 0; 	}
		public IContextInformation getContextInformation() { return null; }
		public void apply(IDocument document) {} // not called
		public Point getSelection(IDocument document) { return null; }
		public void selected(ITextViewer viewer, boolean smartToggle) {}
		public void unselected(ITextViewer viewer) { }
		public boolean validate(IDocument document, int offset, DocumentEvent event) { return false; }
	}
	
	public static void installLinkedVisibilityProposals(LinkedCorrectionProposal proposal, ASTRewrite rewrite, List modifiers) {
		ASTNode modifier= findVisibilityModifier(modifiers);
		if (modifier != null) {
			int selected= ((Modifier) modifier).getKeyword().toFlagValue();
			proposal.addLinkedPosition(rewrite.track(modifier), false, KEY_MODIFIER);
			proposal.addLinkedPositionProposal(KEY_MODIFIER, new ModifierLinkedModeProposal(selected));
			
			// add all others
			int[] flagValues= { Modifier.PUBLIC, 0, Modifier.PROTECTED, Modifier.PRIVATE };
			for (int i= 0; i < flagValues.length; i++) {
				if (flagValues[i] != selected) {
					proposal.addLinkedPositionProposal(KEY_MODIFIER,  new ModifierLinkedModeProposal(flagValues[i]));
				}
			}
		} 
	}
	
	private static Modifier findVisibilityModifier(List modifiers) {
		for (int i= 0; i < modifiers.size(); i++) {
			Object curr= modifiers.get(i);
			if (curr instanceof Modifier) {
				Modifier modifier= (Modifier) curr;
				ModifierKeyword keyword= modifier.getKeyword();
				if (keyword == ModifierKeyword.PUBLIC_KEYWORD || keyword == ModifierKeyword.PROTECTED_KEYWORD || keyword == ModifierKeyword.PRIVATE_KEYWORD) {
					return modifier;
				}
			}
		}
		return null;
	}

	public static final boolean hasSuppressWarningsProposal(int problemId) {
		return CorrectionEngine.getWarningToken(problemId) != null; // Surpress warning annotations
	}
	
	
	public static void addSuppressWarningsProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) {
		if (problem.isError()) {
			return;
		}
		String warningToken= CorrectionEngine.getWarningToken(problem.getProblemId());
		if (warningToken == null) {
			return;
		}
		/*for (Iterator iter= proposals.iterator(); iter.hasNext();) {
			Object element= iter.next();
			if (element instanceof ICommandAccess && ADD_SUPPRESSWARNINGS_ID.equals(((ICommandAccess) element).getCommandId())) {
				return; // only one at a time
			}
		}*/
		
		ASTNode node= problem.getCoveringNode(context.getASTRoot());
		if (node == null) {
			return;
		}
		if (node.getLocationInParent() == VariableDeclarationFragment.NAME_PROPERTY) {
			ASTNode parent= node.getParent();
			if (parent.getLocationInParent() == VariableDeclarationStatement.FRAGMENTS_PROPERTY) {
				addSuppressWarningsProposal(context.getCompilationUnit(), parent.getParent(), warningToken, -2, proposals);
				return;
			}
		} else if (node.getLocationInParent() == SingleVariableDeclaration.NAME_PROPERTY) {
			addSuppressWarningsProposal(context.getCompilationUnit(), node.getParent(), warningToken, -2, proposals);
			return;
		}
		
		ASTNode target= ASTResolving.findParentBodyDeclaration(node);
		if (target != null) {
			addSuppressWarningsProposal(context.getCompilationUnit(), target, warningToken, -3, proposals);
		}
	}
	
	private static String getFirstFragmentName(List fragments) {
		if (fragments.size() > 0) {
			return ((VariableDeclarationFragment) fragments.get(0)).getName().getIdentifier();
		}
		return new String();
	}
	
	private static void addSuppressWarningsProposal(ICompilationUnit cu, ASTNode node, String warningToken, int relevance, Collection proposals) {

		ChildListPropertyDescriptor property= null;
		String name;
		switch (node.getNodeType()) {
			case ASTNode.SINGLE_VARIABLE_DECLARATION:
				property= SingleVariableDeclaration.MODIFIERS2_PROPERTY;
				name= ((SingleVariableDeclaration) node).getName().getIdentifier();
				break;
			case ASTNode.VARIABLE_DECLARATION_STATEMENT:
				property= VariableDeclarationStatement.MODIFIERS2_PROPERTY;
				name= getFirstFragmentName(((VariableDeclarationStatement) node).fragments());
				break;
			case ASTNode.TYPE_DECLARATION:
				property= TypeDeclaration.MODIFIERS2_PROPERTY;
				name= ((TypeDeclaration) node).getName().getIdentifier();
				break;
			case ASTNode.ANNOTATION_TYPE_DECLARATION:
				property= AnnotationTypeDeclaration.MODIFIERS2_PROPERTY;
				name= ((AnnotationTypeDeclaration) node).getName().getIdentifier();
				break;
			case ASTNode.ENUM_DECLARATION:
				property= EnumDeclaration.MODIFIERS2_PROPERTY;
				name= ((EnumDeclaration) node).getName().getIdentifier();
				break;
			case ASTNode.FIELD_DECLARATION:	
				property= FieldDeclaration.MODIFIERS2_PROPERTY;
				name= getFirstFragmentName(((FieldDeclaration) node).fragments());
				break;
			case ASTNode.INITIALIZER:
				property= Initializer.MODIFIERS2_PROPERTY;
				name= CorrectionMessages.ModifierCorrectionSubProcessor_suppress_warnings_initializer_label;
				break;
			case ASTNode.METHOD_DECLARATION:
				property= MethodDeclaration.MODIFIERS2_PROPERTY;
				name= ((MethodDeclaration) node).getName().getIdentifier() + "()"; //$NON-NLS-1$
				break;
			case ASTNode.ANNOTATION_TYPE_MEMBER_DECLARATION:
				property= AnnotationTypeMemberDeclaration.MODIFIERS2_PROPERTY;
				name= ((AnnotationTypeMemberDeclaration) node).getName().getIdentifier() + "()"; //$NON-NLS-1$
				break;
			case ASTNode.ENUM_CONSTANT_DECLARATION:
				property= EnumConstantDeclaration.MODIFIERS2_PROPERTY;
				name= ((EnumConstantDeclaration) node).getName().getIdentifier();
				break;
			default:
				JavaPlugin.logErrorMessage("SuppressWarning quick fix: wrong node kind: " + node.getNodeType()); //$NON-NLS-1$
				return;
		}
		
		AST ast= node.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);
		
		StringLiteral newStringLiteral= ast.newStringLiteral();
		newStringLiteral.setLiteralValue(warningToken);
		
		Annotation existing= findExistingAnnotation((List) node.getStructuralProperty(property));
		if (existing == null) {
			ListRewrite listRewrite= rewrite.getListRewrite(node, property);
			
			SingleMemberAnnotation newAnnot= ast.newSingleMemberAnnotation();
			newAnnot.setTypeName(ast.newSimpleName("SuppressWarnings")); //$NON-NLS-1$

			newAnnot.setValue(newStringLiteral);
			
			listRewrite.insertFirst(newAnnot, null);
		} else if (existing instanceof SingleMemberAnnotation) {
			SingleMemberAnnotation annotation= (SingleMemberAnnotation) existing;
			Expression value= annotation.getValue();
			if (!addSuppressArgument(rewrite, value, newStringLiteral)) {
				rewrite.set(existing, SingleMemberAnnotation.VALUE_PROPERTY, newStringLiteral, null);
			}
		} else if (existing instanceof NormalAnnotation) {
			NormalAnnotation annotation= (NormalAnnotation) existing;
			Expression value= findValue(annotation.values());
			if (!addSuppressArgument(rewrite, value, newStringLiteral)) {
				ListRewrite listRewrite= rewrite.getListRewrite(annotation, NormalAnnotation.VALUES_PROPERTY);
				MemberValuePair pair= ast.newMemberValuePair();
				pair.setName(ast.newSimpleName("value")); //$NON-NLS-1$
				pair.setValue(newStringLiteral);
				listRewrite.insertFirst(pair, null);
			}	
		}
		String label= Messages.format(CorrectionMessages.ModifierCorrectionSubProcessor_suppress_warnings_label, new String[] { warningToken, name });
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_ANNOTATION);
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, relevance, image);
		proposal.setCommandId(ADD_SUPPRESSWARNINGS_ID);
		proposals.add(proposal);
	}
	
	private static boolean addSuppressArgument(ASTRewrite rewrite, Expression value, StringLiteral newStringLiteral) {
		if (value instanceof ArrayInitializer) {
			ListRewrite listRewrite= rewrite.getListRewrite(value, ArrayInitializer.EXPRESSIONS_PROPERTY);
			listRewrite.insertLast(newStringLiteral, null);
		} else if (value instanceof StringLiteral) {
			ArrayInitializer newArr= rewrite.getAST().newArrayInitializer();
			newArr.expressions().add(rewrite.createMoveTarget(value));
			newArr.expressions().add(newStringLiteral);
			rewrite.replace(value, newArr, null);
		} else {
			return false;
		}
		return true;
	}
	
	
	private static Expression findValue(List keyValues) {
		for (int i= 0, len= keyValues.size(); i < len; i++) {
			MemberValuePair curr= (MemberValuePair) keyValues.get(i);
			if ("value".equals(curr.getName().getIdentifier())) { //$NON-NLS-1$
				return curr.getValue();
			}
		}
		return null;
	}
	
	private static Annotation findExistingAnnotation(List modifiers) {
		for (int i= 0, len= modifiers.size(); i < len; i++) {
			Object curr= modifiers.get(i);
			if (curr instanceof NormalAnnotation || curr instanceof SingleMemberAnnotation) {
				Annotation annotation= (Annotation) curr;
				String fullyQualifiedName= annotation.getTypeName().getFullyQualifiedName();
				if ("SuppressWarnings".equals(fullyQualifiedName) || "java.lang.SuppressWarnings".equals(fullyQualifiedName)) { //$NON-NLS-1$ //$NON-NLS-2$
					return annotation;
				}
			}
		}
		return null;
	}
}
