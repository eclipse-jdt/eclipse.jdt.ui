/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
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
 *   Red Hat Inc. - copied and modified from CallHierarchyCore
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.callhierarchy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IModuleDescription;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.manipulation.JavaManipulation;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;
import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;

import org.eclipse.jdt.internal.ui.util.StringMatcher;

public class CallHierarchyCore {

    private static final String PREF_USE_IMPLEMENTORS= "PREF_USE_IMPLEMENTORS"; //$NON-NLS-1$
    private static final String PREF_USE_FILTERS= "PREF_USE_FILTERS"; //$NON-NLS-1$
    private static final String PREF_FILTERS_LIST= "PREF_FILTERS_LIST"; //$NON-NLS-1$
    private static final String PREF_FILTER_TESTCODE= "PREF_FILTER_TESTCODE"; //$NON-NLS-1$

    private String defaultIgnoreFilters= "java.*,javax.*"; //$NON-NLS-1$

    private static CallHierarchyCore fgInstance;
    private IJavaSearchScope fSearchScope;
    private StringMatcher[] fFilters;

    public static CallHierarchyCore getDefault() {
        if (fgInstance == null) {
            fgInstance= new CallHierarchyCore();
        }

        return fgInstance;
    }

    public boolean isSearchUsingImplementorsEnabled() {
        return Boolean.parseBoolean(JavaManipulation.getPreference(PREF_USE_IMPLEMENTORS, null));
    }

    public boolean isFilterTestCode() {
        return Boolean.parseBoolean(JavaManipulation.getPreference(PREF_FILTER_TESTCODE, null));
    }

    public Collection<IJavaElement> getImplementingMethods(IMethod method) {
        if (isSearchUsingImplementorsEnabled()) {
            IJavaElement[] result= Implementors.getInstance().searchForImplementors(new IJavaElement[] {
                        method
                    }, new NullProgressMonitor());

            if ((result != null) && (result.length > 0)) {
                return Arrays.asList(result);
            }
        }

        return new ArrayList<>(0);
    }

    public Collection<IJavaElement> getInterfaceMethods(IMethod method) {
        if (isSearchUsingImplementorsEnabled()) {
            IJavaElement[] result= Implementors.getInstance().searchForInterfaces(new IJavaElement[] {
                        method
                    }, new NullProgressMonitor());

            if ((result != null) && (result.length > 0)) {
                return Arrays.asList(result);
            }
        }

        return new ArrayList<>(0);
    }

    public MethodWrapper[] getCallerRoots(IMember[] members) {
        return getRoots(members, true);
    }

    public MethodWrapper[] getCalleeRoots(IMember[] members) {
        return getRoots(members, false);
    }

	private MethodWrapper[] getRoots(IMember[] members, boolean callers) {
		ArrayList<MethodWrapper> roots= new ArrayList<>();
		for (IMember member : members) {
			if (member instanceof IType) {
				IType type= (IType) member;
				try {
					if (! type.isAnonymous()) {
						IMethod[] constructors= JavaElementUtil.getAllConstructors(type);
						if (constructors.length == 0) {
							addRoot(member, roots, callers); // IType is a stand-in for the non-existing default constructor
						} else {
							for (IMethod constructor : constructors) {
								addRoot(constructor, roots, callers);
							}
						}
					} else {
						addRoot(member, roots, callers);
					}
				} catch (JavaModelException e) {
					JavaManipulationPlugin.log(e);
				}
			} else {
				addRoot(member, roots, callers);
			}
		}
        return roots.toArray(new MethodWrapper[roots.size()]);
	}

	private void addRoot(IMember member, ArrayList<MethodWrapper> roots, boolean callers) {
		MethodCall methodCall= new MethodCall(member);
		MethodWrapper root;
		if (callers) {
			root= new CallerMethodWrapper(null, methodCall);
		} else {
			root= new CalleeMethodWrapper(null, methodCall);
		}
		roots.add(root);
	}

