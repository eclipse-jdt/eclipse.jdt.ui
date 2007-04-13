/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedPosition;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.Assignment.Operator;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.formatter.IndentManipulation;

import org.eclipse.jdt.internal.corext.codemanipulation.GetterSetterUtil;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.IFix;
import org.eclipse.jdt.internal.corext.fix.Java50Fix;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalModel;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalPositionGroup;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.jdt.internal.corext.refactoring.sef.SelfEncapsulateFieldRefactoring;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.fix.Java50CleanUp;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringSaveHelper;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringStarter;
import org.eclipse.jdt.internal.ui.refactoring.sef.SelfEncapsulateFieldWizard;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
  */
public class ModifierCorrectionSubProcessor {


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
			bindingDecl= varDecl.getVariableDeclaration();
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
					throw new IllegalArgumentException("not supported"); //$NON-NLS-1$
			}
			ICompilationUnit targetCU= isLocalVar ? cu : ASTResolving.findCompilationUnitForBinding(cu, context.getASTRoot(), typeBinding.getTypeDeclaration());
			if (targetCU != null) {
				Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
				proposals.add(new ModifierChangeCompletionProposal(label, targetCU, bindingDecl, selectedNode, includedModifiers, excludedModifiers, relevance, image));
			}
		}
		if (kind == TO_VISIBLE && bindingDecl.getKind() == IBinding.VARIABLE) {
			UnresolvedElementsSubProcessor.getVariableProposals(context, problem, (IVariableBinding) bindingDecl, proposals);
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
			IMethodBinding defining= Bindings.findOverriddenMethod(method, false);
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
			overriddenInClass= Bindings.findOverriddenMethodInType(curr, method);
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
			binding= ((IVariableBinding) binding).getVariableDeclaration();
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
			String label= null;
			int problemId= problem.getProblemId();

			
			int excludedModifiers= 0;
			int includedModifiers= 0;

			switch (problemId) {
				case IProblem.CannotHideAnInstanceMethodWithAStaticMethod:
				case IProblem.UnexpectedStaticModifierForMethod:
					excludedModifiers= Modifier.STATIC;
					label= Messages.format(CorrectionMessages.ModifierCorrectionSubProcessor_changemethodtononstatic_description, methodName);
					break;
				case IProblem.UnexpectedStaticModifierForField:
					excludedModifiers= Modifier.STATIC;
					label= Messages.format(CorrectionMessages.ModifierCorrectionSubProcessor_changefieldmodifiertononstatic_description, methodName);
					break;
				case IProblem.IllegalModifierCombinationFinalVolatileForField:
					excludedModifiers= Modifier.VOLATILE;
					label= CorrectionMessages.ModifierCorrectionSubProcessor_removevolatile_description;
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
					if (((IMethodBinding) binding).isConstructor()) {
						excludedModifiers |= Modifier.STATIC;
					}
					break;
				case IProblem.IllegalModifierForVariable:
					excludedModifiers= ~Modifier.FINAL;
					break;
				default:
					Assert.isTrue(false, "not supported"); //$NON-NLS-1$
					return;
			}
			
			if (label == null)
				label= Messages.format(CorrectionMessages.ModifierCorrectionSubProcessor_removeinvalidmodifiers_description, methodName);

			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			proposals.add(new ModifierChangeCompletionProposal(label, cu, binding, selectedNode, includedModifiers, excludedModifiers, relevance, image));
			
			if (problemId == IProblem.IllegalModifierCombinationFinalVolatileForField) {
				proposals.add(new ModifierChangeCompletionProposal(CorrectionMessages.ModifierCorrectionSubProcessor_removefinal_description, cu, binding, selectedNode, 0, Modifier.FINAL, relevance + 1, image));
			}
			
			if (problemId == IProblem.UnexpectedStaticModifierForField && binding instanceof IVariableBinding) {
				ITypeBinding declClass= ((IVariableBinding) binding).getDeclaringClass();
				if (declClass.isMember()) {
					proposals.add(new ModifierChangeCompletionProposal(CorrectionMessages.ModifierCorrectionSubProcessor_changemodifiertostaticfinal_description, cu, binding, selectedNode, Modifier.FINAL, Modifier.VOLATILE, relevance + 1, image));
					ASTNode parentType= context.getASTRoot().findDeclaringNode(declClass);
					if (parentType != null) {
						proposals.add(new ModifierChangeCompletionProposal(CorrectionMessages.ModifierCorrectionSubProcessor_addstatictoparenttype_description, cu, declClass, parentType, Modifier.STATIC, 0, relevance - 1, image));
					}
				}
			}
			if (problemId == IProblem.UnexpectedStaticModifierForMethod && binding instanceof IMethodBinding) {
				ITypeBinding declClass= ((IMethodBinding) binding).getDeclaringClass();
				if (declClass.isMember()) {
					ASTNode parentType= context.getASTRoot().findDeclaringNode(declClass);
					if (parentType != null) {
						proposals.add(new ModifierChangeCompletionProposal(CorrectionMessages.ModifierCorrectionSubProcessor_addstatictoparenttype_description, cu, declClass, parentType, Modifier.STATIC, 0, relevance - 1, image));
					}
				}
			}
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
			binding= ((IVariableBinding) binding).getVariableDeclaration();
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			String label= Messages.format(CorrectionMessages.ModifierCorrectionSubProcessor_changemodifiertofinal_description, binding.getName());
			proposals.add(new ModifierChangeCompletionProposal(label, cu, binding, selectedNode, Modifier.FINAL, 0, 5, image));
		}
	}
	
	public static void addOverrideAnnotationProposal(IInvocationContext context, IProblemLocation problem, Collection proposals) throws CoreException {
		IFix fix= Java50Fix.createAddOverrideAnnotationFix(context.getASTRoot(), problem);
		if (fix != null) {
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			Map options= new Hashtable();
			options.put(CleanUpConstants.ADD_MISSING_ANNOTATIONS, CleanUpConstants.TRUE);
			options.put(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE, CleanUpConstants.TRUE);
			FixCorrectionProposal proposal= new FixCorrectionProposal(fix, new Java50CleanUp(options), 5, image, context);
			proposals.add(proposal);
		}
	}
	
	public static void addDeprecatedAnnotationProposal(IInvocationContext context, IProblemLocation problem, Collection proposals) throws CoreException {
		IFix fix= Java50Fix.createAddDeprectatedAnnotation(context.getASTRoot(), problem);
		if (fix != null) {
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			Map options= new Hashtable();
			options.put(CleanUpConstants.ADD_MISSING_ANNOTATIONS, CleanUpConstants.TRUE);
			options.put(CleanUpConstants.ADD_MISSING_ANNOTATIONS_DEPRECATED, CleanUpConstants.TRUE);
			FixCorrectionProposal proposal= new FixCorrectionProposal(fix, new Java50CleanUp(options), 5, image, context);
			proposals.add(proposal);
		}
	}
	
	public static void addOverridingDeprecatedMethodProposal(IInvocationContext context, IProblemLocation problem, Collection proposals) throws CoreException {
		
		ICompilationUnit cu= context.getCompilationUnit();

		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		if (!(selectedNode instanceof MethodDeclaration)) {
			return;
		}
		boolean is50OrHigher= JavaModelUtil.is50OrHigher(cu.getJavaProject());
		MethodDeclaration methodDecl= (MethodDeclaration) selectedNode;
		AST ast= methodDecl.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);
		if (is50OrHigher) {
			Annotation annot= ast.newMarkerAnnotation();
			annot.setTypeName(ast.newName("Deprecated")); //$NON-NLS-1$
			rewrite.getListRewrite(methodDecl, methodDecl.getModifiersProperty()).insertFirst(annot, null);
		}
		Javadoc javadoc= methodDecl.getJavadoc();
		if (javadoc != null || !is50OrHigher) {
			if (!is50OrHigher) {
				javadoc= ast.newJavadoc();
				rewrite.set(methodDecl, MethodDeclaration.JAVADOC_PROPERTY, javadoc, null);
			}
			TagElement newTag= ast.newTagElement();
			newTag.setTagName(TagElement.TAG_DEPRECATED);
			JavadocTagsSubProcessor.insertTag(rewrite.getListRewrite(javadoc, Javadoc.TAGS_PROPERTY), newTag, null);
		}
		
		String label= CorrectionMessages.ModifierCorrectionSubProcessor_overrides_deprecated_description;
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, 15, image);
		proposals.add(proposal);
	}
		
	public static void removeOverrideAnnotationProposal(IInvocationContext context, IProblemLocation problem, Collection proposals) throws CoreException {
		ICompilationUnit cu= context.getCompilationUnit();

		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		if (!(selectedNode instanceof MethodDeclaration)) {
			return;
		}
		MethodDeclaration methodDecl= (MethodDeclaration) selectedNode;
		Annotation annot= findAnnotation("java.lang.Override", methodDecl.modifiers()); //$NON-NLS-1$
		if (annot != null) {
			ASTRewrite rewrite= ASTRewrite.create(annot.getAST());
			rewrite.remove(annot, null);
			String label= CorrectionMessages.ModifierCorrectionSubProcessor_remove_override;
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, 6, image);
			proposals.add(proposal);
			
			QuickAssistProcessor.getCreateInSuperClassProposals(context, methodDecl.getName(), proposals);
		}
	}

	private static final String KEY_MODIFIER= "modifier"; //$NON-NLS-1$
	
	private static class ModifierLinkedModeProposal extends LinkedProposalPositionGroup.Proposal {

		private final int fModifier;

		public ModifierLinkedModeProposal(int modifier, int relevance) {
			super(null, null, relevance);
			fModifier= modifier;
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

		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.corext.fix.PositionGroup.Proposal#computeEdits(int, org.eclipse.jface.text.link.LinkedPosition, char, int, org.eclipse.jface.text.link.LinkedModeModel)
		 */
		public TextEdit computeEdits(int offset, LinkedPosition currentPosition, char trigger, int stateMask, LinkedModeModel model) throws CoreException {
			try {
				IDocument document= currentPosition.getDocument();
				MultiTextEdit edit= new MultiTextEdit();
				int documentLen= document.getLength();
				if (fModifier == 0) {
					int end= currentPosition.offset + currentPosition.length; // current end position
					int k= end;
					while (k < documentLen && IndentManipulation.isIndentChar(document.getChar(k))) {
						k++;
					}
					// first remove space then replace range (remove space can destroy empty position)
					edit.addChild(new ReplaceEdit(end, k - end, new String())); // remove extra spaces
					edit.addChild(new ReplaceEdit(currentPosition.offset, currentPosition.length, new String()));
				} else {
					// first then replace range the insert space (insert space can destroy empty position)
					edit.addChild(new ReplaceEdit(currentPosition.offset, currentPosition.length, ModifierKeyword.fromFlagValue(fModifier).toString()));
					int end= currentPosition.offset + currentPosition.length; // current end position
					if (end < documentLen && !Character.isWhitespace(document.getChar(end))) {
						edit.addChild(new ReplaceEdit(end, 0, String.valueOf(' '))); // insert extra space
					}
				}
				return edit;
			} catch (BadLocationException e) {
				throw new CoreException(new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, IStatus.ERROR, e.getMessage(), e));
			}
		}
	}
	
	public static void installLinkedVisibilityProposals(LinkedProposalModel linkedProposalModel, ASTRewrite rewrite, List modifiers, boolean inInterface) {
		ASTNode modifier= findVisibilityModifier(modifiers);
		if (modifier != null) {
			int selected= ((Modifier) modifier).getKeyword().toFlagValue();
			
			LinkedProposalPositionGroup positionGroup= linkedProposalModel.getPositionGroup(KEY_MODIFIER, true);
			positionGroup.addPosition(rewrite.track(modifier), false);
			positionGroup.addProposal(new ModifierLinkedModeProposal(selected, 10));
			
			// add all others
			int[] flagValues= inInterface ? new int[] { Modifier.PUBLIC, 0 } : new int[] { Modifier.PUBLIC, 0, Modifier.PROTECTED, Modifier.PRIVATE };
			for (int i= 0; i < flagValues.length; i++) {
				if (flagValues[i] != selected) {
					positionGroup.addProposal(new ModifierLinkedModeProposal(flagValues[i], 9 - i));
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
	
	private static Annotation findAnnotation(String qualifiedTypeName, List modifiers) {
		for (int i= 0; i < modifiers.size(); i++) {
			Object curr= modifiers.get(i);
			if (curr instanceof Annotation) {
				Annotation annot= (Annotation) curr;
				ITypeBinding binding= annot.getTypeName().resolveTypeBinding();
				if (binding != null && qualifiedTypeName.equals(binding.getQualifiedName())) {
					return annot;
				}
			}
		}
		return null;
	}
	
	private static class ProposalParameter {
		final boolean useSuper;
		final ICompilationUnit compilationUnit;
		final ASTRewrite astRewrite;
		final Expression accessNode;
		final Expression qualifier;
		final IVariableBinding variableBinding;
		public ProposalParameter(boolean useSuper, ICompilationUnit compilationUnit, ASTRewrite rewrite, Expression accessNode, Expression qualifier, IVariableBinding variableBinding) {
			this.useSuper= useSuper;
			this.compilationUnit= compilationUnit;
			this.astRewrite= rewrite;
			this.accessNode= accessNode;
			this.qualifier= qualifier;
			this.variableBinding= variableBinding;
		}
	}
	
	private static class SelfEncapsulateFieldProposal extends ChangeCorrectionProposal {

		private IField fField;

		public SelfEncapsulateFieldProposal(int relevance, IField field, boolean isReadAccess) {
			super(getDescription(isReadAccess), null, relevance, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE));
			fField=field;
		}

		private static String getDescription(boolean getter) {
			if (getter)
				return CorrectionMessages.ModifierCorrectionSubProcessor_creategetterunsingencapsulatefield_description;
			else
				return CorrectionMessages.ModifierCorrectionSubProcessor_createsetterusingencapsulatefield_description;
		}

		public void apply(IDocument document) {
			Shell shell= JavaPlugin.getActiveWorkbenchShell();
			try {
				final SelfEncapsulateFieldRefactoring refactoring= new SelfEncapsulateFieldRefactoring(fField);
				refactoring.setVisibility(Flags.AccPublic);
				refactoring.setConsiderVisibility(false);//private field references are just searched in local file
				new RefactoringStarter().activate(refactoring, new SelfEncapsulateFieldWizard(refactoring), shell, "", RefactoringSaveHelper.SAVE_JAVA_ONLY_UPDATES); //$NON-NLS-1$
			} catch (JavaModelException e) {
				ExceptionHandler.handle(e, CorrectionMessages.ModifierCorrectionSubProcessor_encapsulate_field_error_title, CorrectionMessages.ModifierCorrectionSubProcessor_encapsulate_field_error_message);
			}
		}
	}
	
	public static void addGetterSetterProposal(IInvocationContext context, IProblemLocation problem, Collection proposals, int relevance) {
		ASTNode coveringNode = problem.getCoveringNode(context.getASTRoot());
		ICompilationUnit compilationUnit= context.getCompilationUnit();
		if (coveringNode instanceof SimpleName) {
			SimpleName sn=(SimpleName) coveringNode;
			if (sn.isDeclaration())
				return;
			IVariableBinding variableBinding= (IVariableBinding) sn.resolveBinding();
			if (variableBinding==null || !variableBinding.isField())
				return;
			ChangeCorrectionProposal proposal= getProposal(compilationUnit,sn,variableBinding,relevance);
			if (proposal!=null)
				proposals.add(proposal);
		}
	}

	private static ChangeCorrectionProposal getProposal(ICompilationUnit cu, SimpleName sn, IVariableBinding variableBinding, int relevance) {
		Expression accessNode= sn;
		Expression qualifier= null;
		AST ast= sn.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);
		boolean useSuper= false;
		boolean writeAccess = ASTResolving.isWriteAccess(sn);
		ASTNode parent = sn.getParent();
		switch(parent.getNodeType()){
		case ASTNode.QUALIFIED_NAME:
				accessNode= (Expression) parent;
				qualifier= ((QualifiedName) parent).getQualifier();
				break;
			case ASTNode.SUPER_FIELD_ACCESS:
				accessNode= (Expression) parent;
				qualifier= ((SuperFieldAccess) parent).getQualifier();
				useSuper= true;
				break;
		}
		ProposalParameter gspc= new ProposalParameter(useSuper, cu, rewrite, accessNode, qualifier, variableBinding);
		if (writeAccess)
			return addSetterProposal(gspc, relevance);
		else
			return addGetterProposal(gspc, relevance);		
	}

	/**
	 * Proposes a getter for this field
	 * @param context 
	 * @param relevance relevance of this proposal
	 * @return the proposal if available or null
	 */
	private static ChangeCorrectionProposal addGetterProposal(ProposalParameter context, int relevance) {
		IMethodBinding method = findGetter(context);
		if (method!=null){
			Expression mi = createMethodInvocation(context, method, null);
			context.astRewrite.replace(context.accessNode, mi, null);
			
			String label=Messages.format(CorrectionMessages.ModifierCorrectionSubProcessor_replacewithgetter_description, context.accessNode);
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.compilationUnit, context.astRewrite, relevance, image);
			return proposal;
		} else {
			IJavaElement element= context.variableBinding.getJavaElement();
			if (element instanceof IField) {
				IField field= (IField) element;
				try {
					if (RefactoringAvailabilityTester.isSelfEncapsulateAvailable(field))
						return new SelfEncapsulateFieldProposal(relevance,field,true);
				} catch (JavaModelException e) {
					JavaPlugin.log(e);
				}
			}
		}
		return null;
	}

	private static IMethodBinding findGetter(ProposalParameter context) {
		ITypeBinding returnType= context.variableBinding.getType();
		String getterName= GetterSetterUtil.getGetterName(context.variableBinding, context.compilationUnit.getJavaProject(), null, isBoolean(context));
		ITypeBinding declaringType= context.variableBinding.getDeclaringClass();
		IMethodBinding getter= Bindings.findMethodInHierarchy(declaringType, getterName, new ITypeBinding[0]);
		if (getter != null && getter.getReturnType().isAssignmentCompatible(returnType) && Modifier.isStatic(getter.getModifiers()) == Modifier.isStatic(context.variableBinding.getModifiers()))
			return getter;
		return null;
	}

	private static Expression createMethodInvocation(ProposalParameter context, IMethodBinding method, Expression argument) {
		AST ast= context.astRewrite.getAST();
		Expression qualifier= context.qualifier;
		if (context.useSuper) {
			SuperMethodInvocation invocation= ast.newSuperMethodInvocation();
			invocation.setName(ast.newSimpleName(method.getName()));
			if (qualifier != null)
				invocation.setQualifier((Name) context.astRewrite.createCopyTarget(qualifier));
			if (argument != null)
				invocation.arguments().add(argument);
			return invocation;
		} else {
			MethodInvocation invocation= ast.newMethodInvocation();
			invocation.setName(ast.newSimpleName(method.getName()));
			if (qualifier != null)
				invocation.setExpression((Expression) context.astRewrite.createCopyTarget(qualifier));
			if (argument != null)
				invocation.arguments().add(argument);
			return invocation;
		}
	}
	
	/**
	 * Proposes a setter for this field
	 * @param context 
	 * @param relevance relevance of this proposal
	 * @return the proposal if available or null
	 */
	private static ChangeCorrectionProposal addSetterProposal(ProposalParameter context, int relevance) {
		boolean isBoolean= isBoolean(context);
		String setterName= GetterSetterUtil.getSetterName(context.variableBinding, context.compilationUnit.getJavaProject(), null, isBoolean);
		ITypeBinding declaringType= context.variableBinding.getDeclaringClass();
		IMethodBinding method= Bindings.findMethodInHierarchy(declaringType, setterName, new ITypeBinding[] { context.variableBinding.getType() });
		if (method != null && Bindings.isVoidType(method.getReturnType()) && (Modifier.isStatic(method.getModifiers()) == Modifier.isStatic(context.variableBinding.getModifiers()))) {
			Expression assignedValue= getAssignedValue(context);
			if (assignedValue == null)
				return null; //we don't know how to handle those cases.
			Expression mi= createMethodInvocation(context, method, assignedValue);
			context.astRewrite.replace(context.accessNode.getParent(), mi, null);

			String label= Messages.format(CorrectionMessages.ModifierCorrectionSubProcessor_replacewithsetter_description, context.accessNode);
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.compilationUnit, context.astRewrite, relevance, image);
			return proposal;
		} else {
			IJavaElement element= context.variableBinding.getJavaElement();
			if (element instanceof IField) {
				IField field= (IField) element;
				try {
					if (RefactoringAvailabilityTester.isSelfEncapsulateAvailable(field))
						return new SelfEncapsulateFieldProposal(relevance, field, false);
				} catch (JavaModelException e) {
					JavaPlugin.log(e);
				}
			}
		}
		return null;
	}

	private static boolean isBoolean(ProposalParameter context) {
		AST ast= context.astRewrite.getAST();
		boolean isBoolean= ast.resolveWellKnownType("boolean") == context.variableBinding.getType(); //$NON-NLS-1$
		if (!isBoolean)
			isBoolean= ast.resolveWellKnownType("java.lang.Boolean") == context.variableBinding.getType(); //$NON-NLS-1$
		return isBoolean;
	}

	private static Expression getAssignedValue(ProposalParameter context) {
		ASTNode parent= context.accessNode.getParent();
		AST ast= context.astRewrite.getAST();
		switch (parent.getNodeType()) {
			case ASTNode.ASSIGNMENT:
				Assignment assignment= ((Assignment) parent);
				Expression rightHandSide= assignment.getRightHandSide();
				Expression copiedRightOp= (Expression) context.astRewrite.createCopyTarget(rightHandSide);
				if (isNotInBlock(parent))
					break;
				if (assignment.getOperator() == Operator.ASSIGN) {
					ITypeBinding rightHandSideType= rightHandSide.resolveTypeBinding();
					copiedRightOp= checkForNarrowCast(context, copiedRightOp, true, rightHandSideType);
					return copiedRightOp;
				}
				IMethodBinding getter= findGetter(context);
				if (getter != null) {
					InfixExpression infix= ast.newInfixExpression();
					infix.setLeftOperand(createMethodInvocation(context, getter, null));
					infix.setOperator(ASTNodes.convertToInfixOperator(assignment.getOperator()));
					infix.setRightOperand(copiedRightOp);
					ITypeBinding infixType= infix.resolveTypeBinding();
					return checkForNarrowCast(context, infix, true, infixType);
				}
				break;
			case ASTNode.POSTFIX_EXPRESSION:
				PostfixExpression po= (PostfixExpression) parent;
				if (isNotInBlock(parent))
					break;
				InfixExpression.Operator postfixOp= null;
				if (po.getOperator() == PostfixExpression.Operator.INCREMENT)
					postfixOp= InfixExpression.Operator.PLUS;
				if (po.getOperator() == PostfixExpression.Operator.DECREMENT)
					postfixOp= InfixExpression.Operator.MINUS;
				if (postfixOp == null)
					break;
				return createInfixInvocationFromPostPrefixExpression(context, postfixOp);
			case ASTNode.PREFIX_EXPRESSION:
				PrefixExpression pe= (PrefixExpression) parent;
				if (isNotInBlock(parent))
					break;
				InfixExpression.Operator prefixOp= null;
				if (pe.getOperator() == PrefixExpression.Operator.INCREMENT)
					prefixOp= InfixExpression.Operator.PLUS;
				if (pe.getOperator() == PrefixExpression.Operator.DECREMENT)
					prefixOp= InfixExpression.Operator.MINUS;
				if (prefixOp == null)
					break;
				return createInfixInvocationFromPostPrefixExpression(context, prefixOp);
		}

		return null;
	}

	private static boolean isNotInBlock(ASTNode parent) {
		ASTNode grandParent= parent.getParent();
		return (grandParent.getNodeType() != ASTNode.EXPRESSION_STATEMENT) || (grandParent.getParent().getNodeType() != ASTNode.BLOCK);
	}

	private static Expression createInfixInvocationFromPostPrefixExpression(ProposalParameter context, InfixExpression.Operator operator) {
		AST ast= context.astRewrite.getAST();
		IMethodBinding getter= findGetter(context);
		if (getter != null) {
			InfixExpression infix= ast.newInfixExpression();
			infix.setLeftOperand(createMethodInvocation(context, getter, null));
			infix.setOperator(operator);
			NumberLiteral number= ast.newNumberLiteral();
			number.setToken("1"); //$NON-NLS-1$
			infix.setRightOperand(number);
			ITypeBinding infixType= infix.resolveTypeBinding();
			return checkForNarrowCast(context, infix, true, infixType);
		}
		return null;
	}

	/**
	 * 
	 * @param context general context
	 * @param expression the right handside
	 * @param parenthesize if true places () around expression
	 * @param expressionType the type of the right handside. Can be null
	 * @return the casted expression if necessary
	 */
	private static Expression checkForNarrowCast(ProposalParameter context, Expression expression, boolean parenthesize, ITypeBinding expressionType) {
		PrimitiveType castTo=null;
		ITypeBinding type= context.variableBinding.getType();
		if (type.isEqualTo(expressionType))
			return expression; //no cast for same type
		AST ast= context.astRewrite.getAST();
		if (JavaModelUtil.is50OrHigher(context.compilationUnit.getJavaProject())){
			if (ast.resolveWellKnownType("java.lang.Character").isEqualTo(type)) //$NON-NLS-1$
				castTo=ast.newPrimitiveType(PrimitiveType.CHAR);
			if (ast.resolveWellKnownType("java.lang.Byte").isEqualTo(type)) //$NON-NLS-1$
				castTo=ast.newPrimitiveType(PrimitiveType.BYTE);
			if (ast.resolveWellKnownType("java.lang.Short").isEqualTo(type)) //$NON-NLS-1$
				castTo=ast.newPrimitiveType(PrimitiveType.SHORT);	
		}
		if (ast.resolveWellKnownType("char").isEqualTo(type)) //$NON-NLS-1$
			castTo=ast.newPrimitiveType(PrimitiveType.CHAR);
		if (ast.resolveWellKnownType("byte").isEqualTo(type)) //$NON-NLS-1$
			castTo=ast.newPrimitiveType(PrimitiveType.BYTE);
		if (ast.resolveWellKnownType("short").isEqualTo(type)) //$NON-NLS-1$
			castTo=ast.newPrimitiveType(PrimitiveType.SHORT);
		if (castTo!=null){
			CastExpression cast= ast.newCastExpression();
			if (parenthesize){
				ParenthesizedExpression parenthesized= ast.newParenthesizedExpression();
				parenthesized.setExpression(expression);
				cast.setExpression(parenthesized);
			} else
				cast.setExpression(expression);
			cast.setType(castTo);
			return cast;
		}
		return expression;
	}

}
