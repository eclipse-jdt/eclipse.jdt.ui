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
import org.eclipse.jdt.core.dom.AST;
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
		SearchParticipant[] participants= SearchUtils.getDefaultSearchParticipants();
		IJavaSearchScope searchScope= SearchEngine.createJavaSearchScope(fElements, IJavaSearchScope.SOURCES);
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
				analyzeCU(cu);
			}
		};
		new SearchEngine().search(pattern, participants, searchScope, requestor, new SubProgressMonitor(pm, 9));
		fTypeConstraintFactory.newCu();
		
		AugmentRawContClConstraintsSolver solver= new AugmentRawContClConstraintsSolver(fTypeConstraintFactory);
		solver.solveConstraints();
		fDeclarationsToUpdate= solver.getDeclarationsToUpdate();
		solver= null; //free caches
	}


	protected void analyzeCU(ICompilationUnit cu) {
		AugmentRawContClConstraintCreator unitCollector= new AugmentRawContClConstraintCreator(fTypeConstraintFactory);
		CompilationUnit unitAST= new RefactoringASTParser(AST.JLS3).parse(cu, true);
		unitAST.accept(unitCollector);
		ITypeConstraint2[] unitConstraints= fTypeConstraintFactory.getNewTypeConstraints();
		//TODO: add required methods/cus to "toscan" list
		
		fProcessedCus.add(cu);
		
		
// -------------- from unitGranularityConstraintCollection(): -------------
		
//		GenericizeConstraintCreator gcc= new GenericizeConstraintCreator(gvf, fProject);
//		Collection/*<ITypeConstraint>*/ constraints= new HashSet();
//		GenericizeVariableFactory gvf= new GenericizeVariableFactory(fProject);
//		fCUsScanned= new HashSet();
//		
//		Set cUsToScan= new HashSet();
//
//		fContextMapper= new CallSiteToTargetMapper() { // The trivial mapping
//			public Iterator/*<MethodContextPair>*/ mapCallSiteToTargets(IContext callingCtxt, CompilationUnitRange callSite) {
//				Set results= new HashSet();
//				results.add(new MethodContextPair(null, callingCtxt));
//				return results.iterator();
//			}
//		};
//		gcc.setMapper(fContextMapper);
//
//		fMethodsToScan= new HashSet();
//		fMethodsScanned= new HashSet();
//		for(int i= 0; i < fCUs.length; i++)
//			cUsToScan.add(fCUs[i]);
//		do {
//			long cuCollectionStart= System.currentTimeMillis();
//
//			// Scan a compilation unit from cUsToScan.
//			ICompilationUnit unit= (ICompilationUnit) cUsToScan.iterator().next();
//			ConstraintCollector unitCollector= new ConstraintCollector(gcc);
//			CompilationUnit unitAST= ASTCreator.createAST(unit, null);
//
////			if (DEBUG_COLLECTION)
//				System.out.println("[" + cUsToScan.size() + "] Scanning " + unit.getParent().getElementName() + "." + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
//									unit.getElementName() + " for constraints."); //$NON-NLS-1$
//
//			cUsToScan.remove(unit);
//			fCUsScanned.add(unit);
//			unitAST.accept(unitCollector);
//
//			if (DEBUG_COLLECTION)
//				System.out.println("Scanning " + unit.getParent().getElementName() + "." + //$NON-NLS-1$ //$NON-NLS-2$
//									unit.getElementName() + " done."); //$NON-NLS-1$
//
//			ITypeConstraint[] unitConstraints= unitCollector.getConstraints();
//
//			constraints.addAll(Arrays.asList(unitConstraints));
//
//			fStatistics.fConstraintGenTime += (System.currentTimeMillis() - cuCollectionStart);
//
//			// Now add any compilation units whose analysis is required by
//			// references made in the type constraints we just got.
//			if (DEBUG_COLLECTION) System.out.println("  Adding referenced CU's to scan list..."); //$NON-NLS-1$
//
//			long callGraphStart= System.currentTimeMillis();
//
//			addReferencedCUs(unitConstraints, cUsToScan);
//			addUnitsCallingMethods(cUsToScan);
//
//			fStatistics.fTotalCallGraphTime += (System.currentTimeMillis() - callGraphStart);
//
//			if (DEBUG_COLLECTION) System.out.println("  Done adding referenced CU's to scan list."); //$NON-NLS-1$
//		} while(cUsToScan.size() > 0);
		
	}
	
	public HashMap getDeclarationsToUpdate() {
		return fDeclarationsToUpdate;
	}

}
