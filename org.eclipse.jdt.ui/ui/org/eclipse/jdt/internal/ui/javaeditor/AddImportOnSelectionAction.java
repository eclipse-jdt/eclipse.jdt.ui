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

package org.eclipse.jdt.internal.ui.javaeditor;


import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IStatusLineManager;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.IEditingSupport;
import org.eclipse.jface.text.IEditingSupportRegistry;
import org.eclipse.jface.text.source.ISourceViewer;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.eclipse.ui.progress.IProgressService;
import org.eclipse.ui.texteditor.IUpdate;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.ui.IWorkingCopyManager;

import org.eclipse.jdt.internal.corext.codemanipulation.AddImportsOperation;
import org.eclipse.jdt.internal.corext.codemanipulation.AddImportsOperation.IChooseImportQuery;
import org.eclipse.jdt.internal.corext.util.TypeInfo;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.jdt.internal.ui.util.ElementValidator;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.TypeInfoLabelProvider;


public class AddImportOnSelectionAction extends Action implements IUpdate {

	private CompilationUnitEditor fEditor;
	
	public AddImportOnSelectionAction(CompilationUnitEditor editor) {	
		super(JavaEditorMessages.getString("AddImportOnSelection.label"));		 //$NON-NLS-1$
		setToolTipText(JavaEditorMessages.getString("AddImportOnSelection.tooltip")); //$NON-NLS-1$
		setDescription(JavaEditorMessages.getString("AddImportOnSelection.description")); //$NON-NLS-1$
		fEditor= editor;
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.ADD_IMPORT_ON_SELECTION_ACTION);
		setEnabled(getCompilationUnit() != null);	
	}
	
	public void update() {
		setEnabled(fEditor != null && getCompilationUnit() != null);
	}	
			
	private ICompilationUnit getCompilationUnit () {
		if (fEditor == null) {
			return null;
		}
		
		IWorkingCopyManager manager= JavaPlugin.getDefault().getWorkingCopyManager();				
		return manager.getWorkingCopy(fEditor.getEditorInput());
	}
	
	/*
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	public void run() {
		final ICompilationUnit cu= getCompilationUnit();
		if (cu == null || fEditor == null)
			return;
		if (!ElementValidator.checkValidateEdit(cu, getShell(), JavaEditorMessages.getString("AddImportOnSelection.error.title"))) //$NON-NLS-1$
			return;
		if (!ActionUtil.isProcessable(getShell(), cu))
			return;
		
		ISelection selection= fEditor.getSelectionProvider().getSelection();
		IDocument doc= fEditor.getDocumentProvider().getDocument(fEditor.getEditorInput());
		if (selection instanceof ITextSelection && doc != null) {
			final ITextSelection textSelection= (ITextSelection) selection;
			AddImportOnSelectionAction.SelectTypeQuery query= new SelectTypeQuery(getShell());
			AddImportsOperation op= new AddImportsOperation(cu, doc, textSelection.getOffset(), textSelection.getLength(), query);
			IEditingSupport helper= createViewerHelper(textSelection, query);
			try {
				registerHelper(helper);
				IProgressService progressService= PlatformUI.getWorkbench().getProgressService();
				progressService.runInUI(fEditor.getSite().getWorkbenchWindow(), new WorkbenchRunnableAdapter(op, op.getScheduleRule()), op.getScheduleRule());
				IStatus status= op.getStatus();
				if (!status.isOK()) {
					IStatusLineManager manager= getStatusLineManager();
					if (manager != null) {
						manager.setMessage(status.getMessage());
					}
				}
			} catch (InvocationTargetException e) {
				ExceptionHandler.handle(e, getShell(), JavaEditorMessages.getString("AddImportOnSelection.error.title"), null); //$NON-NLS-1$
			} catch (InterruptedException e) {
				// Do nothing. Operation has been canceled.
			} finally {
				deregisterHelper(helper);
			}
		}		
	}

	private IEditingSupport createViewerHelper(final ITextSelection selection, final SelectTypeQuery query) {
		return new IEditingSupport() {

			public boolean isOriginator(DocumentEvent event, IRegion subjectRegion) {
				return subjectRegion.getOffset() <= selection.getOffset() + selection.getLength() &&  selection.getOffset() <= subjectRegion.getOffset() + subjectRegion.getLength();
			}

			public boolean ownsFocusShell() {
				return query.isShowing();
			}
			
		};
	}
	
	private void registerHelper(IEditingSupport helper) {
		ISourceViewer viewer= fEditor.getViewer();
		if (viewer instanceof IEditingSupportRegistry) {
			IEditingSupportRegistry registry= (IEditingSupportRegistry) viewer;
			registry.register(helper);
		}
	}

	private void deregisterHelper(IEditingSupport helper) {
		ISourceViewer viewer= fEditor.getViewer();
		if (viewer instanceof IEditingSupportRegistry) {
			IEditingSupportRegistry registry= (IEditingSupportRegistry) viewer;
			registry.unregister(helper);
		}
	}

	private Shell getShell() {
		return fEditor.getSite().getShell();
	}
	
	private static class SelectTypeQuery implements IChooseImportQuery {
		
		private final Shell fShell;
		private boolean fIsShowing;

		public SelectTypeQuery(Shell shell) {
			fShell= shell;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.corext.codemanipulation.AddImportsOperation.IChooseImportQuery#chooseImport(org.eclipse.jdt.internal.corext.util.TypeInfo[], java.lang.String)
		 */
		public TypeInfo chooseImport(TypeInfo[] results, String containerName) {
			int nResults= results.length;
			
			if (nResults == 0) {
				return null;
			} else if (nResults == 1) {
				return results[0];
			}
			
			if (containerName.length() != 0) {
				for (int i= 0; i < nResults; i++) {
					TypeInfo curr= results[i];
					if (containerName.equals(curr.getTypeContainerName())) {
						return curr;
					}
				}
			}		
			fIsShowing= true;
			ElementListSelectionDialog dialog= new ElementListSelectionDialog(fShell, new TypeInfoLabelProvider(TypeInfoLabelProvider.SHOW_FULLYQUALIFIED));
			dialog.setTitle(JavaEditorMessages.getString("AddImportOnSelection.dialog.title")); //$NON-NLS-1$
			dialog.setMessage(JavaEditorMessages.getString("AddImportOnSelection.dialog.message")); //$NON-NLS-1$
			dialog.setElements(results);
			if (dialog.open() == Window.OK) {
				fIsShowing= false;
				return (TypeInfo) dialog.getFirstResult();
			}
			fIsShowing= false;
			return null;
		}
		
		boolean isShowing() {
			return fIsShowing;
		}
	}
		
	
	private IStatusLineManager getStatusLineManager() {
		IEditorActionBarContributor contributor= fEditor.getEditorSite().getActionBarContributor();
		if (contributor instanceof EditorActionBarContributor) {
			return ((EditorActionBarContributor) contributor).getActionBars().getStatusLineManager();
		}
		return null;
	}
	
	/**
	 * @return Returns the scheduling rule for this operation
	 */
	public ISchedulingRule getScheduleRule() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}
	
}
