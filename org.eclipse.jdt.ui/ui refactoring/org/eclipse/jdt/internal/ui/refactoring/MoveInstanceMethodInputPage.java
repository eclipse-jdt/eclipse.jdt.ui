package org.eclipse.jdt.internal.ui.refactoring;


import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.util.TableLayoutComposite;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.structure.MoveInstanceMethodRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.structure.MoveInstanceMethodRefactoring.INewReceiver;

class MoveInstanceMethodInputPage extends UserInputWizardPage {

	private static final String PAGE_NAME= "MOVE_INSTANCE_METHOD_INPUT_PAGE"; 
	private static final int ROW_COUNT= 7;
	
	public MoveInstanceMethodInputPage() {
		super(PAGE_NAME, true);
	}

	public void createControl(Composite parent) {
		Composite result= new Composite(parent, SWT.NONE);
		setControl(result);
		GridLayout gl= new GridLayout();
		gl.numColumns= 2;
		result.setLayout(gl);
		
		createNewReceiverList(result);
		createNewMethodNameField(result);
		createOriginalReceiverParameterNameField(result);
		WorkbenchHelp.setHelp(getControl(), IJavaHelpContextIds.MOVE_MEMBERS_WIZARD_PAGE);		
	}
	
	private void createOriginalReceiverParameterNameField(Composite result) {
		Label label= new Label(result, SWT.SINGLE);
		label.setText("&Original receiver parameter name:");
		label.setLayoutData(new GridData());
		
		final Text text= new Text(result, SWT.SINGLE | SWT.BORDER);
		text.setText(getMoveRefactoring().getOriginalReceiverParameterName());
		text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		text.addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent arg0) {
				RefactoringStatus status= getMoveRefactoring().setOriginalReceiverParameterName(text.getText());
				setPageComplete(status);
			}
		});
	}
	
	private void createNewMethodNameField(Composite result) {
		Label label= new Label(result, SWT.SINGLE);
		label.setText("New &method name:");
		label.setLayoutData(new GridData());

		final Text text= new Text(result, SWT.SINGLE | SWT.BORDER);
		text.setText(getMoveRefactoring().getNewMethodName());
		text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		text.addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent arg0) {
				RefactoringStatus status= getMoveRefactoring().setNewMethodName(text.getText());
				setPageComplete(status);
			}
		});
	}

	private void createNewReceiverList(Composite result) {
		Label label= new Label(result, SWT.SINGLE);
		label.setText("&New receiver:");
		GridData gd0= new GridData();
		gd0.horizontalSpan= 2;
		label.setLayoutData(gd0);

		TableLayoutComposite layouter= new TableLayoutComposite(result, SWT.NULL);
		addColumnLayoutData(layouter);

		Table table= new Table(layouter, SWT.SINGLE | SWT.BORDER);
		table.setHeaderVisible(true);
		table.setLinesVisible(false);		

		TableColumn column0= new TableColumn(table, SWT.NONE);		
		column0.setText("Name");
		column0.setResizable(true); 

		TableColumn column1= new TableColumn(table, SWT.NONE);
		column1.setText("Type Name");
		column1.setResizable(true);
		 
		TableColumn column2= new TableColumn(table, SWT.NONE);
		column2.setText("Parameter / Field");
		column2.setResizable(true);

		TableViewer viewer= new TableViewer(table);
		viewer.setContentProvider(new StaticObjectArrayContentProvider());
		viewer.setLabelProvider(createReceiverArrayLabelProvider());
		viewer.setInput(getMoveRefactoring().getPossibleNewReceivers());
		
		viewer.addSelectionChangedListener(new ISelectionChangedListener(){
			public void selectionChanged(SelectionChangedEvent event) {
				Object first= ((IStructuredSelection)event.getSelection()).getFirstElement();
				if (! (first instanceof INewReceiver))
					return;
				INewReceiver chosen= (INewReceiver)first;	
				getMoveRefactoring().chooseNewReceiver(chosen);
			}
		});

		GridData gd= new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		gd.heightHint= table.getGridLineWidth() + table.getItemHeight() * ROW_COUNT;
//		gd.widthHint= 40;
		gd.horizontalSpan= 2;
		layouter.setLayoutData(gd);
	}
	
	private void addColumnLayoutData(TableLayoutComposite layouter) {
		layouter.addColumnData(new ColumnWeightData(33, true));
		layouter.addColumnData(new ColumnWeightData(33, true));
		layouter.addColumnData(new ColumnWeightData(34, true));
	}
	
	private  ITableLabelProvider createReceiverArrayLabelProvider() {
		return new ITableLabelProvider(){
			public void dispose() {
			}
			public boolean isLabelProperty(Object element, String property) {
				return true;
			}
			public void addListener(ILabelProviderListener listener) {
			}
			public void removeListener(ILabelProviderListener listener) {
			}

			public Image getColumnImage(Object element, int columnIndex) {
				return null;
			}
			public String getColumnText(Object element, int columnIndex) {
				INewReceiver newReceiver= (INewReceiver)element;
				switch (columnIndex) {
					case 0 :
						return newReceiver.getName();
					case 1 :
						return Bindings.getFullyQualifiedName(newReceiver.getType());
					case 2 :
						return newReceiver.isParameter() ? "parameter": "field" ;
					default :
						Assert.isTrue(false);
						return null;
				}
			}
		};
	}
	
	private String[] createReceiverText(INewReceiver newReceiver) {
		String paramStr= newReceiver.isParameter() ? "parameter": "field" ;
		return new String[]{newReceiver.getName(), newReceiver.getType().getName(), paramStr};
	}

	private MoveInstanceMethodRefactoring getMoveRefactoring(){
		return (MoveInstanceMethodRefactoring)getRefactoring();
	}	
}
