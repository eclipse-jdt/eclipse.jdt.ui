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

public class Performance {

	public static String PERFORMANCE_METER_FACTORY= "PerformanceMeterFactory";
	
	public static PerformanceMeterFactory createPerformanceMeterFactory() {
		String factoryName= System.getProperty(PERFORMANCE_METER_FACTORY);
		if (factoryName != null && factoryName.length() > 0)
			try {
				Class c= Class.forName(factoryName);
				return (PerformanceMeterFactory) c.newInstance();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (ClassCastException e) {
				e.printStackTrace();
			}
		return createDefaultPerformanceMeterFactory();
	}
	
	private static PerformanceMeterFactory createDefaultPerformanceMeterFactory() {
		return new OSPerformanceMeterFactory();
	}
}
