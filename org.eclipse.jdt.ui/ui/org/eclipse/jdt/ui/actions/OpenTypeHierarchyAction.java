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
package org.eclipse.jdt.ui.actions;

import java.util.List;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;

import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionChecker;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.OpenActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.util.OpenTypeHierarchyUtil;

/**
 * This action opens a type hierarchy on a element represented by either
 * <ul>
 * 	<li>a text selection inside a Java editor, or </li>
 * 	<li>a structured selection of a view part showing Java elements</li>
 * </ul>
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.0
 */
public class OpenTypeHierarchyAction extends SelectionDispatchAction {
	
	private JavaEditor fEditor;
	
	/**
	 * Creates a new <code>OpenTypeHierarchyAction</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public OpenTypeHierarchyAction(UnifiedSite site) {
		super(site);
		setText(ActionMessages.getString("OpenTypeHierarchyAction.label")); //$NON-NLS-1$
		setToolTipText(ActionMessages.getString("OpenTypeHierarchyAction.tooltip")); //$NON-NLS-1$
		setDescription(ActionMessages.getString("OpenTypeHierarchyAction.description")); //$NON-NLS-1$		
	}
	
	/**
	 * Creates a new <code>OpenAction</code>.
	 * <p>
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * </p>
	 */
	public OpenTypeHierarchyAction(JavaEditor editor) {
		this(UnifiedSite.create(editor.getEditorSite()));
		fEditor= editor;
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	protected void selectionChanged(ITextSelection selection) {
		setEnabled(fEditor != null);
	}

	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	protected void selectionChanged(IStructuredSelection selection) {
		setEnabled(getCandidates(selection) != null);
	}
	
	private IJavaElement[] getCandidates(IStructuredSelection selection) {
		if (selection.size() != 1)
			return null;
		return OpenTypeHierarchyUtil.getCandidates(selection.getFirstElement());
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	protected void run(ITextSelection selection) throws JavaModelException {
		run(SelectionConverter.codeResolveOrInput(fEditor));
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	protected void run(IStructuredSelection selection) {
		IJavaElement[] candidates= getCandidates(selection);
		if (candidates == null || candidates.length == 0)
			return;
		run(candidates);
	}
	
	private void run(IJavaElement[] elements) {
		OpenTypeHierarchyUtil.open(elements, getSite().getWorkbenchWindow());
	}	
}