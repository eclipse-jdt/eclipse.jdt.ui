/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000,2001
 */
package org.eclipse.jdt.internal.ui.compare;

import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.source.SourceViewer;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.contentmergeviewer.ITokenComparator;
import org.eclipse.compare.contentmergeviewer.TextMergeViewer;

import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;
import org.eclipse.jdt.ui.text.JavaTextTools;


public class JavaMergeViewer extends TextMergeViewer {			
		
	public JavaMergeViewer(Composite parent, int styles, CompareConfiguration mp) {
		super(parent, styles, mp);
	}
	
	public String getTitle() {
		return "Java Source Compare";
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
