/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
	
	public Status getStatus() {
		//TODO: Cache failure count in hierarchy? Recheck behavior when introducing filters
		Status suiteStatus= getSuiteStatus();
//		Assert.isTrue(suiteStatus.isNotRun()
//				|| (suiteStatus == Status.FAILURE || suiteStatus == Status.ERROR));
		
		TestElement[] children= (TestElement[]) fChildren.toArray(new TestElement[fChildren.size()]); // copy list to avoid concurreny problems
		if (children.length == 0)
			return suiteStatus;
		
		Status cumulated= children[0].getStatus();
		
		for (int i= 1; i < children.length; i++) {
			Status childStatus= children[i].getStatus();
			cumulated= Status.combineStatus(cumulated, childStatus);
		}
		// not necessary, see special code in Status.combineProgress()
//		if (suiteStatus.isErrorOrFailure() && cumulated.isNotRun())
//			return suiteStatus; //progress is Done if error in Suite and no children run
		return Status.combineStatus(cumulated, suiteStatus);
	}

	public Status getSuiteStatus() {
		return super.getStatus();
	}
	
	public String toString() {
		return super.toString() + " (" + fChildren.size() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
	}

}
