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

import org.eclipse.core.runtime.Platform;

import org.eclipse.jdt.text.tests.JdtTextTestPlugin;
import org.eclipse.jdt.text.tests.performance.OSPerformanceMeterFactory;
import org.eclipse.jdt.text.tests.performance.PerformanceMeterFactory;
import org.eclipse.jdt.text.tests.performance.eval.Evaluator;

public class Performance {

	private static final String PLUGIN_ID= JdtTextTestPlugin.PLUGIN_ID;

	private static final String PERFORMANCE_METER_FACTORY= "/option/performanceMeterFactory";
	
	private static final String PERFORMANCE_METER_FACTORY_PROPERTY= "PerformanceMeterFactory";

	private static Performance fgDefault;
	
	/**
	 * Returns the singleton of <code>Performance</code>
	 * 
	 * @return the singleton of <code>Performance</code>
	 */
	public static Performance getDefault() {
		if (fgDefault == null)
			fgDefault= new Performance();
		return fgDefault;
	}
	
	private Performance() {
	}
	
	/**
	 * Asserts default properties of the measurements captured by the given
	 * performance meter.
	 * 
	 * @param performanceMeter
	 * @throws RuntimeException if the properties do not hold
	 */
	public void assertPerformance(PerformanceMeter performanceMeter) {
		Evaluator.getDefaultEvaluator().evaluate(performanceMeter.getSample());
	}
	
	public static PerformanceMeterFactory createPerformanceMeterFactory() {
		PerformanceMeterFactory factory;
		factory= tryInstantiate(System.getProperty(PERFORMANCE_METER_FACTORY_PROPERTY));
		if (factory != null)
			return factory;
		
		factory= tryInstantiate(Platform.getDebugOption(PLUGIN_ID + PERFORMANCE_METER_FACTORY));
		if (factory != null)
			return factory;
		
		return createDefaultPerformanceMeterFactory();
	}
	
	private static PerformanceMeterFactory tryInstantiate(String className) {
		PerformanceMeterFactory instance= null;
		if (className != null && className.length() > 0) {
			try {
				Class c= JdtTextTestPlugin.getDefault().getBundle().loadClass(className);
				instance= (PerformanceMeterFactory) c.newInstance();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (ClassCastException e) {
				e.printStackTrace();
			}
		}
		return instance;
	}

	private static PerformanceMeterFactory createDefaultPerformanceMeterFactory() {
		return new OSPerformanceMeterFactory();
	}
}
