package org.eclipse.jdt.internal.debug.ui;

/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2001
 */

import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jdt.internal.ui.IPreferencesConstants;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

/**
 * The main page in a <code>JDIAttachLauncherWizard</code>. 
 */
public class JDIAttachLauncherWizardPage extends WizardPage implements Listener {

	private static final String PREFIX= "jdi_attach_launcher_wizard_page.";

	private static final String PORT= PREFIX + "port";
	private static final String HOST= PREFIX + "host";
	private static final String ALLOW_TERMINATE= PREFIX + "allowTerminate";
	private static final String ERROR= PREFIX + "error.";
	private static final String PORT_ERROR= ERROR + "port";

	private String fInitialHost;
	private String fInitialPort;
	private boolean fAllowTerminate;

	// widgets
	private Text fPortField;
	private Text fHostField;
	private Button fAllowTerminateButton;

	// constants
	private static final int SIZING_TEXT_FIELD_WIDTH= 250;
	private static final int SIZING_INDENTATION_WIDTH= 10;

	/**
	 * Constructs a <code>JDIAttachLauncherWizardPage</code> with the given launcher and pre-computed children
	 */
	public JDIAttachLauncherWizardPage() {
		super(DebugUIUtils.getResourceString(PREFIX + "title"));
		setImageDescriptor(DebugUITools.getImageDescriptor(IDebugUIConstants.IMG_ACT_DEBUG));
		setDescription(DebugUIUtils.getResourceString(PREFIX + "description"));
	}

	/**
	 * Creates the control and contents of the page - three fields
	 */
	public void createControl(Composite ancestor) {
		Composite composite= new Composite(ancestor, SWT.NULL);

		composite.addHelpListener(new HelpListener() {
			public void helpRequested(HelpEvent event) {
				performHelp();
			}
		});

		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));

		getPreferenceValues();
		
		// create a 2 column layout for the other controls
		Composite pageGroup= new Composite(composite, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		pageGroup.setLayout(layout);
		pageGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		createHostGroup(pageGroup);
		createPortGroup(pageGroup);
		createTerminateGroup(pageGroup);

		initializeSettings();
		setControl(composite);
	}

	/**
	 * Convenience method to set the message line
	 */
	public void setMessage(String message) {
		super.setErrorMessage(null);
		super.setMessage(message);
	}

	/**
	 * Convenience method to set the error line
	 */
	public void setErrorMessage(String message) {
		super.setMessage(null);
		super.setErrorMessage(message);
	}

	/**
	 * Initialize the settings:<ul>
	 * <li>Put the cursor in the project name field area
	 * </ul>
	 */
	protected void initializeSettings() {
		Runnable runnable= new Runnable() {
			public void run() {
				fHostField.setFocus();
				setTitle(DebugUIUtils.getResourceString(PREFIX + "title"));
				setPageComplete(true);
			}
		};
		Display.getCurrent().asyncExec(runnable);
	}

	protected void getPreferenceValues() {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		fInitialPort= store.getString(IPreferencesConstants.ATTACH_LAUNCH_PORT);
		fInitialHost= store.getString(IPreferencesConstants.ATTACH_LAUNCH_HOST);
		fAllowTerminate= store.getBoolean(IPreferencesConstants.ATTACH_LAUNCH_ALLOW_TERMINATE);
	}

	public void setPreferenceValues() {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(IPreferencesConstants.ATTACH_LAUNCH_PORT, getPort());
		store.setValue(IPreferencesConstants.ATTACH_LAUNCH_HOST, getHost());
		store.setValue(IPreferencesConstants.ATTACH_LAUNCH_ALLOW_TERMINATE, getAllowTerminate());
	}

	/**
	 * Creates the host name specification visual components.
	 *	 
	 * @see org.eclipse.swt.widgets.Composite
	 */
	protected void createHostGroup(Composite parent) {
		// new host label
		Label hostLabel= new Label(parent, SWT.NONE);
		hostLabel.setText(DebugUIUtils.getResourceString(HOST));

		// new host entry field
		fHostField= new Text(parent, SWT.BORDER);

		GridData data= new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint= SIZING_TEXT_FIELD_WIDTH;
		fHostField.setLayoutData(data);

		if (fInitialHost != null) {
			fHostField.setText(fInitialHost);
		}
		fHostField.addListener(SWT.Modify, this);
	}

	/**
	 * Creates the port specification visual components.
	 *	 
	 * @see org.eclipse.swt.widgets.Composite
	 */
	protected void createPortGroup(Composite parent) {
		// new port label
		Label portLabel= new Label(parent, SWT.NONE);
		portLabel.setText(DebugUIUtils.getResourceString(PORT));

		// new port entry field
		fPortField= new Text(parent, SWT.BORDER);
		GridData data= new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint= SIZING_TEXT_FIELD_WIDTH;
		fPortField.setLayoutData(data);

		if (fInitialPort != null) {
			fPortField.setText(fInitialPort);
		}
		fPortField.addListener(SWT.Modify, this);
	}

	/**
	 * Creates "allow terminate" visual components.
	 *	 
	 * @see org.eclipse.swt.widgets.Composite
	 */
	protected void createTerminateGroup(Composite parent) {
		// add empty label
		Label l= new Label(parent, SWT.NONE);
		
		// add terminate check box
		fAllowTerminateButton= new Button(parent, SWT.CHECK);
		fAllowTerminateButton.setText(DebugUIUtils.getResourceString(ALLOW_TERMINATE));
		fAllowTerminateButton.setSelection(fAllowTerminate);
	}
	
	/**
	 * @see Listener
	 */
	public void handleEvent(Event ev) {
		Widget source= ev.widget;
		setPageComplete(validatePage());
	}

	/**
	 * Returns a <code>boolean</code> indicating whether this page's visual
	 * components currently all contain valid values.
	 */
	protected boolean validatePage() {
		return validatePortGroup() && validateHostGroup();
	}

	/**
	 * Returns a <code>boolean</code> indicating whether this page's port name
	 * specification group's visual components currently all contain valid values.
	 */
	protected boolean validatePortGroup() {
		String portFieldContents= fPortField.getText();
		if (portFieldContents.equals("")) {
			setErrorMessage(DebugUIUtils.getResourceString(PORT_ERROR));
			return false;
		}
		try {
			Integer.parseInt(portFieldContents);
		} catch (NumberFormatException nfe) {
			setErrorMessage(DebugUIUtils.getResourceString(PORT_ERROR));
			return false;
		}
		return true;
	}

	/**
	 * Returns a <code>boolean</code> indicating whether this page's host name
	 * specification group's visual components currently all contain valid values.
	 */
	protected boolean validateHostGroup() {
		String host= fHostField.getText();
		return true;
	}

	protected String getPort() {
		return fPortField.getText();
	}

	protected String getHost() {
		return fHostField.getText();
	}

	protected boolean getAllowTerminate() {
		return fAllowTerminateButton.getSelection();
	}
	
}
