/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.text.java.hover;

import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.information.IInformationProvider;
import org.eclipse.jface.text.information.IInformationProviderExtension;
import org.eclipse.jface.text.information.IInformationProviderExtension2;

import org.eclipse.ui.IEditorPart;

import org.eclipse.jdt.internal.ui.text.JavaWordFinder;


public class JavaInformationProvider implements IInformationProvider, IInformationProviderExtension, IInformationProviderExtension2 {

	protected BestMatchHover fImplementation;

	public JavaInformationProvider(IEditorPart editor) {
		if (editor != null) {
			fImplementation= new BestMatchHover();
			fImplementation.setEditor(editor);
		}
	}

	/*
	 * @see IInformationProvider#getSubject(ITextViewer, int)
	 */
	@Override
	public IRegion getSubject(ITextViewer textViewer, int offset) {

		if (textViewer != null)
			return JavaWordFinder.findWord(textViewer.getDocument(), offset);

		return null;
	}

	/**
	 * @see IInformationProvider#getInformation(ITextViewer, IRegion)
	 * @deprecated use {@link IInformationProviderExtension#getInformation2(ITextViewer, IRegion)}
	 */
	@Deprecated
	@Override
	public String getInformation(ITextViewer textViewer, IRegion subject) {
		if (fImplementation != null) {
			String s= fImplementation.getHoverInfo(textViewer, subject);
			if (s != null && s.trim().length() > 0) {
				return s;
			}
		}
		return null;
	}

	/*
	 * @see org.eclipse.jface.text.information.IInformationProviderExtension#getInformation2(org.eclipse.jface.text.ITextViewer, org.eclipse.jface.text.IRegion)
	 */
	@Override
	public Object getInformation2(ITextViewer textViewer, IRegion subject) {
		if (fImplementation == null)
			return null;
		return fImplementation.getHoverInfo2(textViewer, subject, true);
	}

	/*
	 * @see IInformationProviderExtension2#getInformationPresenterControlCreator()
	 * @since 3.1
	 */
	@Override
	public IInformationControlCreator getInformationPresenterControlCreator() {
		if (fImplementation == null)
			return null;
		return fImplementation.getInformationPresenterControlCreator();
	}
}
