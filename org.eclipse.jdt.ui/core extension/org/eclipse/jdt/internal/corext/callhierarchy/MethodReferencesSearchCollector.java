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

import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.search.IJavaSearchResultCollector;

class MethodReferencesSearchCollector implements IJavaSearchResultCollector {
    private CallSearchResultCollector fSearchResults;
    private IProgressMonitor fProgressMonitor;
    private boolean fRequireExactMatch = true;

    MethodReferencesSearchCollector() {
        fSearchResults = new CallSearchResultCollector();
    }

    public Map getCallers() {
        return fSearchResults.getCallers();
    }

    /**
     * @see org.eclipse.jdt.core.search.IJavaSearchResultCollector#getProgressMonitor()
     */
    public IProgressMonitor getProgressMonitor() {
        return fProgressMonitor;
    }

    /**
     * @see org.eclipse.jdt.core.search.IJavaSearchResultCollector#aboutToStart()
     */
    public void aboutToStart() {}

    /**
     * @see org.eclipse.jdt.core.search.IJavaSearchResultCollector#accept(org.eclipse.core.resources.IResource, int, int, org.eclipse.jdt.core.IJavaElement, int)
     */
    public void accept(IResource resource, int start, int end,
        IJavaElement enclosingElement, int accuracy) throws CoreException {
        if (fRequireExactMatch && (accuracy != IJavaSearchResultCollector.EXACT_MATCH)) {
            return;
        }

        if (enclosingElement != null && enclosingElement instanceof IMember) {
            IMember member= (IMember) enclosingElement;
            switch (enclosingElement.getElementType()) {
                case IJavaElement.METHOD:
                case IJavaElement.TYPE:
                case IJavaElement.FIELD:
                case IJavaElement.INITIALIZER:
                    fSearchResults.addMember(member, member, start, end);
                    break;
            }
        }
    }

    /**
     * @see org.eclipse.jdt.core.search.IJavaSearchResultCollector#done()
     */
    public void done() {}

    /**
     * @param monitor
     */
    void setProgressMonitor(IProgressMonitor monitor) {
        this.fProgressMonitor = monitor;
    }
}
