/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.jdt.launching.JavaRuntime;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeItem;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ITreeContentProvider;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.jarpackager.CheckboxTreeAndListGroup;
import org.eclipse.jdt.internal.ui.util.SWTUtil;

public class JavadocTreeWizardPage extends JavadocWizardPage {

	private CheckboxTreeAndListGroup fInputGroup;

	protected IWorkspaceRoot fRoot;
	protected String fWorkspace;

	//private JavadocTreeViewerFilter fFilter;

	protected Text fDestinationText;
	protected Text fDocletText;
	protected Text fDocletTypeText;
	protected Button fStandardButton;
	protected Button fDestinationBrowserButton;
	protected Button fCustomButton;
	protected Button fPrivateVisibility;
	protected Button fProtectedVisibility;
	protected Button fPackageVisibility;
	protected Button fPublicVisibility;
	private Label fDocletLabel;
	private Label fDocletTypeLabel;
	private Label fDestinationLabel;
	private CLabel fDescriptionLabel;

	protected String fVisibilitySelection;
	protected boolean fDocletSelected;

	private JavadocOptionsManager fStore;
	private JavadocWizard fWizard;

	protected StatusInfo fDestinationStatus;
	protected StatusInfo fDocletStatus;
	protected StatusInfo fTreeStatus;
	protected StatusInfo fPreferenceStatus;
	protected StatusInfo fWizardStatus;

	private final int PREFERENCESTATUS= 0;
	private final int CUSTOMSTATUS= 1;
	private final int STANDARDSTATUS= 2;
	private final int TREESTATUS= 3;

	/**
	 * Constructor for JavadocTreeWizardPage.
	 * @param pageName
	 */
	protected JavadocTreeWizardPage(String pageName, JavadocOptionsManager store) {
		super(pageName);
		setDescription(JavadocExportMessages.getString("JavadocTreeWizardPage.javadoctreewizardpage.description")); //$NON-NLS-1$

		fStore= store;

		// Status variables
		fDestinationStatus= new StatusInfo();
		fDocletStatus= new StatusInfo();
		fTreeStatus= new StatusInfo();
		fPreferenceStatus= new StatusInfo();
		fWizardStatus= store.getWizardStatus();
	}

