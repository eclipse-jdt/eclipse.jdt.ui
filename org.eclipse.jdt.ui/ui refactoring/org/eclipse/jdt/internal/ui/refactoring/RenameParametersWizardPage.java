/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.core.refactoring.tagging.IMultiRenameRefactoring;
import org.eclipse.jdt.internal.core.refactoring.tagging.IReferenceUpdatingRefactoring;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLayoutData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
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
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.help.DialogPageContextComputer;
import org.eclipse.ui.help.WorkbenchHelp;

public class RenameParametersWizardPage extends UserInputWizardPage {

	public static final String PAGE_NAME= "RenameParametersInputPage"; //$NON-NLS-1$
	
	private static final String[] PROPERTIES= {"old", "new"}; //$NON-NLS-2$ //$NON-NLS-1$
	private static final int OLDNAME_PROP= 0; 
	private static final int NEWNAME_PROP= 1; 
	
	private TableViewer fViewer;
	private boolean fAlreadyShown= false;
	
	public RenameParametersWizardPage(boolean isLastUserPage) {
		super(PAGE_NAME, isLastUserPage);
	}
	
	private IMultiRenameRefactoring getMultiRenameRefactoring(){
		return (IMultiRenameRefactoring)getRefactoring();
	}			
	
	/* non java-doc
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		Table table= createTableComposite(parent);
		
		fViewer= new TableViewer(table);
		fViewer.setUseHashlookup(true);
		
		final CellEditor editors[]= new CellEditor[2];
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

		fViewer.setCellEditors(editors);
		
		fViewer.setColumnProperties(PROPERTIES);
		fViewer.setCellModifier(new CellModifier());

		fViewer.setContentProvider(new ParameterPairContentProvider());
		fViewer.setLabelProvider(new ParameterPairLabelProvider());
		
		fViewer.setInput(createNamePairs());
		
		WorkbenchHelp.setHelp(getControl(), new DialogPageContextComputer(this, IJavaHelpContextIds.RENAME_PARAMS_WIZARD_PAGE));
	}
	
	/* (non-Javadoc)
	 * Method declared in IDialogPage
	 */
	public void dispose() {
		fViewer= null;
	}
	
	/* (non-Javadoc)
	 * Method declared in WizardPage
	 */
	public void setVisible(boolean visible) {
		if (visible) {
			if (fAlreadyShown)
				tableModified(getNewParameterNames());
			else{
				setPageComplete(false);
				setErrorMessage(null);	
			}	
			fAlreadyShown= true;
		}
		super.setVisible(visible);
	}
	
	private RefactoringStatus validateTable(Map renamings) throws JavaModelException{
		IMultiRenameRefactoring ref= getMultiRenameRefactoring();
		ref.setNewNames(renamings);
		return ref.checkNewNames();
	}
	
	private void tableModified(Map renamings) {
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
	
	private Map getNewParameterNames(){
		ParameterNamePair[] input= (ParameterNamePair[])fViewer.getInput();
		Map result= new HashMap();
		for (int i= 0; i < input.length; i++){
			result.put(input[i].oldName, input[i].newName);
		}	
		return result;
	}
	
	private Table createTableComposite(Composite parent){
		Composite c= new Composite(parent, SWT.NONE);
		c.setLayout(new GridLayout());
		c.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Table table= addTable(c);
		addCheckBox(c);
		setControl(c);
		return table;
	}
	
	private Table addTable(Composite c){
		Table table= new Table(c, SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.FULL_SELECTION | SWT.HIDE_SELECTION | SWT.BORDER);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		GridData gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint= table.getGridLineWidth() + table.getItemHeight() * 5;
		table.setLayoutData(gd);
		table.setLayout(createTableLayout(table));
		return table;
	}

	private void addCheckBox(Composite c) {
		if (! (getRefactoring() instanceof IReferenceUpdatingRefactoring))
			return;
		
		final IReferenceUpdatingRefactoring ref= (IReferenceUpdatingRefactoring)getRefactoring();
		if (! ref.canEnableUpdateReferences())
			return;
			
		final Button checkBox= new Button(c, SWT.CHECK);
		checkBox.setText("Update references");
		checkBox.setSelection(ref.getUpdateReferences());
		checkBox.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				ref.setUpdateReferences(checkBox.getSelection());
			}
		});
		checkBox.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
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
		tc.setText(RefactoringMessages.getString("RenameParametersWizardPage.old_names")); //$NON-NLS-1$
		
		tc= new TableColumn(table, SWT.NONE, 1);
		tc.setResizable(columnLayoutData[1].resizable);
		tc.setText(RefactoringMessages.getString("RenameParametersWizardPage.new_names")); //$NON-NLS-1$
		return layout;
	}
	
	private ParameterNamePair[] createNamePairs()   {
		try{
			Map renamings= getMultiRenameRefactoring().getNewNames();
			Set result= new HashSet();
			for (Iterator iterator = renamings.keySet().iterator(); iterator.hasNext();) {
				String oldName = (String) iterator.next();
				String newName= (String)renamings.get(oldName);
				ParameterNamePair each= new ParameterNamePair();	
				each.oldName= oldName;
				each.newName= newName;
				result.add(each);
			}
			return (ParameterNamePair[]) result.toArray(new ParameterNamePair[result.size()]);
		} catch (JavaModelException e){
			ExceptionHandler.handle(e, "Exception", "Unexpected exception occurred. See log for details.");
			return new ParameterNamePair[0];
		}	
	}
	
	//--------- private classes 
	private static class ParameterNamePair{
		String oldName;
		String newName;
	}
	
	private static class ParameterPairLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object element, int columnIndex) {
			if (! (element instanceof ParameterNamePair)) 
				return ""; //$NON-NLS-1$
			if (columnIndex == OLDNAME_PROP)
				return ((ParameterNamePair) element).oldName;
			else
				return ((ParameterNamePair) element).newName;
		}
		
		public Image getColumnImage(Object element, int columnIndex){
			return null;
		}
	};
	
	private static class ParameterPairContentProvider implements IStructuredContentProvider {
		
		public Object[] getElements(Object inputElement) {
			return (Object[])inputElement;
		}

		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
	}
	
	private class CellModifier implements ICellModifier {
		
		public boolean canModify(Object element, String property) {
			return (property.equals(PROPERTIES[NEWNAME_PROP]));
		}
		
		public Object getValue(Object element, String property) {
			if (! (element instanceof ParameterNamePair))
				return null;
			if (property.equals(PROPERTIES[OLDNAME_PROP]))
				return ((ParameterNamePair) element).oldName;
			if (property.equals(PROPERTIES[NEWNAME_PROP]))
				return ((ParameterNamePair) element).newName;
			return null;
		}
		
		public void modify(Object element, String property, Object value) {
			if (! (element instanceof TableItem)) 
				return;
			Object data= ((TableItem) element).getData();
			if (! (data instanceof ParameterNamePair)) 
				return;
			ParameterNamePair s= (ParameterNamePair) data;
			if (property.equals(PROPERTIES[NEWNAME_PROP])) {
				s.newName= (String) value;
				tableModified(getNewParameterNames());
				fViewer.update(s, new String[] { property });
			}
		}
	};
	
}
