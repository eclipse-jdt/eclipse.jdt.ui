package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportContainer;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.compiler.IProblem;

import org.eclipse.jdt.ui.IWorkingCopyManager;

import org.eclipse.jdt.internal.corext.codemanipulation.OrganizeImportsOperation;
import org.eclipse.jdt.internal.corext.codemanipulation.OrganizeImportsOperation.IChooseImportQuery;
import org.eclipse.jdt.internal.corext.util.TypeInfo;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.jdt.internal.ui.dialogs.MultiElementListSelectionDialog;
import org.eclipse.jdt.internal.ui.preferences.ImportOrganizePreferencePage;
import org.eclipse.jdt.internal.ui.util.BusyIndicatorRunnableContext;
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
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.ORGANIZE_IMPORTS_ACTION);					
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
			boolean ignoreLowerCaseNames= ImportOrganizePreferencePage.doIgnoreLowerCaseNames();
			OrganizeImportsOperation op= new OrganizeImportsOperation(cu, prefOrder, threshold, ignoreLowerCaseNames, false, createChooseImportQuery());
			try {
				BusyIndicatorRunnableContext context= new BusyIndicatorRunnableContext();
				context.run(false, true, new WorkbenchRunnableAdapter(op));
				int nImportsAdded= op.getNumberOfImportsAdded();
				String message;
				if (nImportsAdded >= 0) {
					message= JavaEditorMessages.getFormattedString("OrganizeImportsAction.summary_added", String.valueOf(nImportsAdded)); //$NON-NLS-1$
				} else {
					message= JavaEditorMessages.getFormattedString("OrganizeImportsAction.summary_removed", String.valueOf(-nImportsAdded)); //$NON-NLS-1$
				}
				setStatusBarMessage(message);
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
			JavaPlugin.logErrorMessage("OrganizeImportsAction.run: Working copy is null"); //$NON-NLS-1$
		}	
	}
	
	private IChooseImportQuery createChooseImportQuery() {
		return new IChooseImportQuery() {
			public TypeInfo[] chooseImports(TypeInfo[][] openChoices, ISourceRange[] ranges) {
				return doChooseImports(openChoices, ranges);
			}
		};
	}
	
	private TypeInfo[] doChooseImports(TypeInfo[][] openChoices, final ISourceRange[] ranges) {
		// remember selection
		ISelection sel= fEditor.getSelectionProvider().getSelection();
		TypeInfo[] result= null;;
		ILabelProvider labelProvider= new TypeInfoLabelProvider(TypeInfoLabelProvider.SHOW_FULLYQUALIFIED);
		
		MultiElementListSelectionDialog dialog= new MultiElementListSelectionDialog(JavaPlugin.getActiveWorkbenchShell(), labelProvider) {
			protected void handleSelectionChanged() {
				super.handleSelectionChanged();
				// show choices in editor
				doListSelectionChanged(getCurrentPage(), ranges);
			}
		};
		dialog.setTitle(JavaEditorMessages.getString("OrganizeImportsAction.selectiondialog.title")); //$NON-NLS-1$
		dialog.setMessage(JavaEditorMessages.getString("OrganizeImportsAction.selectiondialog.message")); //$NON-NLS-1$
		dialog.setElements(openChoices);
		if (dialog.open() == dialog.OK) {
			Object[] res= dialog.getResult();			
			result= new TypeInfo[res.length];
			for (int i= 0; i < res.length; i++) {
				Object[] array= (Object[]) res[i];
				if (array.length > 0)
					result[i]= (TypeInfo) array[0];
			}
		}
		// restore selection
		if (sel instanceof ITextSelection) {
			ITextSelection textSelection= (ITextSelection) sel;
			fEditor.selectAndReveal(textSelection.getOffset(), textSelection.getLength());
		}
		return result;
	}
	
	private void doListSelectionChanged(int page, ISourceRange[] ranges) {
		if (page >= 0 && page < ranges.length) {
			ISourceRange range= ranges[page];
			fEditor.selectAndReveal(range.getOffset(), range.getLength());
		}
	}
	
	public void setContentEditor(ITextEditor editor) {
		fEditor= editor;
		setEnabled(editor != null && fEditor.isEditable());
	}
	
	private void showParsingErrorDialog(IProblem problem) {
		String title= JavaEditorMessages.getString("OrganizeImportsAction.error.title"); //$NON-NLS-1$
		String[] args= { String.valueOf(problem.getSourceLineNumber()), problem.getMessage() };
		String message= JavaEditorMessages.getFormattedString("OrganizeImportsAction.error.parsing.message", args); //$NON-NLS-1$
		MessageDialog.openInformation(JavaPlugin.getActiveWorkbenchShell(), title, message);
	}
	
	private void setStatusBarMessage(String message) {
		IEditorActionBarContributor contributor= fEditor.getEditorSite().getActionBarContributor();
		if (contributor instanceof EditorActionBarContributor) {
			IStatusLineManager manager= ((EditorActionBarContributor) contributor).getActionBars().getStatusLineManager();
			manager.setMessage(message);
		}
	}
	
}
