/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.javadocexport;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.launching.JavaRuntime;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.javadocexport.JavadocWizardPage.EnableSelectionAdapter;
import org.eclipse.jdt.internal.ui.preferences.JavadocPreferencePage;
import org.eclipse.jdt.internal.ui.util.SWTUtil;

public class JavadocTreeWizardPage extends JavadocWizardPage {

	private JavadocProjectContentProvider fProjectContentProvider;
	private JavaElementLabelProvider fProjectLabelProvider;
	private JavadocCheckboxTreeAndListGroup fInputGroup;

	protected IWorkspaceRoot fRoot;
	protected String fWorkspace;

	private final String DOCUMENT_DIRECTORY= "doc";

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
		setDescription("Select types for Javadoc generation.");

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
		Composite composite= new Composite(parent, SWT.NONE);
		GridLayout compositeGridLayout= new GridLayout();
		composite.setLayoutData(createGridData(GridData.FILL_BOTH, 0, 0));
		compositeGridLayout.numColumns= 6;
		composite.setLayout(compositeGridLayout);

		createInputGroup(composite);
		createVisibilitySet(composite);
		createOptionsSet(composite);

		setControl(composite);

	}

	protected void createInputGroup(Composite composite) {

		Label treeLabel= createLabel(composite, SWT.NONE, "Select types for which javadoc will be generated:", createGridData(6));
		Composite c= new Composite(composite, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.numColumns= 1;
		layout.makeColumnsEqualWidth= true;
		c.setLayout(layout);
		c.setLayoutData(createGridData(GridData.FILL_BOTH, 6, 0));

		ITreeContentProvider treeContentProvider= new JavadocProjectContentProvider();
		ITreeContentProvider listContentProvider= new JavadocMemberContentProvider();
		fInputGroup=
			new JavadocCheckboxTreeAndListGroup(
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
			}
		});
		//fFilter= new JavadocTreeViewerFilter();
		//fInputGroup.getTableViewer().addFilter(fFilter);


		try {
			setTreeChecked(fStore.getPackagenames().toArray(), fStore.getJavaProject());
		} catch(JavaModelException e) {
			JavaPlugin.logErrorMessage(e.getMessage());
		}
		if (fStore.getJavaProject() != null) {
			fInputGroup.getTreeViewer().expandToLevel(fStore.getJavaProject(), 4);
		}
