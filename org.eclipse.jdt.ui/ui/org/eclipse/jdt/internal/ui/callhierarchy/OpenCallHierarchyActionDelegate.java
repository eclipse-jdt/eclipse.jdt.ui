/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *   Jesper Kamstrup Linnet (eclipse@kamstrup-linnet.dk) - initial API and implementation 
 * 			(report 36180: Callers/Callees view)
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.callhierarchy;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

/**
 * @see IEditorActionDelegate
 */
public class OpenCallHierarchyActionDelegate implements IEditorActionDelegate,
    IObjectActionDelegate {
    private IMethod fSelectedMethod;
    private IEditorPart fEditor;
    private IWorkbenchPartSite fSite;

    /**
     * The constructor.
     */
    public OpenCallHierarchyActionDelegate() {}

    /**
     * @see IEditorActionDelegate#setActiveEditor
     */
    public void setActiveEditor(IAction action, IEditorPart targetEditor) {
        fEditor= targetEditor;
        fSite= targetEditor != null ? targetEditor.getSite() : null;
        fSelectedMethod= null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IObjectActionDelegate#setActivePart(org.eclipse.jface.action.IAction, org.eclipse.ui.IWorkbenchPart)
     */
    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
        fEditor= null;
        fSite= targetPart != null ? targetPart.getSite() : null;
        fSelectedMethod= null;
    }

    /**
     * @see IEditorActionDelegate#run
     */
    public void run(IAction action) {
        CallHierarchyViewPart callersView = findAndShowCallersView();

        if (callersView != null) {
            try {
                IJavaElement element = null;

                if (fSelectedMethod != null) {
                    element = fSelectedMethod;
                } else {
                    element = SelectionConverter.codeResolve((JavaEditor) fEditor,
                            getShell(), CallHierarchyMessages.getString("OpenCallHierarchyActionDelegate.error.title"), //$NON-NLS-1$
                            CallHierarchyMessages.getString("OpenCallHierarchyActionDelegate.error.message")); //$NON-NLS-1$

                    if (element == null || element.getElementType() != IJavaElement.METHOD) {
                        element = SelectionConverter.getElementAtOffset((JavaEditor) fEditor);
                    }
                }

                if ((element != null) &&
                            (element.getElementType() == IJavaElement.METHOD)) {
                    callersView.setMethod((IMethod) element);
                }
            } catch (JavaModelException e) {
                JavaPlugin.log(e);
            }
        }
    }

    private Shell getShell() {
        return JavaPlugin.getActiveWorkbenchWindow().getShell();
    }

    /* (non-Javadoc)
     * @see IEditorActionDelegate#selectionChanged
     */
    public void selectionChanged(IAction action, ISelection selection) {
        fSelectedMethod = null;

        if ((selection != null) && selection instanceof IStructuredSelection) {
            Object o = ((IStructuredSelection) selection).getFirstElement();

            if (o instanceof IMethod) {
                IMethod method = (IMethod) o;

                fSelectedMethod = method;
            }
        }
    }

    private CallHierarchyViewPart findAndShowCallersView() {
        if (fSite != null) {
            return CallHierarchyViewPart.findAndShowCallersView(fSite);
        }

        return null;
    }
}
