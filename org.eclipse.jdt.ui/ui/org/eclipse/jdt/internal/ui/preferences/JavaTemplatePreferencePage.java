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
package org.eclipse.jdt.internal.ui.preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.persistence.TemplatePersistenceData;

import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.texteditor.templates.TemplatePreferencePage;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaSourceViewer;
import org.eclipse.jdt.internal.ui.text.IJavaPartitions;
import org.eclipse.jdt.internal.ui.text.template.preferences.TemplateVariableProcessor;

public class JavaTemplatePreferencePage extends TemplatePreferencePage implements IWorkbenchPreferencePage {

	private TemplateVariableProcessor fTemplateProcessor;
	
	public JavaTemplatePreferencePage() {
		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());
		setTemplateStore(JavaPlugin.getDefault().getTemplateStore());
		setContextTypeRegistry(JavaPlugin.getDefault().getTemplateContextRegistry());
		fTemplateProcessor= new TemplateVariableProcessor();
	}

	/*
	 * @see org.eclipse.jface.preference.IPreferencePage#performOk()
	 */
	public boolean performOk() {
		boolean ok= super.performOk();

		JavaPlugin.getDefault().savePluginPreferences();
		
		return ok;
	}
	
	/*
	 * @see org.eclipse.ui.texteditor.templates.TemplatePreferencePage#getFormatterPreferenceKey()
	 */
	protected String getFormatterPreferenceKey() {
		return PreferenceConstants.TEMPLATES_USE_CODEFORMATTER;
	}
	
	/*
	 * @see org.eclipse.ui.texteditor.templates.TemplatePreferencePage#createTemplateEditDialog(org.eclipse.jface.text.templates.Template, boolean, boolean)
	 */
	protected Dialog createTemplateEditDialog(Template template, boolean edit, boolean isNameModifiable) {
		return new EditTemplateDialog(getShell(), template, edit, isNameModifiable, getContextTypeRegistry());
	}
	
	
	/*
	 * @see org.eclipse.ui.texteditor.templates.TemplatePreferencePage#createViewer(org.eclipse.swt.widgets.Composite)
	 */
	protected SourceViewer createViewer(Composite parent) {
		Label label= new Label(parent, SWT.NONE);
		label.setText(PreferencesMessages.getString("CodeTemplateBlock.preview")); //$NON-NLS-1$
		GridData data= new GridData();
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
		control.setLayoutData(data);
		
		return viewer;
	}
	
	
	/*
	 * @see org.eclipse.ui.texteditor.templates.TemplatePreferencePage#updateViewerInput()
	 */
	protected void updateViewerInput() {
		IStructuredSelection selection= (IStructuredSelection) getTableViewer().getSelection();
		SourceViewer viewer= getViewer();
		
		if (selection.size() == 1 && selection.getFirstElement() instanceof TemplatePersistenceData) {
			TemplatePersistenceData data= (TemplatePersistenceData) selection.getFirstElement();
			Template template= data.getTemplate();
			TemplateContextType type= JavaPlugin.getDefault().getTemplateContextRegistry().getContextType(template.getContextTypeId());
			fTemplateProcessor.setContextType(type);
			viewer.getDocument().set(template.getPattern());
		} else {
			viewer.getDocument().set(""); //$NON-NLS-1$
		}		
	}
}