package org.eclipse.jdt.internal.ui.javadocexport;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.javadoc.JavaDocLocations;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.jdt.internal.ui.dialogs.StatusDialog;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;

import org.eclipse.jdt.internal.ui.preferences.JavadocConfigurationBlock;
import org.eclipse.jdt.internal.ui.util.SWTUtil;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.CheckedListDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;

public class JavadocStandardWizardPage extends JavadocWizardPage {

	private JavadocOptionsManager fStore;
	private JavadocWizard fWizard;
	private Composite fUpperComposite;
	
	private Group fBasicOptionsGroup;
	private Group fTagsGroup;
	
	private Button 	fTitleButton;
	private Text fTitleText;
	protected Text fStyleSheetText;
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
	
	private CheckedListDialogField fListDialogField;
	private IJavaProject lastProject;
	private final int STYLESHEETSTATUS= 0;
	private StatusInfo fStyleSheetStatus;
	protected ArrayList fButtonsList;
	private Map fTempLinks;

	public JavadocStandardWizardPage(String pageName, JavadocOptionsManager store) {
		super(pageName);
		setDescription(JavadocExportMessages.getString("JavadcoStandardWizardPage.description")); //$NON-NLS-1$

		fStore= store;
		fButtonsList= new ArrayList();
		fStyleSheetStatus= new StatusInfo();
		
	}
	/*
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		
		initializeDialogUnits(parent);
		lastProject= null;
		fWizard= (JavadocWizard)this.getWizard();
	
		fUpperComposite= new Composite(parent, SWT.NONE);
		fUpperComposite.setLayoutData(createGridData(GridData.FILL_VERTICAL | GridData.FILL_HORIZONTAL, 1, 0));
		
		GridLayout layout= createGridLayout(4);
		layout.marginHeight= 0;
		fUpperComposite.setLayout(layout);
		
		createBasicOptionsGroup(fUpperComposite);
		createTagOptionsGroup(fUpperComposite);
		createListDialogField(fUpperComposite);
		createStyleSheetGroup(fUpperComposite);
		
		setControl(fUpperComposite);
		WorkbenchHelp.setHelp(fUpperComposite, IJavaHelpContextIds.JAVADOC_STANDARD_PAGE);
	}
		private void createBasicOptionsGroup(Composite composite) {
		
		fTitleButton= createButton(composite, SWT.CHECK, JavadocExportMessages.getString("JavadcoStandardWizardPage.titlebutton.label"), createGridData(1));  //$NON-NLS-1$
		fTitleText= createText(composite, SWT.SINGLE | SWT.BORDER, null, createGridData(GridData.FILL_HORIZONTAL, 3, 0));
		String text= fStore.getTitle();
		if(!text.equals("")) { //$NON-NLS-1$
			fTitleText.setText(text);
			fTitleButton.setSelection(true);
		} else fTitleText.setEnabled(false);
		
		fBasicOptionsGroup= new Group(composite, SWT.SHADOW_ETCHED_IN);
		fBasicOptionsGroup.setLayout(createGridLayout(1));
		fBasicOptionsGroup.setLayoutData(createGridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL, 2, 0));
		fBasicOptionsGroup.setText(JavadocExportMessages.getString("JavadcoStandardWizardPage.basicgroup.label"));  //$NON-NLS-1$

		fUse= new FlaggedButton(fBasicOptionsGroup, JavadocExportMessages.getString("JavadcoStandardWizardPage.usebutton.label"), new GridData(GridData.FILL_HORIZONTAL), fStore.USE, true); //$NON-NLS-1$
		fHierarchyCheck= new FlaggedButton(fBasicOptionsGroup, JavadocExportMessages.getString("JavadcoStandardWizardPage.hierarchybutton.label"), new GridData(GridData.FILL_HORIZONTAL), fStore.NOTREE, false);  //$NON-NLS-1$
		fNavigatorCheck= new FlaggedButton(fBasicOptionsGroup,  JavadocExportMessages.getString("JavadcoStandardWizardPage.navigartorbutton.label"), new GridData(GridData.FILL_HORIZONTAL), fStore.NONAVBAR, false); //$NON-NLS-1$

		fIndexCheck= new FlaggedButton(fBasicOptionsGroup, JavadocExportMessages.getString("JavadcoStandardWizardPage.indexbutton.label"), new GridData(GridData.FILL_HORIZONTAL), fStore.NOINDEX, false); //$NON-NLS-1$

		fSeperatedIndexCheck= new FlaggedButton(fBasicOptionsGroup, JavadocExportMessages.getString("JavadcoStandardWizardPage.seperateindexbutton.label"), createGridData(GridData.GRAB_HORIZONTAL, 1, convertWidthInCharsToPixels(3)), fStore.SPLITINDEX, true);  //$NON-NLS-1$
		fSeperatedIndexCheck.getButton().setEnabled(fIndexCheck.getButton().getSelection());

		fIndexCheck.getButton().addSelectionListener(new ToggleSelectionAdapter(new Control[] { fSeperatedIndexCheck.getButton()}) {
			public void validate() {
			}
		});
		
		fTitleButton.addSelectionListener(new ToggleSelectionAdapter(new Control[] { fTitleText}) {
			public void validate() {
			}
		});

	}

	private void createTagOptionsGroup(Composite composite) {
		fTagsGroup= new Group(composite, SWT.SHADOW_ETCHED_IN);
		fTagsGroup.setLayout(createGridLayout(1));
		fTagsGroup.setLayoutData(createGridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL, 2, 0));
		fTagsGroup.setText(JavadocExportMessages.getString("JavadcoStandardWizardPage.tagsgroup.label")); //$NON-NLS-1$

		fAuthorCheck= new FlaggedButton(fTagsGroup, JavadocExportMessages.getString("JavadcoStandardWizardPage.authorbutton.label"), new GridData(GridData.FILL_HORIZONTAL), fStore.AUTHOR, true); //$NON-NLS-1$
		fVersionCheck= new FlaggedButton(fTagsGroup, JavadocExportMessages.getString("JavadcoStandardWizardPage.versionbutton.label"), new GridData(GridData.FILL_HORIZONTAL), fStore.VERSION, true); //$NON-NLS-1$
		fDeprecatedCheck= new FlaggedButton(fTagsGroup, JavadocExportMessages.getString("JavadcoStandardWizardPage.deprecatedbutton.label"), new GridData(GridData.FILL_HORIZONTAL), fStore.NODEPRECATED, false); //$NON-NLS-1$
		fDeprecatedList= new FlaggedButton(fTagsGroup, JavadocExportMessages.getString("JavadcoStandardWizardPage.deprecatedlistbutton.label"), createGridData(GridData.FILL_HORIZONTAL, 1, convertWidthInCharsToPixels(3)), fStore.NODEPRECATEDLIST, false); //$NON-NLS-1$
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

		fStyleSheetButton= createButton(c, SWT.CHECK, JavadocExportMessages.getString("JavadcoStandardWizardPage.stylesheettext.label"), createGridData(1));  //$NON-NLS-1$
		fStyleSheetText= createText(c, SWT.SINGLE | SWT.BORDER, null, createGridData(GridData.FILL_HORIZONTAL, 1, 0));
		//there really aught to be a way to specify this
		((GridData) fStyleSheetText.getLayoutData()).widthHint = 200;
		fStyleSheetBrowseButton= createButton(c, SWT.PUSH, JavadocExportMessages.getString("JavadocStandardWizardPage.stylesheetbrowsebutton.label"), createGridData(GridData.HORIZONTAL_ALIGN_END, 1, 0));   //$NON-NLS-1$
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

	private void createListDialogField(Composite composite) {
		Composite c= new Composite(composite, SWT.NONE);
		c.setLayout(createGridLayout(3));
		c.setLayoutData(createGridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL, 4, 0));
		((GridLayout) c.getLayout()).marginWidth= 0;
	
		String[] buttonlabels= new String[] {JavadocExportMessages.getString("JavadcoStandardWizardPage.selectallbutton.label") , JavadocExportMessages.getString("JavadcoStandardWizardPage.clearallbutton.label"), JavadocExportMessages.getString("JavadocStandardWizardPage.configurebutton.label")}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		JavadocLinkDialogLabelProvider labelProvider= new JavadocLinkDialogLabelProvider();
		
		fListDialogField= new CheckedListDialogField(new ListAdapter(), buttonlabels, labelProvider);
		fListDialogField.setCheckAllButtonIndex(0);
		fListDialogField.setUncheckAllButtonIndex(1);
	
		createLabel(c, SWT.NONE, JavadocExportMessages.getString("JavadcoStandardWizardPage.referencedclasses.label"), createGridData(GridData.HORIZONTAL_ALIGN_BEGINNING, 4, 0)); //$NON-NLS-1$
		fListDialogField.doFillIntoGrid(c, 3);
		
		LayoutUtil.setHorizontalGrabbing(fListDialogField.getListControl(null));
		
		fListDialogField.enableButton(2, false);
	
		fTempLinks= createTempLinksStore();
		updateCheckedListGroup();
		
	}

	private Map createTempLinksStore() {
		Map temp= new HashMap();
		IProject[]  projects= fStore.getRoot().getProjects();
		for (int i = 0; i < projects.length; i++) {
			IProject iProject = projects[i];
			IJavaProject javaProject= JavaCore.create(iProject);
			if(javaProject!= null) {
				String links= fStore.getLinks(javaProject);
				temp.put(javaProject, links);
			}
		}
		return temp;
	}

	private void checkListDialogFieldElements(List referencedClasses) {
		List checkedElements= new ArrayList();
		String hrefs=  (String)fTempLinks.get(fWizard.getProject());
		
		URL url= null;
		if(!hrefs.equals("")) { //$NON-NLS-1$
		
			for (Iterator iterator = referencedClasses.iterator(); iterator.hasNext();) {
				IJavaElement element = (IJavaElement) iterator.next();
				try {
					url= JavaDocLocations.getJavadocBaseLocation(element);
				} catch(JavaModelException e) {
					JavaPlugin.log(e);
					continue;
				}
				StringTokenizer tokenizer = new  StringTokenizer(hrefs, ";"); //$NON-NLS-1$
				while(tokenizer.hasMoreElements()) {
					String href = (String)tokenizer.nextElement();
					if((url!=null) && href.equals(url.toExternalForm())) {
						checkedElements.add(element);		
						break;
					}		
				}		
			}	
		}
		fListDialogField.setCheckedElements(checkedElements);
	}
	
	/**
	 * Method finds a list of all referenced libararies and projects.
	 * 
	 */
	private void findReferencedElements(List referencedClasses, IJavaProject jproject, List visited) throws JavaModelException {

		//to avoid loops
		if (visited.contains(jproject)) {
			return;
		}
		visited.add(jproject);

		IClasspathEntry[] entries= jproject.getResolvedClasspath(true);
		for (int i= 0; i < entries.length; i++) {
			IClasspathEntry curr= entries[i];
			switch (curr.getEntryKind()) {
				case IClasspathEntry.CPE_LIBRARY :
					IPackageFragmentRoot el= jproject.getPackageFragmentRoot(curr.getPath().toOSString());
					if (el != null) {
						if (!referencedClasses.contains(el)) {
							referencedClasses.add(el);
						}
					}
					break;
				case IClasspathEntry.CPE_PROJECT :
					IProject reqProject= (IProject) fStore.getRoot().findMember(curr.getPath());
					IJavaProject javaProject= JavaCore.create(reqProject);

					if (reqProject.isOpen()) {
						if (!referencedClasses.contains(javaProject)) {
							referencedClasses.add(javaProject);
							findReferencedElements(referencedClasses, javaProject, visited);
						}
					}
					break;
			}

		}

	}
		
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
						fStyleSheetStatus.setError(JavadocExportMessages.getString("JavadcoStandardWizardPage.stylesheetnopath.error"));  //$NON-NLS-1$
					} else if ((ext == null) || !ext.equalsIgnoreCase("css")) { //$NON-NLS-1$
						fStyleSheetStatus.setError(JavadocExportMessages.getString("JavadcoStandardWizardPage.stylesheetnotcss.error")); //$NON-NLS-1$
					}
				}
				break;
		}

		updateStatus(findMostSevereStatus());

	}
	
	private IStatus findMostSevereStatus() {
		return StatusUtil.getMostSevere(new IStatus[] { fStyleSheetStatus});
	}
	
	protected void finish() {

			if (fTitleButton.getSelection())
				fStore.setTitle(fTitleText.getText());
			else
				fStore.setTitle(""); //$NON-NLS-1$

			//don't store the buttons if they are not enabled
			//this will change when there is a single page aimed at the standard doclet
			if (true) {
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

			if (fStyleSheetText.getEnabled())
				fStore.setStyleSheet(fStyleSheetText.getText());
			else
				fStore.setStyleSheet(""); //$NON-NLS-1$

			String hrefs = makeHrefString();
			fStore.setLinks(fWizard.getProject(), hrefs);

		}

	protected String makeHrefString() {
		boolean firstTime= true;
		StringBuffer buf= new StringBuffer();
		List els = fListDialogField.getCheckedElements();
		URL url = null;
		for (Iterator iterator = els.iterator(); iterator.hasNext();) {
			try {
				IJavaElement element = (IJavaElement) iterator.next();
				url = JavaDocLocations.getJavadocBaseLocation(element);
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
				continue;
			}
			if (url != null) {
				if(firstTime)
					firstTime= false;
				else buf.append(";"); //$NON-NLS-1$
				buf.append(url.toExternalForm());
			}
		}
		return buf.toString();
	}

	//get the links
	

	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			doValidation(STYLESHEETSTATUS);

			//update elements CheckedListDialogField
			updateCheckedListGroup();
		} else {
			String hrefs= makeHrefString();
			fTempLinks.put(fWizard.getProject(), hrefs);	
		}
	}

	public void updateCheckedListGroup() {
	
		List referencedClasses = new ArrayList();
		List visited = new ArrayList();
		try {
			IJavaProject currProject = fWizard.getProject();
			if (lastProject != currProject) {
				lastProject= currProject;
				findReferencedElements(referencedClasses, currProject, visited);
				fListDialogField.removeAllElements();
				fListDialogField.addElements(referencedClasses);
				//compare with elements in list with those that are checked.
				checkListDialogFieldElements(referencedClasses);
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
	}
	
	public void init() {
		updateStatus(new StatusInfo());
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

	private class ListAdapter implements IListAdapter {
		
		/**
		 * @see IListAdapter#customButtonPressed(DialogField, int)
		 */
		public void customButtonPressed(DialogField field, int index) {
				if(index == 2)
					doEditButtonPressed();
		}


		/**
		 * @see IListAdapter#selectionChanged(DialogField)
		 */
		public void selectionChanged(DialogField field) {
			List selection = fListDialogField.getSelectedElements();
			if(selection.size() != 1) {
					fListDialogField.enableButton(2, false);
			} else {
				fListDialogField.enableButton(2, true);
			}
		}

	}
	
		/**
		 * Method doEditButtonPressed.
		 */
	private void doEditButtonPressed() {

		StructuredSelection selection =	(StructuredSelection) fListDialogField.getTableViewer().getSelection();

		Object obj = selection.getFirstElement();
		if (obj instanceof IAdaptable) {
			IJavaElement el = (IJavaElement) ((IAdaptable) obj).getAdapter(IJavaElement.class);

			JavadocPropertyDialog jdialog = new JavadocPropertyDialog(getShell(), el);
			jdialog.open();

		}
	}
	
	private class JavadocPropertyDialog extends StatusDialog implements IStatusChangeListener {
		
		private JavadocConfigurationBlock fJavadocConfigurationBlock;
		private IJavaElement fElement;
				
		public JavadocPropertyDialog(Shell parent, IJavaElement selection) {
			super(parent);
			setTitle(JavadocExportMessages.getString("JavadocStandardWizardPage.javadocpropertydialog.title")); //$NON-NLS-1$
	
			fJavadocConfigurationBlock= new JavadocConfigurationBlock( selection, parent , this);
		}
				
		protected Control createDialogArea(Composite parent) {
			Composite composite= (Composite)super.createDialogArea(parent);			
			Control inner= fJavadocConfigurationBlock.createContents(composite);
			inner.setLayoutData(new GridData(GridData.FILL_BOTH));
			return composite;
		}
		
		public void statusChanged(IStatus status) {
			updateStatus(status);
			
		}
			
		/**
		 * @see Dialog#okPressed()
		 */
		protected void okPressed() {
			fJavadocConfigurationBlock.performOk();
			super.okPressed();
			
			fListDialogField.refresh();
		}

		/*
		 * @see org.eclipse.jface.window.Window#configureShell(Shell)
		 */
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			WorkbenchHelp.setHelp(newShell, IJavaHelpContextIds.JAVADOC_PROPERTY_DIALOG);
		}
	}
}
