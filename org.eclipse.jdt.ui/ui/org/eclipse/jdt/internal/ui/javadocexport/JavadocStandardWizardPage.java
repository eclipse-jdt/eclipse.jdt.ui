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

package org.eclipse.jdt.internal.ui.javadocexport;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ViewerSorter;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusDialog;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.preferences.JavadocConfigurationBlock;
import org.eclipse.jdt.internal.ui.util.SWTUtil;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.CheckedListDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;

public class JavadocStandardWizardPage extends JavadocWizardPage {
	
	
	private final int STYLESHEETSTATUS= 1;
	private final int LINK_REFERENCES= 2;
	
	private JavadocOptionsManager fStore;
	private Composite fUpperComposite;

	private Group fBasicOptionsGroup;
	private Group fTagsGroup;

	private Button fTitleButton;
	private Text fTitleText;
	private Text fStyleSheetText;
	private FlaggedButton fDeprecatedList;
	private FlaggedButton fDeprecatedCheck;
	private FlaggedButton fIndexCheck;
	private FlaggedButton fSeperatedIndexCheck;
	private Button fStyleSheetBrowseButton;
	private Button fStyleSheetButton;

	private CheckedListDialogField fListDialogField;

	private StatusInfo fStyleSheetStatus;
	private StatusInfo fLinkRefStatus;
	
	private ArrayList fButtonsList;
	private JavadocTreeWizardPage fFirstPage;


