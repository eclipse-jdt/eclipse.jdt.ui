package org.eclipse.jdt.internal.ui.preferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.internal.debug.core.JDIDebugPlugin;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.IPreferencesConstants;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.help.DialogPageContextComputer;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.model.WorkbenchViewerSorter;

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
	private Label fFilterViewerLabel;
	private CheckboxTableViewer fFilterViewer;
	private Button fUseFiltersCheckbox;
	private Button fAddPackageButton;
	private Button fAddTypeButton;
	private Button fRemoveFilterButton;
	private Button fAddFilterButton;
	private Text fFilterText;
	private Label fFilterTextLabel;
	
	private StepFilterContentProvider fStepFilterContentProvider;
	
	public class StepFilterContentProvider implements IStructuredContentProvider {
		private JDIDebugPlugin fPlugin;
		private CheckboxTableViewer fViewer;
		private List fActive;
		private List fInactive;
		
		public StepFilterContentProvider() {
			fPlugin = JDIDebugPlugin.getDefault();
			fActive = new ArrayList(fPlugin.getActiveStepFilters());
			fInactive = new ArrayList(fPlugin.getInactiveStepFilters());
		}
		
		public void saveFilters() {
			fPlugin.setUseStepFilters(fUseFiltersCheckbox.getSelection());
			fPlugin.setActiveStepFilters(fActive);
			fPlugin.setInactiveStepFilters(fInactive);
		}
		
		public void addActiveFilter(String filter) {
			if (!fActive.contains(filter)) {
				fActive.add(filter);
				fViewer.add(filter);
				fViewer.setChecked(filter, true);
			}
		}
		
		public void initializeCheckedState() {
			setCheckedState(fActive, true);
			setCheckedState(fInactive, false);
		}
		
		private void setCheckedState(List list, boolean checked) {
			Iterator iterator = list.iterator();
			while (iterator.hasNext()) {
				String filter = (String)iterator.next();
				fViewer.setChecked(filter, checked);
			}
		}
		
		public void removeFilters(Object[] filters) {
			for (int i = 0; i < filters.length; i++) {
				String filter = (String)filters[i];
				if (fActive.contains(filter)) {
					fActive.remove(filter);
				} else {
					fInactive.remove(filter);
				}
			}
			fViewer.remove(filters);
		}
		
		public void toggleFilter(String filter, boolean currentlyActive) {
			if (currentlyActive) {
				fActive.remove(filter);
				fInactive.add(filter);
			} else {
				fInactive.remove(filter);
				fActive.add(filter);
			}
			fViewer.setChecked(filter, !currentlyActive);
		}
		
		public Object[] getElements(Object inputElement) {
			List filters = new ArrayList(fActive);
			filters.addAll(fInactive);
			return filters.toArray();
		}
		
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			fViewer = (CheckboxTableViewer)viewer;	
		}
		
		public void dispose() {
		}		
	}
	
	public class StepFilterLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object object, int column) {
			if (column == 0) {
				return (String)object;
			}
			return "";
		}
		
		public Image getColumnImage(Object object, int column) {
			return null;
		}
	}

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
		
		createStepFilterPreferences(composite);
		
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
	 * Create a group to contain the step filter related widgetry
	 */
	private void createStepFilterPreferences(Composite parent) {
		Group group = createGroup(parent, 1, "Step filters");
		
		Composite container = new Composite(group, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		container.setLayout(layout);
		
		fUseFiltersCheckbox = new Button(container, SWT.CHECK);
		fUseFiltersCheckbox.setText("Use &step filters");
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.horizontalSpan = 2;
		fUseFiltersCheckbox.setLayoutData(gd);	
		fUseFiltersCheckbox.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent se) {
				toggleStepFilterWidgetsEnabled(fUseFiltersCheckbox.getSelection());
			}
			public void widgetDefaultSelected(SelectionEvent se) {
			}
		});	
		
		Composite textContainer = new Composite(container, SWT.NONE);
		GridLayout textLayout = new GridLayout();
		textLayout.numColumns = 2;
		textLayout.marginHeight = 0;
		textLayout.marginWidth = 0;
		textContainer.setLayout(textLayout);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		textContainer.setLayoutData(gd);
		
		fAddFilterButton = new Button(container, SWT.PUSH);
		fAddFilterButton.setText("Add &Filter");
		gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		fAddFilterButton.setLayoutData(gd);
		fAddFilterButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent se) {
				addActiveFilter();
			}
			public void widgetDefaultSelected(SelectionEvent se) {
			}
		});
		
		fFilterTextLabel = new Label(textContainer, SWT.NONE);
		fFilterTextLabel.setText("Filter");
		
		fFilterText = new Text(textContainer, SWT.BORDER);
		fFilterText.setEditable(true);
		gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 300;
		fFilterText.setLayoutData(gd);
		
		fFilterViewerLabel = new Label(container, SWT.NONE);
		fFilterViewerLabel.setText("");
		gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 2;
		fFilterViewerLabel.setLayoutData(gd);
		
		fFilterViewer = new CheckboxTableViewer(container, SWT.BORDER | SWT.MULTI);
		fStepFilterContentProvider = new StepFilterContentProvider();
		fFilterViewer.setContentProvider(fStepFilterContentProvider);
		fFilterViewer.setLabelProvider(new StepFilterLabelProvider());
		fFilterViewer.setSorter(new WorkbenchViewerSorter());
		fFilterViewer.setInput(JDIDebugPlugin.getDefault());
		fStepFilterContentProvider.initializeCheckedState();
		gd = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
		gd.heightHint = 300;
		gd.widthHint = 300;
		fFilterViewer.getTable().setLayoutData(gd);
		fFilterViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				String filter = (String)event.getElement();
				boolean checked = event.getChecked();
				fStepFilterContentProvider.toggleFilter(filter, !checked);
			}
		});
		fFilterViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				ISelection selection = event.getSelection();
				if (selection.isEmpty()) {
					fRemoveFilterButton.setEnabled(false);
				} else {
					fRemoveFilterButton.setEnabled(true);					
				}
			}
		});
		
		Composite buttonContainer = new Composite(container, SWT.NONE);
		gd = new GridData(GridData.FILL_VERTICAL);
		buttonContainer.setLayoutData(gd);
		GridLayout buttonLayout = new GridLayout();
		buttonLayout.numColumns = 1;
		buttonLayout.marginHeight = 0;
		buttonLayout.marginWidth = 0;
		buttonContainer.setLayout(buttonLayout);
		
		fAddPackageButton = new Button(buttonContainer, SWT.PUSH);
		fAddPackageButton.setText("Add &package");
		gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		fAddPackageButton.setLayoutData(gd);
		fAddPackageButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent se) {
				addPackage();
			}
			public void widgetDefaultSelected(SelectionEvent se) {
			}
		});
		
		fAddTypeButton = new Button(buttonContainer, SWT.PUSH);
		fAddTypeButton.setText("Add &type");
		gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		fAddTypeButton.setLayoutData(gd);
		fAddTypeButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent se) {
				addType();
			}
			public void widgetDefaultSelected(SelectionEvent se) {
			}
		});
		
		fRemoveFilterButton = new Button(buttonContainer, SWT.PUSH);
		fRemoveFilterButton.setText("&Remove");
		gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		fRemoveFilterButton.setLayoutData(gd);
		fRemoveFilterButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent se) {
				removeFilters();
			}
			public void widgetDefaultSelected(SelectionEvent se) {
			}
		});
		fRemoveFilterButton.setEnabled(false);
		
		boolean enabled = JDIDebugPlugin.getDefault().useStepFilters();
		fUseFiltersCheckbox.setSelection(enabled);
		toggleStepFilterWidgetsEnabled(enabled);
	}
	
	private void toggleStepFilterWidgetsEnabled(boolean enabled) {
		fFilterViewer.getTable().setEnabled(enabled);
		fFilterViewerLabel.setEnabled(enabled);
		fAddPackageButton.setEnabled(enabled);
		fAddTypeButton.setEnabled(enabled);
		fAddFilterButton.setEnabled(enabled);
		fFilterTextLabel.setEnabled(enabled);
		fFilterText.setEnabled(enabled);
		if (!enabled) {
			fRemoveFilterButton.setEnabled(enabled);
		}
	}
	
	private void addActiveFilter() {
		String filter = fFilterText.getText();
		if ( (filter != null) && (filter.trim().length() > 0) ) {
			fStepFilterContentProvider.addActiveFilter(filter);
			fFilterText.setText("");
		}
	}
	
	private void removeFilters() {
		IStructuredSelection selection = (IStructuredSelection)fFilterViewer.getSelection();		
		fStepFilterContentProvider.removeFilters(selection.toArray());
	}
	
	private void addType() {
		Shell shell= JavaPlugin.getActiveWorkbenchShell();
		SelectionDialog dialog= null;
		try {
			dialog= JavaUI.createTypeDialog(shell, new ProgressMonitorDialog(shell),
				SearchEngine.createWorkspaceScope(), IJavaElementSearchConstants.CONSIDER_TYPES, false);
		} catch (JavaModelException jme) {
			String title= "Add type to step filters";
			String message= "Could not open type selection dialog for step filters";
			ExceptionHandler.handle(jme, title, message);
			return;
		}
	
		dialog.setTitle("Add type to step filters");
		dialog.setMessage("Select a type to filter when stepping");
		if (dialog.open() == IDialogConstants.CANCEL_ID) {
			return;
		}
		
		Object[] types= dialog.getResult();
		if (types != null && types.length > 0) {
			IType type = (IType)types[0];
			fStepFilterContentProvider.addActiveFilter(type.getFullyQualifiedName());
		}		
	}
	
	private void addPackage() {
		Shell shell= JavaPlugin.getActiveWorkbenchShell();
		SelectionDialog dialog = null;
		try {
			dialog = createAllPackagesDialog(shell);
		} catch (JavaModelException jme) {
			String title= "Add package to step filters";
			String message= "Could not open package selection dialog for step filters";
			ExceptionHandler.handle(jme, title, message);
			return;			
		}
	
		dialog.setTitle("Add package to step filters");
		dialog.setMessage("Select a package to filter when stepping");
		if (dialog.open() == IDialogConstants.CANCEL_ID) {
			return;
		}
		
		Object[] packages= dialog.getResult();
		if (packages != null && packages.length > 0) {
			IJavaElement pkg = (IJavaElement)packages[0];
			String filter = pkg.getElementName();
			if (filter.length() < 1) {
				filter = "(default package)";
			} else {
				filter += ".*"; //$NON-NLS-1$
			}
			fStepFilterContentProvider.addActiveFilter(filter);
		}		
	}

	/**
	 * Ignore package fragments that contain only non-java resources, and make sure
	 * that each fragment is added to the list only once.
	 */
	private SelectionDialog createAllPackagesDialog(Shell shell) throws JavaModelException{
		IWorkspaceRoot wsroot= JavaPlugin.getWorkspace().getRoot();
		IJavaModel model= JavaCore.create(wsroot);
		IJavaProject[] projects= model.getJavaProjects();
		Set packageNameSet= new HashSet(); 
		List packageList = new ArrayList();
		for (int i = 0; i < projects.length; i++) {						
			IPackageFragment[] pkgs= projects[i].getPackageFragments();	
			for (int j = 0; j < pkgs.length; j++) {
				IPackageFragment pkg = pkgs[j];
				if (!pkg.hasChildren() && (pkg.getNonJavaResources().length > 0)) {
					continue;
				}
				if (packageNameSet.add(pkg.getElementName())) {
					packageList.add(pkg);
				}
			}
		}
		int flags= JavaElementLabelProvider.SHOW_DEFAULT;
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(shell, new JavaElementLabelProvider(flags), false, false);
		dialog.setElements(packageList);
		return dialog;
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
		fStepFilterContentProvider.saveFilters();
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
		
		setDefaultStepFilterValues();			
	}
	
	private void setDefaultStepFilterValues() {
		
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

