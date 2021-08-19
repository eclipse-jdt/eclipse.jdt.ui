/*******************************************************************************
 * Copyright (c) 2008, 2019 Mateusz Matela and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Mateusz Matela <mateusz.matela@gmail.com> - [code manipulation] [dcr] toString() builder wizard - https://bugs.eclipse.org/bugs/show_bug.cgi?id=26070
 *     Mateusz Matela <mateusz.matela@gmail.com> - [toString] toString() generator: Fields in declaration order - https://bugs.eclipse.org/bugs/show_bug.cgi?id=279924
  *    Pierre-Yves B. <pyvesdev@gmail.com> - Check whether enclosing instance implements hashCode and equals - https://bugs.eclipse.org/539900
 *******************************************************************************/
package org.eclipse.jdt.ui.actions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.resources.IWorkspaceRunnable;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Modifier;

import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.tostringgeneration.GenerateToStringOperation;
import org.eclipse.jdt.internal.corext.codemanipulation.tostringgeneration.ToStringGenerationSettings;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.dialogs.GenerateToStringDialog;
import org.eclipse.jdt.internal.ui.dialogs.SourceActionDialog;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;

/**
 * Adds method implementations for <code>{@link java.lang.Object#toString()}</code> The action opens a
 * dialog from which the user can choose the fields and methods to be considered.
 * <p>
 * Will open the parent compilation unit in a Java editor. The result is
 * unsaved, so the user can decide if the changes are acceptable.
 * <p>
 * The action is applicable to structured selections containing elements of type
 * {@link org.eclipse.jdt.core.IType}.
 *
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 *
 * @since 3.5
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
public class GenerateToStringAction extends GenerateMethodAbstractAction {

	private static final String METHODNAME_TO_STRING= "toString"; //$NON-NLS-1$

	private List<IVariableBinding> fFields;

	private List<IVariableBinding> fInheritedFields;

	private List<IVariableBinding> fSelectedFields;

	private List<IMethodBinding> fMethods;

	private List<IMethodBinding> fInheritedMethods;

	private GenerateToStringOperation operation;

	private static class ToStringInfo {

		public boolean foundToString= false;

		public boolean foundFinalToString= false;

		public ToStringInfo(ITypeBinding typeBinding) {
			for (IMethodBinding declaredMethod : typeBinding.getDeclaredMethods()) {
				if (METHODNAME_TO_STRING.equals(declaredMethod.getName()) && declaredMethod.getParameterTypes().length == 0) {
					this.foundToString= true;
					if (Modifier.isFinal(declaredMethod.getModifiers())) {
						this.foundFinalToString= true;
					}
				}
			}
		}
	}

	/**
	 * Creates a new generate tostring action.
	 * <p>
	 * The action requires that the selection provided by the site's selection
	 * provider is of type
	 * {@link org.eclipse.jface.viewers.IStructuredSelection}.
	 *
	 * @param site the workbench site providing context information for this
	 *            action
	 */
	public GenerateToStringAction(IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.GenerateToStringAction_label);
		setDescription(ActionMessages.GenerateToStringAction_description);
		setToolTipText(ActionMessages.GenerateToStringAction_tooltip);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.GENERATE_TOSTRING_ACTION);
	}

	/**
	 * Note: This constructor is for internal use only. Clients should not call
	 * this constructor.
	 *
	 * @param editor the compilation unit editor
	 *
	 * @noreference This constructor is not intended to be referenced by clients.
	 */
	public GenerateToStringAction(CompilationUnitEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled((fEditor != null && SelectionConverter.canOperateOn(fEditor)));
	}

	@Override
	RefactoringStatus checkMember(Object object) {
		// no conditions need to be checked
		return new RefactoringStatus();
	}

	@Override
	RefactoringStatus checkGeneralConditions(IType type, CodeGenerationSettings settings, Object[] selected) {
		return operation.checkConditions();
	}

	 @Override
	RefactoringStatus checkSuperClass(ITypeBinding superclass) {
		RefactoringStatus status= new RefactoringStatus();
		if (new ToStringInfo(superclass).foundFinalToString) {
			status.addError(Messages.format(ActionMessages.GenerateMethodAbstractAction_final_method_in_superclass_error, new String[] {
					Messages.format(ActionMessages.GenerateMethodAbstractAction_super_class, BasicElementLabels.getJavaElementName(superclass.getQualifiedName())),
					ActionMessages.GenerateToStringAction_tostring }), createRefactoringStatusContext(superclass.getJavaElement()));
		}
		return status;
	}

	@Override
	RefactoringStatus checkEnclosingClass(ITypeBinding enclosingClass) {
		// no conditions need to be checked
		return new RefactoringStatus();
	}

	@Override
	SourceActionDialog createDialog(Shell shell, IType type) throws JavaModelException {
		IVariableBinding[] fieldBindings= fFields.toArray(new IVariableBinding[0]);
		IVariableBinding[] inheritedFieldBindings= fInheritedFields.toArray(new IVariableBinding[0]);
		IVariableBinding[] selectedFieldBindings= fSelectedFields.toArray(new IVariableBinding[0]);
		IMethodBinding[] methodBindings= fMethods.toArray(new IMethodBinding[0]);
		IMethodBinding[] inheritededMethodBindings= fInheritedMethods.toArray(new IMethodBinding[0]);
		return new GenerateToStringDialog(shell, fEditor, type, fieldBindings, inheritedFieldBindings, selectedFieldBindings, methodBindings, inheritededMethodBindings);
	}

	@Override
	IWorkspaceRunnable createOperation(Object[] selectedBindings, CodeGenerationSettings settings, boolean regenerate, IJavaElement type, IJavaElement elementPosition) {
		return operation= GenerateToStringOperation.createOperation(fTypeBinding, selectedBindings, fUnit, elementPosition, (ToStringGenerationSettings)settings, true, false);
	}

	@Override
	CodeGenerationSettings createSettings(IType type, SourceActionDialog dialog) {
		ToStringGenerationSettings settings= ((GenerateToStringDialog) dialog).getGenerationSettings();
		super.createSettings(type, dialog).setSettings(settings);
		settings.createComments= dialog.getGenerateComment();
		settings.useBlocks= useBlocks(type.getJavaProject());
		String version= fUnit.getJavaElement().getJavaProject().getOption(JavaCore.COMPILER_SOURCE, true);
		settings.is50orHigher= !JavaModelUtil.isVersionLessThan(version, JavaCore.VERSION_1_5);
		settings.is60orHigher= !JavaModelUtil.isVersionLessThan(version, JavaCore.VERSION_1_6);
		return settings;
	}

	@Override
	boolean generateCandidates() throws JavaModelException {
		HashMap<IJavaElement, IVariableBinding> fieldsToBindings= new HashMap<>();
		HashMap<IJavaElement, IVariableBinding> selectedFieldsToBindings= new HashMap<>();
		for (IVariableBinding candidateField : fTypeBinding.getDeclaredFields()) {
			if (!Modifier.isStatic(candidateField.getModifiers())) {
				fieldsToBindings.put(candidateField.getJavaElement(), candidateField);
				if (!Modifier.isTransient(candidateField.getModifiers())) {
					selectedFieldsToBindings.put(candidateField.getJavaElement(), candidateField);
				}
			}
		}
		IType type= (IType)fTypeBinding.getJavaElement();
		final IField[] allFields;
		if (type.isRecord()) {
			allFields= type.getRecordComponents();
		} else {
			allFields= type.getFields();
		}
		fFields= new ArrayList<>();
		populateMembers(fFields, allFields, fieldsToBindings);
		fSelectedFields= new ArrayList<>();
		populateMembers(fSelectedFields, allFields, selectedFieldsToBindings);

		HashMap<IJavaElement, IMethodBinding> methodsToBindings= new HashMap<>();
		for (IMethodBinding candidateMethod : fTypeBinding.getDeclaredMethods()) {
			if (!Modifier.isStatic(candidateMethod.getModifiers()) && candidateMethod.getParameterTypes().length == 0 && !"void".equals(candidateMethod.getReturnType().getName()) && !"toString".equals(candidateMethod.getName()) && !"clone".equals(candidateMethod.getName())) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				methodsToBindings.put(candidateMethod.getJavaElement(), candidateMethod);
			}
		}
		fMethods= new ArrayList<>();
		populateMembers(fMethods, type.getMethods(), methodsToBindings);

		fInheritedFields= new ArrayList<>();
		fInheritedMethods= new ArrayList<>();
		ITypeBinding typeBinding= fTypeBinding;
		while ((typeBinding= typeBinding.getSuperclass()) != null) {
			type = (IType)typeBinding.getJavaElement();
			for (IVariableBinding candidateField : typeBinding.getDeclaredFields()) {
				if (!Modifier.isPrivate(candidateField.getModifiers()) && !Modifier.isStatic(candidateField.getModifiers()) && !contains(fFields, candidateField) && !contains(fInheritedFields, candidateField)) {
					fieldsToBindings.put(candidateField.getJavaElement(), candidateField);
				}
			}
			populateMembers(fInheritedFields, type.getFields(), fieldsToBindings);

			for (IMethodBinding candidateMethod : typeBinding.getDeclaredMethods()) {
				if (!Modifier.isPrivate(candidateMethod.getModifiers()) && !Modifier.isStatic(candidateMethod.getModifiers()) && candidateMethod.getParameterTypes().length == 0 && !"void".equals(candidateMethod.getReturnType().getName()) && !contains(fMethods, candidateMethod) && !contains(fInheritedMethods, candidateMethod) && !"clone".equals(candidateMethod.getName())) { //$NON-NLS-1$ //$NON-NLS-2$
					methodsToBindings.put(candidateMethod.getJavaElement(), candidateMethod);
				}
			}
			populateMembers(fInheritedMethods, type.getMethods(), methodsToBindings);
		}

		return true;
	}

	/**
	 * Populates <code>result</code> with the bindings from <code>membersToBindings</code>, sorted
	 * in the order of <code>allMembers</code>.
	 *
	 * @param result list of bindings from membersToBindings, sorted in source order
	 * @param allMembers all member elements in source order
	 * @param membersToBindings map from {@link IMember} to {@link IBinding}
	 * @since 3.6
	 */
	private static <T extends IBinding> void populateMembers(List<T> result, IMember[] allMembers, HashMap<IJavaElement, T> membersToBindings) {
		for (IMember member : allMembers) {
			T memberBinding= membersToBindings.remove(member);
			if (memberBinding != null) {
				result.add(memberBinding);
			}
		}
	}

	private static <T extends IBinding> boolean contains(List<T> inheritedFields, T member) {
		for (T object : inheritedFields) {
			if (object instanceof IVariableBinding && member instanceof IVariableBinding)
				if (((IVariableBinding) object).getName().equals(((IVariableBinding) member).getName()))
					return true;
			if (object instanceof IMethodBinding && member instanceof IMethodBinding)
				if (((IMethodBinding) object).getName().equals(((IMethodBinding) member).getName()))
					return true;
		}
		return false;
	}

	@Override
	String getAlreadyImplementedErrorMethodName() {
		return ActionMessages.GenerateToStringAction_tostring;
	}

	@Override
	boolean isMethodAlreadyImplemented(ITypeBinding typeBinding) {
		return new ToStringInfo(typeBinding).foundToString;
	}

	@Override
	String getErrorCaption() {
		return ActionMessages.GenerateToStringAction_error_caption;
	}

	@Override
	String getNoMembersError() {
		//no members error never occurs
		return null;
	}

}
