/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.nls;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.filebuffers.ITextFileBuffer;

import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.InvalidInputException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringFileBuffers;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class NLSUtil {

	//no instances
	private NLSUtil() {
	}

	/**
	 * Returns null if an error occurred.
	 * closes the stream 
	 */
	public static String readString(InputStream is) {
		if (is == null)
			return null;
		BufferedReader reader= null;
		try {
			StringBuffer buffer= new StringBuffer();
			char[] part= new char[2048];
			int read= 0;
			reader= new BufferedReader(new InputStreamReader(is, "8859_1")); //$NON-NLS-1$

			while ((read= reader.read(part)) != -1)
				buffer.append(part, 0, read);

			return buffer.toString();

		} catch (IOException ex) {
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException ex) {
				}
			}
		}
		return null;
	}

	/**
	 * Creates and returns an NLS tag edit for a string that is at the specified position in 
	 * a compilation unit. Returns <code>null</code> if the string is already NLSed 
	 * or the edit could not be created for some other reason.
	 * @throws CoreException 
	 */
	public static TextEdit createNLSEdit(ICompilationUnit cu, int position) throws CoreException {
		NLSLine nlsLine= scanCurrentLine(cu, position);
		if (nlsLine == null)
			return null;
		NLSElement element= findElement(nlsLine, position);
		if (element.hasTag())
			return null;
		NLSElement[] elements= nlsLine.getElements();
		int indexInElementList= Arrays.asList(elements).indexOf(element);
		try {
			int editOffset= computeInsertOffset(elements, indexInElementList, cu);
			String editText= " " + NLSElement.createTagText(indexInElementList + 1); //tags are 1-based //$NON-NLS-1$
			return new InsertEdit(editOffset, editText);
		} catch (BadLocationException exception) {
			JavaPlugin.log(exception);
			return null;
		}
	}

	private static NLSLine scanCurrentLine(ICompilationUnit cu, int position) throws JavaModelException {
		try {
			Assert.isTrue(position >= 0 && position <= cu.getSourceRange().getLength());
			NLSLine[] allLines= NLSScanner.scan(cu);
			for (int i= 0; i < allLines.length; i++) {
				NLSLine line= allLines[i];
				if (findElement(line, position) != null)
					return line;
			}
			return null;
		} catch (InvalidInputException e) {
			return null;
		}
	}

	private static boolean isPositionInElement(NLSElement element, int position) {
		Region elementPosition= element.getPosition();
		return (elementPosition.getOffset() <= position && position <= elementPosition.getOffset() + elementPosition.getLength());
	}

	private static NLSElement findElement(NLSLine line, int position) {
		NLSElement[] elements= line.getElements();
		for (int i= 0; i < elements.length; i++) {
			NLSElement element= elements[i];
			if (isPositionInElement(element, position))
				return element;
		}
		return null;
	}

	//we try to find a good place to put the nls tag
	//first, try to find the previous nlsed-string and try putting after its tag
	//if no such string exists, try finding the next nlsed-string try putting before its tag
	//otherwise, find the line end and put the tag there
	private static int computeInsertOffset(NLSElement[] elements, int index, ICompilationUnit cu) throws CoreException, BadLocationException {
		NLSElement previousTagged= findPreviousTagged(index, elements);
		if (previousTagged != null)
			return previousTagged.getTagPosition().getOffset() + previousTagged.getTagPosition().getLength();
		NLSElement nextTagged= findNextTagged(index, elements);
		if (nextTagged != null)
			return nextTagged.getTagPosition().getOffset();
		return findLineEnd(cu, elements[index].getPosition().getOffset());
	}

	private static NLSElement findPreviousTagged(int startIndex, NLSElement[] elements) {
		int i= startIndex - 1;
		while (i >= 0) {
			if (elements[i].hasTag())
				return elements[i];
			i--;
		}
		return null;
	}

	private static NLSElement findNextTagged(int startIndex, NLSElement[] elements) {
		int i= startIndex + 1;
		while (i < elements.length) {
			if (elements[i].hasTag())
				return elements[i];
			i++;
		}
		return null;
	}

	private static int findLineEnd(ICompilationUnit cu, int position) throws CoreException, BadLocationException {
		try {
			ITextFileBuffer buffer= RefactoringFileBuffers.connect(cu);
			IRegion lineInfo= buffer.getDocument().getLineInformationOfOffset(position);
			return lineInfo.getOffset() + lineInfo.getLength();
		} finally {
			RefactoringFileBuffers.disconnect(cu);
		}
	}
}
