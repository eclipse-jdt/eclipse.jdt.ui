/* * (c) Copyright IBM Corp. 2000, 2001. * All Rights Reserved. */package org.eclipse.jdt.internal.ui.launcher;import org.eclipse.core.runtime.CoreException;import org.eclipse.jdt.core.IJavaProject;import org.eclipse.jdt.launching.IVMInstall;import org.eclipse.jdt.launching.JavaRuntime;import org.eclipse.jface.viewers.ColumnWeightData;import org.eclipse.jface.viewers.ISelection;import org.eclipse.jface.viewers.ISelectionChangedListener;import org.eclipse.jface.viewers.IStructuredSelection;import org.eclipse.jface.viewers.StructuredSelection;import org.eclipse.jface.viewers.TableLayout;import org.eclipse.jface.viewers.TableViewer;import org.eclipse.swt.SWT;import org.eclipse.swt.layout.GridData;import org.eclipse.swt.layout.GridLayout;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Control;import org.eclipse.swt.widgets.Table;import org.eclipse.swt.widgets.TableColumn;

public class VMSelector {
	private TableViewer fVMList;
	
	protected Control createContents(Composite ancestor) {		
		Composite parent= new Composite(ancestor, SWT.NULL);
		GridLayout layout= new GridLayout();
		layout.numColumns= 1;
		parent.setLayout(layout);

				fVMList= new TableViewer(parent, SWT.BORDER | SWT.MULTI);		GridData gd= new GridData(GridData.FILL_BOTH);		fVMList.getTable().setLayoutData(gd);		fVMList.setLabelProvider(new VMLabelProvider());		fVMList.setContentProvider(new VMContentProvider());			Table table= fVMList.getTable();				table.setHeaderVisible(true);		table.setLinesVisible(true);				TableLayout tableLayout= new TableLayout();		table.setLayout(tableLayout);				TableColumn column1= new TableColumn(table, SWT.NULL);		column1.setText("VM Install Type");		tableLayout.addColumnData(new ColumnWeightData(50));			TableColumn column2= new TableColumn(table, SWT.NULL);		column2.setText("Name");		tableLayout.addColumnData(new ColumnWeightData(50));				fVMList.setInput(JavaRuntime.getVMInstallTypes());		
		return parent;
	}

		
	/**
	 * Must be called after createContents
	 */ 
	public void initFromProject(IJavaProject project) {
		IVMInstall vm= null;
		try {
			selectVM(JavaRuntime.getVMInstall(project));
		} catch (CoreException e) {
		}
	}
	
	public void selectVM(IVMInstall vm) {
		if (vm != null)
			fVMList.setSelection(new StructuredSelection(vm));
	}
	
	public IVMInstall getSelectedVM() {
		IStructuredSelection selection= (IStructuredSelection)fVMList.getSelection();
		if (selection.isEmpty())
			return null;
		Object o= selection.getFirstElement();
		return (IVMInstall)o;
	}
	
	public void addSelectionChangedListener(ISelectionChangedListener l) {
		fVMList.addSelectionChangedListener(l);
	}
	
	public void removeSelectionChangedListener(ISelectionChangedListener l) {
		fVMList.removeSelectionChangedListener(l);
	}
	
	public boolean validateSelection(ISelection sel) {
		if (sel instanceof IStructuredSelection) {
			IStructuredSelection selection= (IStructuredSelection)sel;
			if (selection.size() == 0)
				return true;
			return selection.size() == 1 && selection.getFirstElement() instanceof IVMInstall;
		} else {
			return false;
		}
	}

}
