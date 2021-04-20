/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     John Kaplan, johnkaplantech@gmail.com - 108071 [code templates] template for body of newly created class
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.preferences;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;

import org.eclipse.core.resources.IProject;

import org.eclipse.text.templates.TemplatePersistenceData;
import org.eclipse.text.templates.TemplateReaderWriter;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateContextType;

import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;

import org.eclipse.ui.texteditor.ITextEditorActionConstants;

import org.eclipse.jdt.internal.core.manipulation.CodeTemplateContextType;
import org.eclipse.jdt.internal.core.manipulation.ProjectTemplateStore;
import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.IJavaPartitions;
import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaSourceViewer;
import org.eclipse.jdt.internal.ui.preferences.EditTemplateDialog.TextViewerAction;
import org.eclipse.jdt.internal.ui.text.template.preferences.TemplateVariableProcessor;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ITreeListAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.TreeListDialogField;


public class CodeTemplateBlock extends OptionsConfigurationBlock {

	private class CodeTemplateAdapter extends ViewerComparator implements ITreeListAdapter<String>, IDialogFieldListener {

		private final Object[] NO_CHILDREN= new Object[0];

		@Override
		public void customButtonPressed(TreeListDialogField<String> field, int index) {
			doButtonPressed(index, field.getSelectedElements());
		}

		@Override
		public void selectionChanged(TreeListDialogField<String> field) {
			List<Object> selected= field.getSelectedElements();
			field.enableButton(IDX_EDIT, canEdit(selected));
			field.enableButton(IDX_EXPORT, !selected.isEmpty());

			updateSourceViewerInput(selected);
		}

		@Override
		public void doubleClicked(TreeListDialogField<String> field) {
			List<Object> selected= field.getSelectedElements();
			if (canEdit(selected)) {
				doButtonPressed(IDX_EDIT, selected);
			}
		}

		@Override
		public Object[] getChildren(TreeListDialogField<String> field, Object element) {
			if (element == COMMENT_NODE || element == CODE_NODE) {
				return getTemplateOfCategory(element == COMMENT_NODE);
			}
			return NO_CHILDREN;
		}

		@Override
		public Object getParent(TreeListDialogField<String> field, Object element) {
			if (element instanceof TemplatePersistenceData) {
				TemplatePersistenceData data= (TemplatePersistenceData) element;
				if (data.getTemplate().getName().endsWith(CodeTemplateContextType.COMMENT_SUFFIX)) {
					return COMMENT_NODE;
				}
				return CODE_NODE;
			}
			return null;
		}

		@Override
		public boolean hasChildren(TreeListDialogField<String> field, Object element) {
			return (element == COMMENT_NODE || element == CODE_NODE);
		}

		@Override
		public void dialogFieldChanged(DialogField field) {
			if (field == fGenerateComments) {
				setValue(PREF_GENERATE_COMMENTS, fGenerateComments.isSelected());
			}
		}

		@Override
		public void keyPressed(TreeListDialogField<String> field, KeyEvent event) {
		}

		@Override
		public int category(Object element) {
			if (element == COMMENT_NODE) {
				return 1;
			} else if (element == CODE_NODE) {
				return 2;
			}

			TemplatePersistenceData data= (TemplatePersistenceData) element;
			String id= data.getId();

			if (id != null) {
				switch (id) {
					case CodeTemplateContextType.NEWTYPE_ID:
						return 101;
					case CodeTemplateContextType.CLASSBODY_ID:
						return 102;
					case CodeTemplateContextType.INTERFACEBODY_ID:
						return 103;
					case CodeTemplateContextType.ENUMBODY_ID:
						return 104;
					case CodeTemplateContextType.ANNOTATIONBODY_ID:
						return 105;
					case CodeTemplateContextType.METHODSTUB_ID:
						return 106;
					case CodeTemplateContextType.CONSTRUCTORSTUB_ID:
						return 107;
					case CodeTemplateContextType.GETTERSTUB_ID:
						return 108;
					case CodeTemplateContextType.SETTERSTUB_ID:
						return 109;
					case CodeTemplateContextType.CATCHBLOCK_ID:
						return 110;
					case CodeTemplateContextType.RECORDBODY_ID:
						return 111;
					case CodeTemplateContextType.FILECOMMENT_ID:
						return 1;
					case CodeTemplateContextType.TYPECOMMENT_ID:
						return 2;
					case CodeTemplateContextType.FIELDCOMMENT_ID:
						return 3;
					case CodeTemplateContextType.CONSTRUCTORCOMMENT_ID:
						return 4;
					case CodeTemplateContextType.METHODCOMMENT_ID:
						return 5;
					case CodeTemplateContextType.OVERRIDECOMMENT_ID:
						return 6;
					case CodeTemplateContextType.DELEGATECOMMENT_ID:
						return 7;
					case CodeTemplateContextType.GETTERCOMMENT_ID:
						return 8;
					case CodeTemplateContextType.SETTERCOMMENT_ID:
						return 9;
					case CodeTemplateContextType.MODULECOMMENT_ID:
						return 10;
					default:
						break;
				}
			}
			return 1000;
		}
	}

