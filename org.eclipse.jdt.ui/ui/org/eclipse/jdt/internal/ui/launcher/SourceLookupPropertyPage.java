package org.eclipse.jdt.internal.ui.launcher;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;

import org.eclipse.ui.dialogs.ListSelectionDialog;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.viewsupport.ListContentProvider;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;
import org.eclipse.jdt.internal.ui.wizards.swt.MGridData;
import org.eclipse.jdt.internal.ui.wizards.swt.MGridLayout;
import org.eclipse.jdt.launching.ProjectSourceLocator;

public class SourceLookupPropertyPage extends JavaProjectPropertyPage {
	private ListDialogField fListField;
	private Button fUseBuildClasspath;
	private Button fUseCustomLookup;
	private boolean fFirst= true;
	
	private static final String PREFIX= "SourceLookupPropertyPage.";
	private static final String ADD_PROJECT= PREFIX+"add_project.label";
	private static final String UP= PREFIX+"up.label";
	private static final String DOWN= PREFIX+"down.label";
	private static final String REMOVE= PREFIX+"remove.label";
	private static final String USE_BUILDPATH= PREFIX+"use_buildpath.label";
	private static final String USE_CUSTOM= PREFIX+"use_custom.label";
	private static final String ADD_PROJECT_MESSAGE= PREFIX+"add_project.message";
	private static final String JM_EXCEPTION= PREFIX+"javamodel.error.";
	
	class ListAdapter implements IListAdapter {
		public void customButtonPressed(DialogField field, int index) {
			addProject();
		}
		public void selectionChanged(DialogField field) {
		}
	}
	
	public SourceLookupPropertyPage() {
	}

	public Control createJavaContents(Composite ancestor) {
		fListField= new ListDialogField(new ListAdapter(),
				new String[] { JavaLaunchUtils.getResourceString(ADD_PROJECT) },
				new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_BASICS), 
				ListDialogField.UPDOWN);
				
		fListField.setUpButtonLabel(JavaLaunchUtils.getResourceString(UP));
		fListField.setDownButtonLabel(JavaLaunchUtils.getResourceString(DOWN));
		fListField.setRemoveButtonLabel(JavaLaunchUtils.getResourceString(REMOVE) );

		Group parent= new Group(ancestor, SWT.NONE);
		MGridLayout gl= new MGridLayout();
		gl.numColumns= 3;
		parent.setLayout(gl);
		
		fUseBuildClasspath= new Button(parent, SWT.RADIO);
		fUseBuildClasspath.setText(JavaLaunchUtils.getResourceString(USE_BUILDPATH));
		MGridData gd= new MGridData();
		gd.horizontalSpan= 3;
		fUseBuildClasspath.setLayoutData(gd);
		
		fUseCustomLookup= new Button(parent, SWT.RADIO);
		fUseCustomLookup.setText(JavaLaunchUtils.getResourceString(USE_CUSTOM));
		gd= new MGridData();
		gd.horizontalSpan= 3;
		fUseCustomLookup.setLayoutData(gd);
		fUseCustomLookup.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				enableCustomLookup(fUseCustomLookup.getSelection());
			}
		});
		
		fListField.doFillIntoGrid(parent, 3);
		
		return parent;
	}
	
	public void setVisible(boolean visible) {
		IJavaProject p= getJavaProject();
		if (p != null && fFirst && visible) {
			fFirst= false;
			initialSetup(p);
		}
	}
	
	private void initialSetup(IJavaProject project) {
		List lookup= null;
		IJavaProject[] customLookup= null;
		try {
			customLookup= ProjectSourceLocator.getSourceLookupPath(project);
		} catch (JavaModelException e) {
		}
		if (customLookup != null) {
			lookup= new ArrayList();
			for (int i= 0; i < customLookup.length; i++) {
				lookup.add(customLookup[i]);
			}
			enableCustomLookup(true);
		} else {
			lookup= computeDefaultLookup(project);
			enableCustomLookup(false);
		}
		fListField.setElements(lookup);
	}

	protected void performDefaults() {
		fListField.setElements(computeDefaultLookup(getJavaProject()));
		enableCustomLookup(false);
		super.performDefaults();
	}

	public boolean performOk() {
		IJavaProject p= getJavaProject();
		if (p == null)
			return true;
		try {
			if (fUseBuildClasspath.getSelection()) {
				ProjectSourceLocator.setSourceLookupPath(p, null);
			} else {
				List lookup= fListField.getElements();
				IJavaProject[] projects= new IJavaProject[lookup.size()];
				ProjectSourceLocator.setSourceLookupPath(p, (IJavaProject[])lookup.toArray(projects));
			}
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, getShell(), JavaLaunchUtils.getResourceBundle(), JM_EXCEPTION);
		}
		return true;
	}
	
	private void enableCustomLookup(boolean enable) {
		fListField.setEnabled(enable);
		if (enable) {
			fUseBuildClasspath.setSelection(false);
			fUseCustomLookup.setSelection(true);
		} else {
			fUseBuildClasspath.setSelection(true);
			fUseCustomLookup.setSelection(false);
		}
	}
	
	
	private List computeDefaultLookup(IJavaProject project) {
		ArrayList lookup= new ArrayList();
		lookup.add(project);
		
		try {
			List referenced= getProjectsOnClassPath(project);
			lookup.addAll(referenced);
		} catch (JavaModelException e) {
		}
		return lookup;
	}
	
	private List getProjectsOnClassPath(IJavaProject project) throws JavaModelException {
		IJavaModel jm= project.getJavaModel();
		IClasspathEntry[] cp= project.getClasspath();
		ArrayList l= new ArrayList();
		
		for (int i= 0; i < cp.length; i++) {
			if (cp[i].getEntryKind() == IClasspathEntry.CPE_PROJECT) {
				IJavaProject p= jm.getJavaProject(cp[i].getPath().lastSegment());
				l.add(p);
			}
		}
		return l;
	}
	
	private void addProject() {
		List unusedProjects= getUnusedProjects();
		ListSelectionDialog dialog= new ListSelectionDialog(getShell(), unusedProjects, 
			new ListContentProvider(), 
			new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_BASICS), 
			JavaLaunchUtils.getResourceString(ADD_PROJECT_MESSAGE));
		if (dialog.open() == dialog.OK) {
			Object[] result= dialog.getResult();
			for (int i= 0; i < result.length; i++) {
				fListField.addElement(result[i]);
			}
		}
	}
	
	private List getUnusedProjects() {
		ArrayList unused= new ArrayList();
		try {
			IJavaProject[] allProjects= getJavaProject().getJavaModel().getJavaProjects();
			List used= fListField.getElements();
			for (int i= 0; i < allProjects.length; i++) {
				if (!used.contains(allProjects[i]))
					unused.add(allProjects[i]);
			}
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, getShell(), JavaLaunchUtils.getResourceBundle(), JM_EXCEPTION);
		}
		return unused;
	}
}