package org.eclipse.jdt.internal.ui.launcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.viewers.ViewerSorter;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.launching.ProjectSourceLocator;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.launcher.LauncherMessages;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.CheckedListDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.swt.MGridLayout;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.*;

public class SourceLookupBlock {
	
	private IJavaProject fJavaProject;
	
	private SelectionButtonDialogField fUseDefaultRadioButton;
	private SelectionButtonDialogField fUseDefinedRadioButton;
	private CheckedListDialogField fProjectList;
	
	private Control fSWTControl;

	private class SourceLookupAdapter implements IListAdapter, IDialogFieldListener {
		public void customButtonPressed(DialogField field, int index) {
		}

		public void selectionChanged(DialogField field) {
		}

		public void dialogFieldChanged(DialogField field) {
			buttonPressed();
		}
	}
	
	public SourceLookupBlock(IJavaProject project) {
		fJavaProject= project;
		
		SourceLookupAdapter adapter= new SourceLookupAdapter();
		
		fUseDefaultRadioButton= new SelectionButtonDialogField(SWT.RADIO);
		fUseDefaultRadioButton.setDialogFieldListener(adapter);
		fUseDefaultRadioButton.setLabelText(LauncherMessages.getString("SourceLookupBlock.default.label"));

		fUseDefinedRadioButton= new SelectionButtonDialogField(SWT.RADIO);
		fUseDefinedRadioButton.setDialogFieldListener(adapter);
		fUseDefinedRadioButton.setLabelText(LauncherMessages.getString("SourceLookupBlock.defined.label"));

		String[] buttonLabels= new String[] {
			/* 0 */ LauncherMessages.getString("SourceLookupBlock.projects.checkall"),
			/* 1 */ LauncherMessages.getString("SourceLookupBlock.projects.uncheckall"),
			/* 2 */ null,
			/* 3 */ LauncherMessages.getString("SourceLookupBlock.projects.up"),
			/* 4 */ LauncherMessages.getString("SourceLookupBlock.projects.down"),
		};
		
		fProjectList= new CheckedListDialogField(adapter, buttonLabels, new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_BASICS));
		fProjectList.setLabelText(LauncherMessages.getString("SourceLookupBlock.projects.label"));
		fProjectList.setCheckAllButtonIndex(0);
		fProjectList.setUncheckAllButtonIndex(1);
		fProjectList.setUpButtonIndex(3);
		fProjectList.setDownButtonIndex(4);
		
		initializeFields();
	}

	public void initializeFields() {
		ArrayList projects= new ArrayList();
		ArrayList checked= new ArrayList();
		boolean useClasspath= true;
	
		try {
			IJavaProject[] customLookup= ProjectSourceLocator.getSourceLookupPath(fJavaProject);
			if (customLookup != null) {
				projects.addAll(Arrays.asList(customLookup));
				checked.addAll(Arrays.asList(customLookup));
				useClasspath= false;
			} else {
				getProjectsFromClaspath(fJavaProject, projects);
			}
			checked= new ArrayList(projects);
			IJavaProject[] allProjects= fJavaProject.getJavaModel().getJavaProjects();
			for (int i= 0; i < allProjects.length; i++) {
				IJavaProject curr= allProjects[i];
				if (!projects.contains(curr)) {
					projects.add(curr);
				}
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
		fUseDefaultRadioButton.setSelection(useClasspath);
		fUseDefinedRadioButton.setSelection(!useClasspath);
		fProjectList.setElements(projects);
		fProjectList.setCheckedElements(checked);
	}

	/*
	 * Create the content.
	 */
	public Control createControl(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		
		MGridLayout layout= new MGridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 2;
		composite.setLayout(layout);
		
		fUseDefaultRadioButton.doFillIntoGrid(composite, 2);
		fUseDefinedRadioButton.doFillIntoGrid(composite, 2);
		
		fProjectList.doFillIntoGrid(composite, 3);
		LayoutUtil.setHorizontalSpan(fProjectList.getLabelControl(null), 2);
		
		fSWTControl= composite;
		return composite;
	}

	
	private Shell getShell() {
		if (fSWTControl != null) {
			return fSWTControl.getShell();
		}
		return JavaPlugin.getActiveWorkbenchShell();
	}
		
	
	private void buttonPressed() {
		fProjectList.setEnabled(!fUseDefaultRadioButton.isSelected());
	}
	
	private void getProjectsFromClaspath(IJavaProject project, ArrayList res) throws JavaModelException {
		if (res.contains(project)) {
			return;
		}
		res.add(project);
		IJavaModel model= project.getJavaModel();
		
		IClasspathEntry[] entries= project.getRawClasspath();
		for (int i= 0; i < entries.length; i++) {
			IClasspathEntry curr= entries[i];
			if (curr.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
				IJavaProject jproject= model.getJavaProject(curr.getPath().lastSegment());
				if (jproject.exists()) {
					getProjectsFromClaspath(jproject, res);
				}
			}
		}
	}
	
	
	public void applyChanges() throws JavaModelException {
		if (fUseDefaultRadioButton.isSelected()) {
			ProjectSourceLocator.setSourceLookupPath(fJavaProject, null);
		} else {
			List projects= fProjectList.getCheckedElements();
			ProjectSourceLocator.setSourceLookupPath(fJavaProject, (IJavaProject[]) projects.toArray(new IJavaProject[projects.size()]));
		}	
	}
	

}

