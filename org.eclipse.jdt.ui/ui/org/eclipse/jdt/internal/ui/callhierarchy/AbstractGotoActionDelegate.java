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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.viewers.ISelection;

import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.OpenActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

/**
 * @see IEditorActionDelegate
 */
public abstract class AbstractGotoActionDelegate implements IEditorActionDelegate {
    private IEditorPart fEditor;

    public AbstractGotoActionDelegate() {}

    /**
     * @see IEditorActionDelegate#setActiveEditor
     */
    public void setActiveEditor(IAction action, IEditorPart targetEditor) {
        this.fEditor = targetEditor;
    }

    /**
     * @see IEditorActionDelegate#run
     */
    public void run(IAction action) {
        clearErrorMessage();

        if (fEditor instanceof JavaEditor) {
            try {
                IJavaElement element = SelectionConverter.codeResolve((JavaEditor) fEditor,
                        getShell(), "Title - TODO", "Message - TODO");

                if (element != null) {
                    IJavaElement[] members = internalSearchForMembers(element,
                            getProgressMonitor());

                    if (members != null) {
                        if (members.length == 0) {
                            IJavaElement foundElement = OpenActionUtil.selectJavaElement(members,
                                    getShell(), getSelectMethodTitle(),
                                    getSelectMethodMessage());

                            if (foundElement != null) {
                                OpenActionUtil.open(foundElement, true);
                            }
                        } else {
                            showInfoMessage(getNoResultsMessage());
                        }
                    }
                }
            } catch (JavaModelException e) {
                Utility.logError("Error finding/jumping to method", e);
            } catch (PartInitException e) {
                Utility.logError("Error finding/jumping to method", e);
            }
        }
    }

    private Shell getShell() {
        return JavaPlugin.getActiveWorkbenchWindow().getShell();
    }

    /**
     * @see IEditorActionDelegate#selectionChanged
     */
    public void selectionChanged(IAction action, ISelection selection) {}

    protected abstract String getEmptySelectionMessage();

    protected abstract String getNoResultsMessage();

    protected abstract String getSelectMethodMessage();

    protected abstract String getSelectMethodTitle();

    protected abstract String getTaskName();

    protected IJavaElement[] internalSearchForMembers(IJavaElement element,
        IProgressMonitor progressMonitor) {
        if (progressMonitor == null) {
            progressMonitor = new NullProgressMonitor();
        }

        progressMonitor.beginTask(getTaskName(), IProgressMonitor.UNKNOWN);

        IJavaElement[] result = null;

        try {
            result = searchForMembers(element, progressMonitor);
        } finally {
            progressMonitor.done();
        }

        return result;
    }

    protected abstract IJavaElement[] searchForMembers(IJavaElement element,
        IProgressMonitor progressMonitor);

    private IProgressMonitor getProgressMonitor() {
        return new NullProgressMonitor();

        //        return getStatusLineManager().getProgressMonitor();
    }

    private IStatusLineManager getStatusLineManager() {
        IStatusLineManager statusLineManager = fEditor.getEditorSite().getActionBars()
                                                      .getStatusLineManager();
        statusLineManager.setCancelEnabled(true);

        return statusLineManager;
    }

    private void clearErrorMessage() {
        showErrorMessage(null);
    }

    private void showErrorMessage(String msg) {
        getStatusLineManager().setErrorMessage(msg);
    }

    private void showInfoMessage(String msg) {
        getStatusLineManager().setMessage(msg);
        clearErrorMessage();
    }
}
