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
package org.eclipse.jdt.internal.ui.refactoring;

import java.util.Arrays;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.actions.GlobalBuildAction;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.refactoring.actions.ListDialog;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public class RefactoringSaveHelper {

	private boolean fFilesSaved;
	private boolean fAllowSaveAlways;

	
	public RefactoringSaveHelper(boolean allowSaveAlways) {
		fAllowSaveAlways= allowSaveAlways;
	}
	
	public RefactoringSaveHelper() {
		this(true);
	}

	public boolean saveEditors(Shell shell) {
		IEditorPart[] dirtyEditors= EditorUtility.getDirtyEditors();
		if (dirtyEditors.length == 0)
			return true;
		if (! saveAllDirtyEditors(shell, dirtyEditors))
			return false;
		try {
			// Save isn't cancelable.
			IWorkspace workspace= ResourcesPlugin.getWorkspace();
			IWorkspaceDescription description= workspace.getDescription();
			boolean autoBuild= description.isAutoBuilding();
			description.setAutoBuilding(false);
			workspace.setDescription(description);
			try {
				if (!JavaPlugin.getActiveWorkbenchWindow().getWorkbench().saveAllEditors(false))
					return false;
				fFilesSaved= true;
			} finally {
				description.setAutoBuilding(autoBuild);
				workspace.setDescription(description);
			}
			return true;
		} catch (CoreException e) {
			ExceptionHandler.handle(e, shell, 
				RefactoringMessages.RefactoringStarter_saving, RefactoringMessages.RefactoringStarter_unexpected_exception);  
			return false;
		}
	}

	public void triggerBuild() {
		if (fFilesSaved && ResourcesPlugin.getWorkspace().getDescription().isAutoBuilding()) {
			new GlobalBuildAction(JavaPlugin.getActiveWorkbenchWindow(), IncrementalProjectBuilder.INCREMENTAL_BUILD).run();
		}
	}
	
	private boolean saveAllDirtyEditors(Shell shell, IEditorPart[] dirtyEditors) {
		if (fAllowSaveAlways && RefactoringSavePreferences.getSaveAllEditors()) //must save everything
			return true;
		ListDialog dialog= new ListDialog(shell) {
			protected Control createDialogArea(Composite parent) {
				Composite result= (Composite) super.createDialogArea(parent);
				if (fAllowSaveAlways) {
					final Button check= new Button(result, SWT.CHECK);
					check.setText(RefactoringMessages.RefactoringStarter_always_save); 
					check.setSelection(RefactoringSavePreferences.getSaveAllEditors());
					check.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent e) {
							RefactoringSavePreferences.setSaveAllEditors(check.getSelection());
						}
					});
					applyDialogFont(result);
				}
				return result;
			}
		};
		dialog.setTitle(RefactoringMessages.RefactoringStarter_save_all_resources); 
		dialog.setAddCancelButton(true);
		dialog.setLabelProvider(createDialogLabelProvider());
		dialog.setMessage(RefactoringMessages.RefactoringStarter_must_save); 
		dialog.setContentProvider(new ArrayContentProvider());
		dialog.setInput(Arrays.asList(dirtyEditors));
		return dialog.open() == Window.OK;
	}
	
	public boolean hasFilesSaved() {
		return fFilesSaved;
	}
	
	private ILabelProvider createDialogLabelProvider() {
		return new LabelProvider() {
			public Image getImage(Object element) {
				return ((IEditorPart) element).getTitleImage();
			}
			public String getText(Object element) {
				return ((IEditorPart) element).getTitle();
			}
		};
	}	
}
