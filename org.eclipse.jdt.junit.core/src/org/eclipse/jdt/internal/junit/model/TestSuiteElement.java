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

package org.eclipse.jdt.internal.junit.model;

import java.util.ArrayList;
import java.util.List;


public class TestSuiteElement extends TestElement {
	
	private List/*<TestElement>*/ fChildren;
	
	public TestSuiteElement(TestSuiteElement parent, String id, String testName, int childrenCount) {
		super(parent, id, testName);
		fChildren= new ArrayList(childrenCount);
	}

	public void addChild(TestElement child) {
		fChildren.add(child);
	}
	
	public TestElement[] getChildren() {
		return (TestElement[]) fChildren.toArray(new TestElement[fChildren.size()]);
	}
	
	/**
	 * @return one of the STATUS_* constants
	 */
	public Status getStatus() {
		//TODO: Cache failure count in hierarchy? Recheck behavior when introducing filters
		Status highest= Status.NOT_RUN;
		TestElement[] children= (TestElement[]) fChildren.toArray(new TestElement[fChildren.size()]); // copy list to avoid concurreny problems
		for (int i= 0; i < children.length; i++) {
			TestElement testElement= children[i];
			Status childStatus= testElement.getStatus();
			
			if (childStatus == Status.RUNNING)
				return Status.RUNNING;
			else if (childStatus.getPriority() < highest.getPriority())
				highest= childStatus;
		}
		return highest;
	}

	public String toString() {
		return super.toString() + " (" + fChildren.size() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
	}

}
