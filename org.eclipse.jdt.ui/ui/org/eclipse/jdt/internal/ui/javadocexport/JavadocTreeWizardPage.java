/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.javadocexport;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.wizard.IWizardPage;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.launching.JavaRuntime;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.jarpackager.CheckboxTreeAndListGroup;
import org.eclipse.jdt.internal.ui.preferences.JavadocPreferencePage;
import org.eclipse.jdt.internal.ui.util.SWTUtil;

public class JavadocTreeWizardPage extends JavadocWizardPage {

	private JavadocProjectContentProvider fProjectContentProvider;
	private JavaElementLabelProvider fProjectLabelProvider;
	private CheckboxTreeAndListGroup fInputGroup;

	protected IWorkspaceRoot fRoot;
	protected String fWorkspace;

	private final String DOCUMENT_DIRECTORY= "doc"; //$NON-NLS-1$

	private File fTempFile;

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

	private String fDialogSectionName;
	protected String fVisibilitySelection;
	protected boolean docletselected;
	
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
		fWizard = 	(JavadocWizard)this.getWizard();
		
		Composite composite= new Composite(parent, SWT.NONE);
		GridLayout compositeGridLayout= new GridLayout();
		composite.setLayoutData(createGridData(GridData.FILL_BOTH, 0, 0));
		compositeGridLayout.numColumns= 6;
		composite.setLayout(compositeGridLayout);

		createInputGroup(composite);
		createVisibilitySet(composite);
		createOptionsSet(composite);

