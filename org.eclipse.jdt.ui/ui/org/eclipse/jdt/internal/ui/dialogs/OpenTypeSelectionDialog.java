/*******************************************************************************
 * Copyright (c) 2000, 2026 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.dialogs;


import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.commands.ActionHandler;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.LineNumberRulerColumn;
import org.eclipse.jface.text.source.SourceViewer;

import org.eclipse.ui.ActiveShellExpression;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.TypeNameMatch;

import org.eclipse.jdt.internal.corext.util.TypeInfoFilter;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.dialogs.TypeSelectionExtension;
import org.eclipse.jdt.ui.text.IJavaPartitions;
import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;
import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.javaeditor.JavaSourceViewer;
import org.eclipse.jdt.internal.ui.preferences.PreferencesMessages;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;

/**
 * A type selection dialog used for opening types.
 */
public class OpenTypeSelectionDialog extends FilteredTypesSelectionDialog {

	private static final String DIALOG_SETTINGS= "org.eclipse.jdt.internal.ui.dialogs.OpenTypeSelectionDialog2"; //$NON-NLS-1$

	private SelectionButtonDialogField fOpenTypesInferWildcards;

	private Composite fPreviewContainer;

	private SourceViewer fPreviewViewer;

	public OpenTypeSelectionDialog(Shell parent, boolean multi, IRunnableContext context, IJavaSearchScope scope, int elementKinds) {
		this(parent, multi, context, scope, elementKinds, null);
	}

	public OpenTypeSelectionDialog(Shell parent, boolean multi, IRunnableContext context, IJavaSearchScope scope, int elementKinds, TypeSelectionExtension extension) {
		super(parent, multi, context, scope, elementKinds, extension);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(newShell, IJavaHelpContextIds.OPEN_TYPE_DIALOG);
	}

	@Override
	protected IDialogSettings getDialogSettings() {
		IDialogSettings settings= JavaPlugin.getDefault().getDialogSettings().getSection(DIALOG_SETTINGS);

		if (settings == null) {
			settings= JavaPlugin.getDefault().getDialogSettings().addNewSection(DIALOG_SETTINGS);
		}

		return settings;
	}

	private void doDialogFieldChanged(DialogField field) {
		IPreferenceStore preferenceStore= JavaPlugin.getDefault().getPreferenceStore();

		if (field == fOpenTypesInferWildcards)
			preferenceStore.setValue(PreferenceConstants.OPEN_TYPES_INFER_WILDCARDS, fOpenTypesInferWildcards.isSelected());
		applyFilter();
	}

	@Override
	protected Control createButtonBar(Composite parent) {
		IPreferenceStore preferenceStore= JavaPlugin.getDefault().getPreferenceStore();
		IDialogFieldListener listener= this::doDialogFieldChanged;

		Composite newComposite= new Composite(parent, NONE);
		GridLayout layout= new GridLayout();
		layout.numColumns= 1;
		layout.marginLeft= 5;
		newComposite.setLayout(layout);
		GridData gd= new GridData();
		gd.horizontalIndent= 5;
		newComposite.setLayoutData(gd);
		fOpenTypesInferWildcards= new SelectionButtonDialogField(SWT.CHECK);
		fOpenTypesInferWildcards.setDialogFieldListener(listener);
		fOpenTypesInferWildcards.setLabelText(PreferencesMessages.OpenTypesDialog_default_wildcard_between_camel_case_parts_label);
		fOpenTypesInferWildcards.doFillIntoGrid(newComposite, 1);
		fOpenTypesInferWildcards.setSelection(preferenceStore.getBoolean(PreferenceConstants.OPEN_TYPES_INFER_WILDCARDS));

		return super.createButtonBar(parent);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		if (getClass() == OpenTypeSelectionDialog.class) {
			addCommand(IWorkbenchCommandConstants.EDIT_PASTE, getPatternControl(), () -> {
				Text text= (Text) getPatternControl();
				Clipboard clipboard= new Clipboard(text.getDisplay());
				try {
					String clipText= (String) clipboard.getContents(TextTransfer.getInstance());
					try {
						String toInsert= TypeInfoFilter.simplifySearchText(clipText);
						clipboard.setContents(new Object[] { toInsert },
								new Transfer[] { TextTransfer.getInstance() });
						text.paste();
					} finally {
						clipboard.setContents(new Object[] { clipText },
								new Transfer[] { TextTransfer.getInstance() });
					}
				} finally {
					clipboard.dispose();
				}
			});
			createButton(parent, IDialogConstants.OK_ID, JavaUIMessages.OpenTypeSelectionDialog_open, true);
			createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		} else
			super.createButtonsForButtonBar(parent);
	}

