/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.actions;

import java.util.Arrays;
import java.util.Iterator;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPart;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICodeAssist;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.IClassFileEditorInput;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

import org.eclipse.jdt.ui.IWorkingCopyManager;

public class SelectionConverter {

	private static final IJavaElement[] EMPTY_RESULT= new IJavaElement[0];
	
	private SelectionConverter() {
		// no instance
	}
	
	public static IJavaElement[] getElements(IStructuredSelection selection) {
		if (!selection.isEmpty()) {
			IJavaElement[] result= new IJavaElement[selection.size()];
			int i= 0;
			for (Iterator iter= selection.iterator(); iter.hasNext(); i++) {
				Object element= (Object) iter.next();
				if (!(element instanceof IJavaElement))
					return EMPTY_RESULT;
				result[i]= (IJavaElement)element;
			}
			return result;
		}
		return EMPTY_RESULT;
	}
	
	public static IJavaElement[] codeResolveOrInput(JavaEditor editor) throws JavaModelException {
		IJavaElement input= getInput(editor);
		ITextSelection selection= (ITextSelection)editor.getSelectionProvider().getSelection();
		IJavaElement[] result= codeResolve(input, selection);
		if (result.length == 0 && selection.getLength() == 0) {
			result= new IJavaElement[] {input};
		}
		return result;
	}
	
	public static IJavaElement codeResolveOrInput(JavaEditor editor, Shell shell, String title, String message) throws JavaModelException {
		IJavaElement[] elements= codeResolveOrInput(editor);
		if (elements == null || elements.length == 0)
			return null;
		IJavaElement candidate= elements[0];
		if (elements.length > 1) {
			candidate= OpenActionUtil.selectJavaElement(Arrays.asList(elements), shell, title, message);
		}
		return candidate;
	}
		
	public static IJavaElement[] codeResolve(JavaEditor editor) throws JavaModelException {
			return codeResolve(getInput(editor), (ITextSelection)editor.getSelectionProvider().getSelection());
	}
	
	public static IJavaElement elementAtOffset(JavaEditor editor) throws JavaModelException {
			return elementAtOffset(getInput(editor), (ITextSelection)editor.getSelectionProvider().getSelection());
	}
	
	public static IJavaElement getInput(JavaEditor editor) {
		IEditorInput input= editor.getEditorInput();
		if (input instanceof IClassFileEditorInput)
			return ((IClassFileEditorInput)input).getClassFile();
		IWorkingCopyManager manager= JavaPlugin.getDefault().getWorkingCopyManager();				
		return manager.getWorkingCopy(input);			
	}

	private static IJavaElement[] codeResolve(IJavaElement input, ITextSelection selection) throws JavaModelException {
			if (input instanceof ICodeAssist) {
				IJavaElement[] elements= ((ICodeAssist)input).codeSelect(selection.getOffset(), selection.getLength());
				if (elements != null && elements.length > 0)
					return elements;
			}
			return EMPTY_RESULT;
	}
	
	private static IJavaElement elementAtOffset(IJavaElement input, ITextSelection selection) throws JavaModelException {
		if (input instanceof ICompilationUnit) {
			ICompilationUnit cunit= (ICompilationUnit)input;
			if (cunit.isWorkingCopy()) {
				synchronized (cunit) {
					cunit.reconcile();
				}
			}
			IJavaElement ref= cunit.getElementAt(selection.getOffset());
			if (ref != null) {
				return ref;
			}
		} else if (input instanceof IClassFile) {
			IJavaElement ref= ((IClassFile)input).getElementAt(selection.getOffset());
			if (ref != null) {
				return ref;
			}
		}
		return null;
	}
}
