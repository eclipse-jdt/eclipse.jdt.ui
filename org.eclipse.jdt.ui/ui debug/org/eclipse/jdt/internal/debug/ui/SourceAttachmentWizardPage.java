package org.eclipse.jdt.internal.debug.ui;
/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000
 */
 import org.eclipse.core.runtime.IPath;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.JavaPluginImages;import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;import org.eclipse.jdt.internal.ui.preferences.SourceAttachmentPropertyPage;import org.eclipse.jdt.ui.JavaElementLabelProvider;import org.eclipse.jface.preference.IPreferencePageContainer;import org.eclipse.jface.preference.IPreferenceStore;import org.eclipse.jface.wizard.IWizard;import org.eclipse.jface.wizard.IWizardPage;import org.eclipse.swt.SWT;import org.eclipse.swt.events.SelectionEvent;import org.eclipse.swt.events.SelectionListener;import org.eclipse.swt.layout.GridLayout;import org.eclipse.swt.widgets.Button;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Control;import org.eclipse.swt.widgets.Label;

/**
 * A wizard page to attach source at debug time.
 */
public class SourceAttachmentWizardPage extends SourceAttachmentPropertyPage implements IWizardPage, IPreferencePageContainer {
	
	private final static String PREFIX = "source_attachment_wizard_page.";
	private final static String PAGE_TITLE = PREFIX + "title";
	private final static String PAGE_DESCRIPTION = PREFIX + "description";
	private final static String NO_SOURCE = PREFIX + "no_source";
	
	private IWizard fWizard;
	private Button fNoSourceButton;
	private Control fPrefControl;
	private boolean fNoSource = false;
	
	/**
	 * @see IWizardPage#setWizard(org.eclipse.jface.wizard.IWizard)
	 */
	public void setWizard(IWizard wizard) {
		fWizard = wizard;
	}

	/**
	 * @see IWizardPage#setPreviousPage(org.eclipse.jface.wizard.IWizardPage)
	 */
	public void setPreviousPage(IWizardPage page) {
	}

	/**
	 * @see IWizardPage#isPageComplete()
	 */
	public boolean isPageComplete() {
		IPath path = getSourceAttachmentBlock().getSourceAttachmentPath();
		return fNoSourceButton.getSelection() || (isValid() && path != null && !path.isEmpty());
	}

	/**
	 * @see IWizardPage#getWizard()
	 */
	public IWizard getWizard() {
		return fWizard;
	}

	/**
	 * @see IWizardPage#getPreviousPage()
	 */
	public IWizardPage getPreviousPage() {
		return null;
	}

	/**
	 * @see IWizardPage#getNextPage()
	 */
	public IWizardPage getNextPage() {
		return null;
	}

	/**
	 * @see IWizardPage#canFlipToNextPage()
	 */
	public boolean canFlipToNextPage() {
		return false;
	}


	/**
	 * @see IDialogPage
	 */
	public void createControl(Composite parent) {
		
		
		Composite root= new Composite(parent, SWT.NONE);
		GridLayout l= new GridLayout();
		l.numColumns= 1;
		l.verticalSpacing = 10;
		l.makeColumnsEqualWidth= true;
		root.setLayout(l);
		
		createJarLabel(root);
		
		fPrefControl = createContents(root);

		setTitle(DebugUIUtils.getResourceString(PAGE_TITLE));
		setDescription(DebugUIUtils.getResourceString(PAGE_DESCRIPTION));
		setImageDescriptor(JavaPluginImages.DESC_WIZBAN_JAVA_LAUNCH);
		
		// spacer
		new Label(root, SWT.NONE);
		
		fNoSourceButton= new Button(root, SWT.CHECK);
		fNoSourceButton.setText(DebugUIUtils.getResourceString(NO_SOURCE));
		fNoSourceButton.addSelectionListener(
			new SelectionListener() {
				public void widgetSelected(SelectionEvent e) {
					updateButtons();
				}

				public void widgetDefaultSelected(SelectionEvent e) {
					updateButtons();
				}
				
			}
		);
		
		setControl(root);
	}
	
	protected void createJarLabel(Composite parent) {
		Composite root= new Composite(parent, SWT.NONE);
		GridLayout l= new GridLayout();
		l.numColumns= 2;
		root.setLayout(l);

		JavaElementLabelProvider lp = new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_SMALL_ICONS);
		Label label1 = new Label(root, SWT.NONE);
		label1.setImage(lp.getImage(getElement()));
		Label label2 = new Label(root, SWT.NONE);
		label2.setText(((IJavaElement)getElement()).getElementName());

	}
	
	/**
	 * @see IWizardPage#getName()
	 */
	public String getName() {
		return DebugUIUtils.getResourceString(PAGE_TITLE);
	}
	
	public IPreferencePageContainer getContainer() {
		return this;
	}

	/**
	 * @see IPreferencePageContainer#updateTitle()
	 */
	public void updateTitle() {
		if (getControl() != null) 
			getWizard().getContainer().updateTitleBar();
	}

	/**
	 * @see IPreferencePageContainer#updateMessage()
	 */
	public void updateMessage() {
		if (getControl() != null)
			getWizard().getContainer().updateMessage();
	}

	/**
	 * @see IPreferencePageContainer#updateButtons()
	 */
	public void updateButtons() {
		if (getControl() != null) {
			fNoSource = fNoSourceButton.getSelection();
			fPrefControl.setEnabled(!fNoSource);
			getWizard().getContainer().updateButtons();
		}
	}

	/**
	 * @see IPreferencePageContainer
	 */
	public IPreferenceStore getPreferenceStore() {
		return JavaPlugin.getDefault().getPreferenceStore();
	}	
	
	public void statusInfoChanged(StatusInfo status) {
		super.statusChanged(status);
		updateButtons();
	}
	
	public boolean isNoSource() {
		return fNoSource;
	}
}
