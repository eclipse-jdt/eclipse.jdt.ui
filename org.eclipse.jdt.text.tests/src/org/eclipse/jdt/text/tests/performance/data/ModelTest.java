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
package org.eclipse.jdt.text.tests.performance.data;


/**
 * @since 3.1
 */
public class ModelTest {

	public static void main(String[] args) {
		new ModelTest().run();
	}

	private void run() {
		PerformanceDataModel model= new PerformanceDataModel("/home/tei/tmp/perfmsr");
		System.out.println("there are " + model.getMeteringSessions().length + " files in the model");
		MeteringSession session= model.getMeteringSessions()[0];
		System.out.println(session);
		DataPoint datapoint= session.getDataPoints()[0];
		System.out.println(datapoint);
		Scalar[] scalars= datapoint.getScalars();
		for (int i= 0; i < scalars.length; i++) {
			System.out.println(scalars[i]);
		}
	}
}