	private static class CodeTemplateLabelProvider extends LabelProvider {

		@Override
		public Image getImage(Object element) {
			return null;

		}

		@Override
		public String getText(Object element) {
			if (element == COMMENT_NODE || element == CODE_NODE) {
				return (String) element;
			}
			TemplatePersistenceData data= (TemplatePersistenceData) element;
			String id=data.getId();
			if (id != null) {
				switch (id) {
					case CodeTemplateContextType.CATCHBLOCK_ID:
						return PreferencesMessages.CodeTemplateBlock_catchblock_label;
					case CodeTemplateContextType.METHODSTUB_ID:
						return PreferencesMessages.CodeTemplateBlock_methodstub_label;
					case CodeTemplateContextType.CONSTRUCTORSTUB_ID:
						return PreferencesMessages.CodeTemplateBlock_constructorstub_label;
					case CodeTemplateContextType.GETTERSTUB_ID:
						return PreferencesMessages.CodeTemplateBlock_getterstub_label;
					case CodeTemplateContextType.SETTERSTUB_ID:
						return PreferencesMessages.CodeTemplateBlock_setterstub_label;
					case CodeTemplateContextType.NEWTYPE_ID:
						return PreferencesMessages.CodeTemplateBlock_newtype_label;
					case CodeTemplateContextType.CLASSBODY_ID:
						return PreferencesMessages.CodeTemplateBlock_classbody_label;
					case CodeTemplateContextType.INTERFACEBODY_ID:
						return PreferencesMessages.CodeTemplateBlock_interfacebody_label;
					case CodeTemplateContextType.ENUMBODY_ID:
						return PreferencesMessages.CodeTemplateBlock_enumbody_label;
					case CodeTemplateContextType.RECORDBODY_ID:
						return PreferencesMessages.CodeTemplateBlock_recordbody_label;
					case CodeTemplateContextType.ANNOTATIONBODY_ID:
						return PreferencesMessages.CodeTemplateBlock_annotationbody_label;
					case CodeTemplateContextType.FILECOMMENT_ID:
						return PreferencesMessages.CodeTemplateBlock_filecomment_label;
					case CodeTemplateContextType.TYPECOMMENT_ID:
						return PreferencesMessages.CodeTemplateBlock_typecomment_label;
					case CodeTemplateContextType.FIELDCOMMENT_ID:
						return PreferencesMessages.CodeTemplateBlock_fieldcomment_label;
					case CodeTemplateContextType.METHODCOMMENT_ID:
						return PreferencesMessages.CodeTemplateBlock_methodcomment_label;
					case CodeTemplateContextType.OVERRIDECOMMENT_ID:
						return PreferencesMessages.CodeTemplateBlock_overridecomment_label;
					case CodeTemplateContextType.DELEGATECOMMENT_ID:
						return PreferencesMessages.CodeTemplateBlock_delegatecomment_label;
					case CodeTemplateContextType.CONSTRUCTORCOMMENT_ID:
						return PreferencesMessages.CodeTemplateBlock_constructorcomment_label;
					case CodeTemplateContextType.GETTERCOMMENT_ID:
						return PreferencesMessages.CodeTemplateBlock_gettercomment_label;
					case CodeTemplateContextType.SETTERCOMMENT_ID:
						return PreferencesMessages.CodeTemplateBlock_settercomment_label;
					case CodeTemplateContextType.MODULECOMMENT_ID:
						return PreferencesMessages.CodeTemplateBlock_modulecomment_label;
					default:
						break;
				}
			}
			return data.getTemplate().getDescription();
		}
	}

