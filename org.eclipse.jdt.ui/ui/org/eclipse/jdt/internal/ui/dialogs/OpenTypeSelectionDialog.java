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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

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
import org.eclipse.jface.text.IDocumentPartitioner;
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
import org.eclipse.jdt.internal.ui.text.FastJavaPartitionScanner;
import org.eclipse.jdt.internal.ui.text.FastJavaPartitioner;
import org.eclipse.jdt.internal.ui.util.SWTUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;

/**
 * A type selection dialog used for opening types.
 */
public class OpenTypeSelectionDialog extends FilteredTypesSelectionDialog {

	private static final String DIALOG_SETTINGS= "org.eclipse.jdt.internal.ui.dialogs.OpenTypeSelectionDialog2"; //$NON-NLS-1$

	private SelectionButtonDialogField fOpenTypesInferWildcards;

	/**
	 * Delay in milliseconds before a preview is computed, so that quickly navigating through the
	 * result list does not compute a preview for every intermediate selection.
	 */
	private static final int PREVIEW_DELAY_MS= 50;

	/** The partition types of the Java partitioner, the same as the ones used by the Java editor. */
	private static final String[] LEGAL_PARTITION_TYPES= {
			IJavaPartitions.JAVA_DOC,
			IJavaPartitions.JAVA_MULTI_LINE_COMMENT,
			IJavaPartitions.JAVA_SINGLE_LINE_COMMENT,
			IJavaPartitions.JAVA_STRING,
			IJavaPartitions.JAVA_CHARACTER,
			IJavaPartitions.JAVA_MULTI_LINE_STRING,
			IJavaPartitions.JAVA_MARKDOWN_COMMENT
	};

	/**
	 * Result of the background preview computation.
	 *
	 * @param document the document to show
	 * @param nameOffset the offset of the type name in the document, or -1
	 * @param nameLength the length of the type name
	 */
	private record PreviewContent(IDocument document, int nameOffset, int nameLength) {
	}

	private Composite fPreviewContainer;

	private SourceViewer fPreviewViewer;

	/** Incremented for every new request and whenever the preview is cleared. UI thread only. */
	private int fPreviewGeneration;

	/** The scheduled or running preview job, or <code>null</code>. UI thread only. */
	private Job fPreviewJob;

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
	public boolean close() {
		cancelPendingPreview();
		return super.close();
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
		fPreviewViewer.getTextWidget().setFont(JFaceResources.getFont(PreferenceConstants.EDITOR_TEXT_FONT));
		fPreviewViewer.getTextWidget().setData("org.eclipse.e4.ui.css.disabled", Boolean.TRUE); //$NON-NLS-1$
		fPreviewViewer.setEditable(false);
		SWTUtil.setAccessibilityText(fPreviewViewer.getTextWidget(), JavaUIMessages.OpenTypeSelectionDialog_preview_accessible_name);

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
		// Refresh first: if the validation in the super class rejects the selection, it reports an
		// error status, and updateStatus() then clears the preview and cancels this request.
		refreshPreview(selection);
		super.handleSelected(selection);
	}

	/**
	 * Invalidates any pending or running preview computation, so that its result is
	 * ignored. Must be called from the UI thread.
	 */
	private void cancelPendingPreview() {
		fPreviewGeneration++;
		if (fPreviewJob != null) {
			fPreviewJob.cancel();
			fPreviewJob= null;
		}
	}

	/**
	 * Returns whether the preview viewer is missing or already disposed. The text widget of a
	 * viewer is <code>null</code> once its control is disposed, for example when the shell is
	 * disposed directly without calling {@link #close()}.
	 *
	 * @return <code>true</code> if the preview can no longer be used
	 */
	private boolean isPreviewDisposed() {
		return fPreviewViewer == null || fPreviewViewer.getTextWidget() == null || fPreviewViewer.getTextWidget().isDisposed();
	}

	private void clearPreview() {
		cancelPendingPreview();
		if (!isPreviewDisposed()) {
			// An empty document instead of null: with no document, the verify listener of the
			// viewer fails with a NullPointerException when the widget text is changed.
			fPreviewViewer.setDocument(new Document());
		}
		updatePreviewVisibility(false);
	}

	private void refreshPreview(StructuredSelection selection) {
		if (isPreviewDisposed()) {
			return;
		}
		if (selection == null || selection.isEmpty() || selection.size() > 1) {
			clearPreview();
			return;
		}
		if (!(selection.getFirstElement() instanceof TypeNameMatch match)) {
			clearPreview();
			return;
		}

		// Only cheap work happens on the UI thread. Resolving the type, reading the source,
		// building and partitioning the document is done by the job.
		cancelPendingPreview();
		int generation= fPreviewGeneration;
		Display display= fPreviewViewer.getTextWidget().getDisplay();

		Job job= new Job(JavaUIMessages.OpenTypeSelectionDialog_preview_job_name) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				PreviewContent content= computePreviewContent(match, monitor);
				if (monitor.isCanceled() || display.isDisposed()) {
					return Status.CANCEL_STATUS;
				}
				display.asyncExec(() -> showPreview(generation, content));
				return Status.OK_STATUS;
			}
		};
		job.setSystem(true);
		job.setPriority(Job.INTERACTIVE);
		fPreviewJob= job;
		job.schedule(PREVIEW_DELAY_MS);
	}

	/**
	 * Computes the preview content. Runs in a background job and must not access any widget.
	 *
	 * @param match the selected type
	 * @param monitor the progress monitor
	 * @return the content to show, or <code>null</code> if the preview must be cleared
	 */
	private static PreviewContent computePreviewContent(TypeNameMatch match, IProgressMonitor monitor) {
		try {
			IType type= match.getType();
			if (type == null || !type.exists()) {
				return null;
			}

			String source= null;
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
			if (monitor.isCanceled()) {
				return null;
			}
			if (source == null) {
				return noSourceContent();
			}

			int nameOffset= -1;
			int nameLength= 0;
			ISourceRange nameRange= type.getNameRange();
			if (nameRange != null) {
				nameOffset= nameRange.getOffset();
				nameLength= nameRange.getLength();
			}

			Document document= new Document(source);
			// Bound to the project of the type, so that text blocks are recognized according to its
			// compliance and not according to the project of whichever editor is active.
			IDocumentPartitioner partitioner= new FastJavaPartitioner(new FastJavaPartitionScanner(type.getJavaProject()), LEGAL_PARTITION_TYPES);
			document.setDocumentPartitioner(IJavaPartitions.JAVA_PARTITIONING, partitioner);
			partitioner.connect(document);
			return new PreviewContent(document, nameOffset, nameLength);
		} catch (JavaModelException e) {
			return noSourceContent();
		}
	}

	private static PreviewContent noSourceContent() {
		return new PreviewContent(new Document(JavaUIMessages.OpenTypeSelectionDialog_preview_no_source), -1, 0);
	}

	/**
	 * Installs the computed content. Runs in the UI thread.
	 *
	 * @param generation the generation the content was computed for
	 * @param content the content, or <code>null</code> to clear the preview
	 */
	private void showPreview(int generation, PreviewContent content) {
		if (generation != fPreviewGeneration || isPreviewDisposed()) {
			return; // outdated by a newer selection, a clear, or the dialog is gone
		}
		fPreviewJob= null;
		if (content == null) {
			clearPreview();
			return;
		}

		IDocument document= content.document();
		int nameOffset= content.nameOffset();
		int nameLength= content.nameLength();

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
	}

}
