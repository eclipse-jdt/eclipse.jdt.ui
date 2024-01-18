/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction.proposals;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.text.edits.TextEdit;

import org.eclipse.ltk.core.refactoring.Change;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IModuleDescription;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ModuleDeclaration;
import org.eclipse.jdt.core.dom.ModuleDirective;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.RequiresDirective;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.manipulation.ChangeCorrectionProposalCore;
import org.eclipse.jdt.core.manipulation.SharedASTProviderCore;
import org.eclipse.jdt.core.provisional.JavaModelAccess;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;

import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

public class AddModuleRequiresCorrectionProposalCore extends ChangeCorrectionProposalCore {

	private final String fChangeDescription;
	private final String fModuleName;
	private final ICompilationUnit fModuleCu;

	public AddModuleRequiresCorrectionProposalCore(String moduleName, String changeName, String changeDescription, ICompilationUnit moduleCu, int relevance) {
		super(changeName, null, relevance);
		this.fModuleName= moduleName;
		this.fChangeDescription= changeDescription;
		this.fModuleCu= moduleCu;
	}

	@Override
	protected Change createChange() throws CoreException {
		CompilationUnitChange addRequiresChange= createAddRequiresChange();
		return addRequiresChange;
	}

	@Override
	public Object getAdditionalProposalInfo(IProgressMonitor monitor) {
		return getChangeDescription();
	}

	private CompilationUnitChange createAddRequiresChange() throws CoreException {
		if (getModuleName() == null || getModuleCu() == null) {
			return null;
		}
		CompilationUnit astRoot= SharedASTProviderCore.getAST(getModuleCu(), SharedASTProviderCore.WAIT_YES, null);
		ModuleDeclaration moduleDecl= astRoot.getModule();
		if (moduleDecl == null) {
			return null;
		}
		AST ast= astRoot.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);
		CompilationUnitChange cuChange= null;
		ListRewrite listRewrite= rewrite.getListRewrite(moduleDecl, ModuleDeclaration.MODULE_DIRECTIVES_PROPERTY);
		boolean requiresAlreadyPresent= false;
		List<ModuleDirective> moduleStatements= moduleDecl.moduleStatements();
		ModuleDirective lastModuleRequiresDirective= null;
		if (moduleStatements != null) {
			for (ModuleDirective directive : moduleStatements) {
				if (directive instanceof RequiresDirective) {
					Name name= ((RequiresDirective) directive).getName();
					if (getModuleName().equals(name.getFullyQualifiedName())) {
						requiresAlreadyPresent= true;
						break;
					}
					lastModuleRequiresDirective= directive;
				}
			}
		}
		if (!requiresAlreadyPresent) {
			RequiresDirective exp= ast.newRequiresDirective();
			exp.setName(ast.newName(getModuleName()));
			if (lastModuleRequiresDirective != null) {
				listRewrite.insertAfter(exp, lastModuleRequiresDirective, null);
			} else {
				listRewrite.insertLast(exp, null);
			}
			try {
				cuChange= new CompilationUnitChange(getName(), getModuleCu());
				TextEdit resultingEdits= rewrite.rewriteAST();
				TextChangeCompatibility.addTextEdit(cuChange, getName(), resultingEdits);
			} catch (IllegalArgumentException e1) {
				JavaManipulationPlugin.log(e1);
			}
		}

		return cuChange;
	}

	public ICompilationUnit getModuleCu() {
		return fModuleCu;
	}

	public String getModuleName() {
		return fModuleName;
	}

	public String getChangeDescription() {
		return fChangeDescription;
	}

	/**
	 * Returns the list of package fragments for the matching types based on a given string pattern. The
	 * remaining parameters are used to narrow down the type of expected results.
	 *
	 * @param stringPattern the given pattern
	 * @param typeRule determines the nature of the searched elements
	 * @param javaElement limits the search scope to this element
	 * @return list of package fragments for the matching types
	 */
	public static List<IPackageFragment> getPackageFragmentsOfMatchingTypesImpl(String stringPattern, int typeRule, IJavaElement javaElement) {
		int matchRule= SearchPattern.R_ERASURE_MATCH | SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE;
		SearchPattern searchPattern= SearchPattern.createPattern(stringPattern, typeRule, IJavaSearchConstants.DECLARATIONS, matchRule);
		if (searchPattern == null) {
			return null;
		}
		List<IPackageFragment> packageFragments= new ArrayList<>();
		SearchRequestor requestor= new SearchRequestor() {
			@Override
			public void acceptSearchMatch(SearchMatch match) throws CoreException {
				Object element= match.getElement();
				if (element instanceof IPackageFragment) {
					packageFragments.add((IPackageFragment) element);
				} else if (element instanceof IType) {
					IType enclosingType= (IType) element;
					packageFragments.add(enclosingType.getPackageFragment());
				}
			}
		};
		SearchParticipant[] participants= new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() };
		IJavaSearchScope scope= SearchEngine.createJavaSearchScope(new IJavaElement[] { javaElement });
		try {
			new SearchEngine().search(searchPattern, participants, scope, requestor, null);
		} catch (CoreException e) {
			//do nothing
		}
		return packageFragments;
	}

	public static IModuleDescription getModuleDescription(IJavaElement element) {
		IModuleDescription projectModule= null;
		try {
			switch (element.getElementType()) {
				case IJavaElement.JAVA_PROJECT:
					IJavaProject project= (IJavaProject) element;
					if (JavaModelUtil.is9OrHigher(project)) {
						projectModule= project.getModuleDescription();
					}
					break;
				case IJavaElement.PACKAGE_FRAGMENT_ROOT:
					IPackageFragmentRoot root= (IPackageFragmentRoot) element;
					projectModule= root.getModuleDescription();
					break;
				default:
					//do nothing
			}
			if (projectModule == null) {
				projectModule= JavaModelAccess.getAutomaticModuleDescription(element);
			}
		} catch (JavaModelException | IllegalArgumentException e) {
			JavaManipulationPlugin.log(e);
		}
		return projectModule;
	}
}
