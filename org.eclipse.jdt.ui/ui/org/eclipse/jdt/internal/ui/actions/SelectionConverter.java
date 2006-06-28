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
package org.eclipse.jdt.internal.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICodeAssist;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

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
	 * selection is not of type <code>IJavaElement</code>
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
		
	public static IJavaElement[] codeResolveOrInputForked(JavaEditor editor) throws InvocationTargetException, InterruptedException {
		IJavaElement input= getInput(editor);
		ITextSelection selection= (ITextSelection)editor.getSelectionProvider().getSelection();
		IJavaElement[] result= performForkedCodeResolve(input, selection);
		if (result.length == 0) {
			result= new IJavaElement[] {input};
		}
		return result;
	}
				
	public static IJavaElement[] codeResolve(JavaEditor editor) throws JavaModelException {
		return codeResolve(editor, true);
	}
		
	/**
	 * @param primaryOnly if <code>true</code> only primary working copies will be returned
	 * @since 3.2
	 */
	public static IJavaElement[] codeResolve(JavaEditor editor, boolean primaryOnly) throws JavaModelException {
		return codeResolve(getInput(editor, primaryOnly), (ITextSelection)editor.getSelectionProvider().getSelection());
	}
	
	/**
	 * Perform a code resolve in a separate thread.
	 * @param primaryOnly if <code>true</code> only primary working copies will be returned
	 * @throws InterruptedException 
	 * @throws InvocationTargetException 
	 * @since 3.2
	 */
	public static IJavaElement[] codeResolveForked(JavaEditor editor, boolean primaryOnly) throws InvocationTargetException, InterruptedException {
		return performForkedCodeResolve(getInput(editor, primaryOnly), (ITextSelection)editor.getSelectionProvider().getSelection());
	}
			
	public static IJavaElement getElementAtOffset(JavaEditor editor) throws JavaModelException {
		return getElementAtOffset(editor, true);
	}
	
	/**
	 * @param primaryOnly if <code>true</code> only primary working copies will be returned
	 * @since 3.2
	 */
	private static IJavaElement getElementAtOffset(JavaEditor editor, boolean primaryOnly) throws JavaModelException {
		return getElementAtOffset(getInput(editor, primaryOnly), (ITextSelection)editor.getSelectionProvider().getSelection());
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
		return getInput(editor, true);
	}
	
	/**
	 * @param primaryOnly if <code>true</code> only primary working copies will be returned
	 * @since 3.2
	 */
	private static IJavaElement getInput(JavaEditor editor, boolean primaryOnly) {
		if (editor == null)
			return null;
		return EditorUtility.getEditorInputJavaElement(editor, primaryOnly);
	}
	
	public static ICompilationUnit getInputAsCompilationUnit(JavaEditor editor) {
		Object editorInput= SelectionConverter.getInput(editor);
		if (editorInput instanceof ICompilationUnit)
			return (ICompilationUnit)editorInput;
		return null;
	}

	public static IClassFile getInputAsClassFile(JavaEditor editor) {
		Object editorInput= SelectionConverter.getInput(editor);
		if (editorInput instanceof IClassFile)
			return (IClassFile)editorInput;
		return null;
	}

	private static IJavaElement[] performForkedCodeResolve(final IJavaElement input, final ITextSelection selection) throws InvocationTargetException, InterruptedException {
		final class CodeResolveRunnable implements IRunnableWithProgress {
			IJavaElement[] result;
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					result= codeResolve(input, selection);
				} catch (JavaModelException e) {
					throw new InvocationTargetException(e);
				}
			}
		}
		CodeResolveRunnable runnable= new CodeResolveRunnable();
		PlatformUI.getWorkbench().getProgressService().busyCursorWhile(runnable);
		return runnable.result;
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

	/**
	 * Shows a dialog for resolving an ambiguous java element.
	 * Utility method that can be called by subclasses.
	 */
	public static IJavaElement selectJavaElement(IJavaElement[] elements, Shell shell, String title, String message) {
		int nResults= elements.length;
		if (nResults == 0)
			return null;
		if (nResults == 1)
			return elements[0];
		
		int flags= JavaElementLabelProvider.SHOW_DEFAULT | JavaElementLabelProvider.SHOW_QUALIFIED | JavaElementLabelProvider.SHOW_ROOT;
						
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(shell, new JavaElementLabelProvider(flags));
		dialog.setTitle(title);
		dialog.setMessage(message);
		dialog.setElements(elements);
		
		if (dialog.open() == Window.OK) {
			return (IJavaElement) dialog.getFirstResult();
		}		
		return null;
	}
}
