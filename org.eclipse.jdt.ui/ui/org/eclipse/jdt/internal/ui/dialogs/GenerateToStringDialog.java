/*******************************************************************************
 * Copyright (c) 2008 Mateusz Matela and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Mateusz Matela <mateusz.matela@gmail.com> - [code manipulation] [dcr] toString() builder wizard - https://bugs.eclipse.org/bugs/show_bug.cgi?id=26070
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.dialogs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.fieldassist.ContentAssistCommandAdapter;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;

import org.eclipse.jdt.internal.corext.codemanipulation.tostringgeneration.GenerateToStringOperation;
import org.eclipse.jdt.internal.corext.codemanipulation.tostringgeneration.ToStringGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.tostringgeneration.ToStringTemplateParser;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaElementImageDescriptor;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.util.SWTUtil;
import org.eclipse.jdt.internal.ui.viewsupport.BindingLabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;

/**
 * Dialog for the generate toString() action.
 * 
 * @since 3.5
 */
public class GenerateToStringDialog extends SourceActionDialog {

	private static class GenerateToStringContentProvider implements ITreeContentProvider {

		private static final Object[] EMPTY= new Object[0];

		private Object[] fFields;

		private Object[] fMethods;

		private Object[] fInheritedFields;

		private Object[] fInheritedMethods;

		private Object[] mainNodes;

		private static final String fieldsNode= JavaUIMessages.GenerateToStringDialog_fields_node;

		private static final String methodsNode= JavaUIMessages.GenerateToStringDialog_methods_node;

		private static final String inheritedFieldsNode= JavaUIMessages.GenerateToStringDialog_inherited_fields_node;

		private static final String inheritedMethodsNode= JavaUIMessages.GenerateToStringDialog_inherited_methods_node;

		public GenerateToStringContentProvider(IVariableBinding[] fields, IVariableBinding[] inheritedFields, IMethodBinding[] methods, IMethodBinding[] inheritedMethods) {
			ArrayList nodes= new ArrayList();
			fFields= (Object[])fields.clone();
			if (fFields.length > 0)
				nodes.add(fieldsNode);
			fInheritedFields= (Object[])inheritedFields.clone();
			if (fInheritedFields.length > 0)
				nodes.add(inheritedFieldsNode);
			fMethods= (Object[])methods.clone();
			if (fMethods.length > 0)
				nodes.add(methodsNode);
			fInheritedMethods= (Object[])inheritedMethods.clone();
			if (fInheritedMethods.length > 0)
				nodes.add(inheritedMethodsNode);
			mainNodes= nodes.toArray();
		}

		private int getElementPosition(Object element, Object[] array) {
			for (int i= 0; i < array.length; i++) {
				if (array[i].equals(element)) {
					return i;
				}
			}
			return -1;
		}

		private Object[] getContainingArray(Object element) {
			if (element instanceof String)
				return mainNodes;
			if (element instanceof IVariableBinding) {
				if (getElementPosition(element, fFields) >= 0)
					return fFields;
				if (getElementPosition(element, fInheritedFields) >= 0)
					return fInheritedFields;
			}
			if (element instanceof IMethodBinding) {
				if (getElementPosition(element, fMethods) >= 0)
					return fMethods;
				if (getElementPosition(element, fInheritedMethods) >= 0)
					return fInheritedMethods;
			}
			return EMPTY;
		}

		public boolean canMoveDown(Object element) {
			Object[] array= getContainingArray(element);
			int position= getElementPosition(element, array);
			return position != -1 && position != array.length - 1;
		}

		public boolean canMoveUp(Object element) {
			return getElementPosition(element, getContainingArray(element)) > 0;
		}

		public void down(Object element, CheckboxTreeViewer tree) {
			move(element, tree, 1);
		}

		public void up(Object element, CheckboxTreeViewer tree) {
			move(element, tree, -1);
		}

