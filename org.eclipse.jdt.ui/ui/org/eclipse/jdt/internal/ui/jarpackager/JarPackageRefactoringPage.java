/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.jarpackager;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.wizard.WizardPage;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchContentProvider;

import org.eclipse.jdt.internal.corext.Assert;

import org.eclipse.jdt.ui.JavaElementSorter;
import org.eclipse.jdt.ui.ProblemsLabelDecorator;
import org.eclipse.jdt.ui.jarpackager.JarPackageData;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.viewsupport.JavaUILabelProvider;

/**
 * Jar package wizard page to configure refactorings to be attached to the JAR.
 * 
 * @since 3.2
 */
final class JarPackageRefactoringPage extends WizardPage implements IJarPackageWizardPage {

	/** Content provider for projects whose refactorings are exported */
	private static class RefactoringProjectContentProvider extends WorkbenchContentProvider {

		/** The no children constant */
		private static final Object[] NO_CHILDREN= {};

		/**
		 * {@inheritDoc}
		 */
		public Object[] getElements(Object element) {
			if (element instanceof IProject[])
				return (Object[]) element;
			return NO_CHILDREN;
		}
	}

	/** The page name */
	private final static String PAGE_NAME= "jarRefactoringsWizardPage"; //$NON-NLS-1$

	/** The export structual only dialog settings store */
	private static final String STORE_EXPORT_STRUCTURAL_ONLY= PAGE_NAME + ".EXPORT_STRUCTURAL_ONLY"; //$NON-NLS-1$

	/** The export deprecation information dialog settings store */
	private static final String STORE_EXPORT_DEPRECATION_INFO= PAGE_NAME + ".EXPORT_DEPRECATION_INFO"; //$NON-NLS-1$

	/** The jar package data */
	private final JarPackageData fJarPackageData;

	/** The projects table viewer */
	private CheckboxTableViewer fTableViewer;

	/**
	 * Creates a new jar package refactoring page.
	 * 
	 * @param data
	 *            the jar package data
	 */
	public JarPackageRefactoringPage(final JarPackageData data) {
		super(PAGE_NAME);
		Assert.isNotNull(data);
		fJarPackageData= data;
		setTitle(JarPackagerMessages.JarPackageRefactoringPage_title);
		setDescription(JarPackagerMessages.JarPackageRefactoringPage_description);
	}

	/**
	 * {@inheritDoc}
	 */
	public void createControl(final Composite parent) {
		initializeDialogUnits(parent);
		final IDialogSettings settings= getDialogSettings();
		if (settings != null)
			fJarPackageData.setExportStructuralOnly(settings.getBoolean(STORE_EXPORT_STRUCTURAL_ONLY));
		final Composite composite= new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));
		Label label= new Label(composite, SWT.NONE);
		label.setText(JarPackagerMessages.JarPackageRefactoringPage_viewer_caption);
		createProjectTable(composite);
		createPlainLabel(composite, JarPackagerMessages.JarPackageWizardPage_options_label);
		createOptionsGroup(composite);
		setControl(composite);
		Dialog.applyDialogFont(composite);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IJavaHelpContextIds.JARREFACTORING_WIZARD_PAGE);
	}

	/**
	 * Creates the options group of this page.
	 * 
	 * @param parent
	 *            the parent control
	 */
	protected void createOptionsGroup(final Composite parent) {
		Assert.isNotNull(parent);
		final Composite group= new Composite(parent, SWT.NONE);
		final GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		group.setLayout(layout);
		final Button structural= new Button(group, SWT.CHECK | SWT.LEFT);
		structural.setText(JarPackagerMessages.JarPackageRefactoringPage_export_structural_only);
		structural.setSelection(fJarPackageData.isExportStructuralOnly());
		structural.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(final SelectionEvent event) {
				fJarPackageData.setExportStructuralOnly(structural.getSelection());
			}
		});
		final Button deprecations= new Button(group, SWT.CHECK | SWT.LEFT);
		deprecations.setText(JarPackagerMessages.JarPackageRefactoringPage_include_deprecation_info0);
		deprecations.setSelection(fJarPackageData.isDeprecationAware());
		deprecations.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(final SelectionEvent event) {
				fJarPackageData.setDeprecationAware(deprecations.getSelection());
			}
		});
	}

	/**
	 * Creates a new label with a bold font.
	 * 
	 * @param parent
	 *            the parent control
	 * @param text
	 *            the label text
	 * @return the new label control
	 */
	protected Label createPlainLabel(Composite parent, String text) {
		Label label= new Label(parent, SWT.NONE);
		label.setText(text);
		label.setFont(parent.getFont());
		GridData data= new GridData();
		data.verticalIndent= IDialogConstants.VERTICAL_SPACING;
		data.verticalAlignment= GridData.FILL;
		data.horizontalAlignment= GridData.FILL;
		label.setLayoutData(data);
		return label;
	}

	/**
	 * Creates the project table of this page.
	 * 
	 * @param parent
	 *            the parent control
	 */
	protected void createProjectTable(final Composite parent) {
		Assert.isNotNull(parent);
		fTableViewer= CheckboxTableViewer.newCheckList(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		final GridData data= new GridData(SWT.FILL, SWT.FILL, true, true);
		data.heightHint= 150;
		fTableViewer.getTable().setLayoutData(data);
		fTableViewer.setLabelProvider(new DecoratingLabelProvider(new JavaUILabelProvider(), new ProblemsLabelDecorator()));
		fTableViewer.setContentProvider(new RefactoringProjectContentProvider());
		fTableViewer.setSorter(new JavaElementSorter());
		fTableViewer.addCheckStateListener(new ICheckStateListener() {

			public final void checkStateChanged(final CheckStateChangedEvent event) {
				final Collection collection= Arrays.asList(fTableViewer.getCheckedElements());
				fJarPackageData.setRefactoringProjects((IProject[]) collection.toArray(new IProject[collection.size()]));
			}
		});
		handleProjectsChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	public void finish() {
		final IDialogSettings settings= getDialogSettings();
		if (settings != null) {
			settings.put(STORE_EXPORT_STRUCTURAL_ONLY, fJarPackageData.isExportStructuralOnly());
			settings.put(STORE_EXPORT_DEPRECATION_INFO, fJarPackageData.isDeprecationAware());
		}
	}

	/**
	 * Returns the available projects.
	 * 
	 * @return the available projects
	 */
	protected IProject[] getAvailableProjects() {
		final HashSet set= new HashSet(32);
		final Object[] elements= fJarPackageData.getElements();
		for (int index= 0; index < elements.length; index++) {
			final Object element= elements[index];
			if (element instanceof IAdaptable) {
				final IAdaptable adaptable= (IAdaptable) element;
				final IResource resource= (IResource) adaptable.getAdapter(IResource.class);
				if (resource != null) {
					final IProject project= resource.getProject();
					if (project != null)
						set.add(project);
				}
			}
		}
		final IProject[] projects= new IProject[set.size()];
		set.toArray(projects);
		return projects;
	}

	/**
	 * Handles the projects changed event.
	 */
	protected void handleProjectsChanged() {
		final IProject[] projects= getAvailableProjects();
		fTableViewer.setInput(projects);
		final IProject[] checked= fJarPackageData.getRefactoringProjects();
		if (checked != null && checked.length > 0)
			fTableViewer.setCheckedElements(checked);
		else {
			fTableViewer.setCheckedElements(projects);
			fJarPackageData.setRefactoringProjects(projects);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void setVisible(final boolean visible) {
		handleProjectsChanged();
		super.setVisible(visible);
	}
}