	/*
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);
		fWizard= (JavadocWizard) this.getWizard();

		Composite composite= new Composite(parent, SWT.NONE);
		GridLayout compositeGridLayout= new GridLayout();
		composite.setLayoutData(createGridData(GridData.FILL_BOTH, 0, 0));
		compositeGridLayout.numColumns= 6;
		composite.setLayout(compositeGridLayout);

		createInputGroup(composite);
		createVisibilitySet(composite);
		createOptionsSet(composite);

		setControl(composite);
		Dialog.applyDialogFont(composite);
		WorkbenchHelp.setHelp(composite, IJavaHelpContextIds.JAVADOC_TREE_PAGE);
	}

	protected void createInputGroup(Composite composite) {

		createLabel(composite, SWT.NONE, JavadocExportMessages.getString("JavadocTreeWizardPage.checkboxtreeandlistgroup.label"), createGridData(6)); //$NON-NLS-1$
		Composite c= new Composite(composite, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.numColumns= 1;
		layout.makeColumnsEqualWidth= true;
		c.setLayout(layout);
		c.setLayoutData(createGridData(GridData.FILL_BOTH, 6, 0));

		ITreeContentProvider treeContentProvider= new JavadocProjectContentProvider();
		ITreeContentProvider listContentProvider= new JavadocMemberContentProvider();
		fInputGroup= new CheckboxTreeAndListGroup(c, fStore.getRoot(), treeContentProvider, new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT), listContentProvider, new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT), SWT.NONE, convertWidthInCharsToPixels(60), convertHeightInCharsToPixels(10));

		fInputGroup.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent e) {
				doValidation(TREESTATUS);
				fWizard.removeAllProjects();
				setProjects();
			}
		});

		//the store will contain at least one project in it's list so long as
		//the workspace is not empty.
		if (!fStore.getJavaProjects().isEmpty())
			setTreeChecked(fStore.getSelectedElements(), (IJavaProject) fStore.getJavaProjects().get(0));

		fInputGroup.aboutToOpen();
	}

	private void createVisibilitySet(Composite composite) {

		GridLayout visibilityLayout= createGridLayout(4);
		visibilityLayout.marginHeight= 0;
		visibilityLayout.marginWidth= 0;
		Composite visibilityGroup= new Composite(composite, SWT.NONE);
		visibilityGroup.setLayoutData(createGridData(GridData.FILL_HORIZONTAL, 6, 0));
		visibilityGroup.setLayout(visibilityLayout);

		createLabel(visibilityGroup, SWT.NONE, JavadocExportMessages.getString("JavadocTreeWizardPage.visibilitygroup.label"), createGridData(GridData.FILL_HORIZONTAL, 4, 0)); //$NON-NLS-1$
		fPrivateVisibility= createButton(visibilityGroup, SWT.RADIO, JavadocExportMessages.getString("JavadocTreeWizardPage.privatebutton.label"), createGridData(GridData.FILL_HORIZONTAL, 1, 0)); //$NON-NLS-1$
		fPackageVisibility= createButton(visibilityGroup, SWT.RADIO, JavadocExportMessages.getString("JavadocTreeWizardPage.packagebutton.label"), createGridData(GridData.FILL_HORIZONTAL, 1, 0)); //$NON-NLS-1$
		fProtectedVisibility= createButton(visibilityGroup, SWT.RADIO, JavadocExportMessages.getString("JavadocTreeWizardPage.protectedbutton.label"), createGridData(GridData.FILL_HORIZONTAL, 1, 0)); //$NON-NLS-1$
		fPublicVisibility= createButton(visibilityGroup, SWT.RADIO, JavadocExportMessages.getString("JavadocTreeWizardPage.publicbutton.label"), createGridData(GridData.FILL_HORIZONTAL, 1, 0)); //$NON-NLS-1$

		fDescriptionLabel= new CLabel(visibilityGroup, SWT.LEFT);
		fDescriptionLabel.setLayoutData(createGridData(GridData.FILL_HORIZONTAL, 4, convertWidthInCharsToPixels(3) -  3)); // INDENT of CLabel

		fPrivateVisibility.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (((Button) e.widget).getSelection()) {
					fVisibilitySelection= fStore.PRIVATE;
					fDescriptionLabel.setText(JavadocExportMessages.getString("JavadocTreeWizardPage.privatevisibilitydescription.label")); //$NON-NLS-1$
				}
			}
		});
		fPackageVisibility.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (((Button) e.widget).getSelection()) {
					fVisibilitySelection= fStore.PACKAGE;
					fDescriptionLabel.setText(JavadocExportMessages.getString("JavadocTreeWizardPage.packagevisibledescription.label")); //$NON-NLS-1$
				}
			}
		});
		fProtectedVisibility.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (((Button) e.widget).getSelection()) {
					fVisibilitySelection= fStore.PROTECTED;
					fDescriptionLabel.setText(JavadocExportMessages.getString("JavadocTreeWizardPage.protectedvisibilitydescription.label")); //$NON-NLS-1$
				}
			}
		});

		fPublicVisibility.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (((Button) e.widget).getSelection()) {
					fVisibilitySelection= fStore.PUBLIC;
					fDescriptionLabel.setText(JavadocExportMessages.getString("JavadocTreeWizardPage.publicvisibilitydescription.label")); //$NON-NLS-1$
				}
			}
		});

		setVisibilitySettings();

	}

	protected void setVisibilitySettings() {

		fVisibilitySelection= fStore.getAccess();
		fPrivateVisibility.setSelection(fVisibilitySelection.equals(fStore.PRIVATE));
		if (fPrivateVisibility.getSelection())
			fDescriptionLabel.setText(JavadocExportMessages.getString("JavadocTreeWizardPage.privatevisibilitydescription.label")); //$NON-NLS-1$
		//$NON-NLS-1$
		fProtectedVisibility.setSelection(fVisibilitySelection.equals(fStore.PROTECTED));
		if (fProtectedVisibility.getSelection())
			fDescriptionLabel.setText(JavadocExportMessages.getString("JavadocTreeWizardPage.protectedvisibilitydescription.label")); //$NON-NLS-1$
		//$NON-NLS-1$
		fPackageVisibility.setSelection(fVisibilitySelection.equals(fStore.PACKAGE));
		if (fPackageVisibility.getSelection())
			fDescriptionLabel.setText(JavadocExportMessages.getString("JavadocTreeWizardPage.packagevisibledescription.label")); //$NON-NLS-1$
		//$NON-NLS-1$
		fPublicVisibility.setSelection(fVisibilitySelection.equals(fStore.PUBLIC));
		if (fPublicVisibility.getSelection())
			fDescriptionLabel.setText(JavadocExportMessages.getString("JavadocTreeWizardPage.publicvisibilitydescription.label")); //$NON-NLS-1$
		//$NON-NLS-1$
	}

	private void createOptionsSet(Composite composite) {

		GridLayout optionSetLayout= createGridLayout(3);
		optionSetLayout.marginHeight= 0;
		optionSetLayout.marginWidth= 0;
		Composite optionSetGroup= new Composite(composite, SWT.NONE);
		optionSetGroup.setLayoutData(createGridData(GridData.FILL_BOTH, 6, 0));
		optionSetGroup.setLayout(optionSetLayout);

		fStandardButton= createButton(optionSetGroup, SWT.RADIO, JavadocExportMessages.getString("JavadocTreeWizardPage.standarddocletbutton.label"), createGridData(GridData.HORIZONTAL_ALIGN_FILL, 3, 0)); //$NON-NLS-1$

		GridData gd= new GridData();
		gd.horizontalSpan= 1;
		fDestinationLabel= createLabel(optionSetGroup, SWT.NONE, JavadocExportMessages.getString("JavadocTreeWizardPage.destinationfield.label"), createGridData(GridData.HORIZONTAL_ALIGN_BEGINNING, 1, convertWidthInCharsToPixels(3))); //$NON-NLS-1$
		fDestinationText= createText(optionSetGroup, SWT.SINGLE | SWT.BORDER, null, createGridData(GridData.FILL_HORIZONTAL, 1, 0));
		//there really aught to be a way to specify this
		 ((GridData) fDestinationText.getLayoutData()).widthHint= 200;
		fDestinationText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				doValidation(STANDARDSTATUS);
			}
		});

		fDestinationBrowserButton= createButton(optionSetGroup, SWT.PUSH, JavadocExportMessages.getString("JavadocTreeWizardPage.destinationbrowse.label"), createGridData(GridData.HORIZONTAL_ALIGN_FILL, 1, 0)); //$NON-NLS-1$
		SWTUtil.setButtonDimensionHint(fDestinationBrowserButton);

		//Option to use custom doclet
		fCustomButton= createButton(optionSetGroup, SWT.RADIO, JavadocExportMessages.getString("JavadocTreeWizardPage.customdocletbutton.label"), createGridData(3)); //$NON-NLS-1$

		//For Entering location of custom doclet
		fDocletTypeLabel= createLabel(optionSetGroup, SWT.NONE, JavadocExportMessages.getString("JavadocTreeWizardPage.docletnamefield.label"), createGridData(GridData.HORIZONTAL_ALIGN_FILL, 1, convertWidthInCharsToPixels(3))); //$NON-NLS-1$
		fDocletTypeText= createText(optionSetGroup, SWT.SINGLE | SWT.BORDER, null, createGridData(GridData.HORIZONTAL_ALIGN_FILL, 2, 0));
		fDocletTypeText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				doValidation(CUSTOMSTATUS);
			}

		});

		fDocletLabel= createLabel(optionSetGroup, SWT.NONE, JavadocExportMessages.getString("JavadocTreeWizardPage.docletpathfield.label"), createGridData(GridData.HORIZONTAL_ALIGN_FILL, 1, convertWidthInCharsToPixels(3))); //$NON-NLS-1$
		fDocletText= createText(optionSetGroup, SWT.SINGLE | SWT.BORDER, null, createGridData(GridData.HORIZONTAL_ALIGN_FILL, 2, 0));
		fDocletText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				doValidation(CUSTOMSTATUS);
			}

		});

		//Add Listeners
		fCustomButton.addSelectionListener(new EnableSelectionAdapter(new Control[] { fDocletLabel, fDocletText, fDocletTypeLabel, fDocletTypeText }, new Control[] { fDestinationLabel, fDestinationText, fDestinationBrowserButton }));
		fCustomButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				doValidation(CUSTOMSTATUS);
			}
		});
		fStandardButton.addSelectionListener(new EnableSelectionAdapter(new Control[] { fDestinationLabel, fDestinationText, fDestinationBrowserButton }, new Control[] { fDocletLabel, fDocletText, fDocletTypeLabel, fDocletTypeText }));
		fStandardButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				doValidation(STANDARDSTATUS);
			}
		});
		fDestinationBrowserButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
					String text= handleFolderBrowseButtonPressed(fDestinationText.getText(), fDestinationText.getShell(), JavadocExportMessages.getString("JavadocTreeWizardPage.destinationbrowsedialog.title"), //$NON-NLS-1$
	JavadocExportMessages.getString("JavadocTreeWizardPage.destinationbrowsedialog.label")); //$NON-NLS-1$
				fDestinationText.setText(text);
			}
		});

		setOptionSetSettings();
	}

	public boolean getCustom() {
		return fCustomButton.getSelection();
	}

	private void setOptionSetSettings() {

		if (!fStore.fromStandard()) {
			fCustomButton.setSelection(true);
			fDocletText.setText(fStore.getDocletPath());
			fDocletTypeText.setText(fStore.getDocletName());
			//take the destination as the destination of the first project
			fDestinationText.setText(fStore.getDestination((IJavaProject) fWizard.getSelectedProjects().iterator().next()));
			fDestinationText.setEnabled(false);
			fDestinationBrowserButton.setEnabled(false);
			fDestinationLabel.setEnabled(false);
		} else {
			fStandardButton.setSelection(true);
			if (fWizard.getSelectedProjects().size() == 1)
				fDestinationText.setText(fStore.getDestination((IJavaProject) fWizard.getSelectedProjects().iterator().next()));
			else
				fDestinationText.setText(fStore.getDestination());
			fDocletText.setText(fStore.getDocletPath());
			fDocletTypeText.setText(fStore.getDocletName());
			fDocletText.setEnabled(false);
			fDocletLabel.setEnabled(false);
			fDocletTypeText.setEnabled(false);
			fDocletTypeLabel.setEnabled(false);
		}

	}

	/**
	 * Receives of list of elements selected by the user and passes them
	 * to the CheckedTree. List can contain multiple projects and elements from
	 * different projects. If the list of seletected elements is empty a default
	* project is selected.
	 */

