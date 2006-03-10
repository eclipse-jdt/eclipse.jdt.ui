/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
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

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.SourceViewer;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.structure.IMemberActionInfo;
import org.eclipse.jdt.internal.corext.refactoring.structure.PullUpRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.structure.PullUpRefactoringProcessor;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.Messages;
import org.eclipse.jdt.internal.corext.util.Strings;

import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaElementSorter;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;
import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.javaeditor.JavaSourceViewer;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.PixelConverter;
import org.eclipse.jdt.internal.ui.util.SWTUtil;
import org.eclipse.jdt.internal.ui.util.TableLayoutComposite;

public final class PullUpWizard extends RefactoringWizard {

	private static class PullUpInputPage1 extends UserInputWizardPage {

		private static class MemberActionInfo implements IMemberActionInfo {

			static final int DECLARE_ABSTRACT_ACTION= 1;// values are important here

			private static final String DECLARE_ABSTRACT_LABEL= RefactoringMessages.PullUpInputPage1_declare_abstract;

			private static final String NO_LABEL= ""; //$NON-NLS-1$ 

			private static final String[] FIELD_LABELS= { NO_LABEL};

			private static final String[] METHOD_LABELS;// indices correspond to values

			static final int NO_ACTION= 2;

			static final int PULL_UP_ACTION= 0;// values are important here

			private static final String PULL_UP_LABEL= RefactoringMessages.PullUpInputPage1_pull_up;

			private static final String[] TYPE_LABELS;// indices correspond to values
			static {
				METHOD_LABELS= new String[2];
				METHOD_LABELS[PULL_UP_ACTION]= PULL_UP_LABEL;
				METHOD_LABELS[DECLARE_ABSTRACT_ACTION]= DECLARE_ABSTRACT_LABEL;

				TYPE_LABELS= new String[1];
				TYPE_LABELS[PULL_UP_ACTION]= PULL_UP_LABEL;
			}

			private static void assertAction(final IMember member, final int action) {
				if (member instanceof IMethod) {
					try {
						Assert.isTrue(action != DECLARE_ABSTRACT_ACTION || !JdtFlags.isStatic(member));
					} catch (JavaModelException e) {
						JavaPlugin.log(e); // cannot show any ui here
					}
					Assert.isTrue(action == NO_ACTION || action == DECLARE_ABSTRACT_ACTION || action == PULL_UP_ACTION);
				} else {
					Assert.isTrue(action == NO_ACTION || action == PULL_UP_ACTION);
				}
			}

			private int fAction;

			private final IMember fMember;

			MemberActionInfo(final IMember member, final int action) {
				Assert.isTrue((member instanceof IMethod) || (member instanceof IField) || (member instanceof IType));
				assertAction(member, action);
				fMember= member;
				fAction= action;
			}

			int getAction() {
				return fAction;
			}

