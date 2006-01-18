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
package org.eclipse.jdt.internal.ui.refactoring.reorg;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.JFaceResources;

import org.eclipse.ui.PlatformUI;

import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;

import org.eclipse.ltk.core.refactoring.Refactoring;

import org.eclipse.jdt.internal.corext.refactoring.tagging.ICommentProvider;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IDelegatingUpdating;
import org.eclipse.jdt.internal.corext.refactoring.tagging.INameUpdating;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IQualifiedNameUpdating;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IReferenceUpdating;
import org.eclipse.jdt.internal.corext.refactoring.tagging.ITextUpdating;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.preferences.ScrolledPageContent;
import org.eclipse.jdt.internal.ui.refactoring.DelegateUIHelper;
import org.eclipse.jdt.internal.ui.refactoring.QualifiedNameComponent;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.TextInputWizardPage;

abstract class RenameInputWizardPage extends TextInputWizardPage {

	private String fHelpContextID;
	private Button fUpdateReferences;
	private Text fCommentField;
	private Button fUpdateTextualMatches;
	private Button fUpdateQualifiedNames;
	private Button fLeaveDelegateCheckBox;
	private ExpandableSettingSection fLeaveDelegateSection;
	private QualifiedNameComponent fQualifiedNameComponent;
	private final List fExpandableSections= new ArrayList();

	private static final String UPDATE_TEXTUAL_MATCHES= "updateTextualMatches"; //$NON-NLS-1$
	private static final String UPDATE_QUALIFIED_NAMES= "updateQualifiedNames"; //$NON-NLS-1$
	private static final String SETTINGS_EXPANDED= "expanded"; //$NON-NLS-1$

	/**
	 * Creates a new rename wizard input page.
	 * @param isLastUserPage <code>true</code> if this page is the wizard's last
	 *  user input page. Otherwise <code>false</code>.
	 * @param initialValue the initial value
	 */
	public RenameInputWizardPage(String description, String contextHelpId, boolean isLastUserPage, String initialValue) {
		super(description, isLastUserPage, initialValue);
		fHelpContextID= contextHelpId;
	}

