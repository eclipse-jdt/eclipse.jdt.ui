/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.corext.refactoring.generics;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IFile;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.BindingKey;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTRequestor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeLiteral;

import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.generics.InferTypeArgumentsUpdate.CuUpdate;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.types.TType;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.typesets.EnumeratedTypeSet;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.CastVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.CollectionElementVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ConstraintVariable2;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.TypeVariable2;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaElementLabels;

import org.eclipse.jdt.internal.ui.IJavaStatusConstants;
import org.eclipse.jdt.internal.ui.JavaPlugin;

public class InferTypeArgumentsRefactoring extends Refactoring {
	
	private static final boolean BUG_98165_core_wrong_source_range= true;
	
	private static final String REWRITTEN= "InferTypeArgumentsRefactoring.rewritten"; //$NON-NLS-1$
	
	private TextChangeManager fChangeManager;
	private final IJavaElement[] fElements;
	private InferTypeArgumentsTCModel fTCModel;

	private boolean fAssumeCloneReturnsSameType;
	private boolean fLeaveUnconstrainedRaw;
	
	private InferTypeArgumentsRefactoring(IJavaElement[] elements) {
		fElements= elements;
	}
	
	public static InferTypeArgumentsRefactoring create(IJavaElement[] elements) throws JavaModelException {
		if (RefactoringAvailabilityTester.isInferTypeArgumentsAvailable(elements)) {
			return new InferTypeArgumentsRefactoring(elements);
		}
		return null;
	}
	
	/*
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#getName()
	 */
	public String getName() {
		return RefactoringCoreMessages.InferTypeArgumentsRefactoring_name; 
	}
	
	public void setAssumeCloneReturnsSameType(boolean assume) {
		fAssumeCloneReturnsSameType= assume;
	}
	
	public boolean getAssumeCloneReturnsSameType() {
		return fAssumeCloneReturnsSameType;
	}
	
	public void setLeaveUnconstrainedRaw(boolean raw) {
		fLeaveUnconstrainedRaw= raw;
	}
	
	public boolean getLeaveUnconstrainedRaw() {
		return fLeaveUnconstrainedRaw;
	}
	
	/*
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#checkInitialConditions(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		RefactoringStatus result= check15();
		pm.done();
		return result;
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#checkFinalConditions(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkFinalConditions(final IProgressMonitor pm) throws CoreException, OperationCanceledException {
		HashMap/*<IJavaProject, List<JavaElement>>*/ projectsToElements= getJavaElementsPerProject(fElements);
		pm.beginTask("", projectsToElements.size() + 2); //$NON-NLS-1$
		final RefactoringStatus result= new RefactoringStatus();
		try {
			fTCModel= new InferTypeArgumentsTCModel();
			final InferTypeArgumentsConstraintCreator unitCollector= new InferTypeArgumentsConstraintCreator(fTCModel, fAssumeCloneReturnsSameType);
			
			for (Iterator iter= projectsToElements.entrySet().iterator(); iter.hasNext(); ) {
				Entry entry= (Entry) iter.next();
				IJavaProject project= (IJavaProject) entry.getKey();
				List javaElementsList= (List) entry.getValue();
				IJavaElement[] javaElements= (IJavaElement[]) javaElementsList.toArray(new IJavaElement[javaElementsList.size()]);
				List cus= Arrays.asList(JavaModelUtil.getAllCompilationUnits(javaElements));
				
				int batchSize= 150;
				int batches= ((cus.size()-1) / batchSize) + 1;
				SubProgressMonitor projectMonitor= new SubProgressMonitor(pm, 1);
				projectMonitor.beginTask("", batches); //$NON-NLS-1$
				projectMonitor.setTaskName(RefactoringCoreMessages.InferTypeArgumentsRefactoring_building); 
				for (int i= 0; i < batches; i++) {
					List batch= cus.subList(i * batchSize, Math.min(cus.size(), (i + 1) * batchSize));
					ICompilationUnit[] batchCus= (ICompilationUnit[]) batch.toArray(new ICompilationUnit[batch.size()]);
					final SubProgressMonitor batchMonitor= new SubProgressMonitor(projectMonitor, 1);
					batchMonitor.subTask(RefactoringCoreMessages.InferTypeArgumentsRefactoring_calculating_dependencies); 
					
					ASTParser parser= ASTParser.newParser(AST.JLS3);
					parser.setProject(project);
					parser.setCompilerOptions(RefactoringASTParser.getCompilerOptions(project));
					parser.setResolveBindings(true);
					parser.createASTs(batchCus, new String[0], new ASTRequestor() {
						public void acceptAST(final ICompilationUnit source, final CompilationUnit ast) {
							batchMonitor.subTask(source.getElementName());
							ast.setProperty(RefactoringASTParser.SOURCE_PROPERTY, source);
	
							Platform.run(new ISafeRunnable() {
								public void run() throws Exception {
									IProblem[] problems= ast.getProblems();
									for (int p= 0; p < problems.length; p++) {
										if (problems[p].isError()) {
											String cuName= JavaElementLabels.getElementLabel(source, JavaElementLabels.CU_QUALIFIED);
											String msg= MessageFormat.format(RefactoringCoreMessages.InferTypeArgumentsRefactoring_error_in_cu_skipped, new Object[] {cuName});
											result.addError(msg, JavaStatusContext.create(source, new SourceRange(problems[p])));
											return;
										}
									}
									ast.accept(unitCollector);
								}
								public void handleException(Throwable exception) {
									String cuName= JavaElementLabels.getElementLabel(source, JavaElementLabels.CU_QUALIFIED);
									String msg= MessageFormat.format(RefactoringCoreMessages.InferTypeArgumentsRefactoring_internal_error, new Object[] {cuName});
									JavaPlugin.log(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IJavaStatusConstants.INTERNAL_ERROR, msg, null));
									String msg2= MessageFormat.format(RefactoringCoreMessages.InferTypeArgumentsRefactoring_error_skipped, new Object[] {cuName});
									result.addError(msg2, JavaStatusContext.create(source));
								}
							});
							
							fTCModel.newCu();
						}
						public void acceptBinding(String bindingKey, IBinding binding) {
							//do nothing
						}
					}, batchMonitor);
				}
				
				projectMonitor.done();
				fTCModel.newCu();
			}
			
//			Display.getDefault().syncExec(new Runnable() {
//				public void run() {
//					MessageDialog.openInformation(Display.getCurrent().getActiveShell(), "Debugging...", "after constraint gen");
//				}
//			});
			
