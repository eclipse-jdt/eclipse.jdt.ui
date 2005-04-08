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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeLiteral;

import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.types.TType;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.types.TypeEnvironment;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.typesets.EnumeratedTypeSet;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.typesets.TypeSet;
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
	
	private static final String REWRITTEN= "InferTypeArgumentsRefactoring.rewritten"; //$NON-NLS-1$
	
	private TextChangeManager fChangeManager;
	private final IJavaElement[] fElements;
	private InferTypeArgumentsTCModel fTCModel;

	private boolean fAssumeCloneReturnsSameType;
	private boolean fLeaveUnconstrainedRaw;
	
	private InferTypeArgumentsRefactoring(IJavaElement[] elements) {
		fElements= elements;
	}
	
	public static InferTypeArgumentsRefactoring create(IJavaElement[] elements) {
		if (RefactoringAvailabilityTester.isInferTypeArgumentsAvailable(elements)) {
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
		//TODO: check selection: no binaries
		pm.done();
		return new RefactoringStatus();
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#checkFinalConditions(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkFinalConditions(final IProgressMonitor pm) throws CoreException, OperationCanceledException {
		HashMap/*<IJavaProject, List<JavaElement>>*/ projectsToElements= getJavaElementsPerProject(fElements);
		pm.beginTask("", projectsToElements.size() + 1); //$NON-NLS-1$
		pm.setTaskName(RefactoringCoreMessages.getString("InferTypeArgumentsRefactoring.checking_preconditions")); //$NON-NLS-1$
		try {
			RefactoringStatus result= check15();
			
			fTCModel= new InferTypeArgumentsTCModel();
			final InferTypeArgumentsConstraintCreator unitCollector= new InferTypeArgumentsConstraintCreator(fTCModel, fAssumeCloneReturnsSameType);
			
			pm.setTaskName(RefactoringCoreMessages.getString("InferTypeArgumentsRefactoring.building")); //$NON-NLS-1$
			
			for (Iterator iter= projectsToElements.entrySet().iterator(); iter.hasNext(); ) {
				Entry entry= (Entry) iter.next();
				IJavaProject project= (IJavaProject) entry.getKey();
				List javaElementsList= (List) entry.getValue();
				IJavaElement[] javaElements= (IJavaElement[]) javaElementsList.toArray(new IJavaElement[javaElementsList.size()]);
				final ICompilationUnit[] cus= JavaModelUtil.getAllCompilationUnits(javaElements);
				
				final SubProgressMonitor projectMonitor= new SubProgressMonitor(pm, 1);
				ASTParser parser= ASTParser.newParser(AST.JLS3);
				parser.setCompilerOptions(RefactoringASTParser.getCompilerOptions(project));
				parser.setResolveBindings(true);
				parser.setProject(project);
				parser.createASTs(cus, new String[0], new ASTRequestor() {
					public void acceptAST(ICompilationUnit source, CompilationUnit ast) {
						projectMonitor.subTask(source.getElementName());
						ast.setProperty(RefactoringASTParser.SOURCE_PROPERTY, source);
						ast.accept(unitCollector);
						fTCModel.newCu();
					}
					public void acceptBinding(String bindingKey, IBinding binding) {
						//do nothing
					}
				}, projectMonitor);
				
				projectMonitor.done();
				fTCModel.newCu();
			}
			pm.setTaskName(RefactoringCoreMessages.getString("InferTypeArgumentsRefactoring.solving")); //$NON-NLS-1$
			InferTypeArgumentsConstraintsSolver solver= new InferTypeArgumentsConstraintsSolver(fTCModel);
			solver.solveConstraints();
			HashMap declarationsToUpdate= solver.getDeclarationsToUpdate();
			HashMap castsToRemove= solver.getCastsToRemove();
			solver= null; //free caches
			
			fChangeManager= new TextChangeManager();
			rewriteDeclarations(declarationsToUpdate, castsToRemove, new SubProgressMonitor(pm, 1));
			
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
		
//		SuperTypesSet.clear();
//		SubTypesSet.clear();
//		SubTypesOfSingleton.clear();
//		TypeUniverseSet.clear();
//		SuperTypesOfSingleton.clear();
		EnumeratedTypeSet.resetCount();
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
		// Problem: only FATAL errors from checkInitialConditions() are shown to the user.
		RefactoringStatus result= new RefactoringStatus();
		HashSet/*<IJavaProject>*/ checkedProjects= new HashSet/*<IJavaProject>*/();
		
		for (int i= 0; i < fElements.length; i++) {
			IJavaProject javaProject= fElements[i].getJavaProject();
			if (! checkedProjects.contains(javaProject)) {
				if (! JavaModelUtil.is50OrHigher(javaProject)) {
					String message= RefactoringCoreMessages.getFormattedString("InferTypeArgumentsRefactoring.notCompileUnless50", javaProject.getElementName()); //$NON-NLS-1$
					result.addError(message);
				}
				checkedProjects.add(javaProject);
			}
		}
		return result;
	}

	private void rewriteDeclarations(HashMap /*<ICompilationUnit, List<ConstraintVariable2>>*/ declarationsToUpdate, HashMap castsToRemove, IProgressMonitor pm) throws CoreException {
		Set entrySet= declarationsToUpdate.entrySet();
		pm.beginTask("", entrySet.size()); //$NON-NLS-1$
		pm.setTaskName(RefactoringCoreMessages.getString("InferTypeArgumentsRefactoring.creatingChanges")); //$NON-NLS-1$
		for (Iterator iter= entrySet.iterator(); iter.hasNext();) {
			Map.Entry entry= (Map.Entry) iter.next();
			ICompilationUnit cu= (ICompilationUnit) entry.getKey();
			pm.worked(1);
			pm.subTask(cu.getElementName());

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
					rewriteCastVariable(castCv, rewrite);
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
			ConstraintVariable2 parentElement= ((CollectionElementVariable2) cv).getParentConstraintVariable();
			if (parentElement instanceof VariableVariable2) {
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
			} else if (parentElement instanceof TypeVariable2) {
				TypeVariable2 typeCv= (TypeVariable2) parentElement;
				rewriteTypeVariable(typeCv, rewrite);
			
			} else {
				//TODO
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
			
			rewrite.getASTRewrite().replace(originalType, newType, rewrite.createGroupDescription(RefactoringCoreMessages.getString("InferTypeArgumentsRefactoring.addTypeArguments"))); //$NON-NLS-1$
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
				ITypeBinding[] binding= TypeEnvironment.createTypeBindings(new TType[] {chosenType}, rewrite.getCu().getJavaProject());
				if (binding[0] == null) {
					return null;
				} else {
					typeArgument= rewrite.getImportRewrite().addImport(binding[0], rewrite.getAST());
					//TODO: elements:
					ArrayList nestedTypeArgumentCvs= getTypeArgumentCvs(elementCv);
					Type[] nestedTypeArguments= getTypeArguments(typeArgument, nestedTypeArgumentCvs, rewrite);
					if (nestedTypeArguments != null) {
						ParameterizedType parameterizedType= rewrite.getAST().newParameterizedType(typeArgument);
						for (int j= 0; j < nestedTypeArguments.length; j++)
							parameterizedType.typeArguments().add(nestedTypeArguments[j]);
						typeArgument= parameterizedType;
					}
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
		CastExpression castExpression= (CastExpression) castCv.getRange().getNode(rewrite.getRoot());
		Expression expression= castExpression.getExpression();
		ASTNode nodeToReplace;
		if (castExpression.getParent() instanceof ParenthesizedExpression)
			nodeToReplace= castExpression.getParent();
		else
			nodeToReplace= castExpression;
		
		Expression newExpression= (Expression) rewrite.getASTRewrite().createMoveTarget(expression);
		rewrite.getASTRewrite().replace(nodeToReplace, newExpression, rewrite.createGroupDescription(RefactoringCoreMessages.getString("InferTypeArgumentsRefactoring.removeCast"))); //$NON-NLS-1$
		rewrite.getImportRemover().registerRemovedNode(nodeToReplace);
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
