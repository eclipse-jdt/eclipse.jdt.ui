/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.text.DefaultLineTracker;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.compare.contentmergeviewer.IMergeViewerContentProvider;

import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.ITextChange;
import org.eclipse.jdt.internal.ui.JavaPlugin;

public class MergeTextViewerContentProvider implements IMergeViewerContentProvider {

	private int fLeftSize;
	
	public void dispose(){
	}
	
	public void inputChanged(Viewer viewer, Object o1, Object o2) {
	}

	//---- Ancestor -------------------------------------------------------------

	public boolean showAncestor(Object element) {
		return false;
	}

	public Image getAncestorImage(Object element) {
		return null;
	}

	public String getAncestorLabel(Object element) {
		return null;
	}

	public Object getAncestorContent(Object element) {
		return null;
	}

	//---- Left side -----------------------------------------------------------

	public Image getLeftImage(Object element) {
		return null;
	}

	public String getLeftLabel(Object element) {
		return RefactoringMessages.getString("MergeTextViewerContentProvider.original_source"); //$NON-NLS-1$
	}

	public Object getLeftContent(Object element) {
		if (!(element instanceof ITextChange))
			return null;
			
		try {
			ITextChange change= (ITextChange)element;
			String content= change.getCurrentContent();
			fLeftSize= content.length();
//			
//			if (true) {
//				content= getText(change, content, 0);
//			}
			return attachPartitioner(new Document(content));
		} catch (JavaModelException e) {
			return null;
		}
	}

	public void saveLeftContent(Object element, byte[] content) {
	}

	public boolean isLeftEditable(Object element) {
		return false;
	}

	//---- Right side -----------------------------------------------------------

	public Image getRightImage(Object element) {
		return null;
	}

	public String getRightLabel(Object element) {
		return RefactoringMessages.getString("MergeTextViewerContentProvider.refactored_source"); //$NON-NLS-1$
	}

	public Object getRightContent(Object element) {
		if (!(element instanceof ITextChange))
			return null;
			
		try {
			ITextChange change= (ITextChange)element;
			String content= change.getPreview();
//			if (true) {
//				int diff= fLeftSize - content.length();
//				content= getText(change, content, diff);
//			}
			return attachPartitioner(new Document(content));
		} catch (JavaModelException e) {
			return null;
		}
	}

	public void saveRightContent(Object element, byte[] content) {
	}

	public boolean isRightEditable(Object element) {
		return false;
	}

	//---- Helpers ---------------------------------------------------------------

	private String getText(IChange change, String content, int sizeDiff) throws JavaModelException {
		String result= content;
		Object element= change.getModifiedLanguageElement();
		if (element instanceof ISourceReference) {
			ISourceRange range= ((ISourceReference)element).getSourceRange();
			result= content.substring(
				correctStart(content, range.getOffset()), 
				(range.getOffset() + range.getLength() - 1) - sizeDiff + 1);
		}	
		return result;	
	}
	
	private int correctStart(String content, int fromIndex) {
		if (fromIndex <= 0)
			return 0;
			
		int result= 0;
		String[] delimiters= DefaultLineTracker.DELIMITERS;
		for (int i= 0; i < delimiters.length; i++) {
			String delimiter= delimiters[i];
			result= Math.max(content.lastIndexOf(delimiter, fromIndex) + delimiter.length(), result);
		}
		return result;
	}
	
	private IDocument attachPartitioner(IDocument document) {
		IDocumentPartitioner partitioner= createJavaPartitioner();
		if (partitioner != null) {
			document.setDocumentPartitioner(partitioner);
			partitioner.connect(document);
		}
		return document;
	}
	
	static private IDocumentPartitioner createJavaPartitioner() {
		JavaTextTools jtt= getJavaTextTools();
		if (jtt != null)
			return jtt.createDocumentPartitioner();
		return null;
	}
	
	static private JavaTextTools getJavaTextTools() {
		JavaPlugin plugin= JavaPlugin.getDefault();
		if (plugin != null)
			return plugin.getJavaTextTools();
		return null;
	}
}


