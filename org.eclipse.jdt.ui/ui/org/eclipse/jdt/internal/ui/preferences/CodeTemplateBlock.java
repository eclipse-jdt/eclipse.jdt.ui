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
package org.eclipse.jdt.internal.ui.preferences;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.persistence.TemplatePersistenceData;
import org.eclipse.jface.text.templates.persistence.TemplateReaderWriter;
import org.eclipse.jface.text.templates.persistence.TemplateStore;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.jdt.internal.corext.template.java.CodeTemplateContextType;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaSourceViewer;
import org.eclipse.jdt.internal.ui.text.IJavaPartitions;
import org.eclipse.jdt.internal.ui.text.template.preferences.TemplateVariableProcessor;
import org.eclipse.jdt.internal.ui.util.PixelConverter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ITreeListAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.TreeListDialogField;

/**
  */
public class CodeTemplateBlock {
	
	private class CodeTemplateAdapter implements ITreeListAdapter, IDialogFieldListener {

		private final Object[] NO_CHILDREN= new Object[0];

		public void customButtonPressed(TreeListDialogField field, int index) {
			doButtonPressed(index, field.getSelectedElements());
		}
			
		public void selectionChanged(TreeListDialogField field) {
			List selected= field.getSelectedElements();
			field.enableButton(IDX_EDIT, canEdit(selected));
			field.enableButton(IDX_EXPORT, !selected.isEmpty());
			
			updateSourceViewerInput(selected);
		}

		public void doubleClicked(TreeListDialogField field) {
			List selected= field.getSelectedElements();
			if (canEdit(selected)) {
				doButtonPressed(IDX_EDIT, selected);
			}
		}

		public Object[] getChildren(TreeListDialogField field, Object element) {
			if (element == COMMENT_NODE || element == CODE_NODE) {
				return getTemplateOfCategory(element == COMMENT_NODE);
			}
			return NO_CHILDREN;
		}

		public Object getParent(TreeListDialogField field, Object element) {
			if (element instanceof TemplatePersistenceData) {
				TemplatePersistenceData data= (TemplatePersistenceData) element;
				if (data.getTemplate().getName().endsWith(CodeTemplateContextType.COMMENT_SUFFIX)) {
					return COMMENT_NODE;
				}
				return CODE_NODE;
			}
			return null;
		}

		public boolean hasChildren(TreeListDialogField field, Object element) {
			return (element == COMMENT_NODE || element == CODE_NODE);
		}

		public void dialogFieldChanged(DialogField field) {
		}

		public void keyPressed(TreeListDialogField field, KeyEvent event) {
		}
	
	}
	
	private static class CodeTemplateLabelProvider extends LabelProvider {
	
		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
		 */
		public Image getImage(Object element) {
			return null;

		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
		 */
		public String getText(Object element) {
			if (element == COMMENT_NODE || element == CODE_NODE) {
				return (String) element;
			}
			TemplatePersistenceData data= (TemplatePersistenceData) element;
			Template template= data.getTemplate();
			String name= template.getName();
			if (CodeTemplateContextType.CATCHBLOCK.equals(name)) {
				return PreferencesMessages.getString("CodeTemplateBlock.catchblock.label"); //$NON-NLS-1$
			} else if (CodeTemplateContextType.METHODSTUB.equals(name)) {
				return PreferencesMessages.getString("CodeTemplateBlock.methodstub.label"); //$NON-NLS-1$
			} else if (CodeTemplateContextType.CONSTRUCTORSTUB.equals(name)) {
				return PreferencesMessages.getString("CodeTemplateBlock.constructorstub.label"); //$NON-NLS-1$
			} else if (CodeTemplateContextType.GETTERSTUB.equals(name)) {
				return PreferencesMessages.getString("CodeTemplateBlock.getterstub.label"); //$NON-NLS-1$
			} else if (CodeTemplateContextType.SETTERSTUB.equals(name)) {
				return PreferencesMessages.getString("CodeTemplateBlock.setterstub.label"); //$NON-NLS-1$
			} else if (CodeTemplateContextType.NEWTYPE.equals(name)) {
				return PreferencesMessages.getString("CodeTemplateBlock.newtype.label"); //$NON-NLS-1$
			} else if (CodeTemplateContextType.TYPECOMMENT.equals(name)) {
				return PreferencesMessages.getString("CodeTemplateBlock.typecomment.label"); //$NON-NLS-1$
			} else if (CodeTemplateContextType.FIELDCOMMENT.equals(name)) {
				return PreferencesMessages.getString("CodeTemplateBlock.fieldcomment.label"); //$NON-NLS-1$
			} else if (CodeTemplateContextType.METHODCOMMENT.equals(name)) {
				return PreferencesMessages.getString("CodeTemplateBlock.methodcomment.label"); //$NON-NLS-1$
			} else if (CodeTemplateContextType.OVERRIDECOMMENT.equals(name)) {
				return PreferencesMessages.getString("CodeTemplateBlock.overridecomment.label"); //$NON-NLS-1$
			} else if (CodeTemplateContextType.CONSTRUCTORCOMMENT.equals(name)) {
				return PreferencesMessages.getString("CodeTemplateBlock.constructorcomment.label"); //$NON-NLS-1$
			} else if (CodeTemplateContextType.GETTERCOMMENT.equals(name)) {
				return PreferencesMessages.getString("CodeTemplateBlock.gettercomment.label"); //$NON-NLS-1$
			} else if (CodeTemplateContextType.SETTERCOMMENT.equals(name)) {
				return PreferencesMessages.getString("CodeTemplateBlock.settercomment.label"); //$NON-NLS-1$
			}
			return template.getDescription();
		}

	}	
	
