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
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.dialogs.Dialog;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.util.SWTUtil;

public class JavadocSpecificsWizardPage extends JavadocWizardPage {

	private Button fAntBrowseButton;
	private Button fCheckbrowser;
	private Text fAntText;
	private Button fOverViewButton;
	private Button fOverViewBrowseButton;
	private Button fAntButton;
	private Button fJDK14Button;
	
	private Composite fLowerComposite;
	private Text fOverViewText;
	private Text fExtraOptionsText;
	private Text fVMOptionsText;

	private StatusInfo fOverviewStatus;
	private StatusInfo fAntStatus;

	private JavadocOptionsManager fStore;

	private final int OVERVIEWSTATUS= 1;
	private final int ANTSTATUS= 2;


	protected JavadocSpecificsWizardPage(String pageName, JavadocOptionsManager store) {
		super(pageName);
		setDescription(JavadocExportMessages.getString("JavadocSpecificsWizardPage.description")); //$NON-NLS-1$

		fStore= store;

		fOverviewStatus= new StatusInfo();
		fAntStatus= new StatusInfo();
	}

	/*
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);

		fLowerComposite= new Composite(parent, SWT.NONE);
		fLowerComposite.setLayoutData(createGridData(GridData.FILL_BOTH, 1, 0));

		GridLayout layout= createGridLayout(3);
		layout.marginHeight= 0;
		fLowerComposite.setLayout(layout);

		createExtraOptionsGroup(fLowerComposite);
		createAntGroup(fLowerComposite);

		setControl(fLowerComposite);
		Dialog.applyDialogFont(fLowerComposite);
		WorkbenchHelp.setHelp(fLowerComposite, IJavaHelpContextIds.JAVADOC_SPECIFICS_PAGE);

	} //end method createControl

	private void createExtraOptionsGroup(Composite composite) {
		Composite c= new Composite(composite, SWT.NONE);
		c.setLayout(createGridLayout(3));
		c.setLayoutData(createGridData(GridData.FILL_HORIZONTAL, 3, 0));
		((GridLayout) c.getLayout()).marginWidth= 0;

		fOverViewButton= createButton(c, SWT.CHECK, JavadocExportMessages.getString("JavadocSpecificsWizardPage.overviewbutton.label"), createGridData(1)); //$NON-NLS-1$
		fOverViewText= createText(c, SWT.SINGLE | SWT.BORDER, null, createGridData(GridData.FILL_HORIZONTAL, 1, 0));
		//there really aught to be a way to specify this
		 ((GridData) fOverViewText.getLayoutData()).widthHint= 200;
		fOverViewBrowseButton= createButton(c, SWT.PUSH, JavadocExportMessages.getString("JavadocSpecificsWizardPage.overviewbrowse.label"), createGridData(GridData.HORIZONTAL_ALIGN_END, 1, 0)); //$NON-NLS-1$
		SWTUtil.setButtonDimensionHint(fOverViewBrowseButton);

		String str= fStore.getOverview();
		if (str.length() == 0) {
			//default
			fOverViewText.setEnabled(false);
			fOverViewBrowseButton.setEnabled(false);
		} else {
			fOverViewButton.setSelection(true);
			fOverViewText.setText(str);
		}

		createLabel(composite, SWT.NONE, JavadocExportMessages.getString("JavadocSpecificsWizardPage.vmoptionsfield.label"), createGridData(GridData.HORIZONTAL_ALIGN_BEGINNING, 3, 0)); //$NON-NLS-1$
		fVMOptionsText= createText(composite, SWT.SINGLE | SWT.BORDER, null, createGridData(GridData.HORIZONTAL_ALIGN_FILL, 3, 0));
		fVMOptionsText.setText(fStore.getVMParams());
		
		
		createLabel(composite, SWT.NONE, JavadocExportMessages.getString("JavadocSpecificsWizardPage.extraoptionsfield.label"), createGridData(GridData.HORIZONTAL_ALIGN_BEGINNING, 3, 0)); //$NON-NLS-1$
		fExtraOptionsText= createText(composite, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.H_SCROLL | SWT.V_SCROLL, null, createGridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.FILL_VERTICAL, 3, 0));
		//fExtraOptionsText.setSize(convertWidthInCharsToPixels(60), convertHeightInCharsToPixels(10));

		fExtraOptionsText.setText(fStore.getAdditionalParams());

		fJDK14Button= createButton(composite, SWT.CHECK, JavadocExportMessages.getString("JavadocSpecificsWizardPage.jdk14mode.label"), createGridData(3)); //$NON-NLS-1$
		fJDK14Button.setSelection(fStore.isJDK14Mode());
		

		//Listeners
		fOverViewButton.addSelectionListener(new ToggleSelectionAdapter(new Control[] { fOverViewBrowseButton, fOverViewText }) {
			public void validate() {
				doValidation(OVERVIEWSTATUS);
			}
		});

		fOverViewText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				doValidation(OVERVIEWSTATUS);
			}
		});

		fOverViewBrowseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				handleFileBrowseButtonPressed(fOverViewText, new String[] { "*.html" }, JavadocExportMessages.getString("JavadocSpecificsWizardPage.overviewbrowsedialog.title")); //$NON-NLS-1$ //$NON-NLS-2$
			}
		});

	}

	private void createAntGroup(Composite composite) {
		Composite c= new Composite(composite, SWT.NONE);
		c.setLayout(createGridLayout(3));
		c.setLayoutData(createGridData(GridData.FILL_HORIZONTAL, 3, 0));
		((GridLayout) c.getLayout()).marginWidth= 0;

		fAntButton= createButton(c, SWT.CHECK, JavadocExportMessages.getString("JavadocSpecificsWizardPage.antscriptbutton.label"), createGridData(3)); //$NON-NLS-1$
		createLabel(c, SWT.NONE, JavadocExportMessages.getString("JavadocSpecificsWizardPage.antscripttext.label"), createGridData(GridData.HORIZONTAL_ALIGN_BEGINNING, 1, 0)); //$NON-NLS-1$
		fAntText= createText(c, SWT.SINGLE | SWT.BORDER, null, createGridData(GridData.FILL_HORIZONTAL, 1, 0));
		//there really aught to be a way to specify this
		 ((GridData) fAntText.getLayoutData()).widthHint= 200;

		fAntText.setText(fStore.getAntpath());

		fAntBrowseButton= createButton(c, SWT.PUSH, JavadocExportMessages.getString("JavadocSpecificsWizardPage.antscriptbrowse.label"), createGridData(GridData.HORIZONTAL_ALIGN_END, 1, 0)); //$NON-NLS-1$
		SWTUtil.setButtonDimensionHint(fAntBrowseButton);
		fAntText.setEnabled(false);
		fAntBrowseButton.setEnabled(false);
		
		fCheckbrowser= createButton(c, SWT.CHECK, JavadocExportMessages.getString("JavadocSpecificsWizardPage.openbrowserbutton.label"), createGridData(3)); //$NON-NLS-1$
		fCheckbrowser.setSelection(fStore.doOpenInBrowser());

		fAntButton.addSelectionListener(new ToggleSelectionAdapter(new Control[] { fAntText, fAntBrowseButton }) {
			public void validate() {
				doValidation(ANTSTATUS);
			}
		});

		fAntText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				doValidation(ANTSTATUS);
			}
		});

		fAntBrowseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {

				String temp= fAntText.getText();
				IPath path= new Path(temp);
				String file= path.lastSegment();
				if (file == null)
					file= "javadoc.xml";//$NON-NLS-1$
				path= path.removeLastSegments(1);

				temp= handleFolderBrowseButtonPressed(path.toOSString(), fAntText.getShell(), JavadocExportMessages.getString("JavadocSpecificsWizardPage.antscriptbrowsedialog.title"), JavadocExportMessages.getString("JavadocSpecificsWizardPage.antscriptbrowsedialog.label")); //$NON-NLS-1$ //$NON-NLS-2$

				path= new Path(temp);
				path= path.addTrailingSeparator().append(file);
				fAntText.setText(path.toOSString());

			}
		});
	} //end method createExtraOptionsGroup

	private void doValidation(int val) {
		File file= null;
		String ext= null;
		Path path= null;

		switch (val) {

			case OVERVIEWSTATUS :
				fOverviewStatus= new StatusInfo();
				if (fOverViewButton.getSelection()) {
					path= new Path(fOverViewText.getText());
					file= path.toFile();
					ext= path.getFileExtension();
					if ((file == null) || !file.exists()) {
						fOverviewStatus.setError(JavadocExportMessages.getString("JavadocSpecificsWizardPage.overviewnotfound.error")); //$NON-NLS-1$
					} else if ((ext == null) || !ext.equalsIgnoreCase("html")) { //$NON-NLS-1$
						fOverviewStatus.setError(JavadocExportMessages.getString("JavadocSpecificsWizardPage.overviewincorrect.error")); //$NON-NLS-1$
					}
				}
				break;
			case ANTSTATUS :
				fAntStatus= new StatusInfo();
				if (fAntButton.getSelection()) {
					path= new Path(fAntText.getText());
					ext= path.getFileExtension();
					IPath antSeg= path.removeLastSegments(1);

					if ((!antSeg.isValidPath(antSeg.toOSString())) || (ext == null) || !(ext.equalsIgnoreCase("xml"))) //$NON-NLS-1$
						fAntStatus.setError(JavadocExportMessages.getString("JavadocSpecificsWizardPage.antfileincorrect.error")); //$NON-NLS-1$
					else if (path.toFile().exists())
						fAntStatus.setWarning(JavadocExportMessages.getString("JavadocSpecificsWizardPage.antfileoverwrite.warning")); //$NON-NLS-1$
				}
				break;
		}

		updateStatus(findMostSevereStatus());

	}

	/*
	 * @see JavadocWizardPage#onFinish()
	 */

	protected void updateStore() {

		fStore.setVMParams(fVMOptionsText.getText());
		fStore.setAdditionalParams(fExtraOptionsText.getText());

		if (fOverViewText.getEnabled())
			fStore.setOverview(fOverViewText.getText());
		else
			fStore.setOverview(""); //$NON-NLS-1$

		//for now if there are multiple then the ant file is not stored for specific projects	
		if (fAntText.getEnabled()) {
			fStore.setGeneralAntpath(fAntText.getText());
		}
		fStore.setOpenInBrowser(fCheckbrowser.getSelection());
		fStore.setJDK14Mode(fJDK14Button.getSelection());

	}

	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			doValidation(OVERVIEWSTATUS);
			doValidation(ANTSTATUS);
		}
	}

	public void init() {
		updateStatus(new StatusInfo());
	}

	private IStatus findMostSevereStatus() {
		return StatusUtil.getMostSevere(new IStatus[] { fAntStatus, fOverviewStatus });
	}

	public boolean generateAnt() {
		return fAntButton.getSelection();
	}

}
