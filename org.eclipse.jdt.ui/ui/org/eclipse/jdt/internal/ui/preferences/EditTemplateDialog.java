/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.preferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import org.eclipse.debug.internal.ui.actions.TextViewerAction;

import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextListener;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.TextEvent;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.IUpdate;

import org.eclipse.jdt.ui.IContextMenuConstants;
import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;
import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusDialog;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.text.template.Template;
import org.eclipse.jdt.internal.ui.text.template.TemplateContext;
import org.eclipse.jdt.internal.ui.text.template.TemplateInterpolator;
import org.eclipse.jdt.internal.ui.text.template.TemplateMessages;
import org.eclipse.jdt.internal.ui.text.template.TemplateVariableProcessor;
import org.eclipse.jdt.internal.ui.text.template.VariableEvaluator;
import org.eclipse.jdt.internal.ui.util.SWTUtil;

/**
 * Dialog to edit a template.
 */
public class EditTemplateDialog extends StatusDialog {

	private static class SimpleJavaSourceViewerConfiguration extends JavaSourceViewerConfiguration {

		SimpleJavaSourceViewerConfiguration(JavaTextTools tools, ITextEditor editor) {
			super(tools, editor);
		}
		
		/*
		 * @see SourceViewerConfiguration#getContentAssistant(ISourceViewer)
		 */
		public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {

			ContentAssistant assistant= new ContentAssistant();
			assistant.setContentAssistProcessor(new TemplateVariableProcessor(), IDocument.DEFAULT_CONTENT_TYPE);

			assistant.enableAutoActivation(true);
			assistant.setAutoActivationDelay(500);
			assistant.setProposalPopupOrientation(assistant.PROPOSAL_OVERLAY);
			assistant.setContextInformationPopupOrientation(assistant.CONTEXT_INFO_ABOVE);
			assistant.setInformationControlCreator(getInformationControlCreator(sourceViewer));
			
			Color background= getColorManager().getColor(new RGB(254, 241, 233));
			assistant.setContextInformationPopupBackground(background);
			assistant.setContextSelectorBackground(background);
			assistant.setProposalSelectorBackground(background);
			
			return assistant;
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
	
	private Button fInsertVariableButton;

	private TemplateInterpolator fInterpolator= new TemplateInterpolator();
	private TemplateVerifier fVerifier= new TemplateVerifier();
	
	private boolean fSuppressError= true; // #4354
	
	private Map fGlobalActions= new HashMap(10);
	private List fSelectionActions = new ArrayList(3);
		
	public EditTemplateDialog(Shell parent, Template template, boolean edit) {
		super(parent);
		
		int shellStyle= getShellStyle();
		setShellStyle(shellStyle | SWT.MAX | SWT.RESIZE);
		
		if (edit)
			setTitle(TemplateMessages.getString("EditTemplateDialog.title.edit")); //$NON-NLS-1$
		else
			setTitle(TemplateMessages.getString("EditTemplateDialog.title.new")); //$NON-NLS-1$

		fTemplate= template;
	}
	
	/*
	 * @see Dialog#createDialogArea(Composite)
	 */
	protected Control createDialogArea(Composite ancestor) {
		Composite parent= new Composite(ancestor, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		parent.setLayout(layout);
		parent.setLayoutData(new GridData(GridData.FILL_BOTH));
		
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

		composite= new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(GridData.FILL_VERTICAL));
		layout= new GridLayout();		
		layout.marginWidth= 0;
		layout.marginHeight= 0;
		composite.setLayout(layout);

		createLabel(composite, TemplateMessages.getString("EditTemplateDialog.pattern")); //$NON-NLS-1$
		fPatternEditor= createEditor(parent);
		
		Label filler= new Label(composite, SWT.NONE);		
		filler.setLayoutData(new GridData(GridData.FILL_VERTICAL));
		
		fInsertVariableButton= new Button(composite, SWT.NONE);
		fInsertVariableButton.setLayoutData(getButtonGridData(fInsertVariableButton));
		fInsertVariableButton.setText(TemplateMessages.getString("EditTemplateDialog.insert.variable")); //$NON-NLS-1$
		fInsertVariableButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				fPatternEditor.doOperation(ISourceViewer.CONTENTASSIST_PROPOSALS);			
			}

			public void widgetDefaultSelected(SelectionEvent e) {}
		});

		fNameText.setText(fTemplate.getName());
		fDescriptionText.setText(fTemplate.getDescription());
		fContextCombo.select(getIndex(fTemplate.getContext()));

		initializeActions();

