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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;

import org.eclipse.jdt.internal.corext.dom.AbortSearchException;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

public class ReplaceQualifiedTypeFixCore implements IProposableFix {

	public static class ReplaceQualifiedTypeVisitor extends ASTVisitor {

		ArrayList<QualifiedName> searchResults;
		String fullQualifiedName;
		String className;
		QualifiedName sourceBinding;
		ITypeBinding sourceTypeBinding;

		public ReplaceQualifiedTypeVisitor(QualifiedName sourceBinding, ITypeBinding sourceTypeBinding, String fullQualifiedName, String className,ArrayList<QualifiedName> searchResults) {
			this.fullQualifiedName = fullQualifiedName;
			this.searchResults = searchResults;
			this.sourceBinding = sourceBinding;
			this.sourceTypeBinding = sourceTypeBinding;
			this.className = className;
		}

		@Override
		public boolean visit(SimpleName node) {
			if ( node.getFullyQualifiedName().equals(className)) {
				//resolve fully qualified name and add to list
				IBinding binding = node.resolveBinding();
				if ( binding instanceof ITypeBinding) {
					//System.out.println("node: " + node.toString());
					ITypeBinding tbdg = node.resolveTypeBinding();
					if(!tbdg.isEqualTo(sourceTypeBinding)) {
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
			//Do something
			//System.out.println("tdecl: " + tdecl.getName());
			declVisit(tdecl.resolveBinding());
			return true;
		}

		private boolean declVisit(ITypeBinding binding) {
			if (binding == null) {
				return false;
			}
			if (binding.getName().equals(className)) {
				throw new AbortSearchException();
			}
			checkTypeBinding(binding);

			declVisit(binding.getSuperclass());
			for(ITypeBinding curInterface : binding.getInterfaces()) {
				checkTypeBinding(curInterface);
				declVisit(curInterface.getSuperclass());
			}
			return false;
		}

		private void checkTypeBinding(ITypeBinding typeBinding) {
			if (typeBinding == null) {
				throw new AbortSearchException();
			}
			ITypeBinding[] bindings = typeBinding.getDeclaredTypes();
			for ( ITypeBinding binding : bindings) {
				if(Modifier.isProtected(binding.getModifiers()) || Modifier.isPublic(binding.getModifiers())) {
					if (binding.getName().equals(className) && !binding.getQualifiedName().equals(fullQualifiedName)) {
						throw new AbortSearchException();
					}
				} else if(!Modifier.isPrivate(binding.getModifiers())) {
					if(binding.getName().equals(className) && !binding.getQualifiedName().equals(fullQualifiedName)) {
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
			// We stop the visit in any case.
			return false;
		}

		public ArrayList<QualifiedName> getSearchResults() {
			return searchResults;
		}

	}

	boolean isImportFound = false;
	String className;
	String fullQualifiedName;
	IImportDeclaration[] imports;

	ArrayList<QualifiedName> searchResults;
	QualifiedName sourceBinding;

	public ReplaceQualifiedTypeFixCore(QualifiedName fullQualifiedName, IImportDeclaration[] imports) {
		this.fullQualifiedName = fullQualifiedName.getFullyQualifiedName();
		this.imports = imports;
		//Check for length
		this.className = fullQualifiedName.getName().getFullyQualifiedName();
		this.searchResults = new ArrayList<>();
		this.sourceBinding = fullQualifiedName;
	}

	@Override
	public CompilationUnitChange createChange(IProgressMonitor progressMonitor) throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	public ArrayList<QualifiedName> create(ASTNode node, String fullQualifiedName) {
		ASTNode rootNode = node.getRoot();
		for(IImportDeclaration cur_import : imports) {
			String fullString = cur_import.getElementName();
			if (cur_import.isOnDemand()) {
				String onDemandImport = fullString.substring(0, cur_import.getElementName().length()-2);
				String sourceQualifier = sourceBinding.getQualifier().getFullyQualifiedName();
				if (sourceQualifier.contains(onDemandImport)) {
					isImportFound = true;
				}
			} else if (cur_import.getElementName().equals(fullQualifiedName)) {
				isImportFound = true;
			} else {
				String importClassname = fullString.substring(fullString.lastIndexOf('.')+1);
				if (importClassname.equals(className)) {
					return null;
				}
			}
		}
		ITypeBinding sourceTypeBinding = sourceBinding.resolveTypeBinding();
		if (sourceTypeBinding != null) {
			ReplaceQualifiedTypeVisitor rfqnVisitor = new ReplaceQualifiedTypeVisitor(sourceBinding, sourceTypeBinding,fullQualifiedName, className,searchResults);
			try {
				rootNode.accept(rfqnVisitor);
				if (isImportFound) {
					System.out.println("I found the import...");
					// Don't need to add an import
				} else {
					// 	We need to add an import
				}
				ArrayList<QualifiedName> itemsToModify = rfqnVisitor.getSearchResults();
				return itemsToModify;
			} catch (AbortSearchException ase) {
				return null;
			}
		}
		// If i am arrived here, this means there was an issue in getting the typeBinding for the sourceBinding.
		// So we should abort the search
		throw new AbortSearchException();
	}

	@Override
	public String getDisplayString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getAdditionalProposalInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IStatus getStatus() {
		// TODO Auto-generated method stub
		return null;
	}

	public static class ReplaceQualiiedTypeOperation extends CompilationUnitRewriteOperation {

		@Override
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModelCore linkedModel) throws CoreException {
			// TODO Auto-generated method stub

		}

	}
}
