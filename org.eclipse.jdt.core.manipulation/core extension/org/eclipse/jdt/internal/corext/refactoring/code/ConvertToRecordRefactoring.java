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
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.code;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.RecordDeclaration;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.TargetSourceRangeComputer;
import org.eclipse.jdt.core.refactoring.descriptors.ConvertToRecordDescriptor;
import org.eclipse.jdt.core.refactoring.descriptors.JavaRefactoringDescriptor;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchPattern;

import org.eclipse.jdt.internal.core.manipulation.BindingLabelProviderCore;
import org.eclipse.jdt.internal.core.manipulation.JavaElementLabelsCore;
import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.core.refactoring.descriptors.RefactoringSignatureDescriptorFactory;
import org.eclipse.jdt.internal.corext.codemanipulation.GetterSetterUtil;
import org.eclipse.jdt.internal.corext.dom.ASTFlattener;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.dom.SelectionAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.JDTRefactoringDescriptorComment;
import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringDescriptorUtil;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringScopeFactory;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationRefactoringChange;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.util.Progress;

public class ConvertToRecordRefactoring extends Refactoring {

	private int fSelectionStart;
	private int fSelectionLength;
	private ICompilationUnit fCu;
	private CompilationUnit fASTRoot;
	private MethodDeclaration fConstructor;
	private AbstractTypeDeclaration fTypeDeclaration;
	private ITypeBinding fTypeBinding;
	Map<IVariableBinding, IMethodBinding> fGetterMap= new HashMap<>();
	TextChangeManager fChangeManager;
	CompilationUnitRewrite fBaseCURewrite;


	public ConvertToRecordRefactoring(ICompilationUnit unit, CompilationUnit node, int selectionStart, int selectionLength) {
		Assert.isTrue(selectionStart >= 0);
		Assert.isTrue(selectionLength >= 0);
		fSelectionStart= selectionStart;
		fSelectionLength= selectionLength;
		fCu= unit;
		fASTRoot= node;
	}

	/**
	 * Creates a new inline constant refactoring.
	 * @param unit the compilation unit, or <code>null</code> if invoked by scripting
	 * @param selectionStart start
	 * @param selectionLength length
	 */
	public ConvertToRecordRefactoring(ICompilationUnit unit, int selectionStart, int selectionLength) {
		this(unit, null, selectionStart, selectionLength);
	}

	@Override
	public String getName() {
		return RefactoringCoreMessages.ConvertToRecordRefactoring_name;
	}

