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
package org.eclipse.jdt.ui.actions;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IEditorStatusLine;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.javaeditor.IClassFileEditorInput;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.search.ExceptionOccurrencesFinder;
import org.eclipse.jdt.internal.ui.search.FindOccurrencesEngine;

/**
 * Action to find all originators of a exception (e.g. method invocations, 
 * class casts, ...) for a given exception.
 * <p> 
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 3.1
 */
public class FindExceptionOccurrencesAction extends SelectionDispatchAction {
	
	private JavaEditor fEditor;
	
	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * 
	 * @param editor the Java editor
	 */
	public FindExceptionOccurrencesAction(JavaEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(getEditorInput(editor) != null);
	}
	
	/**
	 * Creates a new <code>FindExceptionOccurrencesAction</code>. The action 
	 * requires that the selection provided by the site's selection provider is of type 
	 * <code>IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public FindExceptionOccurrencesAction(IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.FindExceptionOccurrences_text); 
		setToolTipText(ActionMessages.FindExceptionOccurrences_toolTip); 
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.FIND_EXCEPTION_OCCURRENCES);
	}
	
	//---- Text Selection ----------------------------------------------------------------------
	
	/**
	 * {@inheritDoc}
	 */
	public void selectionChanged(ITextSelection selection) {
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * @since 3.2
	 */
	public void selectionChanged(IStructuredSelection selection) {
		setEnabled(getMember(selection) != null);
	}

	/* (non-JavaDoc)
	 * Method declared in SelectionDispatchAction.
	 */
	public final void run(ITextSelection ts) {
		IJavaElement input= getEditorInput(fEditor);
		if (!ActionUtil.isProcessable(getShell(), input))
			return;
		FindOccurrencesEngine engine= FindOccurrencesEngine.create(input, new ExceptionOccurrencesFinder());
		try {
			String result= engine.run(ts.getOffset(), ts.getLength());
			if (result != null)
				showMessage(getShell(), fEditor, result);
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
	}

	private static IJavaElement getEditorInput(JavaEditor editor) {
		IEditorInput input= editor.getEditorInput();
		if (input instanceof IClassFileEditorInput)
			return ((IClassFileEditorInput)input).getClassFile();
		return  JavaPlugin.getDefault().getWorkingCopyManager().getWorkingCopy(input);
	} 
		
	private static void showMessage(Shell shell, JavaEditor editor, String msg) {
		IEditorStatusLine statusLine= (IEditorStatusLine) editor.getAdapter(IEditorStatusLine.class);
		if (statusLine != null) 
			statusLine.setMessage(true, msg, null); 
		shell.getDisplay().beep();
	}
	
	private IMember getMember(IStructuredSelection selection) {
		return null;
	}
}
