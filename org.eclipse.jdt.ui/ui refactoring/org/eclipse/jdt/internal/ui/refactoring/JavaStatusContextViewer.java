/*******************************************************************************
 * Copyright (c) 2003 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IFile;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.SourceViewer;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;

import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStringStatusContext;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.InternalClassFileEditorInput;
import org.eclipse.jdt.internal.ui.javaeditor.JavaSourceViewer;

import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;
import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.ltk.ui.refactoring.TextStatusContextViewer;


public class JavaStatusContextViewer extends TextStatusContextViewer {

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.refactoring.IStatusContextViewer#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
		IPreferenceStore store= JavaPlugin.getDefault().getCombinedPreferenceStore();
		getSourceViewer().configure(new JavaSourceViewerConfiguration(JavaPlugin.getDefault().getJavaTextTools().getColorManager(), store, null, null));
	}
	
	protected SourceViewer createSourceViewer(Composite parent) {
		IPreferenceStore store= JavaPlugin.getDefault().getCombinedPreferenceStore();
		return new JavaSourceViewer(parent, null, null, false, SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI | SWT.FULL_SELECTION, store);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.refactoring.IStatusContextViewer#setInput(java.lang.Object)
	 */
	public void setInput(RefactoringStatusContext context) {
		if (context instanceof JavaStatusContext) {
			JavaStatusContext jsc= (JavaStatusContext)context;
			IDocument document= null;
			if (jsc.isBinary()) {
				IEditorInput editorInput= new InternalClassFileEditorInput(jsc.getClassFile());
				document= getDocument(JavaPlugin.getDefault().getClassFileDocumentProvider(), editorInput);
				updateTitle(jsc.getClassFile());
			} else {
				ICompilationUnit cunit= jsc.getCompilationUnit();
				if (cunit.isWorkingCopy()) {
					try {
						document= newJavaDocument(cunit.getSource());
					} catch (JavaModelException e) {
						// document is null which is a valid input.
					}
				} else {
					IEditorInput editorInput= new FileEditorInput((IFile)cunit.getResource());
					document= getDocument(JavaPlugin.getDefault().getCompilationUnitDocumentProvider(), editorInput);
				}
				updateTitle(cunit);
			}
			setInput(document, createRegion(jsc.getSourceRange()));
		} else if (context instanceof JavaStringStatusContext) {
			updateTitle(null);
			JavaStringStatusContext sc= (JavaStringStatusContext)context;
			setInput(newJavaDocument(sc.getSource()), createRegion(sc.getSourceRange()));
		}
	}
	
	private IDocument newJavaDocument(String source) {
		IDocument result= new Document(source);
		JavaTextTools textTools= JavaPlugin.getDefault().getJavaTextTools();
		textTools.setupJavaDocumentPartitioner(result);
		return result;
	}
	
	private static IRegion createRegion(ISourceRange range) {
		return new Region(range.getOffset(), range.getLength());
	}
	
	private IDocument getDocument(IDocumentProvider provider, IEditorInput input) {
		if (input == null)
			return null;
		IDocument result= null;
		try {
			provider.connect(input);
			result= provider.getDocument(input);
		} catch (CoreException e) {
		} finally {
			provider.disconnect(input);
		}
		return result;
	}	
}