			String getActionLabel() {
				switch (fAction) {
					case PULL_UP_ACTION:
						return PULL_UP_LABEL;
					case DECLARE_ABSTRACT_ACTION:
						return DECLARE_ABSTRACT_LABEL;
					case NO_ACTION:
						return NO_LABEL;
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

			IMember getMember() {
				return fMember;
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.jdt.internal.corext.refactoring.structure.IMemberActionInfo#isNoAction()
			 */
			public boolean isActive() {
				return getAction() != NO_ACTION;
			}

			public boolean isEditable() {
				if (fAction == NO_ACTION)
					return false;
				if (!isMethodInfo())
					return false;
				final IMethod method= (IMethod) fMember;
				try {
					return !JdtFlags.isStatic(method);
				} catch (JavaModelException e) {
					JavaPlugin.log(e); // no ui here
					return false;
				}
			}

			public boolean isFieldInfo() {
				return getMember() instanceof IField;
			}

			public boolean isMethodInfo() {
				return getMember() instanceof IMethod;
			}

			public boolean isTypeInfo() {
				return getMember() instanceof IType;
			}

			void setAction(final int action) {
				assertAction(fMember, action);
				fAction= action;
			}
		}

		private static class MemberActionInfoLabelProvider extends LabelProvider implements ITableLabelProvider {

			private final ILabelProvider fJavaElementLabelProvider= new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT | JavaElementLabelProvider.SHOW_SMALL_ICONS);

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
			public Image getColumnImage(final Object element, final int columnIndex) {
				final MemberActionInfo mac= (MemberActionInfo) element;
				switch (columnIndex) {
					case MEMBER_COLUMN:
						return fJavaElementLabelProvider.getImage(mac.getMember());
					case ACTION_COLUMN:
						return null;
					default:
						Assert.isTrue(false);
						return null;
				}
			}

			/*
			 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object, int)
			 */
			public String getColumnText(final Object element, final int columnIndex) {
				final MemberActionInfo mac= (MemberActionInfo) element;
				switch (columnIndex) {
					case MEMBER_COLUMN:
						return fJavaElementLabelProvider.getText(mac.getMember());
					case ACTION_COLUMN:
						return mac.getActionLabel();
					default:
						Assert.isTrue(false);
						return null;
				}
			}
		}

		private class PullUpCellModifier implements ICellModifier {

			public boolean canModify(final Object element, final String property) {
				if (!ACTION_PROPERTY.equals(property))
					return false;

				return ((MemberActionInfo) element).isEditable();
			}

			public Object getValue(final Object element, final String property) {
				if (!ACTION_PROPERTY.equals(property))
					return null;

				final MemberActionInfo mac= (MemberActionInfo) element;
				return new Integer(mac.getAction());
			}

			public void modify(final Object element, final String property, final Object value) {
				if (!ACTION_PROPERTY.equals(property))
					return;
				final int action= ((Integer) value).intValue();
				MemberActionInfo mac;
				if (element instanceof Item) {
					mac= (MemberActionInfo) ((Item) element).getData();
				} else
					mac= (MemberActionInfo) element;
				if (!canModify(mac, property))// workaround for 37266 (which resulted in jdt ui bug 34926)
					return;
				Assert.isTrue(mac.isMethodInfo());
				mac.setAction(action);
				PullUpInputPage1.this.updateUIElements(null, true);
			}
		}

		private static final int ACTION_COLUMN= 1;

		private final static String ACTION_PROPERTY= "action"; //$NON-NLS-1$	

		private static final int MEMBER_COLUMN= 0;

		private final static String MEMBER_PROPERTY= "member"; //$NON-NLS-1$	

		public static final String PAGE_NAME= "PullUpMethodsInputPage1"; //$NON-NLS-1$

		private static final int ROW_COUNT= 10;

		private static final String SETTING_INSTANCEOF= "InstanceOf"; //$NON-NLS-1$

		private static final String SETTING_REPLACE= "Replace"; //$NON-NLS-1$

		private static int countEditableInfos(final MemberActionInfo[] infos) {
			int result= 0;
			for (int i= 0; i < infos.length; i++) {
				final MemberActionInfo info= infos[i];
				if (info.isEditable())
					result++;
			}
			return result;
		}

		private static void putToStingMapping(final Map result, final String[] actionLabels, final int actionIndex) {
			result.put(actionLabels[actionIndex], new Integer(actionIndex));
		}

		private static void setInfoAction(final MemberActionInfo[] infos, final int action) {
			for (int i= 0; i < infos.length; i++) {
				infos[i].setAction(action);
			}
		}

		private Button fCreateStubsButton;

		private Button fDeselectAllButton;

		private Button fEditButton;

		private Button fInstanceofButton;

		private Button fReplaceButton;

		private Button fSelectAllButton;

		private Label fStatusLine;

		private Combo fSuperclassCombo;

		private IType[] fSuperclasses;

		private CheckboxTableViewer fTableViewer;

		private final PullUpInputPage2 fSuccessorPage;
		
		public PullUpInputPage1(PullUpInputPage2 page) {
			super(PAGE_NAME);
			fSuccessorPage= page;
			setMessage(RefactoringMessages.PullUpInputPage1_page_message);
		}

		private boolean areAllMembersMarkedAsPullUp() {
			return getMembersForAction(MemberActionInfo.PULL_UP_ACTION).length == getTableInputAsMemberActionInfoArray().length;
		}

		private boolean areAllMembersMarkedAsWithNoAction() {
			return getMembersWithNoAction().length == getTableInputAsMemberActionInfoArray().length;
		}

		public boolean canFlipToNextPage() {
			if (canSkipSecondInputPage())
				// cannot call super here because it tries to compute successor page, which is expensive
				return isPageComplete();

			return super.canFlipToNextPage();
		}

		private boolean canSkipSecondInputPage() {
			return getMethodsToPullUp().length == 0;
		}

		private void checkPageCompletionStatus(final boolean displayErrorMessage) {
			if (areAllMembersMarkedAsWithNoAction()) {
				if (displayErrorMessage)
					setErrorMessage(RefactoringMessages.PullUpInputPage1_Select_members_to_pull_up);
				setPageComplete(false);
			} else {
				setErrorMessage(null);
				setPageComplete(true);
			}
			fSuccessorPage.fireSettingsChanged();
		}

		private MemberActionInfo[] convertPullableMemberToMemberActionInfoArray() {
			final PullUpRefactoringProcessor processor= getPullUpRefactoring().getPullUpProcessor();
			final List toPullUp= Arrays.asList(processor.getMembersToMove());
			final IMember[] members= processor.getPullableMembersOfDeclaringType();
			final MemberActionInfo[] result= new MemberActionInfo[members.length];
			for (int i= 0; i < members.length; i++) {
				final IMember member= members[i];
				if (toPullUp.contains(member))
					result[i]= new MemberActionInfo(member, MemberActionInfo.PULL_UP_ACTION);
				else
					result[i]= new MemberActionInfo(member, MemberActionInfo.NO_ACTION);
			}
			return result;
		}

		private void createButtonComposite(final Composite parent) {
			final Composite composite= new Composite(parent, SWT.NONE);
			composite.setLayoutData(new GridData(GridData.FILL_VERTICAL));
			final GridLayout gl= new GridLayout();
			gl.marginHeight= 0;
			gl.marginWidth= 0;
			composite.setLayout(gl);

			fSelectAllButton= new Button(composite, SWT.PUSH);
			fSelectAllButton.setText(RefactoringMessages.PullUpWizard_select_all_label);
			fSelectAllButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			fSelectAllButton.setEnabled(true);
			SWTUtil.setButtonDimensionHint(fSelectAllButton);
			fSelectAllButton.addSelectionListener(new SelectionAdapter() {

				public void widgetSelected(final SelectionEvent event) {
					final IMember[] members= getMembers();
					setActionForMembers(members, MemberActionInfo.PULL_UP_ACTION);
					updateUIElements(null, true);
				}
			});

			fDeselectAllButton= new Button(composite, SWT.PUSH);
			fDeselectAllButton.setText(RefactoringMessages.PullUpWizard_deselect_all_label);
			fDeselectAllButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			fDeselectAllButton.setEnabled(false);
			SWTUtil.setButtonDimensionHint(fDeselectAllButton);
			fDeselectAllButton.addSelectionListener(new SelectionAdapter() {

				public void widgetSelected(final SelectionEvent event) {
					final IMember[] members= getMembers();
					setActionForMembers(members, MemberActionInfo.NO_ACTION);
					updateUIElements(null, true);
				}
			});

			fEditButton= new Button(composite, SWT.PUSH);
			fEditButton.setText(RefactoringMessages.PullUpInputPage1_Edit);

			final GridData data= new GridData(GridData.FILL_HORIZONTAL);
			data.verticalIndent= new PixelConverter(parent).convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
			fEditButton.setLayoutData(data);
			fEditButton.setEnabled(false);
			SWTUtil.setButtonDimensionHint(fEditButton);
			fEditButton.addSelectionListener(new SelectionAdapter() {

				public void widgetSelected(final SelectionEvent event) {
					PullUpInputPage1.this.editSelectedMembers();
				}
			});

			final Button addButton= new Button(composite, SWT.PUSH);
			addButton.setText(RefactoringMessages.PullUpInputPage1_Add_Required);
			addButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			SWTUtil.setButtonDimensionHint(addButton);
			addButton.addSelectionListener(new SelectionAdapter() {

				public void widgetSelected(final SelectionEvent event) {
					PullUpInputPage1.this.markAdditionalRequiredMembersAsMembersToPullUp();
				}
			});
		}

		public void createControl(final Composite parent) {
			final Composite composite= new Composite(parent, SWT.NONE);
			final GridLayout gl= new GridLayout();
			gl.numColumns= 2;
			composite.setLayout(gl);

			createSuperTypeCombo(composite);
			createSpacer(composite);
			createSuperTypeCheckbox(composite);
			createInstanceOfCheckbox(composite, gl.marginWidth);
			fReplaceButton.addSelectionListener(new SelectionAdapter() {

				public void widgetSelected(final SelectionEvent e) {
					fInstanceofButton.setEnabled(fReplaceButton.getSelection());
				}
			});
			createStubCheckbox(composite);
			createSpacer(composite);
			createMemberTableLabel(composite);
			createMemberTableComposite(composite);
			createStatusLine(composite);

			setControl(composite);
			Dialog.applyDialogFont(composite);
			initializeCheckboxes();
			PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IJavaHelpContextIds.PULL_UP_WIZARD_PAGE);
		}

