/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */

package org.eclipse.jdt.internal.ui.launcher;

import java.util.List;import org.eclipse.core.runtime.CoreException;import org.eclipse.jdt.internal.ui.util.ExceptionHandler;import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;import org.eclipse.jdt.launching.IVM;import org.eclipse.jdt.launching.IVMType;import org.eclipse.jdt.launching.JavaRuntime;import org.eclipse.jface.preference.PreferencePage;import org.eclipse.swt.SWT;import org.eclipse.swt.layout.GridData;import org.eclipse.swt.layout.GridLayout;import org.eclipse.swt.widgets.Button;import org.eclipse.swt.widgets.Combo;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Control;import org.eclipse.swt.widgets.Event;import org.eclipse.swt.widgets.Label;import org.eclipse.swt.widgets.Listener;import org.eclipse.swt.widgets.Table;import org.eclipse.swt.widgets.TableItem;import org.eclipse.ui.IWorkbench;import org.eclipse.ui.IWorkbenchPreferencePage;

/*
 * The page for setting the default java runtime preference.
 */
public class VMPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {	
	public static final String PREFIX= "org.eclipse.jdt.ui.launcher.";
	public static final String DEFAULT_VM_LABEL= "launcher.default_vm.label";
	public static final String PREF_VM= PREFIX+"default_vm";

	private Combo fVMTypeCombo;
	private Table fVMList;
	private Button fAddButton;
	private Button fRemoveButton;
	private Button fEditButton;
	private Button fDefaultCheckbox;
	
	private IVMType[] fVMTypes;

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
		noDefaultAndApplyButton();
		
		Composite parent= new Composite(ancestor, SWT.NULL);
		GridLayout layout= new GridLayout();
		layout.numColumns= 3;
		parent.setLayout(layout);
		
		Label l1= new Label(parent, SWT.NULL);
		l1.setText("VM Type:");
		l1.setLayoutData(new GridData());
		
