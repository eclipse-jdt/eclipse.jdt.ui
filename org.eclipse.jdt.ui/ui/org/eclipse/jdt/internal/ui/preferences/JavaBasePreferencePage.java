/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
// AW
package org.eclipse.jdt.internal.ui.preferences;

import java.util.ArrayList;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
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
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
	
/*
 * The page for setting general java plugin preferences.
 * See PreferenceConstants to access or change these values through public API.
 */
public class JavaBasePreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String LINK_PACKAGES_TO_EDITOR= PreferenceConstants.LINK_PACKAGES_TO_EDITOR;
	public static final String LINK_TYPEHIERARCHY_TO_EDITOR= PreferenceConstants.LINK_TYPEHIERARCHY_TO_EDITOR;
	public static final String OPEN_TYPE_HIERARCHY= PreferenceConstants.OPEN_TYPE_HIERARCHY;
	public static final String OPEN_TYPE_HIERARCHY_IN_PERSPECTIVE= PreferenceConstants.OPEN_TYPE_HIERARCHY_IN_PERSPECTIVE;
	public static final String OPEN_TYPE_HIERARCHY_IN_VIEW_PART= PreferenceConstants.OPEN_TYPE_HIERARCHY_IN_VIEW_PART;
	public static final String LINK_BROWSING_VIEW_TO_EDITOR= PreferenceConstants.LINK_BROWSING_VIEW_TO_EDITOR;

	public static final String DOUBLE_CLICK= PreferenceConstants.DOUBLE_CLICK;
	public static final String DOUBLE_CLICK_GOES_INTO= PreferenceConstants.DOUBLE_CLICK_GOES_INTO;
	public static final String DOUBLE_CLICK_EXPANDS= PreferenceConstants.DOUBLE_CLICK_EXPANDS;

	public static final String UPDATE_JAVA_VIEWS= PreferenceConstants.UPDATE_JAVA_VIEWS;
	public static final String UPDATE_ON_SAVE= PreferenceConstants.UPDATE_ON_SAVE;
	public static final String UPDATE_WHILE_EDITING= PreferenceConstants.UPDATE_WHILE_EDITING;

	public static boolean linkBrowsingViewSelectionToEditor() {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		return store.getBoolean(JavaBasePreferencePage.LINK_BROWSING_VIEW_TO_EDITOR);
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
		return false;
		//return JavaPlugin.getDefault().getPreferenceStore().getBoolean(OPEN_TYPE_HIERARCHY_REUSE_PERSPECTIVE);
	}
	
	public static boolean doubleClickGoesInto() {
		return DOUBLE_CLICK_GOES_INTO.equals(JavaPlugin.getDefault().getPreferenceStore().getString(DOUBLE_CLICK));
	}

	public static boolean reconcileJavaViews() {
		String update= JavaPlugin.getDefault().getPreferenceStore().getString(UPDATE_JAVA_VIEWS);
		return UPDATE_WHILE_EDITING.equals(update);
	}

	private ArrayList fCheckBoxes;
	private ArrayList fRadioButtons;
	private ArrayList fTextControls;
	
	private SelectionListener fSelectionListener;
	private ModifyListener fModifyListener;

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

	/*
	public static void initDefaults(IPreferenceStore store) {
		store.setDefault(LINK_PACKAGES_TO_EDITOR, true);
		store.setDefault(LINK_TYPEHIERARCHY_TO_EDITOR, false);
		store.setDefault(LINK_BROWSING_VIEW_TO_EDITOR, true);
		store.setDefault(OPEN_TYPE_HIERARCHY, OPEN_TYPE_HIERARCHY_IN_VIEW_PART);
		//store.setDefault(OPEN_TYPE_HIERARCHY_REUSE_PERSPECTIVE, false);

		store.setDefault(DOUBLE_CLICK, DOUBLE_CLICK_EXPANDS);
		store.setDefault(UPDATE_JAVA_VIEWS, UPDATE_WHILE_EDITING);
	}
	*/

	/*
	 * @see IWorkbenchPreferencePage#init(IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}		
	
	/*
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
		
		new Label(composite, SWT.NONE); // spacer
		new Label(composite, SWT.NONE).setText(JavaUIMessages.getString("JavaBasePreferencePage.linkSettings.text")); //$NON-NLS-1$
		addCheckBox(composite, JavaUIMessages.getString("JavaBasePreferencePage.linkJavaBrowsingViewsCheckbox.text"), LINK_BROWSING_VIEW_TO_EDITOR); //$NON-NLS-1$
		addCheckBox(composite, JavaUIMessages.getString("JavaBasePreferencePage.linkPackageView"), LINK_PACKAGES_TO_EDITOR); //$NON-NLS-1$
		addCheckBox(composite, JavaUIMessages.getString("JavaBasePreferencePage.linkTypeHierarchy"), LINK_TYPEHIERARCHY_TO_EDITOR); //$NON-NLS-1$

		new Label(composite, SWT.NONE); // spacer
		Label doubleClickLabel1= new Label(composite, SWT.NONE);
		doubleClickLabel1.setText(JavaUIMessages.getString("JavaBasePreferencePage.updateJavaViews")); //$NON-NLS-1$

		Composite doubleClickRadioGroup1= new Composite(composite, SWT.NONE);
		layout= new GridLayout();
		layout.marginHeight= 0;
		doubleClickRadioGroup1.setLayout(layout);		
		addRadioButton(doubleClickRadioGroup1, JavaUIMessages.getString("JavaBasePreferencePage.onSave"), UPDATE_JAVA_VIEWS, UPDATE_ON_SAVE); //$NON-NLS-1$
		addRadioButton(doubleClickRadioGroup1, JavaUIMessages.getString("JavaBasePreferencePage.whileEditing"), UPDATE_JAVA_VIEWS, UPDATE_WHILE_EDITING);  //$NON-NLS-1$
		Label notice= new Label(composite, SWT.WRAP);
		notice.setText(JavaUIMessages.getString("JavaBasePreferencePage.notice.outliner"));  //$NON-NLS-1$
		GridData noticeData= new GridData(GridData.FILL_HORIZONTAL);
		noticeData.grabExcessHorizontalSpace= true;
		noticeData.widthHint= convertWidthInCharsToPixels(60);
		notice.setLayoutData(noticeData);

		new Label(composite, SWT.NONE); // spacer
		Label doubleClickLabel= new Label(composite, SWT.NONE);
		doubleClickLabel.setText(JavaUIMessages.getString("JavaBasePreferencePage.doubleclick.action"));  //$NON-NLS-1$

		Composite doubleClickRadioGroup= new Composite(composite, SWT.NONE);
		layout= new GridLayout();
		layout.marginHeight= 0;
		doubleClickRadioGroup.setLayout(layout);		
		addRadioButton(doubleClickRadioGroup, JavaUIMessages.getString("JavaBasePreferencePage.doubleclick.gointo"), DOUBLE_CLICK, DOUBLE_CLICK_GOES_INTO); //$NON-NLS-1$
		addRadioButton(doubleClickRadioGroup, JavaUIMessages.getString("JavaBasePreferencePage.doubleclick.expand"), DOUBLE_CLICK, DOUBLE_CLICK_EXPANDS); //$NON-NLS-1$

		new Label(composite, SWT.NONE); // spacer
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

	
		return composite;
	}
	
		
	private void updateStatus(IStatus status) {
		setValid(!status.matches(IStatus.ERROR));
		StatusUtil.applyToStatusLine(this, status);
	}		
	
	private void controlChanged(Widget widget) {
	}
	
	private void controlModified(Widget widget) {
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


