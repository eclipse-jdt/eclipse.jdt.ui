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
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTRequestor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
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
	private final boolean DEBUG= false;

	public AugmentRawContainerClientsAnalyzer(IJavaElement[] elements) {
		fElements= elements;
		fProcessedCus= new HashSet();
	}

	public void analyzeContainerReferences(IProgressMonitor pm, RefactoringStatus result) throws CoreException {
		pm.beginTask("", 4); //$NON-NLS-1$
		
		IJavaProject project= fElements[0].getJavaProject();
		fTypeConstraintFactory= new AugmentRawContainerClientsTCModel(project);
		
		pm.setTaskName("Building collections hierarchy...");
		GenericContainers genericContainers= GenericContainers.create(project, new SubProgressMonitor(pm, 1));
		IType[] containerTypes= genericContainers.getContainerTypes();
		
		SearchPattern pattern= RefactoringSearchEngine.createOrPattern(containerTypes, IJavaSearchConstants.REFERENCES);
		//TODO: Add container methods (from ContainerMethods)?
		// -> still misses calls of kind myObj.takeList(myObj.getList()) in CU that doesn't import List
		IJavaSearchScope searchScope= SearchEngine.createJavaSearchScope(fElements, IJavaSearchScope.SOURCES);

		SubProgressMonitor subPm= new SubProgressMonitor(pm, 3);
		if (true) { //TODO: disabled until jdt.core has been tagged
			analyzeInCompilerLoop(project, searchScope, pattern, subPm, result);
		} else {
			analyzeInSearchLoop(searchScope, pattern, subPm);
		}
		
		fTypeConstraintFactory.newCu();
		pm.setTaskName("Solving constraints...");
		AugmentRawContClConstraintsSolver solver= new AugmentRawContClConstraintsSolver(fTypeConstraintFactory);
		solver.solveConstraints();
		fDeclarationsToUpdate= solver.getDeclarationsToUpdate();
		fCastsToRemove= solver.getCastsToRemove();
		solver= null; //free caches
		pm.done();
	}

	private void analyzeInSearchLoop(IJavaSearchScope searchScope, SearchPattern pattern, SubProgressMonitor subPm) throws CoreException {
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
				fTypeConstraintFactory.newCu();
				fProcessedCus.add(cu);
			}
		};
		new SearchEngine().search(pattern, participants, searchScope, requestor, subPm);
	}

	private void analyzeInCompilerLoop(IJavaProject project, IJavaSearchScope searchScope, SearchPattern pattern, final IProgressMonitor pm, RefactoringStatus result) throws JavaModelException {
		pm.beginTask("", 2); //$NON-NLS-1$
		pm.setTaskName("Finding affected compilation units...");
		final ICompilationUnit[] cus= RefactoringSearchEngine.findAffectedCompilationUnits(pattern, searchScope, new SubProgressMonitor(pm, 1), result);
		//TODO: creation of bindings in ContainerMethods should be in loop.
		//Problem: must be completed before loop starts. => do this as first action of first call to ASTRequestor#acceptAST()
		final AugmentRawContClConstraintCreator unitCollector= new AugmentRawContClConstraintCreator(fTypeConstraintFactory);
//		String[] containerKeys= getContainerKeys(unitCollector.getTCModel());
		pm.setTaskName("Building constraints system...");
		
		ASTParser parser= ASTParser.newParser(AST.JLS3);
		parser.setCompilerOptions(RefactoringASTParser.getCompilerOptions(project));
		parser.setResolveBindings(true);
		parser.setProject(project);
		parser.createASTs(cus, new String[0], new ASTRequestor() {
			public void acceptAST(CompilationUnit ast, ICompilationUnit source) {
				pm.subTask(source.getElementName());
				if (DEBUG)
					System.out.println("ASTRequestor#acceptAST(" + source.getElementName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
				ast.setProperty(RefactoringASTParser.SOURCE_PROPERTY, source);
				ast.accept(unitCollector);
				//TODO: add required methods/cus to "toscan" list
				fTypeConstraintFactory.newCu();
				fProcessedCus.add(source);
			}
			public void acceptBinding(IBinding binding, String bindingKey) {
				if (DEBUG)
					System.out.println("ASTRequestor#acceptBinding(" + binding.getName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}, new SubProgressMonitor(pm, 1));
		pm.done();
	}

	private String[] getContainerKeys(AugmentRawContainerClientsTCModel model) {
		String[] keys= new String[] {
				model.getPrimitiveBooleanType().getKey(),
				model.getPrimitiveIntType().getKey(),
				model.getVoidType().getKey(),
				model.getObjectType().getKey(),
				model.getStringType().getKey(),
				model.getCollectionType().getKey(),
				model.getListType().getKey(),
				model.getLinkedListType().getKey(),
				model.getVectorType().getKey(),
				model.getIteratorType().getKey(),
				model.getListIteratorType().getKey(),
				model.getEnumerationType().getKey(),
				model.getCollectionsType().getKey()
		};
		return keys;
	}

	public HashMap getDeclarationsToUpdate() {
		return fDeclarationsToUpdate;
	}

	public HashMap getCastsToRemove() {
		return fCastsToRemove;
	}

}
