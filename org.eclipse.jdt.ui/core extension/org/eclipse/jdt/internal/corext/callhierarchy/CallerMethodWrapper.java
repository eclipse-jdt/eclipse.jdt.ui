/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *   Jesper Kamstrup Linnet (eclipse@kamstrup-linnet.dk) - initial API and implementation 
 *          (report 36180: Callers/Callees view)
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.callhierarchy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

class CallerMethodWrapper extends MethodWrapper {
    public CallerMethodWrapper(MethodWrapper parent, MethodCall methodCall) {
        super(parent, methodCall);
    }

    protected IJavaSearchScope getSearchScope() {
        return CallHierarchy.getDefault().getSearchScope();
    }

    protected String getTaskName() {
        return "Finding callers...";
    }

    /* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper#createMethodWrapper(org.eclipse.jdt.internal.corext.callhierarchy.MethodCall)
	 */
	protected MethodWrapper createMethodWrapper(MethodCall methodCall) {
        return new CallerMethodWrapper(this, methodCall);
    }

	/**
     * @return The result of the search for children
	 * @see org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper#findChildren(org.eclipse.core.runtime.IProgressMonitor)
     */
    protected Map findChildren(IProgressMonitor progressMonitor) {
        try {
            MethodReferencesSearchCollector searchCollector = new MethodReferencesSearchCollector();
            SearchEngine searchEngine = new SearchEngine();

            for (Iterator iter = getMembers().iterator();
                        iter.hasNext() && !progressMonitor.isCanceled();) {
                IMember member = (IMember) iter.next();
                searchCollector.setProgressMonitor(new SubProgressMonitor(
                        progressMonitor, 10, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
                searchEngine.search(ResourcesPlugin.getWorkspace(), member,
                    IJavaSearchConstants.REFERENCES, getSearchScope(), searchCollector);
            }

            return searchCollector.getCallers();
        } catch (JavaModelException e) {
            Utility.logError("Error finding callers", e);

            return new HashMap(0);
        }
    }

    /**
     * Returns a collection of IMember instances representing what to search for 
     */
    private Collection getMembers() {
        Collection result = new ArrayList();

        result.add(getMember());
        if (getMember().getElementType() == IJavaElement.METHOD) {
            result.addAll(CallHierarchy.getDefault().getInterfaceMethods((IMethod) getMember()));
        }

        return result;
    }
}
