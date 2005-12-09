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

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;

import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
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
public final class JarPackageRefactoringPage extends WizardPage implements IJarPackageWizardPage {

	/** Content provider for projects whose refactorings are exported */
	private static class RefactoringProjectContentProvider extends WorkbenchContentProvider {

		/** The no children constant */
		private static final Object[] NO_CHILDREN= {};

		/*
		 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
		 */
		public Object[] getElements(Object element) {
			if (element instanceof IProject[])
				return (Object[]) element;
			return NO_CHILDREN;
		}
	}

	/** Listener for the time combo boxes */
	private class TimeComboBoxListener extends SelectionAdapter implements ModifyListener {

		/*
		 * @see org.eclipse.swt.events.ModifyListener#modifyText(org.eclipse.swt.events.ModifyEvent)
		 */
		public void modifyText(final ModifyEvent event) {
			try {
				handleTimeChanged();
			} catch (NumberFormatException exception) {
				fJarPackageData.setHistoryStart(0);
			}
		}

		/*
		 * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
		 */
		public void widgetSelected(final SelectionEvent event) {
			try {
				handleTimeChanged();
			} catch (NumberFormatException exception) {
				fJarPackageData.setHistoryStart(0);
			}
		}
	}

	/** Description */
	private static final int MONTH_OFFSET= -1;

	/** The page name */
	private final static String PAGE_NAME= "jarRefactoringsWizardPage"; //$NON-NLS-1$

	/** The export structual only dialog settings store */
	private static final String STORE_EXPORT_STRUCTURAL_ONLY= PAGE_NAME + ".EXPORT_STRUCTURAL_ONLY"; //$NON-NLS-1$

	/** The date label */
	private Label fDateLabel;

	/** The day combo */
	private Combo fDayCombo;

	/** The hour combo */
	private Combo fHourCombo;

	/** The jar package data */
	private final JarPackageData fJarPackageData;

	/** The minute combo */
	private Combo fMinuteCombo;

	/** The month combo */
	private Combo fMonthCombo;

	/** The projects table viewer */
	private CheckboxTableViewer fTableViewer;

	/** The time label */
	private Label fTimeLabel;

