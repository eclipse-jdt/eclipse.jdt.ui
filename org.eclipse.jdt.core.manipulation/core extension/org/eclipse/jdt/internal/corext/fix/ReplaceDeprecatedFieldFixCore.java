/*******************************************************************************
 * Copyright (c) 2025 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
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

import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;

import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.refactoring.structure.ImportRemover;

public class ReplaceDeprecatedFieldFixCore extends CompilationUnitRewriteOperationsFixCore {

	public ReplaceDeprecatedFieldFixCore(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperation operation) {
		super(name, compilationUnit, operation);
	}

	public ReplaceDeprecatedFieldFixCore(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperation operations[]) {
		super(name, compilationUnit, operations);
	}

	public static ReplaceDeprecatedFieldFixCore create(String name, String replacement, CompilationUnit compilationUnit, ASTNode node) {
		return new ReplaceDeprecatedFieldFixCore(name, compilationUnit, new ReplaceDeprecatedFieldProposalOperation(node, replacement));
	}

	public static class ReplaceDeprecatedFieldProposalOperation extends CompilationUnitRewriteOperation {

		private final ASTNode fNode;
		private final String fReplacement;

		public ReplaceDeprecatedFieldProposalOperation(ASTNode node, String name) {
			this.fNode= node;
			this.fReplacement= name;
		}

		@Override
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModelCore linkedModel) throws CoreException {
			AST ast= cuRewrite.getAST();
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			ImportRewrite importRewrite= cuRewrite.getImportRewrite();
			ImportRemover importRemover= cuRewrite.getImportRemover();
			IPackageDeclaration[] packageDecls= cuRewrite.getCu().getPackageDeclarations();

			IVariableBinding fieldBinding= null;
			switch (fNode.getNodeType()) {
				case ASTNode.QUALIFIED_NAME:
					fieldBinding= (IVariableBinding) ((QualifiedName)fNode).resolveBinding();
					break;
				case ASTNode.SIMPLE_NAME:
					fieldBinding= (IVariableBinding) ((SimpleName)fNode).resolveBinding();
					break;
				case ASTNode.FIELD_ACCESS:
					fieldBinding= ((FieldAccess)fNode).resolveFieldBinding();
					break;
				case ASTNode.SUPER_FIELD_ACCESS:
					fieldBinding= ((SuperFieldAccess)fNode).resolveFieldBinding();
					break;
				default:
					break;
			}

			boolean sameClass= false;
			boolean mustQualify= false;
			int simpleNameQualifierIndex= -1;
			String simpleNameQualifier= ""; //$NON-NLS-1$
			int lastDot= fReplacement.lastIndexOf('.');
			if (lastDot > 0) {
				int startIndex= lastDot - 1;
				while (startIndex >= 0 && Character.isJavaIdentifierPart(fReplacement.charAt(startIndex))) {
					--startIndex;
				}
				simpleNameQualifierIndex= startIndex + 1;
				simpleNameQualifier= fReplacement.substring(simpleNameQualifierIndex, lastDot);
				if (fieldBinding != null) {
					ITypeBinding classBinding= fieldBinding.getDeclaringClass();
					String className= classBinding.getQualifiedName();
					if (className != null && className.equals(fReplacement.substring(0, lastDot))) {
						sameClass= true;
					}
				}
				IImportDeclaration[] importDecls= cuRewrite.getCu().getImports();
				for (IImportDeclaration importDecl : importDecls) {
					String elementName= importDecl.getElementName();
					if (!importDecl.isOnDemand()) {
						int elementLastDot= elementName.lastIndexOf('.');
						if (elementLastDot > 0) {
							String importName= elementName.substring(elementLastDot + 1);
							if (importName.equals(simpleNameQualifier) && !elementName.equals(fReplacement.substring(0, lastDot))
									&& !fReplacement.substring(0, lastDot).equals(packageDecls[0].getElementName())) {
								mustQualify= !sameClass;
								break;
							}
						}
					}
				}
			}

			if (mustQualify) {
				Name newName= ast.newName(fReplacement);
				rewrite.replace(fNode, newName, null);
			} else if (sameClass) {
				class GetSimpleNameVisitor extends ASTVisitor {
					private SimpleName nameToReplace= fNode instanceof SimpleName simpleName ? simpleName : null;
					@Override
					public boolean visit(SimpleName node) {
						if (node.getLocationInParent() == FieldAccess.NAME_PROPERTY
								|| node.getLocationInParent() == SuperFieldAccess.NAME_PROPERTY
								|| node.getLocationInParent() == QualifiedName.NAME_PROPERTY) {
							nameToReplace= node;
						}
						return true;
					}
					public SimpleName getNameToReplace() {
						return nameToReplace;
					}
				}
				GetSimpleNameVisitor visitor= new GetSimpleNameVisitor();
				fNode.accept(visitor);
				SimpleName nameToReplace= visitor.getNameToReplace();
				SimpleName newName= ast.newSimpleName(fReplacement.substring(lastDot + 1));
				rewrite.replace(nameToReplace, newName, null);
			} else {
				importRewrite.addImport(fReplacement.substring(0, lastDot));
				Name newName= ast.newName(fReplacement.substring(simpleNameQualifierIndex));
				rewrite.replace(fNode, newName, null);
			}
			importRemover.registerRemovedNode(fNode);
		}
	}

}