	public String getClassName() {
		IJavaElement element= null;
		try {
			element= fCu.getElementAt(fSelectionStart);
		} catch (JavaModelException e) {
			// ignore
		}
		if (element instanceof IType type)
			return type.getElementName();
		if (element instanceof IMember member) {
			return member.getDeclaringType().getElementName();
		}
		return "class"; //$NON-NLS-1$
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		try {
			pm.beginTask("", 1); //$NON-NLS-1$

			RefactoringStatus result= Checks.validateModifiesFiles(ResourceUtil.getFiles(new ICompilationUnit[]{fCu}), getValidationContext(), pm);
			if (result.hasFatalError())
				return result;

			if (fASTRoot == null) {
				fASTRoot= Checks.convertICUtoCU(fCu);
			}
			if (fASTRoot == null) {
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ConvertToRecordRefactoring_unexpected_error);
			}
			ASTNode selected= getSelectedNode(fASTRoot, fSelectionStart, fSelectionLength);

			ASTNode selectedType= ASTNodes.getFirstAncestorOrNull(selected,
					AbstractTypeDeclaration.class, RecordDeclaration.class, AnonymousClassDeclaration.class);

			if (!(selectedType instanceof TypeDeclaration)) {
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ConvertToRecordRefactoring_no_type);
			}
			fTypeDeclaration= (TypeDeclaration)selectedType;
			fTypeBinding= fTypeDeclaration.resolveBinding();
			if (fTypeBinding == null) {
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ConvertToRecordRefactoring_unexpected_error);
			}

			if (fTypeBinding.getSuperclass() != null
					&& !fTypeBinding.getSuperclass().isEqualTo(fASTRoot.getAST().resolveWellKnownType("java.lang.Object"))) { //$NON-NLS-1$
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ConvertToRecordRefactoring_cannot_extend);
			}

			if (fTypeBinding.getInterfaces().length != 0) {
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ConvertToRecordRefactoring_not_simple_case);
			}

			int typeModifiers= fTypeBinding.getModifiers();
			if (Modifier.isSealed(typeModifiers)) {
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ConvertToRecordRefactoring_not_simple_case);
			}

			IVariableBinding[] fields= fTypeBinding.getDeclaredFields();
			for (IVariableBinding field : fields) {
				int modifiers= field.getModifiers();
				if (!Modifier.isPrivate(modifiers)) {
					return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ConvertToRecordRefactoring_not_private);
				}
				if (Modifier.isStatic(modifiers)) {
					return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ConvertToRecordRefactoring_has_static_members);
				}
			}

			IMethodBinding[] methodBindings= fTypeBinding.getDeclaredMethods();
			boolean hasConstructor= false;
			for (IMethodBinding method : methodBindings) {
				if (method.isConstructor()) {
					if (hasConstructor) {
						return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ConvertToRecordRefactoring_has_constructors);
					}
					hasConstructor= true;
				}
				if (Modifier.isStatic(method.getModifiers())) {
					return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ConvertToRecordRefactoring_has_static_members);
				}
			}
			if (!hasConstructor) {
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ConvertToRecordRefactoring_not_simple_case);
			}

			if (fTypeBinding.getJavaElement() instanceof IType sourceType) {
				IMethod[] methods= sourceType.getMethods();
				for (IVariableBinding field : fields) {
					IMethodBinding getter= findGetter(fTypeBinding, field);
					if (getter != null) {
						fGetterMap.put(field, getter);
					}
					IMethodBinding setter= findSetter(fTypeBinding, field);
					if (setter != null) {
						return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.ConvertToRecordRefactoring_setter_found, field.getName()));
					}
				}

				if (methods.length > fields.length + 1) {
					return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ConvertToRecordRefactoring_not_simple_case);
				}
				class VisitException extends RuntimeException {
					private static final long serialVersionUID= 1L;

					public VisitException(String message) {
						super(message);
					}
				}
				try {
					fTypeDeclaration.accept(new ASTVisitor() {
						@Override
						public boolean visit(FieldDeclaration node) {
							List<VariableDeclarationFragment> fragments= node.fragments();
							for (VariableDeclarationFragment fragment : fragments) {
								if (fragment.getInitializer() != null) {
									throw new VisitException(RefactoringCoreMessages.ConvertToRecordRefactoring_fields_initialized);
								}
							}
							return false;
						}
						@Override
						public boolean visit(MethodDeclaration node) {
							List<Statement> statements= ASTNodes.asList(node.getBody());
							if (node.isConstructor()) {
								if (fConstructor != null) {
									throw new VisitException(RefactoringCoreMessages.ConvertToRecordRefactoring_multiple_constructors);
								}
								fConstructor= node;
								for (Statement statement : statements) {
									if (statement instanceof ExpressionStatement expStmt) {
										if (expStmt.getExpression() instanceof Assignment assignment) {
											Expression leftHandSide= assignment.getLeftHandSide();
											Expression rightHandSide= assignment.getRightHandSide();
											if (rightHandSide instanceof SimpleName simpleName) {
												IBinding binding= simpleName.resolveBinding();
												if (binding instanceof IVariableBinding simpleNameBinding && simpleNameBinding.isParameter()) {
													if (getFieldBinding(leftHandSide) != null) {
														continue;
													}
												}
											}
										}
									}
									throw new VisitException(RefactoringCoreMessages.ConvertToRecordRefactoring_nonstandard_constructor);
								}
							} else {
								if (statements.size() != 1 || !(statements.get(0) instanceof ReturnStatement retStmt)) {
									throw new VisitException(RefactoringCoreMessages.ConvertToRecordRefactoring_not_implicit_getter);
								} else {
									Expression exp= retStmt.getExpression();
									IVariableBinding fieldBinding= getFieldBinding(exp);
									if (fieldBinding == null) {
										throw new VisitException(RefactoringCoreMessages.ConvertToRecordRefactoring_not_implicit_getter);
									}
									IMethodBinding methodBinding= node.resolveBinding();
									if (methodBinding == null) {
										throw new VisitException(RefactoringCoreMessages.ConvertToRecordRefactoring_unexpected_error);
									}
									if (!fGetterMap.containsKey(fieldBinding)) {
										fGetterMap.put(fieldBinding, methodBinding);
									} else if (!methodBinding.isEqualTo(fGetterMap.get(fieldBinding))) {
										throw new VisitException(RefactoringCoreMessages.ConvertToRecordRefactoring_not_simple_case);
									}
								}
							}
							return false;
						}

						private IVariableBinding getFieldBinding(Expression exp) {
							IVariableBinding varBinding= null;
							if (exp instanceof FieldAccess fieldAccess) {
								varBinding= fieldAccess.resolveFieldBinding();
							} else if (exp instanceof SimpleName simpleName) {
								IBinding simpleNameBinding= simpleName.resolveBinding();
								if (simpleNameBinding instanceof IVariableBinding) {
									varBinding= (IVariableBinding)simpleNameBinding;
								}
							}
							if (varBinding != null && varBinding.isField()) {
								return varBinding;
							}
							return null;
						}
					});
				} catch (VisitException e) {
					return RefactoringStatus.createFatalErrorStatus(e.getMessage());
				}
			}

			if (fGetterMap.size() < fields.length) {
				result.merge(RefactoringStatus.createWarningStatus(RefactoringCoreMessages.ConvertToRecordRefactoring_not_enough_getters));
			}

			return result;
		} finally {
			pm.done();
		}

	}

	private boolean isBoolean(IVariableBinding field) {
		AST ast= fASTRoot.getAST();
		boolean isBoolean= ast.resolveWellKnownType("boolean") == field.getType(); //$NON-NLS-1$
		if (!isBoolean)
			isBoolean= ast.resolveWellKnownType("java.lang.Boolean") == field.getType(); //$NON-NLS-1$
		return isBoolean;
	}

	private IMethodBinding findGetter(ITypeBinding declaringType, IVariableBinding variableBinding) {
		ITypeBinding returnType= variableBinding.getType();
		String getterName= GetterSetterUtil.getGetterName(variableBinding, fCu.getJavaProject(), null, isBoolean(variableBinding));
		if (declaringType == null)
			return null;
		IMethodBinding getter= Bindings.findMethodInHierarchy(declaringType, getterName, new ITypeBinding[0]);
		if (getter != null && getter.getReturnType().isAssignmentCompatible(returnType) && Modifier.isStatic(getter.getModifiers()) == Modifier.isStatic(variableBinding.getModifiers()))
			return getter;
		return null;
	}

	private IMethodBinding findSetter(ITypeBinding declaringType, IVariableBinding variableBinding) {
		ITypeBinding fieldType= variableBinding.getType();
		String setterName= GetterSetterUtil.getSetterName(variableBinding, fCu.getJavaProject(), null, isBoolean(variableBinding));
		if (declaringType == null)
			return null;
		IMethodBinding setter= Bindings.findMethodInHierarchy(declaringType, setterName, new ITypeBinding[] { fieldType });
		return setter;
	}

	public static ASTNode getSelectedNode(CompilationUnit cu, int selectionOffset, int selectionLength) {
		SelectionAnalyzer analyzer= new SelectionAnalyzer(Selection.createFromStartLength(selectionOffset, selectionLength), false);
		cu.accept(analyzer);

		return analyzer.getFirstSelectedNode();
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		RefactoringStatus status= new RefactoringStatus();
		fBaseCURewrite= new CompilationUnitRewrite(fCu, fASTRoot);
		fChangeManager= new TextChangeManager();
		return status;
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		pm.beginTask(RefactoringCoreMessages.ExtractClassRefactoring_progress_create_change, 10);
		try {
			ICompilationUnit typeCU= fCu;
			ArrayList<Change> changes= new ArrayList<>();
			createNewRecord();
			updateReferences(pm);
			fChangeManager.manage(typeCU, fBaseCURewrite.createChange(true, pm));
			changes.addAll(Arrays.asList(fChangeManager.getAllChanges()));
			final Map<String, String> arguments= new HashMap<>();
			String project= null;
			IJavaProject javaProject= fCu.getJavaProject();
			if (javaProject != null)
				project= javaProject.getElementName();
			int flags= RefactoringDescriptor.STRUCTURAL_CHANGE | JavaRefactoringDescriptor.JAR_REFACTORING | RefactoringDescriptor.MULTI_CHANGE;
			final String description= Messages.format(RefactoringCoreMessages.ConvertToRecordRefactoring_descriptor_description_short, BasicElementLabels.getJavaElementName(fTypeDeclaration.getName().getFullyQualifiedName()));
			final String header= Messages.format(RefactoringCoreMessages.ConvertToRecordRefactoring_descriptor_description, new String[] { BindingLabelProviderCore.getBindingLabel(fTypeBinding, JavaElementLabelsCore.ALL_FULLY_QUALIFIED)});
			final JDTRefactoringDescriptorComment comment= new JDTRefactoringDescriptorComment(project, this, header);
			comment.addSetting(Messages.format(RefactoringCoreMessages.InlineMethodRefactoring_original_pattern, BindingLabelProviderCore.getBindingLabel(fTypeBinding, JavaElementLabelsCore.ALL_FULLY_QUALIFIED)));
			final ConvertToRecordDescriptor descriptor= RefactoringSignatureDescriptorFactory.createConvertToRecordDescriptor(project, description, comment.asString(), arguments, flags);
			arguments.put(JavaRefactoringDescriptorUtil.ATTRIBUTE_INPUT, JavaRefactoringDescriptorUtil.elementToHandle(project, fCu));
			arguments.put(JavaRefactoringDescriptorUtil.ATTRIBUTE_SELECTION, Integer.toString(fSelectionStart) + " " + Integer.toString(fSelectionLength)); //$NON-NLS-1$
			DynamicValidationRefactoringChange change= new DynamicValidationRefactoringChange(descriptor, RefactoringCoreMessages.ConvertToRecordRefactoring_name, changes
					.toArray(new Change[changes.size()]));
			return change;
		} finally {
			pm.done();
		}
	}

	private void createNewRecord() {
		AST ast= fBaseCURewrite.getAST();
		ASTRewrite rewrite= fBaseCURewrite.getASTRewrite();
		rewrite.setTargetSourceRangeComputer(new TargetSourceRangeComputer() {
			@Override
			public SourceRange computeSourceRange(final ASTNode nodeWithComment) {
				if (Boolean.TRUE.equals(nodeWithComment.getProperty(ASTNodes.UNTOUCH_COMMENT))) {
					return new SourceRange(nodeWithComment.getStartPosition(), nodeWithComment.getLength());
				}

				return super.computeSourceRange(nodeWithComment);
			}
		});

		RecordDeclaration newRecordDeclaration= null;
		if (fTypeDeclaration.getJavadoc() instanceof Javadoc javadoc) {
			StringBuilder builder= new StringBuilder();
			builder.append(ASTFlattener.asString(javadoc));
			builder.append("\n"); //$NON-NLS-1$
			List<IExtendedModifier> modifiers= fTypeDeclaration.modifiers();
			for (IExtendedModifier modifier : modifiers) {
				builder.append(modifier.toString());
				builder.append(" "); //$NON-NLS-1$
			}
			builder.append("record " + fTypeDeclaration.getName().getFullyQualifiedName() + " ("); //$NON-NLS-1$ //$NON-NLS-2$
			List<SingleVariableDeclaration> parameters= fConstructor.parameters();
			String separator= ""; //$NON-NLS-1$
			for (SingleVariableDeclaration parameter : parameters) {
				builder.append(separator);
				builder.append(ASTFlattener.asString(parameter));
				separator= ", "; //$NON-NLS-1$
			}
			builder.append(") {}"); //$NON-NLS-1$
			newRecordDeclaration= (RecordDeclaration) rewrite.createStringPlaceholder(builder.toString(), ASTNode.RECORD_DECLARATION);
		} else {
			newRecordDeclaration= ast.newRecordDeclaration();
			newRecordDeclaration.setName((SimpleName) rewrite.createCopyTarget(fTypeDeclaration.getName()));
			List<IExtendedModifier> modifiers= fTypeDeclaration.modifiers();
			List<IExtendedModifier> recordModifiers= newRecordDeclaration.modifiers();
			for (IExtendedModifier modifier : modifiers) {
				IExtendedModifier newModifier= (IExtendedModifier) rewrite.createCopyTarget((ASTNode)modifier);
				recordModifiers.add(newModifier);
			}
			List<SingleVariableDeclaration> components= newRecordDeclaration.recordComponents();
			List<SingleVariableDeclaration> parameters= fConstructor.parameters();
			for (SingleVariableDeclaration parameter : parameters) {
				SingleVariableDeclaration newSingleVariableDeclaration= (SingleVariableDeclaration) rewrite.createCopyTarget(parameter);
				components.add(newSingleVariableDeclaration);
			}
		}
		ASTNodes.replaceButKeepComment(rewrite, fTypeDeclaration, newRecordDeclaration, null);
	}

	private RefactoringStatus updateReferences(IProgressMonitor pm) throws CoreException {
		RefactoringStatus status= new RefactoringStatus();
		pm.beginTask(RefactoringCoreMessages.ConvertToRecordRefactoring_progress_updating_references, 100);
		try {
			pm.worked(10);
			if (pm.isCanceled())
				throw new OperationCanceledException();
			List<IMethod> methods= new ArrayList<>();
			for (Entry<IVariableBinding, IMethodBinding> entry : fGetterMap.entrySet()) {
				IMethodBinding methodBinding= entry.getValue();
				IMethod method= (IMethod) methodBinding.getJavaElement();
				if (method != null) {
					methods.add(method);
				}
			}
			if (methods.isEmpty()) {
				return status;
			}
			SearchPattern pattern= RefactoringSearchEngine.createOrPattern(methods.toArray(new IMethod[methods.size()]), IJavaSearchConstants.METHOD | IJavaSearchConstants.REFERENCES);
			SearchResultGroup[] results= RefactoringSearchEngine.search(pattern, RefactoringScopeFactory.create(fTypeBinding.getJavaElement()), pm, status);
			IProgressMonitor spm= Progress.subMonitor(pm, 90);
			spm.beginTask(RefactoringCoreMessages.ConvertToRecordRefactoring_progress_updating_references, results.length * 10);
			try {
				for (SearchResultGroup group : results) {
					ICompilationUnit unit= group.getCompilationUnit();

					CompilationUnitRewrite cuRewrite;
					if (unit.equals(fBaseCURewrite.getCu()))
						cuRewrite= fBaseCURewrite;
					else
						cuRewrite= new CompilationUnitRewrite(unit);
					spm.worked(1);

					status.merge(replaceReferences(group, cuRewrite));
					if (cuRewrite != fBaseCURewrite) //Change for fBaseCURewrite will be generated later
						fChangeManager.manage(unit, cuRewrite.createChange(true, Progress.subMonitor(spm, 9)));
					if (spm.isCanceled())
						throw new OperationCanceledException();
				}
			} finally {
				spm.done();
			}
		} finally {
			pm.done();
		}
		return status;
	}

	private RefactoringStatus replaceReferences(SearchResultGroup group, CompilationUnitRewrite cuRewrite) {
		TextEditGroup editGroup= cuRewrite.createGroupDescription(RefactoringCoreMessages.ConvertToRecordRefactoring_group_replace_getters);
		AST ast= cuRewrite.getAST();
		ASTRewrite rewrite= cuRewrite.getASTRewrite();

		RefactoringStatus status= new RefactoringStatus();

		for (SearchMatch searchMatch : group.getSearchResults()) {
			ASTNode node= NodeFinder.perform(cuRewrite.getRoot(), searchMatch.getOffset(), searchMatch.getLength());
			if (node instanceof MethodInvocation methodInvocation) {
				SimpleName methodName= methodInvocation.getName();
				String newName= methodName.getFullyQualifiedName();
				for (Entry<IVariableBinding, IMethodBinding> entry : fGetterMap.entrySet()) {
					if (entry.getValue().getName().equals(methodName.getFullyQualifiedName())) {
						newName= entry.getKey().getName();
						break;
					}
				}
				SimpleName newMethodName= ast.newSimpleName(newName);
				rewrite.replace(methodName, newMethodName, editGroup);
			}
		}
		return status;
	}

}