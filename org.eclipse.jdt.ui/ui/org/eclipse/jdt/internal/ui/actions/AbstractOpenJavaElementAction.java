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
package org.eclipse.jdt.internal.ui.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.Action;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;


/**
 * Opens a Java element in the specified editor. Subclasses must overwrite
 * <code>IAction.run</code> and knit the methods together.
 * 
 * @deprecated Use org.eclipse.jdt.ui.actions.OpenAction or SelectionDispatchAction instead
 */
public abstract class AbstractOpenJavaElementAction extends Action {
	
	/**
	 * Creates a new action without label. Initializing is 
	 * subclass responsibility.
	 */
	protected AbstractOpenJavaElementAction() {
	}
	
	/**
	 * Creates a new action without label. Initializing is 
	 * subclass responsibility.
	 */
	protected AbstractOpenJavaElementAction(String label) {
		super(label);
	}	
	
	/**
	 * Opens the editor on the given element and subsequently selects it.
	 */
	protected void open(IJavaElement element) throws JavaModelException, PartInitException {
		IEditorPart part= EditorUtility.openInEditor(element);
		EditorUtility.revealInEditor(part, element);
	}
	
	/**
	 * Filters out source references from the given code resolve results.
	 * A utility method that can be called by subclassers. 
	 */
	protected List filterResolveResults(IJavaElement[] codeResolveResults) {
		int nResults= codeResolveResults.length;
		List refs= new ArrayList(nResults);
		for (int i= 0; i < nResults; i++) {
			if (codeResolveResults[i] instanceof ISourceReference)
				refs.add(codeResolveResults[i]);
		}
		return refs;
	}
						
	/**
	 * Shows a dialog for resolving an ambigous java element.
	 * Utility method that can be called by subclassers.
	 */
	protected IJavaElement selectJavaElement(List elements, Shell shell, String title, String message) {
		
		int nResults= elements.size();
		
		if (nResults == 0)
			return null;
		
		if (nResults == 1)
			return (IJavaElement) elements.get(0);
		
		int flags= JavaElementLabelProvider.SHOW_DEFAULT
						| JavaElementLabelProvider.SHOW_QUALIFIED
						| JavaElementLabelProvider.SHOW_ROOT;
						
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(shell, new JavaElementLabelProvider(flags));
		dialog.setTitle(title);
		dialog.setMessage(message);
		dialog.setElements(elements.toArray());
		
		if (dialog.open() == dialog.OK) {
			Object[] selection= dialog.getResult();
			if (selection != null && selection.length > 0) {
				nResults= selection.length;
				for (int i= 0; i < nResults; i++) {
					Object current= selection[i];
					if (current instanceof IJavaElement)
						return (IJavaElement) current;
				}
			}
		}		
		return null;
	}
}