	private static void addCommand(String commandId, Control control, Runnable runnable) {
		IHandlerService hs= PlatformUI.getWorkbench().getService(IHandlerService.class);
		if (hs == null) {
			return;
		}
		IAction action= new Action("any_" + commandId) { //$NON-NLS-1$
			@Override
			public void run() {
				if (control.isDisposed()) {
					return;
				}
				runnable.run();
			}
		};
		IHandlerActivation handlerActivation= hs.activateHandler(commandId, new ActionHandler(action),
				new ActiveShellExpression(control.getShell()));
		control.addDisposeListener(_ -> hs.deactivateHandler(handlerActivation));
	}

	@Override
	public void create() {
		super.create();
		Control patternControl= getPatternControl();
		if (patternControl instanceof Text text) {
			text.addModifyListener(_ -> {
				if (text.getText().trim().isEmpty()) {
					refreshPreview(null);
				}
			});
		}
	}

	@Override
	protected void updateStatus(IStatus status) {
		super.updateStatus(status);
		if (status != null && status.matches(IStatus.ERROR)) {
			refreshPreview(null);
		}
	}

	@Override
	protected Control createExtendedContentArea(Composite parent) {
		Control addition= super.createExtendedContentArea(parent);
		createPreviewArea(parent);
		return addition != null ? addition : fPreviewContainer;
	}

	private void createPreviewArea(Composite parent) {
		fPreviewContainer= new Composite(parent, SWT.BORDER);
		GridData gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan= 2;
		gd.heightHint= 150;
		gd.exclude= true;
		fPreviewContainer.setLayoutData(gd);
		fPreviewContainer.setVisible(false);
		fPreviewContainer.setLayout(new FillLayout());

		CompositeRuler ruler= new CompositeRuler();
		LineNumberRulerColumn lineCol= new LineNumberRulerColumn();
		ruler.addDecorator(0, lineCol);

		IPreferenceStore store= JavaPlugin.getDefault().getCombinedPreferenceStore();
		fPreviewViewer= new JavaSourceViewer(fPreviewContainer, ruler, null, false, SWT.LEFT_TO_RIGHT | SWT.H_SCROLL | SWT.V_SCROLL | SWT.READ_ONLY, store);
		fPreviewViewer.getTextWidget().setFont(JFaceResources.getFont(JFaceResources.TEXT_FONT));
		fPreviewViewer.getTextWidget().setData("org.eclipse.e4.ui.css.disabled", Boolean.TRUE); //$NON-NLS-1$
		fPreviewViewer.setEditable(false);

		JavaTextTools tools= JavaPlugin.getDefault().getJavaTextTools();
		if (tools != null) {
			fPreviewViewer.configure(new JavaSourceViewerConfiguration(tools.getColorManager(), store, null, IJavaPartitions.JAVA_PARTITIONING));
		}
	}

