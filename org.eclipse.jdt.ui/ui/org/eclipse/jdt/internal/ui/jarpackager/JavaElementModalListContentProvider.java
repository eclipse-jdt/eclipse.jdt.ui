/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.jarpackager;

import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.viewsupport.AbstractJavaElementContentProvider;
 
/**
 * Content provider for Java elements in a modal list viewer.
 */
class JavaElementModalListContentProvider extends AbstractJavaElementContentProvider {
	
	/**
	 * Processes a delta recursively. When more than two children are affected the
	 * tree is fully refreshed starting at this node. The delta is processed in the
	 * current thread but the viewer updates are posted to the UI thread.
	 */
	protected void processDelta(IJavaElementDelta delta) throws JavaModelException {
		// do nothing	}
}