	protected void setTreeChecked(IJavaElement[] sourceElements, IJavaProject project) {

		if (sourceElements.length < 1)
			fInputGroup.initialCheckTreeItem(project);

		else {
			for (int i= 0; i < sourceElements.length; i++) {
				IJavaElement curr= sourceElements[i];
				if (curr instanceof ICompilationUnit) {
					fInputGroup.initialCheckListItem(curr);
				} else if (curr instanceof IPackageFragment) {
					fInputGroup.initialCheckTreeItem(curr);
				} else if (curr instanceof IJavaProject) {
					fInputGroup.initialCheckTreeItem(curr);
				} else if (curr instanceof IPackageFragmentRoot) {
					IPackageFragmentRoot root= (IPackageFragmentRoot) curr;
					if (!root.isArchive())
						fInputGroup.initialCheckTreeItem(curr);
				}
			}
		}
	}

	private IPath[] getSourcePath(IJavaProject[] projects) {
		ArrayList res= new ArrayList();
		//loops through all projects and gets a list if of thier sourpaths
		for (int k= 0; k < projects.length; k++) {
			IJavaProject iJavaProject= projects[k];

			try {
				IPackageFragmentRoot[] roots= iJavaProject.getPackageFragmentRoots();
				for (int i= 0; i < roots.length; i++) {
					IPackageFragmentRoot curr= roots[i];
					if (curr.getKind() == IPackageFragmentRoot.K_SOURCE) {
						IResource resource= curr.getResource();
						if (resource != null) {
							IPath p= resource.getLocation();
							if (p != null) {
								res.add(p);
							}
						}
					}
				}
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
		}
		return (IPath[]) res.toArray(new IPath[res.size()]);
	}

	private IPath[] getClassPath(IJavaProject[] javaProjects) {
		ArrayList res= new ArrayList();

		for (int j= 0; j < javaProjects.length; j++) {
			IJavaProject iJavaProject= javaProjects[j];

			try {
				IPath p= iJavaProject.getProject().getLocation();
				if (p == null)
					continue;

				IPath outputLocation= p.append(iJavaProject.getOutputLocation());
				String[] classPath= JavaRuntime.computeDefaultRuntimeClassPath(iJavaProject);

				for (int i= 0; i < classPath.length; i++) {
					String curr= classPath[i];
					IPath path= new Path(curr);
					if (!outputLocation.equals(path)) {
						res.add(path);
					}
				}
			} catch (CoreException e) {
				JavaPlugin.log(e);
			}
		}
		return (IPath[]) res.toArray(new IPath[res.size()]);
	}

	/**
	 * Gets a list of elements to generated javadoc for from each project. 
	 * Javadoc can be generated for either a IPackageFragmentRoot or a ICompilationUnit.
	 */
	private IJavaElement[] getSourceElements(IJavaProject[] projects) {
		ArrayList res= new ArrayList();
		try {
			Set allChecked= fInputGroup.getAllCheckedTreeItems();

			Set incompletePackages= new HashSet();
			for (int h= 0; h < projects.length; h++) {
				IJavaProject iJavaProject= projects[h];

				IPackageFragmentRoot[] roots= iJavaProject.getPackageFragmentRoots();
				for (int i= 0; i < roots.length; i++) {
					IPackageFragmentRoot root= roots[i];
					if (root.getKind() == IPackageFragmentRoot.K_SOURCE) {
						IJavaElement[] packs= root.getChildren();
						for (int k= 0; k < packs.length; k++) {
							IJavaElement curr= packs[k];
							if (curr.getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
								// default packages are always incomplete
								if (curr.getElementName().length() == 0 || !allChecked.contains(curr) || fInputGroup.isTreeItemGreyChecked(curr)) {
									incompletePackages.add(curr.getElementName());
								}
							}
						}
					}
				}
			}

			Iterator checkedElements= fInputGroup.getAllCheckedListItems();
			while (checkedElements.hasNext()) {
				Object element= checkedElements.next();
				if (element instanceof ICompilationUnit) {
					ICompilationUnit unit= (ICompilationUnit) element;
					if (incompletePackages.contains(unit.getParent().getElementName())) {
						res.add(unit);
					}
				}
			}

			Set addedPackages= new HashSet();

			checkedElements= allChecked.iterator();
			while (checkedElements.hasNext()) {
				Object element= checkedElements.next();
				if (element instanceof IPackageFragment) {
					IPackageFragment fragment= (IPackageFragment) element;
					String name= fragment.getElementName();
					if (!incompletePackages.contains(name) && !addedPackages.contains(name)) {
						res.add(fragment);
						addedPackages.add(name);
					}
				}
			}

		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
		return (IJavaElement[]) res.toArray(new IJavaElement[res.size()]);
	}

	protected void finish() {

		if (fCustomButton.getSelection()) {
			fStore.setDocletName(fDocletTypeText.getText());
			fStore.setDocletPath(fDocletText.getText());
			fStore.setFromStandard(false);
		}
		if (fStandardButton.getSelection()) {
			fStore.setFromStandard(true);
			//in case of a single project selection the personal destination is updated for
			//storage in the dialog settings
			if (fWizard.getSelectedProjects().size() == 1) {
				fStore.setDestination((IJavaProject) fWizard.getSelectedProjects().iterator().next(), fDestinationText.getText());
			}
			//the destination used in javadoc generation
			fStore.setDestination(fDestinationText.getText());
		}

		IJavaProject[] projects= (IJavaProject[]) fWizard.getSelectedProjects().toArray(new IJavaProject[fWizard.getSelectedProjects().size()]);

		fStore.setProjects(projects, true);
		fStore.setSourcepath(getSourcePath(projects));
		fStore.setClasspath(getClassPath(projects));
		fStore.setAccess(fVisibilitySelection);
		fStore.setSourceElements(getSourceElements(projects));
	}

	protected void setProjects() {
		TreeItem[] treeItems= fInputGroup.getTree().getItems();
		for (int i= 0; i < treeItems.length; i++) {
			if (treeItems[i].getChecked())
				fWizard.addSelectedProject((IJavaProject) treeItems[i].getData());
		}
	}

	private void doValidation(int validate) {

		switch (validate) {
			case PREFERENCESTATUS :
				fPreferenceStatus= new StatusInfo();
				fDocletStatus= new StatusInfo();
				updateStatus(findMostSevereStatus());
				break;
			case CUSTOMSTATUS :

				if (fCustomButton.getSelection()) {
					fDestinationStatus= new StatusInfo();
					fDocletStatus= new StatusInfo();
					String doclet= fDocletTypeText.getText();
					String docletPath= fDocletText.getText();
					if (doclet.length() == 0) {
						fDocletStatus.setError(JavadocExportMessages.getString("JavadocTreeWizardPage.nodocletname.error")); //$NON-NLS-1$

					} else if (JavaConventions.validateJavaTypeName(doclet).matches(IStatus.ERROR)) {
						fDocletStatus.setError(JavadocExportMessages.getString("JavadocTreeWizardPage.invaliddocletname.error")); //$NON-NLS-1$
					} else if ((docletPath.length() == 0) || !validDocletPath(docletPath)) {
						fDocletStatus.setError(JavadocExportMessages.getString("JavadocTreeWizardPage.invaliddocletpath.error")); //$NON-NLS-1$
					}
					updateStatus(findMostSevereStatus());
				}
				break;

			case STANDARDSTATUS :
				if (fStandardButton.getSelection()) {
					fDestinationStatus= new StatusInfo();
					fDocletStatus= new StatusInfo();
					IPath path= new Path(fDestinationText.getText());
					if (Path.ROOT.equals(path) || Path.EMPTY.equals(path)) {
						fDestinationStatus.setError(JavadocExportMessages.getString("JavadocTreeWizardPage.nodestination.error")); //$NON-NLS-1$
					}
					File file= new File(path.toOSString());
					if (!path.isValidPath(path.toOSString()) || file.isFile()) {
						fDestinationStatus.setError(JavadocExportMessages.getString("JavadocTreeWizardPage.invaliddestination.error")); //$NON-NLS-1$
					}
					if ((path.append("package-list").toFile().exists()) || (path.append("index.html").toFile().exists())) //$NON-NLS-1$//$NON-NLS-2$
						fDestinationStatus.setWarning(JavadocExportMessages.getString("JavadocTreeWizardPage.warning.mayoverwritefiles")); //$NON-NLS-1$
					updateStatus(findMostSevereStatus());
				}
				break;

			case TREESTATUS :

				fTreeStatus= new StatusInfo();

				if (fInputGroup.getAllCheckedTreeItems().size() == 0)
					fTreeStatus.setError(JavadocExportMessages.getString("JavadocTreeWizardPage.invalidtreeselection.error")); //$NON-NLS-1$
				updateStatus(findMostSevereStatus());

				break;
		} //end switch
	}

	private boolean validDocletPath(String docletPath) {
		StringTokenizer tokens= new StringTokenizer(docletPath, ";"); //$NON-NLS-1$
		while (tokens.hasMoreTokens()) {
			File file= new File(tokens.nextToken());
			if (!file.exists())
				return false;
		}
		return true;
	}

	/**
	 * Finds the most severe error (if there is one)
	 */
	private IStatus findMostSevereStatus() {
		return StatusUtil.getMostSevere(new IStatus[] { fPreferenceStatus, fDestinationStatus, fDocletStatus, fTreeStatus, fWizardStatus });
	}

	public void init() {
		updateStatus(new StatusInfo());
	}

	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			doValidation(STANDARDSTATUS);
			doValidation(CUSTOMSTATUS);
			doValidation(TREESTATUS);
			doValidation(PREFERENCESTATUS);
		}
	}

	public IPath getDestination() {
		if (fStandardButton.getSelection()) {
			return new Path(fDestinationText.getText());
		}
		return null;
	}

}