	public void createControl(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		setControl(composite);
		initializeDialogUnits(composite);
		
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 1;
		composite.setLayout(layout);
		GridData data= new GridData(GridData.FILL_HORIZONTAL);
		composite.setLayoutData(data);

		Composite textComposite= new Composite(composite, SWT.NONE);
		textComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));	
		layout= new GridLayout();
		layout.numColumns= 2;
		layout.verticalSpacing= 8;
		textComposite.setLayout(layout);

		Label label= new Label(textComposite, SWT.NONE);
		label.setText(getLabelText());
		Text text= createTextInputField(textComposite);
		text.selectAll();
		data= new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint= convertWidthInCharsToPixels(25);
		text.setLayoutData(data);

		ScrolledPageContent content= new ScrolledPageContent(composite);
		data= new GridData(GridData.FILL, GridData.FILL, true, true);
		data.heightHint= convertHeightInCharsToPixels(9);
		content.setLayoutData(data);
		Composite scrollingComposite= content.getBody();
		layout= new GridLayout();
		layout.numColumns= 1;
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		scrollingComposite.setLayout(layout);
		data= new GridData(GridData.FILL, GridData.FILL, true, true);

		addAdditionalOptions(scrollingComposite);
		if (scrollingComposite.getChildren().length > 0) {
			Label separator= new Label(scrollingComposite, SWT.NONE);
			data= new GridData(GridData.FILL, GridData.FILL, true, false);
			data.heightHint= convertVerticalDLUsToPixels(1);
			separator.setLayoutData(data);
		}
		addOptionalUpdateCheckboxes(scrollingComposite, new GridLayout().marginWidth);
		addOptionalLeaveDelegateCheckbox(scrollingComposite);
		addOptionalCommentField(scrollingComposite);
		updateForcePreview();
		restoreSectionExpansionStates(getRefactoringSettings());

		Dialog.applyDialogFont(composite);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), fHelpContextID);
	}

	private static ScrolledPageContent getScrolledPageContent(final Control control) {
		Control parent= control.getParent();
		while (!(parent instanceof ScrolledPageContent) && parent != null)
			parent= parent.getParent();
		if (parent instanceof ScrolledPageContent)
			return (ScrolledPageContent) parent;
		return null;
	}

	private static void makeScrollable(final Control control) {
		final ScrolledPageContent content= getScrolledPageContent(control);
		if (content != null)
			content.adaptChild(control);
	}

	private ExpandableSettingSection createExpandableSection(Composite parent, Refactoring refactoring, String label, int nColumns, IDescriptionProvider provider) {
		final ExpandableSettingSection expandable= new ExpandableSettingSection(parent, refactoring, SWT.NONE, provider);
		expandable.setText(label);
		expandable.setExpanded(false);
		expandable.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT));
		expandable.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, nColumns, 1));
		expandable.addExpansionListener(new ExpansionAdapter() {

			public void expansionStateChanged(ExpansionEvent event) {
				final ExpandableComposite source= ((ExpandableComposite) event.getSource());
				if (event.getState()) {
					for (final Iterator iterator= fExpandableSections.iterator(); iterator.hasNext();) {
						final ExpandableComposite composite= (ExpandableComposite) iterator.next();
						if (composite != source)
							composite.setExpanded(false);
					}
				}
				final ScrolledPageContent content= getScrolledPageContent(source);
				if (content != null)
					content.reflow(true);
			}
		});
		fExpandableSections.add(expandable);
		makeScrollable(expandable);
		return expandable;
	}

	private void storeSectionExpansionStates(final IDialogSettings settings) {
		for (int index= 0; index < fExpandableSections.size(); index++)
			settings.put(SETTINGS_EXPANDED + String.valueOf(index), ((ExpandableComposite) fExpandableSections.get(index)).isExpanded());
	}

	private void restoreSectionExpansionStates(final IDialogSettings settings) {
		for (int index= 0; index < fExpandableSections.size(); index++) {
			final ExpandableComposite expandable= (ExpandableComposite) fExpandableSections.get(index);
			if (settings == null)
				expandable.setExpanded(index == 0); // only expand the first node by default
			else
				expandable.setExpanded(settings.getBoolean(SETTINGS_EXPANDED + String.valueOf(index)));
		}
	}

	/**
	 * Clients can override this method to provide more UI elements. By default, does nothing
	 * 
	 * @param composite the parent composite
	 */
	protected void addAdditionalOptions(Composite composite) {
		// none by default
	}

	public void setVisible(boolean visible) {
		if (visible) {
			INameUpdating nameUpdating= (INameUpdating)getRefactoring().getAdapter(INameUpdating.class);
			if (nameUpdating != null) {
				String newName= getNewName(nameUpdating);
				if (newName != null && newName.length() > 0 && !newName.equals(getInitialValue())) {
					Text textField= getTextField();
					textField.setText(newName);
					textField.setSelection(0, newName.length());
				}
			}
		}
		super.setVisible(visible);
	}

	/**
	 * Returns the new name for the Java element or <code>null</code>
	 * if no new name is provided
	 * 
	 * @return the new name or <code>null</code>
	 */
	protected String getNewName(INameUpdating nameUpdating) {
		return nameUpdating.getNewElementName();
	}
	
	protected boolean saveSettings() {
		if (getContainer() instanceof Dialog)
			return ((Dialog)getContainer()).getReturnCode() == IDialogConstants.OK_ID;
		return true;
	}

	public void dispose() {
		if (saveSettings()) {
			saveBooleanSetting(UPDATE_TEXTUAL_MATCHES, fUpdateTextualMatches);
			saveBooleanSetting(UPDATE_QUALIFIED_NAMES, fUpdateQualifiedNames);
			if (fQualifiedNameComponent != null)
				fQualifiedNameComponent.savePatterns(getRefactoringSettings());
			DelegateUIHelper.saveLeaveDelegateSetting(fLeaveDelegateCheckBox);
		}
		storeSectionExpansionStates(getRefactoringSettings());
	}

	private void addOptionalCommentField(final Composite parent) {
		final ICommentProvider provider= (ICommentProvider) getRefactoring().getAdapter(ICommentProvider.class);
		if (provider == null || !provider.canEnableComment())
			return;
		final ExpandableComposite expandable= createExpandableSection(parent, getRefactoring(), RefactoringMessages.RenameInputWizardPage_comment_section, 1, new IDescriptionProvider() {

			public String getDescription(final Refactoring refactoring) {
				final ICommentProvider commentProvider= (ICommentProvider) refactoring.getAdapter(ICommentProvider.class);
				if (commentProvider != null && commentProvider.canEnableComment()) {
					final String comment= commentProvider.getComment();
					if (comment != null && !"".equals(comment)) //$NON-NLS-1$
						return RefactoringMessages.RenameInputWizardPage_expand_hint;
				}
				return null;
			}
		});
		final Composite composite= new Composite(expandable, SWT.NONE);
		expandable.setClient(composite);
		composite.setLayout(new GridLayout(1, false));
		fCommentField= new Text(composite, SWT.MULTI | SWT.BORDER);
		final GridData data= new GridData(GridData.FILL, GridData.CENTER, true, false);
		data.heightHint= convertHeightInCharsToPixels(3);
		data.grabExcessHorizontalSpace= true;
		fCommentField.setLayoutData(data);
		makeScrollable(fCommentField);
		fCommentField.addModifyListener(new ModifyListener() {

			public void modifyText(final ModifyEvent e) {
				final String text= fCommentField.getText();
				if (text != null && !"".equals(text)) //$NON-NLS-1$
					provider.setComment(text);
				else
					provider.setComment(null);
			}
		});
	}

	private void addOptionalUpdateCheckboxes(final Composite parent, final int marginWidth) {
		final IReferenceUpdating referenceUpdating= (IReferenceUpdating) getRefactoring().getAdapter(IReferenceUpdating.class);
		if (referenceUpdating == null || !referenceUpdating.canEnableUpdateReferences())
			return;
		final ITextUpdating textUpdating= (ITextUpdating) getRefactoring().getAdapter(ITextUpdating.class);
		if (textUpdating == null || !textUpdating.canEnableTextUpdating())
			return;
		final IQualifiedNameUpdating qualifiedUpdating= (IQualifiedNameUpdating) getRefactoring().getAdapter(IQualifiedNameUpdating.class);
		if (qualifiedUpdating == null || qualifiedUpdating.canEnableQualifiedNameUpdating())
			return;
		boolean value= false;
		final ExpandableComposite expandable= createExpandableSection(parent, getRefactoring(), RefactoringMessages.RenameInputWizardPage_update_section, 1, new IDescriptionProvider() {

			public String getDescription(final Refactoring refactoring) {
				final List settings= new ArrayList();
				final IReferenceUpdating references= (IReferenceUpdating) refactoring.getAdapter(IReferenceUpdating.class);
				if (references != null && references.canEnableUpdateReferences()) {
					final boolean result= references.getUpdateReferences();
					if (result)
						settings.add(RefactoringMessages.RenameInputWizardPage_references_setting);
				}
				final ITextUpdating text= (ITextUpdating) refactoring.getAdapter(ITextUpdating.class);
				if (text != null && text.canEnableTextUpdating()) {
					final boolean result= text.getUpdateTextualMatches();
					if (result)
						settings.add(RefactoringMessages.RenameInputWizardPage_textual_setting);
				}
				final IQualifiedNameUpdating qualified= (IQualifiedNameUpdating) refactoring.getAdapter(IQualifiedNameUpdating.class);
				if (qualified != null && qualified.canEnableQualifiedNameUpdating()) {
					final boolean result= qualified.getUpdateQualifiedNames();
					if (result)
						settings.add(RefactoringMessages.RenameInputWizardPage_qualified_setting);
				}
				if (!settings.isEmpty()) {
					String pattern= null;
					final Object[] objects= settings.toArray(new String[settings.size()]);
					switch (objects.length) {
						case 1:
							pattern= RefactoringMessages.RenameInputWizardPage_update_pattern_one;
							break;
						case 2:
							pattern= RefactoringMessages.RenameInputWizardPage_update_pattern_two;
							break;
						default:
							pattern= RefactoringMessages.RenameInputWizardPage_update_pattern_three;
							break;
					}
					return Messages.format(pattern, objects);
				}
				return null;
			}
		});
		final Composite composite= new Composite(expandable, SWT.NONE);
		expandable.setClient(composite);
		composite.setLayout(new GridLayout(1, false));
		fUpdateReferences= new Button(composite, SWT.CHECK);
		fUpdateReferences.setText(RefactoringMessages.RenameInputWizardPage_update_references);
		fUpdateReferences.setSelection(true);
		makeScrollable(fUpdateReferences); // bug 77901
		referenceUpdating.setUpdateReferences(fUpdateReferences.getSelection());
		fUpdateReferences.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				referenceUpdating.setUpdateReferences(fUpdateReferences.getSelection());
			}
		});
		value= getBooleanSetting(UPDATE_TEXTUAL_MATCHES, textUpdating.getUpdateTextualMatches());
		fUpdateTextualMatches= new Button(composite, SWT.CHECK);
		fUpdateTextualMatches.setText(RefactoringMessages.RenameInputWizardPage_update_textual_matches);
		fUpdateTextualMatches.setSelection(value);
		makeScrollable(fUpdateTextualMatches);
		textUpdating.setUpdateTextualMatches(fUpdateTextualMatches.getSelection());
		fUpdateTextualMatches.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				textUpdating.setUpdateTextualMatches(fUpdateTextualMatches.getSelection());
				updateForcePreview();
			}
		});
		value= getBooleanSetting(UPDATE_QUALIFIED_NAMES, qualifiedUpdating.getUpdateQualifiedNames());
		fUpdateQualifiedNames= new Button(composite, SWT.CHECK);
		int indent= marginWidth + fUpdateQualifiedNames.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
		fUpdateQualifiedNames.setText(RefactoringMessages.RenameInputWizardPage_update_qualified_names);
		fUpdateQualifiedNames.setSelection(value);
		makeScrollable(fUpdateQualifiedNames);
		fQualifiedNameComponent= new QualifiedNameComponent(composite, SWT.NONE, qualifiedUpdating, getRefactoringSettings());
		fQualifiedNameComponent.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		GridData data= (GridData) fQualifiedNameComponent.getLayoutData();
		data.horizontalAlignment= GridData.FILL;
		data.horizontalIndent= indent;
		makeScrollable(fQualifiedNameComponent);
		updateQulifiedNameUpdating(qualifiedUpdating, value);

		fUpdateQualifiedNames.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				boolean enabled= ((Button) e.widget).getSelection();
				updateQulifiedNameUpdating(qualifiedUpdating, enabled);
			}
		});
	}

	private void updateQulifiedNameUpdating(final IQualifiedNameUpdating qualifiedUpdating, boolean enabled) {
		fQualifiedNameComponent.setEnabled(enabled);
		qualifiedUpdating.setUpdateQualifiedNames(enabled);
		updateForcePreview();
	}

	private void addOptionalLeaveDelegateCheckbox(final Composite parent) {
		final IDelegatingUpdating delegateUpdating= (IDelegatingUpdating) getRefactoring().getAdapter(IDelegatingUpdating.class);
		if (delegateUpdating == null || !delegateUpdating.canEnableDelegatingUpdating())
			return;
		fLeaveDelegateSection= createExpandableSection(parent, getRefactoring(), RefactoringMessages.RenameInputWizardPage_delegation_section, 1, new IDescriptionProvider() {

			public String getDescription(final Refactoring refactoring) {
				final IDelegatingUpdating updating= (IDelegatingUpdating) refactoring.getAdapter(IDelegatingUpdating.class);
				if (updating != null && updating.canEnableDelegatingUpdating()) {
					final boolean result= updating.getDelegatingUpdating();
					if (result)
						return RefactoringMessages.RenameInputWizardPage_delegate_setting;
				}
				return null;
			}
		});
		final Composite composite= new Composite(fLeaveDelegateSection, SWT.NONE);
		fLeaveDelegateSection.setClient(composite);
		composite.setLayout(new GridLayout(1, false));
		fLeaveDelegateCheckBox= new Button(composite, SWT.CHECK);
		fLeaveDelegateCheckBox.setText(DelegateUIHelper.getLeaveDelegateCheckBoxTitle(false));
		fLeaveDelegateCheckBox.setSelection(DelegateUIHelper.loadLeaveDelegateSetting(delegateUpdating));
		makeScrollable(fLeaveDelegateCheckBox);
		delegateUpdating.setDelegatingUpdating(fLeaveDelegateCheckBox.getSelection());
		fLeaveDelegateCheckBox.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent event) {
				delegateUpdating.setDelegatingUpdating(fLeaveDelegateCheckBox.getSelection());
				fLeaveDelegateSection.updateDescription();
			}
		});
		fLeaveDelegateSection.updateDescription();
	}

	protected void updateLeaveDelegateCheckbox(int delegateCount) {
		if (fLeaveDelegateCheckBox == null)
			return;
		fLeaveDelegateCheckBox.setEnabled(delegateCount > 0);
		fLeaveDelegateCheckBox.setText(DelegateUIHelper.getLeaveDelegateCheckBoxTitle(delegateCount > 1));
		if (delegateCount == 0) {
			fLeaveDelegateCheckBox.setSelection(false);
			final IDelegatingUpdating delegateUpdating= (IDelegatingUpdating) getRefactoring().getAdapter(IDelegatingUpdating.class);
			delegateUpdating.setDelegatingUpdating(false);
		}
		if (fLeaveDelegateSection == null)
			return;
		fLeaveDelegateSection.updateDescription();
	}

	protected String getLabelText() {
		return RefactoringMessages.RenameInputWizardPage_new_name; 
	}

	protected boolean getBooleanSetting(String key, boolean defaultValue) {
		String update= getRefactoringSettings().get(key);
		if (update != null)
			return Boolean.valueOf(update).booleanValue();
		else
			return defaultValue;
	}
	
	protected void saveBooleanSetting(String key, Button checkBox) {
		if (checkBox != null)
			getRefactoringSettings().put(key, checkBox.getSelection());
	}

	private void updateForcePreview() {
		boolean forcePreview= false;
		Refactoring refactoring= getRefactoring();
		ITextUpdating tu= (ITextUpdating) refactoring.getAdapter(ITextUpdating.class);
		IQualifiedNameUpdating qu= (IQualifiedNameUpdating)refactoring.getAdapter(IQualifiedNameUpdating.class);
		if (tu != null) {
			forcePreview= tu.getUpdateTextualMatches();
		}
		if (qu != null) {
			forcePreview |= qu.getUpdateQualifiedNames();
		}
		getRefactoringWizard().setForcePreviewReview(forcePreview);
	}
}
