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

import java.io.Reader;
import java.io.StringReader;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.internal.text.html.BrowserInformationControl;
import org.eclipse.jface.internal.text.html.HTMLPrinter;
import org.eclipse.jface.internal.text.html.HTMLTextPresenter;

import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.ITextHoverExtension;
import org.eclipse.jface.text.information.IInformationProviderExtension2;

import org.eclipse.ui.editors.text.EditorsUI;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.javadoc.JavaDocLocations;

import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.JavadocContentAccess;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.IInformationControlExtension4;

/**
 * Provides Javadoc as hover info for Java elements.
 *
 * @since 2.1
 */
public class JavadocHover extends AbstractJavaEditorTextHover implements IInformationProviderExtension2, ITextHoverExtension {

	
	/**
	 * Presenter control creator.
	 * 
	 * @since 3.3
	 */
	private static final class PresenterControlCreator extends AbstractReusableInformationControlCreator {
		/*
		 * @see org.eclipse.jdt.internal.ui.text.java.hover.AbstractReusableInformationControlCreator#doCreateInformationControl(org.eclipse.swt.widgets.Shell)
		 */
		public IInformationControl doCreateInformationControl(Shell parent) {
			int shellStyle= SWT.RESIZE | SWT.TOOL;
			int style= SWT.V_SCROLL | SWT.H_SCROLL;
			if (BrowserInformationControl.isAvailable(parent))
				return new BrowserInformationControl(parent, shellStyle, style);
			else
				return new DefaultInformationControl(parent, shellStyle, style, new HTMLTextPresenter(false));
		}
	}

	
	/**
	 * Hover control creator.
	 * 
	 * @since 3.3
	 */
	private static final class HoverControlCreator extends AbstractReusableInformationControlCreator {
		/*
		 * @see org.eclipse.jdt.internal.ui.text.java.hover.AbstractReusableInformationControlCreator#doCreateInformationControl(org.eclipse.swt.widgets.Shell)
		 */
		public IInformationControl doCreateInformationControl(Shell parent) {
			if (BrowserInformationControl.isAvailable(parent))
				return new BrowserInformationControl(parent, SWT.TOOL | SWT.NO_TRIM, SWT.NONE, EditorsUI.getTooltipAffordanceString());
			else
				return new DefaultInformationControl(parent, SWT.NONE, new HTMLTextPresenter(true), EditorsUI.getTooltipAffordanceString());
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.text.java.hover.AbstractReusableInformationControlCreator#canReuse(org.eclipse.jface.text.IInformationControl)
		 */
		public boolean canReuse(IInformationControl control) {
			boolean canReuse= super.canReuse(control);
			if (canReuse && control instanceof IInformationControlExtension4)
				((IInformationControlExtension4)control).setStatusText(EditorsUI.getTooltipAffordanceString());
			return canReuse;
				
		}
	}

	private final long LABEL_FLAGS=  JavaElementLabels.ALL_FULLY_QUALIFIED
		| JavaElementLabels.M_PRE_RETURNTYPE | JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.M_PARAMETER_NAMES | JavaElementLabels.M_EXCEPTIONS
		| JavaElementLabels.F_PRE_TYPE_SIGNATURE | JavaElementLabels.M_PRE_TYPE_PARAMETERS | JavaElementLabels.T_TYPE_PARAMETERS
		| JavaElementLabels.USE_RESOLVED;
	private final long LOCAL_VARIABLE_FLAGS= LABEL_FLAGS & ~JavaElementLabels.F_FULLY_QUALIFIED | JavaElementLabels.F_POST_QUALIFIED;

	
	/**
	 * The hover control creator.
	 * 
	 * @since 3.2
	 */
	private IInformationControlCreator fHoverControlCreator;
	/**
	 * The presentation control creator.
	 * 
	 * @since 3.2
	 */
	private IInformationControlCreator fPresenterControlCreator;


	/*
	 * @see IInformationProviderExtension2#getInformationPresenterControlCreator()
	 * @since 3.1
	 */
	public IInformationControlCreator getInformationPresenterControlCreator() {
		if (fPresenterControlCreator == null)
			fPresenterControlCreator= new PresenterControlCreator();
		return fPresenterControlCreator;
	}

	/*
	 * @see ITextHoverExtension#getHoverControlCreator()
	 * @since 3.2
	 */
	public IInformationControlCreator getHoverControlCreator() {
		if (fHoverControlCreator == null)
			fHoverControlCreator= new HoverControlCreator();
		return fHoverControlCreator;
	}

	/*
	 * @see JavaElementHover
	 */
	protected String getHoverInfo(IJavaElement[] result) {

		StringBuffer buffer= new StringBuffer();
		int nResults= result.length;
		if (nResults == 0)
			return null;

		boolean hasContents= false;
		if (nResults > 1) {

			for (int i= 0; i < result.length; i++) {
				HTMLPrinter.startBulletList(buffer);
				IJavaElement curr= result[i];
				if (curr instanceof IMember || curr.getElementType() == IJavaElement.LOCAL_VARIABLE) {
					HTMLPrinter.addBullet(buffer, getInfoText(curr));
					hasContents= true;
				}
				HTMLPrinter.endBulletList(buffer);
			}

		} else {

			IJavaElement curr= result[0];
			if (curr instanceof IMember) {
				IMember member= (IMember) curr;
				HTMLPrinter.addSmallHeader(buffer, getInfoText(member));
				Reader reader;
				try {
					reader= JavadocContentAccess.getHTMLContentReader(member, true, true);
					
					// Provide hint why there's no Javadoc
					if (reader == null && member.isBinary()) {
						boolean hasAttachedJavadoc= JavaDocLocations.getJavadocBaseLocation(member) != null;
						IPackageFragmentRoot root= (IPackageFragmentRoot)member.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
						boolean hasAttachedSource= root != null && root.getSourceAttachmentPath() != null;
						IOpenable openable= member.getOpenable();
						boolean hasSource= openable.getBuffer() != null;

						if (!hasAttachedSource && !hasAttachedJavadoc)
							reader= new StringReader(JavaHoverMessages.JavadocHover_noAttachments);
						else if (!hasAttachedJavadoc && !hasSource)
							reader= new StringReader(JavaHoverMessages.JavadocHover_noAttachedJavadoc);
						else if (!hasAttachedSource)
							reader= new StringReader(JavaHoverMessages.JavadocHover_noAttachedSource);
						else if (!hasSource)
							reader= new StringReader(JavaHoverMessages.JavadocHover_noInformation);
					}
					
				} catch (JavaModelException ex) {
					reader= new StringReader(JavaHoverMessages.JavadocHover_error_gettingJavadoc);
					JavaPlugin.log(ex.getStatus());
				}
				
				if (reader != null) {
					HTMLPrinter.addParagraph(buffer, reader);
				}
				hasContents= true;
			} else if (curr.getElementType() == IJavaElement.LOCAL_VARIABLE || curr.getElementType() == IJavaElement.TYPE_PARAMETER) {
				HTMLPrinter.addSmallHeader(buffer, getInfoText(curr));
				hasContents= true;
			}
		}
		
		if (!hasContents)
			return null;

		if (buffer.length() > 0) {
			HTMLPrinter.insertPageProlog(buffer, 0, getStyleSheet());
			HTMLPrinter.addPageEpilog(buffer);
			return buffer.toString();
		}

		return null;
	}

	private String getInfoText(IJavaElement member) {
		long flags= member.getElementType() == IJavaElement.LOCAL_VARIABLE ? LOCAL_VARIABLE_FLAGS : LABEL_FLAGS;
		String label= JavaElementLabels.getElementLabel(member, flags);
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
