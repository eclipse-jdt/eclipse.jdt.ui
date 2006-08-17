/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.java.hover;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHoverExtension;
import org.eclipse.jface.text.ITextViewer;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.keys.IBindingService;

import org.eclipse.jdt.core.ICodeAssist;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;
import org.eclipse.jdt.ui.text.java.hover.IJavaEditorTextHover;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.IClassFileEditorInput;
import org.eclipse.jdt.internal.ui.javaeditor.WorkingCopyManager;
import org.eclipse.jdt.internal.ui.text.HTMLTextPresenter;
import org.eclipse.jdt.internal.ui.text.JavaWordFinder;

import org.osgi.framework.Bundle;

/**
 * Abstract class for providing hover information for Java elements.
 *
 * @since 2.1
 */
public abstract class AbstractJavaEditorTextHover implements IJavaEditorTextHover, ITextHoverExtension {

	/**
	 * The style sheet (css).
	 * @since 3.2
	 */
	private static String fgStyleSheet;
	private IEditorPart fEditor;
	private IBindingService fBindingService;
	{
		fBindingService= (IBindingService)PlatformUI.getWorkbench().getAdapter(IBindingService.class);
	}

	/*
	 * @see IJavaEditorTextHover#setEditor(IEditorPart)
	 */
	public void setEditor(IEditorPart editor) {
		fEditor= editor;
	}

	protected IEditorPart getEditor() {
		return fEditor;
	}

	protected ICodeAssist getCodeAssist() {
		if (fEditor != null) {
			IEditorInput input= fEditor.getEditorInput();
			if (input instanceof IClassFileEditorInput) {
				IClassFileEditorInput cfeInput= (IClassFileEditorInput) input;
				return cfeInput.getClassFile();
			}

			WorkingCopyManager manager= JavaPlugin.getDefault().getWorkingCopyManager();
			return manager.getWorkingCopy(input, false);
		}

		return null;
	}

	/*
	 * @see ITextHover#getHoverRegion(ITextViewer, int)
	 */
	public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
		return JavaWordFinder.findWord(textViewer.getDocument(), offset);
	}

	/*
	 * @see ITextHover#getHoverInfo(ITextViewer, IRegion)
	 */
	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
		
		/*
		 * The region should be a word region an not of length 0.
		 * This check is needed because codeSelect(...) also finds
		 * the Java element if the offset is behind the word.
		 */
		if (hoverRegion.getLength() == 0)
			return null;
		
		ICodeAssist resolve= getCodeAssist();
		if (resolve != null) {
			try {
				IJavaElement[] result= resolve.codeSelect(hoverRegion.getOffset(), hoverRegion.getLength());
				if (result == null)
					return null;

				int nResults= result.length;
				if (nResults == 0)
					return null;

				return getHoverInfo(result);

			} catch (JavaModelException x) {
				return null;
			}
		}
		return null;
	}

	/**
	 * Provides hover information for the given Java elements.
	 *
	 * @param javaElements the Java elements for which to provide hover information
	 * @return the hover information string
	 * @since 2.1
	 */
	protected String getHoverInfo(IJavaElement[] javaElements) {
		return null;
	}

	/*
	 * @see ITextHoverExtension#getHoverControlCreator()
	 * @since 3.0
	 */
	public IInformationControlCreator getHoverControlCreator() {
		return new IInformationControlCreator() {
			public IInformationControl createInformationControl(Shell parent) {
				return new DefaultInformationControl(parent, SWT.NONE, new HTMLTextPresenter(true), getTooltipAffordanceString());
			}
		};
	}

	/**
	 * Returns the tool tip affordance string.
	 *
	 * @return the affordance string or <code>null</code> if disabled or no key binding is defined
	 * @since 3.0
	 */
	protected String getTooltipAffordanceString() {
		if (fBindingService == null || !JavaPlugin.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.EDITOR_SHOW_TEXT_HOVER_AFFORDANCE))
			return null;

		String keySequence= fBindingService.getBestActiveBindingFormattedFor(IJavaEditorActionDefinitionIds.SHOW_JAVADOC);
		if (keySequence == null)
			return null;
		
		return Messages.format(JavaHoverMessages.JavaTextHover_makeStickyHint, keySequence == null ? "" : keySequence); //$NON-NLS-1$
	}

	/**
	 * Returns the style sheet.
	 *
	 * @since 3.2
	 */
	protected static String getStyleSheet() {
		if (fgStyleSheet == null) {
			Bundle bundle= Platform.getBundle(JavaPlugin.getPluginId());
			URL styleSheetURL= bundle.getEntry("/JavadocHoverStyleSheet.css"); //$NON-NLS-1$
			if (styleSheetURL != null) {
				try {
					styleSheetURL= FileLocator.toFileURL(styleSheetURL);
					BufferedReader reader= new BufferedReader(new InputStreamReader(styleSheetURL.openStream()));
					StringBuffer buffer= new StringBuffer(200);
					String line= reader.readLine();
					while (line != null) {
						buffer.append(line);
						buffer.append('\n');
						line= reader.readLine();
					}
					fgStyleSheet= buffer.toString();
				} catch (IOException ex) {
					JavaPlugin.log(ex);
					fgStyleSheet= ""; //$NON-NLS-1$
				}
			}
		}
		return fgStyleSheet;
	}
	
}
