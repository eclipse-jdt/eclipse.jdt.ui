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
package org.eclipse.jdt.internal.ui.refactoring.nls;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.ui.refactoring.contentassist.JavaSourcePackageFragmentRootCompletionProcessor;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;

class SourceFolderSelectionDialogButtonField extends StringButtonDialogField implements IDialogFieldListener {

	private IPackageFragmentRoot fRoot;
	private SourceChangeListener fListener;
	private IDialogFieldListener fUpdateListener;
	private IJavaProject fProject;

	public SourceFolderSelectionDialogButtonField(String descriptionLabel, String browseLabel, IJavaProject root,
		final IStringButtonAdapter adapter) {
		super(adapter);
		setContentAssistProcessor(new JavaSourcePackageFragmentRootCompletionProcessor());
		fProject= root;
		setLabelText(descriptionLabel);
		setButtonLabel(browseLabel);
		setDialogFieldListener(this);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener#dialogFieldChanged(org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField)
	 */
	public void dialogFieldChanged(DialogField field) {
		// propagate a textchange to the fragment root of this
		String rootString= getRootString();
		String newString= getText();
		if (!rootString.equals(newString)) {
			setRootFromString(newString);
		}
	}

	public void setUpdateListener(IDialogFieldListener updateListener) {
		fUpdateListener= updateListener;
	}

	public Control[] doFillIntoGrid(Composite parent, int nColumns, int textWidth) {
		Control[] res= super.doFillIntoGrid(parent, nColumns);

		final Text text= getTextControl(null);
		LayoutUtil.setWidthHint(text, textWidth);
		LayoutUtil.setHorizontalGrabbing(text);

		return res;
	}

	public void setSourceChangeListener(SourceChangeListener listener) {
		fListener= listener;
	}

	/**
	 * tries to build a packagefragmentroot out of a string and sets the string into this
	 * packagefragmentroot.
	 * 
	 * @param rootString
	 */
	private void setRootFromString(String rootString) {
		String projectName= getProjectName(rootString);
		String fragmentRootName= getFragmentRootName(rootString);

		IPackageFragmentRoot root= null;
		if ((projectName != null) && (fragmentRootName != null)) {
			if (projectName.equals(fProject.getElementName())) {
				root= findFragmentRoot(fProject, getFragmentRootName(rootString));
			}
		}
		setRoot(root);
	}

	public void setRoot(IPackageFragmentRoot root) {
		fRoot= root;

		if (fRoot != null) {
			String str= getRootString();
			if (getText().equals(str) == false) {
				setText(str);
			}
		} else {
			// dont ripple if the root is not a real root
		}

		fListener.sourceRootChanged(fRoot);
		if (fUpdateListener != null) {
			fUpdateListener.dialogFieldChanged(this);
		}
	}

	public IPackageFragmentRoot getRoot() {
		return fRoot;
	}

	private String getProjectName(String fragmentRootString) {
		int idx= fragmentRootString.indexOf('/');
		if (idx == -1) {
			return fragmentRootString;
		}
		return fragmentRootString.substring(0, idx);
	}

	private String getFragmentRootName(String fragmentRootString) {
		int idx= fragmentRootString.indexOf('/');
		if (idx == -1) {
			return null;
		}
		if (idx + 1 < fragmentRootString.length()) {
			return fragmentRootString.substring(idx + 1);
		} else {
			return ""; //$NON-NLS-1$
		}
	}

	private IPackageFragmentRoot findFragmentRoot(IJavaProject project, String rootString) {
		try {
			IPackageFragmentRoot[] roots= project.getPackageFragmentRoots();
			for (int i= 0; i < roots.length; i++) {
				IPackageFragmentRoot root= roots[i];
				if (root.getElementName().equals(rootString)) {
					return root;
				}
			}
		} catch (CoreException e) {
			// nothing to do
		}
		return null;
	}

	private String getRootString() {
		return (fRoot == null) ? "" : fRoot.getPath().makeRelative().toString(); //$NON-NLS-1$
	}


}