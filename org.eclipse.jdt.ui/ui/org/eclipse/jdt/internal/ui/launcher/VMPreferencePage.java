/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.launcher;

import java.lang.reflect.InvocationTargetException;import java.util.Iterator;import org.eclipse.core.runtime.CoreException;import org.eclipse.core.runtime.IPath;import org.eclipse.core.runtime.IProgressMonitor;import org.eclipse.core.runtime.Path;import org.eclipse.jdt.core.JavaCore;import org.eclipse.jdt.internal.ui.preferences.ClasspathVariablesPreferencePage;import org.eclipse.jdt.internal.ui.util.ExceptionHandler;import org.eclipse.jdt.launching.IVMInstall;import org.eclipse.jdt.launching.IVMInstallType;import org.eclipse.jdt.launching.JavaRuntime;import org.eclipse.jdt.launching.LibraryLocation;import org.eclipse.jface.dialogs.ProgressMonitorDialog;import org.eclipse.jface.preference.PreferencePage;import org.eclipse.jface.viewers.CheckStateChangedEvent;import org.eclipse.jface.viewers.CheckboxTableViewer;import org.eclipse.jface.viewers.ColumnWeightData;import org.eclipse.jface.viewers.ICheckStateListener;import org.eclipse.jface.viewers.ISelectionChangedListener;import org.eclipse.jface.viewers.IStructuredSelection;import org.eclipse.jface.viewers.SelectionChangedEvent;import org.eclipse.jface.viewers.StructuredSelection;import org.eclipse.jface.viewers.TableLayout;import org.eclipse.swt.SWT;import org.eclipse.swt.layout.GridData;import org.eclipse.swt.layout.GridLayout;import org.eclipse.swt.widgets.Button;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Control;import org.eclipse.swt.widgets.Event;import org.eclipse.swt.widgets.Label;import org.eclipse.swt.widgets.Listener;import org.eclipse.swt.widgets.Table;import org.eclipse.swt.widgets.TableColumn;import org.eclipse.swt.widgets.TableItem;import org.eclipse.swt.widgets.Text;import org.eclipse.ui.IWorkbench;import org.eclipse.ui.IWorkbenchPreferencePage;import org.eclipse.ui.actions.WorkspaceModifyOperation;

/*
 * The page for setting the default java runtime preference.
 */
public class VMPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {	

	private CheckboxTableViewer fVMList;
	private Button fAddButton;
	private Button fRemoveButton;
	private Button fEditButton;
	
	protected Text fJreLib;	protected Text fJreSource;	protected Text fPkgRoot;		private IVMInstallType[] fVMTypes;
	
	private IPath[] fClasspathVariables= new IPath[3];

