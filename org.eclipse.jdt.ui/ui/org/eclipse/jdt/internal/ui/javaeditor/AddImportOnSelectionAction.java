package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;

import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.IUpdate;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ITypeNameRequestor;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.ui.IWorkingCopyManager;
import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.codemanipulation.AddImportsOperation;
import org.eclipse.jdt.internal.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.jdt.internal.ui.util.TypeInfo;
import org.eclipse.jdt.internal.ui.util.TypeInfoRequestor;

public class AddImportOnSelectionAction extends Action implements IUpdate {
		
	private ITextEditor fEditor;
	
	public AddImportOnSelectionAction(ITextEditor editor) {	
		super(JavaEditorMessages.getString("AddImportOnSelection.label"));		 //$NON-NLS-1$
		setToolTipText(JavaEditorMessages.getString("AddImportOnSelection.tooltip")); //$NON-NLS-1$
		setDescription(JavaEditorMessages.getString("AddImportOnSelection.description")); //$NON-NLS-1$
		
		fEditor= editor;
		WorkbenchHelp.setHelp(this,	new Object[] { IJavaHelpContextIds.ADD_IMPORT_ON_SELECTION_ACTION });	
	}
	
	public AddImportOnSelectionAction() {
		this(null);
	}
	
	public void setContentEditor(ITextEditor editor) {
		fEditor= editor;
	}
	
	public void update() {
		boolean isEnabled= false;
		ISelection selection= fEditor.getSelectionProvider().getSelection();
		if (selection instanceof ITextSelection) {
			isEnabled= (((ITextSelection)selection).getLength() > 0);
		}
		setEnabled(isEnabled);
	}	
			
	private ICompilationUnit getCompilationUnit () {
		IWorkingCopyManager manager= JavaPlugin.getDefault().getWorkingCopyManager();				
		return manager.getWorkingCopy(fEditor.getEditorInput());
	}


	private void addImport(IJavaElement imp, ICompilationUnit cu) {
	}
	
	/**
	 * @see IAction#actionPerformed
	 */
	public void run() {
		ICompilationUnit cu= getCompilationUnit();
		if (cu != null) {
			ISelection s= fEditor.getSelectionProvider().getSelection();
			IDocument doc= fEditor.getDocumentProvider().getDocument(fEditor.getEditorInput());
			ITextSelection selection= (ITextSelection) s;
			
			if (selection.getLength() > 0 && doc != null) {
				try {
					int selStart= selection.getOffset();
					int nameStart= getNameStart(doc, selStart);
					int len= selStart - nameStart + selection.getLength();
					
					String name= doc.get(nameStart, len).trim();
					String typeName= Signature.getSimpleName(name);
					String packName= Signature.getQualifier(name);
					
					IType[] types= findAllTypes(typeName, cu.getJavaProject(), null);
					IType chosen= selectResult(types, packName, getShell());
					if (chosen == null) {
						return;
					}
					removeQualification(doc, nameStart, chosen);
					AddImportsOperation op= new AddImportsOperation(cu, new IJavaElement[] { chosen }, false);
					ProgressMonitorDialog dialog= new ProgressMonitorDialog(getShell());
					try {
						dialog.run(false, true, op);
					} catch (InvocationTargetException e) {
						JavaPlugin.log(e);
						MessageDialog.openError(getShell(), JavaEditorMessages.getString("AddImportOnSelection.error.title"), e.getTargetException().getMessage()); //$NON-NLS-1$
					} catch (InterruptedException e) {
						// Do nothing. Operation has been canceled.
					}
					return;
				} catch (CoreException e) {
					JavaPlugin.log(e);
					ErrorDialog.openError(getShell(), JavaEditorMessages.getString("AddImportOnSelection.error.title"), null, e.getStatus()); //$NON-NLS-1$
				} catch (BadLocationException e) {
					JavaPlugin.log(e);
					MessageDialog.openError(getShell(), JavaEditorMessages.getString("AddImportOnSelection.error.title"), e.getMessage()); //$NON-NLS-1$
				}
			}
		}
		getShell().getDisplay().beep();		
	}
	
	private int getNameStart(IDocument doc, int pos) throws BadLocationException {
		if (pos > 0 && doc.getChar(pos - 1) == '.') {
			pos--;
			while (pos > 0) {
				char ch= doc.getChar(pos - 1);
				if (!Character.isJavaIdentifierPart(ch) && ch != '.') {
					return pos;
				}
				pos--;
			}
		}
		return pos;
	}	
	
	private void removeQualification(IDocument doc, int nameStart, IType type) throws BadLocationException {
		String packName= type.getPackageFragment().getElementName();
		int packLen= packName.length();
		if (packLen > 0) {
			for (int k= 0; k < packLen; k++) {
				if (doc.getChar(nameStart + k) != packName.charAt(k)) {
					return;
				}
			}
			doc.replace(nameStart, packLen + 1, ""); //$NON-NLS-1$
		}
	}	

	/**
	 * Finds a type by the simple name.
	 */
	private static IType[] findAllTypes(String simpleTypeName, IJavaProject jproject, IProgressMonitor monitor) throws JavaModelException, CoreException {
		SearchEngine searchEngine= new SearchEngine();
		IProject project= jproject.getProject();
		IJavaSearchScope searchScope= SearchEngine.createJavaSearchScope(new IResource[] { project });

		ArrayList typeRefsFound= new ArrayList(10);
		ITypeNameRequestor requestor= new TypeInfoRequestor(typeRefsFound);

		searchEngine.searchAllTypeNames(
			project.getWorkspace(), 
			null, 
			simpleTypeName.toCharArray(), 
			IJavaSearchConstants.EXACT_MATCH, 
			IJavaSearchConstants.CASE_SENSITIVE, 
			IJavaSearchConstants.TYPE, 
			searchScope, 
			requestor, 
			IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH, 
			monitor);
			
		int nTypesFound= typeRefsFound.size();
		IType[] res= new IType[nTypesFound];
		for (int i= 0; i < nTypesFound; i++) {
			TypeInfo ref= (TypeInfo) typeRefsFound.get(i);
			res[i]= ref.resolveType(searchScope);
		}
		return res;
	}
	
	private Shell getShell() {
		return fEditor.getSite().getShell();
	}
		
						
	private IType selectResult(IType[] results, String packName, Shell shell) {
		int nResults= results.length;
		
		if (nResults == 0) {
			return null;
		} else if (nResults == 1) {
			return results[0];
		}
		
		if (packName.length() != 0) {
			for (int i= 0; i < results.length; i++) {
				IType curr= (IType) results[i];
				if (packName.equals(curr.getPackageFragment().getElementName())) {
					return curr;
				}
			}
		}		
		int flags= (JavaElementLabelProvider.SHOW_DEFAULT | JavaElementLabelProvider.SHOW_CONTAINER_QUALIFICATION);
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(getShell(), new JavaElementLabelProvider(flags));
		dialog.setTitle(JavaEditorMessages.getString("AddImportOnSelection.dialog.title")); //$NON-NLS-1$
		dialog.setMessage(JavaEditorMessages.getString("AddImportOnSelection.dialog.message")); //$NON-NLS-1$
		dialog.setElements(results);
		if (dialog.open() == dialog.OK) {
			return (IType) dialog.getFirstResult();
		}
		return null;
	}
}