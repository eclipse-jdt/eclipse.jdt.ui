/*******************************************************************************
 * Copyright (c) 2024 SAP SE.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javaeditor;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.source.ISourceViewer;

import org.eclipse.ui.texteditor.stickyscroll.IStickyLine;
import org.eclipse.ui.texteditor.stickyscroll.IStickyLinesProvider;
import org.eclipse.ui.texteditor.stickyscroll.StickyLine;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;

public class StickyLinesProviderJava implements IStickyLinesProvider {

	@Override
	public List<IStickyLine> getStickyLines(ISourceViewer sourceViewer, int lineNumber, StickyLinesProperties properties) {
		LinkedList<IStickyLine> stickyLines= new LinkedList<>();
		JavaEditor javaEditor= (JavaEditor) properties.editor();

		IJavaElement element= null;
		try {
			element= javaEditor.getElementAt(sourceViewer.getDocument().getLineOffset(lineNumber));
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		while (element != null) {
			if (element.getElementType() == IJavaElement.METHOD || element.getElementType() == IJavaElement.TYPE) {
				try {
					ISourceRange sourceRange= ((ISourceReference) element).getNameRange();
					int offset= sourceRange.getOffset();
					int stickyLineNumber= sourceViewer.getDocument().getLineOfOffset(offset);
					stickyLines.addFirst(new StickyLine(stickyLineNumber, sourceViewer));
				} catch (JavaModelException | BadLocationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			element= element.getParent();
		}

		return stickyLines;
	}

}
