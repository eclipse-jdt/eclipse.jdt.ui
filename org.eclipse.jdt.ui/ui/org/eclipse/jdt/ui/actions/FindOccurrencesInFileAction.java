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
package org.eclipse.jdt.ui.actions;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.texteditor.IEditorStatusLine;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.javaeditor.IClassFileEditorInput;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.search.FindOccurrencesEngine;
import org.eclipse.jdt.internal.ui.search.OccurrencesFinder;
import org.eclipse.jdt.internal.ui.search.SearchMessages;

 
/**
 * Action to find all occurrences of a compilation unit member (e.g.
 * fields, methods, types, and local variables) in a file. 
 * <p>
 * Action is applicable to selections containing elements of type
 * <tt>IMember</tt>.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.1
 */
public class FindOccurrencesInFileAction extends SelectionDispatchAction {
	
	private JavaEditor fEditor;
	private IActionBars fActionBars;
	
	/**
	 * Creates a new <code>FindOccurrencesInFileAction</code>. The action requires 
	 * that the selection provided by the view part's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param part the part providing context information for this action
	 */
	public FindOccurrencesInFileAction(IViewPart part) {
		this(part.getSite());
		fActionBars= part.getViewSite().getActionBars();
	}
	
	/**
	 * Creates a new <code>FindOccurrencesInFileAction</code>. The action requires 
	 * that the selection provided by the page's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param page the page providing context information for this action
	 */
	public FindOccurrencesInFileAction(Page page) {
		this(page.getSite());
		fActionBars= page.getSite().getActionBars();
	}
 	
	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public FindOccurrencesInFileAction(JavaEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(getEditorInput(editor) != null);
	}
	
	public FindOccurrencesInFileAction(IWorkbenchSite site) {
		super(site);
		ISelection selection= getSelection();
		if (selection instanceof IStructuredSelection) {
			setEnabled(getMember((IStructuredSelection)selection) != null);		
		}
		setText(SearchMessages.getString("Search.FindOccurrencesInFile.label")); //$NON-NLS-1$
		setToolTipText(SearchMessages.getString("Search.FindOccurrencesInFile.tooltip")); //$NON-NLS-1$
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.FIND_OCCURRENCES_IN_FILE_ACTION);
	}
	
	//---- Structured Selection -------------------------------------------------------------
	
	/* (non-JavaDoc)
	 * Method declared in SelectionDispatchAction.
	 */
	public void selectionChanged(IStructuredSelection selection) {
		setEnabled(getMember(selection) != null);
	}
	
	/* (non-JavaDoc)
	 * Method declared in SelectionDispatchAction.
	 */
	private IMember getMember(IStructuredSelection selection) {
		if (selection.size() != 1)
			return null;
		Object o= selection.getFirstElement();
		if (o instanceof IMember) {
			IMember member= (IMember)o;
			IClassFile file= member.getClassFile();
			if (file != null) {
				try {
					if (file.getSourceRange() != null)
						return member;
				} catch (JavaModelException e) {
					return null;
				}
			}
			return member;
		}
		return null;
	}
	
	/* (non-JavaDoc)
	 * Method declared in SelectionDispatchAction.
	 */
	public void run(IStructuredSelection selection) {
		IMember member= getMember(selection);
		if (!ActionUtil.isProcessable(getShell(), member))
			return;
		FindOccurrencesEngine engine= FindOccurrencesEngine.create(member, new OccurrencesFinder());
		try {
			ISourceRange range= member.getNameRange();
			String result= engine.run(range.getOffset(), range.getLength());
			if (result != null)
				showMessage(getShell(), fActionBars, result);
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
	}
	
	private static void showMessage(Shell shell, IActionBars actionBars, String msg) {
		IStatusLineManager statusLine= actionBars.getStatusLineManager();
		if (statusLine != null)
			statusLine.setMessage(msg);
		shell.getDisplay().beep();
	}
	
	//---- Text Selection ----------------------------------------------------------------------
	
	/* (non-JavaDoc)
	 * Method declared in SelectionDispatchAction.
	 */
	public void selectionChanged(ITextSelection selection) {
	}

	/* (non-JavaDoc)
	 * Method declared in SelectionDispatchAction.
	 */
	public final void run(ITextSelection ts) {
		IJavaElement input= getEditorInput(fEditor);
		if (!ActionUtil.isProcessable(getShell(), input))
			return;
		FindOccurrencesEngine engine= FindOccurrencesEngine.create(input, new OccurrencesFinder());
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
}
