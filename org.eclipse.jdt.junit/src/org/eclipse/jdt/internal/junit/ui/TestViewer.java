/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.junit.ui;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.jdt.internal.junit.model.TestCaseElement;
import org.eclipse.jdt.internal.junit.model.TestElement;
import org.eclipse.jdt.internal.junit.model.TestRoot;
import org.eclipse.jdt.internal.junit.model.TestRunSession;
import org.eclipse.jdt.internal.junit.model.TestSuiteElement;
import org.eclipse.jdt.internal.junit.model.TestElement.Status;


public class TestViewer {
	
	private final class FailuresOnlyFilter extends ViewerFilter {
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (! (element instanceof TestElement))
				return true;
			
			TestElement testElement= ((TestElement) element);
			Status status= testElement.getStatus();
			if (status == Status.FAILURE || status == Status.ERROR)
				return true;
			else if (status == Status.RUNNING)
				return fLayoutMode == TestRunnerViewPart.LAYOUT_HIERARCHICAL; // could be parent of error/failure  
			else
				return false;
		}
	}

	private static class ReverseList extends AbstractList {
		private final List fList;
		public ReverseList(List list) {
			fList= list;
		}
		public Object get(int index) {
			return fList.get(fList.size() - index - 1);
		}
		public int size() {
			return fList.size();
		}
	}
	
	private final TestRunnerViewPart fTestRunnerPart;
	
	private final TreeViewer fTreeViewer;
	private final Image fHierarchyIcon;
	private final TestSessionContentProvider fTestSessionContentProvider;
	private final TestSessionLabelProvider fTestSessionLabelProvider;
	
	private ViewerFilter fFailuresOnlyFilter;
	private int fLayoutMode;
	
	private TestRunSession fTestRunSession;
	
	private boolean fNeedRefresh;
	private HashSet/*<TestElement>*/ fNeedUpdate;
	
	private TestCaseElement fAutoScrollTarget;
	private LinkedList/*<TestSuiteElement>*/ fAutoClose;
	private HashSet/*<TestSuite>*/ fAutoExpand;
	
	public TestViewer(Composite parent, TestRunnerViewPart runner) {
		fTestRunnerPart= runner;
		
		fHierarchyIcon= TestRunnerViewPart.createImage("obj16/testhier.gif"); //$NON-NLS-1$
		parent.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				disposeIcons();
			}
		});
		
		fTreeViewer= new TreeViewer(parent, SWT.V_SCROLL | SWT.SINGLE); //TODO: SWT.MULTI
		fTreeViewer.setUseHashlookup(true);
		fTestSessionContentProvider= new TestSessionContentProvider();
		fTreeViewer.setContentProvider(fTestSessionContentProvider);
		fTestSessionLabelProvider= new TestSessionLabelProvider(fTestRunnerPart);
		fTreeViewer.setLabelProvider(fTestSessionLabelProvider);
		
		fLayoutMode= TestRunnerViewPart.LAYOUT_HIERARCHICAL;
		fFailuresOnlyFilter= null;
		
