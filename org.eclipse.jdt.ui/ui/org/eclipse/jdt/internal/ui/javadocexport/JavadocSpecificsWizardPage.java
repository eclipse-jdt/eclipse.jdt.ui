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
	protected Button fStyleSheetBrowseButton;
	protected Button fStyleSheetButton;
	protected Button fAntBrowseButton;
	private Button fCheckbrowser;
	protected Text fStyleSheetText;
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

	/**
	 * Constructor for JavadocWizardPage.
	 * @param pageName
	 */
	protected JavadocSpecificsWizardPage(String pageName, JavadocOptionsManager store, JavadocTreeWizardPage pred) {
		super(pageName);
		setDescription("Configure Javadoc arguments.");

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
		fUpperComposite.setLayoutData(createGridData(GridData.FILL_HORIZONTAL, 1, 0));
		
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

		setControl(fComposite);

	} //end method createControl

	private void createBasicOptionsGroup(Composite composite) {
		fBasicOptionsGroup= new Group(composite, SWT.SHADOW_ETCHED_IN);
		fBasicOptionsGroup.setLayout(createGridLayout(1));
		fBasicOptionsGroup.setLayoutData(createGridData(GridData.FILL_HORIZONTAL, 2, 0));
		fBasicOptionsGroup.setText("Basic Options");

		fHierarchyCheck= new FlaggedButton(fBasicOptionsGroup, "Ge&nerate hierarchy tree", new GridData(GridData.FILL_HORIZONTAL), fStore.NOTREE, false);
		fNavigatorCheck= new FlaggedButton(fBasicOptionsGroup, "Genera&te navigator bar", new GridData(GridData.FILL_HORIZONTAL), fStore.NONAVBAR, false);

		fIndexCheck= new FlaggedButton(fBasicOptionsGroup, "Generate inde&x", new GridData(GridData.FILL_HORIZONTAL), fStore.NOINDEX, false);

		fSeperatedIndexCheck= new FlaggedButton(fBasicOptionsGroup, "Sepa&rate index per letter", createGridData(GridData.GRAB_HORIZONTAL, 1, convertWidthInCharsToPixels(3)), fStore.SPLITINDEX, true);
		fSeperatedIndexCheck.getButton().setEnabled(fIndexCheck.getButton().getSelection());

		fIndexCheck.getButton().addSelectionListener(new ToggleSelectionAdapter(new Control[] { fSeperatedIndexCheck.getButton()}) {
			public void validate() {
			}
		});

	}

	private void createTagOptionsGroup(Composite composite) {
		fTagsGroup= new Group(composite, SWT.SHADOW_ETCHED_IN);
		fTagsGroup.setLayout(createGridLayout(1));
		fTagsGroup.setLayoutData(createGridData(GridData.FILL_HORIZONTAL, 2, 0));
		fTagsGroup.setText("Document these tags");

		fAuthorCheck= new FlaggedButton(fTagsGroup, "@&author", new GridData(GridData.FILL_HORIZONTAL), fStore.AUTHOR, true);
		fVersionCheck= new FlaggedButton(fTagsGroup, "@v&ersion", new GridData(GridData.FILL_HORIZONTAL), fStore.VERSION, true);
		fDeprecatedCheck= new FlaggedButton(fTagsGroup, "@&deprecated", new GridData(GridData.FILL_HORIZONTAL), fStore.NODEPRECATED, false);
		fDeprecatedList= new FlaggedButton(fTagsGroup, "depre&cated list", createGridData(GridData.FILL_HORIZONTAL, 1, convertWidthInCharsToPixels(3)), fStore.NODEPRECATEDLIST, false);
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

		fStyleSheetButton= createButton(c, SWT.CHECK, "St&yle sheet:", createGridData(1));
		fStyleSheetText= createText(c, SWT.SINGLE | SWT.BORDER, null, createGridData(GridData.FILL_HORIZONTAL, 1, 0));
		fStyleSheetBrowseButton= createButton(c, SWT.PUSH, "Bro&wse...", createGridData(GridData.HORIZONTAL_ALIGN_END, 1, 0));
		SWTUtil.setButtonDimensionHint(fStyleSheetBrowseButton);

		String str= fStore.getStyleSheet();
		if (str.equals("")) {
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
				doValidation();
			}
		});

		fStyleSheetText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				doValidation();
			}
		});

		fStyleSheetBrowseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				handleFileBrowseButtonPressed(fStyleSheetText, new String[] { "*.css" }, "Select stylesheet");
			}
		});

	}

	private void createExtraOptionsGroup(Composite composite) {

		fOverViewButton= createButton(composite, SWT.CHECK, "O&verview:", createGridData(1));
		fOverViewText= createText(composite, SWT.SINGLE | SWT.BORDER, null, createGridData(GridData.FILL_HORIZONTAL, 1, 0));
		fOverViewBrowseButton= createButton(composite, SWT.PUSH, "Br&owse...", createGridData(GridData.HORIZONTAL_ALIGN_END, 1, 0));
		SWTUtil.setButtonDimensionHint(fOverViewBrowseButton);

		String str= fStore.getOverview();
		if (str.equals("")) {
			//default
			fOverViewText.setEnabled(false);
			fOverViewBrowseButton.setEnabled(false);
		} else {
			fOverViewButton.setSelection(true);
			fOverViewText.setText(str);
		}

		Label jdocLocationLabel= createLabel(composite, SWT.NONE, "Extra &Javadoc options (path names with white spaces must be enclosed in quotes):", createGridData(GridData.HORIZONTAL_ALIGN_BEGINNING, 3, 0));
		fExtraOptionsText= createText(composite, SWT.MULTI | SWT.BORDER | SWT.WRAP, null, createGridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.FILL_VERTICAL, 3, 0));
		//fExtraOptionsText.setSize(convertWidthInCharsToPixels(60), convertHeightInCharsToPixels(10));

		str= fStore.getAdditionalParams();
		fExtraOptionsText.setText(str);

		fAntButton= createButton(composite, SWT.CHECK, "&Ant script:", createGridData(1));
		fAntText= createText(composite, SWT.SINGLE | SWT.BORDER, null, createGridData(GridData.FILL_HORIZONTAL, 1, 0));
		fAntText.setText(fStore.getAntpath());
		fAntText.setEnabled(false);
		fAntBrowseButton= createButton(composite, SWT.PUSH, "Br&owse...", createGridData(GridData.HORIZONTAL_ALIGN_END, 1, 0));
		SWTUtil.setButtonDimensionHint(fAntBrowseButton);
		fAntBrowseButton.setEnabled(false);

		fCheckbrowser= createButton(composite, SWT.CHECK, "O&pen generated index file in browser", createGridData(3));		
		
		//Listeners
		fOverViewButton.addSelectionListener(new ToggleSelectionAdapter(new Control[] { fOverViewBrowseButton, fOverViewText }) {
			public void validate() {
				doValidation();
			}
		});

		fOverViewText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				doValidation();
			}
		});

		fOverViewBrowseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				handleFileBrowseButtonPressed(fOverViewText, new String[] { "*.html" }, "Select overview page");
			}
		});

		fAntButton.addSelectionListener(new ToggleSelectionAdapter(new Control[] { fAntText, fAntBrowseButton }) {
			public void validate() {
				doValidation();
			}
		});

		fAntText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				doValidation();
			}
		});

		fAntBrowseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {

				String temp= fAntText.getText();
				IPath path= new Path(temp);
				String file= path.lastSegment();
				path= path.removeLastSegments(1);

				temp= handleFolderBrowseButtonPressed(path.toOSString(),fAntText.getShell(), "Destination Selection", "&Select destination folder for ant script:");

				path= new Path(temp);
				path= path.addTrailingSeparator().append(file);
				fAntText.setText(path.toOSString());

			}
		});
	} //end method createExtraOptionsGroup

	private void doValidation() {

		fStyleSheetStatus= new StatusInfo();
		fOverviewStatus= new StatusInfo();
		fAntStatus= new StatusInfo();
		if (fStyleSheetButton.getSelection()) {
			Path stylePath= new Path(fStyleSheetText.getText());
			File file= new File(fStyleSheetText.getText());
			if (!stylePath.toFile().exists() || !stylePath.getFileExtension().equalsIgnoreCase("css")) {
				fStyleSheetStatus.setError("Not a valid stylesheet.");
			}
		}
		if (fOverViewButton.getSelection()) {
			Path overviewPath= new Path(fOverViewText.getText());
			if (!overviewPath.toFile().exists() || !overviewPath.getFileExtension().equalsIgnoreCase("html")) {
				fOverviewStatus.setError("Not a valid html document.");
			}
		}
		if (fAntButton.getSelection()) {
			IPath antPath= new Path(fAntText.getText());
			String ext= antPath.getFileExtension();
			IPath antSeg= antPath.removeLastSegments(1);
			
			if ((!antSeg.isValidPath(antSeg.toOSString())) || (ext == null) || !(ext.equalsIgnoreCase("xml")))
				fAntStatus.setError("Not a valid Ant file name.");
			else if (antPath.toFile().exists())
				fAntStatus.setWarning("The generated ant file will overwrite the existing ant file.");
		}

		updateStatus(findMostSevereStatus());

	}

	/*
	 * @see JavadocWizardPage#onFinish()
	 */

	protected void finish() {

		Object[] buttons= fButtonsList.toArray();
		for (int i= 0; i < buttons.length; i++) {
			FlaggedButton button= (FlaggedButton) buttons[i];
			if (button.getButton().getEnabled())
				fStore.setBoolean(button.getFlag(), !(button.getButton().getSelection() ^ button.show()));
			else
				fStore.setBoolean(button.getFlag(), false == button.show());
		}

		String str= fExtraOptionsText.getText();
		if (str.length() > 0)
			fStore.setAdditionalParams(str);
		if (fOverViewText.getEnabled())
			fStore.setOverview(fOverViewText.getText());
		if (fStyleSheetText.getEnabled())
			fStore.setStyleSheet(fStyleSheetText.getText());
		if (fAntText.getEnabled())
			fStore.setAntpath(fAntText.getText());
	}

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