//		fInputGroup.aboutToOpen();
//		fInputGroup.refresh();
	}

	private void createVisibilitySet(Composite composite) {

		GridLayout visibilityLayout= createGridLayout(4);
		visibilityLayout.marginHeight= 0;
		visibilityLayout.marginWidth= 0;
		Composite visibilityGroup= new Composite(composite, SWT.NONE);
		visibilityGroup.setLayoutData(createGridData(GridData.FILL_HORIZONTAL, 6, 0));
		visibilityGroup.setLayout(visibilityLayout);

		Label visibilityLabel= createLabel(visibilityGroup, SWT.NONE, "Create Javadoc for members with visibility: ", createGridData(GridData.FILL_HORIZONTAL, 4, 0));
		fPrivateVisibility= createButton(visibilityGroup, SWT.RADIO, "Pr&ivate", createGridData(GridData.FILL_HORIZONTAL, 1, 0));
		fPackageVisibility= createButton(visibilityGroup, SWT.RADIO, "P&ackage", createGridData(GridData.FILL_HORIZONTAL, 1, 0));
		fProtectedVisibility= createButton(visibilityGroup, SWT.RADIO, "Pr&otected", createGridData(GridData.FILL_HORIZONTAL, 1, 0));
		fPublicVisibility= createButton(visibilityGroup, SWT.RADIO, "P&ublic", createGridData(GridData.FILL_HORIZONTAL, 1, 0));

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

		fStandardButton= createButton(optionSetGroup, SWT.RADIO, "Use &Standard Doclet", createGridData(GridData.HORIZONTAL_ALIGN_FILL, 3, 0));

		GridData gd= new GridData();
		gd.horizontalSpan= 1;
		fDestinationLabel= createLabel(optionSetGroup, SWT.NONE, "&Destination: ", createGridData(GridData.HORIZONTAL_ALIGN_BEGINNING, 1, convertWidthInCharsToPixels(3)));
		fDestinationText= createText(optionSetGroup, SWT.SINGLE | SWT.BORDER, null, createGridData(GridData.FILL_HORIZONTAL, 1, 0));
		fDestinationText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				doValidation(STANDARDSTATUS);
			}
		});

		fDestinationBrowserButton= createButton(optionSetGroup, SWT.PUSH, "Bro&wse...", createGridData(GridData.HORIZONTAL_ALIGN_FILL, 1, 0));
		SWTUtil.setButtonDimensionHint(fDestinationBrowserButton);

		//Option to use custom doclet
		fCustomButton= createButton(optionSetGroup, SWT.RADIO, "Use &Custom Doclet", createGridData(3));

		//For Entering location of custom doclet
		fDocletTypeLabel= createLabel(optionSetGroup, SWT.NONE, "Doc&let name: ", createGridData(GridData.HORIZONTAL_ALIGN_FILL, 1, convertWidthInCharsToPixels(3)));
		fDocletTypeText= createText(optionSetGroup, SWT.SINGLE | SWT.BORDER, null, createGridData(GridData.HORIZONTAL_ALIGN_FILL, 2, 0));
		fDocletTypeText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				doValidation(CUSTOMSTATUS);
			}

		});

		fDocletLabel= createLabel(optionSetGroup, SWT.NONE, "Doclet class &path: ", createGridData(GridData.HORIZONTAL_ALIGN_FILL, 1, convertWidthInCharsToPixels(3)));
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
								"Destination Selection",
								"&Select the Javadoc destination folder:");
				fDestinationText.setText(text);
			}
		});
		
		setOptionSetSettings();
	}
	
	public boolean getCustom(){
		return fCustomButton.getSelection();
	}

	private String getDestinationText() {
		Object[] els= fInputGroup.getTreeViewer().getCheckedElements();

		try {
			for (int i= 0; i < els.length; i++) {
				if (els[i] instanceof IJavaProject) {
					IJavaProject iJavaProject= (IJavaProject) els[i];
					return iJavaProject.getUnderlyingResource().getLocation().addTrailingSeparator().append(DOCUMENT_DIRECTORY).toOSString();
				}
			}
		} catch (JavaModelException e) {
			return "";
		} catch (NullPointerException e) {
			return "";
		}
		return "";
	}

	private void setOptionSetSettings() {

			if(!fStore.fromStandard()) {
				fCustomButton.setSelection(true);
				fDocletText.setText(fStore.getDocletPath());
				fDocletTypeText.setText(fStore.getDocletName());
				fDestinationText.setText(fStore.getDestination());
				fDestinationText.setEnabled(false);
				fDestinationBrowserButton.setEnabled(false);
				fDestinationLabel.setEnabled(false);
			} else {
				fStandardButton.setSelection(true);
				fDestinationText.setText(fStore.getDestination());
				fDocletText.setEnabled(false);
				fDocletLabel.setEnabled(false);
				fDocletTypeText.setEnabled(false);
				fDocletTypeLabel.setEnabled(false);
			}

	}

	protected void setTreeChecked(Object[] packagenames, IJavaProject project) throws JavaModelException{

		if(project==null)
			return;
			
		for (int i = 0; i < packagenames.length; i++) {
			if(packagenames[i] instanceof IJavaElement) {
					IJavaElement element = (IJavaElement)packagenames[i];
					if (element instanceof IType) {
						fInputGroup.initialCheckListItem(element);
					} else
						fInputGroup.initialCheckTreeItem(element);	
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
	private List getPackagenames() {
				
			Iterator checkedElements= fInputGroup.getAllCheckedListItems();
			List list= new ArrayList();

			while (checkedElements.hasNext()) {
				Object element= checkedElements.next();
				if (element instanceof IType) {
					IType type= (IType) element;
					IPackageFragment pack= type.getPackageFragment();
					if (fInputGroup.getTreeViewer().getGrayed(pack) || pack.isDefaultPackage()) {
						String qn=JavaModelUtil.getFullyQualifiedName(type);
						list.add(qn);
					}
				}
			}
			Object[] checkedTreeElements= fInputGroup.getTreeViewer().getCheckedElements();
			for (int i= 0; i < checkedTreeElements.length; i++) {
				Object element= checkedTreeElements[i];
				if (element instanceof IPackageFragment) {
					IPackageFragment pack= (IPackageFragment) element;
					if (!fInputGroup.getTreeViewer().getGrayed(pack) && !pack.isDefaultPackage()) {
						
						String en= pack.getElementName();
						list.add(en);
					}
				}
			}
			
			return list;
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
			fStore.setDestination(fDestinationText.getText());
		}
			
		IJavaProject project= getCurrentProject();
		fStore.setProject(project);	
		fStore.setSourcepath(getSourcePath(project));
		fStore.setClasspath(getClassPath(project));	
		fStore.setAccess(fVisibilitySelection);
		fStore.setPackagenames(getPackagenames());
	}

	private void doValidation(int validate) {

		switch (validate) {
			case PREFERENCESTATUS :
				fPreferenceStatus = new StatusInfo();
				fDocletStatus= new StatusInfo();
				if (JavadocPreferencePage.getJavaDocCommand().length() == 0) {
					fPreferenceStatus.setError(
						"Javadoc command location not specified on the Javadoc preference page.");
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
						fDocletStatus.setError("Enter a doclet name.");

					} else if (
						JavaConventions.validateJavaTypeName(doclet).matches(IStatus.ERROR)) {
						fDocletStatus.setError("Invalid doclet name.");
					} else if ((docletPath.length() == 0) || !validDocletPath(docletPath)) {
						fDocletStatus.setError("Not a valid doclet class path.");
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
						fDestinationStatus.setError("Enter the destination folder.");
					}
					File file = new File(path.toOSString());
					if (!path.isValidPath(path.toOSString()) || file.isFile()) {
						fDestinationStatus.setError("Not a valid folder.");
					}
					updateStatus(findMostSevereStatus());
				}
				break;

			case TREESTATUS :

				fTreeStatus = new StatusInfo();
				boolean empty = fInputGroup.getAllCheckedTreeItems().isEmpty();
				if (empty)
					fTreeStatus.setError("Select elements from tree.");
				else {
					int projCount = 0;
					Object[] items = fInputGroup.getAllCheckedTreeItems().toArray();
					for (int i = 0; i < items.length; i++) {
						IJavaElement element = (IJavaElement) items[i];
						if (element instanceof IJavaProject) {
							projCount++;
							if (projCount > 1)
								fTreeStatus.setError(
									"Cannot generate Javadoc for elements in multiple projects.");
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
			if (items[i] instanceof JavaProject) {
				if (project != null) {
					return null;
				}
				project= (JavaProject)items[i];
			}
		}
		return project;
	}

	private boolean validDocletPath(String docletPath) {
		StringTokenizer tokens= new StringTokenizer(docletPath, ";");
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

} //end Class