	private static final Key PREF_GENERATE_COMMENTS= getJDTUIKey(PreferenceConstants.CODEGEN_ADD_COMMENTS);

	private static Key[] getAllKeys() {
		return new Key[] {
			PREF_GENERATE_COMMENTS
		};
	}

	private final static int IDX_EDIT= 0;
	private final static int IDX_IMPORT= 2;
	private final static int IDX_EXPORT= 3;
	private final static int IDX_EXPORTALL= 4;

	protected final static String COMMENT_NODE= PreferencesMessages.CodeTemplateBlock_templates_comment_node;
	protected final static String CODE_NODE= PreferencesMessages.CodeTemplateBlock_templates_code_node;

	private TreeListDialogField<String> fCodeTemplateTree;
	private SelectionButtonDialogField fGenerateComments;

	protected ProjectTemplateStore fTemplateStore;

	private PixelConverter fPixelConverter;
	private SourceViewer fPatternViewer;
	private TextViewerAction fPatternViewerCopyAction;
	private TextViewerAction fPatternViewerSelectAllAction;

	private TemplateVariableProcessor fTemplateProcessor;

	public CodeTemplateBlock(IStatusChangeListener context, IProject project, IWorkbenchPreferenceContainer container) {
		super(context, project, getAllKeys(), container);

		fTemplateStore= new ProjectTemplateStore(project);
		try {
			fTemplateStore.load();
		} catch (IOException e) {
			JavaPlugin.log(e);
		}

		fTemplateProcessor= new TemplateVariableProcessor();

		CodeTemplateAdapter adapter= new CodeTemplateAdapter();

		String[] buttonLabels= new String[] {
			PreferencesMessages.CodeTemplateBlock_templates_edit_button,
			/* */ null,
			PreferencesMessages.CodeTemplateBlock_templates_import_button,
			PreferencesMessages.CodeTemplateBlock_templates_export_button,
			PreferencesMessages.CodeTemplateBlock_templates_exportall_button

		};
		fCodeTemplateTree= new TreeListDialogField<>(adapter, buttonLabels, new CodeTemplateLabelProvider());
		fCodeTemplateTree.setDialogFieldListener(adapter);
		fCodeTemplateTree.setLabelText(PreferencesMessages.CodeTemplateBlock_templates_label);
		fCodeTemplateTree.setViewerComparator(adapter);

		fCodeTemplateTree.enableButton(IDX_EXPORT, false);
		fCodeTemplateTree.enableButton(IDX_EDIT, false);

		fCodeTemplateTree.addElement(COMMENT_NODE);
		fCodeTemplateTree.addElement(CODE_NODE);

		fCodeTemplateTree.selectFirstElement();

		fGenerateComments= new SelectionButtonDialogField(SWT.CHECK | SWT.WRAP);
		fGenerateComments.setDialogFieldListener(adapter);
		fGenerateComments.setLabelText(PreferencesMessages.CodeTemplateBlock_createcomment_label);

		updateControls();
	}

	public void postSetSelection(Object element) {
		fCodeTemplateTree.postSetSelection(new StructuredSelection(element));
	}

