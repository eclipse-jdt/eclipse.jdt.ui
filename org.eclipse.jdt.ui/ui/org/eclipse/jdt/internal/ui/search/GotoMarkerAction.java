/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.Action;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.part.FileEditorInput;

import org.eclipse.search.ui.ISearchResultView;
import org.eclipse.search.ui.ISearchResultViewEntry;
import org.eclipse.search.ui.SearchUI;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.IPackagesViewPart;
import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.InternalClassFileEditorInput;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.SelectionUtil;

public class GotoMarkerAction extends Action {

	private IEditorPart fEditor;
	
	public GotoMarkerAction(){
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.GOTO_MARKER_ACTION);		
	}
	
	public void run() {
		ISearchResultView view= SearchUI.getSearchResultView();		
		Object element= SelectionUtil.getSingleElement(view.getSelection());
		if (element instanceof ISearchResultViewEntry) {
			ISearchResultViewEntry entry= (ISearchResultViewEntry)element;
			show(entry.getSelectedMarker());
		}
	}

	private void show(IMarker marker) {
		IResource resource= marker.getResource();
		if (resource == null || !resource.exists())
			return;
		IWorkbenchPage wbPage= JavaPlugin.getActivePage();
		IJavaElement javaElement= SearchUtil.getJavaElement(marker);
//		if (javaElement == null) {
//			beep();
//			return;
//		}

		if (javaElement != null && javaElement.getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
			// Goto packages view
			try {
				IViewPart view= wbPage.showView(JavaUI.ID_PACKAGES);
				if (view instanceof IPackagesViewPart)
					((IPackagesViewPart)view).selectAndReveal(javaElement);
			} catch (PartInitException ex) {
				ExceptionHandler.handle(ex, SearchMessages.getString("Search.Error.openEditor.title"), SearchMessages.getString("Search.Error.openEditor.message")); //$NON-NLS-2$ //$NON-NLS-1$
			}
		} else {
			if (SearchUI.reuseEditor())
				showWithReuse(marker, resource, javaElement, wbPage);
			else
				showWithoutReuse(marker, javaElement, wbPage);
		}
	}
	
	private void showWithoutReuse(IMarker marker, IJavaElement javaElement, IWorkbenchPage wbPage) {
		IEditorPart editor= null;
		try {
			Object objectToOpen= javaElement;
			if (objectToOpen == null)
				objectToOpen= marker.getResource();
			editor= EditorUtility.openInEditor(objectToOpen, false);
		} catch (PartInitException ex) {
			ExceptionHandler.handle(ex, SearchMessages.getString("Search.Error.openEditor.title"), SearchMessages.getString("Search.Error.openEditor.message")); //$NON-NLS-2$ //$NON-NLS-1$
		} catch (JavaModelException ex) {
			ExceptionHandler.handle(ex, SearchMessages.getString("Search.Error.openEditor.title"), SearchMessages.getString("Search.Error.openEditor.message")); //$NON-NLS-2$ //$NON-NLS-1$
		}
		if (editor != null)
			editor.gotoMarker(marker);
	}

	private void showWithReuse(IMarker marker, IResource resource, IJavaElement javaElement, IWorkbenchPage wbPage) {
		if (javaElement == null || !isBinary(javaElement)) {
			if (resource instanceof IFile)
				showInEditor(marker, wbPage, new FileEditorInput((IFile)resource), JavaUI.ID_CU_EDITOR);
		}
		else {
			IClassFile cf= getClassFile(javaElement);
			if (cf != null)
				showInEditor(marker, wbPage, new InternalClassFileEditorInput(cf), JavaUI.ID_CF_EDITOR);
		}
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
	
	private void showInEditor(IMarker marker, IWorkbenchPage page, IEditorInput input, String editorId) {
		IEditorPart editor= page.findEditor(input);
		if (editor == null) {
			if (fEditor != null && !fEditor.isDirty() && !isPinned(fEditor))
				page.closeEditor(fEditor, false);
				try {
					editor= page.openEditor(input, editorId, false);
				} catch (PartInitException ex) {
					ExceptionHandler.handle(ex, SearchMessages.getString("Search.Error.openEditor.title"), SearchMessages.getString("Search.Error.openEditor.message")); //$NON-NLS-2$ //$NON-NLS-1$
					return;
				}
		} else {
			page.bringToTop(editor);
		}
		if (editor != null) {
			editor.gotoMarker(marker);
			fEditor = editor;
		}
	}

	private void gotoPackagesView(IJavaElement javaElement, IWorkbenchPage wbPage) {
		try {
			IViewPart view= wbPage.showView(JavaUI.ID_PACKAGES);
			if (view instanceof IPackagesViewPart)
				((IPackagesViewPart)view).selectAndReveal(javaElement);
		} catch (PartInitException ex) {
			ExceptionHandler.handle(ex, SearchMessages.getString("Search.Error.openEditor.title"), SearchMessages.getString("Search.Error.openEditor.message")); //$NON-NLS-2$ //$NON-NLS-1$
		}
	}
	
	private IClassFile getClassFile(IJavaElement jElement) {
		if (jElement instanceof IMember)
			return ((IMember)jElement).getClassFile();
		return null;
	}

	private boolean isBinary(IJavaElement jElement) {
		if (jElement instanceof IMember)
			return ((IMember)jElement).isBinary();
		return false;
	}

	private void beep() {
		Shell shell= JavaPlugin.getActiveWorkbenchShell();
		if (shell != null && shell.getDisplay() != null)
			shell.getDisplay().beep();
	}	
}