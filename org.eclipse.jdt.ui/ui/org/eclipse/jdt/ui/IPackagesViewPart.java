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
	 * 
	 * @return the tree viewer used in the Packages view
	 * 
	 * @since 2.0
	 */
	TreeViewer getTreeViewer();
}