	@Override
	public boolean hasProjectSpecificOptions(IProject project) {
		if (super.hasProjectSpecificOptions(project))
			return true;

		if (project != null) {
			return ProjectTemplateStore.hasProjectSpecificTempates(project);
		}
		return false;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock#useProjectSpecificSettings(boolean)
	 * @since 3.5
	 */
	@Override
	public void useProjectSpecificSettings(boolean enable) {
		fCodeTemplateTree.setEnabled(enable); // need to set because super implementation only updates controls
		super.useProjectSpecificSettings(enable);
	}

	@Override
	protected Control createContents(Composite parent) {
		fPixelConverter=  new PixelConverter(parent);

		setShell(parent.getShell());

		Composite composite=  new Composite(parent, SWT.NONE);
		composite.setFont(parent.getFont());

		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 2;
		composite.setLayout(layout);

		fCodeTemplateTree.doFillIntoGrid(composite, 3);
		LayoutUtil.setHorizontalSpan(fCodeTemplateTree.getLabelControl(null), 2);
		LayoutUtil.setHorizontalGrabbing(fCodeTemplateTree.getTreeControl(null));

		fPatternViewer= createViewer(composite, 2);
		addActionsToPatternViewer(fPatternViewer);

		fGenerateComments.doFillIntoGrid(composite, 2);

		return composite;
	}

	@Override
	protected void updateControls() {
		fGenerateComments.setSelection(getBooleanValue(PREF_GENERATE_COMMENTS));
	}

	private SourceViewer createViewer(Composite parent, int nColumns) {
		Label label= new Label(parent, SWT.NONE);
		label.setText(PreferencesMessages.CodeTemplateBlock_preview);
		GridData data= new GridData();
		data.horizontalSpan= nColumns;
		label.setLayoutData(data);

		IDocument document= new Document();
		JavaTextTools tools= JavaPlugin.getDefault().getJavaTextTools();
		tools.setupJavaDocumentPartitioner(document, IJavaPartitions.JAVA_PARTITIONING);
		IPreferenceStore store= JavaPlugin.getDefault().getCombinedPreferenceStore();
		SourceViewer viewer= new JavaSourceViewer(parent, null, null, false, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL, store);
		CodeTemplateSourceViewerConfiguration configuration= new CodeTemplateSourceViewerConfiguration(tools.getColorManager(), store, null, fTemplateProcessor);
		viewer.configure(configuration);

		viewer.setEditable(false);
		Cursor arrowCursor= viewer.getTextWidget().getDisplay().getSystemCursor(SWT.CURSOR_ARROW);
		viewer.getTextWidget().setCursor(arrowCursor);

		// Don't set caret to 'null' as this causes https://bugs.eclipse.org/293263
//		viewer.getTextWidget().setCaret(null);

		viewer.setDocument(document);

		Font font= JFaceResources.getFont(PreferenceConstants.EDITOR_TEXT_FONT);
		viewer.getTextWidget().setFont(font);
		new JavaSourcePreviewerUpdater(viewer, configuration, store);

		Control control= viewer.getControl();
		data= new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.FILL_VERTICAL);
		data.horizontalSpan= nColumns;
		data.heightHint= fPixelConverter.convertHeightInCharsToPixels(5);
		control.setLayoutData(data);

		return viewer;
	}

	private void addActionsToPatternViewer(SourceViewer viewer) {
		// create actions
		fPatternViewerCopyAction = new TextViewerAction(viewer, ITextOperationTarget.COPY);
		fPatternViewerCopyAction.setActionDefinitionId(ActionFactory.COPY.getCommandId());
		fPatternViewerCopyAction.setText(PreferencesMessages.EditTemplateDialog_copy);

		fPatternViewerSelectAllAction = new TextViewerAction(viewer, ITextOperationTarget.SELECT_ALL);
		fPatternViewerSelectAllAction.setActionDefinitionId(ActionFactory.SELECT_ALL.getCommandId());
		fPatternViewerSelectAllAction.setText(PreferencesMessages.EditTemplateDialog_select_all);

		viewer.addSelectionChangedListener(this::updateCopyAction);
		// create context menu
		MenuManager manager = new MenuManager(null, null);
		manager.setRemoveAllWhenShown(true);
		manager.addMenuListener(this::fillPatternViewerContextMenu);

		StyledText text = viewer.getTextWidget();
		Menu menu = manager.createContextMenu(text);
		text.setMenu(menu);
	}

	private void fillPatternViewerContextMenu(IMenuManager menu) {
		menu.add(new Separator(ITextEditorActionConstants.GROUP_EDIT));
		menu.appendToGroup(ITextEditorActionConstants.GROUP_EDIT, fPatternViewerCopyAction);
		menu.appendToGroup(ITextEditorActionConstants.GROUP_EDIT, fPatternViewerSelectAllAction);
	}

