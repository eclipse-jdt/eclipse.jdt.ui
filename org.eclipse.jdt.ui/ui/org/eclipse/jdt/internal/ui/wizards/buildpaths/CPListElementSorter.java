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
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import org.eclipse.jface.viewers.ViewerSorter;

import org.eclipse.jdt.core.IClasspathEntry;

public class CPListElementSorter extends ViewerSorter {
	
	private static final int SOURCE= 0;
	private static final int PROJECT= 1;
	private static final int LIBRARY= 2;
	private static final int VARIABLE= 3;
	private static final int CONTAINER= 4;
	
	private static final int INCLUSION= 5;
	
	private static final int OTHER= 7;
	
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
		} else if (obj instanceof CPListElementAttribute) {
			String key= ((CPListElementAttribute)obj).getKey();
			if (CPListElement.INCLUSION.equals(key)) {
				return INCLUSION;
			}
		}
		return OTHER;
	}
	

}
