/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2001
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
