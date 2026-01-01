/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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

package org.eclipse.jdt.internal.junit.ui;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.jdt.internal.junit.model.TestElement;
import org.eclipse.jdt.internal.junit.model.TestRoot;
import org.eclipse.jdt.internal.junit.model.TestSuiteElement;


public class TestSessionTreeContentProvider implements ITreeContentProvider {

	private final Object[] NO_CHILDREN= new Object[0];

	@Override
	public void dispose() {
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof TestSuiteElement) {
			TestSuiteElement suite = (TestSuiteElement) parentElement;
			Object[] children = suite.getChildren();
			
			// If getChildren() returns empty but we have a single dynamic child,
			// show that child in the tree viewer. This handles the case where a
			// parameterized test has only one parameter value remaining (e.g., after
			// excluding all but one enum value with @EnumSource mode=EXCLUDE).
			// See https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/945
			if (children.length == 0) {
				org.eclipse.jdt.internal.junit.model.TestCaseElement singleChild = suite.getSingleDynamicChild();
				if (singleChild != null) {
					return new Object[] { singleChild };
				}
			}
			
			return children;
		} else {
			return NO_CHILDREN;
		}
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return ((TestRoot) inputElement).getChildren();
	}

	@Override
	public Object getParent(Object element) {
		return ((TestElement) element).getParent();
	}

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof TestSuiteElement) {
			TestSuiteElement suite = (TestSuiteElement) element;
			
			// Check if there are visible children
			if (suite.getChildren().length != 0) {
				return true;
			}
			
			// Even if getChildren() returns empty, check for single dynamic child
			// This ensures the tree shows expansion arrow for suites with one parameter value
			return suite.getSingleDynamicChild() != null;
		} else {
			return false;
		}
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}
}