	/** The year combo */
	private Combo fYearCombo;

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
		setTitle(JarPackagerMessages.RefactoringSelectionPage_title);
		setDescription(JarPackagerMessages.RefactoringSelectionPage_description);
	}

	/*
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
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
		label.setText(JarPackagerMessages.RefactoringSelectionPage_viewer_caption);
		createProjectTable(composite);
		createExportStructuralButton(composite);
		createExportSinceButton(composite);
		createDateArea(composite);
		createTimeArea(composite);
		handleTimeChanged();
		final TimeComboBoxListener listener= new TimeComboBoxListener();
		fMonthCombo.addSelectionListener(listener);
		fDayCombo.addSelectionListener(listener);
		fYearCombo.addModifyListener(listener);
		fHourCombo.addSelectionListener(listener);
		fMinuteCombo.addSelectionListener(listener);
		setControl(composite);
		Dialog.applyDialogFont(composite);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IJavaHelpContextIds.JARREFACTORING_WIZARD_PAGE);
	}

	/**
	 * Creates the date area of this page.
	 * 
	 * @param parent
	 *            the parent control
	 */
	protected void createDateArea(final Composite parent) {
		Assert.isNotNull(parent);

		final Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(4, false));
		final GridData data= new GridData(SWT.FILL, SWT.FILL, false, false);
		data.horizontalIndent= IDialogConstants.HORIZONTAL_MARGIN;
		composite.setLayoutData(data);

		fDateLabel= new Label(composite, SWT.NONE);
		fDateLabel.setText(JarPackagerMessages.RefactoringSelectionPage_date_label);

		fMonthCombo= new Combo(composite, SWT.READ_ONLY);
		fDayCombo= new Combo(composite, SWT.READ_ONLY);
		fDayCombo.setTextLimit(2);
		fYearCombo= new Combo(composite, SWT.READ_ONLY);
		fYearCombo.setTextLimit(4);

		final String days[]= new String[31];
		for (int index= 0; index < 31; index++)
			days[index]= String.valueOf(index + 1);

		final String months[]= new String[12];
		final SimpleDateFormat format= new SimpleDateFormat("MMMM"); //$NON-NLS-1$
		Calendar calendar= Calendar.getInstance();
		for (int index= 0; index < 12; index++) {
			calendar.set(Calendar.MONTH, index);
			months[index]= format.format(calendar.getTime());
		}

		final String years[]= new String[10];
		calendar= Calendar.getInstance();
		for (int index= 0; index < 10; index++)
			years[index]= String.valueOf(calendar.get(Calendar.YEAR) - index);

		fDayCombo.setItems(days);
		fMonthCombo.setItems(months);
		fYearCombo.setItems(years);

		final long stamp= fJarPackageData.getHistoryStart();
		if (stamp > 0)
			calendar.setTimeInMillis(stamp);
		else {
			calendar.add(Calendar.MONTH, MONTH_OFFSET);
			fYearCombo.setEnabled(false);
			fMonthCombo.setEnabled(false);
			fDayCombo.setEnabled(false);
			fDateLabel.setEnabled(false);
		}
		updateDate(calendar);
	}

	/**
	 * Creates the export since button of this page.
	 * 
	 * @param parent
	 *            the parent control
	 */
	protected void createExportSinceButton(final Composite parent) {
		Assert.isNotNull(parent);
		final Button button= new Button(parent, SWT.CHECK);
		final GridData data= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		data.verticalIndent= IDialogConstants.VERTICAL_SPACING;
		button.setText(JarPackagerMessages.JarPackageRefactoringPage_export_caption);
		button.setLayoutData(data);
		button.setSelection(fJarPackageData.getHistoryStart() > 0);
		button.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(final SelectionEvent event) {
				final boolean selected= button.getSelection();
				fYearCombo.setEnabled(selected);
				fMonthCombo.setEnabled(selected);
				fDayCombo.setEnabled(selected);
				fDateLabel.setEnabled(selected);
				fHourCombo.setEnabled(selected);
				fMinuteCombo.setEnabled(selected);
				fTimeLabel.setEnabled(selected);
				if (!selected) {
					fJarPackageData.setHistoryStart(0);
					fJarPackageData.setHistoryEnd(Long.MAX_VALUE - 1);
				} else {
					final Calendar calendar= Calendar.getInstance();
					calendar.add(Calendar.MONTH, MONTH_OFFSET);
					updateDate(calendar);
					updateTime(calendar);
				}
			}
		});
	}

	/**
	 * Creates the export structural refactorings button of this page.
	 * 
	 * @param parent
	 *            the parent control
	 */
	protected void createExportStructuralButton(final Composite parent) {
		Assert.isNotNull(parent);
		final Button button= new Button(parent, SWT.CHECK);
		button.setText(JarPackagerMessages.JarPackageRefactoringPage_export_label);
		button.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		button.setSelection(fJarPackageData.isExportStructuralOnly());
		button.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(final SelectionEvent event) {
				fJarPackageData.setExportStructuralOnly(button.getSelection());
			}
		});
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
	 * Creates the time area of this page.
	 * 
	 * @param parent
	 *            the parent control
	 */
	protected void createTimeArea(final Composite parent) {
		Assert.isNotNull(parent);
		final Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(4, false));
		final GridData data= new GridData(SWT.FILL, SWT.FILL, false, false);
		data.horizontalIndent= IDialogConstants.HORIZONTAL_MARGIN;
		composite.setLayoutData(data);

		fTimeLabel= new Label(composite, SWT.NONE);
		fTimeLabel.setText(JarPackagerMessages.RefactoringSelectionPage_time_label);

		fHourCombo= new Combo(composite, SWT.READ_ONLY);
		fHourCombo.setTextLimit(2);
		fMinuteCombo= new Combo(composite, SWT.READ_ONLY);
		fMinuteCombo.setTextLimit(2);

		final String hours[]= new String[24];
		for (int index= 0; index < 24; index++)
			hours[index]= String.valueOf(index);

		final String minutes[]= new String[60];
		for (int index= 0; index < 60; index++) {
			final String string= String.valueOf(index);
			if (index < 10)
				minutes[index]= "0" + string; //$NON-NLS-1$
			else
				minutes[index]= String.valueOf(index);
		}

		fHourCombo.setItems(hours);
		fMinuteCombo.setItems(minutes);

		final Calendar calendar= Calendar.getInstance();
		final long stamp= fJarPackageData.getHistoryStart();
		if (stamp > 0)
			calendar.setTimeInMillis(stamp);
		else {
			fHourCombo.setEnabled(false);
			fMinuteCombo.setEnabled(false);
			fTimeLabel.setEnabled(false);
		}
		updateTime(calendar);
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.jarpackager.IJarPackageWizardPage#finish()
	 */
	public void finish() {
		final IDialogSettings settings= getDialogSettings();
		if (settings != null)
			settings.put(STORE_EXPORT_STRUCTURAL_ONLY, fJarPackageData.isExportStructuralOnly());
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
	 * Handles the time changed event.
	 * 
	 * @throws NumberFormatException
	 *             if the selected time cannot be determined
	 */
	protected void handleTimeChanged() throws NumberFormatException {
		final Calendar calendar= Calendar.getInstance();
		calendar.set(Integer.parseInt(String.valueOf(fYearCombo.getText())), fMonthCombo.getSelectionIndex(), Integer.parseInt(String.valueOf(fDayCombo.getText())), fHourCombo.getSelectionIndex(), fMinuteCombo.getSelectionIndex(), 0);
		fJarPackageData.setHistoryStart(calendar.getTimeInMillis());
	}

	/*
	 * @see org.eclipse.jface.dialogs.DialogPage#setVisible(boolean)
	 */
	public void setVisible(final boolean visible) {
		handleProjectsChanged();
		super.setVisible(visible);
	}

	/**
	 * Updates the date from the specified calendar.
	 * 
	 * @param calendar
	 *            the calendar
	 */
	private void updateDate(final Calendar calendar) {
		fDayCombo.select(calendar.get(Calendar.DATE) - 1);
		fMonthCombo.select(calendar.get(Calendar.MONTH));
		final String value= String.valueOf(calendar.get(Calendar.YEAR));
		int index= fYearCombo.indexOf(value);
		if (index == -1) {
			fYearCombo.add(value);
			index= fYearCombo.indexOf(value);
		}
		fYearCombo.select(index);
	}

	/**
	 * Updates the time from the specified calendar.
	 * 
	 * @param calendar
	 *            the calendar
	 */
	private void updateTime(final Calendar calendar) {
		fHourCombo.select(calendar.get(Calendar.HOUR_OF_DAY));
		fMinuteCombo.select(calendar.get(Calendar.MINUTE));
	}
}