		private void createInstanceOfCheckbox(final Composite result, final int margin) {
			final PullUpRefactoringProcessor processor= getPullUpRefactoring().getPullUpProcessor();
			final String title= RefactoringMessages.PullUpInputPage1_label_use_in_instanceof;
			fInstanceofButton= new Button(result, SWT.CHECK);
			fInstanceofButton.setSelection(false);
			final GridData gd= new GridData();
			gd.horizontalIndent= (margin + fInstanceofButton.computeSize(SWT.DEFAULT, SWT.DEFAULT).x);
			gd.horizontalSpan= 2;
			fInstanceofButton.setLayoutData(gd);
			fInstanceofButton.setText(title);
			processor.setInstanceOf(fInstanceofButton.getSelection());
			fInstanceofButton.addSelectionListener(new SelectionAdapter() {

				public void widgetSelected(final SelectionEvent e) {
					processor.setInstanceOf(fInstanceofButton.getSelection());
				}
			});
		}

		private void createMemberTable(final Composite parent) {
			final TableLayoutComposite layouter= new TableLayoutComposite(parent, SWT.NONE);
			layouter.addColumnData(new ColumnWeightData(60, true));
			layouter.addColumnData(new ColumnWeightData(40, true));

			final Table table= new Table(layouter, SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION | SWT.CHECK);
			table.setHeaderVisible(true);
			table.setLinesVisible(true);

			final GridData gd= new GridData(GridData.FILL_BOTH);
			gd.heightHint= SWTUtil.getTableHeightHint(table, ROW_COUNT);
			gd.widthHint= convertWidthInCharsToPixels(30);
			layouter.setLayoutData(gd);

			final TableLayout tableLayout= new TableLayout();
			table.setLayout(tableLayout);

			final TableColumn column0= new TableColumn(table, SWT.NONE);
			column0.setText(RefactoringMessages.PullUpInputPage1_Member);

			final TableColumn column1= new TableColumn(table, SWT.NONE);
			column1.setText(RefactoringMessages.PullUpInputPage1_Action);

			fTableViewer= new PullPushCheckboxTableViewer(table);
			fTableViewer.setUseHashlookup(true);
			fTableViewer.setContentProvider(new ArrayContentProvider());
			fTableViewer.setLabelProvider(new MemberActionInfoLabelProvider());
			fTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {

				public void selectionChanged(final SelectionChangedEvent event) {
					updateButtonEnablementState(event.getSelection());
				}
			});
			fTableViewer.addCheckStateListener(new ICheckStateListener() {

				public void checkStateChanged(final CheckStateChangedEvent event) {
					final boolean checked= event.getChecked();
					final MemberActionInfo info= (MemberActionInfo) event.getElement();
					if (checked)
						info.setAction(MemberActionInfo.PULL_UP_ACTION);
					else
						info.setAction(MemberActionInfo.NO_ACTION);
					updateUIElements(null, true);
				}
			});
			fTableViewer.addDoubleClickListener(new IDoubleClickListener() {

				public void doubleClick(final DoubleClickEvent event) {
					PullUpInputPage1.this.editSelectedMembers();
				}
			});

			setTableInput();
			markAsMembersToPullUp(getPullUpRefactoring().getPullUpProcessor().getMembersToMove(), false);
			setupCellEditors(table);
		}

		private void createMemberTableComposite(final Composite parent) {
			final Composite composite= new Composite(parent, SWT.NONE);
			final GridData gd= new GridData(GridData.FILL_BOTH);
			gd.horizontalSpan= 2;
			composite.setLayoutData(gd);
			final GridLayout gl= new GridLayout();
			gl.numColumns= 2;
			gl.marginWidth= 0;
			gl.marginHeight= 0;
			composite.setLayout(gl);

			createMemberTable(composite);
			createButtonComposite(composite);
		}

		private void createMemberTableLabel(final Composite parent) {
			final Label label= new Label(parent, SWT.NONE);
			label.setText(RefactoringMessages.PullUpInputPage1_Specify_actions);
			final GridData gd0= new GridData();
			gd0.horizontalSpan= 2;
			label.setLayoutData(gd0);
		}

		private void createSpacer(final Composite parent) {
			final Label label= new Label(parent, SWT.NONE);
			final GridData gd0= new GridData();
			gd0.horizontalSpan= 2;
			label.setLayoutData(gd0);
		}

		private void createStatusLine(final Composite composite) {
			fStatusLine= new Label(composite, SWT.NONE);
			final GridData gd= new GridData();
			gd.horizontalSpan= 2;
			updateStatusLine();
			fStatusLine.setLayoutData(gd);
		}

		// String -> Integer
		private Map createStringMappingForSelectedMembers() {
			final Map result= new HashMap();
			putToStingMapping(result, MemberActionInfo.METHOD_LABELS, MemberActionInfo.PULL_UP_ACTION);
			putToStingMapping(result, MemberActionInfo.METHOD_LABELS, MemberActionInfo.DECLARE_ABSTRACT_ACTION);
			return result;
		}

		private void createStubCheckbox(final Composite parent) {
			fCreateStubsButton= new Button(parent, SWT.CHECK);
			fCreateStubsButton.setText(RefactoringMessages.PullUpInputPage1_Create_stubs);
			final GridData gd= new GridData();
			gd.horizontalSpan= 2;
			fCreateStubsButton.setLayoutData(gd);
			fCreateStubsButton.setEnabled(false);
			fCreateStubsButton.setSelection(getPullUpRefactoring().getPullUpProcessor().getCreateMethodStubs());
		}

		private void createSuperTypeCheckbox(final Composite parent) {
			fReplaceButton= new Button(parent, SWT.CHECK);
			fReplaceButton.setText(RefactoringMessages.PullUpInputPage1_label_use_destination);
			final GridData gd= new GridData();
			gd.horizontalSpan= 2;
			fReplaceButton.setLayoutData(gd);
			fReplaceButton.setEnabled(true);
			fReplaceButton.setSelection(getPullUpRefactoring().getPullUpProcessor().isReplace());
		}

