/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;

import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;
import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusDialog;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.text.template.Template;
import org.eclipse.jdt.internal.ui.text.template.TemplateContext;
import org.eclipse.jdt.internal.ui.text.template.TemplateInterpolator;
import org.eclipse.jdt.internal.ui.text.template.TemplateMessages;
import org.eclipse.jdt.internal.ui.text.template.VariableEvaluator;

/**
 * Dialog to edit a template.
 */
public class EditTemplateDialog extends StatusDialog {

	// disable content assist
	private static class SimpleJavaSourceViewerConfiguration extends JavaSourceViewerConfiguration {
		SimpleJavaSourceViewerConfiguration(JavaTextTools tools, ITextEditor editor) {
			super(tools, editor);			
		}
		
		/*
		 * @see SourceViewerConfiguration#getContentAssistant(ISourceViewer)
		 */
		public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
			return null;
		}	
	}

	private static class TemplateVerifier implements VariableEvaluator {		
		private String fErrorMessage;
		private boolean fHasAdjacentVariables;
		private boolean fEndsWithVariable;
		
		public void reset() {
			fErrorMessage= null;
			fHasAdjacentVariables= false;
			fEndsWithVariable= false;
		}

		public void acceptError(String message) {
			if (fErrorMessage == null)
				fErrorMessage= message;
		}

		public void acceptText(String text) {
			if (text.length() > 0)
				fEndsWithVariable= false;
		}
		
		public void acceptVariable(String variable) {
			if (fEndsWithVariable)
				fHasAdjacentVariables= true;
			
			fEndsWithVariable= true;
		}

		public boolean hasErrors() {
			return fHasAdjacentVariables || (fErrorMessage != null);	
		}
		
		public String getErrorMessage() {
			if (fHasAdjacentVariables)
				return TemplateMessages.getString("EditTemplateDialog.error.adjacent.variables"); //$NON-NLS-1$

			return fErrorMessage;
		}
	}

	private Template fTemplate;

	private Text fNameText;
	private Text fDescriptionText;
	private Combo fContextCombo;
	private SourceViewer fPatternEditor;
	
//	private Button fAddVariableButton;

	private TemplateInterpolator fInterpolator= new TemplateInterpolator();
	private TemplateVerifier fVerifier= new TemplateVerifier();
	
	private boolean fSuppressError= true; // #4354
		
	public EditTemplateDialog(Shell parent, Template template, boolean edit) {
		super(parent);
		
		if (edit)
			setTitle(TemplateMessages.getString("EditTemplateDialog.title.edit"));
		else
			setTitle(TemplateMessages.getString("EditTemplateDialog.title.new"));

		fTemplate= template;
	}
	
	protected Control createDialogArea(Composite ancestor) {
		Composite parent= new Composite(ancestor, SWT.NONE);
		parent.setLayout(new GridLayout());		
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		parent.setLayout(layout);
		
		createLabel(parent, TemplateMessages.getString("EditTemplateDialog.name")); //$NON-NLS-1$	
		
		Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		layout= new GridLayout();		
		layout.numColumns= 3;
		layout.marginWidth= 0;
		layout.marginHeight= 0;
		composite.setLayout(layout);

		fNameText= createText(composite);
		fNameText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (fSuppressError && (fNameText.getText().trim().length() != 0))
					fSuppressError= false;

				updateButtons();
			}
		});

		createLabel(composite, TemplateMessages.getString("EditTemplateDialog.context")); //$NON-NLS-1$		
		fContextCombo= new Combo(composite, SWT.READ_ONLY);
		fContextCombo.setItems(new String[] {TemplateContext.JAVA, TemplateContext.JAVADOC});
		
		createLabel(parent, TemplateMessages.getString("EditTemplateDialog.description")); //$NON-NLS-1$		
		fDescriptionText= createText(parent);

		Label patternLabel= createLabel(parent, TemplateMessages.getString("EditTemplateDialog.pattern")); //$NON-NLS-1$
		patternLabel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
		fPatternEditor= createEditor(parent);

		// initialize fields
		fNameText.setText(fTemplate.getName());
		fDescriptionText.setText(fTemplate.getDescription());
		fContextCombo.select(getIndex(fTemplate.getContext()));
		fPatternEditor.getDocument().set(fTemplate.getPattern());

		return composite;
	}

	private static Label createLabel(Composite parent, String name) {
		Label label= new Label(parent, SWT.NULL);
		label.setText(name);
		label.setLayoutData(new GridData());

		return label;
	}

	private static Text createText(Composite parent) {
		Text text= new Text(parent, SWT.BORDER);
		text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));		
		
		return text;
	}

	private SourceViewer createEditor(Composite parent) {
		SourceViewer viewer= new SourceViewer(parent, null, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		JavaTextTools tools= JavaPlugin.getDefault().getJavaTextTools();
		viewer.configure(new SimpleJavaSourceViewerConfiguration(tools, null));
		viewer.setEditable(true);
		
		IDocument document= new Document();
		document.addDocumentListener(new IDocumentListener() {
			public void documentAboutToBeChanged(DocumentEvent event) {}

			public void documentChanged(DocumentEvent event) {
				fInterpolator.interpolate(event.getDocument().get(), fVerifier);

				updateButtons();
			}
		});
		viewer.setDocument(document);
	
		Font font= JFaceResources.getFontRegistry().get(JFaceResources.TEXT_FONT);
		viewer.getTextWidget().setFont(font);
		
		Control control= viewer.getControl();
		GridData data= new GridData(GridData.FILL_BOTH);
		data.widthHint= convertWidthInCharsToPixels(60);
		data.heightHint= convertHeightInCharsToPixels(5);
		control.setLayoutData(data);
		
		return viewer;
	}

	private static int getIndex(String context) {
		if (context.equals(TemplateContext.JAVA))
			return 0;
		else if (context.equals(TemplateContext.JAVADOC))
			return 1;
		else
			return -1;
	}
	
	protected void okPressed() {
		// read back fields
		fTemplate.setName(fNameText.getText());
		fTemplate.setDescription(fDescriptionText.getText());
		fTemplate.setContext(fContextCombo.getText());
		fTemplate.setPattern(fPatternEditor.getTextWidget().getText());
		
		super.okPressed();
	}
	
	private void updateButtons() {		
		boolean valid= fNameText.getText().trim().length() != 0;

		StatusInfo status= new StatusInfo();
		
		if (!valid) {
			if (fSuppressError)
				status.setError(""); //$NON-NLS-1$							
			else
				status.setError(TemplateMessages.getString("EditTemplateDialog.error.noname")); //$NON-NLS-1$
 		} else if (fVerifier.hasErrors()) {
 			status.setError(fVerifier.getErrorMessage());	
		}

		updateStatus(status);
	}

}