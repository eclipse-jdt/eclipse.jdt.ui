/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.launcher;

import java.lang.reflect.InvocationTargetException;import java.util.Iterator;import org.eclipse.core.runtime.CoreException;import org.eclipse.core.runtime.IPath;import org.eclipse.core.runtime.IProgressMonitor;import org.eclipse.core.runtime.Path;import org.eclipse.jdt.core.JavaCore;import org.eclipse.jdt.internal.ui.preferences.ClasspathVariablesPreferencePage;import org.eclipse.jdt.internal.ui.util.ExceptionHandler;import org.eclipse.jdt.launching.IVMInstall;import org.eclipse.jdt.launching.IVMInstallType;import org.eclipse.jdt.launching.JavaRuntime;import org.eclipse.jdt.launching.LibraryLocation;import org.eclipse.jface.dialogs.ProgressMonitorDialog;import org.eclipse.jface.preference.PreferencePage;import org.eclipse.jface.viewers.ColumnWeightData;import org.eclipse.jface.viewers.ISelectionChangedListener;import org.eclipse.jface.viewers.IStructuredSelection;import org.eclipse.jface.viewers.SelectionChangedEvent;import org.eclipse.jface.viewers.StructuredSelection;import org.eclipse.jface.viewers.TableLayout;import org.eclipse.jface.viewers.TableViewer;import org.eclipse.swt.SWT;import org.eclipse.swt.layout.GridData;import org.eclipse.swt.layout.GridLayout;import org.eclipse.swt.widgets.Button;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Control;import org.eclipse.swt.widgets.Event;import org.eclipse.swt.widgets.Label;import org.eclipse.swt.widgets.Listener;import org.eclipse.swt.widgets.Table;import org.eclipse.swt.widgets.TableColumn;import org.eclipse.swt.widgets.TableItem;import org.eclipse.ui.IWorkbench;import org.eclipse.ui.IWorkbenchPreferencePage;import org.eclipse.ui.actions.WorkspaceModifyOperation;

/*
 * The page for setting the default java runtime preference.
 */
public class VMPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {	
	public static final String PREFIX= "org.eclipse.jdt.ui.launcher.";
	public static final String DEFAULT_VM_LABEL= "launcher.default_vm.label";
	public static final String PREF_VM= PREFIX+"default_vm";

	private TableViewer fVMList;
	private Button fAddButton;
	private Button fRemoveButton;
	private Button fEditButton;
	private Button fDefaultCheckbox;
	
	private IVMInstallType[] fVMTypes;
	
	private IPath[] fClasspathVariables= new IPath[3];

	public VMPreferencePage() {
		super();
	}
	
	/**
	 * @see IWorkbenchPreferencePage#init
	 */
	public void init(IWorkbench workbench) {
	}
	
