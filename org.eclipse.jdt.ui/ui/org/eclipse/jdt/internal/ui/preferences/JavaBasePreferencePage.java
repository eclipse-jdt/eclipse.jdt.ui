/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
// AW
package org.eclipse.jdt.internal.ui.preferences;

import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.help.DialogPageContextComputer;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
	
/*
 * The page for setting java plugin preferences.
 */
public class JavaBasePreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	
	public static final String LINK_PACKAGES_TO_EDITOR= "org.eclipse.jdt.ui.packages.linktoeditor"; //$NON-NLS-1$
	public static final String LINK_TYPEHIERARCHY_TO_EDITOR= "org.eclipse.jdt.ui.packages.linktypehierarchytoeditor"; //$NON-NLS-1$
	public static final String SRCBIN_FOLDERS_IN_NEWPROJ= "org.eclipse.jdt.ui.wizards.srcBinFoldersInNewProjects"; //$NON-NLS-1$
	public static final String SRCBIN_SRCNAME= "org.eclipse.jdt.ui.wizards.srcBinFoldersSrcName"; //$NON-NLS-1$
	public static final String SRCBIN_BINNAME= "org.eclipse.jdt.ui.wizards.srcBinFoldersBinName"; //$NON-NLS-1$
	public static final String OPEN_TYPE_HIERARCHY= "org.eclipse.jdt.ui.openTypeHierarchy"; //$NON-NLS-1$
	public static final String OPEN_TYPE_HIERARCHY_IN_PERSPECTIVE= "perspective"; //$NON-NLS-1$
	public static final String OPEN_TYPE_HIERARCHY_IN_VIEW_PART= "viewPart"; //$NON-NLS-1$
	public static final String OPEN_TYPE_HIERARCHY_REUSE_PERSPECTIVE="org.eclipse.jdt.ui.typeHierarchy.reusePerspective"; //$NON-NLS-1$
	public static final String LINK_BROWSING_VIEW_TO_EDITOR= "org.eclipse.jdt.ui.browsing.linktoeditor"; //$NON-NLS-1$
	
	public static final String DOUBLE_CLICK= "packageview.doubleclick"; //$NON-NLS-1$
	public static final String DOUBLE_CLICK_GOES_INTO= "packageview.gointo"; //$NON-NLS-1$
	public static final String DOUBLE_CLICK_EXPANDS= "packageview.doubleclick.expands"; //$NON-NLS-1$
	public static final String RECONCILE_JAVA_VIEWS= "JavaUI.reconcile"; //$NON-NLS-1$

	public static boolean useSrcAndBinFolders() {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		return store.getBoolean(SRCBIN_FOLDERS_IN_NEWPROJ);
	}
	
	public static boolean linkBrowsingViewSelectionToEditor() {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		return store.getBoolean(JavaBasePreferencePage.LINK_BROWSING_VIEW_TO_EDITOR);
	}
	
	public static String getSourceFolderName() {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		return store.getString(SRCBIN_SRCNAME);
	}
	
	public static String getOutputLocationName() {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		return store.getString(SRCBIN_BINNAME);
	}		
	
	public static boolean linkPackageSelectionToEditor() {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		return store.getBoolean(LINK_PACKAGES_TO_EDITOR);
	}
	
	public static boolean linkTypeHierarchySelectionToEditor() {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		return store.getBoolean(LINK_TYPEHIERARCHY_TO_EDITOR);
	}	
		
	public static boolean openTypeHierarchyInPerspective() {
		return OPEN_TYPE_HIERARCHY_IN_PERSPECTIVE.equals(
			JavaPlugin.getDefault().getPreferenceStore().getString(OPEN_TYPE_HIERARCHY));
	}
	
	public static boolean openTypeHierarchInViewPart() {
		return OPEN_TYPE_HIERARCHY_IN_VIEW_PART.equals(
			JavaPlugin.getDefault().getPreferenceStore().getString(OPEN_TYPE_HIERARCHY));
	}
	
	public static boolean reusePerspectiveForTypeHierarchy() {
		return JavaPlugin.getDefault().getPreferenceStore().getBoolean(OPEN_TYPE_HIERARCHY_REUSE_PERSPECTIVE);
	}
	
	public static boolean doubleClickGoesInto() {
		return DOUBLE_CLICK_GOES_INTO.equals(
			JavaPlugin.getDefault().getPreferenceStore().getString(DOUBLE_CLICK));
	}

	public static boolean reconcileJavaViews() {
		return JavaPlugin.getDefault().getPreferenceStore().getBoolean(RECONCILE_JAVA_VIEWS);
	}

	private ArrayList fCheckBoxes;
	private ArrayList fRadioButtons;
	private ArrayList fTextControls;
	
	private SelectionListener fSelectionListener;
	private ModifyListener fModifyListener;
	
	
	private Button fFolderButton;
	private Text fBinFolderNameText;
	private Text fSrcFolderNameText;

	public JavaBasePreferencePage() {
		super();
		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());
		setDescription(JavaUIMessages.getString("JavaBasePreferencePage.description")); //$NON-NLS-1$
	
		fRadioButtons= new ArrayList();
		fCheckBoxes= new ArrayList();
		fTextControls= new ArrayList();
		
		fSelectionListener= new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {}

			public void widgetSelected(SelectionEvent e) {
				controlChanged(e.widget);
			}
		};
		
		fModifyListener= new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				controlModified(e.widget);
			}
		};
		
	}

	public static void initDefaults(IPreferenceStore store) {
		store.setDefault(LINK_PACKAGES_TO_EDITOR, true);
		store.setDefault(LINK_TYPEHIERARCHY_TO_EDITOR, false);
		store.setDefault(LINK_BROWSING_VIEW_TO_EDITOR, true);
		store.setDefault(OPEN_TYPE_HIERARCHY, OPEN_TYPE_HIERARCHY_IN_VIEW_PART);
		store.setDefault(OPEN_TYPE_HIERARCHY_REUSE_PERSPECTIVE, false);

		store.setDefault(SRCBIN_FOLDERS_IN_NEWPROJ, false);
		store.setDefault(SRCBIN_SRCNAME, "src"); //$NON-NLS-1$
		store.setDefault(SRCBIN_BINNAME, "bin"); //$NON-NLS-1$

		store.setDefault(DOUBLE_CLICK, DOUBLE_CLICK_EXPANDS);
		store.setDefault(RECONCILE_JAVA_VIEWS, true);
	}
	
	/*
	 * @see IWorkbenchPreferencePage#init(IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}		
	
	/**
	 * @see PreferencePage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
		WorkbenchHelp.setHelp(getControl(), IJavaHelpContextIds.JAVA_BASE_PREFERENCE_PAGE);
	}	

	private Button addCheckBox(Composite parent, String label, String key) {
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		
		Button checkBox= new Button(parent, SWT.CHECK);
		checkBox.setText(label);
		checkBox.setData(key);
		checkBox.setLayoutData(gd);
		
		checkBox.setSelection(getPreferenceStore().getBoolean(key));
		
		fCheckBoxes.add(checkBox);
		return checkBox;
	}
	
	private Button addRadioButton(Composite parent, String label, String key, String value) { 
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		
		Button button= new Button(parent, SWT.RADIO);
		button.setText(label);
		button.setData(new String[] { key, value });
		button.setLayoutData(gd);

		button.setSelection(value.equals(getPreferenceStore().getString(key)));
		
		fRadioButtons.add(button);
		return button;
	}
	
	private Text addTextControl(Composite parent, String label, String key) {
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		Label labelControl= new Label(parent, SWT.NONE);
		labelControl.setText(label);
		labelControl.setLayoutData(gd);
		
		gd= new GridData();
		gd.widthHint= convertWidthInCharsToPixels(40);
		
		Text text= new Text(parent, SWT.SINGLE | SWT.BORDER);
		text.setText(getPreferenceStore().getString(key));
		text.setData(key);
		text.setLayoutData(gd);
		
		fTextControls.add(text);
		return text;
	}	
	
	
	protected Control createContents(Composite parent) {
		initializeDialogUnits(parent);
		
		Composite composite= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		composite.setLayout(layout);

		addCheckBox(composite, JavaUIMessages.getString("JavaBasePreferencePage.reconcileJavaViews"), RECONCILE_JAVA_VIEWS); //$NON-NLS-1$
		
		fFolderButton= addCheckBox(composite, JavaUIMessages.getString("JavaBasePreferencePage.folders"), SRCBIN_FOLDERS_IN_NEWPROJ); //$NON-NLS-1$
		fFolderButton.addSelectionListener(fSelectionListener);
		
		Composite folders= new Composite(composite, SWT.NONE);
		layout= new GridLayout();
		layout.numColumns= 2;
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		folders.setLayout(layout);
		GridData gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalIndent= convertWidthInCharsToPixels(4);
		folders.setLayoutData(gd);
		
		fSrcFolderNameText= addTextControl(folders, JavaUIMessages.getString("JavaBasePreferencePage.folders.src"), SRCBIN_SRCNAME); //$NON-NLS-1$
		fBinFolderNameText= addTextControl(folders, JavaUIMessages.getString("JavaBasePreferencePage.folders.bin"), SRCBIN_BINNAME); //$NON-NLS-1$
		fSrcFolderNameText.addModifyListener(fModifyListener);
		fBinFolderNameText.addModifyListener(fModifyListener);
		
		new Label(composite, SWT.NONE); // spacer
		new Label(composite, SWT.NONE).setText("Java Browsing Settings");
		addCheckBox(composite, "&Link Java Browsing views selection to active editor", LINK_BROWSING_VIEW_TO_EDITOR);
		
		new Label(composite, SWT.NONE).setText("Package view settings");
		addCheckBox(composite, JavaUIMessages.getString("JavaBasePreferencePage.linkPackageView"), LINK_PACKAGES_TO_EDITOR); //$NON-NLS-1$

		Label doubleClickLabel= new Label(composite, SWT.NONE);
		doubleClickLabel.setText(JavaUIMessages.getString("JavaBasePreferencePage.doubleclick.action"));  //$NON-NLS-1$

		Composite doubleClickRadioGroup= new Composite(composite, SWT.NONE);
		layout= new GridLayout();
		layout.marginHeight= 0;
		doubleClickRadioGroup.setLayout(layout);		
		addRadioButton(doubleClickRadioGroup, JavaUIMessages.getString("JavaBasePreferencePage.doubleclick.gointo"), DOUBLE_CLICK, DOUBLE_CLICK_GOES_INTO); //$NON-NLS-1$
		addRadioButton(doubleClickRadioGroup, JavaUIMessages.getString("JavaBasePreferencePage.doubleclick.expand"), DOUBLE_CLICK, DOUBLE_CLICK_EXPANDS); //$NON-NLS-1$
		
		new Label(composite, SWT.NONE).setText(JavaUIMessages.getString("JavaBasePreferencePage.typeHierarchySettings"));  //$NON-NLS-1$
		addCheckBox(composite, JavaUIMessages.getString("JavaBasePreferencePage.linkTypeHierarchy"), LINK_TYPEHIERARCHY_TO_EDITOR); //$NON-NLS-1$
		
		Label label= new Label(composite, SWT.NONE);
		label.setText(JavaUIMessages.getString("JavaBasePreferencePage.openTypeHierarchy")); //$NON-NLS-1$
		
		Composite radioGroup= new Composite(composite, SWT.NONE);
		layout= new GridLayout();
		layout.marginHeight= 0;
		radioGroup.setLayout(layout);		

		addRadioButton(radioGroup, JavaUIMessages.getString("JavaBasePreferencePage.inPerspective"), OPEN_TYPE_HIERARCHY, OPEN_TYPE_HIERARCHY_IN_PERSPECTIVE);  //$NON-NLS-1$
		addRadioButton(radioGroup, JavaUIMessages.getString("JavaBasePreferencePage.inView"), OPEN_TYPE_HIERARCHY, OPEN_TYPE_HIERARCHY_IN_VIEW_PART); //$NON-NLS-1$

		/* Need support from workbench for this. See http://dev.eclipse.org/bugs/show_bug.cgi?id=3962
		final Button reuse= addCheckBox(composite, "&Reuse Type Hierarchy perspective in same window", OPEN_TYPE_HIERARCHY_REUSE_PERSPECTIVE);
		reuse.setEnabled(perspective.getSelection());
		perspective.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				reuse.setEnabled(perspective.getSelection());
			}
		});
		*/
			
		validateFolders();
	
		return composite;
	}
	
	private void validateFolders() {
		boolean useFolders= fFolderButton.getSelection();
		
		fSrcFolderNameText.setEnabled(useFolders);
		fBinFolderNameText.setEnabled(useFolders);
		if (useFolders) {
			String srcName= fSrcFolderNameText.getText();
			String binName= fBinFolderNameText.getText();
			if (srcName.length() + binName.length() == 0) {
				updateStatus(new StatusInfo(IStatus.ERROR,  JavaUIMessages.getString("JavaBasePreferencePage.folders.error.namesempty"))); //$NON-NLS-1$
				return;
			}
			IWorkspace workspace= JavaPlugin.getWorkspace();
			IStatus status;
			if (srcName.length() != 0) {
				status= workspace.validateName(srcName, IResource.FOLDER);
				if (!status.isOK()) {
					String message= JavaUIMessages.getFormattedString("JavaBasePreferencePage.folders.error.invalidsrcname", status.getMessage()); //$NON-NLS-1$
					updateStatus(new StatusInfo(IStatus.ERROR, message));
					return;
				}
			}
			status= workspace.validateName(binName, IResource.FOLDER);
			if (!status.isOK()) {
				String message= JavaUIMessages.getFormattedString("JavaBasePreferencePage.folders.error.invalidbinname", status.getMessage()); //$NON-NLS-1$
				updateStatus(new StatusInfo(IStatus.ERROR, message));
				return;
			}
			IProject dmy= workspace.getRoot().getProject("dmy"); //$NON-NLS-1$
			IClasspathEntry entry= JavaCore.newSourceEntry(dmy.getFullPath().append(srcName));
			IPath outputLocation= dmy.getFullPath().append(binName);
			status= JavaConventions.validateClasspath(JavaCore.create(dmy), new IClasspathEntry[] { entry }, outputLocation);
			if (!status.isOK()) {
				updateStatus(status);
				return;
			}
		}
		updateStatus(new StatusInfo()); // set to OK
	}
		
	private void updateStatus(IStatus status) {
		setValid(!status.matches(IStatus.ERROR));
		StatusUtil.applyToStatusLine(this, status);
	}		
	
	private void controlChanged(Widget widget) {
		if (widget == fFolderButton) {
			validateFolders();
		}
	}
	
	private void controlModified(Widget widget) {
		if (widget == fSrcFolderNameText || widget == fBinFolderNameText) {
			validateFolders();
		}
	}	
	
	/*
	 * @see PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		IPreferenceStore store= getPreferenceStore();
		for (int i= 0; i < fCheckBoxes.size(); i++) {
			Button button= (Button) fCheckBoxes.get(i);
			String key= (String) button.getData();
			button.setSelection(store.getDefaultBoolean(key));
		}
		for (int i= 0; i < fRadioButtons.size(); i++) {
			Button button= (Button) fRadioButtons.get(i);
			String[] info= (String[]) button.getData();
			button.setSelection(info[1].equals(store.getDefaultString(info[0])));
		}
		for (int i= 0; i < fTextControls.size(); i++) {
			Text text= (Text) fTextControls.get(i);
			String key= (String) text.getData();
			text.setText(store.getDefaultString(key));
		}
		validateFolders();
		super.performDefaults();
	}

	/*
	 * @see IPreferencePage#performOk()
	 */
	public boolean performOk() {
		IPreferenceStore store= getPreferenceStore();
		for (int i= 0; i < fCheckBoxes.size(); i++) {
			Button button= (Button) fCheckBoxes.get(i);
			String key= (String) button.getData();
			store.setValue(key, button.getSelection());
		}
		for (int i= 0; i < fRadioButtons.size(); i++) {
			Button button= (Button) fRadioButtons.get(i);
			if (button.getSelection()) {
				String[] info= (String[]) button.getData();
				store.setValue(info[0], info[1]);
			}
		}
		for (int i= 0; i < fTextControls.size(); i++) {
			Text text= (Text) fTextControls.get(i);
			String key= (String) text.getData();
			store.setValue(key, text.getText());
		}			
		
		JavaPlugin.getDefault().savePluginPreferences();
		return super.performOk();
	}
}


