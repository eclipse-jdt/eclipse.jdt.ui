/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.reorg;

import java.util.List;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.dialogs.ISelectionValidator;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;

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
			info.setError(""); //$NON-NLS-1$
			//XXX: Could give more detailed error message
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