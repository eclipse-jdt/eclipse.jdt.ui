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
package org.eclipse.jdt.internal.ui.actions;

import java.util.Iterator;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICodeAssist;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPart;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.IClassFileEditorInput;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

import org.eclipse.jdt.ui.IWorkingCopyManager;

public class SelectionConverter {

	private static final IJavaElement[] EMPTY_RESULT= new IJavaElement[0];
	
	private SelectionConverter() {
		// no instance
	}

	/**
	 * Converts the selection provided by the given part into a structured selection.
	 * The following conversion rules are used:
	 * <ul>
	 *	<li><code>part instanceof JavaEditor</code>: returns a structured selection
	 * 	using code resolve to convert the editor's text selection.</li>
	 * <li><code>part instanceof IWorkbenchPart</code>: returns the part's selection
	 * 	if it is a structured selection.</li>
	 * <li><code>default</code>: returns an empty structured selection.</li>
	 * </ul>
	 */
	public static IStructuredSelection getStructuredSelection(IWorkbenchPart part) throws JavaModelException {
		if (part instanceof JavaEditor)
			return new StructuredSelection(codeResolve((JavaEditor)part));
		ISelectionProvider provider= part.getSite().getSelectionProvider();
		if (provider != null) {
			ISelection selection= provider.getSelection();
			if (selection instanceof IStructuredSelection)
				return (IStructuredSelection)selection;
		}
		return StructuredSelection.EMPTY;
	}

	
	/**
	 * Converts the given structured selection into an array of Java elements.
	 * An empty array is returned if one of the elements stored in the structured
	 * selection is not of tupe <code>IJavaElement</code>
	 */
	public static IJavaElement[] getElements(IStructuredSelection selection) {
		if (!selection.isEmpty()) {
			IJavaElement[] result= new IJavaElement[selection.size()];
			int i= 0;
			for (Iterator iter= selection.iterator(); iter.hasNext(); i++) {
				Object element= iter.next();
				if (!(element instanceof IJavaElement))
					return EMPTY_RESULT;
				result[i]= (IJavaElement)element;
			}
			return result;
		}
		return EMPTY_RESULT;
	}

	public static boolean canOperateOn(JavaEditor editor) {
		if (editor == null)
			return false;
		return getInput(editor) != null;
		
	}
	
	/**
	 * Converts the text selection provided by the given editor into an array of
	 * Java elements. If the selection doesn't cover a Java element and the selection's
	 * length is greater than 0 the methods returns the editor's input element.
	 */
	public static IJavaElement[] codeResolveOrInput(JavaEditor editor) throws JavaModelException {
		IJavaElement input= getInput(editor);
		ITextSelection selection= (ITextSelection)editor.getSelectionProvider().getSelection();
		IJavaElement[] result= codeResolve(input, selection);
		if (result.length == 0) {
			result= new IJavaElement[] {input};
		}
		return result;
	}
	
	public static IJavaElement[] codeResolveOrInputHandled(JavaEditor editor, Shell shell, String title) {
		try {
			return codeResolveOrInput(editor);
		} catch(JavaModelException e) {
			ExceptionHandler.handle(e, shell, title, ActionMessages.getString("SelectionConverter.codeResolve_failed")); //$NON-NLS-1$
		}
		return null;
	}
	
	/**
	 * Converts the text selection provided by the given editor a Java element by
	 * asking the user if code reolve returned more than one result. If the selection 
	 * doesn't cover a Java element and the selection's length is greater than 0 the 
	 * methods returns the editor's input element.
	 */
	public static IJavaElement codeResolveOrInput(JavaEditor editor, Shell shell, String title, String message) throws JavaModelException {
		IJavaElement[] elements= codeResolveOrInput(editor);
		if (elements == null || elements.length == 0)
			return null;
		IJavaElement candidate= elements[0];
		if (elements.length > 1) {
			candidate= OpenActionUtil.selectJavaElement(elements, shell, title, message);
		}
		return candidate;
	}
	
