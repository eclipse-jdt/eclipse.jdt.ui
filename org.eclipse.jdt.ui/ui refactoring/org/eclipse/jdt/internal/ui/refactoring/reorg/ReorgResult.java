/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring.reorg;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.IJavaElement;

public final class ReorgResult {
	private boolean fCanceled;
	private Map fNameChanges;
	// Either IResource or IJavaElement
	private Object fDestination;
    private IResource[] fResourcesToCopy;
    private IJavaElement[] fJavaElementsToCopy;
	
	public ReorgResult(boolean canceled, Map nameChanges) {
		fCanceled= canceled;
		fNameChanges= nameChanges;
	}
	
	public boolean isCanceled() {
		return fCanceled;
	}
	
	public Map getNameChanges() {
		return fNameChanges;
	}

    /**
     * Stores to arguments used by the paste operation.
     * 
     * @param destination the destination 
     * @param resourcesToCopy the resource to be copied
     * @param javaElementsToCopy the Java elements to be copied
     */
    /* package */ void setArguments(Object destination, IResource[] resourcesToCopy, IJavaElement[] javaElementsToCopy) {
    	fDestination= destination;
    	fJavaElementsToCopy= javaElementsToCopy;
    	// The problem is that Java elements which have an underlying resource
    	// show up in the list of resources as well. We sort them out.
    	if (fJavaElementsToCopy != null && resourcesToCopy != null) {
    		List realResources= new ArrayList(resourcesToCopy.length);
    		for (int i= 0; i < resourcesToCopy.length; i++) {
				IResource resource= resourcesToCopy[i];
				if (resource != null && !contains(fJavaElementsToCopy, resource)) {
					realResources.add(resource);
				}
			}
    		fResourcesToCopy= (IResource[])realResources.toArray(new IResource[realResources.size()]);
    	} else {
    		fResourcesToCopy= resourcesToCopy;
    	}
    }
    
    /**
     * Returns the destination.
     * 
     * @return the destination
     */
    public Object getDestination() {
    	return fDestination;
    }
    
    /**
     * Returns the Java elements to be copied. May return <code>null</code>.
     * 
     * @return the Java elements to be copied
     */
    public IJavaElement[] getJavaElementsToCopy() {
    	return fJavaElementsToCopy;
    }
    
    /**
     * Returns the resources to be copied. May returns <code>null</code>.
     * 
     * @return the resources to be copied
     */
    public IResource[] getResourcesToCopy() {
    	return fResourcesToCopy;
    }
    
    private static boolean contains(IJavaElement[] javaElements, IResource resource) {
    	for (int i= 0; i < javaElements.length; i++) {
			IJavaElement element= javaElements[i];
			if (resource.equals(element.getResource()))
				return true;
		}
    	return false;
    }
}
