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
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.SWTUtil;
import org.eclipse.jdt.internal.ui.util.TableLayoutComposite;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.structure.PullUpRefactoring;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;

class PullUpInputPage1 extends UserInputWizardPage {
	
	private static final int ROW_COUNT = 10;
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
				PullUpInputPage1.this.updateUIElements(null);
			}
		}
	}

	private static class MemberActionInfo {
		static final int NO_ACTION= 				0;
		static final int PULL_UP_ACTION= 			1;
		static final int DECLARE_ABSTRACT_ACTION= 2;
		private static final String[] ALL_LABELS;     //indices in this array correspond to action numbers
		private static final String[] LIMITED_LABELS; //indices in this array correspond to action numbers
		private static final String NONE_LABEL= "none";
		private static final String PULL_UP_LABEL= "pull up";
		private static final String DECLARE_ABSTRACT_LABEL= "declare abstract in destination";
		static{
			ALL_LABELS= new String[3];
			ALL_LABELS[NO_ACTION]= NONE_LABEL;
			ALL_LABELS[PULL_UP_ACTION]= PULL_UP_LABEL;
			ALL_LABELS[DECLARE_ABSTRACT_ACTION]= DECLARE_ABSTRACT_LABEL;

			LIMITED_LABELS= new String[2];
			LIMITED_LABELS[NO_ACTION]= NONE_LABEL;
			LIMITED_LABELS[PULL_UP_ACTION]= PULL_UP_LABEL;
		}
				
		private final IMember fMember;
		private int fAction;
		
		MemberActionInfo(IMember member, int action){
			Assert.isTrue(member instanceof IMethod || member instanceof IField);
			Assert.isTrue(action == NO_ACTION || action == DECLARE_ABSTRACT_ACTION || action == PULL_UP_ACTION);
			fMember= member;
			fAction= action;
		}
		
		IMember getMember(){
			return fMember;
		}
		
		int getAction(){
			return fAction;
		}
		
		void setAction(int action){
			Assert.isTrue(action == NO_ACTION || action == DECLARE_ABSTRACT_ACTION || action == PULL_UP_ACTION);
			fAction= action;
		}
		
		String getActionLabel(){
			if (canDeclareAbstract())
				return ALL_LABELS[getAction()];
			else
				return LIMITED_LABELS[getAction()];
		}

		boolean canDeclareAbstract() { //XXX kind of bogus to have it here
			try {
				if (fMember instanceof IField)
					return false;
				else 
					return ! JdtFlags.isAbstract(fMember);
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
				return false;
			}
		}

		String[] getAllowedLabels() {
			if (canDeclareAbstract())
				return ALL_LABELS;		
			else
				return LIMITED_LABELS;
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
	
	public static final String PAGE_NAME= "PullUpMethodsInputPage1"; //$NON-NLS-1$
	private final static String ACTION_PROPERTY= "action"; //$NON-NLS-1$	
	private final static String MEMBER_PROPERTY= "member"; //$NON-NLS-1$	

	private TableViewer fTableViewer;
	private Combo fSuperclassCombo;
	private IType[] fSuperclasses;
	private Button fEditButton;

	private Button fCreateStubsButton;
	public PullUpInputPage1() {
		super(PAGE_NAME, false);
		setMessage("Select the members to pull up and the desired new declaring class for them.\n" +							"If you select methods, them press Next to specify which matching methods you wish to delete.");
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
				
		setControl(composite);
		WorkbenchHelp.setHelp(getControl(), IJavaHelpContextIds.PULL_UP_WIZARD_PAGE);			
	}
	
	private void createStubCheckbox(Composite parent) {
		fCreateStubsButton= new Button(parent, SWT.CHECK);
		fCreateStubsButton.setText("&Create necessary methods stubs in non-abstract subclasses of the destination class");
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
		label.setText("&Select destination class:");
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
		label.setText("&Specify actions for members:");
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
		fEditButton.setText("&Edit...");
		fEditButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fEditButton.setEnabled(false);
		SWTUtil.setButtonDimensionHint(fEditButton);
		fEditButton.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent event) {
				PullUpInputPage1.this.editSelectedMembers();
			}
		});

		Button addButton= new Button(composite, SWT.PUSH);
		addButton.setText("&Pull Up Required Members");
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
			String shellTitle= "Edit members";
			String labelText= "&Mark selected member(s) as:";
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
		if (canAllowAllChoices()){
			putToStingMapping(result, MemberActionInfo.ALL_LABELS, MemberActionInfo.NO_ACTION);
			putToStingMapping(result, MemberActionInfo.ALL_LABELS, MemberActionInfo.PULL_UP_ACTION);
			putToStingMapping(result, MemberActionInfo.ALL_LABELS, MemberActionInfo.DECLARE_ABSTRACT_ACTION);
		} else{
			putToStingMapping(result, MemberActionInfo.LIMITED_LABELS, MemberActionInfo.NO_ACTION);
			putToStingMapping(result, MemberActionInfo.LIMITED_LABELS, MemberActionInfo.PULL_UP_ACTION);
		}
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
	}

	private boolean canAllowAllChoices() {
		MemberActionInfo[] selected= getSelectedMemberActionInfos();
		for (int i = 0; i < selected.length; i++) {
			if (! selected[i].canDeclareAbstract())
				return false;
		}
		return true;
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
				updateButtonEnablementState(event.getSelection());
			}
		});
		
		setTableInput();
		markAsMembersToPullUp(getPullUpRefactoring().getMembersToPullUp());
		setupCellEditors(table);
	}

	private void updateButtonEnablementState(ISelection tableSelection) {
		if (tableSelection instanceof IStructuredSelection){
			IStructuredSelection ss= (IStructuredSelection)tableSelection;
			if (fEditButton != null)
				fEditButton.setEnabled(! ss.isEmpty() && ss.size() != 0);
		}
		fCreateStubsButton.setEnabled(getMethodsToDeclareAbstract().length != 0);
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
			setErrorMessage("Select member(s) to pull up or declare abstract");
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
		//on finish, we have to do more
		getPullUpRefactoring().setMethodsToDelete(getMethodsToPullUp());
		return super.performFinish();
	}
	
	private void initializeRefactoring() {
		getPullUpRefactoring().setMembersToPullUp(getMembersToPullUp());
		getPullUpRefactoring().setMethodsToDeclareAbstract(getMethodsToDeclareAbstract());
		getPullUpRefactoring().setTargetClass(getSelectedClass());
		getPullUpRefactoring().setCreateMethodStubs(fCreateStubsButton.getSelection());
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
			if (macs[i].getAction() == action){
				IMember member= macs[i].getMember();
				if (member instanceof IMethod)
					list.add(member);
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
