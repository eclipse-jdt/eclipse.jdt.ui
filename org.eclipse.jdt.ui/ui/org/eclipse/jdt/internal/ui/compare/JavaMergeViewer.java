/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.compare;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.events.DisposeEvent;

import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.source.SourceViewer;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.contentmergeviewer.ITokenComparator;
import org.eclipse.compare.contentmergeviewer.TextMergeViewer;

import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;
import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

public class JavaMergeViewer extends TextMergeViewer {
	
	private IPropertyChangeListener fPreferenceChangeListener;
	private IPreferenceStore fPreferenceStore;
		
		
	public JavaMergeViewer(Composite parent, int styles, CompareConfiguration mp) {
		super(parent, styles, mp);
		
		fPreferenceStore= JavaPlugin.getDefault().getPreferenceStore();
		if (fPreferenceStore != null) {
			 fPreferenceChangeListener= new IPropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent event) {
					JavaTextTools tools= JavaCompareUtilities.getJavaTextTools();
					if (tools.affectsBehavior(event))
						invalidateTextPresentation();
				}
			};
			fPreferenceStore.addPropertyChangeListener(fPreferenceChangeListener);
		}
	}
	
	protected void handleDispose(DisposeEvent event) {
		if (fPreferenceChangeListener != null) {
			if (fPreferenceStore != null)
				fPreferenceStore.removePropertyChangeListener(fPreferenceChangeListener);
			fPreferenceChangeListener= null;
		}
		super.handleDispose(event);
	}
	
	public String getTitle() {
		return CompareMessages.getString("JavaMergeViewer.title"); //$NON-NLS-1$
	}

	protected ITokenComparator createTokenComparator(String s) {
		return new JavaTokenComparator(s, true);
	}
	
	protected IDocumentPartitioner getDocumentPartitioner() {
		return JavaCompareUtilities.createJavaPartitioner();
	}
		
	protected void configureTextViewer(TextViewer textViewer) {
		if (textViewer instanceof SourceViewer) {
			JavaTextTools tools= JavaCompareUtilities.getJavaTextTools();
			if (tools != null)
				((SourceViewer)textViewer).configure(new JavaSourceViewerConfiguration(tools, null));
		}
	}
}
