/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.junit.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.dialogs.SelectionStatusDialog;

/**
 * Shows a dialog with test methods that refer to the selection.
 */
public class GotoReferencedTestAction implements IWorkbenchWindowActionDelegate {
	ISelection fSelection;
	IWorkbenchWindow fWorkbench;
	
	private void run(IStructuredSelection selection) {
		IJavaElement[] elements= getSelectedElements(selection);
		if (elements == null || elements.length == 0) {
			MessageDialog.openInformation(getShell(), "Go to Test", "Select a method, type, or compilation unit to open tests that refer to them.");
			return;
		}
		try {
			run(elements);
		} catch (CoreException e) {
			ErrorDialog.openError(getShell(), "Go to Test", "Error trying to find test", e.getStatus());
		}
	}
			
	private void run(ITextSelection ITextSelection) {
		try {
			JavaEditor editor= getActiveEditor();
			if (editor == null)
				return;
			IJavaElement element= SelectionConverter.getElementAtOffset(editor);
			int type= element != null ? element.getElementType() : -1;
			if (type != IJavaElement.METHOD && type != IJavaElement.TYPE) {
		 		element= SelectionConverter.getTypeAtOffset(editor);
		 		if (element == null) {
					MessageDialog.openInformation(getShell(), "Go to Test", "Selection is not inside of a type or method.");
					return;
		 		}
			}
			run(new IMember[] { (IMember)element });
		} catch (CoreException e) {
			ErrorDialog.openError(getShell(), "Go to Test", "Error trying to find test", e.getStatus());
		}
	}

	private void run(IJavaElement[] elements) throws PartInitException, JavaModelException {
		IJavaElement element= elements[0];
		
		SelectionStatusDialog dialog = new TestMethodSelectionDialog(getShell(), new ProgressMonitorDialog(getShell()), element); 
		dialog.setTitle("Select Test"); 
		dialog.setMessage("Select a test that refers to '"+element.getElementName()+"'."); 
		
		if (dialog.open() == SelectionDialog.CANCEL) 
			return;
	
		Object result = dialog.getFirstResult();
		if (result == null) 
			return;
				
		openElement((IJavaElement)result);
	}

	private void openElement(IJavaElement result) throws JavaModelException, PartInitException {
		IEditorPart part= JavaUI.openInEditor(result);
		JavaUI.revealInEditor(part, result);
	}
		
	private IJavaElement[] getSelectedElements(IStructuredSelection selection) {
		List elements= selection.toList();
		int size= elements.size();
		if (size == 0)
			return null;
			
		ArrayList result= new ArrayList(size);
			
		for (int i= 0; i < size; i++) {
			Object e= elements.get(i);
			if (e instanceof ICompilationUnit) {
				ICompilationUnit unit= (ICompilationUnit) e;
				IType[] types= new IType[0];
				try {
					types= unit.getTypes();
				} catch (JavaModelException ex) {
				}
				for (int j= 0; j < types.length; j++) {
					result.add(types[j]);
				} 
			}
			else if (e instanceof IMethod || e instanceof IType || e instanceof IField) {
				result.add(e);
			} else {
				return null;
			}
		}
		return (IJavaElement[])result.toArray(new IJavaElement[result.size()]);
	}
		
	public void run(IAction action) {
		if (fSelection instanceof IStructuredSelection)
			run((IStructuredSelection)fSelection);
		else if (fSelection instanceof ITextSelection) 
			run((ITextSelection)fSelection);
	}
	
	public void selectionChanged(IAction action, ISelection selection) {
		fSelection= selection;
	}
		
	private Shell getShell() {
		if (fWorkbench != null)
			return fWorkbench.getShell();
		return JUnitPlugin.getActiveWorkbenchShell();
	}
	
	public void dispose() {
	}
	
	public void init(IWorkbenchWindow window) {
		fWorkbench= window;
	}
	
	private JavaEditor getActiveEditor() {
		IEditorPart editor= fWorkbench.getActivePage().getActiveEditor();
		if (editor instanceof JavaEditor)
			return (JavaEditor) editor;
		return null;
	}
}