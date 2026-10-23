/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.propertiesfileeditor;

import java.util.function.Function;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;

import org.eclipse.jface.viewers.ILabelProvider;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ISynchronizable;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.LineNumberRulerColumn;
import org.eclipse.jface.text.source.SourceViewer;

import org.eclipse.ui.dialogs.TwoPaneElementSelector;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.SWTUtil;

/**
 * The "Open" dialog shown when a properties key has several references. In addition to the two
 * lists of {@link TwoPaneElementSelector}, it shows the source of the selected reference in a
 * read-only preview pane, scrolled to the line of the reference.
 */
class KeyReferenceSelectionDialog extends TwoPaneElementSelector {

	/**
	 * The place of a key reference in a file.
	 *
	 * @param resource the file containing the reference
	 * @param offset the offset of the reference in the file
	 * @param length the length of the reference
	 */
	record KeyLocation(IResource resource, int offset, int length) {
	}

	/**
	 * Result of the background preview computation.
	 *
	 * @param document the document to show
	 * @param offset the offset of the reference in the document, or -1
	 * @param length the length of the reference
	 */
	private record PreviewContent(IDocument document, int offset, int length) {
	}

	/**
	 * Delay in milliseconds before a preview is computed, so that quickly navigating through the
	 * lists does not compute a preview for every intermediate selection.
	 */
	private static final int PREVIEW_DELAY_MS= 50;

	private final Function<Object, KeyLocation> fLocationMapper;

	private Composite fPreviewContainer;

	private SourceViewer fPreviewViewer;

	private LineNumberRulerColumn fLineNumberColumn;

	/** The lower list, whose theme colors the preview copies. */
	private Table fLowerTable;

	/** Incremented for every new request and whenever the preview is cleared. UI thread only. */
	private int fPreviewGeneration;

	/** The scheduled or running preview job, or <code>null</code>. UI thread only. */
	private Job fPreviewJob;

	/** The element the current preview was requested for, or <code>null</code>. UI thread only. */
	private Object fPreviewElement;

	/** Whether the shell was already enlarged for the preview pane. UI thread only. */
	private boolean fShellGrown;

	/**
	 * Creates the dialog.
	 *
	 * @param parent the parent shell
	 * @param elementRenderer the renderer of the upper list
	 * @param qualifierRenderer the renderer of the lower list
	 * @param locationMapper maps a list element to its location, or to <code>null</code> if the
	 *            element has no location
	 */
	KeyReferenceSelectionDialog(Shell parent, ILabelProvider elementRenderer, ILabelProvider qualifierRenderer, Function<Object, KeyLocation> locationMapper) {
		super(parent, elementRenderer, qualifierRenderer);
		fLocationMapper= locationMapper;
	}

	@Override
	protected Table createLowerList(Composite parent) {
		fLowerTable= super.createLowerList(parent);
		return fLowerTable;
	}

	@Override
	public Control createDialogArea(Composite parent) {
		Control area= super.createDialogArea(parent);
		createPreviewArea((Composite) area);
		return area;
	}

	private void createPreviewArea(Composite parent) {
		fPreviewContainer= new Composite(parent, SWT.NONE);
		GridData gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint= 150;
		gd.exclude= true;
		fPreviewContainer.setLayoutData(gd);
		fPreviewContainer.setVisible(false);
		FillLayout layout= new FillLayout();
		layout.marginWidth= 1;
		layout.marginHeight= 1;
		fPreviewContainer.setLayout(layout);
		fPreviewContainer.addPaintListener(e -> {
			Rectangle area= fPreviewContainer.getClientArea();
			e.gc.setForeground(fPreviewContainer.getForeground());
			e.gc.setAlpha(55);
			e.gc.drawRectangle(0, 0, area.width - 1, area.height - 1);
		});

		CompositeRuler ruler= new CompositeRuler();
		fLineNumberColumn= new LineNumberRulerColumn() {
			@Override
			protected int computeNumberOfDigits() {
				// The ruler of the base class is at least two digits wide, which is much in a small pane.
				// One character more than the digits: the blank separates the number from the text.
				IDocument document= getParentRuler().getTextViewer().getDocument();
				int lines= document == null ? 1 : document.getNumberOfLines();
				return String.valueOf(lines).length() + 1;
			}

			@Override
			protected String createDisplayString(int line) {
				// A string as long as the column is drawn at the left edge, so the padding places the number.
				String number= super.createDisplayString(line);
				int free= Math.max(0, computeNumberOfDigits() - number.length());
				return " ".repeat(free / 2) + number + " ".repeat(free - free / 2); //$NON-NLS-1$ //$NON-NLS-2$
			}
		};
		ruler.addDecorator(0, fLineNumberColumn);
		fPreviewViewer= new SourceViewer(fPreviewContainer, ruler, SWT.H_SCROLL | SWT.V_SCROLL | SWT.READ_ONLY);
		applyPreviewFont();
		fPreviewViewer.setEditable(false);
		SWTUtil.setAccessibilityText(fPreviewViewer.getTextWidget(), PropertiesFileEditorMessages.OpenAction_SelectionDialog_preview_accessible_name);
	}

