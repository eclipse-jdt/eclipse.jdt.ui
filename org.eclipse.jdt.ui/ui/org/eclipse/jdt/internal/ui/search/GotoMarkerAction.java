package org.eclipse.jdt.internal.ui.search;
/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;

import org.eclipse.search.ui.ISearchResultView;
import org.eclipse.search.ui.ISearchResultViewEntry;
import org.eclipse.search.ui.SearchUI;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.IPackagesViewPart;
import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.ClassFileEditorInput;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

class GotoMarkerAction extends Action {

	public GotoMarkerAction() {
		super(JavaPlugin.getResourceString("Search.GotoMarkerAction.label"));
	}
	
	public void run() {
		ISearchResultView view= SearchUI.getSearchResultView();		
		view.getSelection();
		ISelection selection= view.getSelection();
		Object element= null;
		if (selection instanceof IStructuredSelection)
			element= ((IStructuredSelection)selection).getFirstElement();
		if (element instanceof ISearchResultViewEntry) {
			ISearchResultViewEntry entry= (ISearchResultViewEntry)element;
			show(entry.getSelectedMarker());
		}
	}
	
	public void show(IMarker marker) {
		IResource resource= marker.getResource();
		if (resource == null)
			return;
		IWorkbenchPage wbPage= JavaPlugin.getActivePage();
		IJavaElement javaElement= getJavaElement(marker);
		if (javaElement == null) {
			beep();
			return;
		}
		if (javaElement.getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
			// Goto packages view
			try {
				IViewPart view= wbPage.showView(JavaUI.ID_PACKAGES);
				if (view instanceof IPackagesViewPart)
					((IPackagesViewPart)view).selectAndReveal(javaElement);
			} catch (PartInitException ex) {
				ExceptionHandler.handle(ex, JavaPlugin.getResourceBundle(), "Search.Error.openEditor.");
			}
		}			
		else if (!isBinary(javaElement)) {
			// This is the workbench's default
			try {
				wbPage.openEditor(marker, false);
			} catch (PartInitException ex) {
				ExceptionHandler.handle(ex, JavaPlugin.getResourceBundle(), "Search.Error.openEditor.");
			}
		}
		else {
			IClassFile cf= getClassFile(javaElement);
			if (cf != null) {
				IEditorPart editor= null;
				try {		
//REMOVE ME		editor= ((org.eclipse.ui.internal.WorkbenchPage)wbPage).openEditor(new ClassFileEditorInput(cf), JavaUI.ID_CF_EDITOR, false);
//USE ME		editor= wbPage.openEditor(new ClassFileEditorInput(cf), JavaUI.ID_CF_EDITOR, false);
				editor= wbPage.openEditor(new ClassFileEditorInput(cf), JavaUI.ID_CF_EDITOR);
				} catch (PartInitException ex) {
					ExceptionHandler.handle(ex, JavaPlugin.getResourceBundle(), "Search.Error.openEditor.");
				}
				if (editor != null) {
					editor.gotoMarker(marker);
				}
			}
		}
	}
	
	private IClassFile getClassFile(IJavaElement jElement) {
		if (jElement instanceof IMember)
			return ((IMember)jElement).getClassFile();
		return null;
	}

	private IJavaElement getJavaElement(IMarker marker) {
		try {
			return JavaCore.create((String)marker.getAttribute(IJavaSearchUIConstants.ATT_JE_HANDLE_ID));
		} catch (CoreException ex) {
			ExceptionHandler.handle(ex, JavaPlugin.getResourceBundle(), "Search.Error.createJavaElement.");
			return null;
		}
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