/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.javadocexport;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogSettings;

import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.util.SWTUtil;

/**
 * @version 	1.0
 * @author
 */
public class JavadocSpecificsWizardPage extends JavadocWizardPage {

	private FlaggedButton fDeprecatedList;
	private FlaggedButton fAuthorCheck;
	private FlaggedButton fVersionCheck;
	private FlaggedButton fDeprecatedCheck;
	private FlaggedButton fHierarchyCheck;
	private FlaggedButton fNavigatorCheck;
	private FlaggedButton fIndexCheck;
	private FlaggedButton fSeperatedIndexCheck;
	private Button fStyleSheetBrowseButton;
	private Button fStyleSheetButton;
	private Button checkbrowser;
	private Text fStyleSheetText;
	private Text fExtraOptionsText;
	private Text fOverViewText;
	private Button fOverViewButton;
	private Button fOverViewBrowseButton;
	private Group fBasicOptionsGroup;
	private Group fTagsGroup;
	private Composite fUpperComposite;
	private Composite fLowerComposite;

	private final String NOTREE= " -notree";
	private final String NOINDEX= " -noindex";
	private final String NONAVBAR= " -nonavbar";
	private final String NODEPRECATED= " -nodeprecated";
	private final String NODEPRECATEDLIST= " -nodeprecatedlist";
	private final String VERSION= " -version";
	private final String AUTHOR= " -author";
	private final String SPLITINDEX= " -splitindex";
	private final String STYLESHEET= " -stylesheetfile ";
	private final String OVERVIEW= " -overview ";

	private StatusInfo fStyleSheetStatus;
	private StatusInfo fOverviewStatus;

	private ArrayList fButtonsList;

	private ControlEnableState fControlEnableState;

	private IDialogSettings fDialogSettings;
	private String fDialogSectionName;
	private JavadocTreeWizardPage fPredecessor;

	/**
	 * Constructor for JavadocWizardPage.
	 * @param pageName
	 */
	protected JavadocSpecificsWizardPage(String pageName, IDialogSettings settings, JavadocTreeWizardPage pred) {
		super(pageName);
		setDescription("Configure Javadoc arguments.");

		fDialogSettings= settings;
		fDialogSectionName= pageName;

		fButtonsList= new ArrayList();

		fPredecessor= pred;

		fStyleSheetStatus= new StatusInfo();
		fOverviewStatus= new StatusInfo();

	}
	/*
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);

		Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayoutData(createGridData(GridData.FILL_BOTH, 0, 0));
		composite.setLayout(createGridLayout(1));

		fUpperComposite= new Composite(composite, SWT.NONE);
		fUpperComposite.setLayoutData(createGridData(GridData.FILL_HORIZONTAL, 1, 0));
		fUpperComposite.setLayout(createGridLayout(4));

		createBasicOptionsGroup(fUpperComposite);
		createTagOptionsGroup(fUpperComposite);
		createStyleSheetGroup(fUpperComposite);

		Label separator= new Label(composite, SWT.HORIZONTAL | SWT.SEPARATOR);
		separator.setLayoutData(createGridData(GridData.FILL_HORIZONTAL, 1, 0));

		fLowerComposite= new Composite(composite, SWT.NONE);
		fLowerComposite.setLayoutData(createGridData(GridData.FILL_BOTH, 1, 0));
		fLowerComposite.setLayout(createGridLayout(4));
		createExtraOptionsGroup(fLowerComposite);

		setControl(composite);

	} //end method createControl

	private void createBasicOptionsGroup(Composite composite) {
		fBasicOptionsGroup= new Group(composite, SWT.SHADOW_ETCHED_IN);
		fBasicOptionsGroup.setLayout(createGridLayout(1));
		fBasicOptionsGroup.setLayoutData(createGridData(GridData.FILL_HORIZONTAL, 2, 0));
		fBasicOptionsGroup.setText("Basic Options");

		fHierarchyCheck= new FlaggedButton(fBasicOptionsGroup, "Ge&nerate Hierarchy tree", new GridData(GridData.FILL_HORIZONTAL), NOTREE, false);
		fNavigatorCheck= new FlaggedButton(fBasicOptionsGroup, "Genera&te Navigator bar", new GridData(GridData.FILL_HORIZONTAL), NONAVBAR, false);

		fIndexCheck= new FlaggedButton(fBasicOptionsGroup, "Generate inde&x", new GridData(GridData.FILL_HORIZONTAL), NOINDEX, false);

		fSeperatedIndexCheck= new FlaggedButton(fBasicOptionsGroup, "Sepa&rate index per letter", createGridData(GridData.GRAB_HORIZONTAL, 1, convertWidthInCharsToPixels(3)), SPLITINDEX, true);
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
		fTagsGroup.setText("Document these Tags");

		fAuthorCheck= new FlaggedButton(fTagsGroup, "@&author", new GridData(GridData.FILL_HORIZONTAL), AUTHOR, true);
		fVersionCheck= new FlaggedButton(fTagsGroup, "@v&ersion", new GridData(GridData.FILL_HORIZONTAL), VERSION, true);
		fDeprecatedCheck= new FlaggedButton(fTagsGroup, "@&deprecated", new GridData(GridData.FILL_HORIZONTAL), NODEPRECATED, false);
		fDeprecatedList= new FlaggedButton(fTagsGroup, "depre&cated list", createGridData(GridData.FILL_HORIZONTAL, 1, convertWidthInCharsToPixels(3)), NODEPRECATEDLIST, false);
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

		fStyleSheetButton= createButton(c, SWT.CHECK, "St&yle Sheet:", createGridData(1));
		fStyleSheetText= createText(c, SWT.SINGLE | SWT.BORDER, null, createGridData(GridData.FILL_HORIZONTAL, 1, 0));
		fStyleSheetText.setEnabled(false);
		fStyleSheetBrowseButton= createButton(c, SWT.PUSH, "Bro&wse...", createGridData(GridData.HORIZONTAL_ALIGN_END, 1, 0));
		SWTUtil.setButtonDimensionHint(fStyleSheetBrowseButton);
		fStyleSheetBrowseButton.setEnabled(false);

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
				handleFileBrowseButtonPressed(fStyleSheetText, new String[] { "*.css" }, "Select Stylesheet");
			}
		});
	}

	private void createExtraOptionsGroup(Composite composite) {
		Label jdocLocationLabel=
			createLabel(composite, SWT.NONE, "Extra &Javadoc options (path names with white spaces must be enclosed in quotes):", createGridData(GridData.HORIZONTAL_ALIGN_BEGINNING, 4, 0));
		fExtraOptionsText= createText(composite, SWT.MULTI | SWT.BORDER | SWT.WRAP, null, createGridData(GridData.FILL_BOTH, 4, 0));

		Composite c= new Composite(composite, SWT.NONE);
		c.setLayout(createGridLayout(3));
		c.setLayoutData(createGridData(GridData.FILL_HORIZONTAL, 4, 0));

		fOverViewButton= createButton(c, SWT.CHECK, "O&verview:", createGridData(1));
		fOverViewText= createText(c, SWT.SINGLE | SWT.BORDER, null, createGridData(GridData.FILL_HORIZONTAL, 1, 0));
		fOverViewBrowseButton= createButton(c, SWT.PUSH, "Br&owse...", createGridData(GridData.HORIZONTAL_ALIGN_END, 1, 0));
		SWTUtil.setButtonDimensionHint(fOverViewBrowseButton);
		fOverViewBrowseButton.setEnabled(false);
		fOverViewText.setEnabled(false);

		Button checkbrowser= createButton(composite, SWT.CHECK, "O&pen generated index file in browser", createGridData(4));
		checkbrowser.setEnabled(false);

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
				handleFileBrowseButtonPressed(fOverViewText, new String[] { "*.html" }, "Select Overview Page");
			}
		});
	} //end method createExtraOptionsGroup

	private void doValidation() {

		fStyleSheetStatus= new StatusInfo();
		fOverviewStatus= new StatusInfo();
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
		updateStatus(fOverviewStatus);
		updateStatus(fStyleSheetStatus);
	}

	/*
	 * @see JavadocWizardPage#onFinish()
	 */
	public void collectArguments(ArrayList cFlags) {

		//This is done because user may never go to the second page
		if (fPredecessor.fCustomButton.getSelection()) {
			fControlEnableState= ControlEnableState.disable(fUpperComposite);
		}

		for (int i= 0; i < fButtonsList.size(); i++) {
			FlaggedButton button= (FlaggedButton) fButtonsList.get(i);
			if (button.getButton().getEnabled()) {
				if (!(button.getButton().getSelection() ^ button.show()))
					cFlags.add(button.getFlag());
			}
		}
		if (fStyleSheetButton.getSelection() && fStyleSheetButton.getEnabled())
			cFlags.add(STYLESHEET + addQuotes(fStyleSheetText.getText()));

		if (!(fExtraOptionsText.getText().length() == 0)) {
			cFlags.add(" " + fExtraOptionsText.getText());
		}
		if (fOverViewButton.getSelection())
			cFlags.add(OVERVIEW + addQuotes(fOverViewText.getText()));
	}

