package org.eclipse.jdt.internal.ui.refactoring;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;

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

import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
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
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.structure.ModifyParametersRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IMultiRenameRefactoring;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.SWTUtil;

public class ModifyParametersInputPage extends UserInputWizardPage {

	public static final String PAGE_NAME= "ModifyParametersInputPage"; //$NON-NLS-1$
	private static final String[] PROPERTIES= {"type", "new"}; //$NON-NLS-2$ //$NON-NLS-1$
	private static final int TYPE_PROP= 0; 
	private static final int NEWNAME_PROP= 1;
	
	private static final int ROW_COUNT= 10; 
	
	private Button fUpButton;
	private Button fDownButton;
	private Label fSignaturePreview;
	private TableViewer fTableViewer;
    private Button fEditButton;
	
	public ModifyParametersInputPage() {
		super(PAGE_NAME, true);
		setMessage(RefactoringMessages.getString("ModifyParametersInputPage.new_order")); //$NON-NLS-1$
	}
	
	public void createControl(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout((new GridLayout()));
		
		createParameterTableComposite(composite);
		
		Label label= new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
		label.setLayoutData((new GridData(GridData.FILL_HORIZONTAL)));
		
		fSignaturePreview= new Label(composite, SWT.WRAP);
		GridData gl= new GridData(GridData.FILL_BOTH);
		gl.widthHint= convertWidthInCharsToPixels(50);
		fSignaturePreview.setLayoutData(gl);
		updateSignaturePreview();
		
		setControl(composite);
		WorkbenchHelp.setHelp(composite, IJavaHelpContextIds.MODIFY_PARAMETERS_WIZARD_PAGE);
	}

	private void createParameterTableComposite(Composite composite) {
		Composite subComposite= new Composite(composite, SWT.NONE);
		subComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		GridLayout subGrid= new GridLayout();
		subGrid.numColumns= 2;
		subGrid.marginWidth= 0;
		subGrid.marginHeight= 0;
		subComposite.setLayout(subGrid);
	
		Label tableLabel= new Label(subComposite, SWT.NONE);
		GridData labelGd= new GridData();
		labelGd.horizontalSpan= 2;
		tableLabel.setLayoutData(labelGd);
		tableLabel.setText(RefactoringMessages.getString("ModifyParametersInputPage.parameters")); //$NON-NLS-1$
		
		createParameterList(subComposite);
		createButtonComposite(subComposite);
	}

	private void createParameterList(Composite parent){
		Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		GridLayout gl= new GridLayout();
		gl.marginHeight= 0;
		gl.marginWidth= 0;
		composite.setLayout(gl);
		
		fTableViewer= new TableViewer(composite, SWT.MULTI | SWT.BORDER | SWT.HIDE_SELECTION |SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL);
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
		fTableViewer.setSorter(new ParameterInfoListSorter(getModifyParametersRefactoring()));
		
		ParameterInfo[] inputElems= createParameterInfos();
		fTableViewer.setInput(inputElems);
		fTableViewer.setSelection(new StructuredSelection(inputElems[0]));
		fTableViewer.addSelectionChangedListener(new ISelectionChangedListener(){
			public void selectionChanged(SelectionChangedEvent event){
				updateButtonsEnabledState();
			}
		});
	}

	private ParameterInfo[] createParameterInfos() {
		try {
			Map renamings= getModifyParametersRefactoring().getNewNames();
			String[] typeNames= getModifyParametersRefactoring().getMethod().getParameterTypes();
			String[] oldNames= getModifyParametersRefactoring().getMethod().getParameterNames();
			Collection result= new ArrayList(typeNames.length);
			
			for (int i= 0; i < oldNames.length; i++){
				result.add(new ParameterInfo(typeNames[i], oldNames[i], (String)renamings.get(oldNames[i])));
			}
			return ((ParameterInfo[]) result.toArray(new ParameterInfo[result.size()]));
		} catch(JavaModelException e) {
			ExceptionHandler.handle(e, RefactoringMessages.getString("ModifyParamatersInputPage.modify_Parameters"), RefactoringMessages.getString("ModifyParametersInputPage.exception")); //$NON-NLS-2$ //$NON-NLS-1$
			return new ParameterInfo[0];
		}		
	}

