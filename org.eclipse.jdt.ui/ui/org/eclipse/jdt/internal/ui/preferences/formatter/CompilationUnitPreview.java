/*
 * Created on 23.01.2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.jdt.internal.ui.preferences.formatter;

import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.formatter.FormattingContextProperties;
import org.eclipse.jface.text.formatter.IContentFormatter;
import org.eclipse.jface.text.formatter.IContentFormatterExtension2;
import org.eclipse.jface.text.formatter.IFormattingContext;

import org.eclipse.jdt.internal.ui.IJavaStatusConstants;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.comment.CommentFormattingContext;


/**
 * @author sib
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class CompilationUnitPreview extends JavaPreview {

    private String fPreviewText;

    /**
     * @param workingValues
     * @param parent
     */
    public CompilationUnitPreview(Map workingValues, Composite parent) {

        super(workingValues, parent);
        // TODO Auto-generated constructor stub
    }

    protected void doFormatPreview() {
        if (fPreviewText == null) {
            fPreviewDocument.set(""); //$NON-NLS-1$
            return;
        }
        fPreviewDocument.set(fPreviewText);
		
		fSourceViewer.setRedraw(false);
		final IFormattingContext context = new CommentFormattingContext();
		try {
			final IContentFormatter formatter =	fViewerConfiguration.getContentFormatter(fSourceViewer);
			if (formatter instanceof IContentFormatterExtension2) {
				final IContentFormatterExtension2 extension = (IContentFormatterExtension2) formatter;
				context.setProperty(FormattingContextProperties.CONTEXT_PREFERENCES, fWorkingValues);
				context.setProperty(FormattingContextProperties.CONTEXT_DOCUMENT, Boolean.valueOf(true));
				extension.format(fPreviewDocument, context);
			} else
				formatter.format(fPreviewDocument, new Region(0, fPreviewDocument.getLength()));
		} catch (Exception e) {
			final IStatus status= new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IJavaStatusConstants.INTERNAL_ERROR, 
				FormatterMessages.getString("JavaPreview.formatter_exception"), e); //$NON-NLS-1$
			JavaPlugin.log(status);
		} finally {
		    context.dispose();
		    fSourceViewer.setRedraw(true);
		}
    }
    
    public void setPreviewText(String previewText) {
//        if (previewText == null) throw new IllegalArgumentException();
        fPreviewText= previewText;
        update();
    }
}