		private void move(Object element, CheckboxTreeViewer tree, int direction) {
			Object[] array= getContainingArray(element);
			int position= getElementPosition(element, array);
			Object temp= array[position];
			array[position]= array[position + direction];
			array[position + direction]= temp;
			tree.setSelection(new StructuredSelection(element));
			tree.refresh();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
		 */
		public Object[] getChildren(Object parentElement) {
			if (parentElement == fieldsNode)
				return fFields;
			if (parentElement == inheritedFieldsNode)
				return fInheritedFields;
			if (parentElement == methodsNode)
				return fMethods;
			if (parentElement == inheritedMethodsNode)
				return fInheritedMethods;
			return EMPTY;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
		 */
		public Object getParent(Object element) {
			Object[] array= getContainingArray(element);
			if (array == fFields)
				return fieldsNode;
			if (array == fInheritedFields)
				return inheritedFieldsNode;
			if (array == fMethods)
				return methodsNode;
			if (array == fInheritedMethods)
				return inheritedMethodsNode;
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
		 */
		public boolean hasChildren(Object element) {
			if (element instanceof String)
				return true;
			return false;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
		 */
		public Object[] getElements(Object inputElement) {
			return mainNodes;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
		 */
		public void dispose() {
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer,
		 * java.lang.Object, java.lang.Object)
		 */
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
	}

	private static class GenerateToStringLabelProvider extends BindingLabelProvider {
		public Image getImage(Object element) {
			ImageDescriptor descriptor= null;
			if (element == GenerateToStringContentProvider.fieldsNode || element == GenerateToStringContentProvider.inheritedFieldsNode)
				descriptor= JavaPluginImages.DESC_FIELD_PUBLIC;
			if (element == GenerateToStringContentProvider.methodsNode || element == GenerateToStringContentProvider.inheritedMethodsNode)
				descriptor= JavaPluginImages.DESC_MISC_PUBLIC;
			if (descriptor != null) {
				descriptor= new JavaElementImageDescriptor(descriptor, 0, JavaElementImageProvider.BIG_SIZE);
				return JavaPlugin.getImageDescriptorRegistry().get(descriptor);
			}
			return super.getImage(element);
		}
	}

	private static class GenerateToStringValidator implements ISelectionStatusValidator {

		private int fNumFields;

		private int fNumMethods;

		public GenerateToStringValidator(int fields, int methods) {
			fNumFields= fields;
			fNumMethods= methods;
		}


		public IStatus validate(Object[] selection) {
			int countFields= 0, countMethods= 0;
			for (int index= 0; index < selection.length; index++) {
				if (selection[index] instanceof IVariableBinding)
					countFields++;
				else if (selection[index] instanceof IMethodBinding)
					countMethods++;
			}

			return new StatusInfo(IStatus.INFO, Messages.format(JavaUIMessages.GenerateToStringDialog_selectioninfo_more, new String[] { String.valueOf(countFields), String.valueOf(fNumFields),
					String.valueOf(countMethods), String.valueOf(fNumMethods) }));
		}
	}

	private class ToStringTemplatesDialog extends Dialog {

		private class ToStringTemplateProposalProvider implements IContentProposalProvider {
			private class Proposal implements IContentProposal {
				final private String proposal;

				private String content;

				public Proposal(String proposal) {
					this.proposal= proposal;
				}

				public String getContent() {
					for (int i= 1; i < Math.min(proposal.length(), latestPosition); i++) {
						if (proposal.substring(0, i).equals(latestContents.substring(latestPosition - i, latestPosition))) {
							return content= proposal.substring(i);
						}
					}
					return proposal;
				}

				public int getCursorPosition() {
					if (content != null)
						return content.length();
					return proposal.length();
				}

				public String getDescription() {
					return (String)parser.getVariableDescriptions().get(proposal);
				}

				public String getLabel() {
					return proposal;
				}
			}

			private IContentProposal[] proposals;

			private String latestContents;

			private int latestPosition;

			public IContentProposal[] getProposals(String contents, int position) {
				if (proposals == null) {
					List proposalsList= new ArrayList();
					String[] tokens= parser.getVariables();
					for (int i= 0; i < tokens.length; i++) {
						proposalsList.add(new Proposal(tokens[i]));
					}
					proposals= (IContentProposal[])proposalsList.toArray(new IContentProposal[0]);
				}
				this.latestContents= contents;
				this.latestPosition= position;
				return proposals;
			}
		}

		private final int ADD_BUTTON= IDialogConstants.CLIENT_ID + 1;

		private final int REMOVE_BUTTON= IDialogConstants.CLIENT_ID + 2;

		private final int APPLY_BUTTON= IDialogConstants.CLIENT_ID + 3;

		private Text templateTextControl;

		private Combo templateNameControl;

		private ToStringTemplateParser parser;

		private List templateNames;

		private List templates;

		private int selectedTemplateNumber;

		private boolean controlsRefreshing= false;

		protected ToStringTemplatesDialog(Shell parentShell, ToStringTemplateParser parser) {
			super(parentShell);
			this.parser= parser;
			this.setShellStyle(this.getShellStyle() | SWT.RESIZE);
			this.create();
		}

		protected Control createDialogArea(Composite parent) {
			getShell().setText(JavaUIMessages.GenerateToStringDialog_templatesManagerTitle);

			Composite composite= (Composite)super.createDialogArea(parent);
			((GridLayout)composite.getLayout()).numColumns= 2;

			Composite leftComposite= new Composite(composite, SWT.NONE);
			leftComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			GridLayout gl= new GridLayout(2, false);
			gl.marginWidth= gl.marginHeight= 0;
			leftComposite.setLayout(gl);

			Label label= new Label(leftComposite, SWT.RIGHT);
			label.setText(JavaUIMessages.GenerateToStringDialog_template_name);

			templateNameControl= new Combo(leftComposite, SWT.FILL);
			templateNameControl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			SWTUtil.setDefaultVisibleItemCount(templateNameControl);
			templateNameControl.addSelectionListener(new SelectionListener() {
				public void widgetDefaultSelected(SelectionEvent e) {
				}

				public void widgetSelected(SelectionEvent e) {
					if (templateNameControl.getSelectionIndex() >= 0) {
						selectedTemplateNumber= templateNameControl.getSelectionIndex();
						templateTextControl.setText((String)templates.get(fGenerationSettings.stringFormatTemplateNumber));
					}
				}
			});

			label= new Label(leftComposite, SWT.LEFT);
			label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
			label.setText(JavaUIMessages.GenerateToStringDialog_template_content);

			templateTextControl= new Text(leftComposite, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
			new ContentAssistCommandAdapter(templateTextControl, new TextContentAdapter(), new ToStringTemplateProposalProvider(), null, new char[] { '$' }, true).setPropagateKeys(false);

			//Ctrl+Enter should execute the default button
			templateTextControl.addTraverseListener(new TraverseListener() {
				public void keyTraversed(TraverseEvent e) {
					if (e.stateMask == SWT.CTRL) {
						buttonPressed(((Integer)getShell().getDefaultButton().getData()).intValue());
					}
				}
			});

			GridData gd= new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
			gd.heightHint= 80;
			gd.widthHint= 450;
			templateTextControl.setLayoutData(gd);

			Composite rightComposite= new Composite(composite, SWT.NONE);
			gl= new GridLayout();
			gl.marginWidth= gl.marginHeight= 0;
			rightComposite.setLayout(gl);
			rightComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
			createButton(rightComposite, ADD_BUTTON, JavaUIMessages.GenerateToStringDialog_templatesManagerNewButton, false);
			createButton(rightComposite, REMOVE_BUTTON, JavaUIMessages.GenerateToStringDialog_templatesManagerRemoveButton, false);
			((GridLayout)rightComposite.getLayout()).numColumns= 1;


			templateNames= new ArrayList(Arrays.asList(getTemplateNames()));
			templates= new ArrayList(Arrays.asList(getTemplates(getDialogSettings())));
			selectedTemplateNumber= fGenerationSettings.stringFormatTemplateNumber;
			refreshControls();

			templateNameControl.addModifyListener(new ModifyListener() {
				private boolean selfRefreshing= false;

				public void modifyText(ModifyEvent e) {
					if (selectedTemplateNumber >= 0 && !controlsRefreshing && !selfRefreshing
							&& (templateNameControl.getSelectionIndex() == -1 || !templateNameControl.getText().equals(templateNames.get(templateNameControl.getSelectionIndex())))) {
						templateNames.set(selectedTemplateNumber, templateNameControl.getText());
						Point textSelection= templateNameControl.getSelection();
						selfRefreshing= true;
						templateNameControl.setItem(selectedTemplateNumber, templateNameControl.getText());
						templateNameControl.select(selectedTemplateNumber);
						templateNameControl.setSelection(textSelection);
						selfRefreshing= false;
						getButton(APPLY_BUTTON).setEnabled(true);
					}
				}
			});
			templateTextControl.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					String newTemplate= fixLineBreaks(templateTextControl.getText());
					if (selectedTemplateNumber >= 0 && !newTemplate.equals(templates.get(selectedTemplateNumber))) {
						templates.set(selectedTemplateNumber, newTemplate);
						getButton(APPLY_BUTTON).setEnabled(true);
					}
				}
			});
			
			applyDialogFont(composite);

			return composite;
		}
		
		private String fixLineBreaks(String input) {
			String systemLineDelimiter= Text.DELIMITER;
			final String javaLineDelimiter= "\n"; //$NON-NLS-1$
			if (!systemLineDelimiter.equals(javaLineDelimiter)) {
				StringBuffer outputBuffer= new StringBuffer(input);
				int pos= outputBuffer.indexOf(systemLineDelimiter);
				while (pos >= 0) {
					outputBuffer.delete(pos, pos+systemLineDelimiter.length());
					outputBuffer.insert(pos, javaLineDelimiter);
					pos= outputBuffer.indexOf(systemLineDelimiter, pos + javaLineDelimiter.length());
				}
				return outputBuffer.toString();
			}
			return input;
		}

		protected void createButtonsForButtonBar(Composite parent) {
			super.createButtonsForButtonBar(parent);
			createButton(parent, APPLY_BUTTON, JavaUIMessages.GenerateToStringDialog_templateManagerApplyButton, false).setEnabled(false);
		}

		private void applyChanges() {
			getDialogSettings().put(ToStringGenerationSettings.SETTINGS_TEMPLATE_NAMES, (String[])templateNames.toArray(new String[0]));
			getDialogSettings().put(ToStringGenerationSettings.SETTINGS_TEMPLATES, (String[])templates.toArray(new String[0]));
			fGenerationSettings.stringFormatTemplateNumber= Math.max(selectedTemplateNumber, 0);
			getButton(APPLY_BUTTON).setEnabled(false);
		}

		protected void buttonPressed(int buttonId) {
			switch (buttonId) {
				case APPLY_BUTTON:
					applyChanges();
					break;
				case IDialogConstants.OK_ID:
					applyChanges();
					close();
					break;
				case IDialogConstants.CANCEL_ID:
					close();
					break;
				case ADD_BUTTON:
					templateNames.add(JavaUIMessages.GenerateToStringDialog_newTemplateName);
					templates.add(ToStringTemplateParser.DEFAULT_TEMPLATE);
					selectedTemplateNumber= templateNames.size() - 1;
					getButton(APPLY_BUTTON).setEnabled(true);
					refreshControls();
					break;
				case REMOVE_BUTTON:
					if (templateNames.size() > 0) {
						templateNames.remove(selectedTemplateNumber);
						templates.remove(selectedTemplateNumber);
					}
					if (selectedTemplateNumber >= templateNames.size())
						selectedTemplateNumber= templateNames.size() - 1;
					getButton(APPLY_BUTTON).setEnabled(true);
					refreshControls();
					break;
			}
		}

		public void refreshControls() {
			controlsRefreshing= true;
			templateNameControl.setItems((String[])templateNames.toArray(new String[0]));
			if (templateNames.size() > 0) {
				templateNameControl.select(selectedTemplateNumber);
				templateTextControl.setText((String)templates.get(selectedTemplateNumber));
			} else {
				templateTextControl.setText(""); //$NON-NLS-1$
			}
			controlsRefreshing= false;
		}
	}

	private ToStringGenerationSettings fGenerationSettings;

	private static final int DOWN_BUTTON= IDialogConstants.CLIENT_ID + 2;

	private static final int UP_BUTTON= IDialogConstants.CLIENT_ID + 1;

	protected Button[] fButtonControls;

	boolean[] fButtonsEnabled;

	private static final int DOWN_INDEX= 1;

	private static final int UP_INDEX= 0;

	public ToStringGenerationSettings getGenerationSettings() {
		return fGenerationSettings;
	}

	public static String[] getTemplates(IDialogSettings dialogSettings) {
		String[] result= dialogSettings.getArray(ToStringGenerationSettings.SETTINGS_TEMPLATES);
		if (result != null && result.length > 0)
			return result;
		return new String[] { ToStringTemplateParser.DEFAULT_TEMPLATE };
	}

	public String[] getTemplateNames() {
		String[] result= getDialogSettings().getArray(ToStringGenerationSettings.SETTINGS_TEMPLATE_NAMES);
		if (result != null && result.length > 0)
			return result;
		return new String[] { JavaUIMessages.GenerateToStringDialog_defaultTemplateName };
	}

	public int getSelectedTemplate() {
		try {
			int result= getDialogSettings().getInt(ToStringGenerationSettings.SETTINGS_SELECTED_TEMPLATE);
			if (result < 0)
				return 0;
			return result;
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	public void setSelectedTemplate(int templateNumber) {
		getDialogSettings().put(ToStringGenerationSettings.SETTINGS_SELECTED_TEMPLATE, templateNumber);
	}

	public GenerateToStringDialog(Shell shell, CompilationUnitEditor editor, IType type, IVariableBinding[] fields, IVariableBinding[] inheritedFields, IVariableBinding[] selectedFields,
			IMethodBinding[] methods, IMethodBinding[] inheritededMethods) throws JavaModelException {
		super(shell, new BindingLabelProvider(), new GenerateToStringContentProvider(fields, inheritedFields, methods, inheritededMethods), editor, type, false);
		setEmptyListMessage(JavaUIMessages.GenerateHashCodeEqualsDialog_no_entries);

		List selected= new ArrayList(Arrays.asList(selectedFields));
		if (selectedFields.length == fields.length && selectedFields.length > 0)
			selected.add(getContentProvider().getParent(selectedFields[0]));
		setInitialElementSelections(selected);

		setTitle(JavaUIMessages.GenerateToStringDialog_dialog_title);
		setMessage(JavaUIMessages.GenerateToStringDialog_select_fields_to_include);
		setValidator(new GenerateToStringValidator(fields.length + inheritedFields.length, methods.length + inheritededMethods.length));
		setSize(60, 18);
		setInput(new Object());

		fGenerationSettings= new ToStringGenerationSettings(getDialogSettings());
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean close() {
		fGenerationSettings.writeDialogSettings(getDialogSettings());

		fGenerationSettings.stringFormatTemplate= getTemplates(getDialogSettings())[fGenerationSettings.stringFormatTemplateNumber];

		fGenerationSettings.createComments= getGenerateComment();

		return super.close();
	}

	public Object[] getResult() {
		Object[] oldResult= super.getResult();
		List newResult= new ArrayList();
		for (int i= 0; i < oldResult.length; i++) {
			if (!(oldResult[i] instanceof String)) {
				newResult.add(oldResult[i]);
			}
		}
		return newResult.toArray();
	}

	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(shell, IJavaHelpContextIds.GENERATE_TOSTRING_SELECTION_DIALOG);
	}

	protected CheckboxTreeViewer createTreeViewer(Composite parent) {
		CheckboxTreeViewer treeViewer= super.createTreeViewer(parent);

		treeViewer.setLabelProvider(new GenerateToStringLabelProvider());

		//expandAll because setSubtreeChecked() used in CheckStateListener below assumes that elements have been expanded
		treeViewer.expandAll();
		//but actually we only need one branch expanded
		treeViewer.collapseAll();
		treeViewer.expandToLevel(GenerateToStringContentProvider.fieldsNode, 1);

		treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection= (IStructuredSelection)getTreeViewer().getSelection();

				Object selected= selection.size() > 0 ? selection.toList().get(0) : null;
				GenerateToStringContentProvider cp= (GenerateToStringContentProvider)getContentProvider();

				fButtonControls[UP_INDEX].setEnabled(cp.canMoveUp(selected));
				fButtonControls[DOWN_INDEX].setEnabled(cp.canMoveDown(selected));
			}

		});
		treeViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				getTreeViewer().setSubtreeChecked(event.getElement(), event.getChecked());
				getTreeViewer().setGrayed(event.getElement(), false);
				Object parentElement= ((ITreeContentProvider)(getTreeViewer().getContentProvider())).getParent(event.getElement());
				if (parentElement != null) {
					Object[] siblings= ((ITreeContentProvider)(getTreeViewer().getContentProvider())).getChildren(parentElement);
					int count= 0;
					for (int i= 0; i < siblings.length; i++) {
						if (getTreeViewer().getChecked(siblings[i]))
							count++;
					}
					if (count == 0)
						getTreeViewer().setGrayChecked(parentElement, false);
					else if (count == siblings.length) {
						getTreeViewer().setChecked(parentElement, true);
						getTreeViewer().setGrayed(parentElement, false);
					} else
						getTreeViewer().setGrayChecked(parentElement, true);
				}
				updateOKStatus();
			}

		});
		return treeViewer;
	}

	protected Composite createSelectionButtons(Composite composite) {
		Composite buttonComposite= super.createSelectionButtons(composite);

		GridLayout layout= new GridLayout();
		buttonComposite.setLayout(layout);

		createUpDownButtons(buttonComposite);

		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 1;

		return buttonComposite;
	}

	protected void buttonPressed(int buttonId) {
		super.buttonPressed(buttonId);
		switch (buttonId) {
			case UP_BUTTON: {
				GenerateToStringContentProvider contentProvider= (GenerateToStringContentProvider)getTreeViewer().getContentProvider();
				List selection= ((IStructuredSelection)getTreeViewer().getSelection()).toList();
				if (selection.size() > 0)
					contentProvider.up(selection.get(0), getTreeViewer());
				updateOKStatus();
				break;
			}
			case DOWN_BUTTON: {
				GenerateToStringContentProvider contentProvider= (GenerateToStringContentProvider)getTreeViewer().getContentProvider();
				List selection= ((IStructuredSelection)getTreeViewer().getSelection()).toList();
				if (selection.size() > 0)
					contentProvider.down(selection.get(0), getTreeViewer());
				updateOKStatus();
				break;
			}
		}
	}

	protected void createUpDownButtons(Composite buttonComposite) {
		int numButtons= 2; // up, down
		fButtonControls= new Button[numButtons];
		fButtonsEnabled= new boolean[numButtons];
		fButtonControls[UP_INDEX]= createButton(buttonComposite, UP_BUTTON, JavaUIMessages.GenerateToStringDialog_up_button, false);
		fButtonControls[DOWN_INDEX]= createButton(buttonComposite, DOWN_BUTTON, JavaUIMessages.GenerateToStringDialog_down_button, false);
		boolean defaultState= false;
		fButtonControls[UP_INDEX].setEnabled(defaultState);
		fButtonControls[DOWN_INDEX].setEnabled(defaultState);
		fButtonsEnabled[UP_INDEX]= defaultState;
		fButtonsEnabled[DOWN_INDEX]= defaultState;
	}

	private Label formatLabel;

	private Combo formatCombo;

	private Button skipNullsButton;

	protected Composite createCommentSelection(Composite parentComposite) {
		Composite composite= super.createCommentSelection(parentComposite);

		Composite composite2= new Composite(composite, SWT.NONE);
		GridLayout layout= new GridLayout(3, false);
		layout.marginWidth= 0;
		layout.marginHeight= 0;
		composite2.setLayout(layout);
		composite2.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, true));

		formatLabel= new Label(composite2, SWT.RIGHT);
		formatLabel.setText(JavaUIMessages.GenerateToStringDialog_string_format_combo);
		GridData gridData= new GridData(SWT.FILL, SWT.CENTER, false, false);
		formatLabel.setLayoutData(gridData);

		formatCombo= new Combo(composite2, SWT.READ_ONLY);
		formatCombo.setItems(getTemplateNames());
		formatCombo.select(fGenerationSettings.stringFormatTemplateNumber);
		formatCombo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		SWTUtil.setDefaultVisibleItemCount(formatCombo);
		formatCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fGenerationSettings.stringFormatTemplateNumber= ((Combo)e.widget).getSelectionIndex();
			}
		});

		final Button formatButton= new Button(composite2, SWT.NONE);
		formatButton.setText(JavaUIMessages.GenerateToStringDialog_manage_templates_button);
		formatButton.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
		formatButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				manageTemplatesButtonSelected();
			}
		});

		final Label styleLabel= new Label(composite2, SWT.RIGHT);
		styleLabel.setText(JavaUIMessages.GenerateToStringDialog_code_style_combo);
		gridData= new GridData(SWT.FILL, SWT.CENTER, false, false);
		styleLabel.setLayoutData(gridData);

		final Combo styleCombo= new Combo(composite2, SWT.READ_ONLY);
		styleCombo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		styleCombo.setItems(GenerateToStringOperation.getStyleNames());
		styleCombo.select(Math.min(fGenerationSettings.toStringStyle, styleCombo.getItemCount() - 1));
		SWTUtil.setDefaultVisibleItemCount(styleCombo);
		styleCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				changeToStringStyle(((Combo)e.widget).getSelectionIndex());
			}
		});

		skipNullsButton= new Button(composite, SWT.CHECK);
		skipNullsButton.setText(JavaUIMessages.GenerateToStringDialog_skip_null_button);
		skipNullsButton.setSelection(fGenerationSettings.skipNulls);
		skipNullsButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				fGenerationSettings.skipNulls= ((Button)event.widget).getSelection();
			}
		});

		final Button arrayButton= new Button(composite, SWT.CHECK);
		arrayButton.setText(JavaUIMessages.GenerateToStringDialog_ignore_default_button);
		arrayButton.setSelection(fGenerationSettings.customArrayToString);
		arrayButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fGenerationSettings.customArrayToString= ((Button)e.widget).getSelection();
			}
		});

		composite2= new Composite(composite, SWT.NONE);
		RowLayout rowLayout= new RowLayout();
		rowLayout.center= true;
		rowLayout.marginLeft= rowLayout.marginRight= rowLayout.marginTop= rowLayout.marginBottom= 0;
		composite2.setLayout(rowLayout);

		final Button limitButton= new Button(composite2, SWT.CHECK);
		limitButton.setText(JavaUIMessages.GenerateToStringDialog_limit_elements_button);
		limitButton.setSelection(fGenerationSettings.limitElements);
		limitButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fGenerationSettings.limitElements= ((Button)e.widget).getSelection();
			}
		});

		final Spinner limitSpinner= new Spinner(composite2, SWT.BORDER);
		limitSpinner.setMinimum(0);
		limitSpinner.setSelection(fGenerationSettings.limitValue);
		limitSpinner.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fGenerationSettings.limitValue= ((Spinner)e.widget).getSelection();
			}
		});

		//invoked to change initial enable state of controls
		changeToStringStyle(styleCombo.getSelectionIndex());

		return composite;
	}

	private void manageTemplatesButtonSelected() {
		ToStringTemplatesDialog dialog= new ToStringTemplatesDialog(getShell(), GenerateToStringOperation.createTemplateParser(fGenerationSettings.toStringStyle));
		dialog.open();
		formatCombo.setItems(getTemplateNames());
		formatCombo.select(Math.min(fGenerationSettings.stringFormatTemplateNumber, formatCombo.getItemCount() - 1));
	}

	private void changeToStringStyle(int style) {
		fGenerationSettings.toStringStyle= style;
		skipNullsButton.setEnabled(style != GenerateToStringOperation.STRING_FORMAT);
		boolean enableFormat= style != GenerateToStringOperation.APACHE_BUILDER && style != GenerateToStringOperation.APACHE_BUILDER_CHAINED && style != GenerateToStringOperation.SPRING_CREATOR
				&& style != GenerateToStringOperation.SPRING_CREATOR_CHAINED;
		formatLabel.setEnabled(enableFormat);
		formatCombo.setEnabled(enableFormat);
	}

}