	public JavadocStandardWizardPage(String pageName, JavadocTreeWizardPage firstPage, JavadocOptionsManager store) {
		super(pageName);
		fFirstPage= firstPage;
		setDescription(JavadocExportMessages.getString("JavadocStandardWizardPage.description")); //$NON-NLS-1$

		fStore= store;
		fButtonsList= new ArrayList();
		fStyleSheetStatus= new StatusInfo();
		fLinkRefStatus= new StatusInfo();
	}
	/*
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {

		initializeDialogUnits(parent);

		fUpperComposite= new Composite(parent, SWT.NONE);
		fUpperComposite.setLayoutData(createGridData(GridData.FILL_VERTICAL | GridData.FILL_HORIZONTAL, 1, 0));

		GridLayout layout= createGridLayout(4);
		layout.marginHeight= 0;
		fUpperComposite.setLayout(layout);

		createBasicOptionsGroup(fUpperComposite);
		createTagOptionsGroup(fUpperComposite);
		createListDialogField(fUpperComposite);
		createStyleSheetGroup(fUpperComposite);

		setControl(fUpperComposite);
		Dialog.applyDialogFont(fUpperComposite);
		WorkbenchHelp.setHelp(fUpperComposite, IJavaHelpContextIds.JAVADOC_STANDARD_PAGE);
	}
	private void createBasicOptionsGroup(Composite composite) {

		fTitleButton= createButton(composite, SWT.CHECK, JavadocExportMessages.getString("JavadocStandardWizardPage.titlebutton.label"), createGridData(1)); //$NON-NLS-1$
		fTitleText= createText(composite, SWT.SINGLE | SWT.BORDER, null, createGridData(GridData.FILL_HORIZONTAL, 3, 0));
		String text= fStore.getTitle();
		if (!text.equals("")) { //$NON-NLS-1$
			fTitleText.setText(text);
			fTitleButton.setSelection(true);
		} else
			fTitleText.setEnabled(false);

		fBasicOptionsGroup= new Group(composite, SWT.SHADOW_ETCHED_IN);
		fBasicOptionsGroup.setLayout(createGridLayout(1));
		fBasicOptionsGroup.setLayoutData(createGridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL, 2, 0));
		fBasicOptionsGroup.setText(JavadocExportMessages.getString("JavadocStandardWizardPage.basicgroup.label")); //$NON-NLS-1$

		new FlaggedButton(fBasicOptionsGroup, JavadocExportMessages.getString("JavadocStandardWizardPage.usebutton.label"), new GridData(GridData.FILL_HORIZONTAL), fStore.USE, true); //$NON-NLS-1$
		new FlaggedButton(fBasicOptionsGroup, JavadocExportMessages.getString("JavadocStandardWizardPage.hierarchybutton.label"), new GridData(GridData.FILL_HORIZONTAL), fStore.NOTREE, false); //$NON-NLS-1$
		new FlaggedButton(fBasicOptionsGroup, JavadocExportMessages.getString("JavadocStandardWizardPage.navigartorbutton.label"), new GridData(GridData.FILL_HORIZONTAL), fStore.NONAVBAR, false); //$NON-NLS-1$

		fIndexCheck= new FlaggedButton(fBasicOptionsGroup, JavadocExportMessages.getString("JavadocStandardWizardPage.indexbutton.label"), new GridData(GridData.FILL_HORIZONTAL), fStore.NOINDEX, false); //$NON-NLS-1$

		fSeperatedIndexCheck= new FlaggedButton(fBasicOptionsGroup, JavadocExportMessages.getString("JavadocStandardWizardPage.seperateindexbutton.label"), createGridData(GridData.GRAB_HORIZONTAL, 1, convertWidthInCharsToPixels(3)), fStore.SPLITINDEX, true); //$NON-NLS-1$
		fSeperatedIndexCheck.getButton().setEnabled(fIndexCheck.getButton().getSelection());

		fIndexCheck.getButton().addSelectionListener(new ToggleSelectionAdapter(new Control[] { fSeperatedIndexCheck.getButton()}));
		fTitleButton.addSelectionListener(new ToggleSelectionAdapter(new Control[] { fTitleText }));

	}

	private void createTagOptionsGroup(Composite composite) {
		fTagsGroup= new Group(composite, SWT.SHADOW_ETCHED_IN);
		fTagsGroup.setLayout(createGridLayout(1));
		fTagsGroup.setLayoutData(createGridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL, 2, 0));
		fTagsGroup.setText(JavadocExportMessages.getString("JavadocStandardWizardPage.tagsgroup.label")); //$NON-NLS-1$

		new FlaggedButton(fTagsGroup, JavadocExportMessages.getString("JavadocStandardWizardPage.authorbutton.label"), new GridData(GridData.FILL_HORIZONTAL), fStore.AUTHOR, true); //$NON-NLS-1$
		new FlaggedButton(fTagsGroup, JavadocExportMessages.getString("JavadocStandardWizardPage.versionbutton.label"), new GridData(GridData.FILL_HORIZONTAL), fStore.VERSION, true); //$NON-NLS-1$
		fDeprecatedCheck= new FlaggedButton(fTagsGroup, JavadocExportMessages.getString("JavadocStandardWizardPage.deprecatedbutton.label"), new GridData(GridData.FILL_HORIZONTAL), fStore.NODEPRECATED, false); //$NON-NLS-1$
		fDeprecatedList= new FlaggedButton(fTagsGroup, JavadocExportMessages.getString("JavadocStandardWizardPage.deprecatedlistbutton.label"), createGridData(GridData.FILL_HORIZONTAL, 1, convertWidthInCharsToPixels(3)), fStore.NODEPRECATEDLIST, false); //$NON-NLS-1$
		fDeprecatedList.getButton().setEnabled(fDeprecatedCheck.getButton().getSelection());

		fDeprecatedCheck.getButton().addSelectionListener(new ToggleSelectionAdapter(new Control[] { fDeprecatedList.getButton()}));
	} //end createTagOptionsGroup

	private void createStyleSheetGroup(Composite composite) {
		Composite c= new Composite(composite, SWT.NONE);
		c.setLayout(createGridLayout(3));
		c.setLayoutData(createGridData(GridData.FILL_HORIZONTAL, 4, 0));
		((GridLayout) c.getLayout()).marginWidth= 0;

		fStyleSheetButton= createButton(c, SWT.CHECK, JavadocExportMessages.getString("JavadocStandardWizardPage.stylesheettext.label"), createGridData(1)); //$NON-NLS-1$
		fStyleSheetText= createText(c, SWT.SINGLE | SWT.BORDER, null, createGridData(GridData.FILL_HORIZONTAL, 1, 0));
		//there really aught to be a way to specify this
		 ((GridData) fStyleSheetText.getLayoutData()).widthHint= 200;
		fStyleSheetBrowseButton= createButton(c, SWT.PUSH, JavadocExportMessages.getString("JavadocStandardWizardPage.stylesheetbrowsebutton.label"), createGridData(GridData.HORIZONTAL_ALIGN_END, 1, 0)); //$NON-NLS-1$
		SWTUtil.setButtonDimensionHint(fStyleSheetBrowseButton);

		String str= fStore.getStyleSheet();
		if (str.equals("")) { //$NON-NLS-1$
			//default
			fStyleSheetText.setEnabled(false);
			fStyleSheetBrowseButton.setEnabled(false);
		} else {
			fStyleSheetButton.setSelection(true);
			fStyleSheetText.setText(str);
		}

		//Listeners
		fStyleSheetButton.addSelectionListener(new ToggleSelectionAdapter(new Control[] { fStyleSheetText, fStyleSheetBrowseButton }) {
			public void validate() {
				doValidation(STYLESHEETSTATUS);
			}
		});

		fStyleSheetText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				doValidation(STYLESHEETSTATUS);
			}
		});

		fStyleSheetBrowseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				handleFileBrowseButtonPressed(fStyleSheetText, new String[] { "*.css" }, JavadocExportMessages.getString("JavadocSpecificsWizardPage.stylesheetbrowsedialog.title")); //$NON-NLS-1$ //$NON-NLS-2$
			}
		});

	}

	private void createListDialogField(Composite composite) {
		Composite c= new Composite(composite, SWT.NONE);
		c.setLayout(createGridLayout(3));
		c.setLayoutData(createGridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL, 4, 0));
		((GridLayout) c.getLayout()).marginWidth= 0;

		String[] buttonlabels= new String[] { JavadocExportMessages.getString("JavadocStandardWizardPage.selectallbutton.label"), JavadocExportMessages.getString("JavadocStandardWizardPage.clearallbutton.label"), JavadocExportMessages.getString("JavadocStandardWizardPage.configurebutton.label")}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		JavadocLinkDialogLabelProvider labelProvider= new JavadocLinkDialogLabelProvider();
		
		ListAdapter adapter= new ListAdapter();
		
		fListDialogField= new CheckedListDialogField(adapter, buttonlabels, labelProvider);
		fListDialogField.setDialogFieldListener(adapter);
		fListDialogField.setCheckAllButtonIndex(0);
		fListDialogField.setUncheckAllButtonIndex(1);
		fListDialogField.setViewerSorter(new ViewerSorter());

		createLabel(c, SWT.NONE, JavadocExportMessages.getString("JavadocStandardWizardPage.referencedclasses.label"), createGridData(GridData.HORIZONTAL_ALIGN_BEGINNING, 4, 0)); //$NON-NLS-1$
		fListDialogField.doFillIntoGrid(c, 3);

		LayoutUtil.setHorizontalGrabbing(fListDialogField.getListControl(null));

		fListDialogField.enableButton(2, false);
	}

	private List getCheckedReferences(JavadocLinkRef[] referencesClasses) {
		List checkedElements= new ArrayList();
		
		String hrefs[]= fStore.getHRefs();
		if (hrefs.length > 0) {
			HashSet set= new HashSet();
			for (int i= 0; i < hrefs.length; i++) {
				set.add(hrefs[i]);
			}
			for (int i = 0; i < referencesClasses.length; i++) {
				JavadocLinkRef curr= referencesClasses[i];
				URL url= curr.getURL();
				if (url != null && set.contains(url.toExternalForm())) {
					checkedElements.add(curr);
				}
			}
		}
		return checkedElements;
	}
	

	
	/**
	 * Returns IJavaProjects and IPaths that will be on the classpath  
	 */
	private JavadocLinkRef[] getReferencedElements(IJavaProject[] checkedProjects) {
		HashSet result= new HashSet();
		for (int i= 0; i < checkedProjects.length; i++) {
			IJavaProject project= checkedProjects[i];
			try {
				collectReferencedElements(project, result);
			} catch (CoreException e) {
				JavaPlugin.log(e);
				// ignore
			}
		}
		return (JavadocLinkRef[]) result.toArray(new JavadocLinkRef[result.size()]);	
	}
	
