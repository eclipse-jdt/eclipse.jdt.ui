package org.eclipse.jdt.internal.ui.preferences;

import org.eclipse.debug.internal.ui.IDebugPreferenceConstants;
import org.eclipse.jdt.internal.ui.*;
import org.eclipse.jface.preference.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.help.DialogPageContextComputer;
import org.eclipse.ui.help.WorkbenchHelp;

/**
 * Preference page for debug preferences that apply specifically to
 * Java Debugging.
 */
public class JavaDebugPreferencePage extends PreferencePage implements IWorkbenchPreferencePage, Listener {

	// UI message keys
	private static final String PREFIX= "JavaDebugPreferencePage."; //$NON-NLS-1$
	private static final String DESCRIPTION= PREFIX + "description"; //$NON-NLS-1$
	private static final String HEX= PREFIX + "hex"; //$NON-NLS-1$
	private static final String CHAR= PREFIX + "char"; //$NON-NLS-1$
	private static final String UNSIGNED= PREFIX + "unsigned"; //$NON-NLS-1$
	private static final String PRIMITIVE_PREFS= PREFIX + "primitivePrefs"; //$NON-NLS-1$
	
	// Preference store keys
	private static final String SHOW_HEX= IPreferencesConstants.SHOW_HEX_VALUES;
	private static final String SHOW_CHARS= IPreferencesConstants.SHOW_CHAR_VALUES;
	private static final String SHOW_UNSIGNED= IPreferencesConstants.SHOW_UNSIGNED_VALUES;
	
	// Preference widgets
	private Button fHexButton;
	private Button fCharButton;
	private Button fUnsignedButton;

	public JavaDebugPreferencePage() {
		super();
		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());
		setDescription(getString(DESCRIPTION)); //$NON-NLS-1$
	}
	
	/**
	 * Set the default preferences for this page.
	 */
	public static void initDefaults(IPreferenceStore store) {
		store.setDefault(SHOW_HEX, false);
		store.setDefault(SHOW_CHARS, false);
		store.setDefault(SHOW_UNSIGNED, false);		
	}

	/**
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		WorkbenchHelp.setHelp(getControl(), new DialogPageContextComputer(this, IJavaHelpContextIds.JAVA_BASE_PREFERENCE_PAGE));
		
		//The main composite
		Composite composite = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		composite.setLayout(layout);
		GridData data = new GridData();
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		composite.setLayoutData(data);		
		
		createPrimitiveDisplayPreferences(composite);
		
		setValues();
		
		return composite;		
	}

	/**
	 * Create the primitive display preferences composite widget
	 */
	private void createPrimitiveDisplayPreferences(Composite parent) {
		Group group= createGroup(parent, 1, getString(PRIMITIVE_PREFS));	
		
		fHexButton= createCheckButton(group, getString(HEX));
		fCharButton= createCheckButton(group, getString(CHAR));
		fUnsignedButton= createCheckButton(group, getString(UNSIGNED));
	}

	/**
	 * @see IWorkbenchPreferencePage#init(IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}

	/**
	 * @see PreferencePage#performOk()
	 * Also, notifies interested listeners
	 */
	public boolean performOk() {
		storeValues();
		getPreferenceStore().firePropertyChangeEvent(IPreferencesConstants.VARIABLE_RENDERING, new Boolean(true), new Boolean(false));
		return true;
	}
	
	/**
	 * Sets the default preferences
	 */
	protected void performDefaults() {
		setDefaultValues();
		super.performDefaults();	
	}
	
	private void setDefaultValues() {
		IPreferenceStore store = getPreferenceStore();
		
		fHexButton.setSelection(store.getDefaultBoolean(SHOW_HEX));
		fCharButton.setSelection(store.getDefaultBoolean(SHOW_CHARS));
		fUnsignedButton.setSelection(store.getDefaultBoolean(SHOW_UNSIGNED));			
	}
	
	/**
	 * Creates a button with the given label and sets the default 
	 * configuration data.
	 */
	private Button createCheckButton(Composite parent, String label) {
		Button button= new Button(parent, SWT.CHECK | SWT.LEFT);
		button.setText(label);
		button.addListener(SWT.Selection, this);		

		// FieldEditor GridData
		GridData data = new GridData();	
		button.setLayoutData(data);
		
		return button;
	}
	
	/**
	 * Creates group control and sets the default layout data.
	 *
	 * @param parent  the parent of the new group
	 * @param numColumns  the number of columns for the new group
	 * @param label  the label for the new group
	 * @return the newly-created coposite
	 */
	private Group createGroup(Composite parent, int numColumns, String label) {
		Group group = new Group(parent, SWT.SHADOW_NONE);
		group.setText(label);

		//GridLayout
		GridLayout layout = new GridLayout();
		layout.numColumns = numColumns;
		group.setLayout(layout);

		//GridData
		GridData data = new GridData();
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		group.setLayoutData(data);
		
		return group;
	}
	
	/**
	 * @see Listener#handleEvent(Event)
	 */
	public void handleEvent(Event event) {	
	}
	
	/**
	 * Set the values of the component widgets based on the
	 * values in the preference store
	 */
	private void setValues() {
		IPreferenceStore store = getPreferenceStore();
		
		fHexButton.setSelection(store.getBoolean(SHOW_HEX));
		fCharButton.setSelection(store.getBoolean(SHOW_CHARS));
		fUnsignedButton.setSelection(store.getBoolean(SHOW_UNSIGNED));		
	}
	
	/**
	 * Store the preference values based on the state of the
	 * component widgets
	 */
	private void storeValues() {
		IPreferenceStore store = getPreferenceStore();
		
		store.setValue(SHOW_HEX, fHexButton.getSelection());
		store.setValue(SHOW_CHARS, fCharButton.getSelection());
		store.setValue(SHOW_UNSIGNED, fUnsignedButton.getSelection());
	}
	
	/**
	 * Get the string associated with the given resource string from
	 * JavaUIMessages
	 */
	private String getString(String key) {
		return JavaUIMessages.getString(key);
	}
}

