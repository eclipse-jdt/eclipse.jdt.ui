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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;

class CallSearchResultCollector {
    private Map fCalledMembers;
    private String[] fPackageNames = null;

    public CallSearchResultCollector() {
        this.fCalledMembers = createCalledMethodsData();

        initializePackageFilters();
    }

    public Map getCallers() {
        return fCalledMembers;
    }

    protected void addMember(IMember member, IMember calledMember, int start, int end) {
        if ((member != null) && (calledMember != null)) {
            if (!isIgnored(calledMember)) {
                MethodCall methodCall = (MethodCall) fCalledMembers.get(calledMember.getHandleIdentifier());

                if (methodCall == null) {
                    methodCall = new MethodCall(calledMember);
                    fCalledMembers.put(calledMember.getHandleIdentifier(), methodCall);
                }

                methodCall.addCallLocation(new CallLocation(member, calledMember, start,
                        end));
            }
        }
    }

    protected Map createCalledMethodsData() {
        return new HashMap();
    }

    /**
     * Method isIgnored.
     * @param enclosingElement
     * @return boolean
     */
    private boolean isIgnored(IMember enclosingElement) {
        if ((fPackageNames != null) && (fPackageNames.length > 0)) {
            String fullyQualifiedName = getTypeOfElement(enclosingElement)
                                         .getFullyQualifiedName();

            for (int i = 0; i < fPackageNames.length; i++) {
                if (matchPackage(fullyQualifiedName, fPackageNames[i])) {
                    return true;
                }
            }
        }

        return false;
    }

    private IType getTypeOfElement(IMember element) {
        if (element.getElementType() == IJavaElement.TYPE) {
            return (IType) element;
        }
        return element.getDeclaringType();
    }

    /**
     * Method getPackageNames.
     * @param strings
     * @return String[]
     */
    private String[] getPackageNames(String[] filters) {
        if (filters != null) {
            for (int i = 0; i < filters.length; i++) {
                if (filters[i].endsWith(".*")) {
                    filters[i] = filters[i].substring(0, filters[i].length() - 2);
                }
            }
        }

        return filters;
    }

    private void initializePackageFilters() {
        String[] filters = CallHierarchy.getDefault().getIgnoreFilters();
        fPackageNames = getPackageNames(filters);
    }

    private boolean matchPackage(String fullyQualifiedName, String filter) {
        return fullyQualifiedName.startsWith(filter);
    }
}