	private void collectReferencedElements(IJavaProject project, HashSet result) throws CoreException {
		IRuntimeClasspathEntry[] unresolved = JavaRuntime.computeUnresolvedRuntimeClasspath(project);
		for (int i= 0; i < unresolved.length; i++) {
			IRuntimeClasspathEntry curr= unresolved[i];
			if (curr.getType() == IRuntimeClasspathEntry.PROJECT) {
				result.add(new JavadocLinkRef(JavaCore.create((IProject) curr.getResource())));
			} else {
				IRuntimeClasspathEntry[] entries= JavaRuntime.resolveRuntimeClasspathEntry(curr, project);
				for (int k = 0; k < entries.length; k++) {
					IRuntimeClasspathEntry entry= entries[k];
					if (entry.getType() == IRuntimeClasspathEntry.PROJECT) {
						result.add(new JavadocLinkRef(JavaCore.create((IProject) entry.getResource())));
					} else if (entry.getType() == IRuntimeClasspathEntry.ARCHIVE) {
						result.add(new JavadocLinkRef(entry.getPath()));
					}
				}
			}
		}
	}
	
	final void doValidation(int VALIDATE) {
		File file= null;
		String ext= null;
		Path path= null;

		switch (VALIDATE) {
			case STYLESHEETSTATUS :
				fStyleSheetStatus= new StatusInfo();
				if (fStyleSheetButton.getSelection()) {
					path= new Path(fStyleSheetText.getText());
					file= new File(fStyleSheetText.getText());
					ext= path.getFileExtension();
					if ((file == null) || !file.exists()) {
						fStyleSheetStatus.setError(JavadocExportMessages.getString("JavadocStandardWizardPage.stylesheetnopath.error")); //$NON-NLS-1$
					} else if ((ext == null) || !ext.equalsIgnoreCase("css")) { //$NON-NLS-1$
						fStyleSheetStatus.setError(JavadocExportMessages.getString("JavadocStandardWizardPage.stylesheetnotcss.error")); //$NON-NLS-1$
					}
				}
				break;
			case LINK_REFERENCES:
				fLinkRefStatus= new StatusInfo();
				List list= fListDialogField.getCheckedElements();
				for (int i= 0; i < list.size(); i++) {
					JavadocLinkRef curr= (JavadocLinkRef) list.get(i);
					URL url= curr.getURL();
					if (url == null) {
						fLinkRefStatus.setWarning(JavadocExportMessages.getString("JavadocStandardWizardPage.nolinkref.error")); //$NON-NLS-1$
						break;
					} else if ("jar".equals(url.getProtocol())) { //$NON-NLS-1$
						fLinkRefStatus.setWarning(JavadocExportMessages.getString("JavadocStandardWizardPage.nojarlinkref.error")); //$NON-NLS-1$
						break;					
					}
				}
				break;
		}

		updateStatus(findMostSevereStatus());

	}

