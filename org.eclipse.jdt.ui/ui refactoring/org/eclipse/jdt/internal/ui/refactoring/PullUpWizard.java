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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
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
import org.eclipse.swt.widgets.Tree;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
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
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizardPage;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.structure.IMemberActionInfo;
import org.eclipse.jdt.internal.corext.refactoring.structure.PullUpRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.javaeditor.JavaSourceViewer;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.SWTUtil;
import org.eclipse.jdt.internal.ui.util.TableLayoutComposite;

import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaElementSorter;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;
import org.eclipse.jdt.ui.text.JavaTextTools;

public class PullUpWizard extends RefactoringWizard {

	public PullUpWizard(PullUpRefactoring ref) {
		super(ref, RefactoringMessages.getString("PullUpWizard.defaultPageTitle")); //$NON-NLS-1$
		setDefaultPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_REFACTOR_PULL_UP);
	}
	
	/* non java-doc
	 * @see RefactoringWizard#addUserInputPages
	 */ 
	protected void addUserInputPages(){
		addPage(new PullUpInputPage1());
		addPage(new PullUpInputPage2());
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard#hasMultiPageUserInput()
	 */
	public boolean hasMultiPageUserInput() {
		return true;
	}
	
	private static class PullUpInputPage1 extends UserInputWizardPage {
	
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
				if (! canModify(mac, property))//workaround for 37266 (which resulted in jdt ui bug 34926)
					return;
				Assert.isTrue(mac.isMethodInfo());
				mac.setAction(action);
				PullUpInputPage1.this.updateUIElements(null, true);
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
			private static final String[] TYPE_LABELS;//indices correspond to values
			static{
				METHOD_LABELS= new String[2];
				METHOD_LABELS[PULL_UP_ACTION]= PULL_UP_LABEL;
				METHOD_LABELS[DECLARE_ABSTRACT_ACTION]= DECLARE_ABSTRACT_LABEL;

				TYPE_LABELS= new String[1];
				TYPE_LABELS[PULL_UP_ACTION]= PULL_UP_LABEL; 
			}
				
			private final IMember fMember;
			private int fAction;
		
			MemberActionInfo(IMember member, int action){
				Assert.isTrue((member instanceof IMethod) || (member instanceof IField) || (member instanceof IType));
				assertAction(member, action);
				fMember= member;
				fAction= action;
			}

			public boolean isTypeInfo() {
				return getMember() instanceof IType;
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
				if (member instanceof IMethod){
					try {
						Assert.isTrue(action != DECLARE_ABSTRACT_ACTION || ! JdtFlags.isStatic(member));
					} catch (JavaModelException e) {
						JavaPlugin.log(e); //cannot show any ui here
					}
					Assert.isTrue(
					action == NO_ACTION ||
					action == DECLARE_ABSTRACT_ACTION ||
					action == PULL_UP_ACTION);
				} else {
					Assert.isTrue(
					action == NO_ACTION ||
					action == PULL_UP_ACTION);
				}	
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
				else if (isMethodInfo())
					return METHOD_LABELS;
				else if (isTypeInfo())	
					return TYPE_LABELS;
				else {
					Assert.isTrue(false);
					return null;
				}	
			}

			public boolean isEditable() {
				if (fAction == NO_ACTION)
					return false;
				if (! isMethodInfo())
					return false;
				IMethod method= (IMethod)fMember;
				try {
					return ! JdtFlags.isStatic(method);
				} catch (JavaModelException e) {
					JavaPlugin.log(e); //no ui here
					return false;
				}
			}

			/* (non-Javadoc)
			 * @see org.eclipse.jdt.internal.corext.refactoring.structure.IMemberActionInfo#isNoAction()
			 */
			public boolean isActive() {
				return getAction() != NO_ACTION;
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
			Dialog.applyDialogFont(composite);
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
			GridData gd= new GridData(GridData.FILL_BOTH);
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
			if (! fEditButton.isEnabled())
				return;

			ISelection preserved= fTableViewer.getSelection();
			try{
				String shellTitle= RefactoringMessages.getString("PullUpInputPage1.Edit_members"); //$NON-NLS-1$
				String labelText= RefactoringMessages.getString("PullUpInputPage1.Mark_selected_members"); //$NON-NLS-1$
				Map stringMapping= createStringMappingForSelectedMembers();
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

		private void updateUIElements(ISelection preserved, boolean displayErrorMessage) {
			fTableViewer.refresh();
			if (preserved != null){
				fTableViewer.getControl().setFocus();
				fTableViewer.setSelection(preserved);
			}
			checkPageCompletionStatus(displayErrorMessage);
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
							markAsMembersToPullUp(getPullUpRefactoring().getAdditionalRequiredMembersToPullUp(pm), true);						
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
					updateUIElements(null, true);
				}
			});
			fTableViewer.addDoubleClickListener(new IDoubleClickListener(){
				public void doubleClick(DoubleClickEvent event) {
					PullUpInputPage1.this.editSelectedMembers();
				}
			});

		
			setTableInput();
			markAsMembersToPullUp(getPullUpRefactoring().getMembersToPullUp(), false);
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
		
		private void checkPageCompletionStatus(boolean displayErrorMessage) {
			if (areAllMembersMarkedAsWithNoAction()){
				if (displayErrorMessage)
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

		private void markAsMembersToPullUp(IMember[] elements, boolean displayErrorMessage){
			setActionForMembers(elements, MemberActionInfo.PULL_UP_ACTION);
			updateUIElements(null, displayErrorMessage);
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
	
		private MemberActionInfo[] getActiveInfos() {
			MemberActionInfo[] infos= getTableInputAsMemberActionInfoArray();
			List result= new ArrayList(infos.length);
			for (int i= 0; i < infos.length; i++) {
				MemberActionInfo info= infos[i];
				if (info.isActive())
					result.add(info);
			}
			return (MemberActionInfo[]) result.toArray(new MemberActionInfo[result.size()]);
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
	
	private static class PullUpInputPage2 extends UserInputWizardPage {

		private static class PullUpFilter extends ViewerFilter {
			private final Set fTypesToShow;
	
			PullUpFilter(ITypeHierarchy hierarchy, IMember[] members) {
				//IType -> IMember[]
				Map typeToMemberArray= PullUpInputPage2.createTypeToMemberArrayMapping(members);
				fTypesToShow= computeTypesToShow(hierarchy, typeToMemberArray);
			}
	
			private static Set computeTypesToShow(ITypeHierarchy hierarchy, Map typeToMemberArray) {
				Set typesToShow= new HashSet();
				typesToShow.add(hierarchy.getType());
				typesToShow.addAll(computeShowableSubtypesOfMainType(hierarchy, typeToMemberArray));
				return typesToShow;
			}
		
			private static Set computeShowableSubtypesOfMainType(ITypeHierarchy hierarchy, Map typeToMemberArray) {
				Set result= new HashSet();
				IType[] subtypes= hierarchy.getAllSubtypes(hierarchy.getType());
				for (int i= 0; i < subtypes.length; i++) {
					IType subtype= subtypes[i];
					if (canBeShown(subtype, typeToMemberArray, hierarchy))
						result.add(subtype);
				}
				return result;
			}
	
			private static boolean canBeShown(IType type, Map typeToMemberArray, ITypeHierarchy hierarchy) {
				if (typeToMemberArray.containsKey(type))
					return true;
				return anySubtypeCanBeShown(type, typeToMemberArray, hierarchy);	
			}
		
			private static boolean anySubtypeCanBeShown(IType type, Map typeToMemberArray, ITypeHierarchy hierarchy){
				IType[] subTypes= hierarchy.getSubtypes(type);
				for (int i= 0; i < subTypes.length; i++) {
					if (canBeShown(subTypes[i], typeToMemberArray, hierarchy))
						return true;
				}
				return false;
			}
	
			/*
			 * @see ViewerFilter#select(Viewer, Object, Object)
			 */
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if (element instanceof IMethod)
					return true;
				return fTypesToShow.contains(element);
			}
		}
	
		private static class PullUpHierarchyContentProvider implements ITreeContentProvider {
			private ITypeHierarchy fHierarchy;
			private Map fTypeToMemberArray; //IType -> IMember[]
			private IType fDeclaringType;
	
			PullUpHierarchyContentProvider(IType declaringType, IMember[] members) {
				fDeclaringType= declaringType;
				fTypeToMemberArray= PullUpInputPage2.createTypeToMemberArrayMapping(members);
			}
	
			/*
			 * @see ITreeContentProvider#getChildren(Object)
			 */
			public Object[] getChildren(Object parentElement) {
				if (parentElement instanceof IType)
					return getSubclassesAndMembers((IType) parentElement);
				else
					return new Object[0];
			}
	
			private Object[] getSubclassesAndMembers(IType type) {
				Set set= new HashSet();
				set.addAll(Arrays.asList(getSubclasses(type)));
				set.addAll(Arrays.asList(getMembers(type)));
				return set.toArray();
			}
	
			private IType[] getSubclasses(IType type) {
				if (type.equals(fDeclaringType))
					return new IType[0];
				return fHierarchy.getSubclasses(type);
			}
	
			private IMember[] getMembers(IType type) {
				if (fTypeToMemberArray.containsKey(type))
					return (IMember[]) (fTypeToMemberArray.get(type));
				else
					return new IMember[0];
			}
			/*
			  * @see ITreeContentProvider#getParent(Object)
			  */
			public Object getParent(Object element) {
				if (element instanceof IType)
					return fHierarchy.getSuperclass((IType) element);
				if (element instanceof IMember)
					return ((IMember) element).getDeclaringType();
				Assert.isTrue(false, "Should not get here"); //$NON-NLS-1$
				return null;
			}
	
			/*
			 * @see ITreeContentProvider#hasChildren(Object)
			 */
			public boolean hasChildren(Object element) {
				if (!(element instanceof IType))
					return false;
				IType type= (IType) element;
				return hasSubtypes(type) || hasMembers(type);
			}
		
			private boolean hasSubtypes(IType type){
				return fHierarchy.getAllSubtypes(type).length > 0;
			}
		
			private boolean hasMembers(IType type){
				return fTypeToMemberArray.containsKey(type);
			}
	
			/*
			 * @see IStructuredContentProvider#getElements(Object)
			 */
			public Object[] getElements(Object inputElement) {
				Assert.isTrue(inputElement ==null || inputElement instanceof ITypeHierarchy);
				return new IType[] { fHierarchy.getType()};
			}
	
			/*
			 * @see IContentProvider#dispose()
			 */
			public void dispose() {
				fHierarchy= null;
				fTypeToMemberArray.clear();
				fTypeToMemberArray= null;
				fDeclaringType= null;
			}
	
			/*
			 * @see IContentProvider#inputChanged(Viewer, Object, Object)
			 */
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
				Assert.isTrue(newInput ==null || newInput instanceof ITypeHierarchy);
				fHierarchy= (ITypeHierarchy) newInput;
			}
		}
	
	  private Label fTypeHierarchyLabel;
	  private SourceViewer fSourceViewer;
	  private PullUpTreeViewer fTreeViewer;
	  public static final String PAGE_NAME= "PullUpMethodsInputPage2"; //$NON-NLS-1$
	
	  public PullUpInputPage2() {
		  super(PAGE_NAME, true);
		  setMessage(RefactoringMessages.getString("PullUpInputPage.select_methods")); //$NON-NLS-1$
	  }

	  /*
	   * @see IDialogPage#createControl(Composite)
	   */
	  public void createControl(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		
		createTreeAndSourceViewer(composite);
		createButtonComposite(composite);
		setControl(composite);
		
		Dialog.applyDialogFont(composite);
		WorkbenchHelp.setHelp(getControl(), IJavaHelpContextIds.PULL_UP_WIZARD_PAGE);			
	  }

		private void createButtonComposite(Composite superComposite) {
			Composite buttonComposite= new Composite(superComposite, SWT.NONE);
			buttonComposite.setLayoutData(new GridData());
			GridLayout bcl= new GridLayout();
			bcl.numColumns= 2;
			bcl.marginWidth= 1;
			buttonComposite.setLayout(bcl);
	
			Button button= new Button(buttonComposite, SWT.PUSH);
			button.setText(RefactoringMessages.getString("PullUpInputPage2.Select")); //$NON-NLS-1$
			button.setLayoutData(new GridData());
			SWTUtil.setButtonDimensionHint(button);
			button.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					PullUpInputPage2.this.checkPulledUp();
					updateTypeHierarchyLabel();
				}
			});
		}
	
		public void checkPulledUp() {
			uncheckAll();
			fTreeViewer.setCheckedElements(getPullUpMethodsRefactoring().getMembersToPullUp());
			IType parent= getPullUpMethodsRefactoring().getDeclaringType();
			fTreeViewer.setChecked(parent, true);
			checkAllParents(parent);
		}
	
		private void checkAllParents(IType parent) {
			ITypeHierarchy th= getTreeViewerInput();
			IType root= getTreeViewerInputRoot();
			IType type= parent;
			while (! root.equals(type)){
				fTreeViewer.setChecked(type, true);
				type= th.getSuperclass(type);			
			}
			fTreeViewer.setChecked(root, true);
		}

		private void uncheckAll() {
			IType root= getTreeViewerInputRoot();
			fTreeViewer.setSubtreeChecked(root, false);
			fTreeViewer.setSubtreeGrayed(root, false);
		}
	
		private IType getTreeViewerInputRoot() {
			return getTreeViewerInput().getType();
		}
	
		private ITypeHierarchy getTreeViewerInput() {
			return (ITypeHierarchy)fTreeViewer.getInput();
		}
	
	  private void updateTypeHierarchyLabel(){
		  String message= RefactoringMessages.getFormattedString("PullUpInputPage.hierarchyLabal", //$NON-NLS-1$
						  new Integer(getCheckedMethods().length).toString());
		  setMessage(message, IMessageProvider.INFORMATION);
	  }	
	
	  private void createTreeAndSourceViewer(Composite superComposite) {
		  SashForm composite= new SashForm(superComposite, SWT.HORIZONTAL);
		  initializeDialogUnits(superComposite);
		  GridData gd = new GridData(GridData.FILL_BOTH);
		  gd.heightHint= convertHeightInCharsToPixels(20);
		  gd.widthHint= convertWidthInCharsToPixels(10);
		  composite.setLayoutData(gd);
		  GridLayout layout= new GridLayout();
		  layout.numColumns= 2; 
		  layout.marginWidth= 0; 
		  layout.marginHeight= 0;
		  layout.horizontalSpacing= 1;
		  layout.verticalSpacing= 1;
		  composite.setLayout(layout);
		
		  createHierarchyTreeComposite(composite);
		  createSourceViewerComposite(composite);
		  composite.setWeights(new int[]{50, 50});
	  }

		public void setVisible(boolean visible) {
			if (visible) {
				initializeTreeViewer();
				setHierarchyLabelText();
			}
			super.setVisible(visible);
		}

		private void initializeTreeViewer() {
			try {
				getContainer().run(false, false, new IRunnableWithProgress() {
					public void run(IProgressMonitor pm) {
						try {
							initializeTreeViewer(pm);
						} finally {
							pm.done();
						}
					}
				});
			} catch (InvocationTargetException e) {
				ExceptionHandler.handle(e, getShell(), RefactoringMessages.getString("PullUpInputPage.pull_Up"), RefactoringMessages.getString("PullUpInputPage.exception")); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (InterruptedException e) {
				Assert.isTrue(false); //not cancellable
			}
		}

	  /*
	   * @see IWizardPage#getNextPage()
	   */	
		public IWizardPage getNextPage() {
			initializeRefactoring();
			return super.getNextPage();
		}

		/*
		 * @see RefactoringWizardPage#performFinish()
		 */
		protected boolean performFinish() {
			initializeRefactoring();
			return super.performFinish();
		}

		private void initializeRefactoring() {
			getPullUpMethodsRefactoring().setMethodsToDelete(getCheckedMethods());
		} 
	
	  private IMethod[] getCheckedMethods(){
		  Object[] checked= fTreeViewer.getCheckedElements();
		  List members= new ArrayList(checked.length);
		  for (int i= 0; i < checked.length; i++) {
			  if (checked[i] instanceof IMethod)
				  members.add(checked[i]);
		  }
		  return (IMethod[]) members.toArray(new IMethod[members.size()]);
	  }
	
		private void createSourceViewerComposite(Composite parent) {
			Composite c= new Composite(parent, SWT.NONE);
			c.setLayoutData(new GridData(GridData.FILL_BOTH));
			GridLayout layout= new GridLayout();
			layout.marginWidth= 0;
			layout.marginHeight= 0;
			layout.horizontalSpacing= 1;
			layout.verticalSpacing= 1;
			c.setLayout(layout);
	
			createSourceViewerLabel(c);
			createSourceViewer(c);
		}
	
		private void createSourceViewer(Composite c) {
			  fSourceViewer= new JavaSourceViewer(c, null, null, false, SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION);
			  fSourceViewer.configure(new JavaSourceViewerConfiguration(getJavaTextTools(), null));
			  fSourceViewer.setEditable(false);
			  fSourceViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
			  fSourceViewer.getControl().setFont(JFaceResources.getFont(PreferenceConstants.EDITOR_TEXT_FONT));			
		}
	
		private void createSourceViewerLabel(Composite c) {
			  Label label= new Label(c, SWT.WRAP);
			  GridData gd= new GridData(GridData.FILL_HORIZONTAL);
			  label.setText(RefactoringMessages.getString("PullUpInputPage2.Source")); //$NON-NLS-1$
			  label.setLayoutData(gd);
		}
	
		private void createHierarchyTreeComposite(Composite parent){
		  Composite composite= new Composite(parent, SWT.NONE);
		  composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		  GridLayout layout= new GridLayout();
		  layout.marginWidth= 0; 
		  layout.marginHeight= 0;
		  layout.horizontalSpacing= 1;
		  layout.verticalSpacing= 1;
		  composite.setLayout(layout);

		  createTypeHierarchyLabel(composite);	
		  createTreeViewer(composite);
	  }
  
		private void createTypeHierarchyLabel(Composite composite) {
			fTypeHierarchyLabel= new Label(composite, SWT.WRAP);
			GridData gd= new GridData(GridData.FILL_HORIZONTAL);
			fTypeHierarchyLabel.setLayoutData(gd);
		}
	
		private void setHierarchyLabelText() {
			  String message= RefactoringMessages.getFormattedString("PullUpInputPage.subtypes", getSupertypeSignature()); //$NON-NLS-1$
			  fTypeHierarchyLabel.setText(message);
		}
	
		private String getSupertypeSignature(){
				return JavaElementUtil.createSignature(getPullUpMethodsRefactoring().getTargetClass());
		}

		private void createTreeViewer(Composite composite) {
			Tree tree= new Tree(composite, SWT.CHECK | SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
			tree.setLayoutData(new GridData(GridData.FILL_BOTH));
			fTreeViewer= new PullUpTreeViewer(tree);
			fTreeViewer.setLabelProvider(new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT));
			fTreeViewer.setUseHashlookup(true);
			fTreeViewer.setSorter(new JavaElementSorter());	
			fTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
				public void selectionChanged(SelectionChangedEvent event) {
					PullUpInputPage2.this.treeViewerSelectionChanged(event);
				}
			});
			fTreeViewer.addCheckStateListener(new ICheckStateListener() {
				public void checkStateChanged(CheckStateChangedEvent event) {
					PullUpInputPage2.this.updateTypeHierarchyLabel();
				}
			});
		}
	
		private void precheckElements(final PullUpTreeViewer treeViewer) {
			IMember[] members= getPullUpMethodsRefactoring().getMembersToPullUp();
			for (int i= 0; i < members.length; i++) {
				treeViewer.setCheckState(members[i], true);
			}
		}

		private void initializeTreeViewer(IProgressMonitor pm) {
			try {
				IMember[] matchingMethods= getPullUpMethodsRefactoring().getMatchingElements(new SubProgressMonitor(pm, 1), false);
				ITypeHierarchy hierarchy= getPullUpMethodsRefactoring().getTypeHierarchyOfTargetClass(new SubProgressMonitor(pm, 1));
				removeAllTreeViewFilters();
				fTreeViewer.addFilter(new PullUpFilter(hierarchy, matchingMethods));
				fTreeViewer.setContentProvider(new PullUpHierarchyContentProvider(getPullUpMethodsRefactoring().getDeclaringType(), matchingMethods));
				fTreeViewer.setInput(hierarchy);
				precheckElements(fTreeViewer);
				fTreeViewer.expandAll();
				updateTypeHierarchyLabel();
			} catch (JavaModelException e) {
				ExceptionHandler.handle(e, RefactoringMessages.getString("PullUpInputPage.pull_up1"), RefactoringMessages.getString("PullUpInputPage.exception")); //$NON-NLS-1$ //$NON-NLS-2$
				fTreeViewer.setInput(null);
			}
		}
	
		private void removeAllTreeViewFilters() {
			ViewerFilter[] filters= fTreeViewer.getFilters();
			for (int i= 0; i < filters.length; i++) {
				fTreeViewer.removeFilter(filters[i]);
			}
		}

	  private void treeViewerSelectionChanged(SelectionChangedEvent event) {
		  try{	
			  showInSourceViewer(getFirstSelectedSourceReference(event));
		  } catch (JavaModelException e){
			  ExceptionHandler.handle(e, RefactoringMessages.getString("PullUpInputPage.pull_up1"), RefactoringMessages.getString("PullUpInputPage.see_log")); //$NON-NLS-1$ //$NON-NLS-2$
		  }
	  }

		private ISourceReference getFirstSelectedSourceReference(SelectionChangedEvent event){
			ISelection s= event.getSelection();
			if (!(s instanceof IStructuredSelection))
				return null;
			IStructuredSelection ss= (IStructuredSelection)s;
			if (ss.size() != 1)
				return null;	
			Object first= ss.getFirstElement();
			if (! (first instanceof ISourceReference))
				return null;
			return (ISourceReference)first;
		}
	
	  private void setSourceViewerContents(String contents) {
		  IDocument document= (contents == null) ? new Document(): new Document(contents);
		  getJavaTextTools().setupJavaDocumentPartitioner(document);
		  fSourceViewer.setDocument(document);
	  }
	
	  private void showInSourceViewer(ISourceReference selected) throws JavaModelException{
		if (selected == null)
			setSourceViewerContents(null);
		else		
			setSourceViewerContents(selected.getSource());
	  }
	
	  private static JavaTextTools getJavaTextTools() {
		  return JavaPlugin.getDefault().getJavaTextTools();	
	  }
	
		//IType -> IMember[]
		private static Map createTypeToMemberArrayMapping(IMember[] members) {
			Map typeToMemberSet= createTypeToMemberSetMapping(members);
	
			Map typeToMemberArray= new HashMap();
			for (Iterator iter= typeToMemberSet.keySet().iterator(); iter.hasNext();) {
				IType type= (IType) iter.next();
				Set memberSet= (Set) typeToMemberSet.get(type);
				IMember[] memberArray= (IMember[]) memberSet.toArray(new IMember[memberSet.size()]);
				typeToMemberArray.put(type, memberArray);
			}
			return typeToMemberArray;
		}
	
		//	IType -> Set of IMember
		private static Map createTypeToMemberSetMapping(IMember[] members) {
			Map typeToMemberSet= new HashMap();
			for (int i= 0; i < members.length; i++) {
				IMember member= members[i];
				IType type= member.getDeclaringType();
				if (! typeToMemberSet.containsKey(type))
					typeToMemberSet.put(type, new HashSet());
				((Set) typeToMemberSet.get(type)).add(member);
			}
			return typeToMemberSet;
		}
	
	  private PullUpRefactoring getPullUpMethodsRefactoring(){
		  return (PullUpRefactoring)getRefactoring();
	  }
	}
}
