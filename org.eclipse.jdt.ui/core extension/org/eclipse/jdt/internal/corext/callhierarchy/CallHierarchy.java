/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Jesper Kamstrup Linnet (eclipse@kamstrup-linnet.dk) - initial API and implementation
 *          (report 36180: Callers/Callees view)
 *   Stephan Herrmann (stephan@cs.tu-berlin.de):
 *          - bug 75800: [call hierarchy] should allow searches for fields
 *   Red Hat Inc. - modified to use CallHierarchyCore instance
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.callhierarchy;

import java.util.Collection;
import java.util.List;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.search.IJavaSearchScope;

import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.StringMatcher;

public class CallHierarchy {
    private static final String PREF_USE_IMPLEMENTORS= "PREF_USE_IMPLEMENTORS"; //$NON-NLS-1$
    private static final String PREF_USE_FILTERS = "PREF_USE_FILTERS"; //$NON-NLS-1$
    private static final String PREF_FILTERS_LIST = "PREF_FILTERS_LIST"; //$NON-NLS-1$
    private static final String PREF_FILTER_TESTCODE= "PREF_FILTER_TESTCODE"; //$NON-NLS-1$

    private static CallHierarchy fgInstance;
    private CallHierarchyCore fgCallHierarchyCore;

    private CallHierarchy() {
        fgCallHierarchyCore = CallHierarchyCore.getDefault();
    }

    public static CallHierarchy getDefault() {
        if (fgInstance == null) {
            fgInstance = new CallHierarchy();
        }

        return fgInstance;
    }

    public boolean isSearchUsingImplementorsEnabled() {
        IPreferenceStore settings = JavaPlugin.getDefault().getPreferenceStore();

        return settings.getBoolean(PREF_USE_IMPLEMENTORS);
    }

    public void setSearchUsingImplementorsEnabled(boolean enabled) {
        IPreferenceStore settings = JavaPlugin.getDefault().getPreferenceStore();

        settings.setValue(PREF_USE_IMPLEMENTORS, enabled);
    }

    public boolean isFilterTestCode() {
        IPreferenceStore settings = JavaPlugin.getDefault().getPreferenceStore();

        return settings.getBoolean(PREF_FILTER_TESTCODE);
    }

    public void setFilterTestCode(boolean enabled) {
        IPreferenceStore settings = JavaPlugin.getDefault().getPreferenceStore();

        settings.setValue(PREF_FILTER_TESTCODE, enabled);
    }


    public Collection<IJavaElement> getImplementingMethods(IMethod method) {
        return fgCallHierarchyCore.getImplementingMethods(method);
    }

    public Collection<IJavaElement> getInterfaceMethods(IMethod method) {
        return fgCallHierarchyCore.getInterfaceMethods(method);
    }

    public MethodWrapper[] getCallerRoots(IMember[] members) {
        return fgCallHierarchyCore.getCallerRoots(members);
    }

    public MethodWrapper[] getCalleeRoots(IMember[] members) {
        return fgCallHierarchyCore.getCalleeRoots(members);
    }

    public static CallLocation getCallLocation(Object element) {
        return CallHierarchyCore.getCallLocation(element);
    }

    public IJavaSearchScope getSearchScope() {
        return fgCallHierarchyCore.getSearchScope();
    }

    public void setSearchScope(IJavaSearchScope searchScope) {
        fgCallHierarchyCore.setSearchScope(searchScope);
    }

    /**
     * Checks whether the fully qualified name is ignored by the set filters.
     *
     * @param fullyQualifiedName the fully qualified name
     *
     * @return <code>true</code> if the fully qualified name is ignored
     */
    public boolean isIgnored(String fullyQualifiedName) {
        StringMatcher[] ignoreFilters= getIgnoreFilters();
        if (ignoreFilters != null) {
            for (StringMatcher ignoreFilter : ignoreFilters) {
                if (ignoreFilter.match(fullyQualifiedName)) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean isFilterEnabled() {
        IPreferenceStore settings = JavaPlugin.getDefault().getPreferenceStore();
        return settings.getBoolean(PREF_USE_FILTERS);
    }

    public void setFilterEnabled(boolean filterEnabled) {
        IPreferenceStore settings = JavaPlugin.getDefault().getPreferenceStore();
        settings.setValue(PREF_USE_FILTERS, filterEnabled);
    }

    /**
     * Returns the current filters as a string.
     * @return returns the filters
     */
    public String getFilters() {
        IPreferenceStore settings = JavaPlugin.getDefault().getPreferenceStore();

        return settings.getString(PREF_FILTERS_LIST);
    }

    public void setFilters(String filters) {
        fgCallHierarchyCore.resetFilters();

        IPreferenceStore settings = JavaPlugin.getDefault().getPreferenceStore();
        settings.setValue(PREF_FILTERS_LIST, filters);
    }

    /**
     * Returns filters for packages which should not be included in the search results.
     *
     * @return StringMatcher[]
     */
    private StringMatcher[] getIgnoreFilters() {
    	return fgCallHierarchyCore.getIgnoreFilters();
    }

    public static boolean arePossibleInputElements(List<?> elements) {
        return CallHierarchyCore.arePossibleInputElements(elements);
	}

	static CompilationUnit getCompilationUnitNode(IMember member, boolean resolveBindings) {
    	ITypeRoot typeRoot= member.getTypeRoot();
        try {
	    	if (typeRoot.exists() && typeRoot.getBuffer() != null) {
				ASTParser parser= ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
				parser.setSource(typeRoot);
				parser.setResolveBindings(resolveBindings);
				return (CompilationUnit) parser.createAST(null);
	    	}
        } catch (JavaModelException e) {
            JavaPlugin.log(e);
        }
        return null;
    }

    public static boolean isPossibleInputElement(Object element){
        return CallHierarchyCore.isPossibleInputElement(element);
    }
}
