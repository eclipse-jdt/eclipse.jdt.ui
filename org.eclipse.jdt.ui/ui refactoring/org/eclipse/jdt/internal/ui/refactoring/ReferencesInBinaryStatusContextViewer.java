/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.refactoring;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.icu.text.Collator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.ViewForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;

import org.eclipse.core.resources.IWorkspaceRoot;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;

import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.NewSearchUI;

import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.ltk.ui.refactoring.IStatusContextViewer;

import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.search.SearchMatch;

import org.eclipse.jdt.internal.corext.refactoring.base.ReferencesInBinaryContext;

import org.eclipse.jdt.ui.StandardJavaElementContentProvider;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.search.AbstractJavaSearchResult;
import org.eclipse.jdt.internal.ui.search.NewSearchResultCollector;
import org.eclipse.jdt.internal.ui.util.SWTUtil;
import org.eclipse.jdt.internal.ui.viewsupport.AppearanceAwareLabelProvider;

public class ReferencesInBinaryStatusContextViewer implements IStatusContextViewer {

	private static class ContentProvider extends StandardJavaElementContentProvider {
		private Map<Object, Object> fChildren= new HashMap<>();
		private Set<Object> fRoots= new HashSet<>();

		@Override
		public Object[] getChildren(Object parentElement) {
			Object children= fChildren.get(parentElement);
			if (children == null) {
				return new Object[0];
			} else if (children instanceof Set) {
				return ((Set<?>) children).toArray();
			} else {
				return new Object[] { children };
			}
		}

		@Override
		public boolean hasChildren(Object element) {
			return getChildren(element).length > 0;
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return fRoots.toArray();
		}

		public void add(Object element) {
			Object parent= getParent(element);
			while (parent != null) {
				if ((parent instanceof IJavaModel) || (parent instanceof IWorkspaceRoot)) {
					fRoots.add(element);
				} else {
					Object oldChildren= fChildren.get(parent);
					if (element.equals(oldChildren)) {
						return;
					} else if (oldChildren instanceof Set) {
						@SuppressWarnings("unchecked")
						Set<Object> oldChildrenSet= (Set<Object>) oldChildren;
						oldChildrenSet.add(element);
						return;
					} else if (oldChildren != null) {
						Set<Object> newChildren= new HashSet<>(4);
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
	private ReferencesInBinaryContext fInput;
	private Button fButton;

	/*
	 * @see org.eclipse.ltk.ui.refactoring.IStatusContextViewer#setInput(org.eclipse.ltk.core.refactoring.RefactoringStatusContext)
	 */
	@Override
	public void setInput(RefactoringStatusContext input) {
		ContentProvider contentProvider= new ContentProvider();

		ReferencesInBinaryContext binariesContext= (ReferencesInBinaryContext) input;
		List<SearchMatch> matches= binariesContext.getMatches();
		for (SearchMatch match : matches) {
			Object element= match.getElement();
			if (element != null)
				contentProvider.add(element);
		}
		fTreeViewer.setContentProvider(contentProvider);
		fTreeViewer.setInput(contentProvider);

		fLabel.setText(binariesContext.getDescription());

		fInput= binariesContext;
		fButton.setEnabled(!matches.isEmpty());
	}


	@Override
	public void createControl(Composite parent) {
		fForm= new ViewForm(parent, SWT.BORDER | SWT.FLAT);
		fForm.marginWidth= 0;
		fForm.marginHeight= 0;

		fLabel= new CLabel(fForm, SWT.NONE);
		fLabel.setText(RefactoringMessages.ReferencesInBinaryStatusContextViewer_title);
		fForm.setTopLeft(fLabel);

		Composite composite= new Composite(fForm, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout layout= new GridLayout(1, false);
		layout.marginWidth= 0;
		layout.marginHeight= 0;
		composite.setLayout(layout);


		fTreeViewer= new TreeViewer(composite, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		final AppearanceAwareLabelProvider labelProvider= new AppearanceAwareLabelProvider();
		fTreeViewer.setLabelProvider(new DelegatingStyledCellLabelProvider(labelProvider));
		fTreeViewer.setComparator(new ViewerComparator() {
			private Collator fCollator= Collator.getInstance();
			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				String l1= labelProvider.getText(e1);
				String l2= labelProvider.getText(e2);
				return fCollator.compare(l1, l2);
			}
		});
		fTreeViewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		fButton= new Button(composite, SWT.PUSH);
		fButton.setText(RefactoringMessages.ReferencesInBinaryStatusContextViewer_show_as_search_button);
		GridData layoutData= new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
		layoutData.widthHint= SWTUtil.getButtonWidthHint(fButton);
		fButton.setLayoutData(layoutData);
		fButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				fillInSearchView();
			}
		});
		fButton.setEnabled(false);

		fForm.setContent(composite);

		Dialog.applyDialogFont(parent);
	}

	protected void fillInSearchView() {
		NewSearchUI.runQueryInBackground(new ReferencesInBinarySearchQuery(fInput), null);
		fButton.setEnabled(false);
	}

	@Override
	public Control getControl() {
		return fForm;
	}


	static class ReferencesInBinarySearchQuery implements ISearchQuery {

		private final ReferencesInBinaryContext fContext;
		private ReferencesInBinarySearchResult fResult;

		public ReferencesInBinarySearchQuery(ReferencesInBinaryContext context) {
			fContext= context;
			fResult= new ReferencesInBinarySearchResult(this);
		}

		@Override
		public boolean canRerun() {
			return false;
		}

		@Override
		public boolean canRunInBackground() {
			return true;
		}

		@Override
		public String getLabel() {
			return fContext.getDescription();
		}

		@Override
		public ISearchResult getSearchResult() {
			return fResult;
		}

		@Override
		public IStatus run(IProgressMonitor monitor) throws OperationCanceledException {
			fResult.removeAll();
			List<SearchMatch> matches= fContext.getMatches();

			NewSearchResultCollector collector= new NewSearchResultCollector(fResult, false);
			collector.beginReporting();

			for (SearchMatch searchMatch : matches) {
				try {
					collector.acceptSearchMatch(searchMatch);
				} catch (CoreException e) {
					// ignore
				}
			}
			collector.endReporting();
			return Status.OK_STATUS;
		}

	}

	public static class ReferencesInBinarySearchResult extends AbstractJavaSearchResult {

		private final ReferencesInBinarySearchQuery fQuery;

		public ReferencesInBinarySearchResult(ReferencesInBinarySearchQuery query) {
			fQuery= query;
		}

		@Override
		public ImageDescriptor getImageDescriptor() {
			return JavaPluginImages.DESC_OBJS_SEARCH_REF;
		}

		@Override
		public String getLabel() {
			return fQuery.getLabel();
		}

		@Override
		public String getTooltip() {
			return getLabel();
		}

		@Override
		public ISearchQuery getQuery() {
			return fQuery;
		}
	}


}
