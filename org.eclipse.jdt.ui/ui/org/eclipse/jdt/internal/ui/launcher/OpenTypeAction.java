/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.launcher;

import java.util.Iterator;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;

import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.WorkbenchException;

import org.eclipse.debug.core.DebugException;import org.eclipse.debug.core.model.IDebugElement;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;

/**
 * A generic example Java properties action. Subclass and provide
 * specific action and enabling code.
 */
public abstract class OpenTypeAction implements IViewActionDelegate {
	private IStructuredSelection fCurrentSelection= null;
	private StructuredViewer fViewer;

	/**
	 * @see IViewActionDelegate
	 */
	public void init(IViewPart view) {
	}

	/**
	 * @see IActionDelegate
	 */
	public void run(IAction action) {
		Iterator enum= getStructuredSelection().iterator();
		//selectionChanged has already checked for correct selection
		try {
			while (enum.hasNext()) {
				Object element= enum.next();
				doAction(element);
			}
		} catch(DebugException e) {
			JavaPlugin.log(e.getStatus());
		}
	}
	
	/**
	 * @see IActionDelegate
	 */
	public void selectionChanged(IAction action, ISelection sel) {
		if (sel instanceof IStructuredSelection) {
			fCurrentSelection= (IStructuredSelection)sel;
			Object[] elements= fCurrentSelection.toArray();
			action.setEnabled(elements.length == 1 && isEnabledFor(elements[0]));
		}
	}
	
	
	protected IStructuredSelection getStructuredSelection() {
		return fCurrentSelection;
	}
	
	protected abstract IDebugElement getDebugElement(IAdaptable element);
	protected abstract String getTypeNameToOpen(IDebugElement element) throws DebugException;
	public abstract boolean isEnabledFor(Object element);
	
	protected void doAction(Object e) throws DebugException {
		IAdaptable element= (IAdaptable) e;
		IDebugElement dbgElement= getDebugElement(element);
		if (dbgElement != null) {
			IType t= JavaLaunchUtils.locateType(getTypeNameToOpen(dbgElement));
			if (t != null) {
				//switchToJavaPerspective();
				try {
					IEditorPart part= EditorUtility.openInEditor(t);
					if (part != null)
						EditorUtility.revealInEditor(part, t);
				} catch (JavaModelException x) {
				} catch (PartInitException x) {
				}
			}
		}
	}

	
	private void switchToJavaPerspective() {
		IWorkbenchWindow window= JavaPlugin.getActiveWorkbenchWindow();
		IWorkbenchPage[] pages= window.getPages();
		for (int i= 0; i < pages.length; i++) {
			if (JavaUI.ID_PERSPECTIVE.equals(pages[i].getPerspective().getId())) {
				window.setActivePage(pages[i]);
				return;
			}
		}
		try {
			window.openPage(JavaUI.ID_PERSPECTIVE, JavaPlugin.getWorkspace().getRoot());
		} catch (WorkbenchException x) {
		}
	}
}