		return composite;
	}

	private static GridData getButtonGridData(Button button) {
		GridData data= new GridData(GridData.FILL_HORIZONTAL);
		data.heightHint= SWTUtil.getButtonHeigthHint(button);
	
		return data;
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
		viewer.setDocument(new Document(fTemplate.getPattern()));
		
		Font font= JFaceResources.getFontRegistry().get(JFaceResources.TEXT_FONT);
		viewer.getTextWidget().setFont(font);
		
		Control control= viewer.getControl();
		GridData data= new GridData(GridData.FILL_BOTH);
		data.widthHint= convertWidthInCharsToPixels(60);
		data.heightHint= convertHeightInCharsToPixels(5);
		control.setLayoutData(data);
		
		viewer.addTextListener(new ITextListener() {
			public void textChanged(TextEvent event) {
				fInterpolator.interpolate(event.getDocumentEvent().getDocument().get(), fVerifier);
				
				updateUndoAction();
				updateButtons();
			}
		});

		viewer.addSelectionChangedListener(new ISelectionChangedListener() {			
			public void selectionChanged(SelectionChangedEvent event) {
				updateSelectionDependentActions();
			}
		});
	
		viewer.getTextWidget().addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
				handleKeyPressed(e);
			}

			public void keyReleased(KeyEvent e) {}
		});
		
		return viewer;
	}

	private void handleKeyPressed(KeyEvent event) {
		if (event.stateMask != SWT.CTRL)
			return;
			
		switch (event.character) {
			case ' ':
				fPatternEditor.doOperation(ISourceViewer.CONTENTASSIST_PROPOSALS);
				break;

			// XXX CTRL-Z
			case (int) 'z' - (int) 'a' + 1:
				fPatternEditor.doOperation(ITextOperationTarget.UNDO);
				break;				
		}
	}

	private void initializeActions() {
		TextViewerAction action= new TextViewerAction(fPatternEditor, fPatternEditor.UNDO);
		action.setText(TemplateMessages.getString("EditTemplateDialog.undo")); //$NON-NLS-1$
		fGlobalActions.put(ITextEditorActionConstants.UNDO, action);

		action= new TextViewerAction(fPatternEditor, fPatternEditor.CUT);
		action.setText(TemplateMessages.getString("EditTemplateDialog.cut")); //$NON-NLS-1$
		fGlobalActions.put(ITextEditorActionConstants.CUT, action);

		action= new TextViewerAction(fPatternEditor, fPatternEditor.COPY);
		action.setText(TemplateMessages.getString("EditTemplateDialog.copy")); //$NON-NLS-1$
		fGlobalActions.put(ITextEditorActionConstants.COPY, action);

		action= new TextViewerAction(fPatternEditor, fPatternEditor.PASTE);
		action.setText(TemplateMessages.getString("EditTemplateDialog.paste")); //$NON-NLS-1$
		fGlobalActions.put(ITextEditorActionConstants.PASTE, action);

		action= new TextViewerAction(fPatternEditor, fPatternEditor.SELECT_ALL);
		action.setText(TemplateMessages.getString("EditTemplateDialog.select.all")); //$NON-NLS-1$
		fGlobalActions.put(ITextEditorActionConstants.SELECT_ALL, action);

		action= new TextViewerAction(fPatternEditor, fPatternEditor.CONTENTASSIST_PROPOSALS);
		action.setText(TemplateMessages.getString("EditTemplateDialog.content.assist")); //$NON-NLS-1$
		fGlobalActions.put("ContentAssistProposal", action); //$NON-NLS-1$

		fSelectionActions.add(ITextEditorActionConstants.CUT);
		fSelectionActions.add(ITextEditorActionConstants.COPY);
		fSelectionActions.add(ITextEditorActionConstants.PASTE);
		
		// create context menu
		MenuManager manager= new MenuManager(null, null);
		manager.setRemoveAllWhenShown(true);
		manager.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager mgr) {
				fillContextMenu(mgr);
			}
		});

		StyledText text= fPatternEditor.getTextWidget();		
		Menu menu= manager.createContextMenu(text);
		text.setMenu(menu);
	}

	private void fillContextMenu(IMenuManager menu) {
		menu.add(new GroupMarker(ITextEditorActionConstants.GROUP_UNDO));
		menu.appendToGroup(ITextEditorActionConstants.GROUP_UNDO, (IAction) fGlobalActions.get(ITextEditorActionConstants.UNDO));
		
		menu.add(new Separator(ITextEditorActionConstants.GROUP_EDIT));		
		menu.appendToGroup(ITextEditorActionConstants.GROUP_EDIT, (IAction) fGlobalActions.get(ITextEditorActionConstants.CUT));
		menu.appendToGroup(ITextEditorActionConstants.GROUP_EDIT, (IAction) fGlobalActions.get(ITextEditorActionConstants.COPY));
		menu.appendToGroup(ITextEditorActionConstants.GROUP_EDIT, (IAction) fGlobalActions.get(ITextEditorActionConstants.PASTE));
		menu.appendToGroup(ITextEditorActionConstants.GROUP_EDIT, (IAction) fGlobalActions.get(ITextEditorActionConstants.SELECT_ALL));

		menu.add(new Separator(IContextMenuConstants.GROUP_GENERATE));
		menu.appendToGroup(IContextMenuConstants.GROUP_GENERATE, (IAction) fGlobalActions.get("ContentAssistProposal")); //$NON-NLS-1$
	}

	protected void updateSelectionDependentActions() {
		Iterator iterator= fSelectionActions.iterator();
		while (iterator.hasNext())
			updateAction((String)iterator.next());		
	}

	protected void updateUndoAction() {
		IAction action= (IAction) fGlobalActions.get(ITextEditorActionConstants.UNDO);
		if (action instanceof IUpdate)
			((IUpdate) action).update();
	}

	protected void updateAction(String actionId) {
		IAction action= (IAction) fGlobalActions.get(actionId);
		if (action instanceof IUpdate)
			((IUpdate) action).update();
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