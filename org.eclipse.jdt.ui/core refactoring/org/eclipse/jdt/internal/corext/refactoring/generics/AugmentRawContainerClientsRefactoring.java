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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.core.resources.IFile;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.CollectionElementVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ConstraintVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.TypeHandle;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.TypeVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.VariableVariable2;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class AugmentRawContainerClientsRefactoring extends Refactoring {

	private TextChangeManager fChangeManager;
	private final IJavaElement[] fElements;
	private AugmentRawContainerClientsAnalyzer fAnalyzer;
	
	private AugmentRawContainerClientsRefactoring(IJavaElement[] elements) {
		fElements= elements;
	}
	
	public static boolean isAvailable(IJavaElement[] elements) {
		return elements.length > 0;
	}

	public static AugmentRawContainerClientsRefactoring create(IJavaElement[] elements) {
		if (isAvailable(elements)) {
			return new AugmentRawContainerClientsRefactoring(elements);
		}
		return null;
	}
	
	/*
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#getName()
	 */
	public String getName() {
		return RefactoringCoreMessages.getString("AugmentRawContainerClientsRefactoring.name"); //$NON-NLS-1$
	}
	
	/*
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#checkInitialConditions(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		//TODO: check selection: no binaries
		return new RefactoringStatus();
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#checkFinalConditions(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		pm.beginTask(RefactoringCoreMessages.getString("AugmentRawContainerClientsRefactoring.checking_preconditions"), 1); //$NON-NLS-1$
		try {
			RefactoringStatus result= check15();
			fAnalyzer= new AugmentRawContainerClientsAnalyzer(fElements);
			fAnalyzer.analyzeContainerReferences(pm, result);
			
			HashMap declarationsToUpdate= fAnalyzer.getDeclarationsToUpdate();
			fChangeManager= new TextChangeManager();
			rewriteDeclarations(declarationsToUpdate);
//			result.merge(performAnalysis(pm));
			
			IFile[] filesToModify= ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits());
			result.merge(Checks.validateModifiesFiles(filesToModify, null));
			return result;
		} finally {
			pm.done();
		}
	}

	private RefactoringStatus check15() {
		RefactoringStatus result= new RefactoringStatus();
		HashMap/*<IJavaProject, Boolean>*/ project15ness= new HashMap/*<IJavaProject, Boolean>*/();
		
		for (int i= 0; i < fElements.length; i++) {
			IJavaProject javaProject= fElements[i].getJavaProject();
			if (! project15ness.containsKey(javaProject)) {
				String sourceLevel= javaProject.getOption(JavaCore.COMPILER_SOURCE, true);
				boolean is15= ! JavaCore.VERSION_1_1.equals(sourceLevel)
						&& ! JavaCore.VERSION_1_2.equals(sourceLevel)
						&& ! JavaCore.VERSION_1_3.equals(sourceLevel)
						&& ! JavaCore.VERSION_1_4.equals(sourceLevel);
				if (! is15) {
					String message= "Changes in project '" + javaProject.getElementName() + "' will not compile until compiler source level is raised to 1.5.";
					result.addError(message);
				}
				project15ness.put(javaProject, Boolean.valueOf(is15));
			}
		}
		return result;
	}

