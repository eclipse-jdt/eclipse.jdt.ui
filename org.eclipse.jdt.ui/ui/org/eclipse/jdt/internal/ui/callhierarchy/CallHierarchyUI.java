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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.OpenStrategy;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.IJavaStatusConstants;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;

import org.eclipse.jdt.internal.corext.callhierarchy.CallHierarchy;
import org.eclipse.jdt.internal.corext.callhierarchy.CallLocation;
import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;

class CallHierarchyUI {
    private static final int DEFAULT_MAX_CALL_DEPTH= 10;    
    private static final String PREF_MAX_CALL_DEPTH = "PREF_MAX_CALL_DEPTH";

    private static CallHierarchyUI fgInstance;

    private CallHierarchyUI() { }

    public static CallHierarchyUI getDefault() {
        if (fgInstance == null) {
            fgInstance = new CallHierarchyUI();
        }

        return fgInstance;
    }

    /**
     * Returns the maximum tree level allowed
     * @return int
     */
    public int getMaxCallDepth() {
        int maxCallDepth;
        
        IPreferenceStore settings = JavaPlugin.getDefault().getPreferenceStore();
        maxCallDepth = settings.getInt(PREF_MAX_CALL_DEPTH);
        if (maxCallDepth < 1 || maxCallDepth > 99) {
            maxCallDepth= DEFAULT_MAX_CALL_DEPTH;
        }

        return maxCallDepth;
    }

    public void setMaxCallDepth(int maxCallDepth) {
        IPreferenceStore settings = JavaPlugin.getDefault().getPreferenceStore();
        settings.setValue(PREF_MAX_CALL_DEPTH, maxCallDepth);
    }
    
    public void initializeDefaultBasePreferences(IPreferenceStore store) {
        store.setDefault(ICallHierarchyPreferencesConstants.PREF_USE_IMPLEMENTORS_FOR_CALLER_SEARCH,
            false);
        store.setDefault(ICallHierarchyPreferencesConstants.PREF_USE_IMPLEMENTORS_FOR_CALLEE_SEARCH,
            false);
    }

    public static void jumpToMember(IJavaElement element) {
        if (element != null) {
            try {
                IEditorPart methodEditor = EditorUtility.openInEditor(element, true);
                JavaUI.revealInEditor(methodEditor, (IJavaElement) element);
            } catch (JavaModelException e) {
                Utility.logError("Error getting underlying resource", e);
            } catch (PartInitException e) {
                Utility.logError("Error opening editor", e);
            }
        }
    }

    public static void openInEditor(Object element, Shell shell, String title) {
        CallLocation callLocation= null;
        if (element instanceof CallLocation) {
            callLocation= (CallLocation) element;
        } else if (element instanceof CallLocation) {
            callLocation= CallHierarchy.getCallLocation(element);
        }

        if (callLocation == null) {
            return;
        }

        try {
            boolean activateOnOpen = OpenStrategy.activateOnOpen();

            IEditorPart methodEditor = EditorUtility.openInEditor(callLocation.getMember(),
                    activateOnOpen);

            if (methodEditor instanceof ITextEditor) {
                ITextEditor editor = (ITextEditor) methodEditor;
                editor.selectAndReveal(callLocation.getStart(),
                    (callLocation.getEnd() - callLocation.getStart()));
            }
        } catch (JavaModelException e) {
            JavaPlugin.log(new Status(IStatus.ERROR, JavaPlugin.getPluginId(),
                    IJavaStatusConstants.INTERNAL_ERROR,
                    CallHierarchyMessages.getString(
                        "CallHierarchyUI.open_in_editor.error.message"), e)); //$NON-NLS-1$

            ErrorDialog.openError(shell, title,
                CallHierarchyMessages.getString(
                    "CallHierarchyUI.open_in_editor.error.messageProblems"), //$NON-NLS-1$
                e.getStatus());
        } catch (PartInitException x) {
            String name = callLocation.getCalledMember().getElementName();
            MessageDialog.openError(shell,
                CallHierarchyMessages.getString(
                    "CallHierarchyUI.open_in_editor.error.messageProblems"), //$NON-NLS-1$
                CallHierarchyMessages.getFormattedString(
                    "CallHierarchyUI.open_in_editor.error.messageArgs", //$NON-NLS-1$
                    new String[] { name, x.getMessage() }));
        }
    }

    /**
     * @param elem
     * @return
     */
    public static IEditorPart isOpenInEditor(Object elem) {
        IJavaElement javaElement= null;
        if (elem instanceof MethodWrapper) {
            javaElement= ((MethodWrapper) elem).getMember();
        } else if (elem instanceof CallLocation) {
            javaElement= ((CallLocation) elem).getCalledMember();
        }
        if (javaElement != null) {
            return EditorUtility.isOpenInEditor(javaElement);
        }
        return null;
    }
}