	private void updatePreviewVisibility(boolean visible) {
		if (fPreviewContainer == null || fPreviewContainer.isDisposed()) {
			return;
		}
		GridData gd= (GridData) fPreviewContainer.getLayoutData();
		if (gd.exclude == visible) {
			gd.exclude= !visible;
			fPreviewContainer.setVisible(visible);
			Composite parent= fPreviewContainer.getParent();
			if (parent != null && !parent.isDisposed()) {
				parent.layout(true, true);
			}
		}
	}

	@Override
	protected void handleSelected(StructuredSelection selection) {
		super.handleSelected(selection);
		refreshPreview(selection);
	}

	private void clearPreview() {
		if (fPreviewViewer != null && !fPreviewViewer.getTextWidget().isDisposed()) {
			fPreviewViewer.setDocument(null);
			fPreviewViewer.getTextWidget().setText(""); //$NON-NLS-1$
		}
		updatePreviewVisibility(false);
	}

	private void refreshPreview(StructuredSelection selection) {
		if (fPreviewViewer == null || fPreviewViewer.getTextWidget().isDisposed()) {
			return;
		}
		if (selection == null || selection.isEmpty() || selection.size() > 1
				|| (getPatternControl() instanceof Text text && text.getText().trim().isEmpty())) {
			clearPreview();
			return;
		}
		Object first= selection.getFirstElement();
		if (!(first instanceof TypeNameMatch)) {
			clearPreview();
			return;
		}
		TypeNameMatch match= (TypeNameMatch) first;
		IType type= match.getType();
		if (type == null || !type.exists()) {
			clearPreview();
			return;
		}
		try {
			String source= null;
			int nameOffset= -1;
			int nameLength= 0;

			ICompilationUnit cu= type.getCompilationUnit();
			if (cu != null) {
				source= cu.getSource();
			} else {
				IClassFile cf= type.getClassFile();
				if (cf != null) {
					source= cf.getSource();
				}
				if (source == null) {
					source= type.getSource();
				}
			}

			if (source == null) {
				fPreviewViewer.setDocument(new Document(JavaUIMessages.OpenTypeSelectionDialog_preview_no_source));
				updatePreviewVisibility(true);
				return;
			}

			ISourceRange nameRange= type.getNameRange();
			if (nameRange != null) {
				nameOffset= nameRange.getOffset();
				nameLength= nameRange.getLength();
			}

			IDocument document= new Document(source);
			JavaTextTools tools= JavaPlugin.getDefault().getJavaTextTools();
			if (tools != null) {
				tools.setupJavaDocumentPartitioner(document, IJavaPartitions.JAVA_PARTITIONING);
			}

			int lineNumber= 0;
			if (nameOffset >= 0) {
				try {
					lineNumber= document.getLineOfOffset(nameOffset);
				} catch (BadLocationException ex) {
					lineNumber= 0;
				}
			}

			fPreviewViewer.setDocument(document);
			updatePreviewVisibility(true);

			if (nameOffset >= 0 && nameLength >= 0 && nameOffset <= document.getLength() - nameLength) {
				try {
					int lineStartOffset= document.getLineOffset(lineNumber);
					fPreviewViewer.revealRange(lineStartOffset, document.getLineLength(lineNumber));
					fPreviewViewer.setSelectedRange(nameOffset, 0);

					if (nameLength > 0) {
						StyleRange existing= fPreviewViewer.getTextWidget().getStyleRangeAtOffset(nameOffset);
						StyleRange highlight= existing == null ? new StyleRange() : (StyleRange) existing.clone();
						highlight.start= nameOffset;
						highlight.length= nameLength;
						highlight.fontStyle|= SWT.BOLD;
						fPreviewViewer.getTextWidget().setStyleRange(highlight);
					}
				} catch (BadLocationException ex) {
					// ignore
				}
			}
		} catch (JavaModelException e) {
			fPreviewViewer.setDocument(new Document(JavaUIMessages.OpenTypeSelectionDialog_preview_no_source));
			updatePreviewVisibility(true);
		}
	}

	SourceViewer getPreviewViewer() {
		return fPreviewViewer;
	}

}
