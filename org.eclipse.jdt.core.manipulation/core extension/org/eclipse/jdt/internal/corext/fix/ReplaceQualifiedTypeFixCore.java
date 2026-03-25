/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - Add first version of ReplceQualifiedTypeFixCore
 */
package org.eclipse.jdt.internal.corext.fix;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.AbortSearchException;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;

public class ReplaceQualifiedTypeFixCore extends CompilationUnitRewriteOperationsFixCore {

	public static class ReplaceQualifiedTypeVisitor extends ASTVisitor {

		ArrayList<QualifiedName> searchResults;

		String fullQualifiedName;

		String className;

		QualifiedName qualifiedName;

		ITypeBinding sourceTypeBinding;

		public ReplaceQualifiedTypeVisitor(QualifiedName qualifiedName, ITypeBinding sourceTypeBinding, String fullQualifiedName, String className) {
			this.fullQualifiedName= fullQualifiedName;
			this.searchResults= new ArrayList<>();
			this.qualifiedName= qualifiedName;
			this.sourceTypeBinding= sourceTypeBinding;
			this.className= className;
		}

		@Override
		public boolean visit(SimpleName node) {
			if (node.getFullyQualifiedName().equals(className)) {
				//resolve fully qualified name and add to list
				IBinding binding= node.resolveBinding();
				if (binding instanceof ITypeBinding) {
					ITypeBinding tbdg= node.resolveTypeBinding();
					if (!tbdg.isEqualTo(sourceTypeBinding)) {
						throw new AbortSearchException();
					}
				}
			}
			return true;
		}

		@Override
		public boolean visit(EnumDeclaration edecl) {
			declVisit(edecl.resolveBinding());
			return true;
		}

		@Override
		public boolean visit(TypeDeclaration tdecl) {
			declVisit(tdecl.resolveBinding());
			return true;
		}

		private void declVisit(ITypeBinding binding) {
			if (binding != null) {
				if (binding.getName().equals(className)) {
					throw new AbortSearchException();
				}
				checkTypeBinding(binding);
				declVisit(binding.getSuperclass());
				for (ITypeBinding curInterface : binding.getInterfaces()) {
					checkTypeBinding(curInterface);
					declVisit(curInterface.getSuperclass());
				}
			}
		}

		private void checkTypeBinding(ITypeBinding typeBinding) {
			if (typeBinding == null) {
				throw new AbortSearchException();
			}
			ITypeBinding[] bindings= typeBinding.getDeclaredTypes();
			for (ITypeBinding binding : bindings) {
				if (Modifier.isProtected(binding.getModifiers()) || Modifier.isPublic(binding.getModifiers())) {
					if (binding.getName().equals(className) && !binding.getQualifiedName().equals(fullQualifiedName)) {
						throw new AbortSearchException();
					}
				} else if (!Modifier.isPrivate(binding.getModifiers())) {
					if (binding.getName().equals(className) && typeBinding.getPackage().getName().equals(qualifiedName.getQualifier().getFullyQualifiedName())) {
						throw new AbortSearchException();
					}
				}
			}
		}

		@Override
		public boolean visit(QualifiedName qname) {
			if (qname.getFullyQualifiedName().equals(fullQualifiedName)) {
				searchResults.add(qname);
			}
			return false;
		}

		public ArrayList<QualifiedName> getSearchResults() {
			return searchResults;
		}

	}

	public ReplaceQualifiedTypeFixCore(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperation operation) {
		super(name, compilationUnit, operation);
	}

	public static ReplaceQualifiedTypeFixCore createReplaceType(CompilationUnit compilationUnit, ASTNode node, IImportDeclaration[] imports) {
		ASTNode rootNode= node.getRoot();
		QualifiedName qualifiedNode= (QualifiedName) (node);
		String className= qualifiedNode.getName().getFullyQualifiedName();
		String fqName= qualifiedNode.getFullyQualifiedName();
		boolean isImportFound= false;
		boolean isImportStatic= Modifier.isStatic(qualifiedNode.resolveBinding().getModifiers());
		if (qualifiedNode.getParent() instanceof ImportDeclaration) {
			return null;
		}
		if (qualifiedNode.resolveBinding() instanceof IVariableBinding) {
			return null;
		}
		for (IImportDeclaration cur_import : imports) {
			String fullString= cur_import.getElementName();
			if (cur_import.isOnDemand()) {
				String onDemandImport= fullString.substring(0, cur_import.getElementName().length() - 2);
				String sourceQualifier= qualifiedNode.getQualifier().getFullyQualifiedName();
				if (sourceQualifier.equals(onDemandImport)) {
					isImportFound= true;
				}
			} else if (cur_import.getElementName().equals(qualifiedNode.getFullyQualifiedName())) {
				isImportFound= true;
			} else {
				String importClassname= fullString.substring(fullString.lastIndexOf('.') + 1);
				if (importClassname.equals(className)) {
					return null;
				}
			}
		}
		ITypeBinding sourceTypeBinding= qualifiedNode.resolveTypeBinding();
		if (sourceTypeBinding != null) {
			ReplaceQualifiedTypeVisitor rfqnVisitor= new ReplaceQualifiedTypeVisitor(qualifiedNode, sourceTypeBinding, fqName, className);
			try {
				rootNode.accept(rfqnVisitor);
				String label= CorrectionMessages.QuickAssistProcessor_replaceQualifiedName_description;
				return new ReplaceQualifiedTypeFixCore(label, compilationUnit,
						new ReplaceQualifiedTypeOperation(qualifiedNode.getName(), rfqnVisitor.getSearchResults(), !isImportFound, isImportStatic));
			} catch (AbortSearchException ase) {
				return null;
			}
		}
		return null;
	}

	public static class ReplaceQualifiedTypeOperation extends CompilationUnitRewriteOperation {

		List<QualifiedName> itemsToModify;

		SimpleName className;

		String packageToAdd;

		boolean needImport;

		boolean isImportStatic;

		public ReplaceQualifiedTypeOperation(SimpleName className, List<QualifiedName> itemsToModify, boolean needImport, boolean isImportStatic) {
			this.itemsToModify= itemsToModify;
			this.className= className;
			this.needImport= needImport;
			this.isImportStatic= isImportStatic;
		}

		@Override
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModelCore linkedModel) throws CoreException {
			TextEditGroup group= createTextEditGroup(CorrectionMessages.QuickAssistProcessor_replaceQualifiedName_description, cuRewrite);
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			for (QualifiedName itemToModify : itemsToModify) {
				SimpleName newItem= ast.newSimpleName(className.getFullyQualifiedName());
				ASTNodes.replaceButKeepComment(rewrite, itemToModify, newItem, group);
			}
			if (needImport) {
				ImportRewrite iRewrite= cuRewrite.getImportRewrite();

				if (isImportStatic) {
					iRewrite.addStaticImport(itemsToModify.get(0).resolveBinding());
				} else {
					iRewrite.addImport(itemsToModify.get(0).getFullyQualifiedName());
				}
			}
		}
	}
}