	/**
	 * @param event The event
	 */
	private void updateCopyAction(SelectionChangedEvent event) {
		if (fPatternViewerCopyAction != null)
			fPatternViewerCopyAction.update();
	}

	protected TemplatePersistenceData[] getTemplateOfCategory(boolean isComment) {
		ArrayList<TemplatePersistenceData> res=  new ArrayList<>();
		for (TemplatePersistenceData curr : fTemplateStore.getTemplateData()) {
			if (isComment == curr.getTemplate().getName().endsWith(CodeTemplateContextType.COMMENT_SUFFIX)) {
				res.add(curr);
			}
		}
		return res.toArray(new TemplatePersistenceData[res.size()]);
	}

	protected static boolean canEdit(List<Object> selected) {
		return selected.size() == 1 && (selected.get(0) instanceof TemplatePersistenceData);
	}

	protected void updateSourceViewerInput(List<Object> selection) {
		if (fPatternViewer == null || fPatternViewer.getTextWidget().isDisposed()) {
			return;
		}
		if (selection.size() == 1 && selection.get(0) instanceof TemplatePersistenceData) {
			TemplatePersistenceData data= (TemplatePersistenceData) selection.get(0);
			Template template= data.getTemplate();
			TemplateContextType type= JavaPlugin.getDefault().getCodeTemplateContextRegistry().getContextType(template.getContextTypeId());
			fTemplateProcessor.setContextType(type);
			fPatternViewer.getDocument().set(template.getPattern());
		} else {
			fPatternViewer.getDocument().set(""); //$NON-NLS-1$
		}
	}

	protected void doButtonPressed(int buttonIndex, List<Object> selected) {
		switch (buttonIndex) {
		case IDX_EDIT:
			edit((TemplatePersistenceData) selected.get(0));
			break;
		case IDX_EXPORT:
			export(selected);
			break;
		case IDX_EXPORTALL:
			exportAll();
			break;
		case IDX_IMPORT:
			import_();
			break;
		default:
			break;
		}
	}

	private void edit(TemplatePersistenceData data) {
		Template newTemplate= new Template(data.getTemplate());
		EditTemplateDialog dialog= new EditTemplateDialog(getShell(), newTemplate, true, false, JavaPlugin.getDefault().getCodeTemplateContextRegistry());
		if (dialog.open() == Window.OK) {
			// changed
			data.setTemplate(dialog.getTemplate());
			fCodeTemplateTree.refresh(data);
			fCodeTemplateTree.selectElements(new StructuredSelection(data));
		}
	}

	private void import_() {
		FileDialog dialog= new FileDialog(getShell(), SWT.SHEET);
		dialog.setText(PreferencesMessages.CodeTemplateBlock_import_title);
		dialog.setFilterExtensions(new String[] {PreferencesMessages.CodeTemplateBlock_import_extension});
		String path= dialog.open();

		if (path == null)
			return;

		try {
			TemplateReaderWriter reader= new TemplateReaderWriter();
			File file= new File(path);
			if (file.exists()) {
				InputStream input= new BufferedInputStream(new FileInputStream(file));
				try {
					for (TemplatePersistenceData data : reader.read(input, null)) {
						updateTemplate(data);
					}
				} finally {
					try {
						input.close();
					} catch (IOException x) {
					}
				}
			}

			fCodeTemplateTree.refresh();
			updateSourceViewerInput(fCodeTemplateTree.getSelectedElements());

		} catch (IOException e) {
			openReadErrorDialog(e);
		}

	}

	private void updateTemplate(TemplatePersistenceData data) {
		for (TemplatePersistenceData templatePersistenceData : fTemplateStore.getTemplateData()) {
			String id= templatePersistenceData.getId();
			if (id != null && id.equals(data.getId())) {
				templatePersistenceData.setTemplate(data.getTemplate());
				break;
			}
		}
	}

	private void exportAll() {
		export(fTemplateStore.getTemplateData());
	}

