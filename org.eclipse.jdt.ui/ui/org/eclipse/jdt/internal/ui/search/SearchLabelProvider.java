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
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.jdt.core.search.IJavaSearchResultCollector;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.search.internal.ui.SearchPlugin;
import org.eclipse.search.internal.ui.SearchPreferencePage;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.Match;
import org.eclipse.swt.graphics.Color;

public abstract class SearchLabelProvider extends LabelProvider implements IColorProvider, IPropertyChangeListener {

	private Color fPotentialMatchFgColor;
	
	protected JavaSearchResultPage fPage;
	private ILabelProvider fLabelProvider;

	public SearchLabelProvider(JavaSearchResultPage page, ILabelProvider inner) {
		fPage= page;
		fLabelProvider= inner;
		SearchPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(this);
	}
	

	public void propertyChange(PropertyChangeEvent event) {
		if (fPotentialMatchFgColor == null)
			return;
		if (SearchPreferencePage.POTENTIAL_MATCH_FG_COLOR.equals(event.getProperty()) || SearchPreferencePage.EMPHASIZE_POTENTIAL_MATCHES.equals(event.getProperty())) {
			fPotentialMatchFgColor.dispose();
			fPotentialMatchFgColor= null;
			LabelProviderChangedEvent lpEvent= new LabelProviderChangedEvent(SearchLabelProvider.this, null); // refresh all
			fireLabelProviderChanged(lpEvent);
		}
	}

	public ILabelProvider getLabelProvider() {
		return fLabelProvider;
	}

	public Color getForeground(Object element) {
		if (SearchPreferencePage.arePotentialMatchesEmphasized()) {
			if (hasPotentialMatches(element))
				return getForegroundColor();
		}
		if (fLabelProvider instanceof IColorProvider)
			return ((IColorProvider)fLabelProvider).getForeground(element);
		return null;
	}

	private boolean hasPotentialMatches(Object element) {
		AbstractTextSearchResult result= fPage.getInput();
		if (result != null) {
			Match[] matches= result.getMatches(element);
			for (int i = 0; i < matches.length; i++) {
				if ((matches[i]) instanceof JavaElementMatch) {
					if (((JavaElementMatch)matches[i]).getAccuracy() == IJavaSearchResultCollector.POTENTIAL_MATCH)
						return true;
				}
			}
		}
		return false;
	}

	public Color getBackground(Object element) {
		if (fLabelProvider instanceof IColorProvider)
			return ((IColorProvider)fLabelProvider).getBackground(element);
		return null;
	}

	private Color getForegroundColor() {
		if (fPotentialMatchFgColor == null) {
			fPotentialMatchFgColor= new Color(JavaPlugin.getActiveWorkbenchShell().getDisplay(), SearchPreferencePage.getPotentialMatchForegroundColor());
		}
		return fPotentialMatchFgColor;
	}
	
	public void dispose() {
		if (fPotentialMatchFgColor != null) {
			fPotentialMatchFgColor.dispose();
		}
		SearchPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(this);
		fLabelProvider.dispose();
		super.dispose();
	}
	
	public void addListener(ILabelProviderListener listener) {
		super.addListener(listener);
		getLabelProvider().addListener(listener);
	}

	public boolean isLabelProperty(Object element, String property) {
		return getLabelProvider().isLabelProperty(element, property);
	}

	public void removeListener(ILabelProviderListener listener) {
		super.removeListener(listener);
		getLabelProvider().removeListener(listener);
	}
}
