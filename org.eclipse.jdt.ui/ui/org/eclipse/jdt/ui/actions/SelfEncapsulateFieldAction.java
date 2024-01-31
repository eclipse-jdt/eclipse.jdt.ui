/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
package org.eclipse.jdt.ui.actions;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringExecutionStarter;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.javaeditor.JavaTextSelection;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * Action to run the self encapsulate field refactoring.
 * <p>
 * Action is applicable to selections containing elements of type
 * <code>IField</code>.
 *
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 *
 * @since 2.0
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
public class SelfEncapsulateFieldAction extends SelectionDispatchAction {

	private JavaEditor fEditor;

	/**
	 * Creates a new <code>SelfEncapsulateFieldAction</code>. The action requires
	 * that the selection provided by the site's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 *
	 * @param site the site providing context information for this action
	 */
	public SelfEncapsulateFieldAction(IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.SelfEncapsulateFieldAction_label);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.SELF_ENCAPSULATE_ACTION);
	}

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 *
	 * @param editor the java editor
	 *
	 * @noreference This constructor is not intended to be referenced by clients.
	 */
	public SelfEncapsulateFieldAction(JavaEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(SelectionConverter.canOperateOn(fEditor));
	}

	//---- text selection -------------------------------------------------------

	@Override
	public void selectionChanged(ITextSelection selection) {
		setEnabled(true);
	}

	/**
	 * Note: This method is for internal use only. Clients should not call this method.
	 *
	 * @param selection the Java text selection
	 * @noreference This method is not intended to be referenced by clients.
	 */
	@Override
	public void selectionChanged(JavaTextSelection selection) {
		try {
			setEnabled(RefactoringAvailabilityTester.isSelfEncapsulateAvailable(selection));
		} catch (JavaModelException e) {
			// http://bugs.eclipse.org/bugs/show_bug.cgi?id=19253
			if (JavaModelUtil.isExceptionToBeLogged(e))
				JavaPlugin.log(e);
			setEnabled(false);//no UI
		}
	}

	@Override
	public void run(ITextSelection selection) {
		try {
			if (!ActionUtil.isEditable(fEditor))
				return;
			IJavaElement[] elements= SelectionConverter.codeResolve(fEditor, true);
			IField[] fields;
			List<IField> selfEncapsulationAvailableFields = new ArrayList<>();
			List<IField> preselected;
			if (elements.length == 1 && (elements[0] instanceof IField)) {
				IField field= (IField) elements[0];
				fields = field.getDeclaringType().getFields();
				preselected = Collections.singletonList((IField) elements[0]);
			} else {
				IType type = (IType) Optional.ofNullable(SelectionConverter.getElementAtOffset(fEditor)).map(element -> element.getAncestor(IJavaElement.TYPE)).orElse(null);
				if (type == null) {
					MessageDialog.openInformation(getShell(), ActionMessages.SelfEncapsulateFieldAction_dialog_title, ActionMessages.SelfEncapsulateFieldAction_dialog_unavailable);
					return;
				} else {
					fields = type.getFields();
					preselected = Collections.emptyList();
				}
			}

			for (IField field: fields) {
				if (RefactoringAvailabilityTester.isSelfEncapsulateAvailable(field)) {
					selfEncapsulationAvailableFields.add(field);
				}
			}
			if (selfEncapsulationAvailableFields.size() == 0) {
				MessageDialog.openInformation(getShell(), ActionMessages.SelfEncapsulateFieldAction_dialog_title, ActionMessages.SelfEncapsulateFieldAction_dialog_unavailable);
				return;
			}
			run(selfEncapsulationAvailableFields, preselected);

		} catch (JavaModelException exception) {
			JavaPlugin.log(exception);
			return;
		}
	}

	//---- structured selection -------------------------------------------------

	@Override
	public void selectionChanged(IStructuredSelection selection) {
		try {
			setEnabled(RefactoringAvailabilityTester.isSelfEncapsulateAvailable(selection));
		} catch (JavaModelException e) {
			// http://bugs.eclipse.org/bugs/show_bug.cgi?id=19253
			if (JavaModelUtil.isExceptionToBeLogged(e))
				JavaPlugin.log(e);
			setEnabled(false);//no UI
		}
	}

	@Override
	public void run(IStructuredSelection selection) {
		try {
			IField firstElement= (IField)selection.getFirstElement();
			if (!ActionUtil.isEditable(getShell(), firstElement))
				return;
			if (RefactoringAvailabilityTester.isSelfEncapsulateAvailable(selection)) {
				run(firstElement);
			}
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, RefactoringMessages.OpenRefactoringWizardAction_refactoring, RefactoringMessages.OpenRefactoringWizardAction_exception);
		}
	}

	//---- private helpers --------------------------------------------------------

	/*
	 * Should be private. But got shipped in this state in 2.0 so changing this is a
	 * breaking API change.
	 */
	public void run(IField field) {
		if (! ActionUtil.isEditable(fEditor, getShell(), field))
			return;
		RefactoringExecutionStarter.startSelfEncapsulateRefactoring(field, getShell());
	}

	/**
	 * @since 3.32
	 */
	private void run(List<IField> fields, List<IField> preselected) {
		List<IField> editableFields = fields.stream().filter(field -> ActionUtil.isEditable(fEditor, getShell(), field)).toList();
		RefactoringExecutionStarter.startSelfEncapsulateRefactoring(editableFields, preselected, getShell());
	}
}
