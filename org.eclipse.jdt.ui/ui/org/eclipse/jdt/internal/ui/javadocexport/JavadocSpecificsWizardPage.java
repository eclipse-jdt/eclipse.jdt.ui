/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.javadocexport;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.javadocexport.JavadocWizardPage.ToggleSelectionAdapter;
import org.eclipse.jdt.internal.ui.util.SWTUtil;
import org.eclipse.jdt.launching.ExecutionArguments;
import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * @version 	1.0
 * @author
 */
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
	private Button checkbrowser;
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

	private Point Pu;
	private Point Pl;
	
	private StatusInfo fStyleSheetStatus;
	private StatusInfo fOverviewStatus;
	private StatusInfo fAntStatus;

	protected ArrayList fButtonsList;

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
		fAntStatus= new StatusInfo();

	}
	
	protected JavadocSpecificsWizardPage(String pageName, JavadocTreeWizardPage pred) {
		this(pageName, null, pred);
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
		fUpperComposite.setLayout(createGridLayout(4));
		
		fLowerComposite= new Composite(fComposite, SWT.NONE);
		fLowerComposite.setLayoutData(createGridData(GridData.FILL_BOTH, 1, 0));
		fLowerComposite.setLayout(createGridLayout(4));
		
		Pu= fUpperComposite.getLocation();
		Pl= fLowerComposite.getLocation();
		
		createBasicOptionsGroup(fUpperComposite);
		createTagOptionsGroup(fUpperComposite);
		createStyleSheetGroup(fUpperComposite);
		//fUpperComposite.setVisible();

		Label separator= new Label(fUpperComposite, SWT.HORIZONTAL | SWT.SEPARATOR);
		separator.setLayoutData(createGridData(GridData.FILL_HORIZONTAL, 4, 0));

		
		createExtraOptionsGroup(fLowerComposite);

		setControl(fComposite);

	} //end method createControl

	private void createBasicOptionsGroup(Composite composite) {
		fBasicOptionsGroup= new Group(composite, SWT.SHADOW_ETCHED_IN);
		fBasicOptionsGroup.setLayout(createGridLayout(1));
		fBasicOptionsGroup.setLayoutData(createGridData(GridData.FILL_HORIZONTAL, 2, 0));
		fBasicOptionsGroup.setText("Basic Options");

		fHierarchyCheck= new FlaggedButton(fBasicOptionsGroup, "Ge&nerate hierarchy tree", new GridData(GridData.FILL_HORIZONTAL), JavadocWizard.NOTREE, false);
		fNavigatorCheck= new FlaggedButton(fBasicOptionsGroup, "Genera&te navigator bar", new GridData(GridData.FILL_HORIZONTAL), JavadocWizard.NONAVBAR, false);

		fIndexCheck= new FlaggedButton(fBasicOptionsGroup, "Generate inde&x", new GridData(GridData.FILL_HORIZONTAL), JavadocWizard.NOINDEX, false);

		fSeperatedIndexCheck= new FlaggedButton(fBasicOptionsGroup, "Sepa&rate index per letter", createGridData(GridData.GRAB_HORIZONTAL, 1, convertWidthInCharsToPixels(3)), JavadocWizard.SPLITINDEX, true);
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

		fAuthorCheck= new FlaggedButton(fTagsGroup, "@&author", new GridData(GridData.FILL_HORIZONTAL), JavadocWizard.AUTHOR, true);
		fVersionCheck= new FlaggedButton(fTagsGroup, "@v&ersion", new GridData(GridData.FILL_HORIZONTAL), JavadocWizard.VERSION, true);
		fDeprecatedCheck= new FlaggedButton(fTagsGroup, "@&deprecated", new GridData(GridData.FILL_HORIZONTAL), JavadocWizard.NODEPRECATED, false);
		fDeprecatedList= new FlaggedButton(fTagsGroup, "depre&cated list", createGridData(GridData.FILL_HORIZONTAL, 1, convertWidthInCharsToPixels(3)), JavadocWizard.NODEPRECATEDLIST, false);
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
		((GridLayout)c.getLayout()).marginWidth= 0;

		fStyleSheetButton= createButton(c, SWT.CHECK, "St&yle sheet:", createGridData(1));
		fStyleSheetText= createText(c, SWT.SINGLE | SWT.BORDER, null, createGridData(GridData.FILL_HORIZONTAL, 1, 0));
		fStyleSheetBrowseButton =createButton(c,SWT.PUSH,"Bro&wse...",
					createGridData(GridData.HORIZONTAL_ALIGN_END, 1, 0));
		SWTUtil.setButtonDimensionHint(fStyleSheetBrowseButton);
		
		if (fDialogSettings != null) {
			String str = fDialogSettings.get(JavadocWizard.STYLESHEET);
			if (str == null) {
				fStyleSheetText.setEnabled(false);
				fStyleSheetBrowseButton.setEnabled(false);
			} else {
				fStyleSheetButton.setSelection(true);
				fStyleSheetText.setText(str);
			}
		} else {
			fStyleSheetText.setEnabled(false);
			fStyleSheetBrowseButton.setEnabled(false);
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
		Label jdocLocationLabel=
			createLabel(composite, SWT.NONE, "Extra &Javadoc options (path names with white spaces must be enclosed in quotes):", createGridData(GridData.HORIZONTAL_ALIGN_BEGINNING, 4, 0));
		fExtraOptionsText= createText(composite, SWT.MULTI | SWT.BORDER | SWT.WRAP, null, createGridData(GridData.FILL_BOTH, 4, 0));
		fExtraOptionsText.setSize(convertWidthInCharsToPixels(60),
				convertHeightInCharsToPixels(10));
	
		Composite c= new Composite(composite, SWT.NONE);
		c.setLayout(createGridLayout(3));
		c.setLayoutData(createGridData(GridData.FILL_HORIZONTAL, 4, 0));
		((GridLayout)c.getLayout()).marginWidth= 0;

		fOverViewButton= createButton(c, SWT.CHECK, "O&verview:", createGridData(1));
		fOverViewText= createText(c, SWT.SINGLE | SWT.BORDER, null, createGridData(GridData.FILL_HORIZONTAL, 1, 0));
		fOverViewBrowseButton= createButton(c, SWT.PUSH, "Br&owse...", createGridData(GridData.HORIZONTAL_ALIGN_END, 1, 0));
		SWTUtil.setButtonDimensionHint(fOverViewBrowseButton);
		
		if (fDialogSettings != null) {

			String str= fDialogSettings.get(JavadocWizard.EXTRAOPTIONS);
			if(str != null)
				fExtraOptionsText.setText(str);

			str = fDialogSettings.get(JavadocWizard.OVERVIEW);
			if (str == null) {
				fOverViewText.setEnabled(false);
				fOverViewBrowseButton.setEnabled(false);
			} else {
				fOverViewButton.setSelection(true);
				fOverViewText.setText(str);
			}
		} else {
			fOverViewText.setEnabled(false);
			fOverViewBrowseButton.setEnabled(false);
		}

//		Button checkbrowser= createButton(composite, SWT.CHECK, "O&pen generated index file in browser", createGridData(4));
//		checkbrowser.setEnabled(false);
		
		fAntButton= createButton(c, SWT.CHECK, "Generate &Ant script:", createGridData(1));
		fAntText= createText(c, SWT.SINGLE | SWT.BORDER, null, createGridData(GridData.FILL_HORIZONTAL, 1, 0));
		fAntText.setText("javadoc.xml");
		fAntText.setEnabled(false);
	
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
		
		fAntButton.addSelectionListener(new ToggleSelectionAdapter(new Control[] {fAntText}) {
			public void validate() {
				doValidation();
			}
		});
		
		fAntText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				doValidation();
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
		if(fAntButton.getSelection()) {
			
			Path path= new Path(fAntText.getText());
			
			//@Improve
			if(false) {
				fAntStatus.setError("Not a valid file name.");	
			}
		}
			
		updateStatus(findMostSevereStatus());

	}

	/*
	 * @see JavadocWizardPage#onFinish()
	 */
	public void collectArguments(ArrayList cFlags, Map map) {

		//This is done because user may never go to the second page
		if (fPredecessor.fCustomButton.getSelection()) {
			fControlEnableState = ControlEnableState.disable(fUpperComposite);
		}

		if (fUpperComposite.getVisible()) {
			for (int i = 0; i < fButtonsList.size(); i++) {
				FlaggedButton button = (FlaggedButton) fButtonsList.get(i);
				if (button.getButton().getEnabled()) {
					if (!(button.getButton().getSelection() ^ button.show())) {
						cFlags.add(makeflag(button.getFlag()));
						map.put(button.getFlag(), "true");
					} else {
						map.put(button.getFlag(), "false");
					}
				} else {
					map.put(button.getFlag(), "false");
				}

			}
			if (fStyleSheetButton.getSelection() && fUpperComposite.getVisible()) {
				cFlags.add(makeflag(JavadocWizard.STYLESHEET));
				cFlags.add(fStyleSheetText.getText());
				map.put(JavadocWizard.STYLESHEET, fStyleSheetText.getText());
			}
		}
		String userArgs = fExtraOptionsText.getText();
		if (userArgs.length() > 0) {
			ExecutionArguments args = new ExecutionArguments("", userArgs);
			String[] argsArray = args.getProgramArgumentsArray();
			for (int i = 0; i < argsArray.length; i++) {
				cFlags.add(argsArray[i]);
			}
			map.put(JavadocWizard.EXTRAOPTIONS, userArgs);
		}
		if (fOverViewButton.getSelection()) {
			cFlags.add(makeflag(JavadocWizard.OVERVIEW));
			cFlags.add(fOverViewText.getText());
			map.put(JavadocWizard.OVERVIEW, fOverViewText.getText());
		}
		
		if(fAntButton.getSelection()) {
			map.put(JavadocWizard.ANT, fAntText.getText());	
		}
	}
	
	private String makeflag(String flag) {
		return "-"+flag;
	}

	//The dialogSettings are not perserved if the wizard has been
	//launched from an Ant Script
	public void finish() {
	}

	protected IDialogSettings preserveDialogSettings() {
		if (fDialogSettings == null) {
			fDialogSettings= new DialogSettings(fDialogSectionName);
		}
		Object[] buttons= fButtonsList.toArray();
		for (int i= 0; i < buttons.length; i++) {
			FlaggedButton button= (FlaggedButton) buttons[i];
			fDialogSettings.put(button.getFlag(), !(button.getButton().getSelection() ^ button.show()));
		}
		
		String str= fExtraOptionsText.getText();
		if(str.length() > 0)
			fDialogSettings.put(JavadocWizard.EXTRAOPTIONS, str);
		if(fOverViewText.getEnabled())
			fDialogSettings.put(JavadocWizard.OVERVIEW, fOverViewText.getText());
		if(fStyleSheetText.getEnabled())
			fDialogSettings.put(JavadocWizard.STYLESHEET, fStyleSheetText.getText());	
		
		return fDialogSettings;
	}

	public void setVisible(boolean visible) {
		super.setVisible(visible);
		
		
				
		if (visible) {
			
			Point pu= fUpperComposite.getLocation();
			Point pl= fLowerComposite.getLocation();
			
			if (fPredecessor.fCustomButton.getSelection()) {
	//@hack
	//--		
				if(pu.y < pl.y) {	
					fUpperComposite.setLocation(pl);
					fLowerComposite.setLocation(pu);
				
					fUpperComposite.setVisible(false);
					fComposite.redraw();
				}	
		
			} else {
				
				if(pu.y > pl.y) {
					fUpperComposite.setLocation(pl);
					fLowerComposite.setLocation(pu);
	
					fUpperComposite.setVisible(true);
					fComposite.redraw();
				}
			}
		//--
		}
	}

	public void init() {
		updateStatus(new StatusInfo());
	}

	private IStatus findMostSevereStatus() {
		return StatusUtil.getMostSevere(new IStatus[] {fAntStatus, fOverviewStatus, fStyleSheetStatus });
	}

	public boolean generateAnt() {
		return fAntButton.getSelection();
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
			if (fDialogSettings == null) {
				//default all options checked
				fButton.setSelection(true);
			} else {
				fButton.setSelection(!(fDialogSettings.getBoolean(fFlag)^fShowFlag));
			}
		}

	} //end class FlaggesButton

} //JavadocSpecificsWizardPage