			pm.setTaskName(RefactoringCoreMessages.InferTypeArgumentsRefactoring_solving); 
			InferTypeArgumentsConstraintsSolver solver= new InferTypeArgumentsConstraintsSolver(fTCModel);
			InferTypeArgumentsUpdate updates= solver.solveConstraints(new SubProgressMonitor(pm, 1));
			solver= null; //free caches
			
			fChangeManager= new TextChangeManager();
			rewriteDeclarations(updates, new SubProgressMonitor(pm, 1));
			
			IFile[] filesToModify= ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits());
			result.merge(Checks.validateModifiesFiles(filesToModify, getValidationContext()));
			return result;
		} finally {
			pm.done();
			clearGlobalState();
		}
	}
	
	private void clearGlobalState() {
		TypeSet.resetCount();
		EnumeratedTypeSet.resetCount();
		fTCModel= null;
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

	private RefactoringStatus check15() throws CoreException {
		RefactoringStatus result= new RefactoringStatus();
		HashSet/*<IJavaProject>*/ checkedProjects= new HashSet/*<IJavaProject>*/();
		
		for (int i= 0; i < fElements.length; i++) {
			IJavaProject javaProject= fElements[i].getJavaProject();
			if (! checkedProjects.contains(javaProject)) {
				if (! JavaModelUtil.is50OrHigher(javaProject)) {
					String message= Messages.format(RefactoringCoreMessages.InferTypeArgumentsRefactoring_not50, javaProject.getElementName()); 
					result.addFatalError(message);
				} else if (! JavaModelUtil.is50OrHigherJRE(javaProject)) {
					String message= Messages.format(RefactoringCoreMessages.InferTypeArgumentsRefactoring_not50Library, javaProject.getElementName());
					result.addFatalError(message);
				}
				checkedProjects.add(javaProject);
			}
		}
		return result;
	}

	private void rewriteDeclarations(InferTypeArgumentsUpdate update, IProgressMonitor pm) throws CoreException {
		HashMap/*<ICompilationUnit, CuUpdate>*/ updates= update.getUpdates();
		
		Set entrySet= updates.entrySet();
		pm.beginTask("", entrySet.size()); //$NON-NLS-1$
		pm.setTaskName(RefactoringCoreMessages.InferTypeArgumentsRefactoring_creatingChanges); 
		for (Iterator iter= entrySet.iterator(); iter.hasNext();) {
			if (pm.isCanceled())
				throw new OperationCanceledException();
			
			Map.Entry entry= (Map.Entry) iter.next();
			ICompilationUnit cu= (ICompilationUnit) entry.getKey();
			pm.worked(1);
			pm.subTask(cu.getElementName());

			CompilationUnitRewrite rewrite= new CompilationUnitRewrite(cu);
			rewrite.setResolveBindings(false);
			CuUpdate cuUpdate= (CuUpdate) entry.getValue();
			
			for (Iterator cvIter= cuUpdate.getDeclarations().iterator(); cvIter.hasNext();) {
				ConstraintVariable2 cv= (ConstraintVariable2) cvIter.next();
				rewriteConstraintVariable(cv, rewrite);
			}
			
			for (Iterator castsIter= cuUpdate.getCastsToRemove().iterator(); castsIter.hasNext();) {
				CastVariable2 castCv= (CastVariable2) castsIter.next();
				rewriteCastVariable(castCv, rewrite);
			}
			
			CompilationUnitChange change= rewrite.createChange();
			if (change != null) {
				fChangeManager.manage(cu, change);
			}
		}
		
	}

	private void rewriteConstraintVariable(ConstraintVariable2 cv, CompilationUnitRewrite rewrite) {
		if (cv instanceof CollectionElementVariable2) {
			ConstraintVariable2 parentElement= ((CollectionElementVariable2) cv).getParentConstraintVariable();
			if (parentElement instanceof TypeVariable2) {
				TypeVariable2 typeCv= (TypeVariable2) parentElement;
				rewriteTypeVariable(typeCv, rewrite);
			} else {
				//only rewrite type variables
			}
		}
	}

	private void rewriteTypeVariable(TypeVariable2 typeCv, CompilationUnitRewrite rewrite) {
		ASTNode node= typeCv.getRange().getNode(rewrite.getRoot());
		if (node instanceof SimpleName) {
			Type originalType= (Type) ((SimpleName) node).getParent();
			
			// Must rewrite all type arguments in one batch. Do the rewrite when the first one is encountered; skip the others.
			Object rewritten= originalType.getProperty(REWRITTEN); //$NON-NLS-1$
			if (rewritten == REWRITTEN)
				return;
			originalType.setProperty(REWRITTEN, REWRITTEN);
			
			ArrayList typeArgumentCvs= getTypeArgumentCvs(typeCv);
			Type[] typeArguments= getTypeArguments(originalType, typeArgumentCvs, rewrite);
			if (typeArguments == null)
				return;
			
			Type movingType= (Type) rewrite.getASTRewrite().createMoveTarget(originalType);
			ParameterizedType newType= rewrite.getAST().newParameterizedType(movingType);
			
			for (int i= 0; i < typeArguments.length; i++)
				newType.typeArguments().add(typeArguments[i]);
			
			rewrite.getASTRewrite().replace(originalType, newType, rewrite.createGroupDescription(RefactoringCoreMessages.InferTypeArgumentsRefactoring_addTypeArguments)); 
		} //TODO: other node types?
	}

	/**
	 * @return the new type arguments, or <code>null</code> iff an argument could not be infered
	 */
	private Type[] getTypeArguments(Type baseType, ArrayList typeArgumentCvs, CompilationUnitRewrite rewrite) {
		if (typeArgumentCvs.size() == 0)
			return null;
		
		Type[] typeArguments= new Type[typeArgumentCvs.size()];
		for (int i= 0; i < typeArgumentCvs.size(); i++) {
			CollectionElementVariable2 elementCv= (CollectionElementVariable2) typeArgumentCvs.get(i);
			Type typeArgument;
			TType chosenType= InferTypeArgumentsConstraintsSolver.getChosenType(elementCv);
			if (chosenType != null) {
				if (chosenType.isWildcardType() && ! unboundedWildcardAllowed(baseType))
					return null; // can't e.g. write "new ArrayList<?>()".
				if (chosenType.isParameterizedType()) // workaround for bug 99124
					chosenType= chosenType.getTypeDeclaration();
				BindingKey bindingKey= new BindingKey(chosenType.getBindingKey());
				typeArgument= rewrite.getImportRewrite().addImportFromSignature(bindingKey.internalToSignature(), rewrite.getAST());
				ArrayList nestedTypeArgumentCvs= getTypeArgumentCvs(elementCv);
				Type[] nestedTypeArguments= getTypeArguments(typeArgument, nestedTypeArgumentCvs, rewrite); //recursion
				if (nestedTypeArguments != null) {
					ParameterizedType parameterizedType= rewrite.getAST().newParameterizedType(typeArgument);
					for (int j= 0; j < nestedTypeArguments.length; j++)
						parameterizedType.typeArguments().add(nestedTypeArguments[j]);
					typeArgument= parameterizedType;
				}

			} else { // couldn't infer an element type (no constraints)
				if (fLeaveUnconstrainedRaw) {
					// every guess could be wrong => leave the whole thing raw
					return null;
				} else {
					if (unboundedWildcardAllowed(baseType)) {
						typeArgument= rewrite.getAST().newWildcardType();
					} else {
						String object= rewrite.getImportRewrite().addImport("java.lang.Object"); //$NON-NLS-1$
						typeArgument= (Type) rewrite.getASTRewrite().createStringPlaceholder(object, ASTNode.SIMPLE_TYPE);
					}
				}
//				ASTNode baseTypeParent= baseType.getParent();
//				if (baseTypeParent instanceof ClassInstanceCreation) {
//					//No ? allowed. Take java.lang.Object.
//					typeArgument= rewrite.getAST().newSimpleType(rewrite.getAST().newName(rewrite.getImportRewrite().addImport("java.lang.Object"))); //$NON-NLS-1$
//				} else if (baseTypeParent instanceof ArrayCreation || baseTypeParent instanceof InstanceofExpression) {
//					//Only ? allowed.
//					typeArgument= rewrite.getAST().newWildcardType();
//				} else {
//					//E.g. field type: can put anything. Choosing ? in order to be most constraining.
//					typeArgument= rewrite.getAST().newWildcardType();
//				}
			}
			typeArguments[i]= typeArgument;
		}
		return typeArguments;
	}

	private ArrayList/*<CollectionElementVariable2>*/ getTypeArgumentCvs(ConstraintVariable2 baseCv) {
		Map elementCvs= fTCModel.getElementVariables(baseCv);
		ArrayList typeArgumentCvs= new ArrayList();
		for (Iterator iter= elementCvs.values().iterator(); iter.hasNext();) {
			CollectionElementVariable2 elementCv= (CollectionElementVariable2) iter.next();
			int index= elementCv.getDeclarationTypeVariableIndex();
			if (index != CollectionElementVariable2.NOT_DECLARED_TYPE_VARIABLE_INDEX) {
				while (index >= typeArgumentCvs.size())
					typeArgumentCvs.add(null); // fill with null until set(index, ..) is possible
				typeArgumentCvs.set(index, elementCv);
			}
		}
		return typeArgumentCvs;
	}

	private boolean unboundedWildcardAllowed(Type originalType) {
		ASTNode parent= originalType.getParent();
		while (parent instanceof Type)
			parent= parent.getParent();
		
		if (parent instanceof ClassInstanceCreation) {
			return false;
		} else if (parent instanceof AbstractTypeDeclaration) {
			return false;
		} else if (parent instanceof TypeLiteral) {
			return false;
		}
		return true;
	}

	private void rewriteCastVariable(CastVariable2 castCv, CompilationUnitRewrite rewrite) {
		ASTNode node= castCv.getRange().getNode(rewrite.getRoot());
		if (! (node instanceof CastExpression) && BUG_98165_core_wrong_source_range)
			return;
		
		CastExpression castExpression= (CastExpression) node;
		Expression expression= castExpression.getExpression();
		ASTNode nodeToReplace;
		if (castExpression.getParent() instanceof ParenthesizedExpression)
			nodeToReplace= castExpression.getParent();
		else
			nodeToReplace= castExpression;
		
		Expression newExpression= (Expression) rewrite.getASTRewrite().createMoveTarget(expression);
		rewrite.getASTRewrite().replace(nodeToReplace, newExpression, rewrite.createGroupDescription(RefactoringCoreMessages.InferTypeArgumentsRefactoring_removeCast)); 
		rewrite.getImportRemover().registerRemovedNode(nodeToReplace);
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#createChange(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		pm.beginTask("", 1); //$NON-NLS-1$
		try {
			DynamicValidationStateChange result= new DynamicValidationStateChange(RefactoringCoreMessages.InferTypeArgumentsRefactoring_name, fChangeManager.getAllChanges()); 
			return result;
		} finally {
			pm.done();
		}	
	}

}
