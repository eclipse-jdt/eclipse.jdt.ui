/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.IWorkbenchSite;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringExecutionStarter;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.javaeditor.JavaTextSelection;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public class RenameJavaElementAction extends SelectionDispatchAction {

	private CompilationUnitEditor fEditor;
	
	public RenameJavaElementAction(IWorkbenchSite site) {
		super(site);
	}
	
	public RenameJavaElementAction(CompilationUnitEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(SelectionConverter.canOperateOn(fEditor));
	}

	//---- Structured selection ------------------------------------------------

	public void selectionChanged(IStructuredSelection selection) {
		try {
			if (selection.size() == 1) {
				setEnabled(canEnable(selection));
				return;
			}
		} catch (JavaModelException e) {
			// http://bugs.eclipse.org/bugs/show_bug.cgi?id=19253
			if (JavaModelUtil.isExceptionToBeLogged(e))
				JavaPlugin.log(e);
		} catch (CoreException e) {
			JavaPlugin.log(e);
		}
		setEnabled(false);
	}
	
	private static boolean canEnable(IStructuredSelection selection) throws CoreException {
		IJavaElement element= getJavaElement(selection);
		if (element == null)
			return false;
		return isRenameAvailable(element);
	} 

	private static IJavaElement getJavaElement(IStructuredSelection selection) {
		if (selection.size() != 1)
			return null;
		Object first= selection.getFirstElement();
		if (! (first instanceof IJavaElement))
			return null;
		return (IJavaElement)first;
	}
	
	public void run(IStructuredSelection selection) {
		IJavaElement element= getJavaElement(selection);
		if (element == null)
			return;
		try {
			run(element);	
		} catch (CoreException e){
			ExceptionHandler.handle(e, RefactoringMessages.RenameJavaElementAction_name, RefactoringMessages.RenameJavaElementAction_exception);  
		}	
	}
	
	//---- text selection ------------------------------------------------------------

	public void selectionChanged(ITextSelection selection) {
		if (selection instanceof JavaTextSelection) {
			try {
				IJavaElement[] elements= ((JavaTextSelection)selection).resolveElementAtOffset();
				if (elements.length == 1) {
					setEnabled(isRenameAvailable(elements[0]));
				} else {
					setEnabled(false);
				}
			} catch (CoreException e) {
				setEnabled(false);
			}
		} else {
			setEnabled(true);
		}
	}

	public void run(ITextSelection selection) {
		try {
			IJavaElement element= getJavaElement();
			if (element != null && isRenameAvailable(element)) {
				run(element);
				return;
			}
		} catch (CoreException e) {
			ExceptionHandler.handle(e, RefactoringMessages.RenameJavaElementAction_name, RefactoringMessages.RenameJavaElementAction_exception);
		}
		MessageDialog.openInformation(getShell(), RefactoringMessages.RenameJavaElementAction_name, RefactoringMessages.RenameJavaElementAction_not_available);
	}
	
	public boolean canRun() {
		try {
			IJavaElement element= getJavaElement();
			if (element == null)
				return false;

			return isRenameAvailable(element);
		} catch (JavaModelException e) {
			if (JavaModelUtil.isExceptionToBeLogged(e))
				JavaPlugin.log(e);
		} catch (CoreException e) {
			JavaPlugin.log(e);
		}
		return false;
	}
	
	private IJavaElement getJavaElement() throws JavaModelException {
		IJavaElement[] elements= SelectionConverter.codeResolve(fEditor); 
		if (elements == null || elements.length != 1)
			return null;
		return elements[0];
	}
	
	//---- helper methods -------------------------------------------------------------------

	private void run(IJavaElement element) throws CoreException {
		// Work around for http://dev.eclipse.org/bugs/show_bug.cgi?id=19104		
		if (!ActionUtil.isProcessable(getShell(), element))
			return;
		//XXX workaround bug 31998
		if (ActionUtil.mustDisableJavaModelAction(getShell(), element))
			return;
		RefactoringExecutionStarter.startRenameRefactoring(element, getShell());
	}

	private static boolean isRenameAvailable(IJavaElement element) throws CoreException {
		switch (element.getElementType()) {
			case IJavaElement.JAVA_PROJECT:
				return RefactoringAvailabilityTester.isRenameAvailable((IJavaProject) element);
			case IJavaElement.PACKAGE_FRAGMENT_ROOT:
				return RefactoringAvailabilityTester.isRenameAvailable((IPackageFragmentRoot) element);
			case IJavaElement.PACKAGE_FRAGMENT:
				return RefactoringAvailabilityTester.isRenameAvailable((IPackageFragment) element);
			case IJavaElement.COMPILATION_UNIT:
				return RefactoringAvailabilityTester.isRenameAvailable((ICompilationUnit) element);
			case IJavaElement.TYPE:
				return RefactoringAvailabilityTester.isRenameAvailable((IType) element);
			case IJavaElement.METHOD:
				final IMethod method= (IMethod) element;
				if (method.isConstructor())
					return RefactoringAvailabilityTester.isRenameAvailable(method.getDeclaringType());
				else
					return RefactoringAvailabilityTester.isRenameAvailable(method);
			case IJavaElement.FIELD:
				final IField field= (IField) element;
				if (Flags.isEnum(field.getFlags()))
				return RefactoringAvailabilityTester.isRenameEnumConstAvailable(field);
				else
					return RefactoringAvailabilityTester.isRenameFieldAvailable(field);
			case IJavaElement.TYPE_PARAMETER:
				return RefactoringAvailabilityTester.isRenameAvailable((ITypeParameter) element);
			case IJavaElement.LOCAL_VARIABLE:
				return RefactoringAvailabilityTester.isRenameAvailable((ILocalVariable) element);
		}
		return false;
	}
}
