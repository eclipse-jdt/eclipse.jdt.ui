/*
 * Created on 23.01.2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.jdt.internal.ui.preferences.formatter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.text.TextUtilities;

import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.ui.IJavaStatusConstants;
import org.eclipse.jdt.internal.ui.JavaPlugin;



public class SnippetPreview extends JavaPreview {
  
    public final static class PreviewSnippet {
        
        public String header;
        public final String source;
        public final int kind;
        
        public PreviewSnippet(int kind, String source) {
            this.kind= kind;
            this.source= source;
        }
    }
    
    private ArrayList fSnippets;

    public SnippetPreview(Map workingValues, Composite parent) {
        super(workingValues, parent);
        fSnippets= new ArrayList();
    }

    protected void doFormatPreview() {
        if (fSnippets.isEmpty()) { 
            fPreviewDocument.set(""); //$NON-NLS-1$
            return;
        }
        
        final String delimiter= TextUtilities.getDefaultLineDelimiter(fPreviewDocument);
        
        final StringBuffer buffer= new StringBuffer();
        for (final Iterator iter= fSnippets.iterator(); iter.hasNext();) {
            final PreviewSnippet snippet= (PreviewSnippet) iter.next();
            String formattedSource;
            try {
                formattedSource= CodeFormatterUtil.format(snippet.kind, snippet.source, 0, null, delimiter, fWorkingValues);
            } catch (Exception e) {
                final IStatus status= new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IJavaStatusConstants.INTERNAL_ERROR, 
                    FormatterMessages.getString("JavaPreview.formatter_exception"), e); //$NON-NLS-1$
                JavaPlugin.log(status);
                continue;
            }
            buffer.append(delimiter);            
            buffer.append(formattedSource);
            buffer.append(delimiter);            
            buffer.append(delimiter);
        }
        fPreviewDocument.set(buffer.toString());
    }
    
    
    
    public void add(PreviewSnippet snippet) {
        fSnippets.add(snippet);
    }
    
    public void remove(PreviewSnippet snippet) {
        fSnippets.remove(snippet);
    }
    
    public void addAll(Collection snippets) {
        fSnippets.addAll(snippets);
    }
    
    public void clear() {
        fSnippets.clear();
    }

}
