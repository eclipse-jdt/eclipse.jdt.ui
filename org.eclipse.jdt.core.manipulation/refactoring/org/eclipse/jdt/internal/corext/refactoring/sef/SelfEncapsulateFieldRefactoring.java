/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
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
 *     jens.lukowski@gmx.de - contributed code to convert prefix and postfix
 *       expressions into a combination of setter and getter calls.
 *     Dmitry Stalnov (dstalnov@fusionone.com) - contributed fix for
 *       bug Encapsulate field can fail when two variables in one variable declaration (see
 *       https://bugs.eclipse.org/bugs/show_bug.cgi?id=51540).
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.sef;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.core.resources.IFile;

import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.participants.ResourceChangeChecker;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Dimension;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.Message;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.manipulation.CodeGeneration;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.EncapsulateFieldDescriptor;
import org.eclipse.jdt.core.refactoring.descriptors.JavaRefactoringDescriptor;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchPattern;

import org.eclipse.jdt.internal.core.manipulation.BindingLabelProviderCore;
import org.eclipse.jdt.internal.core.manipulation.JavaElementLabelsCore;
import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.core.refactoring.descriptors.RefactoringSignatureDescriptorFactory;
import org.eclipse.jdt.internal.corext.codemanipulation.GetterSetterUtil;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.AbortSearchException;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.DimensionRewrite;
import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.corext.dom.VariableDeclarationRewrite;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.JDTRefactoringDescriptorComment;
import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringArguments;
import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringDescriptorUtil;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringScopeFactory;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationRefactoringChange;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.util.JavaConventionsUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.util.Progress;
/**
 * Encapsulates a field into getter and setter calls.
 */
public class SelfEncapsulateFieldRefactoring extends Refactoring {

	private static final String ATTRIBUTE_VISIBILITY= "visibility"; //$NON-NLS-1$
	private static final String ATTRIBUTE_GETTER= "getter"; //$NON-NLS-1$
	private static final String ATTRIBUTE_SETTER= "setter"; //$NON-NLS-1$
	private static final String ATTRIBUTE_INSERTION= "insertion"; //$NON-NLS-1$
	private static final String ATTRIBUTE_COMMENTS= "comments"; //$NON-NLS-1$
	private static final String ATTRIBUTE_DECLARING= "declaring"; //$NON-NLS-1$

	private IField fField;
	private TextChangeManager fChangeManager;

	private CompilationUnit fRoot;
	private VariableDeclarationFragment fFieldDeclaration;
	private ASTRewrite fRewriter;
	private ImportRewrite fImportRewrite;

	private int fVisibility= -1;
	private String fGetterName= ""; //$NON-NLS-1$
	private boolean fCreateGetter;
	private String fSetterName= ""; //$NON-NLS-1$
	private boolean fCreateSetter;
	private String fArgName;
	private boolean fSetterMustReturnValue;
	private int fInsertionIndex;	// -1 represents as first method.
	private boolean fEncapsulateDeclaringClass;
	private boolean fGenerateJavadoc;

	private List<IMethodBinding> fUsedReadNames;
	private List<IMethodBinding> fUsedModifyNames;
	private boolean fConsiderVisibility=true;

	private boolean fSelected;
	private SelfEncapsulateFieldCompositeRefactoring fParentRefactoring;

	private static final String NO_NAME= ""; //$NON-NLS-1$

	/**
	 * Creates a new self encapsulate field refactoring.
	 * @param field the field, or <code>null</code> if invoked by scripting
	 * @throws JavaModelException if initialization failed
	 */
	public SelfEncapsulateFieldRefactoring(IField field) throws JavaModelException {
		fEncapsulateDeclaringClass= true;
		fChangeManager= new TextChangeManager();
		fField= field;
		if (field != null)
			initialize(field);
	}

	public SelfEncapsulateFieldRefactoring(IField field, SelfEncapsulateFieldCompositeRefactoring parentRefactoring) throws JavaModelException {
		this(field);
		this.fParentRefactoring = parentRefactoring;
	}

	private void initialize(IField field) throws JavaModelException {
		setGetterName(GetterSetterUtil.getGetterName(field, null));
		setSetterName(GetterSetterUtil.getSetterName(field, null));
		String argBaseName= StubUtility.getBaseName(field);
		fArgName= StubUtility.suggestArgumentName(field.getJavaProject(), argBaseName, new String[0]);
		checkArgName();
	}

	public void reinitialize() {
		try {
			initialize(fField);
		} catch (JavaModelException e) {
			// ignore
		}
	}

	public IField getField() {
		return fField;
	}

	public String getGetterName() {
		return fGetterName;
	}

	public void setGetterName(String name) {
		Assert.isNotNull(name);
		fGetterName= name;
		fCreateGetter= !name.isEmpty();
	}

	public String getSetterName() {
		return fSetterName;
	}

	public void setSetterName(String name) {
		Assert.isNotNull(name);
		fSetterName= name;
		fCreateSetter= !name.isEmpty();
	}

	public void setInsertionIndex(int index) {
		fInsertionIndex= index;
	}

	public int getVisibility() {
		return fVisibility;
	}

	public void setVisibility(int visibility) {
		fVisibility= visibility;
	}

	public void setEncapsulateDeclaringClass(boolean encapsulateDeclaringClass) {
		fEncapsulateDeclaringClass= encapsulateDeclaringClass;
	}

	public boolean getEncapsulateDeclaringClass() {
		return fEncapsulateDeclaringClass;
	}

	public boolean getGenerateJavadoc() {
		return fGenerateJavadoc;
	}

	public void setGenerateJavadoc(boolean value) {
		fGenerateJavadoc= value;
	}

	//----activation checking ----------------------------------------------------------

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		if (fVisibility < 0)
			fVisibility= getFieldVisibility();
		RefactoringStatus result= new RefactoringStatus();
		result.merge(Checks.checkAvailability(fField));
		if (result.hasFatalError())
			return result;
		fRoot= fParentRefactoring != null
				? fParentRefactoring.getRootForUnit(fField.getCompilationUnit())
				: new RefactoringASTParser(IASTSharedValues.SHARED_AST_LEVEL).parse(fField.getCompilationUnit(), true, pm);
		ISourceRange sourceRange= fField.getNameRange();
		ASTNode node= NodeFinder.perform(fRoot, sourceRange.getOffset(), sourceRange.getLength());
		if (node == null) {
			return mappingErrorFound(result, node);
		}
		fFieldDeclaration= ASTNodes.getParent(node, VariableDeclarationFragment.class);
		if (fFieldDeclaration == null) {
			return mappingErrorFound(result, node);
		}
		if (fFieldDeclaration.resolveBinding() == null) {
			if (!processCompilerError(result, node))
				result.addFatalError(RefactoringCoreMessages.SelfEncapsulateField_type_not_resolveable);
			return result;
		}
		computeUsedNames();
		return result;
	}

	private RefactoringStatus mappingErrorFound(RefactoringStatus result, ASTNode node) {
		if (node != null && (node.getFlags() & ASTNode.MALFORMED) != 0 && processCompilerError(result, node))
			return result;
		result.addFatalError(getMappingErrorMessage());
		return result;
	}

	private boolean processCompilerError(RefactoringStatus result, ASTNode node) {
		Message[] messages= ASTNodes.getMessages(node, ASTNodes.INCLUDE_ALL_PARENTS);
		if (messages.length == 0)
			return false;
		result.addFatalError(Messages.format(
			RefactoringCoreMessages.SelfEncapsulateField_compiler_errors_field,
			new String[] { BasicElementLabels.getJavaElementName(fField.getElementName()), messages[0].getMessage()}));
		return true;
	}

	private String getMappingErrorMessage() {
		return Messages.format(
			RefactoringCoreMessages.SelfEncapsulateField_cannot_analyze_selected_field,
			BasicElementLabels.getJavaElementName(fField.getElementName()));
	}

	//---- Input checking ----------------------------------------------------------

	public RefactoringStatus checkMethodNames() {
		return checkMethodNames(isUsingLocalGetter(), isUsingLocalSetter());
	}

	public RefactoringStatus checkMethodNames(boolean usingLocalGetter, boolean usingLocalSetter) {
		RefactoringStatus result= new RefactoringStatus();
		IType declaringType= fField.getDeclaringType();
		if (fCreateGetter) {
			checkName(result, fGetterName, fUsedReadNames, declaringType, usingLocalGetter, fField);
		}
		if (fCreateSetter) {
			checkName(result, fSetterName, fUsedModifyNames, declaringType, usingLocalSetter, fField);
		}
		return result;
	}

	private static void checkName(RefactoringStatus status, String name, List<IMethodBinding> usedNames, IType type, boolean reUseExistingField, IField field) {
		if ("".equals(name)) { //$NON-NLS-1$
			status.addFatalError(RefactoringCoreMessages.Checks_Choose_name);
			return;
		}
		boolean isStatic= false;
		try {
			isStatic= Flags.isStatic(field.getFlags());
		} catch (JavaModelException e) {
			JavaManipulationPlugin.log(e);
		}
		status.merge(Checks.checkMethodName(name, field));
		for (IMethodBinding method : usedNames) {
			String selector= method.getName();
			if (selector.equals(name)) {
				if (!reUseExistingField) {
					status.addFatalError(Messages.format(RefactoringCoreMessages.SelfEncapsulateField_method_exists, new String[] { BindingLabelProviderCore.getBindingLabel(method, JavaElementLabelsCore.ALL_FULLY_QUALIFIED), BasicElementLabels.getJavaElementName(type.getElementName()) }));
				} else {
					boolean methodIsStatic= Modifier.isStatic(method.getModifiers());
					if (methodIsStatic && !isStatic)
						status.addWarning(Messages.format(RefactoringCoreMessages.SelfEncapsulateFieldRefactoring_static_method_but_nonstatic_field, new String[] { BasicElementLabels.getJavaElementName(method.getName()), BasicElementLabels.getJavaElementName(field.getElementName()) }));
					if (!methodIsStatic && isStatic)
						status.addFatalError(Messages.format(RefactoringCoreMessages.SelfEncapsulateFieldRefactoring_nonstatic_method_but_static_field, new String[] { BasicElementLabels.getJavaElementName(method.getName()), BasicElementLabels.getJavaElementName(field.getElementName()) }));
					return;
				}

			}
		}
		if (reUseExistingField)
			status.addFatalError(Messages.format(
				RefactoringCoreMessages.SelfEncapsulateFieldRefactoring_methoddoesnotexist_status_fatalError,
				new String[] { BasicElementLabels.getJavaElementName(name), BasicElementLabels.getJavaElementName(type.getElementName())}));
	}

	public Integer getFieldVisibility() throws JavaModelException {
		return (this.getField().getFlags() & (Flags.AccPublic | Flags.AccProtected | Flags.AccPrivate));
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException {
		pm.beginTask(NO_NAME, 12);
		pm.setTaskName(RefactoringCoreMessages.SelfEncapsulateField_checking_preconditions);

		RefactoringStatus result= new RefactoringStatus();
		fChangeManager.clear();

		boolean usingLocalGetter= isUsingLocalGetter();
		boolean usingLocalSetter= isUsingLocalSetter();
		result.merge(checkMethodNames(usingLocalGetter, usingLocalSetter));
		pm.worked(1);
		if (result.hasFatalError())
			return result;
		pm.setTaskName(RefactoringCoreMessages.SelfEncapsulateField_searching_for_cunits);
		final IProgressMonitor subPm= Progress.subMonitor(pm, 5);
		SearchPattern pattern= SearchPattern.createPattern(fField, IJavaSearchConstants.REFERENCES);
		if (pattern == null) {
			return result;
		}
		ICompilationUnit[] affectedCUs= RefactoringSearchEngine.findAffectedCompilationUnits(
			pattern,
			RefactoringScopeFactory.create(fField, fConsiderVisibility),
			subPm,
			result, true);

		checkAffectedCUs(result, affectedCUs);
		checkInHierarchy(result, usingLocalGetter, usingLocalSetter);
		if (result.hasFatalError())
			return result;

		pm.setTaskName(RefactoringCoreMessages.SelfEncapsulateField_analyzing);
		IProgressMonitor sub= Progress.subMonitor(pm, 5);
		sub.beginTask(NO_NAME, affectedCUs.length);
		IVariableBinding fieldIdentifier= fFieldDeclaration.resolveBinding();
		ITypeBinding declaringClass=
			ASTNodes.getParent(fFieldDeclaration, AbstractTypeDeclaration.class).resolveBinding();
		ICompilationUnit owner= fField.getCompilationUnit();
		List<TextEditGroup> ownerDescriptions= fParentRefactoring != null
				? fParentRefactoring.getDescriptionsForUnit(owner)
				: new ArrayList<>();
		fRewriter = fParentRefactoring != null
				? fParentRefactoring.getRewriterForUnit(owner, fRoot)
				: ASTRewrite.create(fRoot.getAST());
		fImportRewrite= fParentRefactoring != null
				? fParentRefactoring.getImportRewriterForUnit(owner, fRoot)
				: StubUtility.createImportRewrite(fRoot, true);

		for (ICompilationUnit unit : affectedCUs) {
			sub.subTask(BasicElementLabels.getFileName(unit));
			CompilationUnit root= null;
			ASTRewrite rewriter= null;
			ImportRewrite importRewrite;
			List<TextEditGroup> descriptions;
			if (owner.equals(unit)) {
				root= fRoot;
				rewriter= fRewriter;
				importRewrite= fImportRewrite;
				descriptions= ownerDescriptions;
			} else {
				root= fParentRefactoring != null
						? fParentRefactoring.getRootForUnit(unit)
						: new RefactoringASTParser(IASTSharedValues.SHARED_AST_LEVEL).parse(unit, true);
				rewriter= fParentRefactoring != null
						? fParentRefactoring.getRewriterForUnit(unit, root)
						: ASTRewrite.create(root.getAST());
				descriptions= fParentRefactoring != null
						? fParentRefactoring.getDescriptionsForUnit(unit)
						: new ArrayList<>();
				importRewrite= fParentRefactoring != null
						? fParentRefactoring.getImportRewriterForUnit(unit, root)
						: StubUtility.createImportRewrite(root, true);
			}
			checkCompileErrors(result, root, unit);
			AccessAnalyzer analyzer= new AccessAnalyzer(this, unit, fieldIdentifier, declaringClass, rewriter, importRewrite);
			root.accept(analyzer);
			result.merge(analyzer.getStatus());
			if (!fSetterMustReturnValue)
				fSetterMustReturnValue= analyzer.getSetterMustReturnValue();
			if (result.hasFatalError()) {
				fChangeManager.clear();
				return result;
			}
			descriptions.addAll(analyzer.getGroupDescriptions());
			if (!owner.equals(unit) && fParentRefactoring == null)
				createEdits(unit, rewriter, descriptions, importRewrite);
			sub.worked(1);
			if (pm.isCanceled())
				throw new OperationCanceledException();
		}
		ownerDescriptions.addAll(addGetterSetterChanges(fRoot, fRewriter, owner.findRecommendedLineSeparator(),usingLocalSetter, usingLocalGetter));
		if (fParentRefactoring == null)
			createEdits(owner, fRewriter, ownerDescriptions, fImportRewrite);

		sub.done();
		IFile[] filesToBeModified= ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits());
		result.merge(Checks.validateModifiesFiles(filesToBeModified, getValidationContext(), pm));
		if (result.hasFatalError())
			return result;
		ResourceChangeChecker.checkFilesToBeChanged(filesToBeModified, Progress.subMonitor(pm, 1));
		return result;
	}

	private class AffectedCUVisitor extends ASTVisitor {
		private String fTypeName= null;
		private boolean fGetterConflict= false;

		public String getTypeName() {
			return fTypeName;
		}

		public boolean isGetterConflict() {
			return fGetterConflict;
		}

		@Override
		public boolean visit(TypeDeclaration node) {
			ITypeBinding typeBinding= node.resolveBinding();
			while (typeBinding != null) {
				IMethodBinding[] methodBindings= typeBinding.getDeclaredMethods();
				for (IMethodBinding methodBinding : methodBindings) {
					if (methodBinding.getName().equals(getGetterName()) && methodBinding.getParameterNames().length == 0) {
						fTypeName= typeBinding.getName();
						fGetterConflict= true;
						throw new AbortSearchException();
					}
					if (methodBinding.getName().equals(getSetterName()) && methodBinding.getParameterNames().length == 1) {
						if (methodBinding.getParameterTypes()[0].isEqualTo(fFieldDeclaration.resolveBinding().getType())) {
							fTypeName= typeBinding.getName();
							throw new AbortSearchException();
						}
					}
				}
				typeBinding= typeBinding.getSuperclass();
			}
			return true;
		}

	}

	private void checkAffectedCUs(RefactoringStatus result, ICompilationUnit[] affectedCUs) {
		AffectedCUVisitor visitor= new AffectedCUVisitor();
		try {
			for (ICompilationUnit cu : affectedCUs) {
				CompilationUnit root= new RefactoringASTParser(IASTSharedValues.SHARED_AST_LEVEL).parse(cu, true);
				root.accept(visitor);
			}
		} catch (AbortSearchException e) {
			result.addError(Messages.format(RefactoringCoreMessages.SelfEncapsulateField_subtype_method_exists, new String[] { visitor.isGetterConflict() ? getGetterName() : getSetterName(), visitor.getTypeName() }));
		}
	}

	private void createEdits(ICompilationUnit unit, ASTRewrite rewriter, List<TextEditGroup> groups, ImportRewrite importRewrite) throws CoreException {
		TextChange change= fChangeManager.get(unit);
		MultiTextEdit root= new MultiTextEdit();
		change.setEdit(root);
		root.addChild(importRewrite.rewriteImports(null));
		root.addChild(rewriter.rewriteAST());
		for (TextEditGroup textEditGroup : groups) {
			change.addTextEditGroup(textEditGroup);
		}
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException {
		String project= null;
		IJavaProject javaProject= fField.getJavaProject();
		if (javaProject != null)
			project= javaProject.getElementName();
		final String description= Messages.format(RefactoringCoreMessages.SelfEncapsulateField_descriptor_description_short, BasicElementLabels.getJavaElementName(fField.getElementName()));
		int flags= JavaRefactoringDescriptor.JAR_MIGRATION | JavaRefactoringDescriptor.JAR_REFACTORING | RefactoringDescriptor.STRUCTURAL_CHANGE | RefactoringDescriptor.MULTI_CHANGE;
		final IType declaring= fField.getDeclaringType();
		try {
			if (declaring.isAnonymous() || declaring.isLocal())
				flags|= JavaRefactoringDescriptor.JAR_SOURCE_ATTACHMENT;
		} catch (JavaModelException exception) {
			JavaManipulationPlugin.log(exception);
		}
		JDTRefactoringDescriptorComment comment = getCommentsForDescriptor();
		final Map<String, String> arguments= getArgumentsForDescriptor();
		final EncapsulateFieldDescriptor descriptor= RefactoringSignatureDescriptorFactory.createEncapsulateFieldDescriptor(project, description, comment.asString(), arguments, flags);

		final DynamicValidationRefactoringChange result= new DynamicValidationRefactoringChange(descriptor, getName());
		TextChange[] changes= fChangeManager.getAllChanges();
		pm.beginTask(NO_NAME, changes.length);
		pm.setTaskName(RefactoringCoreMessages.SelfEncapsulateField_create_changes);
		for (TextChange change : changes) {
			result.add(change);
			pm.worked(1);
		}
		pm.done();
		return result;
	}

	public Map<String, String> getArgumentsForDescriptor() {
		IJavaProject javaProject= fField.getJavaProject();
		String project= null;
		if (javaProject != null)
			project= javaProject.getElementName();
		final Map<String, String> arguments= new HashMap<>();
		arguments.put(JavaRefactoringDescriptorUtil.ATTRIBUTE_INPUT, JavaRefactoringDescriptorUtil.elementToHandle(project, fField));
		arguments.put(ATTRIBUTE_VISIBILITY, Integer.toString(fVisibility));
		arguments.put(ATTRIBUTE_INSERTION, Integer.toString(fInsertionIndex));
		if (fCreateSetter) {
			arguments.put(ATTRIBUTE_SETTER, fSetterName);
		}
		if (fCreateGetter) {
			arguments.put(ATTRIBUTE_GETTER, fGetterName);
		}
		arguments.put(ATTRIBUTE_COMMENTS, Boolean.toString(fGenerateJavadoc));
		arguments.put(ATTRIBUTE_DECLARING, Boolean.toString(fEncapsulateDeclaringClass));
		return arguments;
	}

	public JDTRefactoringDescriptorComment getCommentsForDescriptor() {
		IJavaProject javaProject= fField.getJavaProject();
		String project= null;
		if (javaProject != null)
			project= javaProject.getElementName();
		final IType declaring= fField.getDeclaringType();
		final String header= Messages.format(RefactoringCoreMessages.SelfEncapsulateFieldRefactoring_descriptor_description, new String[] { JavaElementLabelsCore.getElementLabel(fField, JavaElementLabelsCore.ALL_FULLY_QUALIFIED), JavaElementLabelsCore.getElementLabel(declaring, JavaElementLabelsCore.ALL_FULLY_QUALIFIED)});
		final JDTRefactoringDescriptorComment comment= new JDTRefactoringDescriptorComment(project, this, header);
		comment.addSetting(Messages.format(RefactoringCoreMessages.SelfEncapsulateField_original_pattern, JavaElementLabelsCore.getElementLabel(fField, JavaElementLabelsCore.ALL_FULLY_QUALIFIED)));
		if (fCreateGetter) {
			comment.addSetting(Messages.format(RefactoringCoreMessages.SelfEncapsulateField_getter_pattern, BasicElementLabels.getJavaElementName(fGetterName)));
		}
		if (fCreateSetter) {
			comment.addSetting(Messages.format(RefactoringCoreMessages.SelfEncapsulateField_setter_pattern, BasicElementLabels.getJavaElementName(fSetterName)));
		}
		String visibility= JdtFlags.getVisibilityString(fVisibility);
		if ("".equals(visibility)) //$NON-NLS-1$
			visibility= RefactoringCoreMessages.SelfEncapsulateField_default_visibility;
		comment.addSetting(Messages.format(RefactoringCoreMessages.SelfEncapsulateField_visibility_pattern, visibility));
		if (fEncapsulateDeclaringClass)
			comment.addSetting(RefactoringCoreMessages.SelfEncapsulateField_use_accessors);
		else
			comment.addSetting(RefactoringCoreMessages.SelfEncapsulateField_do_not_use_accessors);
		if (fGenerateJavadoc)
			comment.addSetting(RefactoringCoreMessages.SelfEncapsulateField_generate_comments);
		return comment;
	}

	@Override
	public String getName() {
		return RefactoringCoreMessages.SelfEncapsulateField_name;
	}

	//---- Helper methods -------------------------------------------------------------

	private void checkCompileErrors(RefactoringStatus result, CompilationUnit root, ICompilationUnit element) {
		for (IProblem problem : root.getProblems()) {
			if (!isIgnorableProblem(problem)) {
				result.addWarning(Messages.format(
					RefactoringCoreMessages.SelfEncapsulateField_compiler_errors_update,
					BasicElementLabels.getFileName(element)), JavaStatusContext.create(element));
				return;
			}
		}
	}

	private void checkInHierarchy(RefactoringStatus status, boolean usingLocalGetter, boolean usingLocalSetter) {
		AbstractTypeDeclaration declaration= ASTNodes.getParent(fFieldDeclaration, AbstractTypeDeclaration.class);
		ITypeBinding type= declaration.resolveBinding();
		if (type != null) {
			ITypeBinding fieldType= fFieldDeclaration.resolveBinding().getType();
			if (fCreateGetter) {
				checkMethodInHierarchy(type, fGetterName, fieldType,
						new ITypeBinding[0], status, usingLocalGetter);
			}
			if (fCreateSetter) {
				checkMethodInHierarchy(type, fSetterName, fFieldDeclaration.getAST().resolveWellKnownType("void"), //$NON-NLS-1$
						new ITypeBinding[] { fieldType }, status, usingLocalSetter);
			}
		}
	}

	public static void checkMethodInHierarchy(ITypeBinding type, String methodName, ITypeBinding returnType, ITypeBinding[] parameters, RefactoringStatus result, boolean reUseMethod) {
		IMethodBinding method= Bindings.findMethodInHierarchy(type, methodName, parameters);
		if (method != null) {
			boolean returnTypeClash= false;
			ITypeBinding methodReturnType= method.getReturnType();
			if (returnType != null && methodReturnType != null) {
				String returnTypeKey= returnType.getKey();
				String methodReturnTypeKey= methodReturnType.getKey();
				if (returnTypeKey == null && methodReturnTypeKey == null) {
					returnTypeClash= returnType != methodReturnType;
				} else if (returnTypeKey != null && methodReturnTypeKey != null) {
					returnTypeClash= !returnTypeKey.equals(methodReturnTypeKey);
				}
			}
			ITypeBinding dc= method.getDeclaringClass();
			if (returnTypeClash) {
				result.addError(Messages.format(RefactoringCoreMessages.Checks_methodName_returnTypeClash,
					new Object[] { BasicElementLabels.getJavaElementName(methodName), BasicElementLabels.getJavaElementName(dc.getName())}),
					JavaStatusContext.create(method));
			} else {
				if (!reUseMethod)
					result.addError(Messages.format(RefactoringCoreMessages.Checks_methodName_overrides,
							new Object[] { BasicElementLabels.getJavaElementName(methodName), BasicElementLabels.getJavaElementName(dc.getName())}),
						JavaStatusContext.create(method));
			}
		} else {
			if (reUseMethod){
				result.addError(Messages.format(RefactoringCoreMessages.SelfEncapsulateFieldRefactoring_nosuchmethod_status_fatalError,
						BasicElementLabels.getJavaElementName(methodName)),
						JavaStatusContext.create(method));
			}
		}
	}

	private void computeUsedNames() {
		fUsedReadNames= new ArrayList<>(0);
		fUsedModifyNames= new ArrayList<>(0);
		IVariableBinding binding= fFieldDeclaration.resolveBinding();
		ITypeBinding type= binding.getType();
		for (IMethodBinding method : binding.getDeclaringClass().getDeclaredMethods()) {
			ITypeBinding[] parameters = method.getParameterTypes();
			if (parameters == null || parameters.length == 0) {
				fUsedReadNames.add(method);
			} else if (parameters.length == 1 && parameters[0] == type) {
				fUsedModifyNames.add(method);
			}
		}
	}

	private List<TextEditGroup> addGetterSetterChanges(CompilationUnit root, ASTRewrite rewriter, String lineDelimiter, boolean usingLocalSetter, boolean usingLocalGetter) throws CoreException {
		List<TextEditGroup> result= new ArrayList<>(2);
		AST ast= root.getAST();
		FieldDeclaration decl= (FieldDeclaration)ASTNodes.getParent(fFieldDeclaration, ASTNode.FIELD_DECLARATION);
		int position= 0;
		int numberOfMethods= 0;
		List<BodyDeclaration> members= ASTNodes.getBodyDeclarations(decl.getParent());
		for (BodyDeclaration element : members) {
			if (element.getNodeType() == ASTNode.METHOD_DECLARATION) {
				if (fInsertionIndex == -1) {
					break;
				} else if (fInsertionIndex == numberOfMethods) {
					position++;
					break;
				}
				numberOfMethods++;
			}
			position++;
		}
		TextEditGroup description;
		ListRewrite rewrite= fRewriter.getListRewrite(decl.getParent(), getBodyDeclarationsProperty(decl.getParent()));
		if (!usingLocalGetter && fCreateGetter) {
			description= new TextEditGroup(RefactoringCoreMessages.SelfEncapsulateField_add_getter);
			result.add(description);
			rewrite.insertAt(createGetterMethod(ast, rewriter, lineDelimiter), position++, description);
		}
		if (!JdtFlags.isFinal(fField) && !usingLocalSetter && fCreateSetter) {
			description= new TextEditGroup(RefactoringCoreMessages.SelfEncapsulateField_add_setter);
			result.add(description);
			rewrite.insertAt(createSetterMethod(ast, rewriter, lineDelimiter), position, description);
		}
		if (fParentRefactoring == null && !JdtFlags.isPrivate(fField) && fCreateGetter && fCreateSetter)
			result.add(makeDeclarationPrivate(rewriter, decl));
		return result;
	}

	public void makeDeclarationPrivateIfNeeded() throws CoreException {
		List<TextEditGroup> description = fParentRefactoring.getDescriptionsForUnit(fField.getCompilationUnit());
		FieldDeclaration decl= (FieldDeclaration)ASTNodes.getParent(fFieldDeclaration, ASTNode.FIELD_DECLARATION);
		if (!JdtFlags.isPrivate(fField) && fCreateGetter && fCreateSetter)
			description.add(makeDeclarationPrivate(fRewriter, decl));
	}

	private TextEditGroup makeDeclarationPrivate(ASTRewrite rewriter, FieldDeclaration decl) {
		TextEditGroup description= new TextEditGroup(RefactoringCoreMessages.SelfEncapsulateField_change_visibility);
		VariableDeclarationFragment[] vdfs= new VariableDeclarationFragment[] { fFieldDeclaration };
		int includedModifiers= Modifier.PRIVATE;
		int excludedModifiers= Modifier.PROTECTED | Modifier.PUBLIC;
		VariableDeclarationRewrite.rewriteModifiers(decl, vdfs, includedModifiers, excludedModifiers, rewriter, description);
		return description;
	}

	private ChildListPropertyDescriptor getBodyDeclarationsProperty(ASTNode declaration) {
		if (declaration instanceof AnonymousClassDeclaration)
			return AnonymousClassDeclaration.BODY_DECLARATIONS_PROPERTY;
		else if (declaration instanceof AbstractTypeDeclaration)
			return ((AbstractTypeDeclaration) declaration).getBodyDeclarationsProperty();
		Assert.isTrue(false);
		return null;
	}

	private MethodDeclaration createSetterMethod(AST ast, ASTRewrite rewriter, String lineDelimiter) throws CoreException {
		FieldDeclaration field= ASTNodes.getParent(fFieldDeclaration, FieldDeclaration.class);
		Type type= field.getType();
		MethodDeclaration result= ast.newMethodDeclaration();
		result.setName(ast.newSimpleName(fSetterName));
		result.modifiers().addAll(ASTNodeFactory.newModifiers(ast, createModifiers()));
		if (fSetterMustReturnValue) {
			result.setReturnType2((Type)rewriter.createCopyTarget(type));
		}
		SingleVariableDeclaration param= ast.newSingleVariableDeclaration();
		result.parameters().add(param);
		param.setName(ast.newSimpleName(fArgName));
		param.setType((Type)rewriter.createCopyTarget(type));
		List<IExtendedModifier> modifiers= field.modifiers();
		for (IExtendedModifier modifier : modifiers) {
			if (modifier.isAnnotation()) {
				String annotationName= ((Annotation)modifier).getTypeName().getFullyQualifiedName();
				if ("NonNull".equals(annotationName) || "Nullable".equals(annotationName)) { //$NON-NLS-1$ //$NON-NLS-2$
					Annotation fieldAnnotation= (Annotation)rewriter.createCopyTarget((Annotation)modifier);
					param.modifiers().add(fieldAnnotation);
				}
			}
		}

		List<Dimension> extraDimensions= DimensionRewrite.copyDimensions(fFieldDeclaration.extraDimensions(), rewriter);
		param.extraDimensions().addAll(extraDimensions);

		Block block= ast.newBlock();
		result.setBody(block);

		String fieldAccess= createFieldAccess();
		String body= CodeGeneration.getSetterMethodBodyContent(fField.getCompilationUnit(), getTypeName(field.getParent()), fSetterName, fieldAccess, fArgName, lineDelimiter);
		if (body != null) {
			ASTNode setterNode= rewriter.createStringPlaceholder(body, ASTNode.BLOCK);
			block.statements().add(setterNode);
		} else {
			Assignment ass= ast.newAssignment();
			ass.setLeftHandSide((Expression) rewriter.createStringPlaceholder(fieldAccess, ASTNode.QUALIFIED_NAME));
			ass.setRightHandSide(ast.newSimpleName(fArgName));
			block.statements().add(ass);
		}
        if (fSetterMustReturnValue) {
        	ReturnStatement rs= ast.newReturnStatement();
        	rs.setExpression(ast.newSimpleName(fArgName));
        	block.statements().add(rs);
        }
        if (fGenerateJavadoc) {
			String string= CodeGeneration.getSetterComment(
				fField.getCompilationUnit(), getTypeName(field.getParent()), fSetterName,
				fField.getElementName(), ASTNodes.asString(type), fArgName,
				StubUtility.getBaseName(fField),
				lineDelimiter);
			if (string != null) {
				Javadoc javadoc= (Javadoc)fRewriter.createStringPlaceholder(string, ASTNode.JAVADOC);
				result.setJavadoc(javadoc);
			}
		}
		return result;
	}

	private MethodDeclaration createGetterMethod(AST ast, ASTRewrite rewriter, String lineDelimiter) throws CoreException {
		FieldDeclaration field= ASTNodes.getParent(fFieldDeclaration, FieldDeclaration.class);
		Type type= field.getType();
		MethodDeclaration result= ast.newMethodDeclaration();
		result.setName(ast.newSimpleName(fGetterName));
		result.modifiers().addAll(ASTNodeFactory.newModifiers(ast, createModifiers()));
		List<IExtendedModifier> modifiers= field.modifiers();
		for (IExtendedModifier modifier : modifiers) {
			if (modifier.isAnnotation()) {
				String annotationName= ((Annotation)modifier).getTypeName().getFullyQualifiedName();
				if ("NonNull".equals(annotationName) || "Nullable".equals(annotationName)) { //$NON-NLS-1$ //$NON-NLS-2$
					Annotation fieldAnnotation= (Annotation)rewriter.createCopyTarget((Annotation)modifier);
					result.modifiers().add(fieldAnnotation);
				}
			}
		}
		Type returnType= DimensionRewrite.copyTypeAndAddDimensions(type, fFieldDeclaration.extraDimensions(), rewriter);
		result.setReturnType2(returnType);

		Block block= ast.newBlock();
		result.setBody(block);

		String body= CodeGeneration.getGetterMethodBodyContent(fField.getCompilationUnit(), getTypeName(field.getParent()), fGetterName, fField.getElementName(), lineDelimiter);
		if (body != null) {
			ASTNode getterNode= rewriter.createStringPlaceholder(body, ASTNode.BLOCK);
	    	block.statements().add(getterNode);
		} else {
			ReturnStatement rs= ast.newReturnStatement();
			rs.setExpression(ast.newSimpleName(fField.getElementName()));
			block.statements().add(rs);
		}
	    if (fGenerateJavadoc) {
			String string= CodeGeneration.getGetterComment(
				fField.getCompilationUnit() , getTypeName(field.getParent()), fGetterName,
				fField.getElementName(), ASTNodes.asString(type),
				StubUtility.getBaseName(fField),
				lineDelimiter);
			if (string != null) {
				Javadoc javadoc= (Javadoc)fRewriter.createStringPlaceholder(string, ASTNode.JAVADOC);
				result.setJavadoc(javadoc);
			}
		}
		return result;
	}

	private int createModifiers() throws JavaModelException {
		int result= 0;
		if (Flags.isPublic(fVisibility))
			result|= Modifier.PUBLIC;
		else if (Flags.isProtected(fVisibility))
			result|= Modifier.PROTECTED;
		else if (Flags.isPrivate(fVisibility))
			result|= Modifier.PRIVATE;
		if (JdtFlags.isStatic(fField))
			result|= Modifier.STATIC;
		return result;
	}

	private String createFieldAccess() throws JavaModelException {
		String fieldName= fField.getElementName();
		boolean nameConflict= fArgName.equals(fieldName);

		if (JdtFlags.isStatic(fField)) {
			if (nameConflict) {
				return JavaModelUtil.concatenateName(fField.getDeclaringType().getElementName(), fieldName);
			}
		} else {
			if (nameConflict || StubUtility.useThisForFieldAccess(fField.getJavaProject())) {
				return "this." + fieldName; //$NON-NLS-1$
			}
		}
		return fieldName;
	}

	private void checkArgName() {
		String fieldName= fField.getElementName();
		boolean isStatic= true;
		try {
			isStatic= JdtFlags.isStatic(fField);
		} catch (JavaModelException e) {
			JavaManipulationPlugin.log(e);
		}
		if ( (isStatic && fArgName.equals(fieldName) && fieldName.equals(fField.getDeclaringType().getElementName()))
				|| JavaConventionsUtil.validateIdentifier(fArgName, fField).getSeverity() == IStatus.ERROR) {
			fArgName= "_" + fArgName; //$NON-NLS-1$
		}
	}

	private String getTypeName(ASTNode type) {
		if (type instanceof AbstractTypeDeclaration) {
			return ((AbstractTypeDeclaration)type).getName().getIdentifier();
		} else if (type instanceof AnonymousClassDeclaration) {
			ClassInstanceCreation node= ASTNodes.getParent(type, ClassInstanceCreation.class);
			return ASTNodes.asString(node.getType());
		}
		Assert.isTrue(false, "Should not happen"); //$NON-NLS-1$
		return null;
	}

	public RefactoringStatus initialize(JavaRefactoringArguments arguments) {
		final String handle= arguments.getAttribute(JavaRefactoringDescriptorUtil.ATTRIBUTE_INPUT);
		if (handle != null) {
			final IJavaElement element= JavaRefactoringDescriptorUtil.handleToElement(arguments.getProject(), handle, false);
			if (element == null || !element.exists() || element.getElementType() != IJavaElement.FIELD)
				return JavaRefactoringDescriptorUtil.createInputFatalStatus(element, getName(), IJavaRefactorings.ENCAPSULATE_FIELD);
			else {
				fField= (IField) element;
				try {
					initialize(fField);
				} catch (JavaModelException exception) {
					return JavaRefactoringDescriptorUtil.createInputFatalStatus(element, getName(), IJavaRefactorings.ENCAPSULATE_FIELD);
				}
			}
		} else
			return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JavaRefactoringDescriptorUtil.ATTRIBUTE_INPUT));
		String name= arguments.getAttribute(ATTRIBUTE_GETTER);
		if (name != null && !name.isEmpty())
			fGetterName= name;
		else
			return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_GETTER));
		name= arguments.getAttribute(ATTRIBUTE_SETTER);
		if (name != null && !name.isEmpty())
			fSetterName= name;
		else
			return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_SETTER));
		final String encapsulate= arguments.getAttribute(ATTRIBUTE_DECLARING);
		if (encapsulate != null) {
			fEncapsulateDeclaringClass= Boolean.parseBoolean(encapsulate);
		} else
			return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_DECLARING));
		final String matches= arguments.getAttribute(ATTRIBUTE_COMMENTS);
		if (matches != null) {
			fGenerateJavadoc= Boolean.parseBoolean(matches);
		} else
			return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_COMMENTS));
		final String visibility= arguments.getAttribute(ATTRIBUTE_VISIBILITY);
		if (visibility != null && !"".equals(visibility)) {//$NON-NLS-1$
			int flag= 0;
			try {
				flag= Integer.parseInt(visibility);
			} catch (NumberFormatException exception) {
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_VISIBILITY));
			}
			fVisibility= flag;
		}
		final String insertion= arguments.getAttribute(ATTRIBUTE_INSERTION);
		if (insertion != null && !"".equals(insertion)) {//$NON-NLS-1$
			int index= 0;
			try {
				index= Integer.parseInt(insertion);
			} catch (NumberFormatException exception) {
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_INSERTION));
			}
			fInsertionIndex= index;
		}
		return new RefactoringStatus();
	}

	public boolean isUsingLocalGetter() {
		return checkName(fGetterName, fUsedReadNames);
	}

	public boolean isUsingLocalSetter() {
		return checkName(fSetterName, fUsedModifyNames);
	}

	private static boolean checkName(String name, List<IMethodBinding> usedNames) {
		for (IMethodBinding method : usedNames) {
			String selector= method.getName();
			if (selector.equals(name)) {
				return true;
			}
		}
		return false;
	}

	private boolean isIgnorableProblem(IProblem problem) {
		if (problem.getID() == IProblem.NotVisibleField)
			return true;
		return false;
	}

	public boolean isConsiderVisibility() {
		return fConsiderVisibility;
	}

	public void setConsiderVisibility(boolean considerVisibility) {
		fConsiderVisibility= considerVisibility;
	}

	public boolean isSelected() {
		return fSelected;
	}

	public void setSelected(boolean selected) {
		fSelected = selected;
	}


}
