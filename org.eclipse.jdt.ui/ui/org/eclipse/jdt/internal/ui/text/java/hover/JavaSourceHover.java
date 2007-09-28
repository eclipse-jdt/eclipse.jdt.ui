/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.java.hover;

import java.io.IOException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHoverExtension;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.information.IInformationProviderExtension2;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.part.IWorkbenchPartOrientation;

import org.eclipse.ui.editors.text.EditorsUI;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.util.Strings;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.JavaCodeReader;


/**
 * Provides source as hover info for Java elements.
 */
public class JavaSourceHover extends AbstractJavaEditorTextHover implements ITextHoverExtension, IInformationProviderExtension2 {

	/*
	 * @see JavaElementHover
	 */
	public String getHoverInfo(ITextViewer textViewer, IRegion region) {
		IJavaElement[] result= getJavaElementsAt(textViewer, region);
		if (result == null || result.length == 0 || result.length > 1)
			return null;

		IJavaElement curr= result[0];
		if ((curr instanceof IMember || curr instanceof ILocalVariable || curr instanceof ITypeParameter) && curr instanceof ISourceReference) {
			try {
				String source= ((ISourceReference) curr).getSource();
				if (source == null)
					return null;

				source= removeLeadingComments(source);
				String delim= StubUtility.getLineDelimiterUsed(result[0]);

				String[] sourceLines= Strings.convertIntoLines(source);
				String firstLine= sourceLines[0];
				if (!Character.isWhitespace(firstLine.charAt(0)))
					sourceLines[0]= ""; //$NON-NLS-1$
				Strings.trimIndentation(sourceLines, curr.getJavaProject());

				if (!Character.isWhitespace(firstLine.charAt(0)))
					sourceLines[0]= firstLine;

				source= Strings.concatenate(sourceLines, delim);

				return source;

			} catch (JavaModelException ex) {
			}
		}

		return null;
	}

	private String removeLeadingComments(String source) {
		final JavaCodeReader reader= new JavaCodeReader();
		IDocument document= new Document(source);
		int i;
		try {
			reader.configureForwardReader(document, 0, document.getLength(), true, false);
			int c= reader.read();
			while (c != -1 && (c == '\r' || c == '\n')) {
				c= reader.read();
			}
			i= reader.getOffset();
			reader.close();
		} catch (IOException ex) {
			i= 0;
		} finally {
			try {
				reader.close();
			} catch (IOException ex) {
				JavaPlugin.log(ex);
			}
		}

		if (i < 0)
			return source;
		return source.substring(i);
	}

	/*
	 * @see org.eclipse.jface.text.ITextHoverExtension#getHoverControlCreator()
	 * @since 3.0
	 */
	public IInformationControlCreator getHoverControlCreator() {
		return new IInformationControlCreator() {
			public IInformationControl createInformationControl(Shell parent) {
				IEditorPart editor= getEditor();
				int shellStyle= SWT.TOOL | SWT.NO_TRIM;
				if (editor instanceof IWorkbenchPartOrientation)
					shellStyle |= ((IWorkbenchPartOrientation)editor).getOrientation();
				return new SourceViewerInformationControl(parent, shellStyle, SWT.NONE, EditorsUI.getTooltipAffordanceString());
			}
		};
	}

	/*
	 * @see IInformationProviderExtension2#getInformationPresenterControlCreator()
	 * @since 3.0
	 */
	public IInformationControlCreator getInformationPresenterControlCreator() {
		return new IInformationControlCreator() {
			public IInformationControl createInformationControl(Shell parent) {
				int style= SWT.V_SCROLL | SWT.H_SCROLL;
				int shellStyle= SWT.RESIZE | SWT.TOOL;
				IEditorPart editor= getEditor();
				if (editor instanceof IWorkbenchPartOrientation)
					shellStyle |= ((IWorkbenchPartOrientation)editor).getOrientation();
				return new SourceViewerInformationControl(parent, shellStyle, style);
			}
		};
	}
}
