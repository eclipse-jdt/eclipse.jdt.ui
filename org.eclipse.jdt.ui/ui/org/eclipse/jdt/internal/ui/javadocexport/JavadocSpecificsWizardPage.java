/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.javadocexport;

import java.io.File;
import java.util.ArrayList;

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
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.dialogs.ControlEnableState;

import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.javadocexport.JavadocWizardPage.ToggleSelectionAdapter;
import org.eclipse.jdt.internal.ui.util.SWTUtil;

public class JavadocSpecificsWizardPage extends JavadocWizardPage {

	protected FlaggedButton fDeprecatedList;
	protected FlaggedButton fAuthorCheck;
	protected FlaggedButton fVersionCheck;
	protected FlaggedButton fDeprecatedCheck;
	protected FlaggedButton fHierarchyCheck;
	protected FlaggedButton fNavigatorCheck;
	protected FlaggedButton fIndexCheck;
	protected FlaggedButton fSeperatedIndexCheck;
	protected FlaggedButton fUse;
	protected Button fStyleSheetBrowseButton;
	protected Button fStyleSheetButton;
	protected Button fAntBrowseButton;
	private Button fCheckbrowser;
	private Button 	fTitleButton;	private Text fTitleText;	protected Text fStyleSheetText;
	protected Text fExtraOptionsText;
	protected Text fOverViewText;
	protected Text fAntText;
	protected Button fOverViewButton;
	private Button fOverViewBrowseButton;
	protected Button fAntButton;
	private Group fBasicOptionsGroup;
	private Group fTagsGroup;
	private Composite fUpperComposite;
	private Composite fLowerComposite;
	private Composite fComposite;
	private Label fSeparator;

	private StatusInfo fStyleSheetStatus;
	private StatusInfo fOverviewStatus;
	private StatusInfo fAntStatus;

	protected ArrayList fButtonsList;

	private ControlEnableState fControlEnableState;

	private JavadocOptionsManager fStore;
	private String fDialogSectionName;
	private JavadocTreeWizardPage fPredecessor;
	
	private final int STYLESHEETSTATUS= 0;
	private final int OVERVIEWSTATUS=1;
	private final int ANTSTATUS= 2;

	/**
	 * Constructor for JavadocWizardPage.
	 * @param pageName
	 */
	protected JavadocSpecificsWizardPage(String pageName, JavadocOptionsManager store, JavadocTreeWizardPage pred) {
		super(pageName);
		setDescription(JavadocExportMessages.getString("JavadocSpecificsWizardPage.description")); //$NON-NLS-1$

		fStore= store;
		fButtonsList= new ArrayList();
		fPredecessor= pred;

		fStyleSheetStatus= new StatusInfo();
		fOverviewStatus= new StatusInfo();
		fAntStatus= new StatusInfo();
	}