	private void updatePreviewVisibility(boolean visible) {
		if (fPreviewContainer == null || fPreviewContainer.isDisposed()) {
			return;
		}
		GridData gd= (GridData) fPreviewContainer.getLayoutData();
		if (gd.exclude == visible) {
			gd.exclude= !visible;
			fPreviewContainer.setVisible(visible);
			if (visible) {
				growShellForPreview();
			}
			Composite parent= fPreviewContainer.getParent();
			if (parent != null && !parent.isDisposed()) {
				parent.layout(true, true);
			}
		}
	}

	/**
	 * Enlarges the shell by the height of the preview pane, the first time the pane is shown, so
	 * that the lists keep their size. The shell never grows beyond the client area of its monitor
	 * and is not shrunk again when the pane is hidden.
	 */
	private void growShellForPreview() {
		if (fShellGrown) {
			return;
		}
		fShellGrown= true;
		Shell shell= fPreviewContainer.getShell();
		if (shell.getMaximized()) {
			return;
		}
		int extra= ((GridData) fPreviewContainer.getLayoutData()).heightHint + 2 * fPreviewContainer.getBorderWidth();
		if (fPreviewContainer.getParent().getLayout() instanceof GridLayout layout) {
			extra+= layout.verticalSpacing;
		}
		Rectangle bounds= shell.getBounds();
		Rectangle area= shell.getMonitor().getClientArea();
		int height= Math.min(bounds.height + extra, area.height);
		int y= Math.max(area.y, Math.min(bounds.y, area.y + area.height - height));
		shell.setBounds(bounds.x, y, bounds.width, height);
	}

	@Override
	public boolean close() {
		cancelPendingPreview();
		return super.close();
	}

	/**
	 * Called whenever the selection of one of the lists changes, when the lower list is refilled,
	 * and when the dialog is created.
	 */
	@Override
	protected boolean validateCurrentSelection() {
		boolean valid= super.validateCurrentSelection();
		refreshPreview(getLowerSelectedElement());
		return valid;
	}

	@Override
	protected void handleEmptyList() {
		super.handleEmptyList();
		clearPreview();
	}

