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
package org.eclipse.jdt.text.tests.performance.eval;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.text.tests.performance.data.Assert;
import org.eclipse.jdt.text.tests.performance.data.Dimension;


/**
 * @since 3.1
 */
public abstract class AssertChecker {
	private Set fDimensions;
	
	public AssertChecker(Dimension dimension) {
		this(new Dimension[] {dimension});
	}
	
	public AssertChecker(Dimension[] dimensions) {
		fDimensions= new HashSet();
		fDimensions.addAll(Arrays.asList(dimensions));
	}
	
	public Dimension[] getDimensions() {
		return (Dimension[]) fDimensions.toArray(new Dimension[fDimensions.size()]);
	}

	protected Dimension getDimension() {
		Assert.isTrue(fDimensions.size() == 1);
		return getDimensions()[0];
	}

	/**
	 * Evaluates the predicate.
	 * 
	 * @param reference statistics of dimensions of the reference metering session
	 * @param measured statistics of dimensions of the metering session to be tested
	 * @param message a message presented to the user upon failure
	 * @return <code>true</code> if the predicate passes, <code>false</code> if it fails
	 */
	public abstract boolean test(StatisticsSession reference, StatisticsSession measured, StringBuffer message);
}
