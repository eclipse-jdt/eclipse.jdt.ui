package org.eclipse.jdt.internal.ui.refactoring;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.changes.CopyRefactoring;
import org.eclipse.jdt.internal.core.refactoring.changes.ReorgRefactoring;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
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
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

public class ReorgInputPage2 extends UserInputWizardPage {
	private TableViewer fViewer;
	private Map fErrorMap; //elements (Object) -> error messages (String)
	
	private static final String[] PROPERTIES= {"error", "replace", "old", "new"};
	private static final int COLUMN_COUNT= 4;
	private static final int ERROR_PROP= 0;
	private static final int REPLACE_PROP= 1; 
	private static final int OLDNAME_PROP= 2; 
	private static final int NEWNAME_PROP= 3; 
	
	public static final String PAGE_NAME= "ReorgPage2";
	
	public ReorgInputPage2(String description){
		super (PAGE_NAME, true);
		setDescription(description);
	}
	
	/**
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		Table table= createTableComposite(parent);
		fViewer= new TableViewer(table);
		fViewer.setUseHashlookup(true);		
		
		final CellEditor editors[]= new CellEditor[COLUMN_COUNT];
		editors[REPLACE_PROP]= new CheckboxCellEditor(table);
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

		fViewer.setContentProvider(new RenamingTupleContentProvider());
		fViewer.setLabelProvider(new RenamingTupleLabelProvider());
		
		tableModified();
		fViewer.setInput(createRenamingTuples());
		fViewer.addSelectionChangedListener(new ISelectionChangedListener(){
			public void selectionChanged(SelectionChangedEvent event){
				ISelection sel= event.getSelection();
				if (! (sel instanceof IStructuredSelection))
					return;
				Object selectedTuple= ((IStructuredSelection)sel).getFirstElement();
				Object selectedObject= ((RenamingTuple)selectedTuple).element;
				setErrorMessage((String)fErrorMap.get(selectedObject));
			}
		});
		
		//FIX ME
		//WorkbenchHelp.setHelp(getControl(), new DialogPageContextComputer(this, IJavaHelpContextIds.RENAME_PARAMS_WIZARD_PAGE));
	}
	
	public void dispose(){
		super.dispose();
		fViewer= null;
	}
	
	/* (non-Javadoc)
	 * Method declared in WizardPage
	 */
	public void setVisible(boolean visible) {
		if (visible) {
			tableModified();
			fViewer.setInput(createRenamingTuples());
		}
		super.setVisible(visible);
	}
	
	public boolean performFinish(){
		initializeRefactoring();
		return super.performFinish();
	}
	
	public IWizardPage getNextPage() {
		initializeRefactoring();
		return super.getNextPage();
	}
	
	private void initializeRefactoring(){
		getReorgRefactoring().setRenamings(getRenamingMap());
	}
	
	private static String getNewName(RenamingTuple tuple){
		if (tuple.replace == RenamingTuple.REPLACE)
			return null;
		return tuple.newName;	
	}
	
	private Table createTableComposite(Composite parent){
		Composite c= new Composite(parent, SWT.NONE);
		c.setLayout(new GridLayout());
		c.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Table table= new Table(c, SWT.H_SCROLL | SWT.V_SCROLL | SWT.SINGLE | SWT.FULL_SELECTION | SWT.HIDE_SELECTION | SWT.BORDER);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		table.setLayoutData(new GridData(GridData.FILL_BOTH));
		table.setLayout(createTableLayout(table));
		
		setControl(c);
		return table;
	}
	
	private TableLayout createTableLayout(Table table) {
		TableLayout layout= new TableLayout();
		ColumnLayoutData[] columnLayoutData= new ColumnLayoutData[COLUMN_COUNT];
		columnLayoutData[ERROR_PROP]= new ColumnWeightData(10);
		columnLayoutData[REPLACE_PROP]= new ColumnWeightData(10);
		columnLayoutData[OLDNAME_PROP]= new ColumnWeightData(40);
		columnLayoutData[NEWNAME_PROP]= new ColumnWeightData(40);
		
		layout.addColumnData(columnLayoutData[ERROR_PROP]);
		layout.addColumnData(columnLayoutData[REPLACE_PROP]);
		layout.addColumnData(columnLayoutData[OLDNAME_PROP]);
		layout.addColumnData(columnLayoutData[NEWNAME_PROP]);
		
		TableColumn tc= new TableColumn(table, SWT.NONE, ERROR_PROP);
		tc.setResizable(columnLayoutData[ERROR_PROP].resizable);
		tc.setAlignment(SWT.CENTER);
		tc.setText("Status");
		
		tc= new TableColumn(table, SWT.NONE, REPLACE_PROP);
		tc.setResizable(columnLayoutData[REPLACE_PROP].resizable);
		tc.setAlignment(SWT.CENTER);
		tc.setText("Replace");
		
		tc= new TableColumn(table, SWT.NONE, OLDNAME_PROP);
		tc.setResizable(columnLayoutData[OLDNAME_PROP].resizable);
		tc.setText("Old Name");
		
		tc= new TableColumn(table, SWT.NONE, NEWNAME_PROP);
		tc.setResizable(columnLayoutData[NEWNAME_PROP].resizable);
		tc.setText("New Name");
		return layout;
	}
	