	/**
	 * Invalidates any pending or running preview computation, so that its result is ignored. Must
	 * be called from the UI thread.
	 */
	private void cancelPendingPreview() {
		fPreviewGeneration++;
		fPreviewElement= null;
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

	private void refreshPreview(Object element) {
		if (isPreviewDisposed()) {
			return;
		}
		KeyLocation location= element == null ? null : fLocationMapper.apply(element);
		if (location == null) {
			clearPreview();
			return;
		}
		if (element == fPreviewElement) {
			return; // already shown or being computed
		}

		// Reading the file and building the document is done by the job.
		cancelPendingPreview();
		fPreviewElement= element;
		int generation= fPreviewGeneration;
		Display display= fPreviewViewer.getTextWidget().getDisplay();

		Job job= new Job(PropertiesFileEditorMessages.OpenAction_SelectionDialog_preview_job_name) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				PreviewContent content= computePreviewContent(location, monitor);
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
	 * @param location the location to show
	 * @param monitor the progress monitor
	 * @return the content to show, or <code>null</code> if the preview must be cleared
	 */
	private static PreviewContent computePreviewContent(KeyLocation location, IProgressMonitor monitor) {
		if (!(location.resource() instanceof IFile file)) {
			return null;
		}
		if (!file.exists()) {
			return noSourceContent();
		}

		IPath path= file.getFullPath();
		ITextFileBufferManager manager= FileBuffers.getTextFileBufferManager();
		try {
			manager.connect(path, LocationKind.NORMALIZE, null);
			try {
				ITextFileBuffer buffer= manager.getTextFileBuffer(path, LocationKind.NORMALIZE);
				IDocument source= buffer == null ? null : buffer.getDocument();
				if (source == null) {
					return noSourceContent();
				}
				// The buffer may be shared with an open editor: copy its text under the document lock.
				String text;
				Object lock= source;
				if (source instanceof ISynchronizable synchronizable && synchronizable.getLockObject() != null) {
					lock= synchronizable.getLockObject();
				}
				synchronized (lock) {
					text= source.get();
				}
				if (monitor.isCanceled()) {
					return null;
				}
				return new PreviewContent(new Document(text), location.offset(), location.length());
			} finally {
				manager.disconnect(path, LocationKind.NORMALIZE, null);
			}
		} catch (CoreException e) {
			JavaPlugin.log(e.getStatus());
			return noSourceContent();
		}
	}

	private static PreviewContent noSourceContent() {
		return new PreviewContent(new Document(PropertiesFileEditorMessages.OpenAction_SelectionDialog_preview_no_source), -1, 0);
	}

	/**
	 * Gives the text and the ruler the font of the lower list, so that the preview looks like the
	 * lists above. The ruler gets the font explicitly, because it computes its width from it and is
	 * not reached by the CSS theme.
	 */
	private void applyPreviewFont() {
		Font font= fLowerTable.getFont();
		StyledText text= fPreviewViewer.getTextWidget();
		if (!font.equals(text.getFont())) {
			text.setFont(font);
		}
		fLineNumberColumn.setFont(font);
		fPreviewViewer.getControl().requestLayout();
	}

	/**
	 * Takes the colors of widgets that the theme styles, so that the preview looks like the rest of
	 * the dialog in every theme and on every platform. The text uses the colors of the lower list.
	 * The ruler paints itself and is not reached by the CSS theme, so it gets the colors of the
	 * dialog, which sets it apart from the text.
	 */
	private void applyPreviewColors() {
		Shell shell= getShell();
		fPreviewViewer.getTextWidget().setBackground(fLowerTable.getBackground());
		fPreviewViewer.getTextWidget().setForeground(fLowerTable.getForeground());
		fPreviewContainer.setForeground(fLowerTable.getForeground());
		fLineNumberColumn.setBackground(shell.getBackground());
		fLineNumberColumn.setForeground(shell.getForeground());
		fLineNumberColumn.redraw();
		fPreviewContainer.redraw();
	}

	/**
	 * Installs the computed content. Runs in the UI thread.
	 *
	 * @param generation the generation the content was computed for
	 * @param content the content, or <code>null</code> to clear the preview
	 */
	private void showPreview(int generation, PreviewContent content) {
		if (generation != fPreviewGeneration || isPreviewDisposed()) {
			return;
		}
		fPreviewJob= null;
		if (content == null) {
			clearPreview();
			return;
		}

		IDocument document= content.document();
		fPreviewViewer.setDocument(document);
		applyPreviewFont();
		applyPreviewColors();
		updatePreviewVisibility(true);

		int offset= content.offset();
		int length= content.length();
		if (offset < 0 || length < 0 || offset > document.getLength() - length) {
			return;
		}
		try {
			int line= document.getLineOfOffset(offset);
			// Reveal the reference itself, not its line, so that it is also scrolled into view
			// horizontally when it is far to the right on a long line. setSelectedRange does not scroll.
			fPreviewViewer.revealRange(offset, length);
			fPreviewViewer.setSelectedRange(offset, 0);
			// revealRange scrolls vertically only when the line is outside the pane, which leaves the
			// line at an edge. Put it in the middle afterwards. With an even number of visible lines,
			// the extra line is shown below the reference.
			StyledText text= fPreviewViewer.getTextWidget();
			int visibleLines= text.getClientArea().height / text.getLineHeight();
			fPreviewViewer.setTopIndex(Math.max(0, line - (visibleLines - 1) / 2));
			if (length > 0) {
				StyleRange existing= fPreviewViewer.getTextWidget().getStyleRangeAtOffset(offset);
				StyleRange highlight= existing == null ? new StyleRange() : (StyleRange) existing.clone();
				highlight.start= offset;
				highlight.length= length;
				highlight.fontStyle|= SWT.BOLD;
				fPreviewViewer.getTextWidget().setStyleRange(highlight);
			}
		} catch (BadLocationException e) {
			JavaPlugin.log(e);
		}
	}
}