		private void createSuperTypeCombo(final Composite parent) {
			try {
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().run(false, false, new IRunnableWithProgress() {

					public void run(final IProgressMonitor pm) throws InvocationTargetException {
						try {
							createSuperTypeCombo(pm, parent);
						} catch (JavaModelException e) {
							throw new InvocationTargetException(e);
						} finally {
							pm.done();
						}
					}
				});
			} catch (InvocationTargetException e) {
				ExceptionHandler.handle(e, getShell(), RefactoringMessages.PullUpInputPage_pull_Up, RefactoringMessages.PullUpInputPage_exception);
			} catch (InterruptedException e) {
				Assert.isTrue(false);// not cancellable
			}
		}

		private void createSuperTypeCombo(final IProgressMonitor pm, final Composite parent) throws JavaModelException {
			final Label label= new Label(parent, SWT.NONE);
			label.setText(RefactoringMessages.PullUpInputPage1_Select_destination);
			label.setLayoutData(new GridData());

			fSuperclassCombo= new Combo(parent, SWT.READ_ONLY);
			fSuperclasses= getPullUpRefactoring().getPullUpProcessor().getPossibleTargetClasses(new RefactoringStatus(), pm);
			Assert.isTrue(fSuperclasses.length > 0);
			for (int i= 0; i < fSuperclasses.length; i++) {
				final String comboLabel= JavaModelUtil.getFullyQualifiedName(fSuperclasses[i]);
				fSuperclassCombo.add(comboLabel);
			}
			fSuperclassCombo.select(fSuperclasses.length - 1);
			fSuperclassCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		}

		public void dispose() {
			fInstanceofButton= null;
			fReplaceButton= null;
			fTableViewer= null;
			super.dispose();
		}

		private void editSelectedMembers() {
			if (!fEditButton.isEnabled())
				return;

			final ISelection preserved= fTableViewer.getSelection();
			try {
				final String shellTitle= RefactoringMessages.PullUpInputPage1_Edit_members;
				final String labelText= RefactoringMessages.PullUpInputPage1_Mark_selected_members;
				final Map stringMapping= createStringMappingForSelectedMembers();
				final String[] keys= (String[]) stringMapping.keySet().toArray(new String[stringMapping.keySet().size()]);
				Arrays.sort(keys);
				final int initialSelectionIndex= getInitialSelectionIndexForEditDialog(stringMapping, keys);
				final ComboSelectionDialog dialog= new ComboSelectionDialog(getShell(), shellTitle, labelText, keys, initialSelectionIndex);
				dialog.setBlockOnOpen(true);
				if (dialog.open() == Window.CANCEL)
					return;
				final int action= ((Integer) stringMapping.get(dialog.getSelectedString())).intValue();
				setInfoAction(getSelectedMemberActionInfos(), action);
			} finally {
				updateUIElements(preserved, true);
			}
		}

		private boolean enableEditButton(final IStructuredSelection ss) {
			if (ss.isEmpty() || ss.size() == 0)
				return false;
			return ss.size() == countEditableInfos(getSelectedMemberActionInfos());
		}

		private MemberActionInfo[] getActiveInfos() {
			final MemberActionInfo[] infos= getTableInputAsMemberActionInfoArray();
			final List result= new ArrayList(infos.length);
			for (int i= 0; i < infos.length; i++) {
				final MemberActionInfo info= infos[i];
				if (info.isActive())
					result.add(info);
			}
			return (MemberActionInfo[]) result.toArray(new MemberActionInfo[result.size()]);
		}

		private int getCommonActionCodeForSelectedInfos() {
			final MemberActionInfo[] infos= getSelectedMemberActionInfos();
			if (infos.length == 0)
				return -1;

			final int code= infos[0].getAction();
			for (int i= 0; i < infos.length; i++) {
				if (code != infos[i].getAction())
					return -1;
			}
			return code;
		}

		private int getInitialSelectionIndexForEditDialog(final Map stringMapping, final String[] keys) {
			final int commonActionCode= getCommonActionCodeForSelectedInfos();
			if (commonActionCode == -1)
				return 0;
			for (final Iterator iter= stringMapping.keySet().iterator(); iter.hasNext();) {
				final String key= (String) iter.next();
				final int action= ((Integer) stringMapping.get(key)).intValue();
				if (commonActionCode == action) {
					for (int i= 0; i < keys.length; i++) {
						if (key.equals(keys[i]))
							return i;
					}
					Assert.isTrue(false);// there's no way
				}
			}
			return 0;
		}

		private IMember[] getMembers() {
			final MemberActionInfo[] macs= getTableInputAsMemberActionInfoArray();
			final List result= new ArrayList(macs.length);
			for (int i= 0; i < macs.length; i++) {
				result.add(macs[i].getMember());
			}
			return (IMember[]) result.toArray(new IMember[result.size()]);
		}

		private IMember[] getMembersForAction(final int action) {
			final MemberActionInfo[] macs= getTableInputAsMemberActionInfoArray();
			final List result= new ArrayList(macs.length);
			for (int i= 0; i < macs.length; i++) {
				if (macs[i].getAction() == action)
					result.add(macs[i].getMember());
			}
			return (IMember[]) result.toArray(new IMember[result.size()]);
		}

		private IMember[] getMembersToPullUp() {
			return getMembersForAction(MemberActionInfo.PULL_UP_ACTION);
		}

		private IMember[] getMembersWithNoAction() {
			return getMembersForAction(MemberActionInfo.NO_ACTION);
		}

		private IMethod[] getMethodsForAction(final int action) {
			final MemberActionInfo[] macs= getTableInputAsMemberActionInfoArray();
			final List list= new ArrayList(macs.length);
			for (int i= 0; i < macs.length; i++) {
				if (macs[i].isMethodInfo() && macs[i].getAction() == action) {
					list.add(macs[i].getMember());
				}
			}
			return (IMethod[]) list.toArray(new IMethod[list.size()]);
		}

		private IMethod[] getMethodsToDeclareAbstract() {
			return getMethodsForAction(MemberActionInfo.DECLARE_ABSTRACT_ACTION);
		}

		private IMethod[] getMethodsToPullUp() {
			return getMethodsForAction(MemberActionInfo.PULL_UP_ACTION);
		}

		public IWizardPage getNextPage() {
			initializeRefactoring();
			storeDialogSettings();
			if (canSkipSecondInputPage())
				return computeSuccessorPage();

			return super.getNextPage();
		}