		fVMTypeCombo= new Combo(parent, SWT.READ_ONLY);
		fVMTypeCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fVMTypeCombo.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event evt) {
				vmTypeChanged(fVMTypes[fVMTypeCombo.getSelectionIndex()]);
			}
		});
		
		
		// filler label for grid
		Label l= new Label(parent, SWT.NULL);
		l.setLayoutData(new GridData());
		
		Label l2= new Label(parent, SWT.NULL);
		l2.setText("VM Instances:");
		GridData gd= new GridData();
		gd.verticalAlignment= gd.BEGINNING;
		l2.setLayoutData(gd);
		
		fVMList= new Table(parent, SWT.BORDER | SWT.MULTI);
		gd= new GridData(GridData.FILL_BOTH);
		fVMList.setLayoutData(gd);
		fVMList.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event evt) {
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
		
		initWithVMTypes(fVMTypeCombo);
		return parent;
	}
	
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible)
			selectVM(JavaRuntime.getDefaultVM());
	}

	private void initWithVMTypes(Combo vmTypeCombo) {
		fVMTypes= JavaRuntime.getVMTypes();
		for (int i= 0; i < fVMTypes.length; i++) {
			vmTypeCombo.add(fVMTypes[i].getName());
		}
		if (vmTypeCombo.getItemCount() > 0) {
			vmTypeCombo.select(0);
			vmTypeChanged(fVMTypes[0]);
		}
	}
	
	private void vmTypeChanged(IVMType vmType) {
		fVMList.removeAll();
		IVM[] vms= vmType.getVMs();
		for (int i= 0; i < vms.length; i++) {
			addVMItem(vms[i]);
		}
		vmSelectionChanged();
	}
	
	private void addVMItem(IVM vm) {
		TableItem item= new TableItem(fVMList, SWT.NULL);
		item.setText(vm.getName());
		item.setData(vm);
	}

	protected void updateVMItem(TableItem item, IVM vm) {
	}
	
	private IVMType getVMType() {
		int selected= fVMTypeCombo.getSelectionIndex();
		if (selected < 0)
			return null;
		return JavaRuntime.getVMTypes()[selected];
	}
	
	// adding
	private void addVM() {
		IVMType vmType= getVMType();
		if (vmType != null)
			addVM(vmType);
	}
	
	private void addVM(IVMType vmType) {
		AddVMDialog dialog= new AddVMDialog(getShell(), vmType);
		dialog.setTitle("Add VM");
		dialog.setVMName("");
		if (dialog.open() != dialog.OK)
			return;
		IVM vm= vmType.createVM(createUniqueId(vmType));
		vm.setInstallLocation(dialog.getInstallLocation());
		vm.setName(dialog.getVMName());
		addVMItem(vm);
	}
	
	private String createUniqueId(IVMType vmType) {
		String id= null;
		do {
			id= String.valueOf(System.currentTimeMillis());
		} while (vmType.findVM(id) != null);
		return id;
	}
	
	// removing
	private void removeVMs() {
		IVMType vmType= getVMType();
		if (vmType != null)
			removeVMs(vmType);
	}
	
	private void removeVMs(IVMType vmType) {
		TableItem[] items= fVMList.getSelection();
		for (int i= 0; i < items.length; i++) {
			vmType.disposeVM(((IVM)items[i].getData()).getId());
			items[i].dispose();
		}
	}
		
	// editing
	private void editVM() {
		TableItem[] selection= fVMList.getSelection();
		// assume it's length one, otherwise this will not be called
		IVM vm= (IVM)selection[0].getData();
		editVM(selection[0], vm);
	}
	
	private void editVM(TableItem item, IVM vm) {
		EditVMDialog dialog= new EditVMDialog(getShell(), vm);
		dialog.setTitle("Edit VM");
		dialog.setInstallLocation(vm.getInstallLocation());
		dialog.setVMName(vm.getName());
		if (dialog.open() != dialog.OK)
			return;
		vm.setInstallLocation(dialog.getInstallLocation());
		vm.setName(dialog.getVMName());
		item.setText(vm.getName());
	}

	public boolean performOk() {
		try {
			JavaRuntime.saveVMConfiguration();
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
		fAddButton.setEnabled(getVMType() != null);
		int selectionCount= fVMList.getSelectionCount();
		fEditButton.setEnabled(selectionCount == 1);
		fDefaultCheckbox.setEnabled(selectionCount == 1);
		fRemoveButton.setEnabled(selectionCount > 0);
	}
	
	private void setDefaultVM() {
		if (fDefaultCheckbox.getSelection()) {
			TableItem[] selection= fVMList.getSelection();
			// assume it's length one, otherwise this will not be called
			IVM vm= (IVM)selection[0].getData();
			JavaRuntime.setDefaultVM(vm);
		} else {
			JavaRuntime.setDefaultVM(null);
		}
	}

	private void updateDefaultVM() {
		TableItem[] selection= fVMList.getSelection();
		if (selection.length != 1) {
			fDefaultCheckbox.setSelection(false);
		
		} else {			
			IVM vm= (IVM)selection[0].getData();
			fDefaultCheckbox.setSelection(vm == JavaRuntime.getDefaultVM());
		}
	}
	
	private void selectVM(IVM vm) {
		if (vm == null)
			return;
		IVMType vmType= vm.getVMType();
		selectVMType(vmType);
		TableItem[] items= fVMList.getItems();
		for (int i= 0; i < items.length; i++) {
			if (items[i].getData() == vm) {
				fVMList.setSelection(i);
				vmSelectionChanged();
			}
		}
	}
	
	private void selectVMType(IVMType vmType) {
		for (int i= 0; i < fVMTypes.length; i++) {
			if (fVMTypes[i] == vmType) {
				fVMTypeCombo.select(i);
				vmTypeChanged(vmType);
				return;
			}
		}

	}
}
