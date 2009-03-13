/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.java.hover;

import java.util.Properties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.Assert;

import org.eclipse.core.resources.IStorage;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.internal.text.html.HTMLPrinter;

import org.eclipse.jface.text.AbstractReusableInformationControlCreator;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IInformationControlExtension2;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;

import org.eclipse.ui.IEditorPart;

import org.eclipse.ui.editors.text.EditorsUI;

import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StringLiteral;

import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.refactoring.nls.AccessorClassReference;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSHintHelper;

import org.eclipse.jdt.ui.SharedASTProvider;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.javaeditor.NLSKeyHyperlink;


/**
 * Provides externalized string as hover info for NLS key.
 *
 * @since 3.1
 */
public class NLSStringHover extends AbstractJavaEditorTextHover {


	/*
	 * @see org.eclipse.jdt.internal.ui.text.java.hover.AbstractJavaEditorTextHover#getHoverRegion(org.eclipse.jface.text.ITextViewer, int)
	 */
	public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
		if (!(getEditor() instanceof JavaEditor))
			return null;

		ITypeRoot je= getEditorInputJavaElement();
		if (je == null)
			return null;

		// Never wait for an AST in UI thread.
		CompilationUnit ast= SharedASTProvider.getAST(je, SharedASTProvider.WAIT_NO, null);
		if (ast == null)
			return null;

		ASTNode node= NodeFinder.perform(ast, offset, 1);
		if (node instanceof StringLiteral) {
			StringLiteral stringLiteral= (StringLiteral)node;
			return new Region(stringLiteral.getStartPosition(), stringLiteral.getLength());
		} else if (node instanceof SimpleName) {
			SimpleName simpleName= (SimpleName)node;
			return new Region(simpleName.getStartPosition(), simpleName.getLength());
		}