		private PullUpRefactoring getPullUpRefactoring() {
			return (PullUpRefactoring) getRefactoring();
		}

		private IType getSelectedClass() {
			return fSuperclasses[fSuperclassCombo.getSelectionIndex()];
		}

		private MemberActionInfo[] getSelectedMemberActionInfos() {
			Assert.isTrue(fTableViewer.getSelection() instanceof IStructuredSelection);
			final IStructuredSelection ss= (IStructuredSelection) fTableViewer.getSelection();
			final List result= ss.toList();
			return (MemberActionInfo[]) result.toArray(new MemberActionInfo[result.size()]);
		}

		private MemberActionInfo[] getTableInputAsMemberActionInfoArray() {
			return (MemberActionInfo[]) fTableViewer.getInput();
		}

		private void initializeCheckBox(final Button checkbox, final String property, final boolean def) {
			final String s= JavaPlugin.getDefault().getDialogSettings().get(property);
			if (s != null)
				checkbox.setSelection(new Boolean(s).booleanValue());
			else
				checkbox.setSelection(def);
		}

		private void initializeCheckboxes() {
			initializeCheckBox(fReplaceButton, SETTING_REPLACE, true);
			initializeCheckBox(fInstanceofButton, SETTING_INSTANCEOF, false);
		}

		private void initializeRefactoring() {
			final PullUpRefactoringProcessor processor= getPullUpRefactoring().getPullUpProcessor();
			processor.setMembersToMove(getMembersToPullUp());
			processor.setMethodsToDeclareAbstract(getMethodsToDeclareAbstract());
			processor.setTargetClass(getSelectedClass());
			processor.setCreateMethodStubs(fCreateStubsButton.getSelection());
			processor.setReplace(fReplaceButton.getSelection());
			processor.setInstanceOf(fInstanceofButton.getSelection());
			processor.setMethodsToDelete(getMethodsToPullUp());
		}

		private void markAdditionalRequiredMembersAsMembersToPullUp() {
			try {
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().run(false, false, new IRunnableWithProgress() {

					public void run(final IProgressMonitor pm) throws InvocationTargetException {
						try {
							markAsMembersToPullUp(getPullUpRefactoring().getPullUpProcessor().getAdditionalRequiredMembersToPullUp(pm), true);
						} catch (JavaModelException e) {
							throw new InvocationTargetException(e);
						} finally {
							pm.done();
						}
					}
				});
			} catch (InvocationTargetException e) {
				ExceptionHandler.handle(e, getShell(), RefactoringMessages.PullUpInputPage_pull_Up, RefactoringMessages.PullUpInputPage_exception);
			} catch (InterruptedException e) {
				Assert.isTrue(false);// not cancellable
			}
		}

		private void markAsMembersToPullUp(final IMember[] elements, final boolean displayErrorMessage) {
			setActionForMembers(elements, MemberActionInfo.PULL_UP_ACTION);
			updateUIElements(null, displayErrorMessage);
		}

		protected boolean performFinish() {
			initializeRefactoring();
			storeDialogSettings();
			return super.performFinish();
		}

		private void setActionForMembers(final IMember[] members, final int action) {
			final MemberActionInfo[] macs= getTableInputAsMemberActionInfoArray();
			for (int i= 0; i < members.length; i++) {
				for (int j= 0; j < macs.length; j++) {
					if (macs[j].getMember().equals(members[i]))
						macs[j].setAction(action);
				}
			}
		}

		private void setTableInput() {
			fTableViewer.setInput(convertPullableMemberToMemberActionInfoArray());
		}

		private void setupCellEditors(final Table table) {
			final ComboBoxCellEditor comboBoxCellEditor= new ComboBoxCellEditor();
			comboBoxCellEditor.setStyle(SWT.READ_ONLY);
			fTableViewer.setCellEditors(new CellEditor[] { null, comboBoxCellEditor});
			fTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {

				public void selectionChanged(final SelectionChangedEvent event) {
					if (comboBoxCellEditor.getControl() == null & !table.isDisposed())
						comboBoxCellEditor.create(table);
					final ISelection sel= event.getSelection();
					if (!(sel instanceof IStructuredSelection))
						return;
					final IStructuredSelection ss= (IStructuredSelection) sel;
					if (ss.size() != 1)
						return;
					final MemberActionInfo mac= (MemberActionInfo) ss.getFirstElement();
					comboBoxCellEditor.setItems(mac.getAllowedLabels());
					comboBoxCellEditor.setValue(new Integer(mac.getAction()));
				}
			});

			final ICellModifier cellModifier= new PullUpCellModifier();
			fTableViewer.setCellModifier(cellModifier);
			fTableViewer.setColumnProperties(new String[] { MEMBER_PROPERTY, ACTION_PROPERTY});
		}

		public void setVisible(final boolean visible) {
			super.setVisible(visible);
			if (visible) {
				fTableViewer.setSelection(new StructuredSelection(getActiveInfos()), true);
				fTableViewer.getControl().setFocus();
			}
		}

		private void storeDialogSettings() {
			final IDialogSettings settings= JavaPlugin.getDefault().getDialogSettings();
			settings.put(SETTING_REPLACE, fReplaceButton.getSelection());
			settings.put(SETTING_INSTANCEOF, fInstanceofButton.getSelection());
		}

		private void updateButtonEnablementState(final ISelection tableSelection) {
			if (fEditButton != null)
				fEditButton.setEnabled(enableEditButton((IStructuredSelection) tableSelection));
			fCreateStubsButton.setEnabled(getMethodsToDeclareAbstract().length != 0);
			fInstanceofButton.setEnabled(fReplaceButton.getSelection());
			if (fSelectAllButton != null)
				fSelectAllButton.setEnabled(!areAllMembersMarkedAsPullUp());
			if (fDeselectAllButton != null)
				fDeselectAllButton.setEnabled(!areAllMembersMarkedAsWithNoAction());
		}

		private void updateStatusLine() {
			if (fStatusLine == null)
				return;
			final int selected= fTableViewer.getCheckedElements().length;
			final String[] keys= { String.valueOf(selected)};
			final String msg= Messages.format(RefactoringMessages.PullUpInputPage1_status_line, keys);
			fStatusLine.setText(msg);
		}

