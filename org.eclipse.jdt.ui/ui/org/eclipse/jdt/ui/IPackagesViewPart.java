/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.ui;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.IViewPart;

/**
 * The standard Packages view presents a Java-centric view of the workspace.
 * Within Java projects, the resource hierarchy is organized into Java packages
 * as described by the project's classpath. Note that this view shows both Java 
 * elements and ordinary resources.
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 *
 * @see JavaUI#ID_PACKAGES
 */
public interface IPackagesViewPart extends IViewPart {
	/**
	 * Selects and reveals the given element in this packages view.
	 * The tree will be expanded as needed to show the element.
	 *
	 * @param element the element to be revealed
	 */
	void selectAndReveal(Object element);
	
	/**
	 * Returns the TreeViewer shown in the Packages view.
	 */
	TreeViewer getTreeViewer();
}