		return null;
	}

	/**
	 * @deprecated see {@link org.eclipse.jface.text.ITextHover#getHoverInfo(ITextViewer, IRegion)}
	 */
	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
		NLSHoverControlInput info= internalGetHoverInfo(textViewer, hoverRegion);
		return info == null ? null : info.fInformation;
	}

	/*
	 * @see org.eclipse.jface.text.ITextHoverExtension2#getHoverInfo2(org.eclipse.jface.text.ITextViewer, org.eclipse.jface.text.IRegion)
	 */
	public Object getHoverInfo2(ITextViewer textViewer, IRegion hoverRegion) {
		return internalGetHoverInfo(textViewer, hoverRegion);
	}

	/**
	 * Returns the hover input.
	 * 
	 * @param textViewer the viewer on which the hover popup should be shown
	 * @param hoverRegion the text range in the viewer which is used to determine the hover display
	 *            information
	 * @return the hover popup display input, or <code>null</code> if none available
	 * 
	 * @see #getHoverInfo2(ITextViewer, IRegion)
	 */
	private NLSHoverControlInput internalGetHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
		if (!(getEditor() instanceof JavaEditor))
			return null;

		ITypeRoot je= getEditorInputJavaElement();
		if (je == null)
			return null;

		CompilationUnit ast= SharedASTProvider.getAST(je, SharedASTProvider.WAIT_ACTIVE_ONLY, null);
		if (ast == null)
			return null;

		ASTNode node= NodeFinder.perform(ast, hoverRegion.getOffset(), hoverRegion.getLength());
		if (!(node instanceof StringLiteral) && !(node instanceof SimpleName))
			return null;

		if (node.getLocationInParent() == QualifiedName.QUALIFIER_PROPERTY)
			return null;

		AccessorClassReference ref= NLSHintHelper.getAccessorClassReference(ast, hoverRegion);
		if (ref == null)
			return null;

		IStorage propertiesFile;
		try {
			propertiesFile= NLSHintHelper.getResourceBundle(je.getJavaProject(), ref);
			if (propertiesFile == null)
				return new NLSHoverControlInput(toHtml(JavaHoverMessages.NLSStringHover_NLSStringHover_PropertiesFileNotDetectedWarning, ""), (IStorage)null, "", getEditor()); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (JavaModelException ex) {
			return null;
		}

		final String propertiesFileName= propertiesFile.getName();
		Properties properties= NLSHintHelper.getProperties(propertiesFile);
		if (properties == null)
			return null;
		if (properties.isEmpty())
			return new NLSHoverControlInput(toHtml(propertiesFileName, JavaHoverMessages.NLSStringHover_NLSStringHover_missingKeyWarning), propertiesFile, "", getEditor()); //$NON-NLS-1$

		String identifier= null;
		if (node instanceof StringLiteral) {
			identifier= ((StringLiteral)node).getLiteralValue();
		} else {
			identifier= ((SimpleName)node).getIdentifier();
		}
		if (identifier == null)
			return null;

		String value= properties.getProperty(identifier, null);
		if (value != null)
			value= HTMLPrinter.convertToHTMLContent(value);
		else
			value= JavaHoverMessages.NLSStringHover_NLSStringHover_missingKeyWarning;

		String buffer= toHtml(propertiesFileName, value);
		return new NLSHoverControlInput(buffer, propertiesFile, identifier, getEditor());
	}

	private String toHtml(String header, String string) {

		StringBuffer buffer= new StringBuffer();

		HTMLPrinter.addSmallHeader(buffer, header);
		HTMLPrinter.addParagraph(buffer, string);
		HTMLPrinter.insertPageProlog(buffer, 0);
		HTMLPrinter.addPageEpilog(buffer);
		return buffer.toString();
	}
	
	/**
	 * The input for NLS hover.
	 * 
	 * @since 3.5
	 */
	private static class NLSHoverControlInput {

		private IStorage fpropertiesFile;
		private String fKeyName;
		private String fInformation;
		private IEditorPart fActiveEditor;

		/**
		 * Creates the NLS hover input.
		 * 
		 * @param information the hover info (string with simple HTML)
		 * @param propertiesFile the properties file, or <code>null</code> if not found
		 * @param key the NLS key
		 * @param editor the active editor part
		 */
		public NLSHoverControlInput(String information, IStorage propertiesFile, String key, IEditorPart editor) {
			fInformation= information;
			fpropertiesFile= propertiesFile;
			fKeyName= key;
			fActiveEditor= editor;
		}
	}
	
	/**
	 * The NLS hover control.
	 * 
	 * @since 3.5
	 */
	static class NLSHoverControl extends DefaultInformationControl implements IInformationControlExtension2 {

		/**
		 * The NLS control input.
		 */
		private NLSHoverControlInput fInput;

		/**
		 * Creates a resizable NLS hover control with the given shell as parent.
		 * 
		 * @param parent the parent shell
		 * @param tbm the toolbar manager or <code>null</code> if toolbar is not desired
		 */
		public NLSHoverControl(Shell parent, ToolBarManager tbm) {
			super(parent, tbm);

		}

		/**
		 * Creates an NLS hover control with the given shell as parent.
		 * 
		 * @param parent the parent shell
		 * @param tooltipAffordanceString the text to be used in the status field or
		 *            <code>null</code> to hide the status field
		 */
		public NLSHoverControl(Shell parent, String tooltipAffordanceString) {
			super(parent, tooltipAffordanceString);
		}

		/**
		 * {@inheritDoc} This control can handle {@link NLSStringHover.NLSHoverControlInput}.
		 */
		public void setInput(Object input) {
			Assert.isLegal(input instanceof NLSHoverControlInput);
			
			NLSHoverControlInput info= (NLSHoverControlInput)input;
			setInformation(info.fInformation);
			fInput= info;
		}

		/**
		 * Returns the control input.
		 * 
		 * @return the control input
		 */
		public NLSHoverControlInput getInput() {
			return fInput;
		}
	}

	/**
	 * Presenter control creator.
	 * 
	 * @since 3.5
	 */
	private static final class PresenterControlCreator extends AbstractReusableInformationControlCreator {
		/*
		 * @see org.eclipse.jdt.internal.ui.text.java.hover.AbstractReusableInformationControlCreator#doCreateInformationControl(org.eclipse.swt.widgets.Shell)
		 */
		public IInformationControl doCreateInformationControl(Shell parent) {
			ToolBarManager tbm= new ToolBarManager(SWT.FLAT);
			NLSHoverControl iControl= new NLSHoverControl(parent, tbm);
			OpenPropertiesFileAction openPropertiesFileAction= new OpenPropertiesFileAction(iControl);
			tbm.add(openPropertiesFileAction);
			tbm.update(true);
			return iControl;
		}
	}

	/**
	 * Hover control creator.
	 * 
	 * @since 3.5
	 */
	private static final class HoverControlCreator extends AbstractReusableInformationControlCreator {

		/**
		 * The presenter control creator.
		 */
		private final IInformationControlCreator fPresenterControlCreator;

		/**
		 * Creates the hover control creator.
		 * 
		 * @param presenterControlCreator the presenter control creator
		 */
		public HoverControlCreator(IInformationControlCreator presenterControlCreator) {
			fPresenterControlCreator= presenterControlCreator;
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.text.java.hover.AbstractReusableInformationControlCreator#doCreateInformationControl(org.eclipse.swt.widgets.Shell)
		 */
		public IInformationControl doCreateInformationControl(Shell parent) {
			return new NLSHoverControl(parent, EditorsUI.getTooltipAffordanceString()) {
				/*
				 * @see org.eclipse.jface.text.IInformationControlExtension5#getInformationPresenterControlCreator()
				 */
				public IInformationControlCreator getInformationPresenterControlCreator() {
					return fPresenterControlCreator;
				}
			};
		}
	}

	/**
	 * The hover control creator.
	 * 
	 * @since 3.5
	 */
	private IInformationControlCreator fHoverControlCreator;

	/**
	 * The presentation control creator.
	 * 
	 * @since 3.5
	 */
	private IInformationControlCreator fPresenterControlCreator;

	/*
	 * @see ITextHoverExtension#getHoverControlCreator()
	 * @since 3.5
	 */
	public IInformationControlCreator getHoverControlCreator() {
		if (fHoverControlCreator == null)
			fHoverControlCreator= new HoverControlCreator(getInformationPresenterControlCreator());
		return fHoverControlCreator;
	}

	/*
	 * @see org.eclipse.jface.text.ITextHoverExtension2#getInformationPresenterControlCreator()
	 * @since 3.5
	 */
	public IInformationControlCreator getInformationPresenterControlCreator() {
		if (fPresenterControlCreator == null)
			fPresenterControlCreator= new PresenterControlCreator();
		return fPresenterControlCreator;
	}


	/**
	 * Action that opens the current hover NLS string in properties file.
	 * 
	 * @since 3.5
	 */
	private static final class OpenPropertiesFileAction extends Action {
		
		/**
		 * The NLS hover control.
		 */
		private NLSHoverControl fControl;

		/**
		 * Creates the action for opening properties file.
		 * 
		 * @param control the NLS hover control
		 */
		public OpenPropertiesFileAction(NLSHoverControl control) {
			fControl= control;
			setText(JavaHoverMessages.NLSStringHover_open_in_properties_file);
			JavaPluginImages.setLocalImageDescriptors(this, "goto_input.gif"); //$NON-NLS-1$
		}

		/*
		 * @see org.eclipse.jface.action.Action#run()
		 */
		public void run() {
			NLSHoverControlInput input= fControl.getInput();
			NLSKeyHyperlink.openKeyInPropertiesFile(input.fKeyName, input.fpropertiesFile, input.fActiveEditor);
		}
	}
}