	/**
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite ancestor) {
		fVMTypes= JavaRuntime.getVMInstallTypes();
		noDefaultAndApplyButton();
		
		Composite parent= new Composite(ancestor, SWT.NULL);
		GridLayout layout= new GridLayout();
		layout.numColumns= 3;
		parent.setLayout(layout);		
		
		Label l2= new Label(parent, SWT.NULL);
		l2.setText("VM Instances:");
		GridData gd= new GridData();
		gd.verticalAlignment= gd.BEGINNING;
		l2.setLayoutData(gd);
		
		fVMList= new TableViewer(parent, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		gd= new GridData(GridData.FILL_BOTH);
		gd.widthHint= convertWidthInCharsToPixels(60);
		fVMList.getTable().setLayoutData(gd);
		fVMList.setLabelProvider(new VMLabelProvider());
		fVMList.setContentProvider(new VMContentProvider());	
		Table table= fVMList.getTable();
		
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		
		TableLayout tableLayout= new TableLayout();
		table.setLayout(tableLayout);
		
		TableColumn column1= new TableColumn(table, SWT.NULL);
		column1.setText("VM Install Type");
		tableLayout.addColumnData(new ColumnWeightData(50));
	
		TableColumn column2= new TableColumn(table, SWT.NULL);
		column2.setText("Name");
		column2.setWidth(convertWidthInCharsToPixels(30));
		tableLayout.addColumnData(new ColumnWeightData(50));
		
		fVMList.setInput(JavaRuntime.getVMInstallTypes());	
		
		fVMList.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent evt) {
				vmSelectionChanged();
			}
		});
		
		Composite buttons= new Composite(parent, SWT.NULL);
		buttons.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
		buttons.setLayout(new GridLayout());
		
		fAddButton= new Button(buttons, SWT.PUSH);
		fAddButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fAddButton.setText("Add...");
		fAddButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event evt) {
				addVM();
			}
		});
		
		
		fRemoveButton= new Button(buttons, SWT.PUSH);
		fRemoveButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fRemoveButton.setText("Remove...");
		fRemoveButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event evt) {
				removeVMs();
			}
		});

		fEditButton= new Button(buttons, SWT.PUSH);
		fEditButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fEditButton.setText("Edit...");
		fEditButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event evt) {
				editVM();
			}
		});
		
		// placeholder
		Label l3= new Label(parent, SWT.NULL);
		l3.setLayoutData(new GridData());
		
		fDefaultCheckbox= new Button(parent, SWT.CHECK);
		fDefaultCheckbox.setText("Default VM");
		fDefaultCheckbox.setLayoutData(new GridData());
		fDefaultCheckbox.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event evt) {
				setDefaultVM();
			}
		});
		
		return parent;
	}
	
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible)
			selectVM(JavaRuntime.getDefaultVMInstall());
	}

	protected void updateVMItem(TableItem item, IVMInstall vm) {
	}
		
	private void addVM() {
		AddVMDialog dialog= new AddVMDialog(getShell(), fVMTypes, fVMTypes[0]);
		dialog.setTitle("Add VM");
		if (dialog.open() != dialog.OK)
			return;
		fVMList.refresh();
	}
	
	
	private void removeVMs() {
		IStructuredSelection selection= (IStructuredSelection)fVMList.getSelection();
		Iterator elements= selection.iterator();
		while (elements.hasNext()) {
			IVMInstall vmInstall= (IVMInstall)elements.next();
			vmInstall.getVMInstallType().disposeVMInstall(vmInstall.getId());
		}
		fVMList.refresh();
	}
		
	// editing
	private void editVM() {
		IStructuredSelection selection= (IStructuredSelection)fVMList.getSelection();
		// assume it's length one, otherwise this will not be called
		IVMInstall vm= (IVMInstall)selection.getFirstElement();
		editVM(vm);
	}
	
	private void editVM(IVMInstall vm) {
		EditVMDialog dialog= new EditVMDialog(getShell(), fVMTypes, vm);
		dialog.setTitle("Edit VM");
		if (dialog.open() != dialog.OK)
			return;
		fVMList.refresh(vm);
	}

	public boolean performOk() {
		try {
			JavaRuntime.saveVMConfiguration();
			setClassPathVariables();
		} catch (CoreException e) {
			ExceptionHandler.handle(e, "VM Configuration", "An exception occurred while saving configuration data");
		}
		return super.performOk();
	}
	
	private void vmSelectionChanged() {
		updateDefaultVM();
		enableButtons();
	}

	private void enableButtons() {
		fAddButton.setEnabled(fVMTypes.length > 0);
		int selectionCount= ((IStructuredSelection)fVMList.getSelection()).size();
		fEditButton.setEnabled(selectionCount == 1);
		fRemoveButton.setEnabled(selectionCount > 0);
		updateDefaultVM();
	}
	
	private void setDefaultVM() {
		if (fDefaultCheckbox.getSelection()) {
			IStructuredSelection selection= (IStructuredSelection)fVMList.getSelection();
			// assume it's length one, otherwise this will not be called
			IVMInstall vm= (IVMInstall)selection.getFirstElement();
			JavaRuntime.setDefaultVMInstall(vm);
		} else {
			JavaRuntime.setDefaultVMInstall(null);
		}
	}

	private void updateDefaultVM() {
		IStructuredSelection selection= (IStructuredSelection)fVMList.getSelection();
		if (selection.size() != 1) {
			fDefaultCheckbox.setSelection(false);
			fDefaultCheckbox.setEnabled(false);
		
		} else {			
			IVMInstall vm= (IVMInstall)selection.getFirstElement();
			boolean isDefault= vm == JavaRuntime.getDefaultVMInstall();
			fDefaultCheckbox.setSelection(isDefault);
			fDefaultCheckbox.setEnabled(!isDefault);
		}
	}
	
	private void selectVM(IVMInstall vm) {
		if (vm == null)
			return;
		fVMList.setSelection(new StructuredSelection(vm));
		vmSelectionChanged();
	}
	
	private void setClassPathVariables() throws CoreException {
		IVMInstall vmInstall= JavaRuntime.getDefaultVMInstall();
		if (vmInstall != null) {
			LibraryLocation desc= vmInstall.getLibraryLocation();
			if (desc == null)
				desc= vmInstall.getVMInstallType().getDefaultLibraryLocation(vmInstall.getInstallLocation());
			setClassPathVariables(desc);
		}
	}
	
	private void setClassPathVariables(final LibraryLocation desc) throws CoreException {
		
		ProgressMonitorDialog dialog= new ProgressMonitorDialog(getShell());
		
		try {
			dialog.run(true, true, new WorkspaceModifyOperation() {
				public void execute(IProgressMonitor pm) throws InvocationTargetException{
					try {
						doSetClasspathVariables(desc, pm);
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
				}
			});
		} catch (InterruptedException e) {
		} catch (InvocationTargetException e) {
			throw (CoreException)e.getTargetException();
		}
	}
	
	private static void doSetClasspathVariables(LibraryLocation desc, IProgressMonitor pm) throws CoreException {
		IPath oldLibrary= JavaCore.getClasspathVariable(ClasspathVariablesPreferencePage.JRELIB_VARIABLE);
		IPath oldSource= JavaCore.getClasspathVariable(ClasspathVariablesPreferencePage.JRESRC_VARIABLE);
		IPath oldPkgRoot= JavaCore.getClasspathVariable(ClasspathVariablesPreferencePage.JRESRCROOT_VARIABLE);

		IPath library= new Path(desc.getSystemLibrary().getAbsolutePath());
		IPath source= new Path(desc.getSystemLibrarySource().getAbsolutePath());
		IPath pkgRoot= desc.getPackageRootPath();
			if (!library.equals(oldLibrary))
				JavaCore.setClasspathVariable(ClasspathVariablesPreferencePage.JRELIB_VARIABLE, library, pm);
			if (!source.equals(oldSource))
				JavaCore.setClasspathVariable(ClasspathVariablesPreferencePage.JRESRC_VARIABLE, source, pm);
			if (!pkgRoot.equals(oldPkgRoot))
				JavaCore.setClasspathVariable(ClasspathVariablesPreferencePage.JRESRCROOT_VARIABLE, pkgRoot, pm);
	}
	
	public static void initializeVMInstall() throws CoreException {
		IVMInstall defaultVM= JavaRuntime.getDefaultVMInstall();
		if (defaultVM == null) {
			defaultVM= getFirstVMInstall();
			if (defaultVM != null)
				JavaRuntime.setDefaultVMInstall(defaultVM);
		}
		IVMInstall vmInstall= JavaRuntime.getDefaultVMInstall();
		if (vmInstall != null) {
			LibraryLocation desc= vmInstall.getLibraryLocation();
			if (desc == null)
				desc= vmInstall.getVMInstallType().getDefaultLibraryLocation(vmInstall.getInstallLocation());
			doSetClasspathVariables(desc, null);
		}
		
	}
	
	private static IVMInstall getFirstVMInstall() {
		IVMInstallType[] vmTypes= JavaRuntime.getVMInstallTypes();
		for (int i= 0; i < vmTypes.length; i++) {
			IVMInstall[] vms= vmTypes[i].getVMInstalls();
			if (vms.length > 0)
				return vms[0];
		}
		return null;
	}
}
