package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportContainer;

import org.eclipse.jdt.ui.IWorkingCopyManager;

import org.eclipse.jdt.internal.compiler.IProblem;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.WorkbenchRunnableWrapper;
import org.eclipse.jdt.internal.ui.codemanipulation.OrganizeImportsOperation;
import org.eclipse.jdt.internal.ui.codemanipulation.OrganizeImportsOperation.IChooseImportQuery;
import org.eclipse.jdt.internal.ui.dialogs.MultiElementListSelectionDialog;
import org.eclipse.jdt.internal.ui.preferences.ImportOrganizePreferencePage;
import org.eclipse.jdt.internal.ui.util.TypeInfo;
import org.eclipse.jdt.internal.ui.util.TypeInfoLabelProvider;

public class OrganizeImportsAction extends Action {
	
	private ITextEditor fEditor;
	
	public OrganizeImportsAction() {
		this(null);
	}
	
	public OrganizeImportsAction(ITextEditor editor) {
		super(JavaEditorMessages.getString("OrganizeImportsAction.label")); //$NON-NLS-1$
		setToolTipText(JavaEditorMessages.getString("OrganizeImportsAction.tooltip")); //$NON-NLS-1$
		setDescription(JavaEditorMessages.getString("OrganizeImportsAction.description")); //$NON-NLS-1$
		
		setContentEditor(editor);
		WorkbenchHelp.setHelp(this,	new Object[] { IJavaHelpContextIds.ORGANIZE_IMPORTS_ACTION });					
	}
	
	public static boolean canActionBeAdded(ISelection selection) {
		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
			List elements= ((IStructuredSelection)selection).toList();
			if (elements.size() == 1) {
				return (elements.get(0) instanceof IImportContainer);
			}
		}
		return false;
	}
	
	/**
	 * @see IAction#actionPerformed
	 */
	public void run() {
		IWorkingCopyManager manager= JavaPlugin.getDefault().getWorkingCopyManager();
		ICompilationUnit cu= manager.getWorkingCopy(fEditor.getEditorInput());
		if (cu != null) {
			String[] prefOrder= ImportOrganizePreferencePage.getImportOrderPreference();
			int threshold= ImportOrganizePreferencePage.getImportNumberThreshold();	
			OrganizeImportsOperation op= new OrganizeImportsOperation(cu, prefOrder, threshold, false, createChooseImportQuery());
			try {
				ProgressMonitorDialog dialog= new ProgressMonitorDialog(JavaPlugin.getActiveWorkbenchShell());
				dialog.run(false, true, new WorkbenchRunnableWrapper(op));
			} catch (InvocationTargetException e) {
				JavaPlugin.log(e);
				MessageDialog.openError(JavaPlugin.getActiveWorkbenchShell(), JavaEditorMessages.getString("OrganizeImportsAction.error.title"), e.getTargetException().getMessage()); //$NON-NLS-1$
			} catch (InterruptedException e) {
				IProblem problem= op.getParsingError();
				if (problem != null) {
					showParsingErrorDialog(problem);
					int start= problem.getSourceStart();
					int end= problem.getSourceEnd();
					if (start != -1 && end != -1) {
						fEditor.selectAndReveal(start, end - start);
					}
				}
			}
		} else {
			JavaPlugin.logErrorMessage("OrganizeImportsAction.run: Working copy is null");
		}	
	}
	
	private IChooseImportQuery createChooseImportQuery() {
		return new IChooseImportQuery() {
			public TypeInfo[] chooseImports(TypeInfo[][] openChoices) {
				return doChooseImports(openChoices);
			}
		};
	}
	
	private TypeInfo[] doChooseImports(TypeInfo[][] openChoices) {
		ILabelProvider labelProvider= new TypeInfoLabelProvider(TypeInfoLabelProvider.SHOW_FULLYQUALIFIED);
		
		MultiElementListSelectionDialog dialog= new MultiElementListSelectionDialog(JavaPlugin.getActiveWorkbenchShell(), labelProvider);
		dialog.setTitle(JavaEditorMessages.getString("OrganizeImportsAction.selectiondialog.title")); //$NON-NLS-1$
		dialog.setMessage(JavaEditorMessages.getString("OrganizeImportsAction.selectiondialog.message")); //$NON-NLS-1$
		dialog.setElements(openChoices);
		if (dialog.open() == dialog.OK) {
			Object[] result= dialog.getResult();
			ArrayList refs= new ArrayList(result.length);
			for (int i= 0; i < result.length; i++) {
				List types= (List) result[i];
				if (types.size() > 0) {
					refs.add(types.get(0));
				}
			}				
			return (TypeInfo[]) refs.toArray(new TypeInfo[refs.size()]);
		}
		return null;
	}
	
	public void setContentEditor(ITextEditor editor) {
		fEditor= editor;
		setEnabled(editor != null && fEditor.isEditable());
	}
	
	private void showParsingErrorDialog(IProblem problem) {
		String title= JavaEditorMessages.getString("OrganizeImportsAction.error.title");
		String[] args= { String.valueOf(problem.getSourceLineNumber()), problem.getMessage() };
		String message= JavaEditorMessages.getFormattedString("OrganizeImportsAction.error.parsing.message", args);
		MessageDialog.openError(JavaPlugin.getActiveWorkbenchShell(), title, message);
	}
}
