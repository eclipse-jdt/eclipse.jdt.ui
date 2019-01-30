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
 *   Red Hat Inc. - refactored to jdt.core.manipulation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.callhierarchy;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

class CallSearchResultCollector {
    /**
     * A map from handle identifier ({@link String}) to {@link MethodCall}.
     */
    private Map<String, MethodCall> fCalledMembers;

    public CallSearchResultCollector() {
        this.fCalledMembers = createCalledMethodsData();
    }

    /**
     * @return a map from handle identifier ({@link String}) to {@link MethodCall}
     */
    public Map<String, MethodCall> getCallers() {
        return fCalledMembers;
    }

    protected void addMember(IMember member, IMember calledMember, int start, int end) {
        addMember(member, calledMember, start, end, CallLocation.UNKNOWN_LINE_NUMBER);
    }

    protected void addMember(IMember member, IMember calledMember, int start, int end, int lineNumber) {
        if ((member != null) && (calledMember != null)) {
            if (!isIgnored(calledMember)) {
                MethodCall methodCall = fCalledMembers.get(calledMember.getHandleIdentifier());

                if (methodCall == null) {
                    methodCall = new MethodCall(calledMember);
                    fCalledMembers.put(calledMember.getHandleIdentifier(), methodCall);
                }

                methodCall.addCallLocation(new CallLocation(member, calledMember, start,
                        end, lineNumber));
            }
        }
    }

    protected Map<String, MethodCall> createCalledMethodsData() {
        return new HashMap<>();
    }

    /**
     * Method isIgnored.
     * @param enclosingElement
     * @return boolean
     */
    private boolean isIgnored(IMember enclosingElement) {
        String fullyQualifiedName = getTypeOfElement(enclosingElement)
                                        .getFullyQualifiedName();

		if (CallHierarchyCore.getDefault().isFilterTestCode()) {
			IClasspathEntry classpathEntry= determineClassPathEntry(enclosingElement);
			if (classpathEntry != null && classpathEntry.isTest()) {
				return true;
			}
		}

        return CallHierarchyCore.getDefault().isIgnored(fullyQualifiedName);
    }

	private static IClasspathEntry determineClassPathEntry(Object element) {
		if (element instanceof IJavaElement) {
			IPackageFragmentRoot packageFragmentRoot= (IPackageFragmentRoot) ((IJavaElement) element).getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
			if (packageFragmentRoot != null) {
				try {
					return packageFragmentRoot.getResolvedClasspathEntry();
				} catch (JavaModelException e) {
					return null;
				}
			}
		}
		return null;
	}

    private IType getTypeOfElement(IMember element) {
        if (element.getElementType() == IJavaElement.TYPE) {
            return (IType) element;
        }

        return element.getDeclaringType();
    }
}
