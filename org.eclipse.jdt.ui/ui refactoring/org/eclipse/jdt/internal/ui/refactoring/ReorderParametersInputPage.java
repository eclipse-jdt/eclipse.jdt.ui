package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import org.eclipse.jface.viewers.ColumnLayoutData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.structure.ReorderParametersRefactoring;
import org.eclipse.jdt.internal.ui.JavaPlugin;

public class ReorderParametersInputPage extends UserInputWizardPage {

	public static final String PAGE_NAME= "ReorderParametersInputPage"; //$NON-NLS-1$
	
	private Button fUpButton;
	private Button fDownButton;
	private TableViewer fTableViewer;
	
	public ReorderParametersInputPage() {
		super(PAGE_NAME, true);
		setMessage("Specify the new order of parameters");
	}
	
	public void createControl(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		GridLayout grid= new GridLayout();
		grid.numColumns= 2;
		composite.setLayout(grid);
		
		createParameterList(composite);
		createButtonComposite(composite);
		setControl(composite);
	}
	
	private void createParameterList(Composite parent){
		fTableViewer= new TableViewer(parent, SWT.SINGLE | SWT.BORDER | SWT.HIDE_SELECTION |SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL);
		fTableViewer.setUseHashlookup(true);
		fTableViewer.getTable().setHeaderVisible(true);
		fTableViewer.getTable().setLinesVisible(true);
		GridData gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint= fTableViewer.getTable().getGridLineWidth() + fTableViewer.getTable().getItemHeight() * 5;
		fTableViewer.getControl().setLayoutData(gd);
		fTableViewer.getTable().setLayout(createTableLayout(fTableViewer.getTable()));
		
		fTableViewer.setContentProvider(new ParameterInfoContentProvider(getReorderParametersRefactoring()));
		fTableViewer.setLabelProvider(new ParameterInfoLabelProvider());
		fTableViewer.setSorter(new ParameterInfoListSorter(getReorderParametersRefactoring()));
		
		fTableViewer.setInput(getReorderParametersRefactoring().getMethod());
		fTableViewer.addSelectionChangedListener(new ISelectionChangedListener(){
			public void selectionChanged(SelectionChangedEvent event){
				ParameterInfo selected= getSelectedItem();
				if (selected == null)
					 return;
				if (isFirst(selected)){
					fUpButton.setEnabled(false);
					fDownButton.setEnabled(true);
				} else if (isLast(selected)){
					fUpButton.setEnabled(true);
					fDownButton.setEnabled(false);
				} else{
					fUpButton.setEnabled(true);
					fDownButton.setEnabled(true);
				}	
			}
		});
	}
	
	private boolean isFirst(ParameterInfo selected){
		return getReorderParametersRefactoring().getNewParameterPosition(selected.name) == 0;
	}
	
	private boolean isLast(ParameterInfo selected){
		return getReorderParametersRefactoring().getNewParameterPosition(selected.name) == 
					(getReorderParametersRefactoring().getParamaterPermutation().length - 1);
	}
	
	private ParameterInfo getSelectedItem(){
		ISelection delection= fTableViewer.getSelection();
		if (! (delection instanceof IStructuredSelection))
			return null;
		return (ParameterInfo)((IStructuredSelection)delection).getFirstElement();
	}
	
	private void createButtonComposite(Composite parent){
		Composite buttonComposite= new Composite(parent, SWT.NONE);
		buttonComposite.setLayoutData(new GridData());
		buttonComposite.setLayout(new GridLayout());

		fUpButton= createButton(buttonComposite, "Move &Up", true);
		fDownButton= createButton(buttonComposite, "Move &Down", false);
	}