	public static IJavaElement codeResolveOrInputHandled(JavaEditor editor, Shell shell, String title, String message) {
		try {
			return codeResolveOrInput(editor, shell, title, message);
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, shell, title, ActionMessages.getString("SelectionConverter.codeResolveOrInput_failed")); //$NON-NLS-1$
		}
		return null;
	}
		
	public static IJavaElement[] codeResolve(JavaEditor editor) throws JavaModelException {
		return codeResolve(getInput(editor), (ITextSelection)editor.getSelectionProvider().getSelection());
	}

	/**
	 * Converts the text selection provided by the given editor a Java element by
	 * asking the user if code reolve returned more than one result. If the selection 
	 * doesn't cover a Java element <code>null</code> is returned.
	 */
	public static IJavaElement codeResolve(JavaEditor editor, Shell shell, String title, String message) throws JavaModelException {
		IJavaElement[] elements= codeResolve(editor);
		if (elements == null || elements.length == 0)
			return null;
		IJavaElement candidate= elements[0];
		if (elements.length > 1) {
			candidate= OpenActionUtil.selectJavaElement(elements, shell, title, message);
		}
		return candidate;
	}
	
	public static IJavaElement[] codeResolveHandled(JavaEditor editor, Shell shell, String title) {
		try {
			return codeResolve(editor);
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, shell, title, ActionMessages.getString("SelectionConverter.codeResolve_failed")); //$NON-NLS-1$
		}
		return null;
	}
	
	public static IJavaElement getElementAtOffset(JavaEditor editor) throws JavaModelException {
		return getElementAtOffset(getInput(editor), (ITextSelection)editor.getSelectionProvider().getSelection());
	}
	
	public static IType getTypeAtOffset(JavaEditor editor) throws JavaModelException {
		IJavaElement element= SelectionConverter.getElementAtOffset(editor);
		IType type= (IType)element.getAncestor(IJavaElement.TYPE);
		if (type == null) {
			ICompilationUnit unit= SelectionConverter.getInputAsCompilationUnit(editor);
			if (unit != null)
				type= unit.findPrimaryType();
		}
		return type;
	}
	
	public static IJavaElement getInput(JavaEditor editor) {
		if (editor == null)
			return null;
		IEditorInput input= editor.getEditorInput();
		if (input instanceof IClassFileEditorInput)
			return ((IClassFileEditorInput)input).getClassFile();
		IWorkingCopyManager manager= JavaPlugin.getDefault().getWorkingCopyManager();				
		return manager.getWorkingCopy(input);			
	}
	
	public static ICompilationUnit getInputAsCompilationUnit(JavaEditor editor) {
		Object editorInput= SelectionConverter.getInput(editor);
		if (editorInput instanceof ICompilationUnit)
			return (ICompilationUnit)editorInput;
		else
			return null;
	}

	public static IJavaElement[] codeResolve(IJavaElement input, ITextSelection selection) throws JavaModelException {
			if (input instanceof ICodeAssist) {
				if (input instanceof ICompilationUnit) {
					JavaModelUtil.reconcile((ICompilationUnit) input);
				}
				IJavaElement[] elements= ((ICodeAssist)input).codeSelect(selection.getOffset(), selection.getLength());
				if (elements != null && elements.length > 0)
					return elements;
			}
			return EMPTY_RESULT;
	}
	
	public static IJavaElement getElementAtOffset(IJavaElement input, ITextSelection selection) throws JavaModelException {
		if (input instanceof ICompilationUnit) {
			ICompilationUnit cunit= (ICompilationUnit) input;
			JavaModelUtil.reconcile(cunit);
			IJavaElement ref= cunit.getElementAt(selection.getOffset());
			if (ref == null)
				return input;
			else
				return ref;
		} else if (input instanceof IClassFile) {
			IJavaElement ref= ((IClassFile)input).getElementAt(selection.getOffset());
			if (ref == null)
				return input;
			else
				return ref;
		}
		return null;
	}
	
//	public static IJavaElement[] resolveSelectedElements(IJavaElement input, ITextSelection selection) throws JavaModelException {
//		IJavaElement enclosing= resolveEnclosingElement(input, selection);
//		if (enclosing == null)
//			return EMPTY_RESULT;
//		if (!(enclosing instanceof ISourceReference))
//			return EMPTY_RESULT;
//		ISourceRange sr= ((ISourceReference)enclosing).getSourceRange();
//		if (selection.getOffset() == sr.getOffset() && selection.getLength() == sr.getLength())
//			return new IJavaElement[] {enclosing};
//	}
	
	public static IJavaElement resolveEnclosingElement(JavaEditor editor, ITextSelection selection) throws JavaModelException {
		return resolveEnclosingElement(getInput(editor), selection);
	}
	
	public static IJavaElement resolveEnclosingElement(IJavaElement input, ITextSelection selection) throws JavaModelException {
		IJavaElement atOffset= null;
		if (input instanceof ICompilationUnit) {
			ICompilationUnit cunit= (ICompilationUnit)input;
			JavaModelUtil.reconcile(cunit);
			atOffset= cunit.getElementAt(selection.getOffset());
		} else if (input instanceof IClassFile) {
			IClassFile cfile= (IClassFile)input;
			atOffset= cfile.getElementAt(selection.getOffset());
		} else {
			return null;
		}
		if (atOffset == null) {
			return input;
		} else {
			int selectionEnd= selection.getOffset() + selection.getLength();
			IJavaElement result= atOffset;
			if (atOffset instanceof ISourceReference) {
				ISourceRange range= ((ISourceReference)atOffset).getSourceRange();
				while (range.getOffset() + range.getLength() < selectionEnd) {
					result= result.getParent();
					if (! (result instanceof ISourceReference)) {
						result= input;
						break;
					}
					range= ((ISourceReference)result).getSourceRange();
				}
			}
			return result;
		}
	}
}
