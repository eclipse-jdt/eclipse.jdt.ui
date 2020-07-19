/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.javadocexport;

import org.eclipse.core.filesystem.URIUtil;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.console.IHyperlink;

import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.debug.ui.console.IConsole;
import org.eclipse.debug.ui.console.IConsoleLineTracker;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;


public class JavadocConsoleLineTracker implements IConsoleLineTracker {

	private static class JavadocConsoleHyperLink implements IHyperlink {

		private IPath fExternalPath;
		private int fLineNumber;

		public JavadocConsoleHyperLink(IPath externalPath, int lineNumber) {
			fExternalPath= externalPath;
			fLineNumber= lineNumber;
		}

		@Override
		public void linkEntered() {
		}

		@Override
		public void linkExited() {
		}

		@Override
		public void linkActivated() {
			try {
				for (IFile curr : ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(URIUtil.toURI(fExternalPath.makeAbsolute()))) {
					IJavaElement element= JavaCore.create(curr);
					if (element != null && element.exists()) {
						IEditorPart part= JavaUI.openInEditor(element, true, false);
						if (part instanceof ITextEditor) {
							revealLine((ITextEditor) part, fLineNumber);
						}
						return;
					}
				}
			} catch (BadLocationException | PartInitException | JavaModelException e) {
				JavaPlugin.log(e);
			}
		}

		private void revealLine(ITextEditor editor, int lineNumber) throws BadLocationException {
			IDocument document= editor.getDocumentProvider().getDocument(editor.getEditorInput());
			IRegion region= document.getLineInformation(lineNumber - 1);
			editor.selectAndReveal(region.getOffset(), 0);
		}

	}


	private IConsole fConsole;

	/**
	 *
	 */
	public JavadocConsoleLineTracker() {
	}

	@Override
	public void init(IConsole console) {
		fConsole= console;
	}

	@Override
	public void lineAppended(IRegion line) {
		try {
			int offset = line.getOffset();
			int length = line.getLength();
			String text = fConsole.getDocument().get(offset, length);

			int index1= text.indexOf(':');
			if (index1 == -1) {
				return;
			}

			int lineNumber= -1;
			IPath path= null;
			int index2= text.indexOf(':', index1 + 1);
			while ((index2 != -1) && (path == null)) {
				if (index1 < index2) {
					try {
						String substr= text.substring(index1 + 1, index2);
						lineNumber= Integer.parseInt(substr);
						path= Path.fromOSString(text.substring(0, index1));
					} catch (NumberFormatException e) {
						// ignore
					}
				}
				index1= index2;
				index2= text.indexOf(':', index1 + 1);
			}

			if (lineNumber != -1) {
				JavadocConsoleHyperLink link= new JavadocConsoleHyperLink(path, lineNumber);
				fConsole.addLink(link, line.getOffset(), index1);

			}
		} catch (BadLocationException e) {
			// ignore
		}
	}



	@Override
	public void dispose() {
		fConsole = null;
	}

}