		private void updateUIElements(final ISelection preserved, final boolean displayErrorMessage) {
			fTableViewer.refresh();
			if (preserved != null) {
				fTableViewer.getControl().setFocus();
				fTableViewer.setSelection(preserved);
			}
			checkPageCompletionStatus(displayErrorMessage);
			updateButtonEnablementState(fTableViewer.getSelection());
			updateStatusLine();
		}
	}

	private static class PullUpInputPage2 extends UserInputWizardPage {

		private static class PullUpFilter extends ViewerFilter {

			private static boolean anySubtypeCanBeShown(final IType type, final Map typeToMemberArray, final ITypeHierarchy hierarchy) {
				final IType[] subTypes= hierarchy.getSubtypes(type);
				for (int i= 0; i < subTypes.length; i++) {
					if (canBeShown(subTypes[i], typeToMemberArray, hierarchy))
						return true;
				}
				return false;
			}

			private static boolean canBeShown(final IType type, final Map typeToMemberArray, final ITypeHierarchy hierarchy) {
				if (typeToMemberArray.containsKey(type))
					return true;
				return anySubtypeCanBeShown(type, typeToMemberArray, hierarchy);
			}

			private static Set computeShowableSubtypesOfMainType(final ITypeHierarchy hierarchy, final Map typeToMemberArray) {
				final Set result= new HashSet();
				final IType[] subtypes= hierarchy.getAllSubtypes(hierarchy.getType());
				for (int i= 0; i < subtypes.length; i++) {
					final IType subtype= subtypes[i];
					if (canBeShown(subtype, typeToMemberArray, hierarchy))
						result.add(subtype);
				}
				return result;
			}

			private static Set computeTypesToShow(final ITypeHierarchy hierarchy, final Map typeToMemberArray) {
				final Set typesToShow= new HashSet();
				typesToShow.add(hierarchy.getType());
				typesToShow.addAll(computeShowableSubtypesOfMainType(hierarchy, typeToMemberArray));
				return typesToShow;
			}

			private final Set fTypesToShow;

			PullUpFilter(final ITypeHierarchy hierarchy, final IMember[] members) {
				// IType -> IMember[]
				final Map typeToMemberArray= PullUpInputPage2.createTypeToMemberArrayMapping(members);
				fTypesToShow= computeTypesToShow(hierarchy, typeToMemberArray);
			}

			/*
			 * @see ViewerFilter#select(Viewer, Object, Object)
			 */
			public boolean select(final Viewer viewer, final Object parentElement, final Object element) {
				if (element instanceof IMethod)
					return true;
				return fTypesToShow.contains(element);
			}
		}

		private static class PullUpHierarchyContentProvider implements ITreeContentProvider {

			private IType fDeclaringType;

			private ITypeHierarchy fHierarchy;

			private Map fTypeToMemberArray; // IType -> IMember[]

			PullUpHierarchyContentProvider(final IType declaringType, final IMember[] members) {
				fDeclaringType= declaringType;
				fTypeToMemberArray= PullUpInputPage2.createTypeToMemberArrayMapping(members);
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
			 * @see ITreeContentProvider#getChildren(Object)
			 */
			public Object[] getChildren(final Object parentElement) {
				if (parentElement instanceof IType)
					return getSubclassesAndMembers((IType) parentElement);
				else
					return new Object[0];
			}

			/*
			 * @see IStructuredContentProvider#getElements(Object)
			 */
			public Object[] getElements(final Object inputElement) {
				Assert.isTrue(inputElement == null || inputElement instanceof ITypeHierarchy);
				return new IType[] { fHierarchy.getType()};
			}

			private IMember[] getMembers(final IType type) {
				if (fTypeToMemberArray.containsKey(type))
					return (IMember[]) (fTypeToMemberArray.get(type));
				else
					return new IMember[0];
			}

			/*
			 * @see ITreeContentProvider#getParent(Object)
			 */
			public Object getParent(final Object element) {
				if (element instanceof IType)
					return fHierarchy.getSuperclass((IType) element);
				if (element instanceof IMember)
					return ((IMember) element).getDeclaringType();
				Assert.isTrue(false, "Should not get here"); //$NON-NLS-1$
				return null;
			}

			private IType[] getSubclasses(final IType type) {
				if (type.equals(fDeclaringType))
					return new IType[0];
				return fHierarchy.getSubclasses(type);
			}

			private Object[] getSubclassesAndMembers(final IType type) {
				final Set set= new HashSet();
				set.addAll(Arrays.asList(getSubclasses(type)));
				set.addAll(Arrays.asList(getMembers(type)));
				return set.toArray();
			}

			/*
			 * @see ITreeContentProvider#hasChildren(Object)
			 */
			public boolean hasChildren(final Object element) {
				if (!(element instanceof IType))
					return false;
				final IType type= (IType) element;
				return hasSubtypes(type) || hasMembers(type);
			}

			private boolean hasMembers(final IType type) {
				return fTypeToMemberArray.containsKey(type);
			}

			private boolean hasSubtypes(final IType type) {
				return fHierarchy.getAllSubtypes(type).length > 0;
			}

			/*
			 * @see IContentProvider#inputChanged(Viewer, Object, Object)
			 */
			public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
				Assert.isTrue(newInput == null || newInput instanceof ITypeHierarchy);
				fHierarchy= (ITypeHierarchy) newInput;
			}
		}

		public static final String PAGE_NAME= "PullUpMethodsInputPage2"; //$NON-NLS-1$

		// IType -> IMember[]
		private static Map createTypeToMemberArrayMapping(final IMember[] members) {
			final Map typeToMemberSet= createTypeToMemberSetMapping(members);

			final Map typeToMemberArray= new HashMap();
			for (final Iterator iter= typeToMemberSet.keySet().iterator(); iter.hasNext();) {
				final IType type= (IType) iter.next();
				final Set memberSet= (Set) typeToMemberSet.get(type);
				final IMember[] memberArray= (IMember[]) memberSet.toArray(new IMember[memberSet.size()]);
				typeToMemberArray.put(type, memberArray);
			}
			return typeToMemberArray;
		}

		// IType -> Set of IMember
		private static Map createTypeToMemberSetMapping(final IMember[] members) {
			final Map typeToMemberSet= new HashMap();
			for (int i= 0; i < members.length; i++) {
				final IMember member= members[i];
				final IType type= member.getDeclaringType();
				if (!typeToMemberSet.containsKey(type))
					typeToMemberSet.put(type, new HashSet());
				((Set) typeToMemberSet.get(type)).add(member);
			}
			return typeToMemberSet;
		}

		private static JavaTextTools getJavaTextTools() {
			return JavaPlugin.getDefault().getJavaTextTools();
		}

		private SourceViewer fSourceViewer;

		private ContainerCheckedTreeViewer fTreeViewer;

		private boolean fChangedSettings= true;

		private Label fTypeHierarchyLabel;

		public PullUpInputPage2() {
			super(PAGE_NAME);
			setMessage(RefactoringMessages.PullUpInputPage_select_methods);
		}

		private void checkAllParents(final IType parent) {
			final ITypeHierarchy th= getTreeViewerInput();
			final IType root= getTreeViewerInputRoot();
			IType type= parent;
			while (!root.equals(type)) {
				fTreeViewer.setChecked(type, true);
				type= th.getSuperclass(type);
			}
			fTreeViewer.setChecked(root, true);
		}

		public void checkPulledUp() {
			uncheckAll();
			final PullUpRefactoringProcessor processor= getPullUpMethodsRefactoring().getPullUpProcessor();
			fTreeViewer.setCheckedElements(processor.getMembersToMove());
			final IType parent= processor.getDeclaringType();
			fTreeViewer.setChecked(parent, true);
			checkAllParents(parent);
		}

		private void createButtonComposite(final Composite superComposite) {
			final Composite buttonComposite= new Composite(superComposite, SWT.NONE);
			buttonComposite.setLayoutData(new GridData());
			final GridLayout bcl= new GridLayout();
			bcl.numColumns= 2;
			bcl.marginWidth= 1;
			buttonComposite.setLayout(bcl);

			final Button button= new Button(buttonComposite, SWT.PUSH);
			button.setText(RefactoringMessages.PullUpInputPage2_Select);
			button.setLayoutData(new GridData());
			SWTUtil.setButtonDimensionHint(button);
			button.addSelectionListener(new SelectionAdapter() {

				public void widgetSelected(final SelectionEvent e) {
					PullUpInputPage2.this.checkPulledUp();
					updateTypeHierarchyLabel();
				}
			});
		}

		/*
		 * @see IDialogPage#createControl(Composite)
		 */
		public void createControl(final Composite parent) {
			final Composite composite= new Composite(parent, SWT.NONE);
			composite.setLayout(new GridLayout());

			createTreeAndSourceViewer(composite);
			createButtonComposite(composite);
			setControl(composite);

			Dialog.applyDialogFont(composite);
			PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IJavaHelpContextIds.PULL_UP_WIZARD_PAGE);
		}

		private void createHierarchyTreeComposite(final Composite parent) {
			final Composite composite= new Composite(parent, SWT.NONE);
			composite.setLayoutData(new GridData(GridData.FILL_BOTH));
			final GridLayout layout= new GridLayout();
			layout.marginWidth= 0;
			layout.marginHeight= 0;
			layout.horizontalSpacing= 1;
			layout.verticalSpacing= 1;
			composite.setLayout(layout);

			createTypeHierarchyLabel(composite);
			createTreeViewer(composite);
		}

		private void createSourceViewer(final Composite c) {
			final IPreferenceStore store= JavaPlugin.getDefault().getCombinedPreferenceStore();
			fSourceViewer= new JavaSourceViewer(c, null, null, false, SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION, store);
			fSourceViewer.configure(new JavaSourceViewerConfiguration(getJavaTextTools().getColorManager(), store, null, null));
			fSourceViewer.setEditable(false);
			fSourceViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
			fSourceViewer.getControl().setFont(JFaceResources.getFont(PreferenceConstants.EDITOR_TEXT_FONT));
		}

		private void createSourceViewerComposite(final Composite parent) {
			final Composite c= new Composite(parent, SWT.NONE);
			c.setLayoutData(new GridData(GridData.FILL_BOTH));
			final GridLayout layout= new GridLayout();
			layout.marginWidth= 0;
			layout.marginHeight= 0;
			layout.horizontalSpacing= 1;
			layout.verticalSpacing= 1;
			c.setLayout(layout);

			createSourceViewerLabel(c);
			createSourceViewer(c);
		}

		private void createSourceViewerLabel(final Composite c) {
			final Label label= new Label(c, SWT.WRAP);
			final GridData gd= new GridData(GridData.FILL_HORIZONTAL);
			label.setText(RefactoringMessages.PullUpInputPage2_Source);
			label.setLayoutData(gd);
		}

		private void createTreeAndSourceViewer(final Composite superComposite) {
			final SashForm composite= new SashForm(superComposite, SWT.HORIZONTAL);
			initializeDialogUnits(superComposite);
			final GridData gd= new GridData(GridData.FILL_BOTH);
			gd.heightHint= convertHeightInCharsToPixels(20);
			gd.widthHint= convertWidthInCharsToPixels(10);
			composite.setLayoutData(gd);
			final GridLayout layout= new GridLayout();
			layout.numColumns= 2;
			layout.marginWidth= 0;
			layout.marginHeight= 0;
			layout.horizontalSpacing= 1;
			layout.verticalSpacing= 1;
			composite.setLayout(layout);

			createHierarchyTreeComposite(composite);
			createSourceViewerComposite(composite);
			composite.setWeights(new int[] { 50, 50});
		}

		private void createTreeViewer(final Composite composite) {
			final Tree tree= new Tree(composite, SWT.CHECK | SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
			tree.setLayoutData(new GridData(GridData.FILL_BOTH));
			fTreeViewer= new ContainerCheckedTreeViewer(tree);
			fTreeViewer.setLabelProvider(new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT));
			fTreeViewer.setUseHashlookup(true);
			fTreeViewer.setSorter(new JavaElementSorter());
			fTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {

				public void selectionChanged(final SelectionChangedEvent event) {
					PullUpInputPage2.this.treeViewerSelectionChanged(event);
				}
			});
			fTreeViewer.addCheckStateListener(new ICheckStateListener() {

				public void checkStateChanged(final CheckStateChangedEvent event) {
					PullUpInputPage2.this.updateTypeHierarchyLabel();
				}
			});
		}

		private void createTypeHierarchyLabel(final Composite composite) {
			fTypeHierarchyLabel= new Label(composite, SWT.WRAP);
			final GridData gd= new GridData(GridData.FILL_HORIZONTAL);
			fTypeHierarchyLabel.setLayoutData(gd);
		}

		private IMethod[] getCheckedMethods() {
			final Object[] checked= fTreeViewer.getCheckedElements();
			final List members= new ArrayList(checked.length);
			for (int i= 0; i < checked.length; i++) {
				if (checked[i] instanceof IMethod)
					members.add(checked[i]);
			}
			return (IMethod[]) members.toArray(new IMethod[members.size()]);
		}

		private ISourceReference getFirstSelectedSourceReference(final SelectionChangedEvent event) {
			final ISelection s= event.getSelection();
			if (!(s instanceof IStructuredSelection))
				return null;
			final IStructuredSelection ss= (IStructuredSelection) s;
			if (ss.size() != 1)
				return null;
			final Object first= ss.getFirstElement();
			if (!(first instanceof ISourceReference))
				return null;
			return (ISourceReference) first;
		}

		/*
		 * @see IWizardPage#getNextPage()
		 */
		public IWizardPage getNextPage() {
			initializeRefactoring();
			return super.getNextPage();
		}

		private PullUpRefactoring getPullUpMethodsRefactoring() {
			return (PullUpRefactoring) getRefactoring();
		}

		private String getSupertypeSignature() {
			return JavaElementUtil.createSignature(getPullUpMethodsRefactoring().getPullUpProcessor().getTargetClass());
		}

		private ITypeHierarchy getTreeViewerInput() {
			return (ITypeHierarchy) fTreeViewer.getInput();
		}

		private IType getTreeViewerInputRoot() {
			return getTreeViewerInput().getType();
		}

		private void initializeRefactoring() {
			getPullUpMethodsRefactoring().getPullUpProcessor().setMethodsToDelete(getCheckedMethods());
		}

		private void initializeTreeViewer() {
			try {
				getContainer().run(false, false, new IRunnableWithProgress() {

					public void run(final IProgressMonitor pm) {
						try {
							initializeTreeViewer(pm);
						} finally {
							pm.done();
						}
					}
				});
			} catch (InvocationTargetException e) {
				ExceptionHandler.handle(e, getShell(), RefactoringMessages.PullUpInputPage_pull_Up, RefactoringMessages.PullUpInputPage_exception);
			} catch (InterruptedException e) {
				Assert.isTrue(false); // not cancellable
			}
		}

		private void initializeTreeViewer(final IProgressMonitor pm) {
			try {
				final PullUpRefactoringProcessor processor= getPullUpMethodsRefactoring().getPullUpProcessor();
				final IMember[] matchingMethods= processor.getMatchingElements(new SubProgressMonitor(pm, 1), false);
				final ITypeHierarchy hierarchy= processor.getTypeHierarchyOfTargetClass(new SubProgressMonitor(pm, 1));
				removeAllTreeViewFilters();
				fTreeViewer.addFilter(new PullUpFilter(hierarchy, matchingMethods));
				fTreeViewer.setContentProvider(new PullUpHierarchyContentProvider(processor.getDeclaringType(), matchingMethods));
				fTreeViewer.setInput(hierarchy);
				precheckElements(fTreeViewer);
				fTreeViewer.expandAll();
				updateTypeHierarchyLabel();
			} catch (JavaModelException e) {
				ExceptionHandler.handle(e, RefactoringMessages.PullUpInputPage_pull_up1, RefactoringMessages.PullUpInputPage_exception);
				fTreeViewer.setInput(null);
			}
		}

		/*
		 * @see RefactoringWizardPage#performFinish()
		 */
		protected boolean performFinish() {
			initializeRefactoring();
			return super.performFinish();
		}

		private void precheckElements(final ContainerCheckedTreeViewer treeViewer) {
			final IMember[] members= getPullUpMethodsRefactoring().getPullUpProcessor().getMembersToMove();
			for (int i= 0; i < members.length; i++) {
				treeViewer.setChecked(members[i], true);
			}
		}

		private void removeAllTreeViewFilters() {
			final ViewerFilter[] filters= fTreeViewer.getFilters();
			for (int i= 0; i < filters.length; i++) {
				fTreeViewer.removeFilter(filters[i]);
			}
		}

		private void setHierarchyLabelText() {
			final String message= Messages.format(RefactoringMessages.PullUpInputPage_subtypes, getSupertypeSignature());
			fTypeHierarchyLabel.setText(message);
		}

		private void setSourceViewerContents(String contents) {
			if (contents != null) {
				final IJavaProject project= getPullUpMethodsRefactoring().getPullUpProcessor().getTargetClass().getJavaProject();
				final String[] lines= Strings.convertIntoLines(contents);
				if (lines.length > 0) {
					final int indent= Strings.computeIndentUnits(lines[lines.length - 1], project);
					contents= Strings.changeIndent(contents, indent, project, "", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			final IDocument document= (contents == null) ? new Document() : new Document(contents);
			getJavaTextTools().setupJavaDocumentPartitioner(document);
			fSourceViewer.setDocument(document);
		}

		public void setVisible(final boolean visible) {
			if (visible && fChangedSettings) {
				fChangedSettings= false;
				initializeTreeViewer();
				setHierarchyLabelText();
			}
			super.setVisible(visible);
		}

		public void fireSettingsChanged() {
			fChangedSettings= true;
		}

		private void showInSourceViewer(final ISourceReference selected) throws JavaModelException {
			if (selected == null)
				setSourceViewerContents(null);
			else
				setSourceViewerContents(selected.getSource());
		}

		private void treeViewerSelectionChanged(final SelectionChangedEvent event) {
			try {
				showInSourceViewer(getFirstSelectedSourceReference(event));
			} catch (JavaModelException e) {
				ExceptionHandler.handle(e, RefactoringMessages.PullUpInputPage_pull_up1, RefactoringMessages.PullUpInputPage_see_log);
			}
		}

		private void uncheckAll() {
			final IType root= getTreeViewerInputRoot();
			fTreeViewer.setChecked(root, false);
		}

		private void updateTypeHierarchyLabel() {
			final String message= Messages.format(RefactoringMessages.PullUpInputPage_hierarchyLabal, new Integer(getCheckedMethods().length).toString());
			setMessage(message, IMessageProvider.INFORMATION);
		}
	}

	public PullUpWizard(final PullUpRefactoring ref) {
		super(ref, WIZARD_BASED_USER_INTERFACE);
		setDefaultPageTitle(RefactoringMessages.PullUpWizard_defaultPageTitle);
		setDefaultPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_REFACTOR_PULL_UP);
	}

	protected void addUserInputPages() {
		PullUpInputPage2 page= new PullUpInputPage2();
		addPage(new PullUpInputPage1(page));
		addPage(page);
	}
}
