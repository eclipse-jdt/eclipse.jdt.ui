/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.search.ui.text.Match;

import org.eclipse.core.resources.IFile;

import org.eclipse.jface.dialogs.MessageDialog;

import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IReusableEditor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.InternalClassFileEditorInput;

public class JavaSearchEditorOpener {

	private static class ReusedEditorWatcher implements IPartListener {
		
		private IEditorPart fReusedEditor;
		private IPartService fPartService;
		
		public ReusedEditorWatcher() {
			fReusedEditor= null;
			fPartService= null;
		}
		
		public IEditorPart getReusedEditor() {
			return fReusedEditor;
		}
		
		public void initialize(IEditorPart editor) {
			if (fReusedEditor != null) {
				fPartService.removePartListener(this);
			}
			fReusedEditor= editor;
			if (editor != null) {
				fPartService= editor.getSite().getWorkbenchWindow().getPartService();
				fPartService.addPartListener(this);
			} else {
				fPartService= null;
			}
		}
		
		public void partOpened(IWorkbenchPart part) {
		}

		public void partDeactivated(IWorkbenchPart part) {
		}

		public void partClosed(IWorkbenchPart part) {
			if (part == fReusedEditor) {
				initialize(null);
			}
		}

		public void partActivated(IWorkbenchPart part) {
		}

		public void partBroughtToTop(IWorkbenchPart part) {
		}
	}
	
	private ReusedEditorWatcher fReusedEditorWatcher;


	public IEditorPart openElement(Object element) throws PartInitException, JavaModelException {
		IWorkbenchPage wbPage= JavaPlugin.getActivePage();
		if (NewSearchUI.reuseEditor())
			return showWithReuse(element, wbPage);
		else
			return showWithoutReuse(element, wbPage);
	}
		
	public IEditorPart openMatch(Match match) throws PartInitException, JavaModelException {
		Object element= getElementToOpen(match);
		return openElement(element);
	}

	protected Object getElementToOpen(Match match) {
		return match.getElement();
	}

	private IEditorPart showWithoutReuse(Object element, IWorkbenchPage wbPage) throws PartInitException, JavaModelException {
		return EditorUtility.openInEditor(element, false);
	}

	private IEditorPart showWithReuse(Object element, IWorkbenchPage wbPage) throws JavaModelException, PartInitException {
		IFile file= getFile(element);
		if (file != null) {
			String editorID= getEditorID(file);
			return showInEditor(wbPage, new FileEditorInput(file), editorID);
		} else {
			IClassFile cf= getClassFile(element);
			if (cf != null)
				return showInEditor(wbPage, new InternalClassFileEditorInput(cf), JavaUI.ID_CF_EDITOR);
		}
		return null;
	}

	private IFile getFile(Object element) throws JavaModelException {
		if (element instanceof IFile)
			return (IFile) element;
		if (element instanceof IJavaElement) {
			IJavaElement jElement= (IJavaElement) element;
			ICompilationUnit cu= (ICompilationUnit) jElement.getAncestor(IJavaElement.COMPILATION_UNIT);
			if (cu != null) {
				return (IFile) cu.getCorrespondingResource();
			}
			IClassFile cf= (IClassFile) jElement.getAncestor(IJavaElement.CLASS_FILE);
			if (cf != null)
				return (IFile) cf.getCorrespondingResource();
		}
		return null;
	}

	private String getEditorID(IFile file) throws PartInitException {
		IEditorDescriptor desc= IDE.getEditorDescriptor(file);
		if (desc == null)
			return JavaPlugin.getDefault().getWorkbench().getEditorRegistry().findEditor(IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID).getId();
		else
			return desc.getId();
	}

	private boolean isPinned(IEditorPart editor) {
		if (editor == null)
			return false;

		IEditorReference[] editorRefs= editor.getEditorSite().getPage().getEditorReferences();
		int i= 0;
		while (i < editorRefs.length) {
			if (editor.equals(editorRefs[i].getEditor(false)))
				return editorRefs[i].isPinned();
			i++;
		}
		return false;
	}

	private IEditorPart showInEditor(IWorkbenchPage page, IEditorInput input, String editorId) {
		IEditorPart editor= page.findEditor(input);
		if (editor != null)
			page.bringToTop(editor);
		else {
			IEditorPart reusedEditor= getReusedEditor();
			boolean isOpen= false;
			if (reusedEditor != null) {
				IEditorReference[] parts= page.getEditorReferences();
				int i= 0;
				while (!isOpen && i < parts.length)
					isOpen= reusedEditor == parts[i++].getEditor(false);
			}

			boolean canBeReused= isOpen && !reusedEditor.isDirty() && !isPinned(reusedEditor);
			boolean showsSameInputType= reusedEditor != null && reusedEditor.getSite().getId().equals(editorId);
			if (canBeReused && !showsSameInputType) {
				page.closeEditor(reusedEditor, false);
				setReusedEditor(null);
			}

			if (canBeReused && showsSameInputType) {
				((IReusableEditor) reusedEditor).setInput(input);
				page.bringToTop(reusedEditor);
				editor= reusedEditor;
			} else {
				try {
					editor= page.openEditor(input, editorId, false);
					if (editor instanceof IReusableEditor)
						setReusedEditor(editor);
					else
						setReusedEditor(null);
				} catch (PartInitException ex) {
					MessageDialog.openError(JavaPlugin.getActiveWorkbenchShell(), SearchMessages.Search_Error_openEditor_title, SearchMessages.Search_Error_openEditor_message); 
					return null;
				}
			}
		}
		return editor;
	}

	private IEditorPart getReusedEditor() {
		if (fReusedEditorWatcher != null) 
			return fReusedEditorWatcher.getReusedEditor();
		return null;
	}
	
	private void setReusedEditor(IEditorPart editor) {
		if (fReusedEditorWatcher == null) {
			fReusedEditorWatcher= new ReusedEditorWatcher();
		}
		fReusedEditorWatcher.initialize(editor);
	}

	private IClassFile getClassFile(Object element) {
		if (!(element instanceof IJavaElement))
			return null;
		if (element instanceof IClassFile)
			return (IClassFile) element;
		IJavaElement jElement= (IJavaElement) element;
		if (jElement instanceof IMember)
			return ((IMember) jElement).getClassFile();
		return null;
	}

}
