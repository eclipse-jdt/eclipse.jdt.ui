package org.eclipse.jdt.internal.ui.refactoring;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLayoutData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.structure.ReorderRenameParameterWrapperRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IMultiRenameRefactoring;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public class ReorderParametersInputPage extends UserInputWizardPage {

	public static final String PAGE_NAME= "ReorderParametersInputPage"; //$NON-NLS-1$
	
	private static final String[] PROPERTIES= {"type", "old", "new"}; //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$
	private static final int TYPE_PROP= 0; 
	private static final int OLDNAME_PROP= 1; 
	private static final int NEWNAME_PROP= 2;
	
	private static final int ROW_COUNT= 5; 
	
	private Button fUpButton;
	private Button fDownButton;
	private Label fSignaturePreview;
	private TableViewer fTableViewer;
	
	public ReorderParametersInputPage() {
		super(PAGE_NAME, true);
		setMessage("Specify the new order of parameters and/or their new names");
	}
	
	public void createControl(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout((new GridLayout()));
		
		createParameterTableComposite(composite);
		
		Label label= new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
		label.setLayoutData((new GridData(GridData.FILL_HORIZONTAL)));
		
		fSignaturePreview= new Label(composite, SWT.NONE);
		fSignaturePreview.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		updateSignaturePreview();
		
		setControl(composite);
	}

	private void createParameterTableComposite(Composite composite) {
		Composite subComposite= new Composite(composite, SWT.NONE);
		subComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		GridLayout subGrid= new GridLayout();
		subGrid.numColumns= 2;
		subComposite.setLayout(subGrid);
		
		createParameterList(subComposite);
		createButtonComposite(subComposite);
	}

	private void createParameterList(Composite parent){
		fTableViewer= new TableViewer(parent, SWT.SINGLE | SWT.BORDER | SWT.HIDE_SELECTION |SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL);
		fTableViewer.setUseHashlookup(true);
		fTableViewer.getTable().setHeaderVisible(true);
		fTableViewer.getTable().setLinesVisible(true);
		GridData gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint= fTableViewer.getTable().getGridLineWidth() + fTableViewer.getTable().getItemHeight() * ROW_COUNT;
		fTableViewer.getControl().setLayoutData(gd);
		fTableViewer.getTable().setLayout(createTableLayout(fTableViewer.getTable()));
		
		addCellEditors();
		
		fTableViewer.setContentProvider(new ParameterInfoContentProvider());
		fTableViewer.setLabelProvider(new ParameterInfoLabelProvider());
		fTableViewer.setSorter(new ParameterInfoListSorter(getReorderRenameParameterWrapperRefactoring()));
		
		fTableViewer.setInput(createParameterInfos());
		fTableViewer.addSelectionChangedListener(new ISelectionChangedListener(){
			public void selectionChanged(SelectionChangedEvent event){
				ParameterInfo selected= getSelectedItem();
				if (selected == null)
					return;
				if (doesMethodHaveOneParameter())
					return; //disabled anyway
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
	
	private ParameterInfo[] createParameterInfos() {
		try {
			Map renamings= getReorderRenameParameterWrapperRefactoring().getNewNames();
			String[] typeNames= getReorderRenameParameterWrapperRefactoring().getMethod().getParameterTypes();
			String[] oldNames= getReorderRenameParameterWrapperRefactoring().getMethod().getParameterNames();
			Collection result= new ArrayList(typeNames.length);
			
			for (int i= 0; i < oldNames.length; i++){
				result.add(new ParameterInfo(typeNames[i], oldNames[i], (String)renamings.get(oldNames[i])));
			}
			return ((ParameterInfo[]) result.toArray(new ParameterInfo[result.size()]));
		} catch(JavaModelException e) {
			ExceptionHandler.handle(e, "Rename Parameters", "Unexpected exception. See log for details.");
			return new ParameterInfo[0];
		}		
	}

	private void addCellEditors(){
		Table table= fTableViewer.getTable();
		final CellEditor editors[]= new CellEditor[PROPERTIES.length];
		
		editors[TYPE_PROP]= new TextCellEditor(table);
		editors[OLDNAME_PROP]= new TextCellEditor(table);
		
		class AutoApplyTextCellEditor extends TextCellEditor {
			public AutoApplyTextCellEditor(Composite parent) {
				super(parent);
			}
			public void fireApplyEditorValue() {
				super.fireApplyEditorValue();
			}
		};
		editors[NEWNAME_PROP]= new AutoApplyTextCellEditor(table);
		editors[NEWNAME_PROP].getControl().addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				((AutoApplyTextCellEditor)editors[NEWNAME_PROP]).fireApplyEditorValue();
			}
		});

		fTableViewer.setCellEditors(editors);
		fTableViewer.setColumnProperties(PROPERTIES);
		fTableViewer.setCellModifier(new ReorderParametersCellModifier());
	}
	
	private boolean isFirst(ParameterInfo selected){
		return getReorderRenameParameterWrapperRefactoring().getNewParameterPosition(selected.oldName) == 0;
	}
	
	private boolean isLast(ParameterInfo selected){
		return getReorderRenameParameterWrapperRefactoring().getNewParameterPosition(selected.oldName) == 
					(getReorderRenameParameterWrapperRefactoring().getParamaterPermutation().length - 1);
	}
	
	private ParameterInfo getSelectedItem(){
		ISelection delection= fTableViewer.getSelection();
		if (! (delection instanceof IStructuredSelection))
			return null;
		return (ParameterInfo)((IStructuredSelection)delection).getFirstElement();
	}
	
	private void createButtonComposite(Composite parent){
		Composite buttonComposite= new Composite(parent, SWT.NONE);
		buttonComposite.setLayoutData(new GridData(GridData.FILL_VERTICAL));
		buttonComposite.setLayout(new GridLayout());

		fUpButton= createButton(buttonComposite, "Move &Up", true);
		fDownButton= createButton(buttonComposite, "Move &Down", false);
		
		if (doesMethodHaveOneParameter()){
			fUpButton.setEnabled(false);
			fDownButton.setEnabled(false);
		}	
	}
	
	private boolean doesMethodHaveOneParameter(){
		return getReorderRenameParameterWrapperRefactoring().getMethod().getParameterTypes().length == 1;
	}

	private Button createButton(Composite buttonComposite, String text, final boolean up) {
		Button button= new Button(buttonComposite, SWT.PUSH);
		button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		button.setText(text);
		button.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				ISelection selection= fTableViewer.getSelection();
				if (selection == null)
					return;
				if (getSelectedItem() == null)
					return;	
				getReorderRenameParameterWrapperRefactoring().setNewParameterOrder(move(up, getSelectedItem()));
				fTableViewer.refresh();
				fTableViewer.getControl().setFocus();
				fTableViewer.setSelection(selection);
				tableModified(getNewParameterNames());
			}
		});
		return button;
	}
	
	private TableLayout createTableLayout(Table table) {
		TableLayout layout= new TableLayout();
		ColumnLayoutData[] columnLayoutData= new ColumnLayoutData[3];
		columnLayoutData[TYPE_PROP]= new ColumnWeightData(34);
		columnLayoutData[OLDNAME_PROP]= new ColumnWeightData(33);
		columnLayoutData[NEWNAME_PROP]= new ColumnWeightData(33);
		
		layout.addColumnData(columnLayoutData[0]);
		layout.addColumnData(columnLayoutData[1]);
		layout.addColumnData(columnLayoutData[2]);
		
		TableColumn tc;
		tc= new TableColumn(table, SWT.NONE, TYPE_PROP);
		tc.setResizable(columnLayoutData[TYPE_PROP].resizable);
		tc.setText("Parameter Type");
		
		tc= new TableColumn(table, SWT.NONE, OLDNAME_PROP);
		tc.setResizable(columnLayoutData[OLDNAME_PROP].resizable);
		tc.setText("Old Parameter Name"); 
		
		tc= new TableColumn(table, SWT.NONE, NEWNAME_PROP);
		tc.setResizable(columnLayoutData[NEWNAME_PROP].resizable);
		tc.setText("New Parameter Name"); 
		
		return layout;
	}
	
	private String[] move(boolean up, ParameterInfo element){
		if (up)
			return moveUp(element);
		else
			return moveDown(element);
	}
	
	private String[] moveUp(ParameterInfo element){
		int position= getReorderRenameParameterWrapperRefactoring().getNewParameterPosition(element.oldName);
		Assert.isTrue(position > 0);
		return swap(getReorderRenameParameterWrapperRefactoring().getNewParameterOrder(), position - 1, position);
	}
	
	private String[] moveDown(ParameterInfo element){
		int position= getReorderRenameParameterWrapperRefactoring().getNewParameterPosition(element.oldName);
		Assert.isTrue(position < getReorderRenameParameterWrapperRefactoring().getParamaterPermutation().length - 1);
		return swap(getReorderRenameParameterWrapperRefactoring().getNewParameterOrder(), position + 1, position);
	}
	
	private static String[] swap(String[] array, int p1, int p2){
		String temp= array[p1];
		array[p1]= array[p2];
		array[p2]= temp;
		return array;
	}
	
	private ReorderRenameParameterWrapperRefactoring getReorderRenameParameterWrapperRefactoring(){
		return	(ReorderRenameParameterWrapperRefactoring)getRefactoring();
	}
	
	private Map getNewParameterNames(){
		ParameterInfo[] input= (ParameterInfo[])fTableViewer.getInput();
		Map result= new HashMap();
		for (int i= 0; i < input.length; i++){
			result.put(input[i].oldName, input[i].newName);
		}	
		return result;
	}
	
	private RefactoringStatus validateTable(Map renamings) throws JavaModelException{
		IMultiRenameRefactoring ref= getReorderRenameParameterWrapperRefactoring();
		ref.setNewNames(renamings);
		return ref.checkNewNames();
	}
	
	private void tableModified(Map renamings) {
		updateErrorStatus(renamings);
		updateSignaturePreview();
	}
	
	private void updateErrorStatus(Map renamings){
		try{
			RefactoringStatus status= validateTable(renamings);
			getRefactoringWizard().setStatus(status);
			if (status != null && status.hasFatalError()) {
				setPageComplete(false);
				setErrorMessage(status.getFirstMessage(RefactoringStatus.FATAL));
			} else {
				setPageComplete(true);	
				setErrorMessage(null);
			}	
		} catch (JavaModelException e){
			ExceptionHandler.handle(e, "Exception", "Unexpected exception occurred. See log for details.");
			setPageComplete(false);
			setErrorMessage(null);
		}
	}
	
	private void updateSignaturePreview() {
		try{
			fSignaturePreview.setText("Method Signature Preview: " + getReorderRenameParameterWrapperRefactoring().getMethodSignaturePreview());
		} catch (JavaModelException e){
			ExceptionHandler.handle(e, "Reorder/rename parameters", "Unexpected exception. See log for details.");
		}	
	}
	
	//--- private classes

	private static class ParameterInfo {
		public String typeName;
		public String oldName;
		public String newName;
		
		ParameterInfo(String typeName, String oldName, String newName){
			this.typeName= typeName;
			this.oldName= oldName;
			this.newName= newName;
		}
	}
	
	private static class ParameterInfoContentProvider implements IStructuredContentProvider {
		
		public Object[] getElements(Object inputElement) {
			return (Object[])inputElement;
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
			if (columnIndex == TYPE_PROP)
				return Signature.toString(tuple.typeName);
			if (columnIndex == OLDNAME_PROP)
				return tuple.oldName;
			if (columnIndex == NEWNAME_PROP)
				return tuple.newName;
			Assert.isTrue(false);
			return "";
		}
	}
	
	private static class ParameterInfoListSorter extends ViewerSorter{
		
		private ReorderRenameParameterWrapperRefactoring fRefactoring;
		
		ParameterInfoListSorter(ReorderRenameParameterWrapperRefactoring ref){
			fRefactoring= ref;
		}
		
		public int compare(Viewer viewer, Object e1, Object e2) {
			ParameterInfo param1= (ParameterInfo)e1;
			ParameterInfo param2= (ParameterInfo)e2;
			return fRefactoring.getNewParameterPosition(param1.oldName) - fRefactoring.getNewParameterPosition(param2.oldName);
		}
	}
	
	private class ReorderParametersCellModifier implements ICellModifier {
		
		public boolean canModify(Object element, String property) {
			return (property.equals(PROPERTIES[NEWNAME_PROP]));
		}
		
		public Object getValue(Object element, String property) {
			if (! (element instanceof ParameterInfo))
				return null;
			if (property.equals(PROPERTIES[TYPE_PROP]))
				return ((ParameterInfo) element).typeName;	
			if (property.equals(PROPERTIES[OLDNAME_PROP]))
				return ((ParameterInfo) element).oldName;
			if (property.equals(PROPERTIES[NEWNAME_PROP]))
				return ((ParameterInfo) element).newName;
			Assert.isTrue(false);	
			return null;
		}
		
		public void modify(Object element, String property, Object value) {
			if (! (element instanceof TableItem)) 
				return;
			Object data= ((TableItem) element).getData();
			if (! (data instanceof ParameterInfo)) 
				return;
			ParameterInfo paremeterInfo= (ParameterInfo) data;
			if (property.equals(PROPERTIES[NEWNAME_PROP])) {
				paremeterInfo.newName= (String) value;
				tableModified(getNewParameterNames());
				fTableViewer.update(paremeterInfo, new String[] { property });
			}
		}
	};
}
