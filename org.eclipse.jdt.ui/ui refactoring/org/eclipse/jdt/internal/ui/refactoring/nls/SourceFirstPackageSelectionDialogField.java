/*****************************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others. All rights reserved. This program
 * and the accompanying materials are made available under the terms of the Common Public
 * License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ****************************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring.nls;

import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;

class SourceFirstPackageSelectionDialogField {

	/* package */ SourceFolderSelectionDialogButtonField fSourceFolderSelection;
	/* package */ PackageFragmentSelection fPackageSelection;
	private Shell fShell;

	public SourceFirstPackageSelectionDialogField(String sourceLabel, String packageLabel, String browseLabel1,
		String browseLabel2, String statusHint, String dialogTitle, String dialogMessage, String dialogEmptyMessage,
		ICompilationUnit cu, IJavaProject root, IDialogFieldListener updateListener, IPackageFragment fragment) {
		fSourceFolderSelection= new SourceFolderSelectionDialogButtonField(sourceLabel, browseLabel1, root,
			new SFStringButtonAdapter());

		fPackageSelection= new PackageFragmentSelection(this, packageLabel, browseLabel2, statusHint,
			new PackageSelectionStringButtonAdapter(this, dialogTitle, dialogMessage, dialogEmptyMessage));
		fPackageSelection.setDialogFieldListener(new PackageSelectionDialogFieldListener());

		fSourceFolderSelection.setSourceChangeListener(fPackageSelection);

		setDefaults(fragment, cu);

		fPackageSelection.setUpdateListener(updateListener);
		fSourceFolderSelection.setUpdateListener(updateListener);
	}

	private void setDefaults(IPackageFragment fragment, ICompilationUnit cu) {
		IJavaElement element= fragment;
		if (element == null) {
			element= cu;
		}

		fSourceFolderSelection.setRoot(searchSourcePackageFragmentRoot(element));
		fPackageSelection.setPackageFragment(searchPackageFragment(element));
	}

	private IPackageFragment searchPackageFragment(IJavaElement jElement) {
		return (IPackageFragment)jElement.getAncestor(IJavaElement.PACKAGE_FRAGMENT);
	}

	private IPackageFragmentRoot searchSourcePackageFragmentRoot(IJavaElement jElement) {
		IJavaElement parent= jElement.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
		if (parent == null) {
			return null;
		}

		IPackageFragmentRoot res= (IPackageFragmentRoot)parent;
		try {
			if (res.getKind() == IPackageFragmentRoot.K_SOURCE) {
				return res;
			}
		} catch (JavaModelException e) {
			// nothing to do
		}

		return null;
	}

	class PackageSelectionDialogFieldListener implements IDialogFieldListener {

		public void dialogFieldChanged(DialogField field) {
			String packName= fPackageSelection.getText();

			if (packName.length() == 0) {
				fPackageSelection.setStatus("(default)"); //$NON-NLS-1$
			} else {
				fPackageSelection.setStatus(""); //$NON-NLS-1$
			}

		}
	}

	class SFStringButtonAdapter implements IStringButtonAdapter {
		public void changeControlPressed(DialogField field) {

			IPackageFragmentRoot newSourceContainer= SourceContainerDialog.getSourceContainer(fShell, ResourcesPlugin
				.getWorkspace().getRoot(), (IJavaElement)getInitElement());
			if (newSourceContainer != null) {
				fSourceFolderSelection.setRoot(newSourceContainer);
			}
		}
	}

	public IPackageFragment getSelected() {
		IPackageFragment res= fPackageSelection.getPackageFragment();
		return res;
	}

	public IPackageFragmentRoot getSelectedFragmentRoot() {
		return fSourceFolderSelection.getRoot();
	}

	public void setSelected(IPackageFragment newSelection) {
		fPackageSelection.setPackageFragment(newSelection);
		fSourceFolderSelection.setRoot(searchSourcePackageFragmentRoot(newSelection));
	}

	public void createControl(Composite parent, int nOfColumns, int textWidth) {
		fShell= parent.getShell();
		fSourceFolderSelection.doFillIntoGrid(parent, nOfColumns, textWidth);
		fPackageSelection.doFillIntoGrid(parent, nOfColumns, textWidth);
	}

	public Object getInitElement() {
		return fSourceFolderSelection.getRoot();
	}

}