	/**
	 * returns Object -> String (new name or null if the same)
	 */
	private Map getRenamingMap(){
		RenamingTuple[] tuples;
		Object viewerInput= fViewer.getInput();
		if (viewerInput == null)
			tuples= createRenamingTuples();
		else
			tuples= (RenamingTuple[])fViewer.getInput();
			
		if (tuples == null)
			return new HashMap(0);
			
		Map map= new HashMap(tuples.length);
		
		for (int i= 0; i < tuples.length; i++){
			map.put(tuples[i].element, getNewName(tuples[i]));
		}
		return map;
	}
	
	boolean hasAnyInput() throws JavaModelException{
		return ! getReorgRefactoring().getElementsToRename().isEmpty();
	}
	
	private RenamingTuple[] createRenamingTuples() {
		try{	
			Map map= getReorgRefactoring().getElementsToRename();
			if (map.isEmpty())
				return null;
			
			RenamingTuple[] result= new RenamingTuple[map.size()];
			Iterator iter= map.keySet().iterator();
			for (int i= 0; i < result.length; i++){
				Object each= iter.next();
				result[i]= new RenamingTuple();
				result[i].element= each;
				result[i].newName= CopyRefactoring.getElementName(each);
				if (((Boolean)map.get(each)).booleanValue())
					result[i].replace= RenamingTuple.REPLACE;
				else 	
					result[i].replace= RenamingTuple.DISABLED;
			}		
			return result;
		} catch (JavaModelException e){
			ExceptionHandler.handle(e, "Copy", "Internal Error");
			return null;
		}	
	}
	
	private ReorgRefactoring getReorgRefactoring(){
		return (ReorgRefactoring)getRefactoring();
	}
	
	private void tableModified(){
		try{
			fErrorMap= getReorgRefactoring().checkRenamings(getRenamingMap());
			setPageComplete(fErrorMap.isEmpty());
		} catch(JavaModelException e){
			ExceptionHandler.handle(e, "Reorg", "Internal Error");
		}
	}
	
	//
	private static class RenamingTuple{
		static final int DISABLED= 0;
		static final int REPLACE= 1;
		static final int NOT_REPLACE= 2;
		
		Object element;
		int replace;
		String newName;
	}
	
	private class RenamingTupleContentProvider implements IStructuredContentProvider {
		
		public Object[] getElements(Object inputElement) {
			return (Object[])inputElement;
		}

		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
	}
	
	
	private class CellModifier implements ICellModifier {
				
		/**
		 * @see ICellModifier#canModify(Object, String)
		 */
		public boolean canModify(Object element, String property) {
			if (property.equals(PROPERTIES[OLDNAME_PROP]))
				return false;
				
			if (property.equals(PROPERTIES[NEWNAME_PROP]))	
				return (((RenamingTuple)element).replace != RenamingTuple.REPLACE);

			if (property.equals(PROPERTIES[REPLACE_PROP]))
				return (((RenamingTuple)element).replace != RenamingTuple.DISABLED);

			return false;
		}
		
		/**
		 * @see ICellModifier#getValue(Object, String)
		 */
		public Object getValue(Object element, String property) {
			if (element instanceof RenamingTuple) {
				if (property.equals(PROPERTIES[REPLACE_PROP])){
					if (((RenamingTuple) element).replace == RenamingTuple.REPLACE)
						return Boolean.TRUE;
					else
						return Boolean.FALSE;
				}	
				if (property.equals(PROPERTIES[OLDNAME_PROP]))
					return  ReorgRefactoring.getElementName(element);
				if (property.equals(PROPERTIES[NEWNAME_PROP]))
					return ((RenamingTuple) element).newName;
			}
			return null;
		}
		
		/**
		 * @see ICellModifier#modify(Object, String, Object)
		 */
		public void modify(Object element, String property, Object value) {
			if (element instanceof TableItem) {
				Object data= ((TableItem) element).getData();
				if (data instanceof RenamingTuple) {
					RenamingTuple s= (RenamingTuple) data;
					if (property.equals(PROPERTIES[NEWNAME_PROP])) {
						s.newName= (String) value;
						tableModified();
						fViewer.update(s, new String[] { property });
					}
					if (property.equals(PROPERTIES[REPLACE_PROP])) {
						if (((Boolean) value).booleanValue())
							s.replace= RenamingTuple.REPLACE;
						else 
							s.replace= RenamingTuple.NOT_REPLACE;
						tableModified();
						fViewer.update(s, new String[] { property });
					}
				}
			}
		}
	};
	
	private class RenamingTupleLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object element, int columnIndex) {
			if (element instanceof RenamingTuple) {
				if (columnIndex == OLDNAME_PROP)
					return ReorgRefactoring.getElementName(((RenamingTuple)element).element);

				if (columnIndex == NEWNAME_PROP){
					if (((RenamingTuple)element).replace == RenamingTuple.REPLACE)
						return "";
					else 	
						return ((RenamingTuple) element).newName;
				}	
				
				return "";	
			}
			return ""; //$NON-NLS-1$
		}
		
		public Image getColumnImage(Object element, int columnIndex){
			if (! (element instanceof RenamingTuple)) 
				return null;
			RenamingTuple tuple= (RenamingTuple)element;
			if (columnIndex == ERROR_PROP){
				if (fErrorMap.containsKey(tuple.element))
					return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_REFACTORING_ERROR);
				else
					return null;	
			}
			if (columnIndex == REPLACE_PROP){
				if (tuple.replace == RenamingTuple.DISABLED)
					return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_REORG_NOTREPLACE_DIS);
					
				if (tuple.replace == RenamingTuple.REPLACE)
					return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_REORG_REPLACE);
				
				return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_REORG_NOTREPLACE);
			}
			return null;
		}
	};
	
		
}

