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
package org.eclipse.jdt.internal.ui.text.java.hover;

import java.io.Reader;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.JavaModelException;


import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.JavadocContentAccess;

import org.eclipse.jdt.internal.ui.text.HTMLPrinter;
import org.eclipse.jdt.internal.ui.text.javadoc.JavaDoc2HTMLTextReader;

/**
 * Provides Javadoc as hover info for Java elements.
 * 
 * @since 2.1
 */
public class JavadocHover extends AbstractJavaEditorTextHover {

	private final long LABEL_FLAGS=  JavaElementLabels.ALL_FULLY_QUALIFIED
		| JavaElementLabels.M_PRE_RETURNTYPE | JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.M_PARAMETER_NAMES | JavaElementLabels.M_EXCEPTIONS 
		| JavaElementLabels.F_PRE_TYPE_SIGNATURE;


	/*
	 * @see JavaElementHover
	 */
	protected String getHoverInfo(IJavaElement[] result) {
		StringBuffer buffer= new StringBuffer();
		int nResults= result.length;
		if (nResults == 0)
			return null;
		
		if (nResults > 1) {
			
			for (int i= 0; i < result.length; i++) {
				HTMLPrinter.startBulletList(buffer);
				IJavaElement curr= result[i];
				if (curr instanceof IMember || curr.getElementType() == IJavaElement.LOCAL_VARIABLE)
					HTMLPrinter.addBullet(buffer, getInfoText(curr));
				HTMLPrinter.endBulletList(buffer);
			}
			
		} else {
			
			IJavaElement curr= result[0];
			if (curr instanceof IMember) {
				IMember member= (IMember) curr;
				HTMLPrinter.addSmallHeader(buffer, getInfoText(member));
				Reader reader;
				try {
					reader= JavadocContentAccess.getContentReader(member, true);
				} catch (JavaModelException ex) {
					return null;
				}
				if (reader != null) {
					HTMLPrinter.addParagraph(buffer, new JavaDoc2HTMLTextReader(reader));
				}
			} else if (curr.getElementType() == IJavaElement.LOCAL_VARIABLE)
				HTMLPrinter.addSmallHeader(buffer, getInfoText(curr));
		}
		
		if (buffer.length() > 0) {
			HTMLPrinter.insertPageProlog(buffer, 0);
			HTMLPrinter.addPageEpilog(buffer);
			return buffer.toString();
		}
		
		return null;
	}

	private String getInfoText(IJavaElement member) {
		String label= JavaElementLabels.getElementLabel(member, LABEL_FLAGS);
		StringBuffer buf= new StringBuffer();
		for (int i= 0; i < label.length(); i++) {
			char ch= label.charAt(i);
			if (ch == '<') {
				buf.append("&lt;"); //$NON-NLS-1$
			} else if (ch == '>') {
				buf.append("&gt;"); //$NON-NLS-1$
			} else {
				buf.append(ch);
			}
		}
		return buf.toString();
	}
}
