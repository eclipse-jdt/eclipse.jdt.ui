/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.javadocexport;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
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

import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.launching.JavaRuntime;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.preferences.JavadocPreferencePage;
import org.eclipse.jdt.internal.ui.util.SWTUtil;

/**
 * @version 	1.0
 * @author
 */

public class JavadocTreeWizardPage extends JavadocWizardPage {

	private JavadocProjectContentProvider fProjectContentProvider;
	private JavaElementLabelProvider fProjectLabelProvider;
	private JavadocCheckboxTreeAndListGroup fInputGroup;

	protected IWorkspaceRoot fRoot;
	protected String fWorkspace;

	private final String DOCUMENT_DIRECTORY= "doc";
	private final String STANDARD_FLAG= "-d";
	private final String DOCLET_FLAG= "-doclet";
	private final String DOCLETPATH_FLAG= "-docletpath";
	private final String SOURCE_FLAG= "-sourcepath";
	private final String CLASSPATH_FLAG= "-classpath";
	private final String PRIVATE= "-private";
	private final String PROTECTED= "-protected";
	private final String PACKAGE= "-package";
	private final String PUBLIC= "-public";
	private final String SETTINGS_VISIBILITY= "VISIBILITY";
	private final String SETTINGS_DESTINATION= "DESTINATION";
	private final String SETTINGS_CUSTOM= "CUSTOM";
	private final String SETTINGS_STANDARD= "STANDARD";
	private final String SETTINGS_DOCLET= "DOCLET";
	private final String SETTINGS_DOCLETPATH= "DOCLETPATH";
	private final String BROWSE= "Browse...";

	private File fTempFile;

	private Text fDestinationText;
	private Text fDocletText;
	private Text fDocletTypeText;
	private Button fStandardButton;
	private Button fDestinationBrowserButton;
	protected Button fCustomButton;
	private Button fPrivateVisibility;
	private Button fProtectedVisibility;
	private Button fPackageVisibility;
	private Button fPublicVisibility;
	private Label fDocletLabel;
	private Label fDocletTypeLabel;
	private Label fDestinationLabel;

	private IDialogSettings fDialogSettings;
	private String fDialogSectionName;
	private int fVisibilitySelection;
	protected boolean docletselected;

	protected StatusInfo fDestinationStatus;
	protected StatusInfo fDocletStatus;
	protected StatusInfo fTreeStatus;

