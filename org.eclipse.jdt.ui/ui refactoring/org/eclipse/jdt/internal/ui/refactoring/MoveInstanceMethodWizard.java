/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.IVariableBinding;

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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.refactoring.structure.MoveInstanceMethodRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.structure.MoveInstanceMethodRefactoring.INewReceiver;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.util.SWTUtil;
import org.eclipse.jdt.internal.ui.util.TableLayoutComposite;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;

public class MoveInstanceMethodWizard extends RefactoringWizard {

	public MoveInstanceMethodWizard(MoveInstanceMethodRefactoring ref) {
		super(ref, DIALOG_BASED_UESR_INTERFACE); 
		setDefaultPageTitle(RefactoringMessages.getString("MoveInstanceMethodWizard.Move_Method")); //$NON-NLS-1$
	}

	/* non java-doc
	 * @see RefactoringWizard#addUserInputPages
	 */ 
	protected void addUserInputPages(){
		addPage(new MoveInstanceMethodInputPage());
	}
	
	private static class MoveInstanceMethodInputPage extends UserInputWizardPage {

		private static final class NewReceiverLabelProvider extends LabelProvider implements ITableLabelProvider {
		
			private final MoveInstanceMethodRefactoring fRefactoring;
			NewReceiverLabelProvider(MoveInstanceMethodRefactoring refactoring){
				fRefactoring= refactoring;
			}
			private final ILabelProvider fJavaElementLabelProvider= new JavaElementLabelProvider();

			/*
			 * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
			 */
			public void dispose() {
				super.dispose();
				fJavaElementLabelProvider.dispose();
			}
			/*
			 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang.Object, int)
			 */
			public Image getColumnImage(Object element, int columnIndex) {
				INewReceiver newReceiver= (INewReceiver)element;
				switch (columnIndex) {
					case 0 :
						if (newReceiver.isParameter())
							return JavaPlugin.getImageDescriptorRegistry().get(JavaPluginImages.DESC_OBJS_LOCAL_VARIABLE);
						Assert.isTrue(newReceiver.isField());
						IField field= getField(newReceiver);
						if (field == null)
							return null;
						return fJavaElementLabelProvider.getImage(field);
					case 1 : return null;
					default :
						Assert.isTrue(false);
						return null;
				}
			}
		
			private IField getField(INewReceiver newReceiver) {
				if (! (newReceiver.getBinding() instanceof IVariableBinding))
					return null;
				try {
					return Bindings.findField((IVariableBinding)newReceiver.getBinding(), fRefactoring.getSourceCU().getJavaProject());
				} catch (JavaModelException e) {
					JavaPlugin.log(e);
					return null;
				}
			}

			/*
			 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object, int)
			 */
			public String getColumnText(Object element, int columnIndex) {
				INewReceiver newReceiver= (INewReceiver)element;
				switch (columnIndex) {
					case 0 :
						return newReceiver.getName();
					case 1 :
						return Bindings.getFullyQualifiedName(newReceiver.getType());
					default :
						Assert.isTrue(false);
						return null;
				}
			}
		}
		private static final String PAGE_NAME= "MOVE_INSTANCE_METHOD_INPUT_PAGE";  //$NON-NLS-1$
		private static final int ROW_COUNT= 7;
		private static final int LABEL_FLAGS= JavaElementLabels.ALL_DEFAULT
				| JavaElementLabels.M_PRE_RETURNTYPE | JavaElementLabels.M_PARAMETER_NAMES;
	
		public MoveInstanceMethodInputPage() {
			super(PAGE_NAME);
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
			Dialog.applyDialogFont(result);
			WorkbenchHelp.setHelp(getControl(), IJavaHelpContextIds.MOVE_MEMBERS_WIZARD_PAGE);		
		}
	
		private void createOriginalReceiverParameterNameField(Composite result) {
			Label label= new Label(result, SWT.SINGLE);
			label.setText(RefactoringMessages.getString("MoveInstanceMethodInputPage.Original_parameter")); //$NON-NLS-1$
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
			label.setText(RefactoringMessages.getString("MoveInstanceMethodInputPage.New_name")); //$NON-NLS-1$
			label.setLayoutData(new GridData());

			final Text text= new Text(result, SWT.SINGLE | SWT.BORDER);
			text.setText(getMoveRefactoring().getNewMethodName());
			text.selectAll();
			text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			text.setFocus();
			text.addModifyListener(new ModifyListener(){
				public void modifyText(ModifyEvent arg0) {
					RefactoringStatus status= getMoveRefactoring().setNewMethodName(text.getText());
					setPageComplete(status);
				}
			});
		}

		private void createNewReceiverList(Composite result) {
			Label label= new Label(result, SWT.SINGLE);
			IMethod method= getMoveRefactoring().getMethodToMove();
			label.setText(RefactoringMessages.getFormattedString(
					"MoveInstanceMethodInputPage.New_receiver", //$NON-NLS-1$
					JavaElementLabels.getElementLabel(method, LABEL_FLAGS)));
			GridData gd0= new GridData();
			gd0.horizontalSpan= 2;
			label.setLayoutData(gd0);

			TableLayoutComposite layouter= new TableLayoutComposite(result, SWT.NULL);
			addColumnLayoutData(layouter);

			Table table= new Table(layouter, SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION);
			table.setHeaderVisible(true);
			table.setLinesVisible(false);		

			TableColumn column0= new TableColumn(table, SWT.NONE);		
			column0.setText(RefactoringMessages.getString("MoveInstanceMethodInputPage.Name")); //$NON-NLS-1$
			column0.setResizable(true); 

			TableColumn column1= new TableColumn(table, SWT.NONE);
			column1.setText(RefactoringMessages.getString("MoveInstanceMethodInputPage.Type_Name")); //$NON-NLS-1$
			column1.setResizable(true);
		 
			TableViewer viewer= new TableViewer(table);
			viewer.setContentProvider(new ArrayContentProvider());
			viewer.setLabelProvider(new NewReceiverLabelProvider(getMoveRefactoring()));
			INewReceiver[] possibleNewReceivers= getMoveRefactoring().getPossibleNewReceivers();
			Assert.isTrue(possibleNewReceivers.length > 0);
			viewer.setInput(possibleNewReceivers);
			INewReceiver chosen= possibleNewReceivers[0];
			viewer.setSelection(new StructuredSelection(new Object[]{chosen}));
			getMoveRefactoring().chooseNewReceiver(chosen);
		
			viewer.addSelectionChangedListener(new ISelectionChangedListener(){
				public void selectionChanged(SelectionChangedEvent event) {
					Object first= ((IStructuredSelection)event.getSelection()).getFirstElement();
					if (! (first instanceof INewReceiver))
						return;
					getMoveRefactoring().chooseNewReceiver((INewReceiver)first);
				}
			});

			GridData gd= new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
			gd.heightHint= SWTUtil.getTableHeightHint(table, ROW_COUNT);
			gd.horizontalSpan= 2;
			layouter.setLayoutData(gd);
		}
	
		private void addColumnLayoutData(TableLayoutComposite layouter) {
			layouter.addColumnData(new ColumnWeightData(40, true));
			layouter.addColumnData(new ColumnWeightData(60, true));
		}
	
		private MoveInstanceMethodRefactoring getMoveRefactoring(){
			return (MoveInstanceMethodRefactoring)getRefactoring();
		}	
	}
}
