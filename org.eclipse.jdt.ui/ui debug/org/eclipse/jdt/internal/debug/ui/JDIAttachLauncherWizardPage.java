package org.eclipse.jdt.internal.debug.ui;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import com.sun.jdi.connect.AttachingConnector;import com.sun.jdi.connect.Connector;import java.util.Map;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.WizardPage;

import org.eclipse.ui.help.DialogPageContextComputer;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.IPreferencesConstants;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;

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
	private static final String HOST_ERROR= ERROR + "host";

	
	private String fInitialHost;
	private String fInitialPort;
	private boolean fAllowTerminate;

	private Text fPortField;
	private Text fHostField;
	private Button fAllowTerminateButton;

	private static final int SIZING_TEXT_FIELD_WIDTH= 250;
	private static final int SIZING_INDENTATION_WIDTH= 10;

	private Connector.Argument fHostParam= null;
	private Connector.Argument fPortParam= null;
	
	/**
	 * Constructs the page.
	 */
	public JDIAttachLauncherWizardPage() {
		super(DebugUIUtils.getResourceString(PREFIX + "title"));
		setImageDescriptor(JavaPluginImages.DESC_WIZBAN_JAVA_ATTACH);
		setDescription(DebugUIUtils.getResourceString(PREFIX + "description"));
		
		AttachingConnector connector= JDIAttachLauncher.getAttachingConnector();
		
		if (connector != null) {
			Map map= connector.defaultArguments();
			fHostParam= (Connector.Argument) map.get("hostname");
			fPortParam= (Connector.Argument) map.get("port");
		}
	}

	/**
	 * Creates the control and contents of the page - three groups
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
		WorkbenchHelp.setHelp(composite, new DialogPageContextComputer(this, IJavaHelpContextIds.JDI_ATTACH_LAUNCHER_WIZARD_PAGE));		
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
	 * Initialize the settings of the page.
	 */
	protected void initializeSettings() {
		Runnable runnable= new Runnable() {
			public void run() {
				if (getControl().isDisposed()) {
					return;
				}
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

	protected void setPreferenceValues() {
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
		new Label(parent, SWT.NONE);
		
		// add terminate check box
		fAllowTerminateButton= new Button(parent, SWT.CHECK);
		fAllowTerminateButton.setText(DebugUIUtils.getResourceString(ALLOW_TERMINATE));
		fAllowTerminateButton.setSelection(fAllowTerminate);
	}
	
	/**
	 * @see Listener
	 */
	public void handleEvent(Event ev) {
		boolean valid= validatePage();
		if (valid) {
			setErrorMessage(null);
		}
		setPageComplete(valid);
	}

	/**
	 * Returns whether this page's visual components
	 * currently all contain valid values.
	 */
	protected boolean validatePage() {
		return validatePortGroup() && validateHostGroup();
	}

	/**
	 * Returns whether this page's port name specification
	 * group's visual components currently all contain valid values.
	 */
	protected boolean validatePortGroup() {
		String portFieldContents= fPortField.getText();
		if (portFieldContents.equals("")) {
			setErrorMessage(DebugUIUtils.getResourceString(PORT_ERROR));
			return false;
		}
		if (fPortParam != null) {
			if (!fPortParam.isValid(portFieldContents)) {
				setErrorMessage(DebugUIUtils.getResourceString(PORT_ERROR));
				return false;
			}
			return true;
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
	 * Returns whether this page's host name specification
	 * group's visual components currently all contain valid values.
	 */
	protected boolean validateHostGroup() {
		String host= fHostField.getText();
		if (fHostParam != null) {
			if (!fHostParam.isValid(host)) {
				setErrorMessage(DebugUIUtils.getResourceString(HOST_ERROR));
			}
		}
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
