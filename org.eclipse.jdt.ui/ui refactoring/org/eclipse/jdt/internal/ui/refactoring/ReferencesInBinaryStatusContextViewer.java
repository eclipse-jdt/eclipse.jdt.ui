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
package org.eclipse.jdt.internal.ui.refactoring;

import com.ibm.icu.text.Collator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IWorkspaceRoot;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.ViewForm;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;

import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.ltk.ui.refactoring.IStatusContextViewer;

import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.search.SearchMatch;

import org.eclipse.jdt.internal.corext.refactoring.base.ReferencesInBinaryContext;

import org.eclipse.jdt.ui.StandardJavaElementContentProvider;

import org.eclipse.jdt.internal.ui.viewsupport.AppearanceAwareLabelProvider;

public class ReferencesInBinaryStatusContextViewer implements IStatusContextViewer {
	
	private static class ContentProvider extends StandardJavaElementContentProvider {
		private Map fChildren= new HashMap();
		private Set fRoots= new HashSet();

		public Object[] getChildren(Object parentElement) {
			Object children= fChildren.get(parentElement);
			if (children == null) {
				return new Object[0];
			} else if (children instanceof Set) {
				return ((Set) children).toArray();
			} else {
				return new Object[] { children };
			}
		}

		public boolean hasChildren(Object element) {
			return getChildren(element).length > 0;
		}

		public Object[] getElements(Object inputElement) {
			return fRoots.toArray();
		}

		public void add(Object element) {
			Object parent= getParent(element);
			while (parent != null) {
				if (parent instanceof IJavaModel) {
					fRoots.add(element);
				} else if (parent instanceof IWorkspaceRoot) {
					fRoots.add(element);
				} else {
					Object oldChildren= fChildren.get(parent);
					if (element.equals(oldChildren)) {
						return;
					} else if (oldChildren instanceof Set) {
						((Set) oldChildren).add(element);
						return;
					} else if (oldChildren != null) {
						Set newChildren= new HashSet(4);
						newChildren.add(oldChildren);
						newChildren.add(element);
						fChildren.put(parent, newChildren);
						return;
					} else {
						fChildren.put(parent, element);
					}
				}
				element= parent;
				parent= getParent(element);
			}
		}
	}
	

	private ViewForm fForm;
	private CLabel fLabel;
	private TreeViewer fTreeViewer;

	/*
	 * @see org.eclipse.ltk.ui.refactoring.IStatusContextViewer#setInput(org.eclipse.ltk.core.refactoring.RefactoringStatusContext)
	 */
	public void setInput(RefactoringStatusContext input) {
		ContentProvider contentProvider= new ContentProvider();
		
		ReferencesInBinaryContext binariesContext= (ReferencesInBinaryContext) input;
		List matches= binariesContext.getMatches();
		for (Iterator iter= matches.iterator(); iter.hasNext();) {
			SearchMatch match= (SearchMatch) iter.next();
			Object element= match.getElement();
			if (element != null)
				contentProvider.add(element);
		}
		fTreeViewer.setContentProvider(contentProvider);
		fTreeViewer.setInput(contentProvider);
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public void createControl(Composite parent) {
		fForm= new ViewForm(parent, SWT.BORDER | SWT.FLAT);
		fForm.marginWidth= 0;
		fForm.marginHeight= 0;
		
		fLabel= new CLabel(fForm, SWT.NONE);
		fLabel.setText(RefactoringMessages.ReferencesInBinaryStatusContextViewer_title);
		fForm.setTopLeft(fLabel);

		fTreeViewer= new TreeViewer(fForm, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		final AppearanceAwareLabelProvider labelProvider= new AppearanceAwareLabelProvider();
		fTreeViewer.setLabelProvider(new DelegatingStyledCellLabelProvider(labelProvider));
		fTreeViewer.setComparator(new ViewerComparator() {
			private Collator fCollator= Collator.getInstance();
			public int compare(Viewer viewer, Object e1, Object e2) {
				String l1= labelProvider.getText(e1);
				String l2= labelProvider.getText(e2);
				return fCollator.compare(l1, l2);
			}
		});
		fForm.setContent(fTreeViewer.getControl());
		
		Dialog.applyDialogFont(parent);
	}

	/**
	 * {@inheritDoc}
	 */
	public Control getControl() {
		return fForm;
	}
	
}
