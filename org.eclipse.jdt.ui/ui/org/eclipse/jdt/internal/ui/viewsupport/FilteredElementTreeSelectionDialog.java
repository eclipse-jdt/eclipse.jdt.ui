/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.viewsupport;

import java.util.ArrayList;
import java.util.StringTokenizer;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;

import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;

import org.eclipse.jdt.internal.corext.util.Strings;

import org.eclipse.jdt.internal.ui.util.SWTUtil;
import org.eclipse.jdt.internal.ui.util.StringMatcher;

/**
 * An element tree selection dialog with a filter box on top.
 */
public class FilteredElementTreeSelectionDialog extends ElementTreeSelectionDialog {

	private static class MultiplePatternFilter extends PatternFilter {
		
		private StringMatcher[] fMatchers;
		
		public void setPattern(String patternString) {
			super.setPattern(patternString);
			fMatchers= null;
			if (patternString != null && patternString.length() > 0) {
				ArrayList res= new ArrayList();
				StringTokenizer tok= new StringTokenizer(patternString, ",;"); //$NON-NLS-1$
				fMatchers= new StringMatcher[tok.countTokens()];
				for (int i= 0; i < fMatchers.length; i++) {
					String token= tok.nextToken().trim();
					if (token.length() > 0) {
						res.add(new StringMatcher(token + '*', true, false));
					}
				}
				if (!res.isEmpty()) {
					fMatchers= (StringMatcher[]) res.toArray(new StringMatcher[res.size()]);
				}
			}
		}
		
		protected boolean wordMatches(String text) {
			if (text != null) {
				if (fMatchers == null || fMatchers.length == 0) {
					return true;
				}
				for (int i= 0; i < fMatchers.length; i++) {
					if (fMatchers[i].match(text)) {
						return true;
					}
				}
			}
			return false;
		}
	}
	
	
	private static class FilteredTreeWithFilter extends FilteredTree {
		public FilteredTreeWithFilter(Composite parent, int treeStyle, String initialFilter) {
			super(parent, treeStyle, new MultiplePatternFilter());
			if (initialFilter != null) {
				setFilterText(initialFilter);
				textChanged();
			}
		}
	}
	
	private String fInitialFilter;
	
	public FilteredElementTreeSelectionDialog(Shell parent, ILabelProvider labelProvider, ITreeContentProvider contentProvider) {
		super(parent, labelProvider, contentProvider);
		fInitialFilter= null;
	}
	
	/**
	 * A comma separate list of patterns that are filled in initial filter list.
	 * Example is: '*.jar, *.zip'
	 * 
	 * @param initialFilter the initial filter or <code>null</code>.
	 */
	public void setInitialFilter(String initialFilter) {
		fInitialFilter= initialFilter;
	}

	protected TreeViewer doCreateTreeViewer(Composite parent, int style) {
		FilteredTree tree= new FilteredTreeWithFilter(parent, style, fInitialFilter); 
		tree.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		applyDialogFont(tree);
		
		TreeViewer viewer= tree.getViewer();
		SWTUtil.setAccessibilityText(viewer.getControl(), Strings.removeMnemonicIndicator(getMessage()));
		return viewer;
	}
	
}