	private void addCellEditors(){
		Table table= fTableViewer.getTable();
		final CellEditor editors[]= new CellEditor[PROPERTIES.length];
		
		editors[TYPE_PROP]= new TextCellEditor(table);
		
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
	
	private ParameterInfo[] getSelectedItems(){
		ISelection selection= fTableViewer.getSelection();
		if (selection == null)
			return new ParameterInfo[0];
			
		if (! (selection instanceof IStructuredSelection))
			return new ParameterInfo[0];
			
		List selected= ((IStructuredSelection)selection).toList();
		return (ParameterInfo[]) selected.toArray(new ParameterInfo[selected.size()]);
	}
	
	private void createButtonComposite(Composite parent){
		Composite buttonComposite= new Composite(parent, SWT.NONE);
		buttonComposite.setLayoutData(new GridData(GridData.FILL_VERTICAL));
		GridLayout gl= new GridLayout();
		gl.marginHeight= 0;
		buttonComposite.setLayout(gl);

		fUpButton= createButton(buttonComposite, RefactoringMessages.getString("ModifyParametersInputPage.move_op"), true); //$NON-NLS-1$
		fDownButton= createButton(buttonComposite, RefactoringMessages.getString("ModifyParametersInputPage.move_down"), false); //$NON-NLS-1$
		fEditButton= createEditButton(buttonComposite);
		updateButtonsEnabledState();
	}

	private void updateButtonsEnabledState() {
		fDownButton.setEnabled(canMoveDown());
		fUpButton.setEnabled(canMoveUp());
		fEditButton.setEnabled(fTableViewer.getTable().getSelectionIndices().length == 1);
	}
	
	private Button createEditButton(Composite buttonComposite) {
		Button button= new Button(buttonComposite, SWT.PUSH);
		button.setText(RefactoringMessages.getString("ModifyParametersInputPage.editButton.text")); //$NON-NLS-1$
		button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		SWTUtil.setButtonDimensionHint(button);
		button.addSelectionListener(new SelectionAdapter(){
				public void widgetSelected(SelectionEvent e) {
					ISelection selection= fTableViewer.getSelection();
					try{
    					ParameterInfo[] selected= getSelectedItems();
    					Assert.isTrue(selected.length == 1);
    					ParameterInfo parameterInfo= selected[0];
    					String key= RefactoringMessages.getString("ModifyParametersInputPage.inputdialog.message"); //$NON-NLS-1$
    					String message= MessageFormat.format(key, new String[]{parameterInfo.oldName});
    					IInputValidator validator= createParameterNameValidator(parameterInfo.oldName);
    					InputDialog dialog= new InputDialog(getShell(), RefactoringMessages.getString("ModifyParametersInputPage.inputDialog.title"), message, parameterInfo.newName, validator); //$NON-NLS-1$
    					if (dialog.open() == InputDialog.CANCEL) {
    						fTableViewer.setSelection(selection);
    						return;
    					}	
    					parameterInfo.newName= dialog.getValue();
    					tableModified(getNewParameterNames());
    					fTableViewer.update(parameterInfo, new String[] { PROPERTIES[NEWNAME_PROP]});
					} finally {
						fTableViewer.refresh();
    					fTableViewer.getControl().setFocus();
    					fTableViewer.setSelection(selection);
					}
				}
			}	
		);	
		return button;	
	}
	
	private static IInputValidator createParameterNameValidator(final String oldName){
		return new IInputValidator(){
            public String isValid(String newText) {
            	if (newText.equals("")) //$NON-NLS-1$
            		return ""; //$NON-NLS-1$
            	if (newText.equals(oldName))
            		return ""; //$NON-NLS-1$
            	IStatus status= JavaConventions.validateFieldName(newText);
            	if (status.getSeverity() == IStatus.ERROR)
            		return status.getMessage();
                return null;
            }
		};
	}

	private Button createButton(Composite buttonComposite, String text, final boolean up) {
		Button button= new Button(buttonComposite, SWT.PUSH);
		button.setText(text);
		button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		SWTUtil.setButtonDimensionHint(button);
		button.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				ISelection savedSelection= fTableViewer.getSelection();
				if (savedSelection == null)
					return;
				if (getSelectedItems().length == 0)
					return;

				if (up)
					setNewParameterOrder(moveUp());
				else	
					setNewParameterOrder(moveDown());
					
				fTableViewer.refresh();
				fTableViewer.getControl().setFocus();
				fTableViewer.setSelection(savedSelection);
				tableModified(getNewParameterNames());
			}
		});
		return button;
	}
	
	private TableLayout createTableLayout(Table table) {
		TableLayout layout= new TableLayout();
		ColumnLayoutData[] columnLayoutData= new ColumnLayoutData[3];
		columnLayoutData[TYPE_PROP]= new ColumnWeightData(50, true);
		columnLayoutData[NEWNAME_PROP]= new ColumnWeightData(50, true);
		
		layout.addColumnData(columnLayoutData[TYPE_PROP]);
		layout.addColumnData(columnLayoutData[NEWNAME_PROP]);
		
		TableColumn tc;
		tc= new TableColumn(table, SWT.NONE, TYPE_PROP);
		tc.setResizable(columnLayoutData[TYPE_PROP].resizable);
		tc.setText(RefactoringMessages.getString("ModifyParametersInputPage.type")); //$NON-NLS-1$
		
		tc= new TableColumn(table, SWT.NONE, NEWNAME_PROP);
		tc.setResizable(columnLayoutData[NEWNAME_PROP].resizable);
		tc.setText(RefactoringMessages.getString("ModifyParametersInputPage.new_Name"));  //$NON-NLS-1$
		
		return layout;
	}

	private static List moveUp(List elements, List move) {
		List res= new ArrayList(elements.size());
		Object floating= null;
		for(Iterator iter= elements.iterator(); iter.hasNext();){
			Object curr= iter.next();
			if (move.contains(curr)) {
				res.add(curr);
			} else {
				if (floating != null)
					res.add(floating);
				floating= curr;
			}
		}
		if (floating != null)
			res.add(floating);
		return res;
	}
	
	private ParameterInfo[] getTableElements() {
		return (ParameterInfo[])fTableViewer.getInput();
	}
	
	private List getSortedTableElements() {
		List elems= Arrays.asList(getTableElements());
		Collections.sort(elems, new ParameterInfoComparator(getModifyParametersRefactoring()));
		return elems;
	}
	
	private String[] moveUp() {
		List toMoveUp= Arrays.asList(getSelectedItems());
		List elems= getSortedTableElements();
	
		List moved= moveUp(elems, toMoveUp);
	
		return getNewNames(moved);
	}
	
	private String[] moveDown() {
		List toMoveDown= Arrays.asList(getSelectedItems());
		List elems= getSortedTableElements();
		
		Collections.reverse(elems);
		List moved= moveUp(elems, toMoveDown);
		Collections.reverse(moved);
		
		return getNewNames(moved);
	}
	
	private String[] getNewNames(List moved) {
		return ParameterInfo.getNewNames((ParameterInfo[]) moved.toArray(new ParameterInfo[moved.size()]));
	}
	
	private boolean canMoveUp() {
		int[] indc= fTableViewer.getTable().getSelectionIndices();
		if (indc.length == 0)
			return false;
		for (int i= 0; i < indc.length; i++) {
			if (indc[i] != i) 
				return true;
		}
		return false;
	}
	
	private boolean canMoveDown() {
		int[] indc= fTableViewer.getTable().getSelectionIndices();
		if (indc.length == 0)
			return false;
		
		for (int i= indc.length - 1, k= getTableElements().length - 1; i >= 0 ; i--, k--) {
			if (indc[i] != k) 
				return true;
		}
		return false;
	}
	
	private void setNewParameterOrder(String[] newNames){
		getModifyParametersRefactoring().setNewParameterOrder(newNames);
	}
	
	private ModifyParametersRefactoring getModifyParametersRefactoring(){
		return	(ModifyParametersRefactoring)getRefactoring();
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
		IMultiRenameRefactoring ref= getModifyParametersRefactoring();
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
			ExceptionHandler.handle(e, RefactoringMessages.getString("ModifyParamatersInputPage.modify_Parameters"), RefactoringMessages.getString("ModifyParametersInputPage.exception1")); //$NON-NLS-2$ //$NON-NLS-1$
			setPageComplete(false);
			setErrorMessage(null);
		}
	}
	
	private void updateSignaturePreview() {
		try{
			fSignaturePreview.setText(RefactoringMessages.getString("ModifyParametersInputPage.method_Signature_Preview") + getModifyParametersRefactoring().getMethodSignaturePreview()); //$NON-NLS-1$
		} catch (JavaModelException e){
			ExceptionHandler.handle(e, RefactoringMessages.getString("ModifyParamatersInputPage.modify_Parameters"), RefactoringMessages.getString("ModifyParametersInputPage.exception")); //$NON-NLS-2$ //$NON-NLS-1$
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
		
		public static String[] getNewNames(ParameterInfo[] elems){
			String[] result= new String[elems.length];
			for (int i= 0; i < elems.length; i++) {
				result[i]= elems[i].newName;
			}
			return result;
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
			if (columnIndex == NEWNAME_PROP)
				return tuple.newName;
			Assert.isTrue(false);
			return ""; //$NON-NLS-1$
		}
	}
	
	private static class ParameterInfoComparator implements Comparator{
		
		private ModifyParametersRefactoring fRefactoring;
		
		ParameterInfoComparator(ModifyParametersRefactoring ref){
			fRefactoring= ref;
		}		
		
		public int compare(Object o1, Object o2) {
			ParameterInfo param1= (ParameterInfo)o1;
			ParameterInfo param2= (ParameterInfo)o2;
			return fRefactoring.getNewParameterPosition(param1.oldName) - fRefactoring.getNewParameterPosition(param2.oldName);
		}
	}
	
	private static class ParameterInfoListSorter extends ViewerSorter{
		
		private Comparator fParameterInfoComparator;
		
		ParameterInfoListSorter(ModifyParametersRefactoring ref){
			fParameterInfoComparator= new ParameterInfoComparator(ref);
		}
		
		public int compare(Viewer viewer, Object e1, Object e2) {
			return fParameterInfoComparator.compare(e1, e2);
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
			ParameterInfo parameterInfo= (ParameterInfo) data;
			if (property.equals(PROPERTIES[NEWNAME_PROP])) {
				parameterInfo.newName= (String) value;
				tableModified(getNewParameterNames());
				fTableViewer.update(parameterInfo, new String[] { property });
			}
		}
	};
}