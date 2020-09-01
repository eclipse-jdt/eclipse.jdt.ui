/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.callhierarchy;

import org.eclipse.swt.widgets.Display;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

import org.eclipse.ui.progress.IDeferredWorkbenchAdapter;
import org.eclipse.ui.progress.IElementCollector;

import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class DeferredMethodWrapper extends MethodWrapperWorkbenchAdapter implements IDeferredWorkbenchAdapter {
    private final CallHierarchyContentProvider fProvider;

    /**
     * A simple job scheduling rule for serializing jobs that shouldn't be run
     * concurrently.
     */
    private static class BatchSimilarSchedulingRule implements ISchedulingRule {
        public String id;

        public BatchSimilarSchedulingRule(String id) {
            this.id = id;
        }

        /*
         * @see org.eclipse.core.runtime.jobs.ISchedulingRule#isConflicting(org.eclipse.core.runtime.jobs.ISchedulingRule)
         */
        @Override
		public boolean isConflicting(ISchedulingRule rule) {
            if (rule instanceof BatchSimilarSchedulingRule) {
                return ((BatchSimilarSchedulingRule) rule).id.equals(id);
            }
            return false;
        }

        /*
         * @see org.eclipse.core.runtime.jobs.ISchedulingRule#contains(org.eclipse.core.runtime.jobs.ISchedulingRule)
         */
        @Override
		public boolean contains(ISchedulingRule rule) {
            return this == rule;
        }
    }

    DeferredMethodWrapper(CallHierarchyContentProvider provider, MethodWrapper methodWrapper) {
    	super(methodWrapper);
        this.fProvider = provider;
    }

    private Object getCalls(IProgressMonitor monitor) {
        return getMethodWrapper().getCalls(monitor);
    }

    @Override
	public void fetchDeferredChildren(Object object, IElementCollector collector, IProgressMonitor monitor) {
    	final DeferredMethodWrapper deferredMethodWrapper= (DeferredMethodWrapper)object;
    	try {
            fProvider.startFetching();
            collector.add((Object[]) deferredMethodWrapper.getCalls(monitor), monitor);
            collector.done();
        } catch (OperationCanceledException e) {
        	final MethodWrapper methodWrapper= deferredMethodWrapper.getMethodWrapper();
			if (!CallHierarchyContentProvider.isExpandWithConstructors(methodWrapper)) {
	        	Display.getDefault().asyncExec(() -> {
					CallHierarchyViewPart viewPart= fProvider.getViewPart();
					if (viewPart != null && !viewPart.getViewer().getControl().isDisposed())
						fProvider.collapseAndRefresh(methodWrapper);
				});
        	}
        } catch (Exception e) {
            JavaPlugin.log(e);
        } finally {
            fProvider.doneFetching();
        }
    }

    @Override
	public boolean isContainer() {
        return true;
    }

    @Override
	public ISchedulingRule getRule(Object o) {
        return new BatchSimilarSchedulingRule("org.eclipse.jdt.ui.callhierarchy.methodwrapper"); //$NON-NLS-1$
    }

    @Override
	public Object[] getChildren(Object o) {
        return this.fProvider.fetchChildren(((DeferredMethodWrapper) o).getMethodWrapper());
    }

}
