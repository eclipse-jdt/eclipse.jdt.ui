/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.compare;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.ISelection;

public class JavaCompareWithEditionAction extends JavaHistoryAction {
	
	private static final String BUNDLE_NAME= "org.eclipse.jdt.internal.ui.compare.AddFromHistoryAction";

	public JavaCompareWithEditionAction(ISelectionProvider sp) {
		super(sp, BUNDLE_NAME);
	}
	
	public void run() {
	}
}
