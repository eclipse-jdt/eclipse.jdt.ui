/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.JavaModelException;

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
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.structure.PushDownRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.structure.PushDownRefactoring.MemberActionInfo;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.SWTUtil;
import org.eclipse.jdt.internal.ui.util.TableLayoutComposite;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;

public class PushDownWizard extends RefactoringWizard {

	public PushDownWizard(PushDownRefactoring ref) {
		super(ref, DIALOG_BASED_UESR_INTERFACE);
		setDefaultPageTitle(RefactoringMessages.getString("PushDownWizard.defaultPageTitle")); //$NON-NLS-1$
		setDefaultPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_REFACTOR_PULL_UP);//XXX incorrect icon
	}

	/* non java-doc
	 * @see RefactoringWizard#addUserInputPages
	 */ 
	protected void addUserInputPages(){
		addPage(new PushDownInputPage());
	}
	
	private static class PushDownInputPage extends UserInputWizardPage {
	
		private class PushDownCellModifier implements ICellModifier {
			/*
			 * @see org.eclipse.jface.viewers.ICellModifier#getValue(java.lang.Object, java.lang.String)
			 */
			public Object getValue(Object element, String property) {
				if (! ACTION_PROPERTY.equals(property))
					return null;

				MemberActionInfo mac= (MemberActionInfo)element;
				return new Integer(mac.getAction());
			}
			/*
			 * @see org.eclipse.jface.viewers.ICellModifier#canModify(java.lang.Object, java.lang.String)
			 */
			public boolean canModify(Object element, String property) {
				if (! ACTION_PROPERTY.equals(property))
					return false;
				return ((MemberActionInfo)element).isEditable();
			}
			/*
			 * @see org.eclipse.jface.viewers.ICellModifier#modify(java.lang.Object, java.lang.String, java.lang.Object)
			 */
			public void modify(Object element, String property, Object value) {
				if (! ACTION_PROPERTY.equals(property))
					return;

				int action= ((Integer)value).intValue();
				MemberActionInfo mac;
				if (element instanceof Item) {
					mac= (MemberActionInfo)((Item)element).getData();
				} else
					mac= (MemberActionInfo)element;
				if (! canModify(mac, property))
					return;
				mac.setAction(action);
				PushDownInputPage.this.updateUIElements(null, true);
			}
		}

		private static class MemberActionInfoLabelProvider extends LabelProvider implements ITableLabelProvider {
			private final ILabelProvider fJavaElementLabelProvider= new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT | JavaElementLabelProvider.SHOW_SMALL_ICONS);

			/*
			 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object, int)
			 */
			public String getColumnText(Object element, int columnIndex) {
				MemberActionInfo mac= (MemberActionInfo)element;
				switch (columnIndex) {
					case MEMBER_COLUMN :	return fJavaElementLabelProvider.getText(mac.getMember());
					case ACTION_COLUMN :	return getActionLabel(mac);
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
					case MemberActionInfo.NO_ACTION: 				return ""; //$NON-NLS-1$
					case MemberActionInfo.PUSH_ABSTRACT_ACTION:	return RefactoringMessages.getString("PushDownInputPage.leave_abstract"); //$NON-NLS-1$
					case MemberActionInfo.PUSH_DOWN_ACTION:		return RefactoringMessages.getString("PushDownInputPage.push_down"); //$NON-NLS-1$
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
					case MEMBER_COLUMN:	return fJavaElementLabelProvider.getImage(mac.getMember());
					case ACTION_COLUMN:	return null;
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
		private PullPushCheckboxTableViewer fTableViewer;
		private Label fStatusLine;
	
		public PushDownInputPage() {
			super(PAGE_NAME);
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
			createStatusLine(composite);
		
			setControl(composite);
			Dialog.applyDialogFont(composite);
			PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IJavaHelpContextIds.PUSH_DOWN_WIZARD_PAGE);
		}

		private void createStatusLine(Composite composite) {
			fStatusLine= new Label(composite, SWT.NONE);
			GridData gd= new GridData();
			gd.horizontalSpan= 2;
			updateStatusLine();
			fStatusLine.setLayoutData(gd);
		}
	
		private void createMemberTableLabel(Composite parent) {
			Label label= new Label(parent, SWT.NONE) ;
			label.setText(RefactoringMessages.getString("PushDownInputPage.Specify_actions")); //$NON-NLS-1$
			GridData gd0= new GridData();
			label.setLayoutData(gd0);
		}

		private void createMemberTableComposite(Composite parent) {
			Composite composite= new Composite(parent, SWT.NONE);
			GridData gd= new GridData(GridData.FILL_BOTH);
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

			final Table table= new Table(layouter, SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION | SWT.CHECK);
			table.setHeaderVisible(true);
			table.setLinesVisible(true);
		
			GridData gd= new GridData(GridData.FILL_BOTH);
			gd.heightHint= SWTUtil.getTableHeightHint(table, ROW_COUNT);
			gd.widthHint= convertWidthInCharsToPixels(30);
			layouter.setLayoutData(gd);
				
			TableLayout tableLayout= new TableLayout();
			table.setLayout(tableLayout);

			TableColumn column0= new TableColumn(table, SWT.NONE);		
			column0.setText(RefactoringMessages.getString("PushDownInputPage.Member")); //$NON-NLS-1$

			TableColumn column1= new TableColumn(table, SWT.NONE);
			column1.setText(RefactoringMessages.getString("PushDownInputPage.Action")); //$NON-NLS-1$
		
			fTableViewer= new PullPushCheckboxTableViewer(table);
			fTableViewer.setUseHashlookup(true);
			fTableViewer.setContentProvider(new ArrayContentProvider());
			fTableViewer.setLabelProvider(new MemberActionInfoLabelProvider());
			fTableViewer.addSelectionChangedListener(new ISelectionChangedListener(){
				public void selectionChanged(SelectionChangedEvent event) {
					PushDownInputPage.this.updateButtonEnablementState((IStructuredSelection)event.getSelection());
				}
			});
			fTableViewer.addCheckStateListener(new ICheckStateListener(){
				public void checkStateChanged(CheckStateChangedEvent event) {
					boolean checked= event.getChecked();
					MemberActionInfo info= (MemberActionInfo)event.getElement();
					if (checked)
						info.setAction(MemberActionInfo.PUSH_DOWN_ACTION);
					else
						info.setAction(MemberActionInfo.NO_ACTION);
					updateUIElements(null, true);
				}
			});
			fTableViewer.addDoubleClickListener(new IDoubleClickListener(){
				public void doubleClick(DoubleClickEvent event) {
					PushDownInputPage.this.editSelectedMembers();
				}
			});

		
			fTableViewer.setInput(getPushDownRefactoring().getMemberActionInfos());
			updateUIElements(null, false);
			setupCellEditors(table);
		}

		private MemberActionInfo[] getActiveInfos() {
			MemberActionInfo[] infos= getPushDownRefactoring().getMemberActionInfos();
			List result= new ArrayList(infos.length);
			for (int i= 0; i < infos.length; i++) {
				PushDownRefactoring.MemberActionInfo info= infos[i];
				if (info.isActive())
					result.add(info);
			}
			return (MemberActionInfo[]) result.toArray(new MemberActionInfo[result.size()]);
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
		
			ICellModifier cellModifier = new PushDownCellModifier();
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
			fEditButton.setText(RefactoringMessages.getString("PushDownInputPage.Edit")); //$NON-NLS-1$
			fEditButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			fEditButton.setEnabled(false);
			SWTUtil.setButtonDimensionHint(fEditButton);
			fEditButton.addSelectionListener(new SelectionAdapter(){
				public void widgetSelected(SelectionEvent event) {
					PushDownInputPage.this.editSelectedMembers();
				}
			});

			Button addButton= new Button(composite, SWT.PUSH);
			addButton.setText(RefactoringMessages.getString("PushDownInputPage.Add_Required")); //$NON-NLS-1$
			addButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			SWTUtil.setButtonDimensionHint(addButton);
			addButton.addSelectionListener(new SelectionAdapter(){
				public void widgetSelected(SelectionEvent event) {
					PushDownInputPage.this.markAdditionalRequiredMembersAsMembersToPushDown();
				}
			});
		}
	
		public void markAdditionalRequiredMembersAsMembersToPushDown() {
			try {
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().run(false, false, new IRunnableWithProgress() {
					public void run(IProgressMonitor pm) throws InvocationTargetException {
						try {
							getPushDownRefactoring().computeAdditionalRequiredMembersToPushDown(pm);
							updateUIElements(null, true);
						} catch (JavaModelException e) {
							throw new InvocationTargetException(e);
						} finally {
							pm.done();
						}
					}
				});
			} catch(InvocationTargetException e) {
				ExceptionHandler.handle(e, getShell(), RefactoringMessages.getString("PushDownInputPage.Push_Down"), RefactoringMessages.getString("PushDownInputPage.Internal_Error")); //$NON-NLS-1$ //$NON-NLS-2$
			} catch(InterruptedException e) {
				Assert.isTrue(false);//not cancellable
			}
		}

		private void editSelectedMembers() {
			if (! fEditButton.isEnabled())
				return;
			ISelection preserved= fTableViewer.getSelection();
			try{
				String shellTitle= RefactoringMessages.getString("PushDownInputPage.Edit_members"); //$NON-NLS-1$
				String labelText= RefactoringMessages.getString("PushDownInputPage.Mark_selected_members"); //$NON-NLS-1$
				Map stringMapping= createStringMappingForSelectedElements();
				String[] keys= (String[]) stringMapping.keySet().toArray(new String[stringMapping.keySet().size()]);
				Arrays.sort(keys);
				int initialSelectionIndex= getInitialSelectionIndexForEditDialog(stringMapping, keys);
				
				ComboSelectionDialog dialog= new ComboSelectionDialog(getShell(), shellTitle, labelText, keys, initialSelectionIndex);
				dialog.setBlockOnOpen(true);
				if (dialog.open() == Window.CANCEL)
					return;
				int action= ((Integer)stringMapping.get(dialog.getSelectedString())).intValue();
				setInfoAction(getSelectedMemberActionInfos(), action);
			} finally{
				updateUIElements(preserved, true);
			}
		}

		private int getInitialSelectionIndexForEditDialog(Map stringMapping, String[] keys) {
			int commonActionCode= getCommonActionCodeForSelectedInfos();
			if (commonActionCode == -1)
				return 0;
			for (Iterator iter= stringMapping.keySet().iterator(); iter.hasNext();) {
				String key= (String) iter.next();
				int action= ((Integer)stringMapping.get(key)).intValue();
				if (commonActionCode == action){
					for (int i= 0; i < keys.length; i++) {
						if (key.equals(keys[i]))
							return i;
					}
					Assert.isTrue(false);//there's no way
				}
			}
			return 0;
		}

		private int getCommonActionCodeForSelectedInfos() {
			MemberActionInfo[] infos= getSelectedMemberActionInfos();
			if (infos.length == 0)
				return -1;
			
			int code= infos[0].getAction();
			for (int i= 0; i < infos.length; i++) {
				if (code != infos[i].getAction())
					return -1;
			}
			return code;
		}

		//String -> Integer
		private Map createStringMappingForSelectedElements() {
			Map result= new HashMap();
			addToStringMapping(result, MemberActionInfo.PUSH_DOWN_ACTION);
			addToStringMapping(result, MemberActionInfo.PUSH_ABSTRACT_ACTION);
			return result;
		}
		private void addToStringMapping(Map result, int action) {
			result.put(MemberActionInfoLabelProvider.getActionLabel(action), new Integer(action));
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
	
		private void updateUIElements(ISelection preserved, boolean displayErrorMessage) {
			fTableViewer.refresh();
			if (preserved != null){
				fTableViewer.getControl().setFocus();
				fTableViewer.setSelection(preserved);
			}
			checkPageCompletionStatus(displayErrorMessage);
			updateButtonEnablementState(getTableSelection());
			updateStatusLine();
		}

		private void updateStatusLine(){
			if (fStatusLine == null)
				return;
			int selected= fTableViewer.getCheckedElements().length;
			String[] keys= {String.valueOf(selected)};
			String msg= RefactoringMessages.getFormattedString("PushDownInputPage.status_line", keys); //$NON-NLS-1$
			fStatusLine.setText(msg);
		}

		private void checkPageCompletionStatus(boolean displayErrorMessage) {
			if (areAllElementsMarkedAsNoAction()){
				if (displayErrorMessage)
					setErrorMessage(RefactoringMessages.getString("PushDownInputPage.Select_members_to_push_down")); //$NON-NLS-1$
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
			
			fEditButton.setEnabled(enableEditButton(tableSelection));
		}
		private boolean enableEditButton(IStructuredSelection tableSelection) {
			if (tableSelection.isEmpty() || tableSelection.size() == 0)
				return false;
			return tableSelection.size() == countEditableInfos(getSelectedMemberActionInfos());
		}

		private static int countEditableInfos(MemberActionInfo[] memberActionInfos) {
			int result= 0;
			for (int i= 0; i < memberActionInfos.length; i++) {
				if (memberActionInfos[i].isEditable())
					result++;
			}
			return result;
		}

		private PushDownRefactoring getPushDownRefactoring(){
			return (PushDownRefactoring)getRefactoring();
		}
	
		/* (non-Javadoc)
		 * @see org.eclipse.jface.dialogs.IDialogPage#setVisible(boolean)
		 */
		public void setVisible(boolean visible) {
			super.setVisible(visible);
			if (visible){
				fTableViewer.setSelection(new StructuredSelection(getActiveInfos()), true);
				fTableViewer.getControl().setFocus();
			}
		}
	}
}
