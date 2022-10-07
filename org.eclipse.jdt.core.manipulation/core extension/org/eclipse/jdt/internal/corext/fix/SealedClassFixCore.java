/*******************************************************************************
 * Copyright (c) 2022 Red Hat Inc. and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;

import org.eclipse.jdt.internal.corext.codemanipulation.ContextSensitiveImportRewriteContext;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;
import org.eclipse.jdt.internal.ui.text.correction.IProblemLocationCore;
import org.eclipse.jdt.internal.ui.util.ASTHelper;

public class SealedClassFixCore extends CompilationUnitRewriteOperationsFixCore {

	public SealedClassFixCore(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperation operation) {
		super(name, compilationUnit, operation);
	}

	public static class AddTypeAsPermittedSubTypeProposalOperation extends CompilationUnitRewriteOperation {

		private TypeDeclaration fsealedType;

		private ITypeBinding fSubTypeBinding;

		private TypeDeclaration fSubType;

		public AddTypeAsPermittedSubTypeProposalOperation(TypeDeclaration sealedType, TypeDeclaration subType, ITypeBinding subTypeBinding) {
			fsealedType= sealedType;
			fSubType= subType;
			fSubTypeBinding= subTypeBinding;
		}

		@Override
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModelCore model) throws CoreException {
			AST ast= fsealedType.getAST();
			String subTypeName= fSubType.getName().getIdentifier();
			Type type= ast.newSimpleType(ast.newSimpleName(subTypeName));

			ASTRewrite astRewrite= cuRewrite.getASTRewrite();
			astRewrite.getListRewrite(fsealedType, TypeDeclaration.PERMITS_TYPES_PROPERTY).insertLast(type, null);

			ImportRewrite importRewrite= cuRewrite.getImportRewrite();
			ImportRewriteContext importRewriteContext= new ContextSensitiveImportRewriteContext(fsealedType.getRoot(), importRewrite);
			importRewrite.addImport(fSubTypeBinding, astRewrite.getAST(), importRewriteContext);
		}
	}

	public static class AddSealedAsDirectSuperTypeProposalOperation extends CompilationUnitRewriteOperation {

		private TypeDeclaration fPermittedTypeDeclaration;

		private TypeDeclaration fSealedType;

		public AddSealedAsDirectSuperTypeProposalOperation(TypeDeclaration permittedTypeDeclaration, TypeDeclaration sealedType) {
			this.fPermittedTypeDeclaration= permittedTypeDeclaration;
			this.fSealedType= sealedType;
		}

		@Override
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModelCore linkedModel) throws CoreException {
			AST ast= fPermittedTypeDeclaration.getAST();
			String sealedTypeName= fSealedType.getName().getIdentifier();
			Type type= ast.newSimpleType(ast.newSimpleName(sealedTypeName));

			boolean isSealedInterface= fSealedType.isInterface();

			ASTRewrite astRewrite= cuRewrite.getASTRewrite();
			if (isSealedInterface) {
				astRewrite.getListRewrite(fPermittedTypeDeclaration, TypeDeclaration.SUPER_INTERFACE_TYPES_PROPERTY).insertLast(type, null);
			} else {
				astRewrite.set(fPermittedTypeDeclaration, TypeDeclaration.SUPERCLASS_TYPE_PROPERTY, type, null);
			}

			ImportRewrite importRewrite= cuRewrite.getImportRewrite();
			ImportRewriteContext importRewriteContext= new ContextSensitiveImportRewriteContext(fPermittedTypeDeclaration.getRoot(), importRewrite);
			importRewrite.addImport(fSealedType.resolveBinding(), astRewrite.getAST(), importRewriteContext);
		}
	}

	public static SealedClassFixCore addTypeAsPermittedSubTypeProposal(CompilationUnit cu, IProblemLocationCore problem) {
		ASTNode selectedNode= problem.getCoveringNode(cu);

		IType sealedType= getSealedType(selectedNode);
		if (sealedType == null) {
			return null;
		}

		while (selectedNode.getParent() instanceof Type) {
			selectedNode= selectedNode.getParent();
		}

		TypeDeclaration subType= getDeclaringType(selectedNode);
		if (subType == null) {
			return null;
		}

		ITypeBinding subTypeBinding= subType.resolveBinding();

		ICompilationUnit compilationUnit= getCompilationUnitForSealedType(sealedType);
		if (compilationUnit == null) {
			return null;
		}
		CompilationUnitRewrite cuRewrite= new CompilationUnitRewrite(compilationUnit);
		TypeDeclaration declaration;
		try {
			declaration= ASTNodeSearchUtil.getTypeDeclarationNode(sealedType, cuRewrite.getRoot());
		} catch (JavaModelException e) {
			return null;
		}
		if (declaration == null) {
			return null;
		}

		AddTypeAsPermittedSubTypeProposalOperation op= new AddTypeAsPermittedSubTypeProposalOperation(declaration, subType, subTypeBinding);

		String subTypeName= subType.getName().getIdentifier();
		String sealedTypeName= sealedType.getElementName();
		String label= Messages.format(CorrectionMessages.LocalCorrectionsSubProcessor_declareSubClassAsPermitsSealedClass_description, new String[] { subTypeName, sealedTypeName });

		return new SealedClassFixCore(label, cuRewrite.getRoot(), op);
	}

	public static SealedClassFixCore addSealedAsDirectSuperTypeProposal(CompilationUnit cu, IProblemLocationCore problem) {
		ASTNode selectedNode= problem.getCoveringNode(cu);

		TypeDeclaration sealedType= getSealedTypeNodeFromPermitsNode(selectedNode);

		if (sealedType == null) {
			return null;
		}

		IType permittedTypeElement= getPermittedType(selectedNode);
		if (permittedTypeElement == null) {
			return null;
		}

		ICompilationUnit compilationUnit= permittedTypeElement.getCompilationUnit();
		if (compilationUnit == null) {
			return null;
		}

		CompilationUnitRewrite cuRewrite= new CompilationUnitRewrite(compilationUnit);
		TypeDeclaration permittedTypeDeclaration;
		try {
			permittedTypeDeclaration= ASTNodeSearchUtil.getTypeDeclarationNode(permittedTypeElement, cuRewrite.getRoot());
		} catch (JavaModelException e) {
			return null;
		}
		if (permittedTypeDeclaration == null) {
			return null;
		}

		boolean isSealedInterface= sealedType.isInterface();
		String permittedTypeName= permittedTypeDeclaration.getName().getIdentifier();
		String sealedTypeName= sealedType.getName().getIdentifier();
		String label;
		if (isSealedInterface) {
			label= Messages.format(CorrectionMessages.LocalCorrectionsSubProcessor_declareSealedAsDirectSuperInterface_description, new String[] { sealedTypeName, permittedTypeName });
		} else {
			label= Messages.format(CorrectionMessages.LocalCorrectionsSubProcessor_declareSealedAsDirectSuperClass_description, new String[] { sealedTypeName, permittedTypeName });
		}

		AddSealedAsDirectSuperTypeProposalOperation op= new AddSealedAsDirectSuperTypeProposalOperation(permittedTypeDeclaration, sealedType);
		return new SealedClassFixCore(label, cuRewrite.getRoot(), op);
	}


	/**
	 * @param selectedNode The selected node representing a type that is inherited by the class
	 * @return a TypeDeclaration representing the declaring type of the selected node or null if not applicable
	 */
	private static TypeDeclaration getDeclaringType(ASTNode selectedNode) {
		if (selectedNode.getLocationInParent() != TypeDeclaration.SUPERCLASS_TYPE_PROPERTY
				&& selectedNode.getLocationInParent() != TypeDeclaration.SUPER_INTERFACE_TYPES_PROPERTY) {
			return null;
		}
		return (TypeDeclaration) selectedNode.getParent();
	}

	/**
	 *
	 * @param selectedNode The selected node representing a type that is inherited by the class
	 * @return an IType representing the sealed type from the inheriting class or null if not applicable
	 */
	public static IType getSealedType(ASTNode selectedNode) {
		if (selectedNode == null) {
			return null;
		}
		if (!ASTHelper.isSealedTypeSupportedInAST(selectedNode.getAST())) {
			return null;
		}

		while (selectedNode.getParent() instanceof Type) {
			selectedNode= selectedNode.getParent();
		}
		if (selectedNode.getLocationInParent() != TypeDeclaration.SUPERCLASS_TYPE_PROPERTY
				&& selectedNode.getLocationInParent() != TypeDeclaration.SUPER_INTERFACE_TYPES_PROPERTY) {
			return null;
		}
		IJavaElement sealedTypeElement= null;
		if (selectedNode instanceof SimpleType) {
			ITypeBinding typeBinding= ((SimpleType) selectedNode).resolveBinding();
			if (typeBinding != null) {
				sealedTypeElement= typeBinding.getJavaElement();
			}
		}
		if (!(sealedTypeElement instanceof IType)) {
			return null;
		}
		return (IType) sealedTypeElement;
	}

	/**
	 *
	 * @param selectedNode The selected node representing a 'permits' node type
	 * @return a TypeDeclaration representing the sealed type or null if not applicable
	 */
	private static TypeDeclaration getSealedTypeNodeFromPermitsNode(ASTNode selectedNode) {
		if (selectedNode == null) {
			return null;
		}
		if (!ASTHelper.isSealedTypeSupportedInAST(selectedNode.getAST())) {
			return null;
		}

		while (selectedNode.getParent() instanceof Type) {
			selectedNode= selectedNode.getParent();
		}
		if (selectedNode.getLocationInParent() != TypeDeclaration.PERMITS_TYPES_PROPERTY) {
			return null;
		}
		TypeDeclaration sealedType= (TypeDeclaration) selectedNode.getParent();
		return sealedType;
	}

	public static ICompilationUnit getCompilationUnitForSealedType(IType sealedType) {
		try {
			if (sealedType.isBinary() || !sealedType.isSealed()) {
				return null;
			}
		} catch (JavaModelException e) {
			return null;
		}
		return sealedType.getCompilationUnit();
	}

	/**
	 * @param selectedNode The selected node representing a 'permits' node type
	 * @return an IType representing the permitted type node or null if not applicable
	 */
	public static IType getPermittedType(ASTNode selectedNode) {
		if (selectedNode == null) {
			return null;
		}
		if (!ASTHelper.isSealedTypeSupportedInAST(selectedNode.getAST())) {
			return null;
		}

		while (selectedNode.getParent() instanceof Type) {
			selectedNode= selectedNode.getParent();
		}
		if (selectedNode.getLocationInParent() != TypeDeclaration.PERMITS_TYPES_PROPERTY) {
			return null;
		}

		IJavaElement permittedTypeElement= null;
		if (selectedNode instanceof SimpleType) {
			ITypeBinding typeBinding= ((SimpleType) selectedNode).resolveBinding();
			if (typeBinding != null) {
				permittedTypeElement= typeBinding.getJavaElement();
			}
		}
		if (!(permittedTypeElement instanceof IType)) {
			return null;
		}
		return (IType) permittedTypeElement;
	}
}
