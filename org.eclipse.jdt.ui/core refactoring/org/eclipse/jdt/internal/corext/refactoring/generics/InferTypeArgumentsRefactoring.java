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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IFile;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTRequestor;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Type;

import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.InferTypeArgumentsTCModel;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.CastVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.CollectionElementVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ConstraintVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.TypeVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.VariableVariable2;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class InferTypeArgumentsRefactoring extends Refactoring {

	private TextChangeManager fChangeManager;
	private final IJavaElement[] fElements;
	private InferTypeArgumentsTCModel fTCModel;
	
	private InferTypeArgumentsRefactoring(IJavaElement[] elements) {
		fElements= elements;
	}
	
	public static boolean isAvailable(IJavaElement[] elements) {
		return elements.length > 0;
	}

	public static InferTypeArgumentsRefactoring create(IJavaElement[] elements) {
		if (isAvailable(elements)) {
			return new InferTypeArgumentsRefactoring(elements);
		}
		return null;
	}
	
	/*
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#getName()
	 */
	public String getName() {
		return RefactoringCoreMessages.getString("InferTypeArgumentsRefactoring.name"); //$NON-NLS-1$
	}
	
	/*
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#checkInitialConditions(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		//TODO: check selection: no binaries
		pm.done();
		return new RefactoringStatus();
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#checkFinalConditions(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkFinalConditions(final IProgressMonitor pm) throws CoreException, OperationCanceledException {
		pm.beginTask("", 4); //$NON-NLS-1$
		pm.setTaskName(RefactoringCoreMessages.getString("InferTypeArgumentsRefactoring.checking_preconditions")); //$NON-NLS-1$
		try {
			HashMap/*<IJavaProject, List<JavaElement>>*/ projectsToElements= getJavaElementsPerProject(fElements);
			RefactoringStatus result= check15();
			
			fTCModel= new InferTypeArgumentsTCModel();
			final InferTypeArgumentsConstraintCreator unitCollector= new InferTypeArgumentsConstraintCreator(fTCModel);
			
			pm.setTaskName("Building constraints system...");
			
			for (Iterator iter= projectsToElements.entrySet().iterator(); iter.hasNext(); ) {
				Entry entry= (Entry) iter.next();
				IJavaProject project= (IJavaProject) entry.getKey();
				List javaElementsList= (List) entry.getValue();
				IJavaElement[] javaElements= (IJavaElement[]) javaElementsList.toArray(new IJavaElement[javaElementsList.size()]);
				final ICompilationUnit[] cus= JavaModelUtil.getAllCompilationUnits(javaElements);
				
				ASTParser parser= ASTParser.newParser(AST.JLS3);
				parser.setCompilerOptions(RefactoringASTParser.getCompilerOptions(project));
				parser.setResolveBindings(true);
				parser.setProject(project);
				parser.createASTs(cus, new String[0], new ASTRequestor() {

					public void acceptAST(ICompilationUnit source, CompilationUnit ast) {
						pm.subTask(source.getElementName());
						ast.setProperty(RefactoringASTParser.SOURCE_PROPERTY, source);
						ast.accept(unitCollector);
						//TODO: add required methods/cus to "toscan" list
						fTCModel.newCu();
					}

					public void acceptBinding(String bindingKey, IBinding binding) {
						//do nothing
					}
				}, new SubProgressMonitor(pm, 1));

				fTCModel.newCu();
			}
			pm.setTaskName("Solving constraints...");
			InferTypeArgumentsConstraintsSolver solver= new InferTypeArgumentsConstraintsSolver(fTCModel);
			solver.solveConstraints();
			HashMap declarationsToUpdate= solver.getDeclarationsToUpdate();
			HashMap castsToRemove= solver.getCastsToRemove();
			solver= null; //free caches
			
			fChangeManager= new TextChangeManager();
			rewriteDeclarations(declarationsToUpdate, castsToRemove, new SubProgressMonitor(pm, 1));
			
			IFile[] filesToModify= ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits());
			result.merge(Checks.validateModifiesFiles(filesToModify, null));
			return result;
		} finally {
			pm.done();
		}
	}

	private HashMap getJavaElementsPerProject(IJavaElement[] elements) {
		HashMap/*<IJavaProject, List<JavaElement>>*/ result= new HashMap/*<IJavaProject, List<JavaElement>>*/();
		for (int i= 0; i < fElements.length; i++) {
			IJavaElement element= fElements[i];
			IJavaProject javaProject= element.getJavaProject();
			ArrayList javaElements= (ArrayList) result.get(javaProject);
			if (javaElements == null) {
				javaElements= new ArrayList();
				result.put(javaProject, javaElements);
			}
			javaElements.add(element);
		}
		return result;
	}

	private RefactoringStatus check15() {
		//TODO: move to checkInitialConditions() and make an error iff jdk15 not available
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

	private void rewriteDeclarations(HashMap /*<ICompilationUnit, List<ConstraintVariable2>>*/ declarationsToUpdate, HashMap castsToRemove, IProgressMonitor pm) throws CoreException {
		Set entrySet= declarationsToUpdate.entrySet();
		pm.beginTask("", entrySet.size()); //$NON-NLS-1$
		pm.setTaskName("Creating changes...");
		for (Iterator iter= entrySet.iterator(); iter.hasNext();) {
			Map.Entry entry= (Map.Entry) iter.next();
			ICompilationUnit cu= (ICompilationUnit) entry.getKey();
			pm.worked(1);
			pm.subTask(cu.getElementName());

			//TODO: use CompilationUnitRewrite
			CompilationUnitRewrite rewrite= new CompilationUnitRewrite(cu);
			rewrite.setResolveBindings(false);
			List cvs= (List) entry.getValue();
			for (Iterator cvIter= cvs.iterator(); cvIter.hasNext();) {
				ConstraintVariable2 cv= (ConstraintVariable2) cvIter.next();
				rewriteConstraintVariable(cv, rewrite);
			}
			
			//TODO: create InferTypeArgumentsUpdate which is a mapping from CU to {declarationsToUpdate, castsToRemove, ...}
			List casts= (List) castsToRemove.get(cu);
			if (casts != null) {
				for (Iterator castsIter= casts.iterator(); castsIter.hasNext();) {
					CastVariable2 castCv= (CastVariable2) castsIter.next();
					CastExpression castExpression= (CastExpression) castCv.getRange().getNode(rewrite.getRoot());
					Expression newExpression= (Expression) rewrite.getASTRewrite().createMoveTarget(castExpression.getExpression());
					rewrite.getASTRewrite().replace(castExpression, newExpression, null); //TODO: use ImportRemover
				}
			}
			
			CompilationUnitChange change= rewrite.createChange();
			if (change != null) {
				fChangeManager.manage(cu, change);
			}
		}
		
	}

	private void rewriteConstraintVariable(ConstraintVariable2 cv, CompilationUnitRewrite rewrite) {
		//TODO: make this clean
		if (cv instanceof CollectionElementVariable2) {
			CollectionElementVariable2 elementCv= (CollectionElementVariable2) cv;
			ConstraintVariable2 element= elementCv.getElementVariable();
			if (element instanceof VariableVariable2) {
				//TODO: don't change twice (as element variable and as type variable
//				String variableBindingKey= ((VariableVariable2) element).getVariableBindingKey();
//				ASTNode node= compilationUnit.findDeclaringNode(variableBindingKey);
//				if (node instanceof VariableDeclarationFragment) {
//					VariableDeclarationStatement stmt= (VariableDeclarationStatement) node.getParent();
//					Type originalType= stmt.getType();
//					if (originalType.isSimpleType() || originalType.isQualifiedType()) {
//						Type movingType= (Type) rewrite.createMoveTarget(originalType);
//						ParameterizedType newType= ast.newParameterizedType(movingType);
//						TypeHandle chosenType= InferTypeArgumentsConstraintsSolver.getChosenType(elementCv);
//						String typeName= chosenType.getSimpleName(); // TODO: use ImportRewrite
//						newType.typeArguments().add(rewrite.createStringPlaceholder(typeName, ASTNode.SIMPLE_TYPE));
//						rewrite.replace(originalType, newType, null); // TODO: description
//					}
//				}
			} else if (element instanceof TypeVariable2) {
				ASTNode node= ((TypeVariable2) element).getRange().getNode(rewrite.getRoot());
				if (node instanceof SimpleName) {
					ITypeBinding chosenType= InferTypeArgumentsConstraintsSolver.getChosenType(elementCv);
					if (chosenType == null)
						return; // couldn't infer an element type
					Type originalType= (Type) ((SimpleName) node).getParent();
					Type movingType= (Type) rewrite.getASTRewrite().createMoveTarget(originalType);
					ParameterizedType newType= rewrite.getAST().newParameterizedType(movingType);
					newType.typeArguments().add(rewrite.getImportRewrite().addImport(chosenType, rewrite.getAST()));
					rewrite.getASTRewrite().replace(originalType, newType, null);
				} //TODO: other node types?
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
			return result;
		} finally{
			pm.done();
			//TODO: clear caches
		}	
	}

}
