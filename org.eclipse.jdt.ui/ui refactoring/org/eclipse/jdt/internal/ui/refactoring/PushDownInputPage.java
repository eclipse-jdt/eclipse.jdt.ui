/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.util.SWTUtil;
import org.eclipse.jdt.internal.ui.util.TableLayoutComposite;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.structure.PushDownRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.structure.PushDownRefactoring.MemberActionInfo;

public class PushDownInputPage extends UserInputWizardPage {
	
	private class PullUpCellModifier implements ICellModifier {
		/*
		 * @see org.eclipse.jface.viewers.ICellModifier#getValue(java.lang.Object, java.lang.String)
		 */
		public Object getValue(Object element, String property) {
			if (ACTION_PROPERTY.equals(property)) {
				MemberActionInfo mac= (MemberActionInfo)element;
				return new Integer(mac.getAction());
			}
			return null;
		}
		/*
		 * @see org.eclipse.jface.viewers.ICellModifier#canModify(java.lang.Object, java.lang.String)
		 */
		public boolean canModify(Object element, String property) {
			return ACTION_PROPERTY.equals(property);
		}
		/*
		 * @see org.eclipse.jface.viewers.ICellModifier#modify(java.lang.Object, java.lang.String, java.lang.Object)
		 */
		public void modify(Object element, String property, Object value) {
			if (ACTION_PROPERTY.equals(property)) {
				int action= ((Integer)value).intValue();
				MemberActionInfo mac= (MemberActionInfo)((Item)element).getData();
				mac.setAction(action);
				PushDownInputPage.this.updateUIElements(null);
			}
		}
	}

	private static class MemberActionInfoLabelProvider extends LabelProvider implements ITableLabelProvider {
		private final ILabelProvider fJavaElementLabelProvider= new JavaElementLabelProvider();

		/*
		 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object, int)
		 */
		public String getColumnText(Object element, int columnIndex) {
			MemberActionInfo mac= (MemberActionInfo)element;
			switch (columnIndex) {
				case MEMBER_COLUMN :
					return fJavaElementLabelProvider.getText(mac.getMember());
				case ACTION_COLUMN :
					return getActionLabel(mac);
				default :
					Assert.isTrue(false);
					return null;
			}
		}
		
		static String[] getAvailableActionLabels(MemberActionInfo mac){
			int[] actions= mac.getAvailableActions();
			String[] result= new String[actions.length];
			for(int i= 0; i < actions.length; i++){
				result[i]= getActionLabel(actions[i]);
			}
			return result;
		}
		
		static String getActionLabel(MemberActionInfo mac) {
			return getActionLabel(mac.getAction());
		}

		static String getActionLabel(int action) {
			switch(action){
				case MemberActionInfo.NO_ACTION:
					return "none";
				case MemberActionInfo.PUSH_ABSTRACT_ACTION:
					return "leave abstract declaration";
				case MemberActionInfo.PUSH_DOWN_ACTION:
					return "push down";
				default:
					Assert.isTrue(false);
					return null;
			}
		}

		/* 
		 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang.Object, int)
		 */
		public Image getColumnImage(Object element, int columnIndex) {
			MemberActionInfo mac= (MemberActionInfo)element;
			switch (columnIndex) {
				case MEMBER_COLUMN :
					return fJavaElementLabelProvider.getImage(mac.getMember());
				case ACTION_COLUMN :
					return null;
				default :
					Assert.isTrue(false);
					return null;
			}
		}
		/*
		 * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
		 */
		public void dispose() {
			super.dispose();
			fJavaElementLabelProvider.dispose();
		}
	}

	private static final int MEMBER_COLUMN= 0;
	private static final int ACTION_COLUMN= 1;
	public static final String PAGE_NAME= "PushDownInputPage"; //$NON-NLS-1$
	private final static String ACTION_PROPERTY= "action"; //$NON-NLS-1$	
	private final static String MEMBER_PROPERTY= "member"; //$NON-NLS-1$	
	
	private static final int ROW_COUNT= 10;

	private Button fEditButton;
	private TableViewer fTableViewer;

	public PushDownInputPage() {
		super(PAGE_NAME, true);
	}

