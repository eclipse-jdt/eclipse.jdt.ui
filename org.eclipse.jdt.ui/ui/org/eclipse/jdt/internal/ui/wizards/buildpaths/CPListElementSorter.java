/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import org.eclipse.jface.viewers.ViewerSorter;

import org.eclipse.jdt.core.IClasspathEntry;

public class CPListElementSorter extends ViewerSorter {
	
	private static final int SOURCE= 0;
	private static final int PROJECT= 1;
	private static final int LIBRARY= 2;
	private static final int VARIABLE= 3;
	private static final int CONTAINER= 4;
	private static final int OTHER= 5;
	
	/*
	 * @see ViewerSorter#category(Object)
	 */
	public int category(Object obj) {
		if (obj instanceof CPListElement) {
			switch (((CPListElement)obj).getEntryKind()) {
			case IClasspathEntry.CPE_LIBRARY:
				return LIBRARY;
			case IClasspathEntry.CPE_PROJECT:
				return PROJECT;
			case IClasspathEntry.CPE_SOURCE:
				return SOURCE;
			case IClasspathEntry.CPE_VARIABLE:
				return VARIABLE;
			case IClasspathEntry.CPE_CONTAINER:
				return CONTAINER;
			}
		}
		return OTHER;
	}

}
