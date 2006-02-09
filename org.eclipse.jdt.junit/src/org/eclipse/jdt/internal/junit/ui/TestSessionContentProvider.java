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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.jdt.internal.junit.model.TestCaseElement;
import org.eclipse.jdt.internal.junit.model.TestElement;
import org.eclipse.jdt.internal.junit.model.TestRoot;
import org.eclipse.jdt.internal.junit.model.TestSuiteElement;


public class TestSessionContentProvider implements ITreeContentProvider {

	private final Object[] NO_CHILDREN= new Object[0];
	
	private Viewer fViewer;
	private int fLayoutMode= TestRunnerViewPart.LAYOUT_HIERARCHICAL;

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		fViewer= viewer;
	}

	public Object[] getChildren(Object parentElement) {
		if (fLayoutMode == TestRunnerViewPart.LAYOUT_FLAT)
			return NO_CHILDREN;
			
		if (parentElement instanceof TestSuiteElement)
			return ((TestSuiteElement) parentElement).getChildren();
		else
			return NO_CHILDREN;
	}

	public Object getParent(Object element) {
		if (fLayoutMode == TestRunnerViewPart.LAYOUT_FLAT)
			return ((TestElement) element).getRoot();
		else
			return ((TestElement) element).getParent();
	}

	public boolean hasChildren(Object element) {
		if (fLayoutMode == TestRunnerViewPart.LAYOUT_FLAT)
			return false;
			
		if (element instanceof TestSuiteElement)
			return ((TestSuiteElement) element).getChildren().length != 0;
		else
			return false;
	}

	public Object[] getElements(Object inputElement) {
		if (fLayoutMode == TestRunnerViewPart.LAYOUT_FLAT) {
			ArrayList all= new ArrayList();
			addAll(all, (TestRoot) inputElement);
			return all.toArray();
		} else {
			return ((TestRoot) inputElement).getChildren();
		}
	}

	private void addAll(ArrayList all, TestSuiteElement suite) {
		TestElement[] children= suite.getChildren();
		for (int i= 0; i < children.length; i++) {
			TestElement element= children[i];
			if (element instanceof TestSuiteElement) {
				addAll(all, (TestSuiteElement) element);
			} else if (element instanceof TestCaseElement) {
				all.add(element);
			}
		}
	}

	public void setLayout(int layoutMode) {
		if (layoutMode != fLayoutMode) {
			fLayoutMode= layoutMode;
			if (fViewer != null) {
//				ISelection selection= fViewer.getSelection();
//				fViewer.refresh();
//				fViewer.setSelection(selection, true);
// workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=125708 :
				List selected= ((IStructuredSelection) fViewer.getSelection()).toList();
				fViewer.refresh();
				fViewer.setSelection(new StructuredSelection(selected), true);

			}
		}
	}

	public void dispose() {
		
	}
}
