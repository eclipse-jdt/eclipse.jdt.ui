/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
package org.eclipse.jdt.internal.corext.refactoring.generics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IFile;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.ChangeDescriptor;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.BindingKey;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
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
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.InferTypeArgumentsDescriptor;

import org.eclipse.jdt.internal.core.manipulation.JavaElementLabelsCore;
import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;
import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.core.refactoring.descriptors.RefactoringSignatureDescriptorFactory;
import org.eclipse.jdt.internal.corext.CorextCore;
import org.eclipse.jdt.internal.corext.SourceRangeFactory;
import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.JDTRefactoringDescriptorComment;
import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringArguments;
import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringDescriptorUtil;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
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
import org.eclipse.jdt.internal.corext.refactoring.util.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.IJavaStatusConstants;

public class InferTypeArgumentsRefactoring extends Refactoring {

	private static final String ATTRIBUTE_CLONE= "clone"; //$NON-NLS-1$
	private static final String ATTRIBUTE_LEAVE= "leave"; //$NON-NLS-1$

	private static final String REWRITTEN= "InferTypeArgumentsRefactoring.rewritten"; //$NON-NLS-1$

	private TextChangeManager fChangeManager;
	private IJavaElement[] fElements;
	private InferTypeArgumentsTCModel fTCModel;

	private boolean fAssumeCloneReturnsSameType;
	private boolean fLeaveUnconstrainedRaw;

	/**
	 * Creates a new infer type arguments refactoring.
	 * @param elements the elements to process, or <code>null</code> if invoked by scripting
	 */
	public InferTypeArgumentsRefactoring(IJavaElement[] elements) {
		fElements= elements;
	}

    public InferTypeArgumentsRefactoring(JavaRefactoringArguments arguments, RefactoringStatus status) {
   		this(null);
   		RefactoringStatus initializeStatus= initialize(arguments);
   		status.merge(initializeStatus);
    }

