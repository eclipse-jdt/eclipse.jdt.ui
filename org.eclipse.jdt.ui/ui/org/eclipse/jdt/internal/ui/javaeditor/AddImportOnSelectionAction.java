/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
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
import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ITypeNameRequestor;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchPattern;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.eclipse.ui.texteditor.IUpdate;

import org.eclipse.jdt.internal.corext.codemanipulation.AddImportsOperation;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.TypeInfo;
import org.eclipse.jdt.internal.corext.util.TypeInfoRequestor;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.util.ElementValidator;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.TypeInfoLabelProvider;

import org.eclipse.jdt.ui.IWorkingCopyManager;


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
		IWorkingCopyManager manager= JavaPlugin.getDefault().getWorkingCopyManager();				
		return manager.getWorkingCopy(fEditor.getEditorInput());
	}
	
	/*
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	public void run() {
		ICompilationUnit cu= getCompilationUnit();
		if (!ElementValidator.checkValidateEdit(cu, getShell(), JavaEditorMessages.getString("AddImportOnSelection.error.title"))) //$NON-NLS-1$
			return;
		if (!ActionUtil.isProcessable(getShell(), cu))
			return;
			
		if (cu != null) {
			ISelection s= fEditor.getSelectionProvider().getSelection();
			IDocument doc= fEditor.getDocumentProvider().getDocument(fEditor.getEditorInput());
			ITextSelection selection= (ITextSelection) s;
			
			if (doc != null) {
				try {
					int nameStart= getNameStart(doc, selection.getOffset());
					int nameEnd= getNameEnd(doc, selection.getOffset() + selection.getLength());
					int len= nameEnd - nameStart;
					
					String name= doc.get(nameStart, len).trim();
					String simpleName= Signature.getSimpleName(name);
					String containerName= Signature.getQualifier(name);
					
					
					IImportDeclaration existingImport= JavaModelUtil.findImport(cu, simpleName);
					if (existingImport != null) {
						if (!existingImport.getElementName().equals(name)) {
							getShell().getDisplay().beep();
							IStatusLineManager manager= getStatusLineManager();
							if (manager != null) {
								String message= JavaEditorMessages.getFormattedString("AddImportOnSelection.error.importclash", existingImport.getElementName()); //$NON-NLS-1$
								manager.setErrorMessage(message);
							}
						} else {
							removeQualification(doc, nameStart, containerName);
						}
						return;
					}			
					
					IJavaSearchScope searchScope= SearchEngine.createJavaSearchScope(new IJavaElement[] { cu.getJavaProject() });
					
					TypeInfo[] types= findAllTypes(simpleName, searchScope, null);
					if (types.length== 0) {
						getShell().getDisplay().beep();
						return;
					}
					
					TypeInfo chosen= selectResult(types, containerName, getShell());
					if (chosen == null) {
						return;
					}
					IType type= chosen.resolveType(searchScope);
					if (type == null) {
						JavaPlugin.logErrorMessage("AddImportOnSelectionAction: Failed to resolve TypeRef: " + chosen.toString()); //$NON-NLS-1$
						MessageDialog.openError(getShell(), JavaEditorMessages.getString("AddImportOnSelection.error.title"), JavaEditorMessages.getString("AddImportOnSelection.error.notresolved.message")); //$NON-NLS-1$ //$NON-NLS-2$
						return;
					}
					removeQualification(doc, nameStart, chosen.getTypeContainerName());
					
					CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings();
					AddImportsOperation op= new AddImportsOperation(cu, new IJavaElement[] { type }, settings, false);
					try {
						PlatformUI.getWorkbench().getProgressService().runInUI(
							PlatformUI.getWorkbench().getProgressService(),
							new WorkbenchRunnableAdapter(op, op.getScheduleRule()),
							op.getScheduleRule());
					} catch (InvocationTargetException e) {
						ExceptionHandler.handle(e, getShell(), JavaEditorMessages.getString("AddImportOnSelection.error.title"), null); //$NON-NLS-1$
					} catch (InterruptedException e) {
						// Do nothing. Operation has been canceled.
					}
					return;
				} catch (CoreException e) {
					ExceptionHandler.handle(e, getShell(), JavaEditorMessages.getString("AddImportOnSelection.error.title"), null); //$NON-NLS-1$
				} catch (BadLocationException e) {
					JavaPlugin.log(e);
					MessageDialog.openError(getShell(), JavaEditorMessages.getString("AddImportOnSelection.error.title"), e.getMessage()); //$NON-NLS-1$
				}
			}
		}		
	}
	

	private int getNameStart(IDocument doc, int pos) throws BadLocationException {
		while (pos > 0) {
			char ch= doc.getChar(pos - 1);
			if (!Character.isJavaIdentifierPart(ch) && ch != '.') {
				return pos;
			}
			pos--;
		}
		return pos;
	}
	
	private int getNameEnd(IDocument doc, int pos) throws BadLocationException {
		if (pos > 0) {
			if (Character.isWhitespace(doc.getChar(pos - 1))) {
				return pos;
			}
		}
		int len= doc.getLength();
		while (pos < len) {
			char ch= doc.getChar(pos);
			if (!Character.isJavaIdentifierPart(ch)) {
				return pos;
			}
			pos++;
		}
		return pos;
	}		
	
	private void removeQualification(IDocument doc, int nameStart, String containerName) throws BadLocationException {
		int containerLen= containerName.length();
		int docLen= doc.getLength();
		if ((containerLen > 0) && (nameStart + containerLen + 1 < docLen)) {
			for (int k= 0; k < containerLen; k++) {
				if (doc.getChar(nameStart + k) != containerName.charAt(k)) {
					return;
				}
			}
			if (doc.getChar(nameStart + containerLen) == '.') {
				doc.replace(nameStart, containerLen + 1, ""); //$NON-NLS-1$
			}
		}
	}	

	/**
	 * Finds a type by the simple name.
	 */
	private static TypeInfo[] findAllTypes(String simpleTypeName, IJavaSearchScope searchScope, IProgressMonitor monitor) throws CoreException {
		SearchEngine searchEngine= new SearchEngine();
		
		ArrayList typeRefsFound= new ArrayList(10);
		ITypeNameRequestor requestor= new TypeInfoRequestor(typeRefsFound);

		searchEngine.searchAllTypeNames(
			null, 
			simpleTypeName.toCharArray(), 
			SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE, 
			IJavaSearchConstants.TYPE, 
			searchScope, 
			requestor, 
			IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH, 
			monitor);
			
		return (TypeInfo[]) typeRefsFound.toArray(new TypeInfo[typeRefsFound.size()]);
	}
	
	private Shell getShell() {
		return fEditor.getSite().getShell();
	}
		
						
	private TypeInfo selectResult(TypeInfo[] results, String containerName, Shell shell) {
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
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(getShell(), new TypeInfoLabelProvider(TypeInfoLabelProvider.SHOW_FULLYQUALIFIED));
		dialog.setTitle(JavaEditorMessages.getString("AddImportOnSelection.dialog.title")); //$NON-NLS-1$
		dialog.setMessage(JavaEditorMessages.getString("AddImportOnSelection.dialog.message")); //$NON-NLS-1$
		dialog.setElements(results);
		if (dialog.open() == Window.OK) {
			return (TypeInfo) dialog.getFirstResult();
		}
		return null;
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
