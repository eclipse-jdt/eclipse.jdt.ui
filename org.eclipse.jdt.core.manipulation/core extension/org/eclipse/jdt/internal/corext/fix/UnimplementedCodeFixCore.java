/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
 *     Red Hat Inc. - copied and modified for use in jdt.core.manipulation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.ArrayList;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.manipulation.ICleanUpFixCore;

import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;
import org.eclipse.jdt.internal.ui.text.correction.IProblemLocationCore;

public class UnimplementedCodeFixCore extends CompilationUnitRewriteOperationsFixCore {

	public static final class MakeTypeAbstractOperation extends CompilationUnitRewriteOperation {

		private final TypeDeclaration fTypeDeclaration;

		public MakeTypeAbstractOperation(TypeDeclaration typeDeclaration) {
			fTypeDeclaration= typeDeclaration;
		}

		@Override
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModelCore linkedProposalPositions) throws CoreException {
			AST ast= cuRewrite.getAST();
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			Modifier newModifier= ast.newModifier(Modifier.ModifierKeyword.ABSTRACT_KEYWORD);
			TextEditGroup textEditGroup= createTextEditGroup(CorrectionMessages.UnimplementedCodeFix_TextEditGroup_label, cuRewrite);
			rewrite.getListRewrite(fTypeDeclaration, TypeDeclaration.MODIFIERS2_PROPERTY).insertLast(newModifier, textEditGroup);

			LinkedProposalPositionGroupCore group= new LinkedProposalPositionGroupCore("modifier"); //$NON-NLS-1$
			group.addPosition(rewrite.track(newModifier), !linkedProposalPositions.hasLinkedPositions());
			linkedProposalPositions.addPositionGroup(group);
		}
	}

	public static ICleanUpFixCore createCleanUp(CompilationUnit root, boolean addMissingMethod, boolean makeTypeAbstract, IProblemLocationCore[] problems) {
		Assert.isLegal(!addMissingMethod || !makeTypeAbstract);
		if (!addMissingMethod && !makeTypeAbstract)
			return null;

		if (problems.length == 0)
			return null;

		ArrayList<CompilationUnitRewriteOperation> operations= new ArrayList<>();

		for (IProblemLocationCore problem : problems) {
			if (addMissingMethod) {
				ASTNode typeNode= getSelectedTypeNode(root, problem);
				if (typeNode != null && !isTypeBindingNull(typeNode)) {
					operations.add(new AddUnimplementedMethodsOperation(typeNode, null));
				}
			} else {
				ASTNode typeNode= getSelectedTypeNode(root, problem);
				if (typeNode instanceof TypeDeclaration) {
					operations.add(new MakeTypeAbstractOperation((TypeDeclaration) typeNode));
				}
			}
		}

		if (operations.isEmpty())
			return null;

		String label;
		if (addMissingMethod) {
			label= CorrectionMessages.UnimplementedMethodsCorrectionProposal_description;
		} else {
			label= CorrectionMessages.UnimplementedCodeFix_MakeAbstractFix_label;
		}
		return new UnimplementedCodeFixCore(label, root, operations.toArray(new CompilationUnitRewriteOperation[operations.size()]));
	}

	public static IProposableFix createAddUnimplementedMethodsFix(final CompilationUnit root, IProblemLocationCore problem) {
		ASTNode typeNode= getSelectedTypeNode(root, problem);
		if (typeNode == null)
			return null;

		if (isTypeBindingNull(typeNode))
			return null;

		AddUnimplementedMethodsOperation operation= new AddUnimplementedMethodsOperation(typeNode, null);
		if (operation.getMethodsToImplement().length > 0) {
			return new UnimplementedCodeFixCore(CorrectionMessages.UnimplementedMethodsCorrectionProposal_description, root, new CompilationUnitRewriteOperation[] { operation });
		}
		return null;
	}

	public static UnimplementedCodeFixCore createMakeTypeAbstractFix(CompilationUnit root, IProblemLocationCore problem) {
		ASTNode typeNode= getSelectedTypeNode(root, problem);
		if (!(typeNode instanceof TypeDeclaration))
			return null;

		TypeDeclaration typeDeclaration= (TypeDeclaration) typeNode;
		MakeTypeAbstractOperation operation= new MakeTypeAbstractOperation(typeDeclaration);

		String label= Messages.format(CorrectionMessages.ModifierCorrectionSubProcessor_addabstract_description, BasicElementLabels.getJavaElementName(typeDeclaration.getName().getIdentifier()));
		return new UnimplementedCodeFixCore(label, root, new CompilationUnitRewriteOperation[] { operation });
	}

	public static ASTNode getSelectedTypeNode(CompilationUnit root, IProblemLocationCore problem) {
		ASTNode selectedNode= problem.getCoveringNode(root);
		if (selectedNode == null)
			return null;

		if (selectedNode.getNodeType() == ASTNode.ANONYMOUS_CLASS_DECLARATION) { // bug 200016
			selectedNode= selectedNode.getParent();
		}

		if (selectedNode.getLocationInParent() == EnumConstantDeclaration.NAME_PROPERTY) {
			selectedNode= selectedNode.getParent();
		}
		if (selectedNode.getNodeType() == ASTNode.SIMPLE_NAME && selectedNode.getParent() instanceof AbstractTypeDeclaration) {
			return selectedNode.getParent();
		} else if (selectedNode.getNodeType() == ASTNode.CLASS_INSTANCE_CREATION) {
			return ((ClassInstanceCreation) selectedNode).getAnonymousClassDeclaration();
		} else if (selectedNode.getNodeType() == ASTNode.ENUM_CONSTANT_DECLARATION) {
			EnumConstantDeclaration enumConst= (EnumConstantDeclaration) selectedNode;
			if (enumConst.getAnonymousClassDeclaration() != null)
				return enumConst.getAnonymousClassDeclaration();
			return enumConst;
		} else {
			return null;
		}
	}

	private static boolean isTypeBindingNull(ASTNode typeNode) {
		if (typeNode instanceof AbstractTypeDeclaration) {
			AbstractTypeDeclaration abstractTypeDeclaration= (AbstractTypeDeclaration) typeNode;
			if (abstractTypeDeclaration.resolveBinding() == null)
				return true;

			return false;
		} else if (typeNode instanceof AnonymousClassDeclaration) {
			AnonymousClassDeclaration anonymousClassDeclaration= (AnonymousClassDeclaration) typeNode;
			if (anonymousClassDeclaration.resolveBinding() == null)
				return true;

			return false;
		} else if (typeNode instanceof EnumConstantDeclaration) {
			return false;
		} else {
			return true;
		}
	}

	public UnimplementedCodeFixCore(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperation[] fixRewriteOperations) {
		super(name, compilationUnit, fixRewriteOperations);
	}
}