	/*
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#getName()
	 */
	@Override
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
	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		RefactoringStatus result= check15();
		pm.done();
		return result;
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#checkFinalConditions(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public RefactoringStatus checkFinalConditions(final IProgressMonitor pm) throws CoreException, OperationCanceledException {
		HashMap<IJavaProject, ArrayList<IJavaElement>> projectsToElements= getJavaElementsPerProject(fElements);
		pm.beginTask("", projectsToElements.size() + 2); //$NON-NLS-1$
		final RefactoringStatus result= new RefactoringStatus();
		try {
			fTCModel= new InferTypeArgumentsTCModel();
			final InferTypeArgumentsConstraintCreator unitCollector= new InferTypeArgumentsConstraintCreator(fTCModel, fAssumeCloneReturnsSameType);

			for (Entry<IJavaProject, ArrayList<IJavaElement>> entry : projectsToElements.entrySet()) {
				IJavaProject project= entry.getKey();
				ArrayList<IJavaElement> javaElementsList= entry.getValue();
				IJavaElement[] javaElements= javaElementsList.toArray(new IJavaElement[javaElementsList.size()]);
				List<ICompilationUnit> cus= Arrays.asList(JavaModelUtil.getAllCompilationUnits(javaElements));
				int batchSize= 150;
				int batches= ((cus.size()-1) / batchSize) + 1;
				SubProgressMonitor projectMonitor= new SubProgressMonitor(pm, 1);
				projectMonitor.beginTask("", batches); //$NON-NLS-1$
				projectMonitor.setTaskName(RefactoringCoreMessages.InferTypeArgumentsRefactoring_building);
				for (int i= 0; i < batches; i++) {
					List<ICompilationUnit> batch= cus.subList(i * batchSize, Math.min(cus.size(), (i + 1) * batchSize));
					ICompilationUnit[] batchCus= batch.toArray(new ICompilationUnit[batch.size()]);
					final SubProgressMonitor batchMonitor= new SubProgressMonitor(projectMonitor, 1);
					batchMonitor.subTask(RefactoringCoreMessages.InferTypeArgumentsRefactoring_calculating_dependencies);
					ASTParser parser= ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
					parser.setProject(project);
					parser.setCompilerOptions(RefactoringASTParser.getCompilerOptions(project));
					parser.setResolveBindings(true);
					parser.createASTs(batchCus, new String[0], new ASTRequestor() {
						@Override
						public void acceptAST(final ICompilationUnit source, final CompilationUnit ast) {
							batchMonitor.subTask(BasicElementLabels.getFileName(source));
							SafeRunner.run(new ISafeRunnable() {
								@Override
								public void run() throws Exception {
									for (IProblem problem : ast.getProblems()) {
										if (problem.isError()) {
											String cuName= JavaElementLabelsCore.getElementLabel(source, JavaElementLabelsCore.CU_QUALIFIED);
											String msg= Messages.format(RefactoringCoreMessages.InferTypeArgumentsRefactoring_error_in_cu_skipped, new Object[] {cuName});
											result.addError(msg, JavaStatusContext.create(source, SourceRangeFactory.create(problem)));
											return;
										}
									}
									ast.accept(unitCollector);
								}

								@Override
								public void handleException(Throwable exception) {
									String cuName= JavaElementLabelsCore.getElementLabel(source, JavaElementLabelsCore.CU_QUALIFIED);
									String msg= Messages.format(RefactoringCoreMessages.InferTypeArgumentsRefactoring_internal_error, new Object[] {cuName});
									JavaManipulationPlugin.log(new Status(IStatus.ERROR, CorextCore.getPluginId(), IJavaStatusConstants.INTERNAL_ERROR, msg, null));
									String msg2= Messages.format(RefactoringCoreMessages.InferTypeArgumentsRefactoring_error_skipped, new Object[] {cuName});
									result.addError(msg2, JavaStatusContext.create(source));
								}
							});
							fTCModel.newCu();
						}

						@Override
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
			result.merge(Checks.validateModifiesFiles(filesToModify, getValidationContext(), pm));
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

	private HashMap<IJavaProject, ArrayList<IJavaElement>> getJavaElementsPerProject(IJavaElement[] elements) {
		HashMap<IJavaProject, ArrayList<IJavaElement>> result= new HashMap<>();
		for (IJavaElement element : elements) {
			IJavaProject javaProject= element.getJavaProject();
			ArrayList<IJavaElement> javaElements= result.get(javaProject);
			if (javaElements == null) {
				javaElements= new ArrayList<>();
				result.put(javaProject, javaElements);
			}
			javaElements.add(element);
		}
		return result;
	}

	private RefactoringStatus check15() throws CoreException {
		RefactoringStatus result= new RefactoringStatus();
		HashSet<IJavaProject> checkedProjects= new HashSet<>();

		for (IJavaElement element : fElements) {
			IJavaProject javaProject= element.getJavaProject();
			if (! checkedProjects.contains(javaProject)) {
				if (! JavaModelUtil.is50OrHigher(javaProject)) {
					String message= Messages.format(RefactoringCoreMessages.InferTypeArgumentsRefactoring_not50, BasicElementLabels.getJavaElementName(javaProject.getElementName()));
					result.addFatalError(message);
				} else if (! JavaModelUtil.is50OrHigherJRE(javaProject)) {
					String message= Messages.format(RefactoringCoreMessages.InferTypeArgumentsRefactoring_not50Library, BasicElementLabels.getJavaElementName(javaProject.getElementName()));
					result.addFatalError(message);
				}
				checkedProjects.add(javaProject);
			}
		}
		return result;
	}

	private void rewriteDeclarations(InferTypeArgumentsUpdate update, IProgressMonitor pm) throws CoreException {
		HashMap<ICompilationUnit, CuUpdate> updates= update.getUpdates();

		Set<Entry<ICompilationUnit, CuUpdate>> entrySet= updates.entrySet();
		pm.beginTask("", entrySet.size()); //$NON-NLS-1$
		pm.setTaskName(RefactoringCoreMessages.InferTypeArgumentsRefactoring_creatingChanges);
		for (Entry<ICompilationUnit, CuUpdate> entry : entrySet) {
			if (pm.isCanceled())
				throw new OperationCanceledException();

			ICompilationUnit cu= entry.getKey();
			pm.worked(1);
			pm.subTask(BasicElementLabels.getFileName(cu));

			CompilationUnitRewrite rewrite= new CompilationUnitRewrite(cu);
			rewrite.setResolveBindings(false);
			CuUpdate cuUpdate= entry.getValue();

			for (ConstraintVariable2 cv : cuUpdate.getDeclarations()) {
				rewriteConstraintVariable(cv, rewrite, fTCModel, fLeaveUnconstrainedRaw, null);
			}
			for (CastVariable2 castCv : cuUpdate.getCastsToRemove()) {
				rewriteCastVariable(castCv, rewrite, fTCModel);
			}

			CompilationUnitChange change= rewrite.createChange(true);
			if (change != null) {
				fChangeManager.manage(cu, change);
			}
		}

	}

	public static ParameterizedType[] inferArguments(SimpleType[] types, InferTypeArgumentsUpdate update, InferTypeArgumentsTCModel model, CompilationUnitRewrite rewrite) {
		for (SimpleType type : types) {
			type.setProperty(REWRITTEN, null);
		}
		List<ParameterizedType> result= new ArrayList<>();
		HashMap<ICompilationUnit, CuUpdate> updates= update.getUpdates();
		for (Entry<ICompilationUnit, CuUpdate> entry : updates.entrySet()) {
			rewrite.setResolveBindings(false);
			for (ConstraintVariable2 cv : entry.getValue().getDeclarations()) {
				ParameterizedType newNode= rewriteConstraintVariable(cv, rewrite, model, false, types);
				if (newNode != null)
					result.add(newNode);
			}
		}
		return result.toArray(new ParameterizedType[result.size()]);
	}

	private static ParameterizedType rewriteConstraintVariable(ConstraintVariable2 cv, CompilationUnitRewrite rewrite, InferTypeArgumentsTCModel tCModel, boolean leaveUnconstraindRaw, SimpleType[] types) {
		if (cv instanceof CollectionElementVariable2) {
			ConstraintVariable2 parentElement= ((CollectionElementVariable2) cv).getParentConstraintVariable();
			if (parentElement instanceof TypeVariable2) {
				TypeVariable2 typeCv= (TypeVariable2) parentElement;
				return rewriteTypeVariable(typeCv, rewrite, tCModel, leaveUnconstraindRaw, types);
			} else {
				//only rewrite type variables
			}
		}
		return null;
	}

	private static ParameterizedType rewriteTypeVariable(TypeVariable2 typeCv, CompilationUnitRewrite rewrite, InferTypeArgumentsTCModel tCModel, boolean leaveUnconstraindRaw, SimpleType[] types) {
		ASTNode node= typeCv.getRange().getNode(rewrite.getRoot());
		if (node instanceof Name && node.getParent() instanceof Type) {
			Type originalType= (Type) node.getParent();

			if (types != null && !has(types, originalType))
				return null;

			// Must rewrite all type arguments in one batch. Do the rewrite when the first one is encountered; skip the others.
			Object rewritten= originalType.getProperty(REWRITTEN);
			if (rewritten == REWRITTEN)
				return null;
			originalType.setProperty(REWRITTEN, REWRITTEN);

			ArrayList<CollectionElementVariable2> typeArgumentCvs= getTypeArgumentCvs(typeCv, tCModel);
			Type[] typeArguments= getTypeArguments(originalType, typeArgumentCvs, rewrite, tCModel, leaveUnconstraindRaw);
			if (typeArguments == null)
				return null;

			Type movingType= (Type) rewrite.getASTRewrite().createMoveTarget(originalType);
			ParameterizedType newType= rewrite.getAST().newParameterizedType(movingType);
			List<Type> newTypeArguments= newType.typeArguments();
			Collections.addAll(newTypeArguments, typeArguments);

			rewrite.getASTRewrite().replace(originalType, newType, rewrite.createGroupDescription(RefactoringCoreMessages.InferTypeArgumentsRefactoring_addTypeArguments));
			return newType;
		}  else {//TODO: other node types?
			return null;
		}
	}

	private static boolean has(SimpleType[] types, Type originalType) {
		for (SimpleType type : types) {
			if (type == originalType) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @param baseType the base type
	 * @param typeArgumentCvs type argument constraint variables
	 * @param rewrite the cu rewrite
	 * @param tCModel the type constraints model
	 * @param leaveUnconstraindRaw <code>true</code> to keep unconstrained type references raw,
	 *            <code>false</code> to infer <code>&lt;?&gt;</code> if possible
	 * @return the new type arguments, or <code>null</code> iff an argument could not be inferred
	 */
	private static Type[] getTypeArguments(Type baseType, ArrayList<CollectionElementVariable2> typeArgumentCvs, CompilationUnitRewrite rewrite, InferTypeArgumentsTCModel tCModel, boolean leaveUnconstraindRaw) {
		if (typeArgumentCvs.isEmpty())
			return null;

		Type[] typeArguments= new Type[typeArgumentCvs.size()];
		for (int i= 0; i < typeArgumentCvs.size(); i++) {
			CollectionElementVariable2 elementCv= typeArgumentCvs.get(i);
			Type typeArgument;
			TType chosenType= InferTypeArgumentsConstraintsSolver.getChosenType(elementCv);
			if (chosenType != null) {
				if (chosenType.isWildcardType() && ! unboundedWildcardAllowed(baseType))
					return null; // can't e.g. write "new ArrayList<?>()".
				if (chosenType.isParameterizedType()) // workaround for bug 99124
					chosenType= chosenType.getTypeDeclaration();
				BindingKey bindingKey= new BindingKey(chosenType.getBindingKey());
				if (chosenType.isLocal()) { // fix for issue #224
					String typeName= chosenType.getName();
					AST ast= rewrite.getAST();
					// need type but do not obtain it via an extraneous import
					typeArgument= ast.newSimpleType(ast.newSimpleName(typeName));
				} else {
					typeArgument= rewrite.getImportRewrite().addImportFromSignature(bindingKey.toSignature(), rewrite.getAST());
				}
				ArrayList<CollectionElementVariable2> nestedTypeArgumentCvs= getTypeArgumentCvs(elementCv, tCModel);
				Type[] nestedTypeArguments= getTypeArguments(typeArgument, nestedTypeArgumentCvs, rewrite, tCModel, leaveUnconstraindRaw); //recursion
				if (nestedTypeArguments != null) {
					ParameterizedType parameterizedType= rewrite.getAST().newParameterizedType(typeArgument);
					List<Type> newtypeArguments= parameterizedType.typeArguments();
					Collections.addAll(newtypeArguments, nestedTypeArguments);
					typeArgument= parameterizedType;
				}

			} else { // couldn't infer an element type (no constraints)
				if (leaveUnconstraindRaw) {
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

	private static ArrayList<CollectionElementVariable2> getTypeArgumentCvs(ConstraintVariable2 baseCv, InferTypeArgumentsTCModel tCModel) {
		Map<String, CollectionElementVariable2> elementCvs= tCModel.getElementVariables(baseCv);
		ArrayList<CollectionElementVariable2> typeArgumentCvs= new ArrayList<>();
		for (CollectionElementVariable2 elementCv : elementCvs.values()) {
			int index= elementCv.getDeclarationTypeVariableIndex();
			if (index != CollectionElementVariable2.NOT_DECLARED_TYPE_VARIABLE_INDEX) {
				while (index >= typeArgumentCvs.size())
					typeArgumentCvs.add(null); // fill with null until set(index, ..) is possible
				typeArgumentCvs.set(index, elementCv);
			}
		}
		return typeArgumentCvs;
	}

	private static boolean unboundedWildcardAllowed(Type originalType) {
		ASTNode parent= originalType.getParent();
		while (parent instanceof Type)
			parent= parent.getParent();

		if (parent instanceof ClassInstanceCreation
				|| parent instanceof AbstractTypeDeclaration
				|| (parent instanceof TypeLiteral)) {
			return false;
		}
		return true;
	}

	private static ASTNode rewriteCastVariable(CastVariable2 castCv, CompilationUnitRewrite rewrite, InferTypeArgumentsTCModel tCModel) {//, List positionGroups) {
		ASTNode node= castCv.getRange().getNode(rewrite.getRoot());

		ConstraintVariable2 expressionVariable= castCv.getExpressionVariable();
		ConstraintVariable2 methodReceiverCv= tCModel.getMethodReceiverCv(expressionVariable);
		if (methodReceiverCv != null) {
			TType chosenReceiverType= InferTypeArgumentsConstraintsSolver.getChosenType(methodReceiverCv);
			if (chosenReceiverType == null
					|| !InferTypeArgumentsTCModel.isAGenericType(chosenReceiverType)
					|| hasUnboundElement(methodReceiverCv, tCModel)) {
				return null;
			}
		}

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
		return newExpression;
	}

	private static boolean hasUnboundElement(ConstraintVariable2 methodReceiverCv, InferTypeArgumentsTCModel tCModel) {
		for (CollectionElementVariable2 elementCv : getTypeArgumentCvs(methodReceiverCv, tCModel)) {
			TType chosenElementType= InferTypeArgumentsConstraintsSolver.getChosenType(elementCv);
			if (chosenElementType == null)
				return true;
		}
		return false;
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#createChange(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		pm.beginTask("", 1); //$NON-NLS-1$
		try {
			DynamicValidationStateChange result= new DynamicValidationStateChange(RefactoringCoreMessages.InferTypeArgumentsRefactoring_name, fChangeManager.getAllChanges()) {

				@Override
				public final ChangeDescriptor getDescriptor() {
					final Map<String, String> arguments= new HashMap<>();
					final IJavaProject project= getSingleProject();
					final String description= RefactoringCoreMessages.InferTypeArgumentsRefactoring_descriptor_description;
					final String header= project != null ? Messages.format(RefactoringCoreMessages.InferTypeArgumentsRefactoring_descriptor_description_project, BasicElementLabels.getJavaElementName(project.getElementName())) : RefactoringCoreMessages.InferTypeArgumentsRefactoring_descriptor_description;
					final String name= project != null ? project.getElementName() : null;
					final JDTRefactoringDescriptorComment comment= new JDTRefactoringDescriptorComment(name, this, header);
					final String[] settings= new String[fElements.length];
					for (int index= 0; index < settings.length; index++)
						settings[index]= JavaElementLabelsCore.getTextLabel(fElements[index], JavaElementLabelsCore.ALL_FULLY_QUALIFIED);
					comment.addSetting(JDTRefactoringDescriptorComment.createCompositeSetting(RefactoringCoreMessages.InferTypeArgumentsRefactoring_original_elements, settings));
					if (fAssumeCloneReturnsSameType)
						comment.addSetting(RefactoringCoreMessages.InferTypeArgumentsRefactoring_assume_clone);
					if (fLeaveUnconstrainedRaw)
						comment.addSetting(RefactoringCoreMessages.InferTypeArgumentsRefactoring_leave_unconstrained);
					final InferTypeArgumentsDescriptor descriptor= RefactoringSignatureDescriptorFactory.createInferTypeArgumentsDescriptor(name, description, comment.asString(), arguments, RefactoringDescriptor.STRUCTURAL_CHANGE | RefactoringDescriptor.MULTI_CHANGE);
					for (int index= 0; index < fElements.length; index++)
						arguments.put(JavaRefactoringDescriptorUtil.ATTRIBUTE_ELEMENT + (index + 1), JavaRefactoringDescriptorUtil.elementToHandle(name, fElements[index]));
					arguments.put(ATTRIBUTE_CLONE, Boolean.toString(fAssumeCloneReturnsSameType));
					arguments.put(ATTRIBUTE_LEAVE, Boolean.toString(fLeaveUnconstrainedRaw));
					return new RefactoringChangeDescriptor(descriptor);
				}
			};
			return result;
		} finally {
			pm.done();
		}
	}

	private IJavaProject getSingleProject() {
		IJavaProject first= null;
		for (IJavaElement element : fElements) {
			final IJavaProject project= element.getJavaProject();
			if (project != null) {
				if (first == null)
					first= project;
				else if (!project.equals(first))
					return null;
			}
		}
		return first;
	}

	private RefactoringStatus initialize(JavaRefactoringArguments arguments) {
		final String clone= arguments.getAttribute(ATTRIBUTE_CLONE);
		if (clone != null) {
			fAssumeCloneReturnsSameType= Boolean.parseBoolean(clone);
		} else
			return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_CLONE));
		final String leave= arguments.getAttribute(ATTRIBUTE_LEAVE);
		if (leave != null) {
			fLeaveUnconstrainedRaw= Boolean.parseBoolean(leave);
		} else
			return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_LEAVE));
		int count= 1;
		final List<IJavaElement> elements= new ArrayList<>();
		String handle= null;
		String attribute= JavaRefactoringDescriptorUtil.ATTRIBUTE_ELEMENT + count;
		final RefactoringStatus status= new RefactoringStatus();
		while ((handle= arguments.getAttribute(attribute)) != null) {
			final IJavaElement element= JavaRefactoringDescriptorUtil.handleToElement(arguments.getProject(), handle, false);
			if (element == null || !element.exists())
				return JavaRefactoringDescriptorUtil.createInputFatalStatus(element, getName(), IJavaRefactorings.INFER_TYPE_ARGUMENTS);
			else
				elements.add(element);
			count++;
			attribute= JavaRefactoringDescriptorUtil.ATTRIBUTE_ELEMENT + count;
		}
		fElements= elements.toArray(new IJavaElement[elements.size()]);
		if (elements.isEmpty())
			return JavaRefactoringDescriptorUtil.createInputFatalStatus(null, getName(), IJavaRefactorings.INFER_TYPE_ARGUMENTS);
		if (!status.isOK())
			return status;
		return new RefactoringStatus();
	}
}
