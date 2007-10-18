/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Jesper Kamstrup Linnet (eclipse@kamstrup-linnet.dk) - initial API and implementation
 *          (report 36180: Callers/Callees view)
 *   Stephan Herrmann (stephan@cs.tu-berlin.de):
 *          - bug 75800: [call hierarchy] should allow searches for fields
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.callhierarchy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.StringMatcher;

public class CallHierarchy {
    private static final String PREF_USE_IMPLEMENTORS= "PREF_USE_IMPLEMENTORS"; //$NON-NLS-1$
    private static final String PREF_USE_FILTERS = "PREF_USE_FILTERS"; //$NON-NLS-1$
    private static final String PREF_FILTERS_LIST = "PREF_FILTERS_LIST"; //$NON-NLS-1$

    private static final String DEFAULT_IGNORE_FILTERS = "java.*,javax.*"; //$NON-NLS-1$
    private static CallHierarchy fgInstance;
    private IJavaSearchScope fSearchScope;
    private StringMatcher[] fFilters;

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

    public Collection getImplementingMethods(IMethod method) {
        if (isSearchUsingImplementorsEnabled()) {
            IJavaElement[] result = Implementors.getInstance().searchForImplementors(new IJavaElement[] {
                        method
                    }, new NullProgressMonitor());

            if ((result != null) && (result.length > 0)) {
                return Arrays.asList(result);
            }
        }

        return new ArrayList(0);
    }

    public Collection getInterfaceMethods(IMethod method) {
        if (isSearchUsingImplementorsEnabled()) {
            IJavaElement[] result = Implementors.getInstance().searchForInterfaces(new IJavaElement[] {
                        method
                    }, new NullProgressMonitor());

            if ((result != null) && (result.length > 0)) {
                return Arrays.asList(result);
            }
        }

        return new ArrayList(0);
    }

    public MethodWrapper getCallerRoot(IMember member) {
        return new CallerMethodWrapper(null, new MethodCall(member));
    }

    public MethodWrapper getCalleeRoot(IMember member) {
        return new CalleeMethodWrapper(null, new MethodCall(member));
    }

    public static CallLocation getCallLocation(Object element) {
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

    public IJavaSearchScope getSearchScope() {
        if (fSearchScope == null) {
            fSearchScope= SearchEngine.createWorkspaceScope();
        }

        return fSearchScope;
    }

    public void setSearchScope(IJavaSearchScope searchScope) {
        this.fSearchScope = searchScope;
    }

    /**
     * Checks whether the fully qualified name is ignored by the set filters.
     *
     * @param fullyQualifiedName
     *
     * @return True if the fully qualified name is ignored.
     */
    public boolean isIgnored(String fullyQualifiedName) {
        if ((getIgnoreFilters() != null) && (getIgnoreFilters().length > 0)) {
            for (int i = 0; i < getIgnoreFilters().length; i++) {
                String fullyQualifiedName1 = fullyQualifiedName;

                if (getIgnoreFilters()[i].match(fullyQualifiedName1)) {
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
        fFilters = null;

        IPreferenceStore settings = JavaPlugin.getDefault().getPreferenceStore();
        settings.setValue(PREF_FILTERS_LIST, filters);
    }

    /**
     * Returns filters for packages which should not be included in the search results.
     *
     * @return StringMatcher[]
     */
    private StringMatcher[] getIgnoreFilters() {
        if (fFilters == null) {
            String filterString = null;

            if (isFilterEnabled()) {
                filterString = getFilters();

                if (filterString == null) {
                    filterString = DEFAULT_IGNORE_FILTERS;
                }
            }

            if (filterString != null) {
                fFilters = parseList(filterString);
            } else {
                fFilters = null;
            }
        }

        return fFilters;
    }

    /**
     * Parses the comma separated string into an array of StringMatcher objects
     * @param listString the string to parse
     *
     * @return list
     */
    private static StringMatcher[] parseList(String listString) {
        List list = new ArrayList(10);
        StringTokenizer tokenizer = new StringTokenizer(listString, ","); //$NON-NLS-1$

        while (tokenizer.hasMoreTokens()) {
            String textFilter = tokenizer.nextToken().trim();
            list.add(new StringMatcher(textFilter, false, false));
        }

        return (StringMatcher[]) list.toArray(new StringMatcher[list.size()]);
    }
    
    static CompilationUnit getCompilationUnitNode(IMember member, boolean resolveBindings) {
    	ITypeRoot typeRoot= member.getTypeRoot();
        try {
	    	if (typeRoot.exists() && typeRoot.getBuffer() != null) {
				ASTParser parser= ASTParser.newParser(AST.JLS3);
				parser.setSource(typeRoot);
				parser.setResolveBindings(resolveBindings);
				return (CompilationUnit) parser.createAST(null);
	    	}
        } catch (JavaModelException e) {
            JavaPlugin.log(e);
        }
        return null;
    }
    
    public static boolean isPossibleParent(Object element) {
    	return element instanceof IMethod || element instanceof IField;
    }
}
