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
 *   Stephan Herrmann (stephan@cs.tu-berlin.de):
 *          - bug 206949: [call hierarchy] filter field accesses (only write or only read)
 *   Red Hat Inc. - refactored to jdt.core.manipulation and modified
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.callhierarchy;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.PlatformObject;

import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.search.IJavaSearchConstants;

/**
 * This class represents the general parts of a method call (either to or from a
 * method).
 */
public abstract class MethodWrapper extends PlatformObject {

	public static IMethodWrapperDynamic fMethodWrapperCore= new MethodWrapperDynamicCore();

	/**
	 * Set the IMethodWrapperCore class to use in MethodWrapper
	 *
	 * @param core the IMethodWrapperCore class to store
	 */
	public static final void setMethodWrapperDynamic(IMethodWrapperDynamic core) {
		fMethodWrapperCore= core;
	}

    private Map<String, MethodCall> fElements = null;

    /*
     * A cache of previously found methods. This cache should be searched
     * before adding a "new" method object reference to the list of elements.
     * This way previously found methods won't be searched again.
     */
    private Map<String, Map<String, MethodCall>> fMethodCache;
    private final MethodCall fMethodCall;
    private final MethodWrapper fParent;
    private int fLevel;
	/**
	 * One of {@link IJavaSearchConstants#REFERENCES}, {@link IJavaSearchConstants#READ_ACCESSES},
	 * or {@link IJavaSearchConstants#WRITE_ACCESSES}, or 0 if not set. Only used for root wrappers.
	 */
    private int fFieldSearchMode;

    public MethodWrapper(MethodWrapper parent, MethodCall methodCall) {
        Assert.isNotNull(methodCall);

        if (parent == null) {
            setMethodCache(new HashMap<>());
            fLevel = 1;
        } else {
            setMethodCache(parent.getMethodCache());
            fLevel = parent.getLevel() + 1;
        }

        this.fMethodCall = methodCall;
        this.fParent = parent;
    }

    @Override
	public <T> T getAdapter(Class<T> adapter) {
        return fMethodWrapperCore.getAdapter(this, adapter);
	}

    public MethodWrapper[] getCalls(IProgressMonitor progressMonitor) {
        if (fElements == null) {
            doFindChildren(progressMonitor);
        }

        MethodWrapper[] result = new MethodWrapper[fElements.size()];
        int i = 0;

        for (String string : fElements.keySet()) {
            MethodCall methodCall = getMethodCallFromMap(fElements, string);
            result[i++] = createMethodWrapper(methodCall);
        }

        return result;
    }

    public int getLevel() {
        return fLevel;
    }

    public IMember getMember() {
        return getMethodCall().getMember();
    }

    public MethodCall getMethodCall() {
        return fMethodCall;
    }

    public String getName() {
        if (getMethodCall() != null) {
            return getMethodCall().getMember().getElementName();
        } else {
            return ""; //$NON-NLS-1$
        }
    }

    public MethodWrapper getParent() {
        return fParent;
    }

    public int getFieldSearchMode() {
    	if (fFieldSearchMode != 0)
    		return fFieldSearchMode;
    	MethodWrapper parent= getParent();
    	while (parent != null) {
			if (parent.fFieldSearchMode != 0)
				return parent.fFieldSearchMode;
			else
				parent= parent.getParent();
		}
    	return IJavaSearchConstants.REFERENCES;
	}

    public void setFieldSearchMode(int fieldSearchMode) {
		fFieldSearchMode= fieldSearchMode;
	}

    @Override
	public boolean equals(Object oth) {
        return fMethodWrapperCore.equals(this,  oth);
    }

    @Override
	public int hashCode() {
        final int PRIME = 1000003;
        int result = 0;

        if (fParent != null) {
            result = (PRIME * result) + fParent.hashCode();
        }

        if (getMethodCall() != null) {
            result = (PRIME * result) + getMethodCall().getMember().hashCode();
        }

        return result;
    }

    private void setMethodCache(Map<String, Map<String, MethodCall>> methodCache) {
        fMethodCache = methodCache;
    }

    protected abstract String getTaskName();

    private void addCallToCache(MethodCall methodCall) {
        Map<String, MethodCall> cachedCalls = lookupMethod(this.getMethodCall());
        cachedCalls.put(methodCall.getKey(), methodCall);
    }

