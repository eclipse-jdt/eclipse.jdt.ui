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
package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.IWorkbenchSite;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;
import org.eclipse.jdt.ui.refactoring.RenameSupport;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

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
			setEnabled(canEnable(selection));
			return;
		} catch (JavaModelException e) {
			// http://bugs.eclipse.org/bugs/show_bug.cgi?id=19253
			if (JavaModelUtil.filterNotPresentException(e))
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
		RenameSupport support= createGeneric(element, null, RenameSupport.UPDATE_REFERENCES);
		return support != null && support.preCheck().isOK();
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
			run(element, null);	
		} catch (CoreException e){
			ExceptionHandler.handle(e, RefactoringMessages.getString("RenameJavaElementAction.name"), RefactoringMessages.getString("RenameJavaElementAction.exception"));  //$NON-NLS-1$ //$NON-NLS-2$
		}	
	}
	
	//---- text selection ------------------------------------------------------------

	public void selectionChanged(ITextSelection selection) {
		if (selection instanceof JavaTextSelection) {
			try {
				IJavaElement[] elements= ((JavaTextSelection)selection).resolveElementAtOffset();
				if (elements.length == 1) {
					RenameSupport support= createGeneric(elements[0], null, RenameSupport.UPDATE_REFERENCES);
					setEnabled(support != null && support.preCheck().isOK());
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
			if (element != null) {
				RenameSupport support= createGeneric(element, null, RenameSupport.UPDATE_REFERENCES);
				if (support != null && support.preCheck().isOK()) {
					run(element, support);
					return;
				}
			}
		} catch (CoreException e){
			ExceptionHandler.handle(e, RefactoringMessages.getString("RenameJavaElementAction.name"), RefactoringMessages.getString("RenameJavaElementAction.exception"));  //$NON-NLS-1$ //$NON-NLS-2$
		}	
		MessageDialog.openInformation(getShell(), RefactoringMessages.getString("RenameJavaElementAction.name"), RefactoringMessages.getString("RenameJavaElementAction.not_available"));  //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	public boolean canRun() {
		IJavaElement element= getJavaElement();
		if (element == null)
			return false;
		try {
			RenameSupport support= createGeneric(element, null, RenameSupport.UPDATE_REFERENCES);
			return support != null && support.preCheck().isOK();
		} catch (JavaModelException e) {
			if (JavaModelUtil.filterNotPresentException(e))
				JavaPlugin.log(e);
		} catch (CoreException e) {
			JavaPlugin.log(e);
		}
		return false;
	}
	
	private IJavaElement getJavaElement() {
		IJavaElement[] elements= SelectionConverter.codeResolveHandled(fEditor, getShell(), RefactoringMessages.getString("RenameJavaElementAction.name")); //$NON-NLS-1$
		if (elements == null || elements.length != 1)
			return null;
		return elements[0];
	}
	
	//---- helper methods -------------------------------------------------------------------

	private void run(IJavaElement element, RenameSupport support) throws CoreException {
		// Work around for http://dev.eclipse.org/bugs/show_bug.cgi?id=19104		
		if (!ActionUtil.isProcessable(getShell(), element))
			return;
		//XXX workaround bug 31998
		if (ActionUtil.mustDisableJavaModelAction(getShell(), element))
			return;
		if (support == null) {
			support= createGeneric(element, null, RenameSupport.UPDATE_REFERENCES);
			if (support == null || !support.preCheck().isOK())
				return;
		}
		support.openDialog(getShell());
	}
	
	private static RenameSupport createGeneric(IJavaElement element, String newName, int flags) throws CoreException {
		switch (element.getElementType()) {
			case IJavaElement.JAVA_PROJECT:
				return RenameSupport.create((IJavaProject)element, newName, flags); 
			case IJavaElement.PACKAGE_FRAGMENT_ROOT:
				return RenameSupport.create((IPackageFragmentRoot)element, newName); 
			case IJavaElement.PACKAGE_FRAGMENT:
				return RenameSupport.create((IPackageFragment)element, newName, flags); 
			case IJavaElement.COMPILATION_UNIT:
				return RenameSupport.create((ICompilationUnit)element, newName, flags); 
			case IJavaElement.TYPE:
				return RenameSupport.create((IType)element, newName, flags); 
			case IJavaElement.METHOD:
				final IMethod method= (IMethod) element;
				if (method.isConstructor())
					return createGeneric(method.getDeclaringType(), newName, flags);
				else
					return RenameSupport.create((IMethod)element, newName, flags); 
			case IJavaElement.FIELD:
				return RenameSupport.create((IField)element, newName, flags);
			case IJavaElement.TYPE_PARAMETER:
				return RenameSupport.create((ITypeParameter)element, newName, flags);
		}
		return null;
	}	
}
