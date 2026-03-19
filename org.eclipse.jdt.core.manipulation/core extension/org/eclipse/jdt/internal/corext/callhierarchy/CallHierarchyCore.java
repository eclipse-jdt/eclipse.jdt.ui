/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IModuleDescription;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.manipulation.JavaManipulation;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;

import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;
import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;

import org.eclipse.jdt.internal.ui.util.StringMatcher;

public class CallHierarchyCore {

	public static final String PREF_SHOW_ALL_CODE = "PREF_SHOW_ALL_CODE";	//$NON-NLS-1$
	public static final String PREF_HIDE_TEST_CODE = "PREF_HIDE_TEST_CODE";	//$NON-NLS-1$
	public static final String PREF_SHOW_TEST_CODE_ONLY = "PREF_SHOW_TEST_CODE_ONLY";	//$NON-NLS-1$

    public static final String PREF_USE_IMPLEMENTORS= "PREF_USE_IMPLEMENTORS"; //$NON-NLS-1$
    public static final String PREF_USE_FILTERS= "PREF_USE_FILTERS"; //$NON-NLS-1$
    public static final String PREF_FILTERS_LIST= "PREF_FILTERS_LIST"; //$NON-NLS-1$
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

    public boolean isShowTestCode() {
        return Boolean.parseBoolean(JavaManipulation.getPreference(PREF_SHOW_TEST_CODE_ONLY, null));
    }

    public boolean isShowAll() {
		return Boolean.parseBoolean(JavaManipulation.getPreference(PREF_SHOW_ALL_CODE, null));
    }

