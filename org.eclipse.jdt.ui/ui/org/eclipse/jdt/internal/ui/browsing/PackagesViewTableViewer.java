/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v0.5
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 *
 * Contributors:
 *     IBM Corporation - initial implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.browsing;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Widget;

import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.internal.ui.viewsupport.ProblemTableViewer;
	
/**
 * Special problem table viewer to handle logical packages.
 */
class PackagesViewTableViewer extends ProblemTableViewer {

	public PackagesViewTableViewer(Composite parent, int style) {
		super(parent, style);
	}

	public void mapElement(Object element, Widget item) {
		if (element instanceof LogicalPackage) {
			LogicalPackage cp= (LogicalPackage) element;
			IPackageFragment[] fragments= cp.getFragments();
			for (int i= 0; i < fragments.length; i++) {
				IPackageFragment fragment= fragments[i];
				super.mapElement(fragment, item);
			}
		}
		super.mapElement(element, item);		
	}

	public void unmapElement(Object element, Widget item) {
		if (element instanceof LogicalPackage) {
			LogicalPackage cp= (LogicalPackage) element;
			IPackageFragment[] fragments= cp.getFragments();
			for (int i= 0; i < fragments.length; i++) {
				IPackageFragment fragment= fragments[i];
				super.unmapElement(fragment, item);
			}	
		}
		super.unmapElement(element, item);
	}
}
