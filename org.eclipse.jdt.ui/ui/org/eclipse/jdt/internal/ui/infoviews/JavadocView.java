/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.infoviews;

import java.io.Reader;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.TextPresentation;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.javadoc.JavaDocAccess;

import org.eclipse.jdt.internal.ui.text.HTMLPrinter;
import org.eclipse.jdt.internal.ui.text.HTMLTextPresenter;
import org.eclipse.jdt.internal.ui.text.javadoc.JavaDoc2HTMLTextReader;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;

/**
 * View which shows Javadoc for a given Java element.
 * 
 * @since 3.0
 */
public class JavadocView extends AbstractInfoView {

	private static final int LABEL_FLAGS=  JavaElementLabels.ALL_FULLY_QUALIFIED
		| JavaElementLabels.M_PRE_RETURNTYPE | JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.M_PARAMETER_NAMES | JavaElementLabels.M_EXCEPTIONS 
		| JavaElementLabels.F_PRE_TYPE_SIGNATURE;

	private StyledText fText;
	private DefaultInformationControl.IInformationPresenter fPresenter;
	private TextPresentation fPresentation= new TextPresentation();

	protected void internalCreatePartControl(Composite parent) {
		fText= new StyledText(parent, SWT.V_SCROLL | SWT.H_SCROLL);
		fText.setEditable(false);
		
		fPresenter= new HTMLTextPresenter(false);
	}

	protected void setForeground(Color color) {
		fText.setForeground(color);
	}

	protected void setBackground(Color color) {
		fText.setBackground(color);
	}

	/*
	 * @see IWorkbenchPart#dispose()
	 */
	protected void internalDispose() {
		fText= null;
	}

	/*
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
	public void setFocus() {
		fText.setFocus();
	}
	
	protected boolean setInput(Object input) {
		if (fText == null || ! (input instanceof IJavaElement))
			return false;

		IJavaElement je= (IJavaElement)input;
		String javadocHtml= null;
		if (je.getElementType() == IJavaElement.COMPILATION_UNIT) {
			try {
				javadocHtml= getJavadocHtml(((ICompilationUnit)je).getTypes());
			} catch (JavaModelException ex) {
				return false;
			}
		} else
			javadocHtml= getJavadocHtml(new IJavaElement[] { je });

		fPresentation.clear();
		Point size= fText.getSize();
		try {
			javadocHtml= fPresenter.updatePresentation(getSite().getShell().getDisplay(), javadocHtml, fPresentation, size.x, size.y);
		} catch (IllegalArgumentException ex) {
			// the javadoc might no longer be valid
			return false;
		}

		if (javadocHtml == null)
			return false;

		fText.setText(javadocHtml);
		TextPresentation.applyTextPresentation(fPresentation, fText);
		
		return true;
	}

	private String getJavadocHtml(IJavaElement[] result) {
		StringBuffer buffer= new StringBuffer();
		int nResults= result.length;
		
		if (nResults > 1) {
			
			for (int i= 0; i < result.length; i++) {
				HTMLPrinter.startBulletList(buffer);
				IJavaElement curr= result[i];
				if (curr instanceof IMember)
					HTMLPrinter.addBullet(buffer, getInfoText((IMember) curr));
				HTMLPrinter.endBulletList(buffer);
			}
			
		} else {
			
			IJavaElement curr= result[0];
			if (curr instanceof IMember) {
				IMember member= (IMember) curr;
//				HTMLPrinter.addSmallHeader(buffer, getInfoText(member));
				Reader reader;
				try {
					reader= JavaDocAccess.getJavaDoc(member, true);
				} catch (JavaModelException ex) {
					return null;
				}
				if (reader != null) {
					HTMLPrinter.addParagraph(buffer, new JavaDoc2HTMLTextReader(reader));
				}
			}
		}
		
		if (buffer.length() > 0) {
			HTMLPrinter.insertPageProlog(buffer, 0);
			HTMLPrinter.addPageEpilog(buffer);
			return buffer.toString();
		}
		
		return null;
	}

	private String getInfoText(IMember member) {
		return JavaElementLabels.getElementLabel(member, LABEL_FLAGS);
	}
}
