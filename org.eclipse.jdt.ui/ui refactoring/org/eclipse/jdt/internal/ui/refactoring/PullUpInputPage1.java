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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
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
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.wizard.IWizardPage;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.SWTUtil;
import org.eclipse.jdt.internal.ui.util.TableLayoutComposite;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.structure.IMemberActionInfo;
import org.eclipse.jdt.internal.corext.refactoring.structure.PullUpRefactoring;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

class PullUpInputPage1 extends UserInputWizardPage {
	
	private class PullUpCellModifier implements ICellModifier {
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
			
			MemberActionInfo mac= (MemberActionInfo)element;
			return mac.isEditable();
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
			Assert.isTrue(mac.isMethodInfo());
			mac.setAction(action);
			PullUpInputPage1.this.updateUIElements(null);
		}
	}

	private static class MemberActionInfo implements IMemberActionInfo{
		static final int PULL_UP_ACTION= 			0;//values are important here
		static final int DECLARE_ABSTRACT_ACTION= 1;//values are important here
		static final int NO_ACTION= 				2;

		private static final String NO_LABEL= ""; //$NON-NLS-1$
		private static final String PULL_UP_LABEL= RefactoringMessages.getString("PullUpInputPage1.pull_up"); //$NON-NLS-1$
		private static final String DECLARE_ABSTRACT_LABEL= RefactoringMessages.getString("PullUpInputPage1.declare_abstract"); //$NON-NLS-1$
		private static final String[] FIELD_LABELS= {NO_LABEL};
		private static final String[] METHOD_LABELS;//indices correspond to values
		static{
			METHOD_LABELS= new String[2];
			METHOD_LABELS[0]= PULL_UP_LABEL;
			METHOD_LABELS[1]= DECLARE_ABSTRACT_LABEL;
		}
				
		private final IMember fMember;
		private int fAction;
		
		MemberActionInfo(IMember member, int action){
			Assert.isTrue((member instanceof IMethod) || (member instanceof IField));
			assertAction(member, action);
			fMember= member;
			fAction= action;
		}
		
		public boolean isMethodInfo() {
			return getMember() instanceof IMethod;
		}

		public boolean isFieldInfo() {
			return getMember() instanceof IField;
		}

		IMember getMember(){
			return fMember;
		}
		
		int getAction(){
			return fAction;
		}
		
		private static void assertAction(IMember member, int action){
			if (member instanceof IMethod)
				Assert.isTrue(
			action == NO_ACTION ||
				action == DECLARE_ABSTRACT_ACTION ||
				action == PULL_UP_ACTION);
			else
				Assert.isTrue(
				action == NO_ACTION ||
				action == PULL_UP_ACTION);
		}
		
		void setAction(int action){
			assertAction(fMember, action);
			fAction= action;
		}
		
		String getActionLabel(){
			switch(fAction){
				case PULL_UP_ACTION: 			return PULL_UP_LABEL;
				case DECLARE_ABSTRACT_ACTION: 	return DECLARE_ABSTRACT_LABEL;
				case NO_ACTION: 				return NO_LABEL;
				default:
					Assert.isTrue(false);
					return null; 
			}
		}

		String[] getAllowedLabels() {
			if (isFieldInfo())
				return FIELD_LABELS;
			else
				return METHOD_LABELS;
		}

		public boolean isEditable() {
			if (isFieldInfo())
				return false;
			if (fAction == NO_ACTION)
				return false;
			return true;	
		}

		public boolean isNoAction() {
			return getAction() == NO_ACTION;
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
					return mac.getActionLabel();
				default :
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
	private static final int ROW_COUNT = 10;
	
	public static final String PAGE_NAME= "PullUpMethodsInputPage1"; //$NON-NLS-1$
	private final static String ACTION_PROPERTY= "action"; //$NON-NLS-1$	
	private final static String MEMBER_PROPERTY= "member"; //$NON-NLS-1$	

	private CheckboxTableViewer fTableViewer;
	private Combo fSuperclassCombo;
	private IType[] fSuperclasses;
	private Button fEditButton;
	private Button fCreateStubsButton;
	private Label fStatusLine;

	public PullUpInputPage1() {
		super(PAGE_NAME, false);
		setMessage(RefactoringMessages.getString("PullUpInputPage1.page_message")); //$NON-NLS-1$
	}

	public void createControl(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		GridLayout gl= new GridLayout();
		gl.numColumns= 2;
		composite.setLayout(gl);

		createSuperTypeCombo(composite);
		createSpacer(composite);
		createStubCheckbox(composite);
		createSpacer(composite);
		createMemberTableLabel(composite);
		createMemberTableComposite(composite);
		createStatusLine(composite);
				
		setControl(composite);
		WorkbenchHelp.setHelp(getControl(), IJavaHelpContextIds.PULL_UP_WIZARD_PAGE);			
	}
	
	private void createStatusLine(Composite composite) {
		fStatusLine= new Label(composite, SWT.NONE);
		GridData gd= new GridData();
		gd.horizontalSpan= 2;
		updateStatusLine();
		fStatusLine.setLayoutData(gd);
	}
	
	private void createStubCheckbox(Composite parent) {
		fCreateStubsButton= new Button(parent, SWT.CHECK);
		fCreateStubsButton.setText(RefactoringMessages.getString("PullUpInputPage1.Create_stubs")); //$NON-NLS-1$
		GridData gd= new GridData();
		gd.horizontalSpan= 2;
		fCreateStubsButton.setLayoutData(gd);
		fCreateStubsButton.setEnabled(false);
		fCreateStubsButton.setSelection(getPullUpRefactoring().getCreateMethodStubs());
	}

	private void createSpacer(Composite parent) {
		Label label= new Label(parent, SWT.NONE) ;
		GridData gd0= new GridData();
		gd0.horizontalSpan= 2;
		label.setLayoutData(gd0);
	}
	
	private PullUpRefactoring getPullUpRefactoring(){
		return (PullUpRefactoring)getRefactoring();
	}

	private void createSuperTypeCombo(final Composite parent) {
		try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().run(false, false, new IRunnableWithProgress() {
				public void run(IProgressMonitor pm) throws InvocationTargetException {
					try {
						createSuperTypeCombo(pm, parent);						
					} catch (JavaModelException e) {
						throw new InvocationTargetException(e);
					} finally {
						pm.done();
					}
				}
			});
		} catch(InvocationTargetException e) {
			ExceptionHandler.handle(e, getShell(), RefactoringMessages.getString("PullUpInputPage.pull_Up"), RefactoringMessages.getString("PullUpInputPage.exception")); //$NON-NLS-1$ //$NON-NLS-2$
		} catch(InterruptedException e) {
			Assert.isTrue(false);//not cancellable
		}
	}
	private void createSuperTypeCombo(IProgressMonitor pm, Composite parent) throws JavaModelException {
		Label label= new Label(parent, SWT.NONE) ;
		label.setText(RefactoringMessages.getString("PullUpInputPage1.Select_destination")); //$NON-NLS-1$
		label.setLayoutData(new GridData());
		
		fSuperclassCombo= new Combo(parent, SWT.READ_ONLY);
		fSuperclasses= getPullUpRefactoring().getPossibleTargetClasses(pm);
		Assert.isTrue(fSuperclasses.length > 0);
		for (int i= 0; i < fSuperclasses.length; i++) {
			String comboLabel= JavaModelUtil.getFullyQualifiedName(fSuperclasses[i]);
			fSuperclassCombo.add(comboLabel);
		}
		fSuperclassCombo.select(fSuperclasses.length - 1);
		fSuperclassCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	}

	private void createMemberTableComposite(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		GridData gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan= 2;
		composite.setLayoutData(gd);
		GridLayout gl= new GridLayout();
		gl.numColumns= 2;
		gl.marginWidth= 0;
		gl.marginHeight= 0;
		composite.setLayout(gl);

		createMemberTable(composite);
		createButtonComposite(composite);
	}

	private void createMemberTableLabel(Composite parent) {
		Label label= new Label(parent, SWT.NONE) ;
		label.setText(RefactoringMessages.getString("PullUpInputPage1.Specify_actions")); //$NON-NLS-1$
		GridData gd0= new GridData();
		gd0.horizontalSpan= 2;
		label.setLayoutData(gd0);
	}
	
	private void createButtonComposite(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(GridData.FILL_VERTICAL));
		GridLayout gl= new GridLayout();
		gl.marginHeight= 0;
		gl.marginWidth= 0;
		composite.setLayout(gl);
		
		fEditButton= new Button(composite, SWT.PUSH);
		fEditButton.setText(RefactoringMessages.getString("PullUpInputPage1.Edit")); //$NON-NLS-1$
		fEditButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fEditButton.setEnabled(false);
		SWTUtil.setButtonDimensionHint(fEditButton);
		fEditButton.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent event) {
				PullUpInputPage1.this.editSelectedMembers();
			}
		});

		Button addButton= new Button(composite, SWT.PUSH);
		addButton.setText(RefactoringMessages.getString("PullUpInputPage1.Add_Required")); //$NON-NLS-1$
		addButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		SWTUtil.setButtonDimensionHint(addButton);
		addButton.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent event) {
				PullUpInputPage1.this.markAdditionalRequiredMembersAsMembersToPullUp();
			}
		});
	}

	private void editSelectedMembers() {
		ISelection preserved= fTableViewer.getSelection();
		try{
			String shellTitle= RefactoringMessages.getString("PullUpInputPage1.Edit_members"); //$NON-NLS-1$
			String labelText= RefactoringMessages.getString("PullUpInputPage1.Mark_selected_members"); //$NON-NLS-1$
			Map stringMapping= createStringMappingForSelectedMembers();
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

	private static void setInfoAction(MemberActionInfo[] infos, int action) {
		for (int i = 0; i < infos.length; i++) {
			infos[i].setAction(action);
		}
	}

	//String -> Integer
	private Map createStringMappingForSelectedMembers() {
		Map result= new HashMap();
		putToStingMapping(result, MemberActionInfo.METHOD_LABELS, MemberActionInfo.PULL_UP_ACTION);
		putToStingMapping(result, MemberActionInfo.METHOD_LABELS, MemberActionInfo.DECLARE_ABSTRACT_ACTION);
		return result;
	}
	private static void putToStingMapping(Map result, String[] actionLabels, int actionIndex){
		result.put(actionLabels[actionIndex], new Integer(actionIndex));
	}

	private void updateUIElements(ISelection preserved) {
		fTableViewer.refresh();
		if (preserved != null){
			fTableViewer.getControl().setFocus();
			fTableViewer.setSelection(preserved);
		}
		checkPageCompletionStatus();
		updateButtonEnablementState(fTableViewer.getSelection());
		updateStatusLine();
	}

	private void updateStatusLine(){
		if (fStatusLine == null)
			return;
		int selected= fTableViewer.getCheckedElements().length;
		String[] keys= {String.valueOf(selected)};
		String msg= RefactoringMessages.getFormattedString("PullUpInputPage1.status_line", keys); //$NON-NLS-1$
		fStatusLine.setText(msg);
	}

	private static int countEditableInfos(MemberActionInfo[] infos) {
		int result= 0;
		for (int i= 0; i < infos.length; i++) {
			MemberActionInfo info= infos[i];
			if (info.isEditable())
				result++;
		}
		return result;
	}
	
	private MemberActionInfo[] getSelectedMemberActionInfos() {
		Assert.isTrue(fTableViewer.getSelection() instanceof IStructuredSelection);
		IStructuredSelection ss= (IStructuredSelection)fTableViewer.getSelection();
		List result= ss.toList();
		return (MemberActionInfo[]) result.toArray(new MemberActionInfo[result.size()]);
	}

	private void markAdditionalRequiredMembersAsMembersToPullUp() {
		try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().run(false, false, new IRunnableWithProgress() {
				public void run(IProgressMonitor pm) throws InvocationTargetException {
					try {
						markAsMembersToPullUp(getPullUpRefactoring().getAdditionalRequiredMembersToPullUp(pm));						
					} catch (JavaModelException e) {
						throw new InvocationTargetException(e);
					} finally {
						pm.done();
					}
				}
			});
		} catch(InvocationTargetException e) {
			ExceptionHandler.handle(e, getShell(), RefactoringMessages.getString("PullUpInputPage.pull_Up"), RefactoringMessages.getString("PullUpInputPage.exception")); //$NON-NLS-1$ //$NON-NLS-2$
		} catch(InterruptedException e) {
			Assert.isTrue(false);//not cancellable
		}
	}

	private void createMemberTable(Composite parent) {
		TableLayoutComposite layouter= new TableLayoutComposite(parent, SWT.NONE);
		layouter.addColumnData(new ColumnWeightData(60, true));
		layouter.addColumnData(new ColumnWeightData(40, true));

		final Table table= new Table(layouter, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION | SWT.CHECK);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		
		GridData gd= new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		gd.heightHint= table.getHeaderHeight() + (table.getGridLineWidth() + table.getItemHeight()) * ROW_COUNT;
		layouter.setLayoutData(gd);

				
		TableLayout tableLayout= new TableLayout();
		table.setLayout(tableLayout);

		TableColumn column0= new TableColumn(table, SWT.NONE);		
		column0.setText(RefactoringMessages.getString("PullUpInputPage1.Member")); //$NON-NLS-1$

		TableColumn column1= new TableColumn(table, SWT.NONE);
		column1.setText(RefactoringMessages.getString("PullUpInputPage1.Action")); //$NON-NLS-1$
		
		fTableViewer= new PullPushCheckboxTableViewer(table);
		fTableViewer.setUseHashlookup(true);
		fTableViewer.setContentProvider(new ArrayContentProvider());
		fTableViewer.setLabelProvider(new MemberActionInfoLabelProvider());
		fTableViewer.addSelectionChangedListener(new ISelectionChangedListener(){
			public void selectionChanged(SelectionChangedEvent event) {
				updateButtonEnablementState(event.getSelection());
			}
		});
		fTableViewer.addCheckStateListener(new ICheckStateListener(){
			public void checkStateChanged(CheckStateChangedEvent event) {
				boolean checked= event.getChecked();
				MemberActionInfo info= (MemberActionInfo)event.getElement();
				if (checked)
					info.setAction(MemberActionInfo.PULL_UP_ACTION);
				else
					info.setAction(MemberActionInfo.NO_ACTION);
				updateUIElements(null);
			}
		});
		
		setTableInput();
		markAsMembersToPullUp(getPullUpRefactoring().getMembersToPullUp());
		setupCellEditors(table);
	}

	private void updateButtonEnablementState(ISelection tableSelection) {
		if (fEditButton != null)
			fEditButton.setEnabled(enableEditButton((IStructuredSelection)tableSelection));
		fCreateStubsButton.setEnabled(getMethodsToDeclareAbstract().length != 0);
	}

	private boolean enableEditButton(IStructuredSelection ss) {
		if (ss.isEmpty() || ss.size() == 0)
			return false;
		return ss.size() == countEditableInfos(getSelectedMemberActionInfos());
	}

	private void setTableInput() {
		fTableViewer.setInput(convertPullableMemberToMemberActionInfoArray());
	}

	private MemberActionInfo[] convertPullableMemberToMemberActionInfoArray() {
		List toPullUp= Arrays.asList(getPullUpRefactoring().getMembersToPullUp());
		IMember[] members= getPullUpRefactoring().getPullableMembersOfDeclaringType();
		MemberActionInfo[] result= new MemberActionInfo[members.length];
		for (int i= 0; i < members.length; i++) {
			IMember member= members[i];
			if (toPullUp.contains(member))
				result[i]= new MemberActionInfo(member, MemberActionInfo.PULL_UP_ACTION);
			else
				result[i]= new MemberActionInfo(member, MemberActionInfo.NO_ACTION);
		}
		return result;
	}

	private void setupCellEditors(final Table table) {
		final ComboBoxCellEditor comboBoxCellEditor= new ComboBoxCellEditor();
		comboBoxCellEditor.setStyle(SWT.READ_ONLY);
		fTableViewer.setCellEditors(new CellEditor [] {null, comboBoxCellEditor});
		fTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				if (comboBoxCellEditor.getControl() == null & ! table.isDisposed())
					comboBoxCellEditor.create(table);
				ISelection sel= event.getSelection();
				if (! (sel instanceof IStructuredSelection))
					return;
				IStructuredSelection ss= (IStructuredSelection)sel;
				if (ss.size() != 1)
					return;
				MemberActionInfo mac= (MemberActionInfo)ss.getFirstElement();
				comboBoxCellEditor.setItems(mac.getAllowedLabels());
				comboBoxCellEditor.setValue(new Integer(mac.getAction()));
			}
		});
		
		ICellModifier cellModifier = new PullUpCellModifier();
		fTableViewer.setCellModifier(cellModifier);
		fTableViewer.setColumnProperties(new String[] {MEMBER_PROPERTY, ACTION_PROPERTY});
	}
		
	private void checkPageCompletionStatus() {
		if (areAllMembersMarkedAsWithNoAction()){
			setErrorMessage(RefactoringMessages.getString("PullUpInputPage1.Select_members_to_pull_up")); //$NON-NLS-1$
			setPageComplete(false);
		} else {
			setErrorMessage(null);
			setPageComplete(true);
		}
	}

	private boolean areAllMembersMarkedAsWithNoAction() {
		return getMembersWithNoAction().length == getTableInputAsMemberActionInfoArray().length;
	}

	/*
	 * @see org.eclipse.jface.wizard.IWizardPage#getNextPage()
	 */
	public IWizardPage getNextPage() {
		initializeRefactoring();
		if (canSkipSecondInputPage())
			return getRefactoringWizard().computeUserInputSuccessorPage(this);
		else 
			return super.getNextPage();
	}
	
	/*
	 * @see org.eclipse.jface.wizard.IWizardPage#canFlipToNextPage()
	 */
	public boolean canFlipToNextPage() {
		if (canSkipSecondInputPage())
		    //cannot call super here because it tries to compute successor page, which is expensive
			return isPageComplete();
		else
			return super.canFlipToNextPage();
	}

	private boolean canSkipSecondInputPage() {
		return getMethodsToPullUp().length == 0;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.refactoring.RefactoringWizardPage#performFinish()
	 */
	protected boolean performFinish() {
		initializeRefactoring();
		return super.performFinish();
	}
	
	private void initializeRefactoring() {
		getPullUpRefactoring().setMembersToPullUp(getMembersToPullUp());
		getPullUpRefactoring().setMethodsToDeclareAbstract(getMethodsToDeclareAbstract());
		getPullUpRefactoring().setTargetClass(getSelectedClass());
		getPullUpRefactoring().setCreateMethodStubs(fCreateStubsButton.getSelection());
		getPullUpRefactoring().setMethodsToDelete(getMethodsToPullUp());
	}
	
	private IType getSelectedClass() {
		return fSuperclasses[fSuperclassCombo.getSelectionIndex()];
	}

	private void markAsMembersToPullUp(IMember[] elements){
		setActionForMembers(elements, MemberActionInfo.PULL_UP_ACTION);
		updateUIElements(null);
	}
		
	private void setActionForMembers(IMember[] members, int action){
		MemberActionInfo[] macs= getTableInputAsMemberActionInfoArray();
		for (int i = 0; i < members.length; i++) {
			for (int j = 0; j < macs.length; j++) {
				if (macs[j].getMember().equals(members[i]))
					macs[j].setAction(action); 
			}
		}
	}	
	
	private MemberActionInfo[] getTableInputAsMemberActionInfoArray() {
		return (MemberActionInfo[])fTableViewer.getInput();
	}

	private IMethod[] getMethodsToPullUp() {
		return getMethodsForAction(MemberActionInfo.PULL_UP_ACTION);
	}

	private IMethod[] getMethodsToDeclareAbstract() {
		return getMethodsForAction(MemberActionInfo.DECLARE_ABSTRACT_ACTION);
	}
	
	private IMethod[] getMethodsForAction(int action) {
		MemberActionInfo[] macs= getTableInputAsMemberActionInfoArray();
		List list= new ArrayList(macs.length);
		for (int i= 0; i < macs.length; i++) {
			if (macs[i].isMethodInfo() && macs[i].getAction() == action){
				list.add(macs[i].getMember());
			}		
		}
		return (IMethod[]) list.toArray(new IMethod[list.size()]);
	}

	private IMember[] getMembersWithNoAction(){
		return getMembersForAction(MemberActionInfo.NO_ACTION);
	}
	
	private IMember[] getMembersToPullUp() {
		return getMembersForAction(MemberActionInfo.PULL_UP_ACTION);
	}
	
	private IMember[] getMembersForAction(int action) {
		MemberActionInfo[] macs= getTableInputAsMemberActionInfoArray();
		List result= new ArrayList(macs.length);
		for (int i = 0; i < macs.length; i++) {
			if (macs[i].getAction() == action)
				result.add(macs[i].getMember());
		}
		return (IMember[]) result.toArray(new IMember[result.size()]);
	}
}
