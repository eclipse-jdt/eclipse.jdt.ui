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

import junit.framework.TestCase;

import org.eclipse.core.runtime.Platform;

import org.eclipse.jdt.text.tests.JdtTextTestPlugin;
import org.eclipse.jdt.text.tests.performance.OSPerformanceMeterFactory;
import org.eclipse.jdt.text.tests.performance.PerformanceMeterFactory;
import org.eclipse.jdt.text.tests.performance.eval.Evaluator;

/**
 * Helper for performance measurements. Currently provides performance meter
 * creation and checking of measurements.
 */
public class Performance {

	private static final String PLUGIN_ID= JdtTextTestPlugin.PLUGIN_ID;

	private static final String PERFORMANCE_METER_FACTORY= "/option/performanceMeterFactory";
	
	private static final String PERFORMANCE_METER_FACTORY_PROPERTY= "PerformanceMeterFactory";

	private static Performance fgDefault;
	
	private PerformanceMeterFactory fPerformanceMeterFactory;
	
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
	
	/**
	 * Creates a performance meter for the given test. Use
	 * {@link Performance#createPerformanceMeter(TestCase, String)} if more
	 * than one performance meter is used for the same test.
	 * 
	 * @param test the test
	 * @return a performance meter for the given test
	 * @throws IllegalArgumentException if a performance meter for the given
	 *                 test has already been created
	 */
	public PerformanceMeter createPerformanceMeter(TestCase test) {
		return getPeformanceMeterFactory().createPerformanceMeter(test);
	}
	
	/**
	 * Creates a performance meter for the given test and meter id.
	 * 
	 * @param test the test
	 * @param meterId the meter id
	 * @return a performance meter for the given test
	 * @throws IllegalArgumentException if a performance meter for the given
	 *                 test and meter id has already been created
	 */
	public PerformanceMeter createPerformanceMeter(TestCase test, String meterId) {
		return getPeformanceMeterFactory().createPerformanceMeter(test, meterId);
	}

	private PerformanceMeterFactory getPeformanceMeterFactory() {
		if (fPerformanceMeterFactory == null)
			fPerformanceMeterFactory= createPerformanceMeterFactory();
		return fPerformanceMeterFactory;
	}
	
	private PerformanceMeterFactory createPerformanceMeterFactory() {
		PerformanceMeterFactory factory;
		factory= tryInstantiate(System.getProperty(PERFORMANCE_METER_FACTORY_PROPERTY));
		if (factory != null)
			return factory;
		
		factory= tryInstantiate(Platform.getDebugOption(PLUGIN_ID + PERFORMANCE_METER_FACTORY));
		if (factory != null)
			return factory;
		
		return createDefaultPerformanceMeterFactory();
	}
	
	private PerformanceMeterFactory tryInstantiate(String className) {
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

	private PerformanceMeterFactory createDefaultPerformanceMeterFactory() {
		return new OSPerformanceMeterFactory();
	}
}
