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

package org.eclipse.test.performance;

import org.eclipse.test.internal.performance.data.Sample;

/**
 * A <code>PerformanceMeter</code> is used to measure an arbitrary operation
 * multiple times. Typical measurements include time, CPU cycle and/or memory
 * consumption.
 * 
 * Example usage in a test case:
 * <pre>
 * public void testOpenEditor() {
 * 	PerformanceMeter performanceMeter= Performance.getDefault().createPerformanceMeter(this);
 * 	for (int i= 0; i < 10; i++) {
 * 		performanceMeter.start();
 * 		openEditor();
 * 		performanceMeter.stop();
 * 		closeEditor();
 * 	}
 * 	performanceMeter.commit();
 * 	Performance.getDefault().assertPerformance(performanceMeter);
 * }
 * </pre>
 * 
 * This class is not intended to be subclassed by clients.
 */
public abstract class PerformanceMeter {

	/**
	 * Called immediately before the operation to measure.
	 */
	public abstract void start();

	/**
	 * Called immediately after the operation to measure.
	 */
	public abstract void stop();

	/**
	 * Called after repeated measurement is done.
	 */
	public abstract void commit();

	protected abstract Sample getSample();
}
