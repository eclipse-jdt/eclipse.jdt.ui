/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */

package org.eclipse.jdt.internal.ui.reorg;

import java.util.List;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.dialogs.ISelectionValidator;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.util.JdtHackFinder;

public class CopyElementsValidator implements ISelectionValidator {
	private List fElementsToCopy;
	private boolean fIsMove;
	
	public CopyElementsValidator(List elements) {
		fElementsToCopy= elements;
	}
	
	public void isValid(Object[] selected, StatusInfo info) {
		if (isValid(selected)) {
			info.setOK();
		} else {
			info.setError("");
			JdtHackFinder.fixme("Could give more detailed error message");
		}
	}
	
	private boolean isValid(Object[] selection) {
		if (selection.length != 1)
			return false;
		Object selected= selection[0];
		if (selected instanceof IJavaProject) {
			try {
				selected= ReorgSupportFactory.getPackageFragmentRoot((IJavaProject)selected);
			} catch (JavaModelException e) {
			}
		}
		ICopySupport support= ReorgSupportFactory.createCopySupport(fElementsToCopy);
		return support.canCopy(fElementsToCopy, selected);	
	}
}