	/**
	 * Creates a method wrapper for the child of the receiver.
	 *
	 * @param methodCall the method call
	 * @return the method wrapper
	 */
    protected abstract MethodWrapper createMethodWrapper(MethodCall methodCall);

    private void doFindChildren(IProgressMonitor progressMonitor) {
        Map<String, MethodCall> existingResults = lookupMethod(getMethodCall());

        if (existingResults != null && !existingResults.isEmpty()) {
            fElements = new HashMap<>();
            fElements.putAll(existingResults);
        } else {
            initCalls();

            if (progressMonitor != null) {
                progressMonitor.beginTask(getTaskName(), 100);
            }

            try {
                performSearch(progressMonitor);
            } catch (OperationCanceledException e){
            	fElements= null;
            	throw e;
            } finally {
                if (progressMonitor != null) {
                    progressMonitor.done();
                }
            }
        }
    }

    /**
     * Determines if the method represents a recursion call (i.e. whether the
     * method call is already in the cache.)
     *
     * @return True if the call is part of a recursion
     */
    public boolean isRecursive() {
		if (fParent instanceof RealCallers)
			return false;
        MethodWrapper current = getParent();

        while (current != null) {
            if (getMember().getHandleIdentifier().equals(current.getMember()
                                                                        .getHandleIdentifier())) {
                return true;
            }

            current = current.getParent();
        }

        return false;
    }

	/**
	 * @return whether this member can have children
	 */
	public abstract boolean canHaveChildren();

    /**
     * This method finds the children of the current IMember (either callers or
     * callees, depending on the concrete subclass).
     * @param progressMonitor a progress monitor
     *
     * @return a map from handle identifier ({@link String}) to {@link MethodCall}
     */
    protected abstract Map<String, MethodCall> findChildren(IProgressMonitor progressMonitor);

    private Map<String, Map<String, MethodCall>> getMethodCache() {
        return fMethodCache;
    }

    private void initCalls() {
        this.fElements = new HashMap<>();

        initCacheForMethod();
    }

    /**
     * Looks up a previously created search result in the "global" cache.
     * @param methodCall the method call
     * @return the List of previously found search results
     */
    private Map<String, MethodCall> lookupMethod(MethodCall methodCall) {
        return getMethodCache().get(methodCall.getKey());
    }

    private void performSearch(IProgressMonitor progressMonitor) {
        fElements = findChildren(progressMonitor);

        for (String string : fElements.keySet()) {
            checkCanceled(progressMonitor);

            MethodCall methodCall = getMethodCallFromMap(fElements, string);
            addCallToCache(methodCall);
        }
    }

    private MethodCall getMethodCallFromMap(Map<String, MethodCall> elements, String key) {
        return elements.get(key);
    }

    private void initCacheForMethod() {
        Map<String, MethodCall> cachedCalls = new HashMap<>();
        getMethodCache().put(this.getMethodCall().getKey(), cachedCalls);
    }

    /**
     * Checks with the progress monitor to see whether the creation of the type hierarchy
     * should be canceled. Should be regularly called
     * so that the user can cancel.
     *
     * @param progressMonitor the progress monitor
     * @exception OperationCanceledException if cancelling the operation has been requested
     * @see IProgressMonitor#isCanceled
     */
    protected void checkCanceled(IProgressMonitor progressMonitor) {
        if (progressMonitor != null && progressMonitor.isCanceled()) {
            throw new OperationCanceledException();
        }
    }

	/**
	 * Removes the given method call from the cache.
	 *
	 * @since 3.6
	 */
	public void removeFromCache() {
		fElements= null;
		fMethodCache.remove(getMethodCall().getKey());
	}

	@Override
	public String toString() {
		StringBuilder builder= new StringBuilder();
		builder.append("MethodWrapper ["); //$NON-NLS-1$
		if (fMethodCall != null) {
			builder.append(fMethodCall);
		}
		if (fParent != null) {
			builder.append(", "); //$NON-NLS-1$
			builder.append("parent="); //$NON-NLS-1$
			builder.append(fParent);
		}
		builder.append("]"); //$NON-NLS-1$
		return builder.toString();
	}
}