//	/**
//	 * Perform the type inference analysis and collect the results.
//	 * @return RefactoringStatus
//	 */
//	private RefactoringStatus performAnalysis(IProgressMonitor pm) {
//		AugmentRawContainerClientsAnalyzer analyzer= new AugmentRawContainerClientsAnalyzer(fOwningClass, fAffectedUnits, fConstraintOptimizations, fDebugMessages);
//	
//		analyzer.setModifyPublicMembers(fModifyPublicMembers);
//		analyzer.setInferMethodTypeParms(fInferMethodTypeParms);
//		analyzer.analyze(pm);
//	
//		Set	affectedCUs= analyzer.getAffectedCUs();
//	
//		fAffectedUnits= (ICompilationUnit[]) affectedCUs.toArray(new ICompilationUnit[affectedCUs.size()]);
//	
//		fDeclarationSites= analyzer.computeDeclarationSiteModifications();
//		fAllocationSites= analyzer.computeAllocationSiteModifications();
//		fRemovableCasts= analyzer.computeRemovableCasts();
//	
//		fAnalyzerStatistics= analyzer.getStatistics();
//	
//		return new RefactoringStatus();
//	}

	private void rewriteDeclarations(HashMap/*<ICompilationUnit, List<ConstraintVariable2>>*/ declarationsToUpdate) throws CoreException {
		RefactoringASTParser parser= new RefactoringASTParser(AST.JLS3);
		Set entrySet= declarationsToUpdate.entrySet();
		for (Iterator iter= entrySet.iterator(); iter.hasNext();) {
			Map.Entry entry= (Map.Entry) iter.next();
			ICompilationUnit cu= (ICompilationUnit) entry.getKey();
			CompilationUnit compilationUnit= parser.parse(cu, true);
			AST ast= compilationUnit.getAST();
			ASTRewrite rewrite= ASTRewrite.create(ast);
			List cvs= (List) entry.getValue();
			for (Iterator cvIter= cvs.iterator(); cvIter.hasNext();) {
				ConstraintVariable2 cv= (ConstraintVariable2) cvIter.next();
				rewriteConstraintVariable(compilationUnit, ast, rewrite, cv);
			}
			TextBuffer buffer= null;
			try {
				buffer= TextBuffer.acquire((IFile) cu.getResource());
				TextEdit edit= rewrite.rewriteAST(buffer.getDocument(), declarationsToUpdate);
				CompilationUnitChange change= new CompilationUnitChange(cu.getElementName(), cu);
				change.setEdit(edit);
				fChangeManager.manage(cu, change);
			} finally {
				TextBuffer.release(buffer);
			}
			
		}
		
	}

	private void rewriteConstraintVariable(CompilationUnit compilationUnit, AST ast, ASTRewrite rewrite, ConstraintVariable2 cv) {
		//TODO: make this clean
		if (cv instanceof CollectionElementVariable2) {
			CollectionElementVariable2 elementCv= (CollectionElementVariable2) cv;
			ConstraintVariable2 element= elementCv.getElementVariable();
			if (element instanceof VariableVariable2) {
				//TODO: don't change twice (as lement variable and as type variable
//				String variableBindingKey= ((VariableVariable2) element).getVariableBindingKey();
//				ASTNode node= compilationUnit.findDeclaringNode(variableBindingKey);
//				if (node instanceof VariableDeclarationFragment) {
//					VariableDeclarationStatement stmt= (VariableDeclarationStatement) node.getParent();
//					Type originalType= stmt.getType();
//					if (originalType.isSimpleType() || originalType.isQualifiedType()) {
//						Type movingType= (Type) rewrite.createMoveTarget(originalType);
//						ParameterizedType newType= ast.newParameterizedType(movingType);
//						TypeHandle chosenType= AugmentRawContClConstraintsSolver.getChosenType(elementCv);
//						String typeName= chosenType.getSimpleName(); // TODO: use ImportRewrite
//						newType.typeArguments().add(rewrite.createStringPlaceholder(typeName, ASTNode.SIMPLE_TYPE));
//						rewrite.replace(originalType, newType, null); // TODO: description
//					}
//				}
			} else if (element instanceof TypeVariable2) {
				ASTNode node= ((TypeVariable2) element).getRange().getNode(compilationUnit);
				if (node instanceof SimpleName) {
					Type originalType= (Type) ((SimpleName) node).getParent();
					//TODO: C&P'd
					Type movingType= (Type) rewrite.createMoveTarget(originalType);
					ParameterizedType newType= ast.newParameterizedType(movingType);
					TypeHandle chosenType= AugmentRawContClConstraintsSolver.getChosenType(elementCv);
					String typeName= chosenType.getSimpleName(); // TODO: use ImportRewrite
					newType.typeArguments().add(rewrite.createStringPlaceholder(typeName, ASTNode.SIMPLE_TYPE));
					rewrite.replace(originalType, newType, null); // TODO: description
				}
			}
		}
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#createChange(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		pm.beginTask("", 1); //$NON-NLS-1$
		try {
			DynamicValidationStateChange result= new DynamicValidationStateChange(RefactoringCoreMessages.getString("Change.javaChanges"), fChangeManager.getAllChanges()); //$NON-NLS-1$
			fAnalyzer= null; // free memory
			return result;
		} finally{
			pm.done();
			//TODO: clear caches
		}	
	}

}