	private void export(List<Object> selected) {
		Set<Object> datas= new HashSet<>();
		for (Object curr : selected) {
			if (curr instanceof TemplatePersistenceData) {
				datas.add(curr);
			} else {
				TemplatePersistenceData[] cat= getTemplateOfCategory(curr == COMMENT_NODE);
				datas.addAll(Arrays.asList(cat));
			}
		}
		export(datas.toArray(new TemplatePersistenceData[datas.size()]));
	}

	private void export(TemplatePersistenceData[] templates) {
		FileDialog dialog= new FileDialog(getShell(), SWT.SAVE | SWT.SHEET);
		String text= templates.length == 1 ? PreferencesMessages.CodeTemplateBlock_export_title_singular : Messages.format(PreferencesMessages.CodeTemplateBlock_export_title_plural, String
				.valueOf(templates.length));
		dialog.setText(text);
		dialog.setFilterExtensions(new String[] {PreferencesMessages.CodeTemplateBlock_export_extension});
		dialog.setFileName(PreferencesMessages.CodeTemplateBlock_export_filename);
		String path= dialog.open();

		if (path == null)
			return;

		File file= new File(path);

		if (file.isHidden()) {
			String title= PreferencesMessages.CodeTemplateBlock_export_error_title;
			String message= Messages.format(PreferencesMessages.CodeTemplateBlock_export_error_hidden, BasicElementLabels.getPathLabel(file));
			MessageDialog.openError(getShell(), title, message);
			return;
		}

		if (file.exists() && !file.canWrite()) {
			String title= PreferencesMessages.CodeTemplateBlock_export_error_title;
			String message= Messages.format(PreferencesMessages.CodeTemplateBlock_export_error_canNotWrite, BasicElementLabels.getPathLabel(file));
			MessageDialog.openError(getShell(), title, message);
			return;
		}

		if (!file.exists() || confirmOverwrite(file)) {
			OutputStream output= null;
			try {
				output= new BufferedOutputStream(new FileOutputStream(file));
				TemplateReaderWriter writer= new TemplateReaderWriter();
				writer.save(templates, output);
				output.close();
			} catch (IOException e) {
				if (output != null) {
					try {
						output.close();
					} catch (IOException e2) {
						// ignore
					}
				}
				openWriteErrorDialog();
			}
		}

	}

	private boolean confirmOverwrite(File file) {
		return MessageDialog.openQuestion(getShell(),
			PreferencesMessages.CodeTemplateBlock_export_exists_title,
			Messages.format(PreferencesMessages.CodeTemplateBlock_export_exists_message, BasicElementLabels.getPathLabel(file)));
	}

	@Override
	public void performDefaults() {
		fTemplateStore.restoreDefaults();

		// refresh
		fCodeTemplateTree.refresh();

		super.performDefaults();
	}

	public boolean performOk(boolean enabled) {
		boolean res= super.performOk();
		if (!res)
			return false;

		if (fProject != null) {
			for (TemplatePersistenceData td : fTemplateStore.getTemplateData()) {
				fTemplateStore.setProjectSpecific(td.getId(), enabled);
			}
		}
		try {
			fTemplateStore.save();
		} catch (IOException e) {
			JavaPlugin.log(e);
			openWriteErrorDialog();
		}
		return true;
	}

	public void performCancel() {
		try {
			fTemplateStore.revertChanges();
		} catch (IOException e) {
			openReadErrorDialog(e);
		}
	}

	private void openReadErrorDialog(Exception e) {
		String title= PreferencesMessages.CodeTemplateBlock_error_read_title;

		String message= e.getLocalizedMessage();
		if (message != null)
			message= Messages.format(PreferencesMessages.CodeTemplateBlock_error_parse_message, message);
		else
			message= PreferencesMessages.CodeTemplateBlock_error_read_message;
		MessageDialog.openError(getShell(), title, message);
	}

	private void openWriteErrorDialog() {
		String title= PreferencesMessages.CodeTemplateBlock_error_write_title;
		String message= PreferencesMessages.CodeTemplateBlock_error_write_message;
		MessageDialog.openError(getShell(), title, message);
	}

	@Override
	protected String[] getFullBuildDialogStrings(boolean workspaceSettings) {
		return null;
	}

	@Override
	protected void validateSettings(Key changedKey, String oldValue, String newValue) {
		// no validation here
	}
}
