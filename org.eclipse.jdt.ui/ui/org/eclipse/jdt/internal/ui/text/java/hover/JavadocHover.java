/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Genady Beryozkin <eclipse@genady.org> - [hovering] tooltip for constant string does not show constant value - https://bugs.eclipse.org/bugs/show_bug.cgi?id=85382
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.java.hover;

import java.io.Reader;
import java.io.StringReader;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.internal.text.html.BrowserInformationControl;
import org.eclipse.jface.internal.text.html.HTMLPrinter;
import org.eclipse.jface.internal.text.html.HTMLTextPresenter;

import org.eclipse.jface.text.AbstractReusableInformationControlCreator;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IInformationControlExtension4;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHoverExtension;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.information.IInformationProviderExtension2;

import org.eclipse.ui.editors.text.EditorsUI;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.javadoc.JavaDocLocations;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.JavadocContentAccess;
import org.eclipse.jdt.ui.SharedASTProvider;

import org.eclipse.jdt.internal.ui.JavaPlugin;


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
			if (!super.canReuse(control))
				return false;
			
			if (control instanceof IInformationControlExtension4)
				((IInformationControlExtension4)control).setStatusText(EditorsUI.getTooltipAffordanceString());
			
			return true;
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
	
	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
		IJavaElement[] result= getJavaElementsAt(textViewer, hoverRegion);
		if (result == null || result.length == 0)
			return null;

		int nResults= result.length;
		StringBuffer buffer= new StringBuffer();
		boolean hasContents= false;
		if (nResults > 1) {

			for (int i= 0; i < result.length; i++) {
				HTMLPrinter.startBulletList(buffer);
				IJavaElement curr= result[i];
				if (curr instanceof IMember || curr.getElementType() == IJavaElement.LOCAL_VARIABLE) {
					HTMLPrinter.addBullet(buffer, getInfoText(curr, hoverRegion));
					hasContents= true;
				}
				HTMLPrinter.endBulletList(buffer);
			}

		} else {

			IJavaElement curr= result[0];
			if (curr instanceof IMember) {
				IMember member= (IMember) curr;
				HTMLPrinter.addSmallHeader(buffer, getInfoText(member, hoverRegion));
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
					JavaPlugin.log(ex);
				}
				
				if (reader != null) {
					HTMLPrinter.addParagraph(buffer, reader);
				}
				hasContents= true;
			} else if (curr.getElementType() == IJavaElement.LOCAL_VARIABLE || curr.getElementType() == IJavaElement.TYPE_PARAMETER) {
				HTMLPrinter.addSmallHeader(buffer, getInfoText(curr, hoverRegion));
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

	private String getInfoText(IJavaElement member, IRegion hoverRegion) {
		long flags= member.getElementType() == IJavaElement.LOCAL_VARIABLE ? LOCAL_VARIABLE_FLAGS : LABEL_FLAGS;
		StringBuffer label= new StringBuffer(JavaElementLabels.getElementLabel(member, flags));
		if (member.getElementType() == IJavaElement.FIELD) {
			String constantValue= getConstantValue((IField)member, hoverRegion);
			if (constantValue != null) {
				IJavaProject javaProject= member.getJavaProject();
				if (JavaCore.INSERT.equals(javaProject.getOption(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_ASSIGNMENT_OPERATOR, true)))
					label.append(' ');
				label.append('=');
				if (JavaCore.INSERT.equals(javaProject.getOption(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_ASSIGNMENT_OPERATOR, true)))
					label.append(' ');
				label.append(constantValue);
			}
		}
		
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

	/*
	 * @since 3.4
	 */
	private static boolean isStaticFinal(IJavaElement member) {
		if (member.getElementType() != IJavaElement.FIELD)
			return false;
		
		IField field= (IField)member;
		try {
			return JdtFlags.isFinal(field) && JdtFlags.isStatic(field);
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
			return false;
		}
	}

	/**
	 * Returns the constant value for the given field.
	 * 
	 * @param field the field
	 * @param hoverRegion the hover region
	 * @return the constant value for the given field or <code>null</code> if none
	 * @since 3.4
	 */
	private String getConstantValue(IField field, IRegion hoverRegion) {
		if (!isStaticFinal(field))
			return null;
		
		ITypeRoot typeRoot= getEditorInputJavaElement();
		if (typeRoot == null)
			return null;
		
		Object constantValue= null;
		
		CompilationUnit unit= SharedASTProvider.getAST(typeRoot, SharedASTProvider.WAIT_ACTIVE_ONLY, null);
		if (unit == null)
			return null;
		
		ASTNode node= NodeFinder.perform(unit, hoverRegion.getOffset(),	hoverRegion.getLength());
		if (node != null && node.getNodeType() == ASTNode.SIMPLE_NAME) {
			IBinding binding= ((SimpleName)node).resolveBinding();
			if (binding != null && binding.getKind() == IBinding.VARIABLE) {
				IVariableBinding variableBinding= (IVariableBinding)binding;
				if (field.equals(variableBinding.getJavaElement())) {
					constantValue= variableBinding.getConstantValue();
				}
			}
		}
		if (constantValue == null)
			return null;
		
		if (constantValue instanceof String) {
			StringBuffer result= new StringBuffer();
			result.append('"');
			String stringConstant= (String)constantValue;
			if (stringConstant.length() > 80) {
				result.append(stringConstant.substring(0, 80));
				result.append(JavaElementLabels.ELLIPSIS_STRING);
			} else {
				result.append(stringConstant);
			}
			result.append('"');
			return result.toString();
			
		} else if (constantValue instanceof Character) {
			String constantResult= '\'' + constantValue.toString() + '\'';
			
			char charValue= ((Character) constantValue).charValue();
			String hexString= Integer.toHexString(charValue);
			StringBuffer hexResult= new StringBuffer("\\u"); //$NON-NLS-1$
			for (int i= hexString.length(); i < 4; i++) {
				hexResult.append('0');
			}
			hexResult.append(hexString);
			return formatWithHexValue(constantResult, hexResult.toString());
			
		} else if (constantValue instanceof Byte) {
			int byteValue= ((Byte) constantValue).intValue() & 0xFF;
			return formatWithHexValue(constantValue, "0x" + Integer.toHexString(byteValue)); //$NON-NLS-1$
			
		} else if (constantValue instanceof Short) {
			int shortValue= ((Short) constantValue).shortValue() & 0xFFFF;
			return formatWithHexValue(constantValue, "0x" + Integer.toHexString(shortValue)); //$NON-NLS-1$
			
		} else if (constantValue instanceof Integer) {
			int intValue= ((Integer) constantValue).intValue();
			return formatWithHexValue(constantValue, "0x" + Integer.toHexString(intValue)); //$NON-NLS-1$
			
		} else if (constantValue instanceof Long) {
			long longValue= ((Long) constantValue).longValue();
			return formatWithHexValue(constantValue, "0x" + Long.toHexString(longValue)); //$NON-NLS-1$
			
		} else {
			return constantValue.toString();
		}
	}

	/**
	 * Creates and returns the a formatted message for the given
	 * constant with its hex value.
	 * 
	 * @param constantValue
	 * @param hexValue
	 * @return a formatted string with constant and hex values
	 * @since 3.4
	 */
	private static String formatWithHexValue(Object constantValue, String hexValue) {
		return Messages.format(JavaHoverMessages.JavadocHover_constantValue_hexValue, new String[] { constantValue.toString(), hexValue });
	}
}
