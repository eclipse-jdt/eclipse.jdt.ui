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

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;

class CalleeMethodWrapper extends MethodWrapper {
    private Comparator fMethodWrapperComparator = new MethodWrapperComparator();

    private class MethodWrapperComparator implements Comparator {
        /* (non-Javadoc)
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        public int compare(Object o1, Object o2) {
            MethodWrapper m1 = (MethodWrapper) o1;
            MethodWrapper m2 = (MethodWrapper) o2;

            CallLocation callLocation1 = m1.getMethodCall().getFirstCallLocation();
            CallLocation callLocation2 = m2.getMethodCall().getFirstCallLocation();

            if ((callLocation1 != null) && (callLocation2 != null)) {
                if (callLocation1.getStart() == callLocation2.getStart()) {
                    return callLocation1.getEnd() - callLocation2.getEnd();
                }

                return callLocation1.getStart() - callLocation2.getStart();
            }

            return 0;
        }
    }

    /**
     * Constructor for CalleeMethodWrapper.
     * @param parent
     * @param method
     */
    public CalleeMethodWrapper(MethodWrapper parent, MethodCall methodCall) {
        super(parent, methodCall);
    }

	/* Returns the calls sorted after the call location
	 * @see org.eclipse.jdt.internal.ui.callhierarchy.MethodWrapper#getCalls()
     */
    public MethodWrapper[] getCalls() {
        MethodWrapper[] result = super.getCalls();
        Arrays.sort(result, fMethodWrapperComparator);

        return result;
    }

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.callhierarchy.MethodWrapper#getTaskName()
     */
    protected String getTaskName() {
        return null;
    }

	/**
	 * @see org.eclipse.jdt.internal.ui.callhierarchy.MethodWrapper#createMethodWrapper(org.eclipse.jdt.internal.ui.callhierarchy.MethodCall)
     */
    protected MethodWrapper createMethodWrapper(MethodCall methodCall) {
        return new CalleeMethodWrapper(this, methodCall);
    }

	/**
     * Find callees called from the current method.
	 * @see org.eclipse.jdt.internal.ui.callhierarchy.MethodWrapper#findChildren(org.eclipse.core.runtime.IProgressMonitor)
     */
    protected Map findChildren(IProgressMonitor progressMonitor) {
        if (getMember().getElementType() == IJavaElement.METHOD) {
            CalleeAnalyzerVisitor visitor = new CalleeAnalyzerVisitor((IMethod) getMember(),
                    progressMonitor);
            ICompilationUnit icu = getMember().getCompilationUnit();
        
            if (icu != null) {
                CompilationUnit cu = AST.parseCompilationUnit(icu, true);
                cu.accept(visitor);
            }
        
            return visitor.getCallees();
        } else {
            return new HashMap();
        }
    }
}
