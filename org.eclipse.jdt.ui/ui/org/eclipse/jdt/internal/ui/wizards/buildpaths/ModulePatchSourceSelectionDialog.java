/*******************************************************************************
 * Copyright (c) 2019 GK Software SE, and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Stephan Herrmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.util.ArrayList;
import java.util.List;

import com.ibm.icu.text.MessageFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IModuleDescription;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;

public class ModulePatchSourceSelectionDialog extends TrayDialog {

	/**
	 * Selective tree showing only java projects and their source folders.
	 */
	static class SourcesContentProvider implements ITreeContentProvider {
		@Override
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof List<?>)
				return ((List<?>) inputElement).toArray();
			return null;
		}
		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof IJavaProject) {
				List<IPackageFragmentRoot> sourceRoots= new ArrayList<>();
				IJavaProject parentProject= (IJavaProject) parentElement;
				collectSourceFolders(sourceRoots, parentProject);
				return sourceRoots.toArray();
			}
			return null;
		}
		@Override
		public Object getParent(Object element) {
			if (element instanceof IPackageFragmentRoot) {
				return ((IPackageFragmentRoot) element).getJavaProject();
			}
			return null;
		}
		@Override
		public boolean hasChildren(Object element) {
			if (element instanceof IJavaProject) {
				return getChildren(element) != null;
			}
			return false;
		}
	}

	class ContextProjectFirstComparator extends ViewerComparator {
		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			if (e1.equals(fContextProject)) {
				return -1;
			}
			if (e2.equals(fContextProject)) {
				return 1;
			}
			return super.compare(viewer, e1, e2);
		}
	}

	private IModuleDescription fFocusModule;
	private List<IJavaProject> fProjects;
	private IJavaProject fContextProject;

	private List<IJavaElement> fResult;

	protected ModulePatchSourceSelectionDialog(Shell shell, IModuleDescription focusModule, IJavaProject contextProject) {
		super(shell);
		fFocusModule= focusModule;
		fContextProject= contextProject;
		findProjects();
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(NewWizardMessages.ModulePatchSourceSelectionDialog_patchSourceLocation_title);
	}

	@Override
	protected int getShellStyle() {
		return super.getShellStyle() | SWT.RESIZE;
	}

	private void  findProjects() {
		fProjects= new ArrayList<>();
		IProject toSkip= null;
		IPackageFragmentRoot focusRoot= (IPackageFragmentRoot) fFocusModule.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
		try {
			if (focusRoot != null) {
				if (focusRoot.getKind() == IPackageFragmentRoot.K_SOURCE) {
					toSkip= focusRoot.getJavaProject().getProject();
				}
			} else if (fFocusModule.isAutoModule()) {
				if (fFocusModule.getJavaProject() != null) {
					toSkip= fFocusModule.getJavaProject().getProject();
				}
			}
		} catch (JavaModelException e1) {
			JavaPlugin.log(e1);
		}
		for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			if (toSkip != null && toSkip.equals(project)) {
				continue;
			}
			try {
				if (project.isOpen() && project.hasNature(JavaCore.NATURE_ID)) {
					IJavaProject jProj= JavaCore.create(project);
					if (jProj.getOwnModuleDescription() == null) {
						fProjects.add(jProj);
					}
				}
			} catch (CoreException e) {
				JavaPlugin.log(e);
			}
		}
	}

	static void collectSourceFolders(List<IPackageFragmentRoot> sourceRoots, IJavaProject project) {
		try {
			for (IClasspathEntry entry : project.getRawClasspath()) {
				if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
					for (IPackageFragmentRoot packageFragmentRoot : project.findPackageFragmentRoots(entry)) {
						if (packageFragmentRoot.getModuleDescription() == null) {
							sourceRoots.add(packageFragmentRoot);
						}
					}
				}
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite= (Composite) super.createDialogArea(parent);
		PixelConverter converter= new PixelConverter(parent);
		int widthHint= converter.convertWidthInCharsToPixels(60);
		Label message= new Label(composite, SWT.LEFT + SWT.WRAP);
		message.setText(MessageFormat.format(
							NewWizardMessages.ModulePatchSourceSelectionDialog_patchSourceLocation_message,
							fFocusModule.getElementName()));
		GridData gdLabel= new GridData(SWT.FILL, SWT.NONE, true, false);
		gdLabel.widthHint= widthHint;
		message.setLayoutData(gdLabel);

		TreeViewer treeViewer= new TreeViewer(composite, SWT.MULTI | SWT.BORDER);
		treeViewer.setContentProvider(new SourcesContentProvider());
		treeViewer.setLabelProvider(new JavaElementLabelProvider());
		treeViewer.setComparator(new ContextProjectFirstComparator());
		treeViewer.addSelectionChangedListener(this::selectionChanged);
		treeViewer.setInput(fProjects);

		GridData gd= new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.widthHint= widthHint;
		gd.heightHint= converter.convertHeightInCharsToPixels(20);
		treeViewer.getControl().setLayoutData(gd);

		return composite;
	}

	@Override
	protected Control createButtonBar(Composite parent) {
		Control control= super.createButtonBar(parent);
		getButton(IDialogConstants.OK_ID).setEnabled(false); // until element(s) enabled
		return control;

	}
	void selectionChanged(SelectionChangedEvent event) {
		IStructuredSelection selection= event.getStructuredSelection();
		fResult= new ArrayList<>();
		for (Object object : selection) {
			if (object instanceof IPackageFragmentRoot) {
				fResult.add((IPackageFragmentRoot) object);
			} else if (object instanceof IJavaProject) {
				fResult.add((IJavaProject) object);
			}
		}
		getButton(IDialogConstants.OK_ID).setEnabled(!fResult.isEmpty());
	}

	public List<IJavaElement> getResult() {
		return fResult;
	}
}
