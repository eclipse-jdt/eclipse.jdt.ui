/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.reorg;

import java.util.List;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.dialogs.ISelectionValidator;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;

public class CopyElementsValidator implements ISelectionValidator {

	private IStatus fErrorStatus= new StatusInfo(IStatus.ERROR, "");
	private IStatus fOKStatus= new StatusInfo();

	private List fElementsToCopy;
	private boolean fIsMove;
	
	public CopyElementsValidator(List elements) {
		fElementsToCopy= elements;
	}
	
	public IStatus isValid(Object[] selected) {
		if (validate(selected)) {
			return fOKStatus;
		}
		//XXX: Could give more detailed error message
		return fErrorStatus;
	}
	
	private boolean validate(Object[] selection) {
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