/* * (c) Copyright IBM Corp. 2000, 2001. * All Rights Reserved. */package org.eclipse.jdt.internal.ui.launcher;import org.eclipse.jdt.launching.IVMInstall;import org.eclipse.jdt.launching.JavaRuntime;import org.eclipse.jface.viewers.ColumnWeightData;import org.eclipse.jface.viewers.ISelection;import org.eclipse.jface.viewers.ISelectionChangedListener;import org.eclipse.jface.viewers.IStructuredSelection;import org.eclipse.jface.viewers.StructuredSelection;import org.eclipse.jface.viewers.TableLayout;import org.eclipse.jface.viewers.TableViewer;import org.eclipse.jface.viewers.Viewer;import org.eclipse.jface.viewers.ViewerSorter;import org.eclipse.swt.SWT;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Control;import org.eclipse.swt.widgets.Table;import org.eclipse.swt.widgets.TableColumn;

public class VMSelector {
	private TableViewer fVMList;		public VMSelector() {	}
	
	protected Control createContents(Composite ancestor) {		

		fVMList= new TableViewer(ancestor, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);		fVMList.setLabelProvider(new VMLabelProvider());		fVMList.setContentProvider(new VMContentProvider());			Table table= fVMList.getTable();				fVMList.setSorter(new ViewerSorter() {			public int compare(Viewer viewer, Object e1, Object e2) {				if ((e1 instanceof IVMInstall) && (e2 instanceof IVMInstall)) {					IVMInstall left= (IVMInstall)e1;					IVMInstall right= (IVMInstall)e2;					String leftType= left.getVMInstallType().getName();					String rightType= right.getVMInstallType().getName();					int res= leftType.compareToIgnoreCase(rightType);					if (res != 0)						return res;					return left.getName().compareToIgnoreCase(right.getName());				}				return super.compare(viewer, e1, e2);			}						public boolean isSorterProperty(Object element, String property) {				return true;			}		});		table.setHeaderVisible(true);		table.setLinesVisible(true);				TableLayout tableLayout= new TableLayout();		table.setLayout(tableLayout);				TableColumn column1= new TableColumn(table, SWT.NULL);		column1.setText(LauncherMessages.getString("vmSelector.jreType")); //$NON-NLS-1$		tableLayout.addColumnData(new ColumnWeightData(30));			TableColumn column2= new TableColumn(table, SWT.NULL);		column2.setText(LauncherMessages.getString("vmSelector.jreName")); //$NON-NLS-1$		tableLayout.addColumnData(new ColumnWeightData(30));				TableColumn column3= new TableColumn(table, SWT.NULL);		column3.setText(LauncherMessages.getString("vmSelector.jreLocation")); //$NON-NLS-1$		tableLayout.addColumnData(new ColumnWeightData(50));				fVMList.setInput(JavaRuntime.getVMInstallTypes());		
		return table;
	}		
	public void selectVM(IVMInstall vm) {
		if (vm != null)
			fVMList.setSelection(new StructuredSelection(vm));
	}
	
	public IVMInstall getSelectedVM() {
		IStructuredSelection selection= (IStructuredSelection)fVMList.getSelection();
		if (selection == null || selection.size() != 1)
			return null;
		return (IVMInstall)selection.getFirstElement();
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
	}	}
