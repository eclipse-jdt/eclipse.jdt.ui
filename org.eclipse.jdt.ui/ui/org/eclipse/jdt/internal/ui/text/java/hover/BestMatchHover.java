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
package org.eclipse.jdt.internal.ui.text.java.hover;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextHoverExtension;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.information.IInformationProviderExtension2;

import org.eclipse.ui.IEditorPart;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.java.hover.IJavaEditorTextHover;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Caution: this implementation is a layer breaker and contains some "shortcuts"
 */
public class BestMatchHover extends AbstractJavaEditorTextHover implements ITextHoverExtension, IInformationProviderExtension2 {

	private static class JavaEditorTextHoverDescriptorComparator implements Comparator {
		
		/*
		 * @see Comparator#compare(Object, Object)
		 */
		public int compare(Object object0, Object object1) {

			JavaEditorTextHoverDescriptor element0= (JavaEditorTextHoverDescriptor)object0;
			JavaEditorTextHoverDescriptor element1= (JavaEditorTextHoverDescriptor)object1;	

			String id0=	element0.getId();
			String id1= element1.getId();
			
			if (id0 != null && id0.equals(id1))
				return 0;
			
			if (id0 != null && JavaProblemHover.isJavaProblemHover(id0))
				return -1;

			if (id1 != null && JavaProblemHover.isJavaProblemHover(id1))
				return +1;


			// now compare non-problem hovers
			if (element0.dependsOn(element1))
				return -1;

			if (element1.dependsOn(element0))
				return +1;
			
			return 0;
		}
	}
	
	private List fTextHoverSpecifications;
	private List fInstantiatedTextHovers;
	private ITextHover fBestHover;

	public BestMatchHover() {
		installTextHovers();
	}

	public BestMatchHover(IEditorPart editor) {
		this();
		setEditor(editor);
	}
	
	/**
	 * Installs all text hovers.
	 */
	private void installTextHovers() {
		
		// initialize lists - indicates that the initialization happened
		fTextHoverSpecifications= new ArrayList(2);
		fInstantiatedTextHovers= new ArrayList(2);

		// populate list
		JavaEditorTextHoverDescriptor[] hoverDescs= JavaPlugin.getDefault().getJavaEditorTextHoverDescriptors();
		for (int i= 0; i < hoverDescs.length; i++) {
			// ensure that we don't add ourselves to the list
			if (!PreferenceConstants.ID_BESTMATCH_HOVER.equals(hoverDescs[i].getId()))
				fTextHoverSpecifications.add(hoverDescs[i]);
		}
		Collections.sort(fTextHoverSpecifications, new JavaEditorTextHoverDescriptorComparator());
	}	

	private void checkTextHovers() {
		if (fTextHoverSpecifications.size() == 0)
			return;

		for (Iterator iterator= new ArrayList(fTextHoverSpecifications).iterator(); iterator.hasNext(); ) {
			JavaEditorTextHoverDescriptor spec= (JavaEditorTextHoverDescriptor) iterator.next();

			IJavaEditorTextHover hover= spec.createTextHover();
			if (hover != null) {
				hover.setEditor(getEditor());
				addTextHover(hover);
				fTextHoverSpecifications.remove(spec);
			}
		}
	}

	protected void addTextHover(ITextHover hover) {
		if (!fInstantiatedTextHovers.contains(hover))
			fInstantiatedTextHovers.add(hover);
	}

	/*
	 * @see ITextHover#getHoverInfo(ITextViewer, IRegion)
	 */
	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {

		checkTextHovers();
		fBestHover= null;

		if (fInstantiatedTextHovers == null)
			return null;

		for (Iterator iterator= fInstantiatedTextHovers.iterator(); iterator.hasNext(); ) {
			ITextHover hover= (ITextHover)iterator.next();

			String s= hover.getHoverInfo(textViewer, hoverRegion);
			if (s != null && s.trim().length() > 0) {
				fBestHover= hover;
				return s;
			}
		}

		return null;
	}

	/*
	 * @see org.eclipse.jface.text.ITextHoverExtension#getInformationControlCreator()
	 * @since 3.0
	 */
	public IInformationControlCreator getInformationControlCreator() {
		if (fBestHover instanceof ITextHoverExtension)
			return ((ITextHoverExtension)fBestHover).getInformationControlCreator();

		return null;
	}

	/*
	 * @see org.eclipse.jface.text.information.IInformationProviderExtension2#getInformationPresenterControlCreator()
	 * @since 3.0
	 */
	public IInformationControlCreator getInformationPresenterControlCreator() {
		if (fBestHover instanceof IInformationProviderExtension2)
			return ((IInformationProviderExtension2)fBestHover).getInformationPresenterControlCreator();

		return null;
	}
}
