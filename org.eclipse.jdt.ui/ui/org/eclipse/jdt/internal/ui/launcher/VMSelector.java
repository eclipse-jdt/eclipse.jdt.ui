/* * (c) Copyright IBM Corp. 2000, 2001. * All Rights Reserved. */package org.eclipse.jdt.internal.ui.launcher;import java.util.ArrayList;import java.util.List;import org.eclipse.core.runtime.CoreException;import org.eclipse.jdt.core.IJavaProject;import org.eclipse.jdt.launching.IVMInstall;import org.eclipse.jdt.launching.JavaRuntime;import org.eclipse.jface.viewers.CheckStateChangedEvent;import org.eclipse.jface.viewers.CheckboxTableViewer;import org.eclipse.jface.viewers.ColumnWeightData;import org.eclipse.jface.viewers.ICheckStateListener;import org.eclipse.jface.viewers.ISelection;import org.eclipse.jface.viewers.ISelectionChangedListener;import org.eclipse.jface.viewers.ISelectionProvider;import org.eclipse.jface.viewers.IStructuredSelection;import org.eclipse.jface.viewers.SelectionChangedEvent;import org.eclipse.jface.viewers.StructuredSelection;import org.eclipse.jface.viewers.TableLayout;import org.eclipse.swt.SWT;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Control;import org.eclipse.swt.widgets.Table;import org.eclipse.swt.widgets.TableColumn;

public class VMSelector implements ISelectionProvider {
	private CheckboxTableViewer fVMList;	private List fListeners;		public VMSelector() {		fListeners= new ArrayList();	}
	
	protected Control createContents(Composite ancestor) {		

		fVMList= new CheckboxTableViewer(ancestor, SWT.BORDER | SWT.SINGLE | SWT.HIDE_SELECTION);		fVMList.setLabelProvider(new VMLabelProvider());		fVMList.setContentProvider(new VMContentProvider());			Table table= fVMList.getTable();				fVMList.addCheckStateListener(new ICheckStateListener() {			public void checkStateChanged(CheckStateChangedEvent event) {				IVMInstall vm=  (IVMInstall)event.getElement();				if (event.getChecked())					fVMList.setCheckedElements(new Object[] { vm });				fireSelectedVMChanged(vm);			}		});		table.setHeaderVisible(true);		table.setLinesVisible(true);				TableLayout tableLayout= new TableLayout();		table.setLayout(tableLayout);				TableColumn column1= table.getColumn(0);		column1.setText("JRE Type");		tableLayout.addColumnData(new ColumnWeightData(30));			TableColumn column2= new TableColumn(table, SWT.NULL);		column2.setText("Name");		tableLayout.addColumnData(new ColumnWeightData(30));				TableColumn column3= new TableColumn(table, SWT.NULL);		column3.setText("Location");		tableLayout.addColumnData(new ColumnWeightData(50));				fVMList.setInput(JavaRuntime.getVMInstallTypes());		
		return table;
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
			fVMList.setCheckedElements(new Object[] { vm });
	}
	
	public IVMInstall getSelectedVM() {
		Object[] selection= fVMList.getCheckedElements();
		if (selection == null || selection.length != 1)
			return null;
		return (IVMInstall)selection[0];
	}
	
	public void addSelectionChangedListener(ISelectionChangedListener l) {
		fListeners.add(l);
	}
	
	public void removeSelectionChangedListener(ISelectionChangedListener l) {
		fListeners.remove(l);
	}		private void fireSelectedVMChanged(IVMInstall vmInstall) {		ISelection selection= null;		if (vmInstall != null)			selection= new StructuredSelection(vmInstall);		else 			selection= new StructuredSelection();		for (int i= 0; i < fListeners.size(); i++) {			ISelectionChangedListener l= (ISelectionChangedListener)fListeners.get(i);			l.selectionChanged(new SelectionChangedEvent(this, selection));		}	}
	
	public boolean validateSelection(ISelection sel) {
		if (sel instanceof IStructuredSelection) {
			IStructuredSelection selection= (IStructuredSelection)sel;
			if (selection.size() == 0)
				return true;
			return selection.size() == 1 && selection.getFirstElement() instanceof IVMInstall;
		} else {
			return false;
		}
	}		public ISelection getSelection() {		return new StructuredSelection(getSelectedVM());	}	/**	 * does nothing	 */	public void setSelection(ISelection selection) {	}
}
