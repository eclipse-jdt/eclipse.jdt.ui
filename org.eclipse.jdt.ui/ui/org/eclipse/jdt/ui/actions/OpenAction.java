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
import org.eclipse.jdt.internal.ui.actions.OpenActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

/**
 * This action opens a java editor on the element represented by either
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
public class OpenAction extends SelectionDispatchAction {
	
	private JavaEditor fEditor;
	
	/**
	 * Creates a new <code>OpenAction</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public OpenAction(UnifiedSite site) {
		super(site);
		setText(ActionMessages.getString("OpenAction.label")); //$NON-NLS-1$
		setToolTipText(ActionMessages.getString("OpenAction.tooltip")); //$NON-NLS-1$
		setDescription(ActionMessages.getString("OpenAction.description")); //$NON-NLS-1$		
	}
	
	/**
	 * Creates a new <code>OpenAction</code>.
	 * <p>
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * </p>
	 */
	public OpenAction(JavaEditor editor) {
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
		boolean enabled= false;
		ActionChecker checker= ActionChecker.create(getSelectionProvider());
		if (checker.elementsAreInstancesOf(selection, ISourceReference.class, false)) {
			enabled= true;
		} else {
			if (!selection.isEmpty() && selection instanceof IStructuredSelection) {
				IStructuredSelection ss= (IStructuredSelection)selection;
				if (ss.size() == 1) {
					Object element= ss.getFirstElement();
					enabled= checkImportDeclaration(element);
				}
			}
		}
		setEnabled(enabled);		
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
		run(SelectionConverter.getElements(selection));
	}
	
	private void run(IJavaElement[] elements) {
		if (elements != null && elements.length > 0) {
			IJavaElement candidate= null;
			if (elements.length == 1 && checkImportDeclaration(elements[0])) {
				IJavaElement declaration= elements[0];
				try {
					candidate= JavaModelUtil.findTypeContainer(declaration.getJavaProject(), declaration.getElementName());
				} catch(JavaModelException e) {
				}
			} else {
				List filtered= OpenActionUtil.filterResolveResults(elements);
				if (filtered.size() == 1) {
					candidate= (IJavaElement)filtered.get(0);
				} else if (filtered.size() > 1) {
					candidate= OpenActionUtil.selectJavaElement(filtered, getShell(), 
						ActionMessages.getString("OpenAction.dialog.title"), 	//$NON-NLS-1$
						ActionMessages.getString("OpenAction.dialog.message")); //$NON-NLS-1$
				}
			}
			if (candidate != null) {
				try {
					OpenActionUtil.open(candidate);
				} catch (JavaModelException x) {
					JavaPlugin.log(x.getStatus());
				} catch (PartInitException x) {
					JavaPlugin.log(x);
				}
				return;
			}
		}
		getShell().getDisplay().beep();		
	}
	
	private boolean checkImportDeclaration(Object element) {
		return element instanceof IImportDeclaration && !((IImportDeclaration)element).isOnDemand();
	}	
}