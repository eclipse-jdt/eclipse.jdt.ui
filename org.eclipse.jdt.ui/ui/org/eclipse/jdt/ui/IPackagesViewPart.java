package org.eclipse.jdt.ui;/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2001
 */


import org.eclipse.ui.IViewPart;

/**
 * The standard packages view presents a Java-centric view of the workspace.
 * Within Java projects, the resource hierarchy is organized into Java packages
 * as described by the project's classpath. Note that this view shows both Java 
 * elements and ordinary resources.
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 *
 * @see IJavaUIConstants#ID_PACKAGES
 */
public interface IPackagesViewPart extends IViewPart {
	/**
	 * Selects and reveals the given element in this packages view.
	 * The tree will be expanded as needed to show the element.
	 *
	 * @param element the element to be revealed
	 */
	void selectAndReveal(Object element);
}