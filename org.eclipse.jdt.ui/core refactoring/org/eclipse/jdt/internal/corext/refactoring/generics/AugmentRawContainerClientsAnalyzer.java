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

package org.eclipse.jdt.internal.corext.refactoring.generics;

import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTRequestor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;

import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.AugmentRawContainerClientsTCModel;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ITypeConstraint2;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.util.SearchUtils;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;


public class AugmentRawContainerClientsAnalyzer {
	
	private final IJavaElement[] fElements;
	private HashSet fProcessedCus;
	private AugmentRawContainerClientsTCModel fTypeConstraintFactory;
	private HashMap fDeclarationsToUpdate;
	private HashMap fCastsToRemove;

	public AugmentRawContainerClientsAnalyzer(IJavaElement[] elements) {
		fElements= elements;
		fProcessedCus= new HashSet();
	}

	public void analyzeContainerReferences(IProgressMonitor pm, RefactoringStatus result) throws CoreException {
		pm.beginTask("", 10); //$NON-NLS-1$
		
		IJavaProject project= fElements[0].getJavaProject();
		fTypeConstraintFactory= new AugmentRawContainerClientsTCModel(project);
		
		GenericContainers genericContainers= GenericContainers.create(project, new SubProgressMonitor(pm, 1));
		IType[] containerTypes= genericContainers.getContainerTypes();
		
		SearchPattern pattern= RefactoringSearchEngine.createOrPattern(containerTypes, IJavaSearchConstants.REFERENCES);
		//TODO: Add container methods (from ContainerMethods)?
		// -> still misses calls of kind myObj.takeList(myObj.getList()) in CU that doesn't import List
		IJavaSearchScope searchScope= SearchEngine.createJavaSearchScope(fElements, IJavaSearchScope.SOURCES);
		
//		analyzeInCompilerLoop(project, searchScope, pattern, new SubProgressMonitor(pm, 9), result);
		
		SearchParticipant[] participants= SearchUtils.getDefaultSearchParticipants();
		SearchRequestor requestor= new SearchRequestor() {
			IResource fLastResource;
			public void acceptSearchMatch(SearchMatch match) throws CoreException {
				if (match.getResource() == fLastResource)
					return;
				fLastResource= match.getResource();
				Object element= match.getElement();
				if (! (element instanceof IJavaElement))
					return;
				IJavaElement javaElement= (IJavaElement) element;
				ICompilationUnit cu= (ICompilationUnit) javaElement.getAncestor(IJavaElement.COMPILATION_UNIT);
				if (cu == null)
					return;
				if (fProcessedCus.contains(cu))
					return; // already processed
				
				//analyzeCU(cu);
				AugmentRawContClConstraintCreator unitCollector= new AugmentRawContClConstraintCreator(fTypeConstraintFactory);
				CompilationUnit unitAST= new RefactoringASTParser(AST.JLS3).parse(cu, true);
				unitAST.accept(unitCollector);
				ITypeConstraint2[] unitConstraints= fTypeConstraintFactory.getNewTypeConstraints();
				//TODO: add required methods/cus to "toscan" list
				fProcessedCus.add(cu);
			}
		};
		new SearchEngine().search(pattern, participants, searchScope, requestor, new SubProgressMonitor(pm, 9));
		
		fTypeConstraintFactory.newCu();
		AugmentRawContClConstraintsSolver solver= new AugmentRawContClConstraintsSolver(fTypeConstraintFactory);
		solver.solveConstraints();
		fDeclarationsToUpdate= solver.getDeclarationsToUpdate();
		fCastsToRemove= solver.getCastsToRemove();
		solver= null; //free caches
	}

	private void analyzeInCompilerLoop(IJavaProject project, IJavaSearchScope searchScope, SearchPattern pattern, IProgressMonitor pm, RefactoringStatus result) throws JavaModelException {
		pm.beginTask("", 2); //$NON-NLS-1$
		final ICompilationUnit[] cus= RefactoringSearchEngine.findAffectedCompilationUnits(pattern, searchScope, new SubProgressMonitor(pm, 1), result);
		//TODO: creation of bindings in ContainerMethods should be in loop! 
		final AugmentRawContClConstraintCreator unitCollector= new AugmentRawContClConstraintCreator(fTypeConstraintFactory);
		ASTParser parser= ASTParser.newParser(AST.JLS3);
		parser.setCompilerOptions(RefactoringASTParser.getCompilerOptions(project));
		parser.setResolveBindings(true);
		parser.setProject(project);
		parser.createASTs(new ASTRequestor() {

			public void acceptAST(ASTNode node) {
				CompilationUnit unitAST= (CompilationUnit) node;
				//TODO: Hack only works for single CU:
				ICompilationUnit cu= cus[0]; unitAST.setProperty(RefactoringASTParser.SOURCE_PROPERTY, cu);
				unitAST.accept(unitCollector);
//				ITypeConstraint2[] unitConstraints= fTypeConstraintFactory.getNewTypeConstraints();
				//TODO: add required methods/cus to "toscan" list
				fProcessedCus.add(cu);

			}

			public ICompilationUnit[] getSources() {
				//TODO: Hack only works for single CU:
				return new ICompilationUnit[] { cus[0] };
			}
		}, new SubProgressMonitor(pm, 1));
		pm.done();
	}

	public HashMap getDeclarationsToUpdate() {
		return fDeclarationsToUpdate;
	}

	public HashMap getCastsToRemove() {
		return fCastsToRemove;
	}

}