	private IStatus findMostSevereStatus() {
		return StatusUtil.getMostSevere(new IStatus[] { fStyleSheetStatus, fLinkRefStatus });
	}

	public void updateStore() {

		if (fTitleButton.getSelection())
			fStore.setTitle(fTitleText.getText());
		else
			fStore.setTitle(""); //$NON-NLS-1$

		//don't store the buttons if they are not enabled
		//this will change when there is a single page aimed at the standard doclet
		if (true) {
			Object[] buttons= fButtonsList.toArray();
			for (int i= 0; i < buttons.length; i++) {
				FlaggedButton button= (FlaggedButton) buttons[i];
				if (button.getButton().getEnabled())
					fStore.setBoolean(button.getFlag(), !(button.getButton().getSelection() ^ button.show()));
				else
					fStore.setBoolean(button.getFlag(), false == button.show());
			}
		}

		if (fStyleSheetText.getEnabled())
			fStore.setStyleSheet(fStyleSheetText.getText());
		else
			fStore.setStyleSheet(""); //$NON-NLS-1$

		fStore.setHRefs(getHRefs());
	}

	private String[] getHRefs() {
		HashSet res= new HashSet();
		List checked= fListDialogField.getCheckedElements();
		for (Iterator iterator= checked.iterator(); iterator.hasNext();) {
			JavadocLinkRef element= (JavadocLinkRef) iterator.next();
			URL url= element.getURL();
			if (url != null) {
				res.add(url.toExternalForm());
			}
		}
		return (String[]) res.toArray(new String[res.size()]);
	}

