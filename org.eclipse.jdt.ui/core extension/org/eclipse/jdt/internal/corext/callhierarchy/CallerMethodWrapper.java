/*******************************************************************************
 * Copyright (c) 2000, orporation and others.
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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;

import org.eclipse.jdt.internal.ui.JavaPlugin;

class CallerMethodWrapper extends MethodWrapper {
    public CallerMethodWrapper(MethodWrapper parent, MethodCall methodCall) {
        super(parent, methodCall);
    }

    protected IJavaSearchScope getSearchScope() {
        return CallHierarchy.getDefault().getSearchScope();
    }

    protected String getTaskName() {
        return CallHierarchyMessages.getString("CallerMethodWrapper.taskname"); //$NON-NLS-1$
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
            MethodReferencesSearchRequestor searchRequestor = new MethodReferencesSearchRequestor();
            SearchEngine searchEngine= new SearchEngine();

            IProgressMonitor monitor= new SubProgressMonitor(progressMonitor, 95, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL);
            IJavaSearchScope searchScope= getSearchScope();
            for (Iterator iter= getMembers().iterator(); iter.hasNext();) {
                checkCanceled(progressMonitor);

                IMember member = (IMember) iter.next();
                SearchPattern pattern= SearchPattern.createPattern(member, IJavaSearchConstants.REFERENCES); 
                searchEngine.search(pattern, new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() },
                        searchScope, searchRequestor, monitor);
            }

            return searchRequestor.getCallers();
        } catch (CoreException e) {
            JavaPlugin.log(e);

            return new HashMap(0);
        }
    }

    /**
     * Returns a collection of IMember instances representing what to search for 
     */
    private Collection getMembers() {
        Collection result = new ArrayList();

        result.add(getMember());

        return result;
    }
}