	/**
	 * Constructor for JavadocTreeWizardPage.
	 * @param pageName
	 */
	protected JavadocTreeWizardPage(String pageName, IDialogSettings settings) {
		super(pageName);
		setDescription("Select types for Javadoc generation.");

		fDialogSettings= settings;
		fDialogSectionName= pageName;

		fRoot= ResourcesPlugin.getWorkspace().getRoot();
		fWorkspace= fRoot.getLocation().toOSString();

		// Status variables
		fDestinationStatus= new StatusInfo();
		fDocletStatus= new StatusInfo();
		fTreeStatus= new StatusInfo();
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
				fRoot,
				treeContentProvider,
				new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT),
				listContentProvider,
				new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT),
				SWT.NONE,
				convertWidthInCharsToPixels(60),
				convertHeightInCharsToPixels(10));

		fInputGroup.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent e) {
				doValidation();
			}
		});

		IStructuredSelection selections= getValidSelection();
		Iterator iter= selections.iterator();
		while (iter.hasNext()) {
			IJavaElement element= (IJavaElement) iter.next();
			if (element instanceof IType) {
				fInputGroup.initialCheckListItem(element);
			} else
				fInputGroup.initialCheckTreeItem(element);
		}
		Object project= getCurrentProject();
		if (project == null) {
			Object[] roots= treeContentProvider.getElements(fRoot);
			if (roots.length > 0) {
				project= roots[0];
			}
		}
		if (project != null) {
			fInputGroup.getTreeViewer().expandToLevel(project, 4);
		}
		
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
				fVisibilitySelection= JavadocWizard.PRIVATE;
			}
		});
		fPackageVisibility.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fVisibilitySelection= JavadocWizard.PACKAGE;
			}
		});
		fProtectedVisibility.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fVisibilitySelection= JavadocWizard.PROTECTED;
			}
		});
		fPublicVisibility.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fVisibilitySelection= JavadocWizard.PUBLIC;
			}
		});

		setVisibilitySetting();

	}

	private void setVisibilitySetting() {

		IDialogSettings section= fDialogSettings.getSection(fDialogSectionName);
		if (section == null) {
			//default setting
			fPrivateVisibility.setSelection(true);
			fVisibilitySelection= JavadocWizard.PRIVATE;
		} else {
			fVisibilitySelection= section.getInt(SETTINGS_VISIBILITY);
			fPrivateVisibility.setSelection(fVisibilitySelection == JavadocWizard.PRIVATE);
			fProtectedVisibility.setSelection(fVisibilitySelection == JavadocWizard.PROTECTED);
			fPackageVisibility.setSelection(fVisibilitySelection == JavadocWizard.PACKAGE);
			fPublicVisibility.setSelection(fVisibilitySelection == JavadocWizard.PUBLIC);
		}

	}

	private void createOptionsSet(Composite composite) {

		GridLayout optionSetLayout= createGridLayout(3);
		optionSetLayout.marginHeight= 0;
		optionSetLayout.marginWidth= 0;
		Composite optionSetGroup= new Composite(composite, SWT.NONE);
		optionSetGroup.setLayoutData(createGridData(GridData.FILL_BOTH, 6, 0));
		optionSetGroup.setLayout(optionSetLayout);

		fStandardButton= createButton(optionSetGroup, SWT.RADIO, "Use Standard Doclet", createGridData(GridData.HORIZONTAL_ALIGN_FILL, 3, 0));

		GridData gd= new GridData();
		gd.horizontalSpan= 1;
		fDestinationLabel= createLabel(optionSetGroup, SWT.NONE, "Destination: ", createGridData(GridData.HORIZONTAL_ALIGN_BEGINNING, 1, convertWidthInCharsToPixels(3)));
		//		fDestinationLabel = createLabel(optionSetGroup, SWT.NONE, "Destination: ", gd);	

		//fDestinationText = createText(optionSetGroup, SWT.SINGLE | SWT.BORDER, null, gd);
		fDestinationText= createText(optionSetGroup, SWT.SINGLE | SWT.BORDER, null, createGridData(GridData.FILL_HORIZONTAL, 1, 0));
		fDestinationText.setText(getDestinationText());
		fDestinationText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				doValidation();
			}
		});

		fDestinationBrowserButton= createButton(optionSetGroup, SWT.PUSH, BROWSE, createGridData(GridData.HORIZONTAL_ALIGN_FILL, 1, 0));
		SWTUtil.setButtonDimensionHint(fDestinationBrowserButton);

		//Option to use custom doclet
		fCustomButton= createButton(optionSetGroup, SWT.RADIO, "Use Custom Doclet", createGridData(3));

		//For Entering location of custom doclet
		fDocletTypeLabel= createLabel(optionSetGroup, SWT.NONE, "Doclet name: ", createGridData(GridData.HORIZONTAL_ALIGN_FILL, 1, convertWidthInCharsToPixels(3)));
		fDocletTypeText= createText(optionSetGroup, SWT.SINGLE | SWT.BORDER, null, createGridData(GridData.HORIZONTAL_ALIGN_FILL, 2, 0));
		fDocletTypeText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				doValidation();
			}

		});

		fDocletLabel= createLabel(optionSetGroup, SWT.NONE, "Doclet class path: ", createGridData(GridData.HORIZONTAL_ALIGN_FILL, 1, convertWidthInCharsToPixels(3)));
		fDocletText= createText(optionSetGroup, SWT.SINGLE | SWT.BORDER, null, createGridData(GridData.HORIZONTAL_ALIGN_FILL, 2, 0));
		fDocletText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				doValidation();
			}

		});

		//Add Listeners
		fCustomButton.addSelectionListener(
			new EnableSelectionAdapter(new Control[] { fDocletLabel, fDocletText, fDocletTypeLabel, fDocletTypeText }, new Control[] { fDestinationLabel, fDestinationText, fDestinationBrowserButton }));
		fCustomButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				doValidation();
			}
		});
		fStandardButton.addSelectionListener(
			new EnableSelectionAdapter(new Control[] { fDestinationLabel, fDestinationText, fDestinationBrowserButton }, new Control[] { fDocletLabel, fDocletText, fDocletTypeLabel, fDocletTypeText }));
		fStandardButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				doValidation();
			}
		});
		fDestinationBrowserButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				handleFolderBrowseButtonPressed(fDestinationText, "Destination Selection", "&Select the Javadoc destination folder:");
			}
		});
		setOptionSetSettings();
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

		IDialogSettings section= fDialogSettings.getSection(fDialogSectionName);
		if (section == null) {
			setOptionsSetDefaults();
		} else {
			if (section.getBoolean(SETTINGS_CUSTOM)) {
				fCustomButton.setSelection(true);
				fDocletText.setText(section.get(SETTINGS_DOCLETPATH));
				fDocletTypeText.setText(section.get(SETTINGS_DOCLET));
				fDestinationText.setEnabled(false);
				fDestinationBrowserButton.setEnabled(false);
				fDestinationLabel.setEnabled(false);
			} else
				setOptionsSetDefaults();
		}

	}

	private void setOptionsSetDefaults() {
		//default setting
		fStandardButton.setSelection(true);
		fDocletText.setEnabled(false);
		fDocletLabel.setEnabled(false);
		fDocletTypeText.setEnabled(false);
		fDocletTypeLabel.setEnabled(false);
	}

	public void collectArguments(ArrayList cFlags) {

		if (fStandardButton.getSelection()) {
			cFlags.add(STANDARD_FLAG);
			cFlags.add(fDestinationText.getText());

		} else if (fCustomButton.getSelection()) {
			cFlags.add(DOCLET_FLAG);
			cFlags.add(fDocletTypeText.getText());

			String docletPath= fDocletText.getText();
			if (docletPath.length() > 0) {
				cFlags.add(DOCLETPATH_FLAG);
				cFlags.add(docletPath);
			}
		}

		IJavaProject jproject= getCurrentProject();

		getSourcePath(jproject, cFlags);
		getClassPath(jproject, cFlags);
		cFlags.add(getVisibilityFlag());
	}

	private String getVisibilityFlag() {
		switch (fVisibilitySelection) {
			case JavadocWizard.PUBLIC :
				return PUBLIC;
			case JavadocWizard.PROTECTED :
				return PROTECTED;
			case JavadocWizard.PACKAGE :
				return PACKAGE;
			default :
				return PRIVATE;

		}
	}

	//to be changed
	private void getSourcePath(IJavaProject project, ArrayList flags) {
		flags.add(SOURCE_FLAG);
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
		//System.out.println("source path " + buf.toString());
		flags.add(buf.toString());
	}

	private void getClassPath(IJavaProject javaProject, ArrayList flags) {
		flags.add(CLASSPATH_FLAG);
		StringBuffer buf= new StringBuffer();

		try {
			String outputLocation= javaProject.getCorrespondingResource().getLocation().append(javaProject.getOutputLocation()).toOSString();
			String[] classPath= JavaRuntime.computeDefaultRuntimeClassPath(javaProject);
			int nAdded= 0;
			for (int i= 0; i < classPath.length; i++) {
				String curr= classPath[i];
				if (outputLocation.equals(curr)) {
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
		//System.out.println("class path " + buf.toString());
		flags.add(buf.toString());
	}

	public String getFileListArgument() {
		return "@"+ createDocletFile();
	}

	//Returns the path were the doclet file will be created
	private String createDocletFile() {

		PrintWriter out= null;

		try {
			fTempFile= File.createTempFile("jdoc", ".txt");
			out= new PrintWriter(new FileWriter(fTempFile));
			Iterator checkedElements= fInputGroup.getAllCheckedListItems();

			while (checkedElements.hasNext()) {
				Object element= checkedElements.next();
				if (element instanceof IType) {
					IType type= (IType) element;
					IPackageFragment pack= type.getPackageFragment();
					if (fInputGroup.getTreeViewer().getGrayed(pack) || pack.isDefaultPackage()) {
						out.println(JavaModelUtil.getFullyQualifiedName(type));
					}
				}
			}
			Object[] checkedTreeElements= fInputGroup.getTreeViewer().getCheckedElements();
			for (int i= 0; i < checkedTreeElements.length; i++) {
				Object element= checkedTreeElements[i];
				if (element instanceof IPackageFragment) {
					IPackageFragment pack= (IPackageFragment) element;
					if (!fInputGroup.getTreeViewer().getGrayed(pack) && !pack.isDefaultPackage()) {
						out.println(pack.getElementName());
					}
				}
			}
			//} catch (FileNotFoundException FNFe) {
			//	JavaPlugin.logErrorMessage("Notify: Core Error file " + path.toString() + " not found");
		} catch (java.io.IOException IOe) {
			JavaPlugin.logErrorMessage("Notify: Java IOException, Unable to Write");
		} finally {
			if (out != null) {
				out.close();
			}
		}
		return fTempFile.getPath();
	}

	public void finish() {
		fTempFile.delete();
		preserveDialogSettings();
	}

	protected void preserveDialogSettings() {

		IDialogSettings section= fDialogSettings.getSection(fDialogSectionName);
		if (section == null)
			section= new DialogSettings(fDialogSectionName);

		section.put(SETTINGS_CUSTOM, fCustomButton.getSelection());
		if (fCustomButton.getSelection()) {
			section.put(SETTINGS_DOCLET, fDocletTypeText.getText());
			section.put(SETTINGS_DOCLETPATH, fDocletText.getText());
		}
		section.put(SETTINGS_STANDARD, fStandardButton.getSelection());
		section.put(SETTINGS_DESTINATION, fDestinationText.getText());
		section.put(SETTINGS_VISIBILITY, fVisibilitySelection);

		//this overwrites the last section
		fDialogSettings.addSection(section);

	}

	private void doValidation() {
		if (JavadocPreferencePage.getJavaDocCommand().length() == 0) {
			fDocletStatus= new StatusInfo();
			fDocletStatus.setError("Javadoc command location not specified on the Javadoc preference page.");
			updateStatus(fDocletStatus);
			return;
		}
		IJavaProject currentProject= getCurrentProject();
		if (currentProject != null) {			
			if (fCustomButton.getSelection()) {
				String doclet= fDocletTypeText.getText();
				String docletPath= fDocletText.getText();
				if (doclet.length() == 0) {
					fDocletStatus= new StatusInfo();
					fDocletStatus.setError("Enter a doclet name.");
					updateStatus(fDocletStatus);
				} else if (!JavaConventions.validateJavaTypeName(doclet).isOK()) {
					fDocletStatus= new StatusInfo();
					fDocletStatus.setError("Invalid doclet name.");
					updateStatus(fDocletStatus);
				} else if ((docletPath.length() == 0) || !validDocletPath(docletPath)) {
					fDocletStatus= new StatusInfo();
					fDocletStatus.setError("Not a valid doclet class path.");
					updateStatus(fDocletStatus);
				} else {
					fDocletStatus= new StatusInfo();
					updateStatus(fDocletStatus);
				}
			} else if (fStandardButton.getSelection()) {
				IPath path= new Path(fDestinationText.getText());
				if (Path.ROOT.equals(path) || Path.EMPTY.equals(path)) {
					fDestinationStatus= new StatusInfo();
					fDestinationStatus.setError("Enter the destination folder.");
					updateStatus(fDestinationStatus);
					return;
				}
				File file= new File(path.toOSString());
				if (path.isValidPath(path.toOSString()) && !file.isFile()) {
					fDestinationStatus= new StatusInfo();
					updateStatus(fDestinationStatus);
					return;
				} else {
					fDestinationStatus= new StatusInfo();
					fDestinationStatus.setError("Not a valid folder.");
					updateStatus(fDestinationStatus);
				}
			}
		} else {
			fTreeStatus= new StatusInfo();
			boolean empty= fInputGroup.getAllCheckedTreeItems().isEmpty();
			if (empty)
				fTreeStatus.setError("Select elements from tree.");
			else
				fTreeStatus.setError("Cannot generate Javadoc for elements in multiple projects.");
			updateStatus(fTreeStatus);
		}
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
				project= (IJavaProject) items[i];
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

	// copied from JarPackageWizard
	protected IStructuredSelection getValidSelection() {
		ISelection currentSelection= JavaPlugin.getActiveWorkbenchWindow().getSelectionService().getSelection();

		if (currentSelection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection= (IStructuredSelection) currentSelection;
			List selectedElements= new ArrayList(structuredSelection.size());
			Iterator iter= structuredSelection.iterator();

			IJavaProject currentProject= null;
			while (iter.hasNext()) {
				Object selectedElement= iter.next();
				IJavaElement elem= getSelectableJavaElement(selectedElement);
				if (elem != null) {
					IJavaProject jproj= elem.getJavaProject();
					if (currentProject == null || currentProject.equals(jproj)) {
						selectedElements.add(elem);
						currentProject= jproj;
					}
				}
			}
			if (!selectedElements.isEmpty()) {
				return new StructuredSelection(selectedElements);
			}
		}

		try {
			IJavaProject[] jproject= JavaCore.create(fRoot).getJavaProjects();
			if (jproject.length > 0) {
				IJavaProject currentProject= jproject[0];
				return new StructuredSelection(currentProject);
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
		return StructuredSelection.EMPTY;

	}

	private IJavaElement getSelectableJavaElement(Object obj) {
		IJavaElement je= null;
		try {
			if (obj instanceof IAdaptable) {
				je= (IJavaElement) ((IAdaptable) obj).getAdapter(IJavaElement.class);
			}

			if (je == null) {
				return null;
			}

			switch (je.getElementType()) {
				case IJavaElement.JAVA_MODEL :
				case IJavaElement.JAVA_PROJECT :
				case IJavaElement.CLASS_FILE :
					break;
				case IJavaElement.PACKAGE_FRAGMENT_ROOT :
					if (containsCompilationUnits((IPackageFragmentRoot) je)) {
						return je;
					}
					break;
				case IJavaElement.PACKAGE_FRAGMENT :
					if (containsCompilationUnits((IPackageFragment) je)) {
						return je;
					}
					break;
				default :
					ICompilationUnit cu= (ICompilationUnit) JavaModelUtil.findElementOfKind(je, IJavaElement.COMPILATION_UNIT);
					if (cu != null) {
						if (cu.isWorkingCopy()) {
							cu= (ICompilationUnit) cu.getOriginalElement();
						}
						IType primaryType= JavaModelUtil.findPrimaryType(cu);
						if (primaryType != null) {
							return primaryType;
						}
					}
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
		IJavaProject project= je.getJavaProject();
		if (project != null) {
			try {
				IPackageFragmentRoot[] roots= project.getPackageFragmentRoots();
				for (int i= 0; i < roots.length; i++) {
					if (containsCompilationUnits(roots[i])) {
						return project;
					}
				}
			} catch(JavaModelException e) {
				JavaPlugin.log(e);
			}
		}
		return null;
	}
	
	private boolean containsCompilationUnits(IPackageFragmentRoot root) throws JavaModelException {
		if (root.getKind() != IPackageFragmentRoot.K_SOURCE) {
			return false;
		}
		
		IJavaElement[] elements= root.getChildren();
		for (int i= 0; i < elements.length; i++) {
			if (elements[i] instanceof IPackageFragment) {
				IPackageFragment fragment= (IPackageFragment) elements[i];
				if (containsCompilationUnits(fragment)) {
					return true;
				}
			}
		}
		return false;
	}
	
	private boolean containsCompilationUnits(IPackageFragment pack) throws JavaModelException {
		return pack.getCompilationUnits().length > 0;
	}

	/**
	 * Finds the most severe error (if there is one)
	 */
	private IStatus findMostSevereStatus() {
		return StatusUtil.getMostSevere(new IStatus[] { fDestinationStatus, fDocletStatus, fTreeStatus });
	}

	public void init() {
		updateStatus(new StatusInfo());
	}

	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			doValidation();
		}
	}

	public IPath getDestination() {
		if (fStandardButton.getSelection()) {
			return new Path(fDestinationText.getText());
		}
		return null;
	}

} //end Class