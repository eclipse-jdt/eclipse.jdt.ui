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

package org.eclipse.test.internal.performance.eval;

import org.eclipse.test.internal.performance.data.Sample;

/**
 * The empty evaluator. Does nothing.
 */
public class EmptyEvaluator implements IEvaluator {

	/*
	 * @see org.eclipse.test.internal.performance.eval.IEvaluator#evaluate(org.eclipse.test.internal.performance.data.Sample)
	 */
	public void evaluate(Sample session) throws RuntimeException {
	}

	/*
	 * @see org.eclipse.test.internal.performance.eval.IEvaluator#setAssertCheckers(org.eclipse.test.internal.performance.eval.AssertChecker[])
	 */
	public void setAssertCheckers(AssertChecker[] asserts) {
	}

	/*
	 * @see org.eclipse.test.internal.performance.eval.IEvaluator#setReferenceFilterProperties(java.lang.String, java.lang.String)
	 */
	public void setReferenceFilterProperties(String driver, String timestamp) {
	}
}