	//get the links

	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			doValidation(STYLESHEETSTATUS);
			updateHRefList(fFirstPage.getCheckedProjects());
		} else {
			fStore.setHRefs(getHRefs());
		}
	}

	/**
	 * Method will refresh the list of referenced libraries and projects
	 * depended on the projects or elements of projects selected in the
	 * TreeViewer on the JavadocTreeWizardPage.
	 */
	private void updateHRefList(IJavaProject[] checkedProjects) {
		JavadocLinkRef[] res= getReferencedElements(checkedProjects);
		fListDialogField.setElements(Arrays.asList(res));
			
		List checked= getCheckedReferences(res);
		fListDialogField.setCheckedElements(checked);
	}

	public void init() {
		updateStatus(new StatusInfo());
	}

	protected class FlaggedButton {

		private Button fButton;
		private String fFlag;
		private boolean fShowFlag;

		public FlaggedButton(Composite composite, String message, GridData gridData, String flag, boolean show) {
			fFlag= flag;
			fShowFlag= show;
			fButton= createButton(composite, SWT.CHECK, message, gridData);
			fButtonsList.add(this);
			setButtonSettings();
		}

		public Button getButton() {
			return fButton;
		}

		public String getFlag() {
			return fFlag;
		}
		public boolean show() {
			return fShowFlag;
		}

		private void setButtonSettings() {

			fButton.setSelection(!(fStore.getBoolean(fFlag) ^ fShowFlag));
		}

	} //end class FlaggesButton

	private class ListAdapter implements IListAdapter, IDialogFieldListener {

		/**
		 * @see IListAdapter#customButtonPressed(ListDialogField, int)
		 */
		public void customButtonPressed(ListDialogField field, int index) {
			if (index == 2)
				doEditButtonPressed();
		}

		/**
		 * @see IListAdapter#selectionChanged(ListDialogField)
		 */
		public void selectionChanged(ListDialogField field) {
			List selection= fListDialogField.getSelectedElements();
			if (selection.size() != 1) {
				fListDialogField.enableButton(2, false);
			} else {
				fListDialogField.enableButton(2, true);
			}
		}
		
		public void doubleClicked(ListDialogField field) {
			doEditButtonPressed();
		}
		
		public void dialogFieldChanged(DialogField field) {
			doValidation(LINK_REFERENCES);
		}

	}

	/**
	 * Method doEditButtonPressed.
	 */
	private void doEditButtonPressed() {

		List selected= fListDialogField.getSelectedElements();
		if (selected.isEmpty()) {
			return;
		}
		JavadocLinkRef obj= (JavadocLinkRef) selected.get(0);
		if (obj != null) {
			JavadocPropertyDialog jdialog= new JavadocPropertyDialog(getShell(), obj);
			jdialog.open();
		}
	}

	private class JavadocPropertyDialog extends StatusDialog implements IStatusChangeListener {

		private JavadocConfigurationBlock fJavadocConfigurationBlock;
		private JavadocLinkRef fElement;

		public JavadocPropertyDialog(Shell parent, JavadocLinkRef selection) {
			super(parent);
			setTitle(JavadocExportMessages.getString("JavadocStandardWizardPage.javadocpropertydialog.title")); //$NON-NLS-1$

			fElement= selection;
			URL initialLocation= selection.getURL();
			fJavadocConfigurationBlock= new JavadocConfigurationBlock(parent, this, initialLocation, selection.isProjectRef());
		}

		protected Control createDialogArea(Composite parent) {
			Composite composite= (Composite) super.createDialogArea(parent);
			Control inner= fJavadocConfigurationBlock.createContents(composite);
			inner.setLayoutData(new GridData(GridData.FILL_BOTH));
			applyDialogFont(composite);		
			return composite;
		}

		public void statusChanged(IStatus status) {
			updateStatus(status);

		}

		/**
		 * @see Dialog#okPressed()
		 */
		protected void okPressed() {
			URL javadocLocation= fJavadocConfigurationBlock.getJavadocLocation();
			fElement.setURL(javadocLocation);
			
			fListDialogField.refresh();
			doValidation(LINK_REFERENCES);
			super.okPressed();
		}

		/*
		 * @see org.eclipse.jface.window.Window#configureShell(Shell)
		 */
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			WorkbenchHelp.setHelp(newShell, IJavaHelpContextIds.JAVADOC_PROPERTY_DIALOG);
		}
	}
}