	private String addQuotes(String str) {
		StringBuffer buf= new StringBuffer("\"");
		buf.append(str);
		buf.append("\"");
		return buf.toString();
	}

	public void finish() {
		preserveDialogSettings();
	}

	protected void preserveDialogSettings() {
		IDialogSettings section= fDialogSettings.getSection(fDialogSectionName);
		if (section == null) {
			section= new DialogSettings(fDialogSectionName);
		}
		Object[] buttons= fButtonsList.toArray();
		for (int i= 0; i < buttons.length; i++) {
			FlaggedButton button= (FlaggedButton) buttons[i];
			if (button.getButton().getSelection())
				section.put(button.getFlag(), true);
			else
				section.put(button.getFlag(), false);
		}
		//will overwrite current section
		fDialogSettings.addSection(section);
	}

	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			if (fPredecessor.fCustomButton.getSelection()) {
				fControlEnableState= ControlEnableState.disable(fUpperComposite);
			} else {
				if (fControlEnableState != null) {
					fControlEnableState.restore();

				}
			}
		}
	}

	public void init() {
		updateStatus(new StatusInfo());
	}

	private IStatus findMostSevereStatus() {
		return StatusUtil.getMostSevere(new IStatus[] { fOverviewStatus, fStyleSheetStatus });
	}

	private class FlaggedButton {

		private Button fButton;
		private String fFlag;
		private boolean fShowFlag;

		public FlaggedButton(Composite composite, String message, GridData gridData, String flag, boolean show) {
			fFlag= flag;
			fShowFlag= show;
			fButton= createButton(composite, SWT.CHECK, message, gridData);
			fButtonsList.add(this);
			setButtonSettings(this);
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

		private void setButtonSettings(FlaggedButton button) {
			IDialogSettings section= fDialogSettings.getSection(fDialogSectionName);
			if (section == null) {
				fButton.setSelection(!fShowFlag);
			} else {
				fButton.setSelection(section.getBoolean(fFlag));
			}
		}

	} //end class FlaggesButton

} //JavadocSpecificsWizardPage