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
package org.eclipse.jdt.text.tests.performance;

import org.eclipse.jdt.text.tests.performance.data.Dimension;
import org.eclipse.jdt.text.tests.performance.data.MeteringSession;


/**
 * @since 3.1
 */
public interface IEvaluator {

	void evaluate(MeteringSession session);

	void setDimensions(Dimension[] dimensions);

	void setReferenceFilterProperties(String driver, String testname, String host, String timestamp);

}