		setControl(composite);
		WorkbenchHelp.setHelp(composite, IJavaHelpContextIds.JAVADOC_TREE_PAGE);
	}

	protected void createInputGroup(Composite composite) {

		Label treeLabel= createLabel(composite, SWT.NONE, JavadocExportMessages.getString("JavadocTreeWizardPage.checkboxtreeandlistgroup.label"), createGridData(6)); //$NON-NLS-1$
		Composite c= new Composite(composite, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.numColumns= 1;
		layout.makeColumnsEqualWidth= true;
		c.setLayout(layout);
		c.setLayoutData(createGridData(GridData.FILL_BOTH, 6, 0));

		ITreeContentProvider treeContentProvider= new JavadocProjectContentProvider();
		ITreeContentProvider listContentProvider= new JavadocMemberContentProvider();
		fInputGroup=
			new CheckboxTreeAndListGroup(
				c,
				fStore.getRoot(),
				treeContentProvider,
				new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT),
				listContentProvider,
				new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT),
				SWT.NONE,
				convertWidthInCharsToPixels(60),
				convertHeightInCharsToPixels(10));

		fInputGroup.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent e) {
				doValidation(TREESTATUS);
				
				IJavaElement el= null;
				Object[] set= fInputGroup.getAllCheckedTreeItems().toArray();	
				for (int i = 0; i < set.length; i++) {
					IJavaElement javaElement = (IJavaElement)set[i];
					if(javaElement instanceof IJavaModel)
						continue;
					else {
						el=(IJavaElement) set[i];
						break;
					}	
				}
				if(el != null)	
					fWizard.setProject(el.getJavaProject());
			}
		});
		//fFilter= new JavadocTreeViewerFilter();
		//fInputGroup.getTableViewer().addFilter(fFilter);


		try {
			setTreeChecked(fStore.getSelectedElements(), fStore.getJavaProject());
		} catch(JavaModelException e) {
			JavaPlugin.log(e);
		}
		
		fInputGroup.aboutToOpen();
	}

	private void createVisibilitySet(Composite composite) {

		GridLayout visibilityLayout= createGridLayout(4);
		visibilityLayout.marginHeight= 0;
		visibilityLayout.marginWidth= 0;
		Composite visibilityGroup= new Composite(composite, SWT.NONE);
		visibilityGroup.setLayoutData(createGridData(GridData.FILL_HORIZONTAL, 6, 0));
		visibilityGroup.setLayout(visibilityLayout);

		Label visibilityLabel= createLabel(visibilityGroup, SWT.NONE, JavadocExportMessages.getString("JavadocTreeWizardPage.visibilitygroup.label"), createGridData(GridData.FILL_HORIZONTAL, 4, 0)); //$NON-NLS-1$
		fPrivateVisibility= createButton(visibilityGroup, SWT.RADIO, JavadocExportMessages.getString("JavadocTreeWizardPage.privatebutton.label"), createGridData(GridData.FILL_HORIZONTAL, 1, 0)); //$NON-NLS-1$
		fPackageVisibility= createButton(visibilityGroup, SWT.RADIO, JavadocExportMessages.getString("JavadocTreeWizardPage.packagebutton.label"), createGridData(GridData.FILL_HORIZONTAL, 1, 0)); //$NON-NLS-1$
		fProtectedVisibility= createButton(visibilityGroup, SWT.RADIO, JavadocExportMessages.getString("JavadocTreeWizardPage.protectedbutton.label"), createGridData(GridData.FILL_HORIZONTAL, 1, 0)); //$NON-NLS-1$
		fPublicVisibility= createButton(visibilityGroup, SWT.RADIO, JavadocExportMessages.getString("JavadocTreeWizardPage.publicbutton.label"), createGridData(GridData.FILL_HORIZONTAL, 1, 0)); //$NON-NLS-1$

		fPrivateVisibility.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (((Button) e.widget).getSelection()) {
					fVisibilitySelection = fStore.PRIVATE;
					//fFilter.setVisibility(fVisibilitySelection);
					//fInputGroup.refresh();
				}
			}
		});
		fPackageVisibility.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (((Button) e.widget).getSelection()) {
					fVisibilitySelection = fStore.PACKAGE;
					//fFilter.setVisibility(fVisibilitySelection);
					//fInputGroup.refresh();
				}
			}
		});
		fProtectedVisibility.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (((Button) e.widget).getSelection()) {
					fVisibilitySelection = fStore.PROTECTED;
					//fFilter.setVisibility(fVisibilitySelection);
					//fInputGroup.refresh();
				}
			}
		});
		fPublicVisibility.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (((Button) e.widget).getSelection()) {
					fVisibilitySelection = fStore.PUBLIC;
					//fFilter.setVisibility(fVisibilitySelection);
					//fInputGroup.refresh();
				}
			}
		});
		
		setVisibilitySettings();

	}

	protected void setVisibilitySettings() {

		
			fVisibilitySelection = fStore.getAccess();
			fPrivateVisibility.setSelection(
				fVisibilitySelection.equals(fStore.PRIVATE));
			fProtectedVisibility.setSelection(
				fVisibilitySelection.equals(fStore.PROTECTED));
			fPackageVisibility.setSelection(
				fVisibilitySelection.equals(fStore.PACKAGE));
			fPublicVisibility.setSelection(
				fVisibilitySelection.equals(fStore.PUBLIC));
			//fFilter.setVisibility(fVisibilitySelection);

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
		((GridData) fDestinationText.getLayoutData()).widthHint = 200;
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
		fCustomButton.addSelectionListener(
			new EnableSelectionAdapter(new Control[] { fDocletLabel, fDocletText, fDocletTypeLabel, fDocletTypeText }, new Control[] { fDestinationLabel, fDestinationText, fDestinationBrowserButton }));
		fCustomButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				doValidation(CUSTOMSTATUS);
			}
		});
		fStandardButton.addSelectionListener(
			new EnableSelectionAdapter(new Control[] { fDestinationLabel, fDestinationText, fDestinationBrowserButton }, new Control[] { fDocletLabel, fDocletText, fDocletTypeLabel, fDocletTypeText }));
		fStandardButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				doValidation(STANDARDSTATUS);
			}
		});
		fDestinationBrowserButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				String text= handleFolderBrowseButtonPressed(
								fDestinationText.getText(), fDestinationText.getShell(),
								JavadocExportMessages.getString("JavadocTreeWizardPage.destinationbrowsedialog.title"), //$NON-NLS-1$
								JavadocExportMessages.getString("JavadocTreeWizardPage.destinationbrowsedialog.label")); //$NON-NLS-1$
				fDestinationText.setText(text);
			}
		});
		
		setOptionSetSettings();
	}
	
	public boolean getCustom(){
		return fCustomButton.getSelection();
	}

	private String getDestinationText() {
		Object[] els= fInputGroup.getAllCheckedTreeItems().toArray();

		try {
			for (int i= 0; i < els.length; i++) {
				if (els[i] instanceof IJavaProject) {
					IJavaProject iJavaProject= (IJavaProject) els[i];
					return iJavaProject.getUnderlyingResource().getLocation().addTrailingSeparator().append(DOCUMENT_DIRECTORY).toOSString();
				}
			}
		} catch (JavaModelException e) {
			return ""; //$NON-NLS-1$
		} catch (NullPointerException e) {
			return ""; //$NON-NLS-1$
		}
		return ""; //$NON-NLS-1$
	}

	private void setOptionSetSettings() {

			if(!fStore.fromStandard()) {
				fCustomButton.setSelection(true);
				fDocletText.setText(fStore.getDocletPath());
				fDocletTypeText.setText(fStore.getDocletName());
				fDestinationText.setText(fStore.getDestination(fWizard.getProject()));
				fDestinationText.setEnabled(false);
				fDestinationBrowserButton.setEnabled(false);
				fDestinationLabel.setEnabled(false);
			} else {
				fStandardButton.setSelection(true);
				fDestinationText.setText(fStore.getDestination(fWizard.getProject()));
				fDocletText.setText(fStore.getDocletPath());
				fDocletTypeText.setText(fStore.getDocletName());
				fDocletText.setEnabled(false);
				fDocletLabel.setEnabled(false);
				fDocletTypeText.setEnabled(false);
				fDocletTypeLabel.setEnabled(false);
			}

	}

	protected void setTreeChecked(
		IJavaElement[] sourceElements,
		IJavaProject project)
		throws JavaModelException {

		if (project == null)
			return;
		if (sourceElements.length < 1)
			fInputGroup.initialCheckTreeItem(project);

		else {
			for (int i = 0; i < sourceElements.length; i++) {
				IJavaElement curr = sourceElements[i];
				if (curr instanceof ICompilationUnit) {
					fInputGroup.initialCheckListItem(curr);
				} else if (curr instanceof IPackageFragment) {
					fInputGroup.initialCheckTreeItem(curr);
				} else if(curr instanceof IJavaProject) {
					//if the only selected element is a project
					if(sourceElements.length==1)	
						fInputGroup.initialCheckTreeItem(curr);
				} else if (curr instanceof IPackageFragmentRoot) {
						IPackageFragmentRoot root= (IPackageFragmentRoot)curr;
						if(!root.isArchive())		
							fInputGroup.initialCheckTreeItem(curr);
				}	
			}
		}
	}

	private String getSourcePath(IJavaProject project) {
		StringBuffer buf= new StringBuffer();
		try {
			IPackageFragmentRoot[] roots= project.getPackageFragmentRoots();
			int nAdded= 0;
			for (int i= 0; i < roots.length; i++) {
				IPackageFragmentRoot curr= roots[i];
				if (curr.getKind() == IPackageFragmentRoot.K_SOURCE) {
					if (nAdded != 0) {
						buf.append(File.pathSeparatorChar);
					}
					buf.append(curr.getUnderlyingResource().getLocation().toOSString());
					nAdded++;
				}
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
		return buf.toString();
	}

	private String getClassPath(IJavaProject javaProject) {
		StringBuffer buf= new StringBuffer();

		try {
			IPath outputLocation= javaProject.getProject().getLocation().append(javaProject.getOutputLocation());
			String[] classPath= JavaRuntime.computeDefaultRuntimeClassPath(javaProject);
			int nAdded= 0;
			for (int i= 0; i < classPath.length; i++) {
				String curr= classPath[i];
				if (outputLocation.equals(new Path(curr))) {
					continue;
				}
				if (nAdded != 0) {
					buf.append(File.pathSeparatorChar);
				}
				buf.append(curr);
				nAdded++;
			}
		} catch (CoreException e) {
			JavaPlugin.log(e);
		} 
		return buf.toString();
	}

	
	//Returns the path were the doclet file will be created
	private IJavaElement[] getSourceElements(IJavaProject currProject) {
		ArrayList res= new ArrayList();
		try {
			Set allChecked= fInputGroup.getAllCheckedTreeItems();
			
			Set incompletePackages= new HashSet();
			IPackageFragmentRoot[] roots= currProject.getPackageFragmentRoots();
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
					String name= ((IPackageFragment) element).getElementName();
					if (!incompletePackages.contains(name) && !addedPackages.contains(name)) {
						res.add(element);
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
		//I have made the assumption that preserve settings will not be
		//called on an ANT file, which is evident...
		
		if (fCustomButton.getSelection()) {
			fStore.setDocletName(fDocletTypeText.getText());
			fStore.setDocletPath(fDocletText.getText());
			fStore.setFromStandard(false);
		}
		if(fStandardButton.getSelection()){
			fStore.setFromStandard(true);
			fStore.setDestination(fWizard.getProject(), fDestinationText.getText());
		}
			
		IJavaProject project= getCurrentProject();
		fStore.setProject(project);	
		fStore.setSourcepath(getSourcePath(project));
		fStore.setClasspath(getClassPath(project));	
		fStore.setAccess(fVisibilitySelection);
		fStore.setSourceElements(getSourceElements(project));
	}

	private void doValidation(int validate) {

		switch (validate) {
			case PREFERENCESTATUS :
				fPreferenceStatus = new StatusInfo();
				fDocletStatus= new StatusInfo();
				if (JavadocPreferencePage.getJavaDocCommand().length() == 0) {
					fPreferenceStatus.setError(
						JavadocExportMessages.getString("JavadocTreeWizardPage.javadoccommand.error")); //$NON-NLS-1$
				}
				updateStatus(findMostSevereStatus());
				break;
			case CUSTOMSTATUS :

				if (fCustomButton.getSelection()) {
					fDestinationStatus = new StatusInfo();
					fDocletStatus = new StatusInfo();
					String doclet = fDocletTypeText.getText();
					String docletPath = fDocletText.getText();
					if (doclet.length() == 0) {
						fDocletStatus.setError(JavadocExportMessages.getString("JavadocTreeWizardPage.nodocletname.error")); //$NON-NLS-1$

					} else if (
						JavaConventions.validateJavaTypeName(doclet).matches(IStatus.ERROR)) {
						fDocletStatus.setError(JavadocExportMessages.getString("JavadocTreeWizardPage.invaliddocletname.error")); //$NON-NLS-1$
					} else if ((docletPath.length() == 0) || !validDocletPath(docletPath)) {
						fDocletStatus.setError(JavadocExportMessages.getString("JavadocTreeWizardPage.invaliddocletpath.error")); //$NON-NLS-1$
					}
					updateStatus(findMostSevereStatus());
				}
				break;

			case STANDARDSTATUS :
				if (fStandardButton.getSelection()) {
					fDestinationStatus = new StatusInfo();
					fDocletStatus= new StatusInfo();
					IPath path = new Path(fDestinationText.getText());
					if (Path.ROOT.equals(path) || Path.EMPTY.equals(path)) {
						fDestinationStatus.setError(JavadocExportMessages.getString("JavadocTreeWizardPage.nodestination.error")); //$NON-NLS-1$
					}
					File file = new File(path.toOSString());
					if (!path.isValidPath(path.toOSString()) || file.isFile()) {
						fDestinationStatus.setError(JavadocExportMessages.getString("JavadocTreeWizardPage.invaliddestination.error")); //$NON-NLS-1$
					}
					updateStatus(findMostSevereStatus());
				}
				break;

			case TREESTATUS :

				fTreeStatus = new StatusInfo();
				Object[] items = fInputGroup.getAllCheckedTreeItems().toArray();

				if (items.length == 0)
					fTreeStatus.setError(JavadocExportMessages.getString("JavadocTreeWizardPage.invalidtreeselection.error")); //$NON-NLS-1$
				else {
					int projCount = 0;
					for (int i = 0; i < items.length; i++) {
						IJavaElement element = (IJavaElement) items[i];
						if (element instanceof IJavaProject) {
							projCount++;
							if (projCount > 1)
								fTreeStatus.setError(
									JavadocExportMessages.getString("JavadocTreeWizardPage.multipleprojectselected.error")); //$NON-NLS-1$
						}
					}
				}
				updateStatus(findMostSevereStatus());

				break;
		} //end switch
	}
	/**
	 * looks at the currently selected projects and returns the current project
	 * returns null if more than one project is checked
	 */
	private IJavaProject getCurrentProject() {
		Object[] items= fInputGroup.getAllCheckedTreeItems().toArray();
			
		IJavaProject project= null;
		for (int i= 0; i < items.length; i++) {
			if (items[i] instanceof IJavaProject) {
				if (project != null) {
					return null;
				}
				project= (IJavaProject)items[i];
			}
		}
		return project;
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
		return StatusUtil.getMostSevere(new IStatus[] {fPreferenceStatus, fDestinationStatus, fDocletStatus, fTreeStatus, fWizardStatus });
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
	
	
	public IWizardPage getNextPage() {
		return super.getNextPage();       
	}
	
} //end Class