	/*
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		GridLayout gl= new GridLayout();
		composite.setLayout(gl);

		createMemberTableLabel(composite);
		createMemberTableComposite(composite);
		
		setControl(composite);
		WorkbenchHelp.setHelp(getControl(), IJavaHelpContextIds.PUSH_DOWN_WIZARD_PAGE);
	}

	private void createMemberTableLabel(Composite parent) {
		Label label= new Label(parent, SWT.NONE) ;
		label.setText("&Specify actions for members:");
		GridData gd0= new GridData();
		label.setLayoutData(gd0);
	}

	private void createMemberTableComposite(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		GridData gd= new GridData(GridData.FILL_HORIZONTAL);
		composite.setLayoutData(gd);
		GridLayout gl= new GridLayout();
		gl.numColumns= 2;
		gl.marginWidth= 0;
		gl.marginHeight= 0;
		composite.setLayout(gl);

		createMemberTable(composite);
		createButtonComposite(composite);
	}

	private void createMemberTable(Composite parent) {
		TableLayoutComposite layouter= new TableLayoutComposite(parent, SWT.NONE);
		layouter.addColumnData(new ColumnWeightData(60, true));
		layouter.addColumnData(new ColumnWeightData(40, true));

		final Table table= new Table(layouter, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		
		GridData gd= new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		gd.heightHint= table.getHeaderHeight() + (table.getGridLineWidth() + table.getItemHeight()) * ROW_COUNT;
		layouter.setLayoutData(gd);

				
		TableLayout tableLayout= new TableLayout();
		table.setLayout(tableLayout);

		TableColumn column0= new TableColumn(table, SWT.NONE);		
		column0.setText("Member");

		TableColumn column1= new TableColumn(table, SWT.NONE);
		column1.setText("Action");
		
		fTableViewer= new TableViewer(table);
		fTableViewer.setUseHashlookup(true);
		fTableViewer.setContentProvider(new StaticObjectArrayContentProvider());
		fTableViewer.setLabelProvider(new MemberActionInfoLabelProvider());
		fTableViewer.addSelectionChangedListener(new ISelectionChangedListener(){
			public void selectionChanged(SelectionChangedEvent event) {
				PushDownInputPage.this.updateButtonEnablementState((IStructuredSelection)event.getSelection());
			}
		});
		
		fTableViewer.setInput(getPushDownRefactoring().getMemberActionInfos());
		updateUIElements(null);
		setupCellEditors(table);
	}

	private void setupCellEditors(final Table table) {
		final ComboBoxCellEditor comboBoxCellEditor= new ComboBoxCellEditor();
		comboBoxCellEditor.setStyle(SWT.READ_ONLY);
		fTableViewer.setCellEditors(new CellEditor [] {null, comboBoxCellEditor});
		fTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				if (comboBoxCellEditor.getControl() == null & ! table.isDisposed())
					comboBoxCellEditor.create(table);
				Assert.isTrue(event.getSelection() instanceof IStructuredSelection);	
				IStructuredSelection ss= (IStructuredSelection)event.getSelection();
				if (ss.size() != 1)
					return;
				MemberActionInfo mac= (MemberActionInfo)ss.getFirstElement();
				comboBoxCellEditor.setItems(MemberActionInfoLabelProvider.getAvailableActionLabels(mac));
				comboBoxCellEditor.setValue(new Integer(mac.getAction()));
			}
		});
		
		ICellModifier cellModifier = new PullUpCellModifier();
		fTableViewer.setCellModifier(cellModifier);
		fTableViewer.setColumnProperties(new String[] {MEMBER_PROPERTY, ACTION_PROPERTY});
	}
		
	private void createButtonComposite(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(GridData.FILL_VERTICAL));
		GridLayout gl= new GridLayout();
		gl.marginHeight= 0;
		gl.marginWidth= 0;
		composite.setLayout(gl);
		
		fEditButton= new Button(composite, SWT.PUSH);
		fEditButton.setText("&Edit Selected...");
		fEditButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fEditButton.setEnabled(false);
		SWTUtil.setButtonDimensionHint(fEditButton);
		fEditButton.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent event) {
				PushDownInputPage.this.editSelectedMembers();
			}
		});
	}
	
	private void editSelectedMembers() {
		ISelection preserved= fTableViewer.getSelection();
		try{
			String shellTitle= "Edit members";
			String labelText= "&Mark selected member(s) as:";
			Map stringMapping= createStringMappingForSelectedElements();
			String[] keys= (String[]) stringMapping.keySet().toArray(new String[stringMapping.keySet().size()]);
			Arrays.sort(keys);
			ComboSelectionDialog dialog= new ComboSelectionDialog(getShell(), shellTitle, labelText, keys);
			dialog.setBlockOnOpen(true);
			if (dialog.open() == Dialog.CANCEL)
				return;
			int action= ((Integer)stringMapping.get(dialog.getSelectedString())).intValue();
			setInfoAction(getSelectedMemberActionInfos(), action);
		} finally{
			updateUIElements(preserved);
		}
	}

	//String -> Integer
	private Map createStringMappingForSelectedElements() {
		MemberActionInfo[] infos= getSelectedMemberActionInfos();
		Set intersection= createSet(new int[]{MemberActionInfo.NO_ACTION, 
										MemberActionInfo.PUSH_ABSTRACT_ACTION,
										MemberActionInfo.PUSH_DOWN_ACTION});
		for (int i= 0; i < infos.length; i++) {
			intersection.retainAll(createSet(infos[i].getAvailableActions()));
		}
		Map result= new HashMap(intersection.size());
		for (Iterator iter= intersection.iterator(); iter.hasNext();) {
			Integer action= (Integer) iter.next();
			String key= MemberActionInfoLabelProvider.getActionLabel(action.intValue());
			result.put(key, action);
		}
		return result;
	}

	private static void setInfoAction(MemberActionInfo[] infos, int action) {
		for (int i = 0; i < infos.length; i++) {
			infos[i].setAction(action);
		}
	}

	private MemberActionInfo[] getSelectedMemberActionInfos() {
		Assert.isTrue(fTableViewer.getSelection() instanceof IStructuredSelection);
		List result= getTableSelection().toList();
		return (MemberActionInfo[]) result.toArray(new MemberActionInfo[result.size()]);
	}
	
	private IStructuredSelection getTableSelection() {
		return (IStructuredSelection)fTableViewer.getSelection();
	}
	
	private static Set createSet(int[] numbers){
		Set result= new HashSet(numbers.length * 2);
		for (int i= 0; i < numbers.length; i++) {
			result.add(new Integer(numbers[i]));
		}
		return result;
	}

	private void updateUIElements(ISelection preserved) {
		fTableViewer.refresh();
		if (preserved != null){
			fTableViewer.getControl().setFocus();
			fTableViewer.setSelection(preserved);
		}
		checkPageCompletionStatus();
		updateButtonEnablementState(getTableSelection());
	}

	private void checkPageCompletionStatus() {
		if (areAllElementsMarkedAsNoAction()){
			setErrorMessage("Select member(s) to push down or declare abstract");
			setPageComplete(false);
		} else {
			setErrorMessage(null);
			setPageComplete(true);
		}
	}
	
	private boolean areAllElementsMarkedAsNoAction() {
		return countInfosForAction(MemberActionInfo.NO_ACTION) == getTableInputAsMemberActionInfoArray().length;
	}

	private int countInfosForAction(int action) {
		MemberActionInfo[] macs= getTableInputAsMemberActionInfoArray();
		int count= 0;
		for (int i= 0; i < macs.length; i++) {
			MemberActionInfo info= macs[i];
			if (info.getAction() == action)
				count++;
		}
		return count;
	}

	private MemberActionInfo[] getTableInputAsMemberActionInfoArray() {
		return (MemberActionInfo[])fTableViewer.getInput();
	}

	private void updateButtonEnablementState(IStructuredSelection tableSelection) {
		if (tableSelection == null || fEditButton == null)
			return;
			
		fEditButton.setEnabled(! tableSelection.isEmpty() && tableSelection.size() != 0);
	}

	private PushDownRefactoring getPushDownRefactoring(){
		return (PushDownRefactoring)getRefactoring();
	}
	
}