//		OpenStrategy handler = new OpenStrategy(fTreeViewer.getTree());
//		handler.addPostSelectionListener(new SelectionAdapter() {
//			public void widgetSelected(SelectionEvent e) {
//				fireSelectionChanged();
//			}
//		});

		
//		initMenu();
		fTreeViewer.getTree().addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				handleSelected();
			}
			public void widgetDefaultSelected(SelectionEvent e) {
				handleDefaultSelected(null);
			}
		});
		
		fNeedRefresh= true;
		fNeedUpdate= new HashSet();
		fAutoClose= new LinkedList();
		fAutoExpand= new HashSet();
	}
	
	public TreeViewer getTreeViewer() {
		return fTreeViewer;
	}

	public synchronized void setActiveSession(TestRunSession testRunSession) {
		fTestRunSession= testRunSession;
		registerAutoScrollTarget(null);
		registerViewerRefresh();
	}

	void handleDefaultSelected(MouseEvent e) {
		IStructuredSelection selection= (IStructuredSelection) fTreeViewer.getSelection();
		if (selection.size() != 1)
			return;

		TestElement testElement= (TestElement) selection.getFirstElement();

		OpenTestAction action;
		if (testElement instanceof TestSuiteElement) {
			action= new OpenTestAction(fTestRunnerPart, testElement.getTestName());
		} else if (testElement instanceof TestCaseElement){
			TestCaseElement testCase= (TestCaseElement) testElement;
			action= new OpenTestAction(fTestRunnerPart, testCase.getClassName(), testCase.getTestMethodName());
		} else {
			throw new IllegalStateException(String.valueOf(testElement));
		}

		if (action.isEnabled())
			action.run();
	}
	
	private void handleSelected() {
		IStructuredSelection selection= (IStructuredSelection) fTreeViewer.getSelection();
		TestElement testElement= null;
		if (selection.size() == 1) {
			testElement= (TestElement) selection.getFirstElement();
		}
		fTestRunnerPart.handleTestSelected(testElement);
	}
	
	void disposeIcons() {
		fHierarchyIcon.dispose();
	}

	public void setShowFailuresOnly(boolean failuresOnly, int layoutMode) {
		fLayoutMode= layoutMode;
		try {
			fTreeViewer.getTree().setRedraw(false);
			
			//avoid realizing all TreeItems in flat mode!
			if (failuresOnly) {
				if (!isShowFailuresOnly()) {
					fFailuresOnlyFilter= new FailuresOnlyFilter();
					fTreeViewer.addFilter(fFailuresOnlyFilter);
				}
				fTestSessionContentProvider.setLayout(layoutMode);
				fTestSessionLabelProvider.setLayout(layoutMode);
				
			} else {
				fTestSessionContentProvider.setLayout(layoutMode);
				fTestSessionLabelProvider.setLayout(layoutMode);
				if (isShowFailuresOnly()) {
					fTreeViewer.removeFilter(fFailuresOnlyFilter);
					fFailuresOnlyFilter= null;
				}
			}
		} finally {
			fTreeViewer.getTree().setRedraw(true);
		}		
	}
	
	private boolean isShowFailuresOnly() {
		return fFailuresOnlyFilter != null;
	}

	/**
	 * To be called periodically by the TestRunnerViewPart (in the UI thread).
	 */
	public void processChangesInUI() {
		TestRoot testRoot;
		if (fTestRunSession != null) {
			testRoot= fTestRunSession.getTestRoot();
		} else {
			testRoot= null;
			fNeedRefresh= true;
		}
		
		if (fNeedRefresh) {
			registerViewerRefresh();
			fNeedRefresh= false;
			fTreeViewer.setInput(testRoot);
			
		} else {
			Object[] toUpdate;
			synchronized (this) {
				toUpdate= fNeedUpdate.toArray();
				fNeedUpdate.clear();
			}
			if (isShowFailuresOnly() && testRoot != null) {
				for (int i= 0; i < toUpdate.length; i++) {
					TestElement testElement= (TestElement) toUpdate[i];
					if (testElement instanceof TestCaseElement) {
						if (testElement.getStatus().isFailure()) {
							if (fTreeViewer.testFindItem(testElement) == null)
								fTreeViewer.add(testRoot, testElement);
							else
								fTreeViewer.update(testElement, null);
						} else {
							fTreeViewer.remove(testElement);
						}
					}
				}
			} else {
				fTreeViewer.update(toUpdate, null);
			}
		}
		autoScrollInUI();
	}

	private void autoScrollInUI() {
		if (! fTestRunnerPart.isAutoScroll()) {
			clearAutoExpand();			
			fAutoClose.clear();
			return;
		}
		
		synchronized (this) {
			for (Iterator iter= fAutoExpand.iterator(); iter.hasNext();) {
				TestSuiteElement suite= (TestSuiteElement) iter.next();
				fTreeViewer.setExpandedState(suite, true);
			}
			clearAutoExpand();
		}
		
		TestCaseElement current= fAutoScrollTarget;
		fAutoScrollTarget= null;
		
		TestSuiteElement parent= current == null ? null : (TestSuiteElement) fTestSessionContentProvider.getParent(current);
		if (fAutoClose.isEmpty() || ! fAutoClose.getLast().equals(parent)) {
			// we're in a new branch, so let's close old OK branches:
			for (ListIterator iter= fAutoClose.listIterator(fAutoClose.size()); iter.hasPrevious();) {
				TestSuiteElement previousAutoOpened= (TestSuiteElement) iter.previous();
				if (previousAutoOpened.equals(parent))
					break;
				
				if (previousAutoOpened.getStatus() == TestElement.Status.OK) {
					// auto-opened the element, and all children are OK -> auto close
					iter.remove();
					fTreeViewer.collapseToLevel(previousAutoOpened, AbstractTreeViewer.ALL_LEVELS);
				}
			}
			
			while (parent != null && ! fTestRunSession.getTestRoot().equals(parent) && fTreeViewer.getExpandedState(parent) == false) {
				fAutoClose.add(parent); // add to auto-opened elements -> close later if STATUS_OK 
				parent= (TestSuiteElement) fTestSessionContentProvider.getParent(parent);
			}
		}
		if (current != null)
			fTreeViewer.reveal(current);
	}

	public void selectFirstFailure() {
		TestCaseElement firstFailure= getNextChildFailure(fTestRunSession.getTestRoot(), true);
		if (firstFailure != null)
			fTreeViewer.setSelection(new StructuredSelection(firstFailure), true);
	}
	
	public void selectFailure(boolean showNext) {
		ITreeSelection selection= (ITreeSelection) fTreeViewer.getSelection();
		TestElement selected= (TestElement) selection.getFirstElement();
		TestElement next;
		
		if (selected == null) {
			next= getNextChildFailure(fTestRunSession.getTestRoot(), showNext);
		} else if (selected instanceof TestSuiteElement) {
			next= getNextChildFailure((TestSuiteElement) selected, showNext);
			if (next == null)
				next= getNextFailureSibling(selected, showNext);
		} else {
			next= getNextFailureSibling(selected, showNext);
		}
		
		if (next != null)
			fTreeViewer.setSelection(new StructuredSelection(next), true);
	}
	
	private TestCaseElement getNextFailureSibling(TestElement current, boolean showNext) {
		TestSuiteElement parent= current.getParent();
		if (parent == null)
			return null;
		
		List siblings= Arrays.asList(parent.getChildren());
		if (! showNext)
			siblings= new ReverseList(siblings);
		
		int nextIndex= siblings.indexOf(current) + 1;
		for (int i= nextIndex; i < siblings.size(); i++) {
			TestElement sibling= (TestElement) siblings.get(i);
			if (sibling.getStatus().isFailure()) {
				if (sibling instanceof TestCaseElement) {
					return (TestCaseElement) sibling;
				} else {
					return getNextChildFailure((TestSuiteElement) sibling, showNext);
				}
			}
		}
		return getNextFailureSibling(parent, showNext);
	}

	private TestCaseElement getNextChildFailure(TestSuiteElement root, boolean showNext) {
		List children= Arrays.asList(root.getChildren());
		if (! showNext)
			children= new ReverseList(children);
		for (int i= 0; i < children.size(); i++) {
			TestElement child= (TestElement) children.get(i);
			if (child.getStatus().isFailure()) {
				if (child instanceof TestCaseElement) {
					return (TestCaseElement) child;
				} else {
					return getNextChildFailure((TestSuiteElement) child, showNext);
				}
			}
		}
		return null;
	}

	public synchronized void registerViewerRefresh() {
		fNeedRefresh= true;
		fNeedUpdate.clear();
		fAutoClose.clear();
		clearAutoExpand();
	}
	
	public synchronized void registerViewerUpdate(final TestElement testElement) {
		// update all parents too:
		TestElement element= testElement;
		do {
			fNeedUpdate.add(element);
			element= element.getParent();
		} while (element != null);
	}

	public synchronized void clearAutoExpand() {
		fAutoExpand.clear();
	}
	
	public void registerAutoScrollTarget(TestCaseElement testCaseElement) {
		fAutoScrollTarget= testCaseElement;
	}

	public synchronized void registerFailedForAutoScroll(TestCaseElement testCaseElement) {
		fAutoExpand.add(fTestSessionContentProvider.getParent(testCaseElement));
	}
}
 