	private final static int IDX_EDIT= 0;
	private final static int IDX_IMPORT= 2;
	private final static int IDX_EXPORT= 3;
	private final static int IDX_EXPORTALL= 4;
	
	protected final static Object COMMENT_NODE= PreferencesMessages.getString("CodeTemplateBlock.templates.comment.node"); //$NON-NLS-1$
	protected final static Object CODE_NODE= PreferencesMessages.getString("CodeTemplateBlock.templates.code.node"); //$NON-NLS-1$
	
	private static final String PREF_JAVADOC_STUBS= PreferenceConstants.CODEGEN_ADD_COMMENTS;
	
	private TreeListDialogField fCodeTemplateTree;
	
	private SelectionButtonDialogField fCreateJavaDocComments;
	
	protected TemplateStore fTemplates;
	
	private PixelConverter fPixelConverter;
	private SourceViewer fPatternViewer;
	private Control fSWTWidget;
	private TemplateVariableProcessor fTemplateProcessor;

	public CodeTemplateBlock() {
		
		fTemplates= JavaPlugin.getDefault().getCodeTemplateStore();
		fTemplateProcessor= new TemplateVariableProcessor();
		
		CodeTemplateAdapter adapter= new CodeTemplateAdapter();

		String[] buttonLabels= new String[] { 
			/* IDX_EDIT*/ PreferencesMessages.getString("CodeTemplateBlock.templates.edit.button"),	//$NON-NLS-1$
			/* */ null,  
			/* IDX_IMPORT */ PreferencesMessages.getString("CodeTemplateBlock.templates.import.button"), //$NON-NLS-1$
			/* IDX_EXPORT */ PreferencesMessages.getString("CodeTemplateBlock.templates.export.button"), //$NON-NLS-1$
			/* IDX_EXPORTALL */ PreferencesMessages.getString("CodeTemplateBlock.templates.exportall.button") //$NON-NLS-1$

		};		
		fCodeTemplateTree= new TreeListDialogField(adapter, buttonLabels, new CodeTemplateLabelProvider());
		fCodeTemplateTree.setDialogFieldListener(adapter);
		fCodeTemplateTree.setLabelText(PreferencesMessages.getString("CodeTemplateBlock.templates.label")); //$NON-NLS-1$

		fCodeTemplateTree.enableButton(IDX_EXPORT, false);
		fCodeTemplateTree.enableButton(IDX_EDIT, false);
		
		fCodeTemplateTree.addElement(COMMENT_NODE);
		fCodeTemplateTree.addElement(CODE_NODE);
		
		fCreateJavaDocComments= new SelectionButtonDialogField(SWT.CHECK | SWT.WRAP);
		fCreateJavaDocComments.setLabelText(PreferencesMessages.getString("CodeTemplateBlock.createcomment.label")); //$NON-NLS-1$
		fCreateJavaDocComments.setSelection(PreferenceConstants.getPreferenceStore().getBoolean(PREF_JAVADOC_STUBS));
		
		fCodeTemplateTree.selectFirstElement();	
	}
	