    public static CallLocation getCallLocation(Object element) {
        CallLocation callLocation= null;

        if (element instanceof MethodWrapper) {
            MethodWrapper methodWrapper= (MethodWrapper) element;
            MethodCall methodCall= methodWrapper.getMethodCall();

            if (methodCall != null) {
                callLocation= methodCall.getFirstCallLocation();
            }
        } else if (element instanceof CallLocation) {
            callLocation= (CallLocation) element;
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
        this.fSearchScope= searchScope;
    }

	/**
	 * Checks whether the fully qualified name is ignored by the set filters.
	 *
	 * @param fullyQualifiedName the fully qualified name
	 *
	 * @return <code>true</code> if the fully qualified name is ignored
	 */
    public boolean isIgnored(String fullyQualifiedName) {
        if ((getIgnoreFilters() != null) && (getIgnoreFilters().length > 0)) {
        	for (StringMatcher ignoreFilter : getIgnoreFilters()) {
        		String fullyQualifiedName1= fullyQualifiedName;
        		if (ignoreFilter.match(fullyQualifiedName1)) {
        			return true;
        		}
        	}
        }

        return false;
    }

    public boolean isFilterEnabled() {
        return Boolean.parseBoolean(JavaManipulation.getPreference(PREF_USE_FILTERS, null));
    }

    /**
     * Returns the current filters as a string.
     * @return returns the filters
     */
    public String getFilters() {
        String pref= JavaManipulation.getPreference(PREF_FILTERS_LIST, null);
        if (pref == null)
        	return ""; //$NON-NLS-1$
        return pref;
    }

    /**
     * Set default ignore filters to use.
     *
     * @param defaultIgnoreFilters comma-separated filter string
     */
    public void setDefaultIgnoreFilters(String defaultIgnoreFilters) {
    	this.defaultIgnoreFilters= defaultIgnoreFilters;
    }

    /**
     * Reset filters variable to null.
     */
    public void resetFilters() {
    	fFilters= null;
    }

    /**
     * Returns filters for packages which should not be included in the search results.
     *
     * @return StringMatcher[]
     */
    public StringMatcher[] getIgnoreFilters() {
        if (fFilters == null) {
            String filterString= null;

            if (isFilterEnabled()) {
                filterString= getFilters();

                if (filterString.isEmpty()) {
                    filterString= defaultIgnoreFilters;
                }
            }

            if (filterString != null) {
                fFilters= parseList(filterString);
            } else {
                fFilters= null;
            }
        }

        return fFilters;
    }

    public static boolean arePossibleInputElements(List<?> elements) {
		if (elements.size() < 1)
			return false;
		for (Object name : elements) {
			if (! isPossibleInputElement(name))
				return false;
		}
		return true;
	}

	/**
	 * Parses the comma separated string into an array of {@link StringMatcher} objects.
	 *
	 * @param listString the string to parse
	 * @return an array of {@link StringMatcher} objects
	 */
    private static StringMatcher[] parseList(String listString) {
        List<StringMatcher> list= new ArrayList<>(10);
        StringTokenizer tokenizer= new StringTokenizer(listString, ","); //$NON-NLS-1$

        while (tokenizer.hasMoreTokens()) {
            String textFilter= tokenizer.nextToken().trim();
            list.add(new StringMatcher(textFilter, false, false));
        }

        return list.toArray(new StringMatcher[list.size()]);
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
            JavaManipulationPlugin.log(e);
        }
        return null;
    }

    public static boolean isPossibleInputElement(Object element){
        if (! (element instanceof IMember))
            return false;

		if (element instanceof IModuleDescription) {
			return false;
		}

        if (element instanceof IType) {
			IType type= (IType) element;
			try {
				return type.isClass() || type.isEnum();
			} catch (JavaModelException e) {
				return false;
			}
		}

        return true;
    }
}
