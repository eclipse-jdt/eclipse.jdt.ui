/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.launcher;

import java.util.Iterator;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IDebugElement;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.util.JavaModelUtil;

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
			String typeName= getTypeNameToOpen(dbgElement);
			try {
				IType t= findTypeInWorkspace(typeName);
				if (t != null) {
					IEditorPart part= EditorUtility.openInEditor(t);
					if (part != null)
						EditorUtility.revealInEditor(part, t);
				}
			} catch (CoreException x) {
				JavaPlugin.log(x);
			}
		}
	}
	
	private IType findTypeInWorkspace(String typeName) throws JavaModelException {
		IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
		IJavaProject[] projects= JavaCore.create(root).getJavaProjects();
		for (int i= 0; i < projects.length; i++) {
			IType type= JavaModelUtil.findType(projects[i], typeName);
			if (type != null) {
				return type;
			}
		}
		return null;
	}
}