	private Button createButton(Composite buttonComposite, String text, final boolean up) {
		Button button= new Button(buttonComposite, SWT.PUSH);
		button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.CENTER));
		button.setText(text);
		button.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				ISelection selection= fTableViewer.getSelection();
				if (selection == null)
					return;
				if (getSelectedItem() == null)
					return;	
				getReorderParametersRefactoring().setNewParameterOrder(move(up, getSelectedItem()));
				fTableViewer.refresh();
				fTableViewer.getControl().setFocus();
				fTableViewer.setSelection(selection);
			}
		});
		return button;
	}
	
	private TableLayout createTableLayout(Table table) {
		TableLayout layout= new TableLayout();
		ColumnLayoutData[] columnLayoutData= new ColumnLayoutData[2];
		columnLayoutData[0]= new ColumnWeightData(50);
		columnLayoutData[1]= new ColumnWeightData(50);
		
		layout.addColumnData(columnLayoutData[0]);
		layout.addColumnData(columnLayoutData[1]);
		
		TableColumn tc= new TableColumn(table, SWT.NONE, 0);
		tc.setResizable(columnLayoutData[0].resizable);
		tc.setText("Parameter type");
		
		tc= new TableColumn(table, SWT.NONE, 1);
		tc.setResizable(columnLayoutData[1].resizable);
		tc.setText("Parameter name"); 
		return layout;
	}
	
	private String[] move(boolean up, ParameterInfo element){
		if (up)
			return moveUp(element);
		else
			return moveDown(element);
	}
	
	private String[] moveUp(ParameterInfo element){
		int position= getReorderParametersRefactoring().getNewParameterPosition(element.name);
		Assert.isTrue(position > 0);
		return swap(getReorderParametersRefactoring().getNewParameterOrder(), position - 1, position);
	}
	
	private String[] moveDown(ParameterInfo element){
		int position= getReorderParametersRefactoring().getNewParameterPosition(element.name);
		Assert.isTrue(position < getReorderParametersRefactoring().getParamaterPermutation().length - 1);
		return swap(getReorderParametersRefactoring().getNewParameterOrder(), position + 1, position);
	}
	
	private static String[] swap(String[] array, int p1, int p2){
		String temp= array[p1];
		array[p1]= array[p2];
		array[p2]= temp;
		return array;
	}
	
	private ReorderParametersRefactoring getReorderParametersRefactoring(){
		return (ReorderParametersRefactoring)getRefactoring();
	}
	
	//--- private classes

	private static class ParameterInfo {
		public String typeName;
		public String name;
		
		ParameterInfo(String typeName, String name){
			this.typeName= typeName;
			this.name= name;
		}
	}
	
	private static class ParameterInfoContentProvider implements IStructuredContentProvider {
		
		private ReorderParametersRefactoring fRefactoring;
		private Object[] fElements; //always return the same thing
		
		ParameterInfoContentProvider(ReorderParametersRefactoring reorderParamsRefactoring){
			fRefactoring= reorderParamsRefactoring;
		}
		
		public Object[] getElements(Object inputElement) {
			if (fElements != null)
				return fElements;
			try{
				IMethod method= (IMethod)inputElement;
				ParameterInfo[] result= new ParameterInfo[method.getNumberOfParameters()];
				String[] paramNames= method.getParameterNames();
				String[] paramTypeNames= method.getParameterTypes();
				for (int i= 0; i < result.length; i++) {
					result[i]= new ParameterInfo(paramTypeNames[i], paramNames[i]);
				}
				fElements= result;
				return result;
			} catch (JavaModelException e){
				JavaPlugin.log(e); //too heavy to show a dialog?
				return null;
			}
		}

		public void dispose() {
			// do nothing
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// do nothing
		}
	}
	
	private static class ParameterInfoLabelProvider extends LabelProvider implements ITableLabelProvider {
		public Image getColumnImage(Object element, int columnIndex){
			return null;
		}
		
		public String getColumnText(Object element, int columnIndex){
			ParameterInfo tuple= (ParameterInfo)element;
			if (columnIndex == 0)
				return Signature.toString(tuple.typeName);
			if (columnIndex == 1)
				return tuple.name;
			Assert.isTrue(false);
			return "";
		}
	}
	
	private static class ParameterInfoListSorter extends ViewerSorter{
		
		private ReorderParametersRefactoring fRefactoring;
		
		ParameterInfoListSorter(ReorderParametersRefactoring ref){
			fRefactoring= ref;
		}
		
		public int compare(Viewer viewer, Object e1, Object e2) {
			ParameterInfo param1= (ParameterInfo)e1;
			ParameterInfo param2= (ParameterInfo)e2;
			return fRefactoring.getNewParameterPosition(param1.name) - fRefactoring.getNewParameterPosition(param2.name);
		}
	}
}