	/*
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);

		fComposite= new Composite(parent, SWT.NONE);
		fComposite.setLayoutData(createGridData(GridData.FILL_BOTH, 0, 0));
		fComposite.setLayout(createGridLayout(1));
	

		fUpperComposite= new Composite(fComposite, SWT.NONE);
		fUpperComposite.setLayoutData(createGridData(GridData.FILL_VERTICAL | GridData.FILL_HORIZONTAL, 1, 0));
		
		GridLayout layout= createGridLayout(4);
		layout.marginHeight= 0;
		fUpperComposite.setLayout(layout);

		fLowerComposite= new Composite(fComposite, SWT.NONE);
		fLowerComposite.setLayoutData(createGridData(GridData.FILL_BOTH, 1, 0));
		
		layout= createGridLayout(3);
		layout.marginHeight= 0;
		fLowerComposite.setLayout(layout);

		createBasicOptionsGroup(fUpperComposite);
		createTagOptionsGroup(fUpperComposite);
		createStyleSheetGroup(fUpperComposite);
		

		//fSeparator= new Label(fUpperComposite, SWT.HORIZONTAL | SWT.SEPARATOR);
		//fSeparator.setLayoutData(createGridData(GridData.FILL_HORIZONTAL, 4, 0));
		//fSeparator.setVisible(false);

		createExtraOptionsGroup(fLowerComposite);
		createAntGroup(fLowerComposite);

		setControl(fComposite);

	} //end method createControl

	private void createBasicOptionsGroup(Composite composite) {
				fTitleButton= createButton(composite, SWT.CHECK, JavadocExportMessages.getString("JavadocSpecificsWizardPage.titlebutton.label"), createGridData(1)); //$NON-NLS-1$		fTitleText= createText(composite, SWT.SINGLE | SWT.BORDER, null, createGridData(GridData.FILL_HORIZONTAL, 3, 0));		String text= fStore.getTitle();		if(!text.equals("")) { //$NON-NLS-1$			fTitleText.setText(text);			fTitleButton.setSelection(true);		} else fTitleText.setEnabled(false);				fBasicOptionsGroup= new Group(composite, SWT.SHADOW_ETCHED_IN);
		fBasicOptionsGroup.setLayout(createGridLayout(1));
		fBasicOptionsGroup.setLayoutData(createGridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL, 2, 0));
		fBasicOptionsGroup.setText(JavadocExportMessages.getString("JavadocSpecificsWizardPage.basicgroup.label")); //$NON-NLS-1$

		fUse= new FlaggedButton(fBasicOptionsGroup, JavadocExportMessages.getString("JavadocSpecificsWizardPage.usebutton.label"), new GridData(GridData.FILL_HORIZONTAL), fStore.USE, true); //$NON-NLS-1$
		fHierarchyCheck= new FlaggedButton(fBasicOptionsGroup, JavadocExportMessages.getString("JavadocSpecificsWizardPage.hierachytreebutton.label"), new GridData(GridData.FILL_HORIZONTAL), fStore.NOTREE, false); //$NON-NLS-1$
		fNavigatorCheck= new FlaggedButton(fBasicOptionsGroup, JavadocExportMessages.getString("JavadocSpecificsWizardPage.navigatorbarbutton.label"), new GridData(GridData.FILL_HORIZONTAL), fStore.NONAVBAR, false); //$NON-NLS-1$

		fIndexCheck= new FlaggedButton(fBasicOptionsGroup, JavadocExportMessages.getString("JavadocSpecificsWizardPage.indexbutton.label"), new GridData(GridData.FILL_HORIZONTAL), fStore.NOINDEX, false); //$NON-NLS-1$

		fSeperatedIndexCheck= new FlaggedButton(fBasicOptionsGroup, JavadocExportMessages.getString("JavadocSpecificsWizardPage.separateindex.button.label"), createGridData(GridData.GRAB_HORIZONTAL, 1, convertWidthInCharsToPixels(3)), fStore.SPLITINDEX, true); //$NON-NLS-1$
		fSeperatedIndexCheck.getButton().setEnabled(fIndexCheck.getButton().getSelection());

		fIndexCheck.getButton().addSelectionListener(new ToggleSelectionAdapter(new Control[] { fSeperatedIndexCheck.getButton()}) {
			public void validate() {
			}
		});
				fTitleButton.addSelectionListener(new ToggleSelectionAdapter(new Control[] { fTitleText}) {			public void validate() {			}		});
	}

	private void createTagOptionsGroup(Composite composite) {
		fTagsGroup= new Group(composite, SWT.SHADOW_ETCHED_IN);
		fTagsGroup.setLayout(createGridLayout(1));
		fTagsGroup.setLayoutData(createGridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL, 2, 0));
		fTagsGroup.setText(JavadocExportMessages.getString("JavadocSpecificsWizardPage.tagsgroup.label")); //$NON-NLS-1$

		fAuthorCheck= new FlaggedButton(fTagsGroup, JavadocExportMessages.getString("JavadocSpecificsWizardPage.authorbutton.label"), new GridData(GridData.FILL_HORIZONTAL), fStore.AUTHOR, true); //$NON-NLS-1$
		fVersionCheck= new FlaggedButton(fTagsGroup, JavadocExportMessages.getString("JavadocSpecificsWizardPage.versionbutton.label"), new GridData(GridData.FILL_HORIZONTAL), fStore.VERSION, true); //$NON-NLS-1$
		fDeprecatedCheck= new FlaggedButton(fTagsGroup, JavadocExportMessages.getString("JavadocSpecificsWizardPage.deprecatedbutton.label"), new GridData(GridData.FILL_HORIZONTAL), fStore.NODEPRECATED, false); //$NON-NLS-1$
		fDeprecatedList= new FlaggedButton(fTagsGroup, JavadocExportMessages.getString("JavadocSpecificsWizardPage.deprecatedlistbutton.label"), createGridData(GridData.FILL_HORIZONTAL, 1, convertWidthInCharsToPixels(3)), fStore.NODEPRECATEDLIST, false); //$NON-NLS-1$
		fDeprecatedList.getButton().setEnabled(fDeprecatedCheck.getButton().getSelection());

		fDeprecatedCheck.getButton().addSelectionListener(new ToggleSelectionAdapter(new Control[] { fDeprecatedList.getButton()}) {
			public void validate() {
			}
		});
	} //end createTagOptionsGroup

	private void createStyleSheetGroup(Composite composite) {
		Composite c= new Composite(composite, SWT.NONE);
		c.setLayout(createGridLayout(3));
		c.setLayoutData(createGridData(GridData.FILL_HORIZONTAL, 4, 0));
		((GridLayout) c.getLayout()).marginWidth= 0;

		fStyleSheetButton= createButton(c, SWT.CHECK, JavadocExportMessages.getString("JavadocSpecificsWizardPage.stylesheetbutton.label"), createGridData(1)); //$NON-NLS-1$
		fStyleSheetText= createText(c, SWT.SINGLE | SWT.BORDER, null, createGridData(GridData.FILL_HORIZONTAL, 1, 0));
		fStyleSheetBrowseButton= createButton(c, SWT.PUSH, JavadocExportMessages.getString("JavadocSpecificsWizardPage.stylesheetbrowse.label"), createGridData(GridData.HORIZONTAL_ALIGN_END, 1, 0)); //$NON-NLS-1$
		SWTUtil.setButtonDimensionHint(fStyleSheetBrowseButton);

		String str= fStore.getStyleSheet();
		if (str.equals("")) { //$NON-NLS-1$
			//default
			fStyleSheetText.setEnabled(false);
			fStyleSheetBrowseButton.setEnabled(false);
		} else {
			fStyleSheetButton.setSelection(true);
			fStyleSheetText.setText(str);
		}

		//Listeners
		fStyleSheetButton.addSelectionListener(new ToggleSelectionAdapter(new Control[] { fStyleSheetText, fStyleSheetBrowseButton }) {
			public void validate() {
				doValidation(STYLESHEETSTATUS);
			}
		});

		fStyleSheetText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				doValidation(STYLESHEETSTATUS);
			}
		});

		fStyleSheetBrowseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				handleFileBrowseButtonPressed(fStyleSheetText, new String[] { "*.css" }, JavadocExportMessages.getString("JavadocSpecificsWizardPage.stylesheetbrowsedialog.title")); //$NON-NLS-1$ //$NON-NLS-2$
			}
		});

	}

	private void createExtraOptionsGroup(Composite composite) {
		Composite c= new Composite(composite, SWT.NONE);
		c.setLayout(createGridLayout(3));
		c.setLayoutData(createGridData(GridData.FILL_HORIZONTAL, 3, 0));
		((GridLayout) c.getLayout()).marginWidth= 0;

		fOverViewButton= createButton(c, SWT.CHECK, JavadocExportMessages.getString("JavadocSpecificsWizardPage.overviewbutton.label"), createGridData(1)); //$NON-NLS-1$
		fOverViewText= createText(c, SWT.SINGLE | SWT.BORDER, null, createGridData(GridData.FILL_HORIZONTAL, 1, 0));
		fOverViewBrowseButton= createButton(c, SWT.PUSH, JavadocExportMessages.getString("JavadocSpecificsWizardPage.overviewbrowse.label"), createGridData(GridData.HORIZONTAL_ALIGN_END, 1, 0)); //$NON-NLS-1$
		SWTUtil.setButtonDimensionHint(fOverViewBrowseButton);

		String str= fStore.getOverview();
		if (str.equals("")) { //$NON-NLS-1$
			//default
			fOverViewText.setEnabled(false);
			fOverViewBrowseButton.setEnabled(false);
		} else {
			fOverViewButton.setSelection(true);
			fOverViewText.setText(str);
		}

		Label jdocLocationLabel= createLabel(composite, SWT.NONE, JavadocExportMessages.getString("JavadocSpecificsWizardPage.extraoptionsfield.label"), createGridData(GridData.HORIZONTAL_ALIGN_BEGINNING, 3, 0)); //$NON-NLS-1$
		fExtraOptionsText= createText(composite, SWT.MULTI | SWT.BORDER | SWT.WRAP, null, createGridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.FILL_VERTICAL, 3, 0));
		//fExtraOptionsText.setSize(convertWidthInCharsToPixels(60), convertHeightInCharsToPixels(10));

		str= fStore.getAdditionalParams();
		fExtraOptionsText.setText(str);
		
		
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
		Label AntLabel= createLabel(c, SWT.NONE, JavadocExportMessages.getString("JavadocSpecificsWizardPage.antscripttext.label"), createGridData(GridData.HORIZONTAL_ALIGN_BEGINNING, 1, 0)); //$NON-NLS-1$
		fAntText= createText(c, SWT.SINGLE | SWT.BORDER, null, createGridData(GridData.FILL_HORIZONTAL, 1, 0));
		fAntText.setText(fStore.getAntpath());
		fAntText.setEnabled(false);
		fAntBrowseButton= createButton(c, SWT.PUSH, JavadocExportMessages.getString("JavadocSpecificsWizardPage.antscriptbrowse.label"), createGridData(GridData.HORIZONTAL_ALIGN_END, 1, 0)); //$NON-NLS-1$
		
		
		SWTUtil.setButtonDimensionHint(fAntBrowseButton);
		fAntBrowseButton.setEnabled(false);
		
		fCheckbrowser= createButton(c, SWT.CHECK, JavadocExportMessages.getString("JavadocSpecificsWizardPage.openbrowserbutton.label"), createGridData(3));		 //$NON-NLS-1$
		
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
				path= path.removeLastSegments(1);

				temp= handleFolderBrowseButtonPressed(path.toOSString(),fAntText.getShell(), JavadocExportMessages.getString("JavadocSpecificsWizardPage.antscriptbrowsedialog.title"), JavadocExportMessages.getString("JavadocSpecificsWizardPage.antscriptbrowsedialog.label")); //$NON-NLS-1$ //$NON-NLS-2$

				path= new Path(temp);
				path= path.addTrailingSeparator().append(file);
				fAntText.setText(path.toOSString());

			}
		});
	} //end method createExtraOptionsGroup

	private void doValidation(int VALIDATE) {
		File file= null;
		String ext= null;
		Path path= null;

		switch (VALIDATE) {
			case STYLESHEETSTATUS :
				fStyleSheetStatus = new StatusInfo();
				if (fStyleSheetButton.getSelection()) {
					path = new Path(fStyleSheetText.getText());
					file = new File(fStyleSheetText.getText());
					ext = path.getFileExtension();
					if ((file == null) || !file.exists()) {
						fStyleSheetStatus.setError(JavadocExportMessages.getString("JavadocSpecificsWizardPage.stylesheetnotfound.error")); //$NON-NLS-1$
					} else if ((ext == null) || !ext.equalsIgnoreCase("css")) { //$NON-NLS-1$
						fStyleSheetStatus.setError(JavadocExportMessages.getString("JavadocSpecificsWizardPage.stylesheetincorrect.error")); //$NON-NLS-1$
					}
				}
				break;

			case OVERVIEWSTATUS :
				fOverviewStatus = new StatusInfo();
				if (fOverViewButton.getSelection()) {
					path = new Path(fOverViewText.getText());
					file = path.toFile();
					ext = path.getFileExtension();
					if ((file == null) || !file.exists()) {
						fOverviewStatus.setError(JavadocExportMessages.getString("JavadocSpecificsWizardPage.overviewnotfound.error")); //$NON-NLS-1$
					} else if ((ext == null) || !ext.equalsIgnoreCase("html")) { //$NON-NLS-1$
						fOverviewStatus.setError(JavadocExportMessages.getString("JavadocSpecificsWizardPage.overviewincorrect.error")); //$NON-NLS-1$
					}
				}
				break;
			case ANTSTATUS :
				fAntStatus = new StatusInfo();
				if (fAntButton.getSelection()) {
					path = new Path(fAntText.getText());
					ext = path.getFileExtension();
					IPath antSeg = path.removeLastSegments(1);

					if ((!antSeg.isValidPath(antSeg.toOSString()))
						|| (ext == null)
						|| !(ext.equalsIgnoreCase("xml"))) //$NON-NLS-1$
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

	protected void finish() {
				if(fTitleButton.getSelection())			fStore.setTitle(fTitleText.getText());		else fStore.setTitle(""); //$NON-NLS-1$

	//don't store the buttons if they are not enabled
	//this will change when there is a single page aimed at the standard doclet
		if (!fPredecessor.getCustom()) {
			Object[] buttons = fButtonsList.toArray();
			for (int i = 0; i < buttons.length; i++) {
				FlaggedButton button = (FlaggedButton) buttons[i];
				if (button.getButton().getEnabled())
					fStore.setBoolean(
						button.getFlag(),
						!(button.getButton().getSelection() ^ button.show()));
				else
					fStore.setBoolean(button.getFlag(), false == button.show());
			}
		}
		String str= fExtraOptionsText.getText();
		if (str.length() > 0) 			fStore.setAdditionalParams(str);
		else fStore.setAdditionalParams(""); //$NON-NLS-1$				if (fOverViewText.getEnabled())
			fStore.setOverview(fOverViewText.getText());
		else fStore.setOverview(""); //$NON-NLS-1$				if (fStyleSheetText.getEnabled())
			fStore.setStyleSheet(fStyleSheetText.getText());
		else fStore.setStyleSheet(""); //$NON-NLS-1$				if (fAntText.getEnabled())
			fStore.setAntpath(fAntText.getText());
		else fStore.setAntpath(""); //$NON-NLS-1$	}

	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {

			if (fPredecessor.getCustom()) {
				fControlEnableState= ControlEnableState.disable(fUpperComposite);
				fCheckbrowser.setEnabled(false);
				//fSeparator.setVisible(true);

			}
		} else {
			if (fPredecessor.getCustom())
				fControlEnableState.restore();
				fCheckbrowser.setEnabled(true);
			//fSeparator.setVisible(false);
		}
	}

	public void init() {
		updateStatus(new StatusInfo());
	}

	private IStatus findMostSevereStatus() {
		return StatusUtil.getMostSevere(new IStatus[] { fAntStatus, fOverviewStatus, fStyleSheetStatus });
	}

	public boolean generateAnt() {
		return fAntButton.getSelection();
	}
	
	public boolean openInBrowser() {
		return fCheckbrowser.getSelection();
	}

	protected class FlaggedButton {

		private Button fButton;
		private String fFlag;
		private boolean fShowFlag;

		public FlaggedButton(Composite composite, String message, GridData gridData, String flag, boolean show) {
			fFlag= flag;
			fShowFlag= show;
			fButton= createButton(composite, SWT.CHECK, message, gridData);
			fButtonsList.add(this);
			setButtonSettings();
		}

		public Button getButton() {
			return fButton;
		}

		public String getFlag() {
			return fFlag;
		}
		public boolean show() {
			return fShowFlag;
		}

		private void setButtonSettings() {

			fButton.setSelection(!(fStore.getBoolean(fFlag) ^ fShowFlag));
		}

	} //end class FlaggesButton

} //JavadocSpecificsWizardPage