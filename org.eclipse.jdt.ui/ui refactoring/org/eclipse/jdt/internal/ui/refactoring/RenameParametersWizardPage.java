/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.core.refactoring.methods.RenameParametersRefactoring;
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
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

public class RenameParametersWizardPage extends UserInputWizardPage {

	public static final String PAGE_NAME= "RenameParametersInputPage"; //$NON-NLS-1$
	
	private static final String[] PROPERTIES= {"old", "new"}; //$NON-NLS-2$ //$NON-NLS-1$
	private static final int OLDNAME_PROP= 0; 
	private static final int NEWNAME_PROP= 1; 
	
	private TableViewer fViewer;
	
	public RenameParametersWizardPage(boolean isLastUserPage) {
		super(PAGE_NAME, isLastUserPage);
	}
	
	private RenameParametersRefactoring getRenameParametersRefactoring(){
		return (RenameParametersRefactoring)getRefactoring();
	}			
	
	/**
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
		
		fViewer.setInput(createParameterNamePairs());
		
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
			tableModified(getNewParameterNames());
		}
		super.setVisible(visible);
	}
	
	private RefactoringStatus validateTable(String[] newNames){
		RenameParametersRefactoring ref= getRenameParametersRefactoring();
		ref.setNewParameterNames(newNames);
		return ref.checkNewNames();
	}
	
	private void tableModified(String[] newNames) {			
		RefactoringStatus status= validateTable(newNames);
		getRefactoringWizard().setStatus(status);
		if (status != null && status.hasFatalError()) {
			setPageComplete(false);
			setErrorMessage(status.getFirstMessage(RefactoringStatus.FATAL));
		} else {
			setPageComplete(true);	
			setErrorMessage(null);
		}	
	}
	
	private String[] getNewParameterNames(){
		ParameterNamePair[] input= (ParameterNamePair[])fViewer.getInput();
		String[] result= new String[input.length];
		for (int i= 0; i < result.length; i++)
			result[i]= input[i].newName;
		return result;
	}
	
	private Table createTableComposite(Composite parent){
		Composite c= new Composite(parent, SWT.NONE);
		c.setLayout(new GridLayout());
		c.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Table table= new Table(c, SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.FULL_SELECTION | SWT.HIDE_SELECTION | SWT.BORDER);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		table.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		table.setLayout(createTableLayout(table));
		
		setControl(c);
		return table;
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
	
	private ParameterNamePair[] createParameterNamePairs(){
		String[] oldNames= getOldParameterNames();
		ParameterNamePair[] result= new ParameterNamePair[oldNames.length];
		for (int i= 0; i < oldNames.length ; i++){
			result[i]= new ParameterNamePair();
			result[i].oldName= oldNames[i];
			result[i].newName= oldNames[i];
		}		
		return result;
	}
	
	private String[] getOldParameterNames(){
		RenameParametersRefactoring ref= getRenameParametersRefactoring();
		String[] oldNames;
		try{
			oldNames= ref.getMethod().getParameterNames();
		} catch (JavaModelException e){
			ExceptionHandler.handle(e, RefactoringMessages.getString("RenameParametersWizardPage.refactoring"), RefactoringMessages.getString("RenameParametersWizardPage.internal_error"));  //$NON-NLS-2$ //$NON-NLS-1$
			oldNames= new String[]{""}; //$NON-NLS-1$
		}
		return oldNames;
	}	
	
	//--------- private classes 
	private static class ParameterNamePair{
		String oldName;
		String newName;
	}
	
	private static class ParameterPairLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object element, int columnIndex) {
			if (element instanceof ParameterNamePair) {
				if (columnIndex == OLDNAME_PROP)
					return ((ParameterNamePair) element).oldName;
				else
					return ((ParameterNamePair) element).newName;
			}
			return ""; //$NON-NLS-1$
		}
		
		public Image getColumnImage(Object element, int columnIndex){
			return null;
		}
	};
	
	private static class ParameterPairContentProvider implements IStructuredContentProvider {
		
		/**
		 * @see IStructuredContentProvider#getElements(Object)
		 */
		public Object[] getElements(Object inputElement) {
			return (Object[])inputElement;
		}

		/**
		 * @see IContentProvider#dispose()
		 */
		public void dispose() {
		}

		/**
		 * @see IContentProvider#inputChanged(Viewer, Object, Object)
		 */
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
	}
	
	private class CellModifier implements ICellModifier {
		
		/**
		 * @see ICellModifier#canModify(Object, String)
		 */
		public boolean canModify(Object element, String property) {
			return (property.equals(PROPERTIES[NEWNAME_PROP]));
		}
		
		/**
		 * @see ICellModifier#getValue(Object, String)
		 */
		public Object getValue(Object element, String property) {
			if (element instanceof ParameterNamePair) {
				if (property.equals(PROPERTIES[OLDNAME_PROP]))
					return ((ParameterNamePair) element).oldName;
				if (property.equals(PROPERTIES[NEWNAME_PROP]))
					return ((ParameterNamePair) element).newName;
			}
			return null;
		}
		
		/**
		 * @see ICellModifier#modify(Object, String, Object)
		 */
		public void modify(Object element, String property, Object value) {
			if (element instanceof TableItem) {
				Object data= ((TableItem) element).getData();
				if (data instanceof ParameterNamePair) {
					ParameterNamePair s= (ParameterNamePair) data;
					if (property.equals(PROPERTIES[NEWNAME_PROP])) {
						s.newName= (String) value;
						tableModified(getNewParameterNames());
						fViewer.update(s, new String[] { property });
					}
				}
			}
		}
	};
	
}