	protected Control createContents(Composite parent) {
		fPixelConverter=  new PixelConverter(parent);
		fSWTWidget= parent;
		
		Composite composite=  new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 2;
		composite.setLayout(layout);
		
		fCodeTemplateTree.doFillIntoGrid(composite, 3);
		LayoutUtil.setHorizontalSpan(fCodeTemplateTree.getLabelControl(null), 2);
		LayoutUtil.setHorizontalGrabbing(fCodeTemplateTree.getTreeControl(null));
		
		fPatternViewer= createViewer(composite, 2);
		
		fCreateJavaDocComments.doFillIntoGrid(composite, 2);
		
		DialogField label= new DialogField();
		label.setLabelText(PreferencesMessages.getString("CodeTemplateBlock.createcomment.description")); //$NON-NLS-1$
		label.doFillIntoGrid(composite, 2);
		
		return composite;
	
	}
	
	private Shell getShell() {
		if (fSWTWidget != null) {
			return fSWTWidget.getShell();
		}
		return JavaPlugin.getActiveWorkbenchShell();			
	}	
	
	private SourceViewer createViewer(Composite parent, int nColumns) {
		Label label= new Label(parent, SWT.NONE);
		label.setText(PreferencesMessages.getString("CodeTemplateBlock.preview")); //$NON-NLS-1$
		GridData data= new GridData();
		data.horizontalSpan= nColumns;
		label.setLayoutData(data);
		
		IDocument document= new Document();
		JavaTextTools tools= JavaPlugin.getDefault().getJavaTextTools();
		tools.setupJavaDocumentPartitioner(document, IJavaPartitions.JAVA_PARTITIONING);
		IPreferenceStore store= JavaPlugin.getDefault().getCombinedPreferenceStore();
		SourceViewer viewer= new JavaSourceViewer(parent, null, null, false, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL, store);
		TemplateEditorSourceViewerConfiguration configuration= new TemplateEditorSourceViewerConfiguration(tools.getColorManager(), store, null, fTemplateProcessor);
		viewer.configure(configuration);
		viewer.setEditable(false);
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
	
	protected TemplatePersistenceData[] getTemplateOfCategory(boolean isComment) {
		ArrayList res=  new ArrayList();
		TemplatePersistenceData[] templates= fTemplates.getTemplateData(false);
		for (int i= 0; i < templates.length; i++) {
			TemplatePersistenceData curr= templates[i];
			if (isComment == curr.getTemplate().getName().endsWith(CodeTemplateContextType.COMMENT_SUFFIX)) {
				res.add(curr);
			}
		}
		return (TemplatePersistenceData[]) res.toArray(new TemplatePersistenceData[res.size()]);
	}
	
	protected static boolean canEdit(List selected) {
		return selected.size() == 1 && (selected.get(0) instanceof TemplatePersistenceData);
	}	
	
	protected void updateSourceViewerInput(List selection) {
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
		
	protected void doButtonPressed(int buttonIndex, List selected) {
		if (buttonIndex == IDX_EDIT) {
			edit((TemplatePersistenceData) selected.get(0));
		} else if (buttonIndex == IDX_EXPORT) {
			export(selected);
		} else if (buttonIndex == IDX_EXPORTALL) {
			exportAll();
		} else if (buttonIndex == IDX_IMPORT) {
			import_();
		}
	}
	
	private void edit(TemplatePersistenceData data) {
		Template newTemplate= new Template(data.getTemplate());
		EditTemplateDialog dialog= new EditTemplateDialog(getShell(), newTemplate, true, false, JavaPlugin.getDefault().getCodeTemplateContextRegistry());
		if (dialog.open() == Window.OK) {
			// changed
			data.setTemplate(newTemplate);
			fCodeTemplateTree.refresh(data);
			fCodeTemplateTree.selectElements(new StructuredSelection(data));
		}
	}
		
	private void import_() {
		FileDialog dialog= new FileDialog(getShell());
		dialog.setText(PreferencesMessages.getString("CodeTemplateBlock.import.title")); //$NON-NLS-1$
		dialog.setFilterExtensions(new String[] {PreferencesMessages.getString("CodeTemplateBlock.import.extension")}); //$NON-NLS-1$
		String path= dialog.open();
		
		if (path == null)
			return;
		
		try {
			TemplateReaderWriter reader= new TemplateReaderWriter();
			File file= new File(path);
			if (file.exists()) {
				InputStream input= new BufferedInputStream(new FileInputStream(file));
				try {
					TemplatePersistenceData[] datas= reader.read(input, null);
					for (int i= 0; i < datas.length; i++) {
						updateTemplate(datas[i]);
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

		} catch (FileNotFoundException e) {
			openReadErrorDialog(e);
		} catch (IOException e) {
			openReadErrorDialog(e);
		}

	}
	
	private void updateTemplate(TemplatePersistenceData data) {
		TemplatePersistenceData[] datas= fTemplates.getTemplateData(true);
		for (int i= 0; i < datas.length; i++) {
			String id= datas[i].getId();
			if (id != null && id.equals(data.getId())) {
				datas[i].setTemplate(data.getTemplate());
				break;
			}
		}
	}
	
	private void exportAll() {
		export(fTemplates.getTemplateData(false));	
	}
	
	private void export(List selected) {
		List datas= new ArrayList();
		for (int i= 0; i < selected.size(); i++) {
			Object curr= selected.get(i);
			if (curr instanceof TemplatePersistenceData) {
				datas.add(curr);
			} else {
				TemplatePersistenceData[] cat= getTemplateOfCategory(curr == COMMENT_NODE);
				datas.addAll(Arrays.asList(cat));
			}
		}
		export((TemplatePersistenceData[]) datas.toArray(new TemplatePersistenceData[datas.size()]));
	}
	
	private void export(TemplatePersistenceData[] templates) {
		FileDialog dialog= new FileDialog(getShell(), SWT.SAVE);
		dialog.setText(PreferencesMessages.getFormattedString("CodeTemplateBlock.export.title", String.valueOf(templates.length))); //$NON-NLS-1$
		dialog.setFilterExtensions(new String[] {PreferencesMessages.getString("CodeTemplateBlock.export.extension")}); //$NON-NLS-1$
		dialog.setFileName(PreferencesMessages.getString("CodeTemplateBlock.export.filename")); //$NON-NLS-1$
		String path= dialog.open();

		if (path == null)
			return;
		
		File file= new File(path);		

		if (file.isHidden()) {
			String title= PreferencesMessages.getString("CodeTemplateBlock.export.error.title"); //$NON-NLS-1$ 
			String message= PreferencesMessages.getFormattedString("CodeTemplateBlock.export.error.hidden", file.getAbsolutePath()); //$NON-NLS-1$
			MessageDialog.openError(getShell(), title, message);
			return;
		}
		
		if (file.exists() && !file.canWrite()) {
			String title= PreferencesMessages.getString("CodeTemplateBlock.export.error.title"); //$NON-NLS-1$
			String message= PreferencesMessages.getFormattedString("CodeTemplateBlock.export.error.canNotWrite", file.getAbsolutePath()); //$NON-NLS-1$
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
				openWriteErrorDialog(e);
			}
		}
		
	}

	private boolean confirmOverwrite(File file) {
		return MessageDialog.openQuestion(getShell(),
			PreferencesMessages.getString("CodeTemplateBlock.export.exists.title"), //$NON-NLS-1$
			PreferencesMessages.getFormattedString("CodeTemplateBlock.export.exists.message", file.getAbsolutePath())); //$NON-NLS-1$
	}

	
	
	public void performDefaults() {
		IPreferenceStore prefs= JavaPlugin.getDefault().getPreferenceStore();
		fCreateJavaDocComments.setSelection(prefs.getDefaultBoolean(PREF_JAVADOC_STUBS));

		fTemplates.restoreDefaults();
		
		// refresh
		fCodeTemplateTree.refresh();
		updateSourceViewerInput(fCodeTemplateTree.getSelectedElements());
	}
	
	public boolean performOk(boolean enabled) {
		IPreferenceStore prefs= PreferenceConstants.getPreferenceStore();
		prefs.setValue(PREF_JAVADOC_STUBS, fCreateJavaDocComments.isSelected());
		JavaPlugin.getDefault().savePluginPreferences();
		
		try {
			fTemplates.save();
		} catch (IOException e) {
			JavaPlugin.log(e);
			openWriteErrorDialog(e);
		}		
		return true;
	}
	
	public void performCancel() {
		try {
			fTemplates.load();			
		} catch (IOException e) {
			openReadErrorDialog(e);
		}
	}
	
	private void openReadErrorDialog(Exception e) {
		String title= PreferencesMessages.getString("CodeTemplateBlock.error.read.title"); //$NON-NLS-1$
		
		String message= e.getLocalizedMessage();
		if (message != null)
			message= PreferencesMessages.getFormattedString("CodeTemplateBlock.error.parse.message", message); //$NON-NLS-1$
		else
			message= PreferencesMessages.getString("CodeTemplateBlock.error.read.message"); //$NON-NLS-1$
		MessageDialog.openError(getShell(), title, message);
	}
	
	private void openWriteErrorDialog(Exception e) {
		String title= PreferencesMessages.getString("CodeTemplateBlock.error.write.title"); //$NON-NLS-1$
		String message= PreferencesMessages.getString("CodeTemplateBlock.error.write.message"); //$NON-NLS-1$
		MessageDialog.openError(getShell(), title, message);
	}	

}