	public VMPreferencePage() {
		super();
		setDescription("Create, remove or edit JRE definitions.\nThe checked JRE will be used by default to build and run Java programs");
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
		layout.numColumns= 2;
		parent.setLayout(layout);		
				
		fVMList= new CheckboxTableViewer(parent, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		GridData gd= new GridData(GridData.FILL_BOTH);
		gd.widthHint= convertWidthInCharsToPixels(80);
		fVMList.getTable().setLayoutData(gd);
		fVMList.setLabelProvider(new VMLabelProvider());
		fVMList.setContentProvider(new VMContentProvider());	
		Table table= fVMList.getTable();
		
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		
		TableLayout tableLayout= new TableLayout();
		table.setLayout(tableLayout);
		
		TableColumn column1= table.getColumn(0);
		column1.setText("JRE Type");
		tableLayout.addColumnData(new ColumnWeightData(30));
	
		TableColumn column2= new TableColumn(table, SWT.NULL);
		column2.setText("Name");
		tableLayout.addColumnData(new ColumnWeightData(30));
		
		TableColumn column3= new TableColumn(table, SWT.NULL);		column3.setText("Location");		tableLayout.addColumnData(new ColumnWeightData(50));				fVMList.setInput(JavaRuntime.getVMInstallTypes());	
		
		fVMList.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent evt) {
				vmSelectionChanged();
			}
		});
		
		fVMList.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {				IVMInstall vm=  (IVMInstall)event.getElement();
				JavaRuntime.setDefaultVMInstall(vm);				updateDefaultVM();
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
		});				Composite jreVarsContainer= new Composite(parent, SWT.NULL);		jreVarsContainer.setLayoutData(new GridData(GridData.FILL_BOTH));		GridLayout jreLayout= new GridLayout();		jreLayout.numColumns= 2;		jreVarsContainer.setLayout(jreLayout);
						
		Label l= new Label(jreVarsContainer, SWT.NULL);		l.setText(ClasspathVariablesPreferencePage.JRELIB_VARIABLE);		l.setLayoutData(new GridData());				fJreLib= new Text(jreVarsContainer, SWT.READ_ONLY | SWT.BORDER);		fJreLib.setLayoutData(new GridData(gd.FILL_HORIZONTAL));		 		l= new Label(jreVarsContainer, SWT.NULL);		l.setText(ClasspathVariablesPreferencePage.JRESRC_VARIABLE);		l.setLayoutData(new GridData());				fJreSource= new Text(jreVarsContainer, SWT.READ_ONLY | SWT.BORDER);		fJreSource.setLayoutData(new GridData(gd.FILL_HORIZONTAL));		l= new Label(jreVarsContainer, SWT.NULL);		l.setText(ClasspathVariablesPreferencePage.JRESRCROOT_VARIABLE);		l.setLayoutData(new GridData());				fPkgRoot= new Text(jreVarsContainer, SWT.READ_ONLY | SWT.BORDER);		fPkgRoot.setLayoutData(new GridData(gd.FILL_HORIZONTAL));				return parent;
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
		dialog.setTitle("Add JRE");
		if (dialog.open() != dialog.OK)
			return;
		fVMList.refresh();		updateJREVariables(JavaRuntime.getDefaultVMInstall());
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
		dialog.setTitle("Edit JRE");
		if (dialog.open() != dialog.OK)
			return;
		fVMList.refresh(vm);		if (JavaRuntime.getDefaultVMInstall() == vm)			updateJREVariables(vm);
	}

	public boolean performOk() {
		try {
			JavaRuntime.saveVMConfiguration();
			setClassPathVariables();
		} catch (CoreException e) {
			ExceptionHandler.handle(e, "JRE Configuration", "An exception occurred while saving configuration data");
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
	
	private void updateDefaultVM() {		IVMInstall defaultVM= JavaRuntime.getDefaultVMInstall();		if (defaultVM != null) {
			fVMList.setCheckedElements(new Object[] { defaultVM });		} else {			fVMList.setCheckedElements(new Object[0]);		}		updateJREVariables(defaultVM);	}		private void updateJREVariables(IVMInstall defaultVM) {		String jreLib= "";		String jreSrc= "";		String pkgPath= "";		if (defaultVM != null) {			LibraryLocation location= defaultVM.getLibraryLocation();			if (location == null)				location= defaultVM.getVMInstallType().getDefaultLibraryLocation(defaultVM.getInstallLocation());			jreLib= location.getSystemLibrary().getAbsolutePath();			jreSrc= location.getSystemLibrarySource().getAbsolutePath();			pkgPath= location.getPackageRootPath().toString();		} else {			IPath lib= JavaCore.getClasspathVariable(ClasspathVariablesPreferencePage.JRELIB_VARIABLE);			if (lib != null)				jreLib= lib.toOSString();			IPath src= JavaCore.getClasspathVariable(ClasspathVariablesPreferencePage.JRESRC_VARIABLE);			if (src != null)				jreSrc= src.toOSString();			IPath pkgRoot= JavaCore.getClasspathVariable(ClasspathVariablesPreferencePage.JRESRCROOT_VARIABLE);			if (pkgRoot != null)				pkgPath= pkgRoot.toOSString();		}		fJreLib.setText(jreLib);		fJreSource.setText(jreSrc);		fPkgRoot.setText(pkgPath);	}
	
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
