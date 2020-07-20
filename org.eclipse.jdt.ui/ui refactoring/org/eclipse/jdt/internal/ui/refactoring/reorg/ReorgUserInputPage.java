/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Dirk Olmes <dirk@xanthippe.ping.de> - [refactoring] Allow expanding/collapsing folders on the ReorgUserInputPage - https://bugs.eclipse.org/bugs/show_bug.cgi?id=430750
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring.reorg;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.reorg.IReorgDestinationValidator;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaElementComparator;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaElementLabels;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;


abstract class ReorgUserInputPage extends UserInputWizardPage{
	private static final long LABEL_FLAGS= JavaElementLabels.ALL_DEFAULT
			| JavaElementLabels.M_PRE_RETURNTYPE | JavaElementLabels.M_PARAMETER_NAMES | JavaElementLabels.F_PRE_TYPE_SIGNATURE;
	private TreeViewer fViewer;
	public ReorgUserInputPage(String pageName) {
		super(pageName);
	}
	@Override
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);
		Composite result= new Composite(parent, SWT.NONE);
		setControl(result);
		result.setLayout(new GridLayout());

		Object initialSelection= getInitiallySelectedElement();
		verifyDestination(initialSelection, true);

		addLabel(result);

		fViewer= createViewer(result);
		fViewer.setSelection(new StructuredSelection(initialSelection), true);
		fViewer.addSelectionChangedListener(ReorgUserInputPage.this::viewerSelectionChanged);
		fViewer.addDoubleClickListener(new TreeViewerDoubleClickListener());
		Dialog.applyDialogFont(result);
	}

	protected Control addLabel(Composite parent) {
		Label label= new Label(parent, SWT.WRAP);
		String text;
		int resources= getResources().length;
		int javaElements= getJavaElements().length;

		if (resources == 0 && javaElements == 1) {
			text= Messages.format(
					ReorgMessages.ReorgUserInputPage_choose_destination_single,
					JavaElementLabels.getElementLabel(getJavaElements()[0], LABEL_FLAGS));
		} else if (resources == 1 && javaElements == 0) {
			text= Messages.format(
					ReorgMessages.ReorgUserInputPage_choose_destination_single,
					BasicElementLabels.getResourceName(getResources()[0]));
		} else {
			text= Messages.format(
					ReorgMessages.ReorgUserInputPage_choose_destination_multi,
					String.valueOf(resources + javaElements));
		}

		label.setText(text);
		GridData data= new GridData(SWT.FILL, SWT.END, true, false);
		data.widthHint= convertWidthInCharsToPixels(50);
		label.setLayoutData(data);
		return label;
	}

	private void viewerSelectionChanged(SelectionChangedEvent event) {
		ISelection selection= event.getSelection();
		if (!(selection instanceof IStructuredSelection))
			return;
		IStructuredSelection ss= (IStructuredSelection)selection;
		verifyDestination(ss.getFirstElement(), false);
	}

	protected abstract Object getInitiallySelectedElement();

	/**
	 * Set and verify the destination.
	 *
	 * @param selected the selected destination
	 * @return the resulting status
	 * @throws JavaModelException the JavaModelException in case it fails
	 */
	protected abstract RefactoringStatus verifyDestination(Object selected) throws JavaModelException;

	protected abstract IResource[] getResources();
	protected abstract IJavaElement[] getJavaElements();

	protected abstract IReorgDestinationValidator getDestinationValidator();

	private final void verifyDestination(Object selected, boolean initialVerification) {
		try {
			RefactoringStatus status= verifyDestination(selected);
			if (initialVerification)
				setPageComplete(status.isOK());
			else
				setPageComplete(status);
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
			setPageComplete(false);
		}
	}

	private TreeViewer createViewer(Composite parent) {
		TreeViewer treeViewer= new TreeViewer(parent, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		GridData gd= new GridData(GridData.FILL_BOTH);
		gd.widthHint= convertWidthInCharsToPixels(40);
		gd.heightHint= convertHeightInCharsToPixels(15);
		treeViewer.getTree().setLayoutData(gd);
		treeViewer.setLabelProvider(new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_SMALL_ICONS));
		treeViewer.setContentProvider(new DestinationContentProvider(getDestinationValidator()));
		treeViewer.setComparator(new JavaElementComparator());
		treeViewer.setInput(JavaCore.create(ResourcesPlugin.getWorkspace().getRoot()));
		return treeViewer;
	}

	protected TreeViewer getTreeViewer() {
		return fViewer;
	}

	private final class TreeViewerDoubleClickListener implements IDoubleClickListener {
		@Override
		public void doubleClick(DoubleClickEvent event) {
			IStructuredSelection selection= (IStructuredSelection) event.getSelection();
			Object element= selection.getFirstElement();
			if (fViewer.isExpandable(element)) {
				if (fViewer.getExpandedState(element)) {
					fViewer.collapseToLevel(element, 1);
				}
				else {
					ITreeContentProvider contentProvider= (ITreeContentProvider) fViewer.getContentProvider();
					Object[] children= contentProvider.getChildren(element);
					if (children.length > 0) {
						fViewer.expandToLevel(element, 1);
					}
				}
			}
		}
	}
}
