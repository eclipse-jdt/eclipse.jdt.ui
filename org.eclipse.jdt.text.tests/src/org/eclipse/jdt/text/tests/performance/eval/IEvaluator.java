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

import org.eclipse.jdt.text.tests.performance.data.Dimension;
import org.eclipse.jdt.text.tests.performance.data.MeteringSession;


/**
 * @since 3.1
 */
public interface IEvaluator {

	/**
	 * Evaluates the given metering session by comparing it against the reference
	 * data selected with <code>setReferenceFilterProperties</code>.
	 * 
	 * @param session the current metering session to evaluate
	 * @throws RuntimeException when the comparison does not pass
	 */
	void evaluate(MeteringSession session) throws RuntimeException;

	/**
	 * Sets the predicates that should be evaluated by the evaluator.
	 * 
	 * @param predicates the preducates that should be evaluated by the evaluator
	 */
	void setPredicates(AssertChecker[] predicates);

	/**
	 * Selects the reference build against which the current session will
	 * be compared in <code>evaluate</code>.
	 * 
	 * @param driver 
	 * @param testname
	 * @param host the hostname for the tests may be null to use any
	 * @param timestamp the timestamp of the reference, may be null to use any
	 */
	void setReferenceFilterProperties(String driver, String testname, String host, String timestamp);

}
