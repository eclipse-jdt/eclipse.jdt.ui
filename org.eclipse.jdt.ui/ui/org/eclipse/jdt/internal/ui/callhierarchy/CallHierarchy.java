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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.OpenStrategy;
import org.eclipse.jface.util.PropertyChangeEvent;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.IJavaStatusConstants;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;

import org.eclipse.jdt.internal.corext.callhierarchy.Implementors;

public class CallHierarchy implements IPropertyChangeListener {
    private static CallHierarchy fInstance;
    private IJavaSearchScope fSearchScope;
    private int fMaxCallDepth = -1;

    private CallHierarchy() {
        JavaPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(this);
    }

    public static CallHierarchy getDefault() {
        if (fInstance == null) {
            fInstance = new CallHierarchy();
        }

        return fInstance;
    }

    /**
     * Parses the comma separated string into an array of strings
     *
     * @return list
     */
    public static String[] parseList(String listString) {
        List list = new ArrayList(10);
        StringTokenizer tokenizer = new StringTokenizer(listString, ","); //$NON-NLS-1$

        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            list.add(token);
        }

        return (String[]) list.toArray(new String[list.size()]);
    }

    /**
     * Returns filters for packages which should not be included in the search
     * results.
     * @return String[]
     */
    public String[] getIgnoreFilters() {
        IPreferenceStore settings = JavaPlugin.getDefault().getPreferenceStore();

        if (settings.getBoolean(ICallHierarchyPreferencesConstants.PREF_USE_FILTERS)) {
            String[] strings = CallHierarchy.parseList(settings.getString(
                        ICallHierarchyPreferencesConstants.PREF_ACTIVE_FILTERS_LIST));

            return strings;
        } else {
            return null;
        }
    }

    /**
     * Returns the maximum tree level allowed
     * @return int
     */
    public int getMaxCallDepth() {
        if (fMaxCallDepth == -1) {
            IPreferenceStore settings = JavaPlugin.getDefault().getPreferenceStore();

            fMaxCallDepth = settings.getInt(ICallHierarchyPreferencesConstants.PREF_MAX_CALL_DEPTH);
        }

        return fMaxCallDepth;
    }

    /**
     * @return
     */
    public IProgressMonitor getProgressMonitor() {
        //        IProgressMonitor progressMonitor = getStatusLineManager().getProgressMonitor();
        return new NullProgressMonitor();
    }

    /**
     * @return
     */
    public boolean isSearchForCalleesUsingImplementorsEnabled() {
        IPreferenceStore settings = JavaPlugin.getDefault().getPreferenceStore();

        return settings.getBoolean(ICallHierarchyPreferencesConstants.PREF_USE_IMPLEMENTORS_FOR_CALLEE_SEARCH);
    }

    /**
     * @return
     */
    public boolean isSearchForCallersUsingImplementorsEnabled() {
        IPreferenceStore settings = JavaPlugin.getDefault().getPreferenceStore();

        return settings.getBoolean(ICallHierarchyPreferencesConstants.PREF_USE_IMPLEMENTORS_FOR_CALLER_SEARCH);
    }

    public void setSearchScope(IJavaSearchScope searchScope) {
        this.fSearchScope = searchScope;
    }

    public IJavaSearchScope getSearchScope() {
        if (fSearchScope != null) {
            return fSearchScope;
        }

        Utility.logDebug("No search scope was set");

        return SearchEngine.createWorkspaceScope();
    }

    public void initializeDefaultBasePreferences(IPreferenceStore store) {
        store.setDefault(ICallHierarchyPreferencesConstants.PREF_MAX_CALL_DEPTH, 10);
        store.setDefault(ICallHierarchyPreferencesConstants.PREF_USE_IMPLEMENTORS_FOR_CALLER_SEARCH,
            false);
        store.setDefault(ICallHierarchyPreferencesConstants.PREF_USE_IMPLEMENTORS_FOR_CALLEE_SEARCH,
            false);
    }

    public void initializeDefaultFilterPreferences(IPreferenceStore store) {
        store.setDefault(ICallHierarchyPreferencesConstants.PREF_ACTIVE_FILTERS_LIST,
            "javax.*,java.*");
        store.setDefault(ICallHierarchyPreferencesConstants.PREF_INACTIVE_FILTERS_LIST,
            "com.ibm.*,com.sun.*,org.omg.*,sun.*,sunw.*"); //$NON-NLS-1$
        store.setDefault(ICallHierarchyPreferencesConstants.PREF_USE_FILTERS, true);
    }

    public void initializeDefaultPreferences(IPreferenceStore store) {
        initializeDefaultBasePreferences(store);
        initializeDefaultFilterPreferences(store);
    }

    /**
     * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(PropertyChangeEvent)
     */
    public void propertyChange(PropertyChangeEvent event) {
        if (event.getProperty().equals(ICallHierarchyPreferencesConstants.PREF_MAX_CALL_DEPTH)) {
            fMaxCallDepth = ((Integer) event.getNewValue()).intValue();
        }
    }

    /**
     * This method removes this class as a property change listener on the plugin's preference
     * store.
     *
     * TODO: Does this class ever receive shutdown messages?
     */
    public void shutdown() {
        JavaPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(this);
    }

    private IStatusLineManager getStatusLineManager() {
        IStatusLineManager statusLineManager = PlatformUI.getWorkbench()
                                                         .getActiveWorkbenchWindow()
                                                         .getActivePage()
                                                         .findView(CallHierarchyViewPart.CALLERS_VIEW_ID)
                                                         .getViewSite().getActionBars()
                                                         .getStatusLineManager();
        statusLineManager.setCancelEnabled(true);

        return statusLineManager;
    }

    /**
     * @param method
     * @return
     */
    public Collection getInterfaceMethods(IMethod method) {
        if (isSearchForCallersUsingImplementorsEnabled()) {
            IJavaElement[] result = Implementors.getInstance().searchForInterfaces(new IJavaElement[] {
                        method
                    }, new NullProgressMonitor());
    
            if ((result != null) && (result.length > 0)) {
                return Arrays.asList(result);
            }
        }
    
        return new ArrayList(0);
    }

    /**
     * @param method
     * @return
     */
    public Collection getImplementingMethods(IMethod method) {
        if (isSearchForCalleesUsingImplementorsEnabled()) {
            IJavaElement[] result = Implementors.getInstance().searchForImplementors(new IJavaElement[] {
                        method
                    }, new NullProgressMonitor());
    
            if ((result != null) && (result.length > 0)) {
                return Arrays.asList(result);
            }
        }
    
        return new ArrayList(0);
    }

    public MethodWrapper getCallerRoot(IMethod method) {
        return new CallerMethodWrapper(null, new MethodCall(method));
    }

    public MethodWrapper getCalleeRoot(IMethod method) {
        return new CalleeMethodWrapper(null, new MethodCall(method));
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

    static CallLocation getCallLocation(Object element) {
        CallLocation callLocation = null;

        if (element instanceof MethodWrapper) {
            MethodWrapper methodWrapper = (MethodWrapper) element;
            MethodCall methodCall = methodWrapper.getMethodCall();

            if (methodCall != null) {
                callLocation = methodCall.getFirstCallLocation();
            }
        } else if (element instanceof CallLocation) {
            callLocation = (CallLocation) element;
        }

        return callLocation;
    }

    public static void openInEditor(Object element, Shell shell, String title) {
        CallLocation callLocation= null;
        if (element instanceof CallLocation) {
            callLocation= (CallLocation) element;
        } else if (element instanceof CallLocation) {
            callLocation= getCallLocation(element);
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
                        "CallHierarchy.open_in_editor.error.message"), e)); //$NON-NLS-1$

            ErrorDialog.openError(shell, title,
                CallHierarchyMessages.getString(
                    "CallHierarchy.open_in_editor.error.messageProblems"), //$NON-NLS-1$
                e.getStatus());
        } catch (PartInitException x) {
            String name = callLocation.getCalledMember().getElementName();
            MessageDialog.openError(shell,
                CallHierarchyMessages.getString(
                    "CallHierarchy.open_in_editor.error.messageProblems"), //$NON-NLS-1$
                CallHierarchyMessages.getFormattedString(
                    "CallHierarchy.open_in_editor.error.messageArgs", //$NON-NLS-1$
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