	public boolean isHideTestCode() {
		return Boolean.parseBoolean(JavaManipulation.getPreference(PREF_HIDE_TEST_CODE, null));
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
							if (type.isRecord()) {
								addRoot(member, roots, callers);
							}
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
            if (typeRoot != null && typeRoot.exists() && typeRoot.getBuffer() != null
                    && JavaCore.isJavaLikeFileName(typeRoot.getElementName())) {
				ASTParser parser= ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
				parser.setSource(typeRoot);
				parser.setResolveBindings(resolveBindings);
				return (CompilationUnit) parser.createAST(null);
	        }
        } catch (JavaModelException e) {
            JavaManipulationPlugin.log(e);
        } catch (ClassCastException e) {
            // Non-standard ITypeRoot (e.g. from a contributed search participant)
            // that does not implement the internal compiler interfaces required
            // by ASTParser — fall through and return null
            JavaManipulationPlugin.log(e);
        }
        return null;
    }

    /**
     * Searches for the first {@link IMember} declaration matching the given
     * name, element type, and optional call-site constraints.
     *
     * <p>Filters are applied in order of cheapness: argument count first
     * (O(1)), then declaring type (string compare or hierarchy lookup),
     * then argument types (per-parameter type compatibility check).
     *
     * @param elementName the simple name to search for
     * @param searchFor one of {@link IJavaSearchConstants#METHOD},
     *            {@link IJavaSearchConstants#FIELD}, or
     *            {@link IJavaSearchConstants#TYPE}
     * @param scope the search scope
     * @param monitor progress monitor, may be {@code null}
     * @param expectedArgCount expected argument count, or {@code -1} to skip
     * @param receiverTypeFQN receiver type FQN to match declaring type
     *            against, or {@code null} to skip
     * @param declaringTypeCandidates FQN candidates for the declaring type,
     *            or {@code null} to skip
     * @param expectedArgTypes argument type FQNs from the call site (may
     *            contain {@code "UNKNOWN"} entries), or {@code null} to skip
     * @return the first matching member, or {@code null} if none found
     */
    static IMember findFirstDeclaration(String elementName, int searchFor,
            IJavaSearchScope scope, IProgressMonitor monitor,
            int expectedArgCount, String receiverTypeFQN,
            List<String> declaringTypeCandidates,
            String[] expectedArgTypes) {
        SearchPattern pattern= SearchPattern.createPattern(
                elementName, searchFor,
                IJavaSearchConstants.DECLARATIONS,
                SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE);
        if (pattern == null)
            return null;
        final IMember[] result= { null };
        try {
            new SearchEngine().search(pattern,
                    SearchEngine.getSearchParticipants(), scope,
                    new SearchRequestor() {
                        @Override
                        public void acceptSearchMatch(SearchMatch match) {
                            if (result[0] != null) {
                                return;
                            }
                            if (!(match.getElement() instanceof IMember m)) {
                                return;
                            }
                            if (m instanceof IMethod method
                                    && !matchesMethod(method,
                                            expectedArgCount,
                                            expectedArgTypes)) {
                                return;
                            }
                            if (m.getDeclaringType() != null) {
                                String declFQN= m.getDeclaringType()
                                        .getFullyQualifiedName();
                                if (receiverTypeFQN != null
                                        && !isTypeOrSupertype(declFQN,
                                                receiverTypeFQN, m)) {
                                    return;
                                }
                                if (declaringTypeCandidates != null
                                        && !declaringTypeCandidates.isEmpty()
                                        && !declaringTypeCandidates
                                                .contains(declFQN)) {
                                    return;
                                }
                            }
                            result[0]= m;
                            throw new OperationCanceledException();
                        }
                    }, monitor);
        } catch (OperationCanceledException e) {
            // short-circuit: first match found
        } catch (CoreException e) {
            JavaManipulationPlugin.log(e);
        }
        return result[0];
    }

    /**
     * Checks if a candidate method matches the expected argument count
     * and types from the call site.
     */
    private static boolean matchesMethod(IMethod method,
            int expectedArgCount, String[] expectedArgTypes) {
        int paramCount= method.getNumberOfParameters();
        // Argument count filter
        if (expectedArgCount >= 0
                && paramCount != expectedArgCount) {
            return false;
        }
        // Argument type filter
        if (expectedArgTypes != null
                && expectedArgTypes.length > 0
                && expectedArgTypes.length == paramCount) {
            String[] paramSigs= method.getParameterTypes();
            IType declType= method.getDeclaringType();
            for (int i= 0; i < paramCount; i++) {
                String callSiteType= expectedArgTypes[i];
                if ("UNKNOWN".equals(callSiteType)) { //$NON-NLS-1$
                    continue;
                }
                String paramFQN= resolveParameterType(
                        paramSigs[i], declType);
                if (paramFQN == null) {
                    continue;
                }
                if (!isTypeCompatible(callSiteType,
                        paramFQN, method)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Resolves a JDT parameter type signature to a fully qualified
     * name using the declaring type's context.
     */
    private static String resolveParameterType(String paramSig,
            IType declaringType) {
        if (declaringType == null) {
            return null;
        }
        try {
            String erasure= Signature.getTypeErasure(paramSig);
            String simpleName= Signature.toString(erasure);
            if (simpleName.contains(".")) { //$NON-NLS-1$
                return simpleName;
            }
            String[][] resolved= declaringType.resolveType(simpleName);
            if (resolved != null && resolved.length > 0) {
                String pkg= resolved[0][0];
                String name= resolved[0][1];
                return pkg.isEmpty() ? name : pkg + "." + name; //$NON-NLS-1$
            }
        } catch (JavaModelException e) {
            // can't resolve
        }
        return null;
    }

    /**
     * Checks if the call-site argument type is compatible with the
     * parameter type. The argument type must be the same as or a
     * subtype of the parameter type.
     */
    private static boolean isTypeCompatible(String argTypeFQN,
            String paramTypeFQN, IMember context) {
        if (argTypeFQN.equals(paramTypeFQN)) {
            return true;
        }
        // The parameter type is a supertype of the argument type
        // (e.g., param is Object, arg is String) — check if
        // argType is-a paramType
        return isTypeOrSupertype(paramTypeFQN, argTypeFQN, context);
    }

    /**
     * Checks if {@code candidateFQN} is the same as or a supertype of
     * {@code receiverFQN}. Uses the JDT type hierarchy when available.
     */
    private static boolean isTypeOrSupertype(String candidateFQN,
            String receiverFQN, IMember context) {
        if (candidateFQN.equals(receiverFQN)) {
            return true;
        }
        // Check common base types without hierarchy lookup
        if ("java.lang.Object".equals(candidateFQN)) { //$NON-NLS-1$
            return true;
        }
        // Try type hierarchy for precise check
        try {
            if (context.getJavaProject() != null) {
                IType receiverType= context.getJavaProject()
                        .findType(receiverFQN);
                if (receiverType != null) {
                    ITypeHierarchy hierarchy=
                            receiverType.newSupertypeHierarchy(null);
                    for (IType superType
                            : hierarchy.getAllSupertypes(receiverType)) {
                        if (candidateFQN.equals(
                                superType.getFullyQualifiedName())) {
                            return true;
                        }
                    }
                }
            }
        } catch (JavaModelException e) {
            JavaManipulationPlugin.log(e);
        }
        return false;
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
				return type.isClass() || type.isEnum() || type.isRecord();
			} catch (JavaModelException e) {
				return false;
			}
		}

        return true;
    }
}
