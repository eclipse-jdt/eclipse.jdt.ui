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

import org.eclipse.test.internal.performance.OSPerformanceMeterFactory;
import org.eclipse.test.internal.performance.PerformanceMeterFactory;
import org.eclipse.test.internal.performance.eval.Evaluator;
import org.eclipse.jdt.text.tests.JdtTextTestPlugin;

/**
 * Helper for performance measurements. Currently provides performance meter
 * creation and checking of measurements.
 * 
 * This class is not intended to be subclassed by clients.
 */
public class Performance {

	private static final String PLUGIN_ID= JdtTextTestPlugin.PLUGIN_ID;

	private static final String PERFORMANCE_METER_FACTORY= "/option/performanceMeterFactory";
	
	private static final String PERFORMANCE_METER_FACTORY_PROPERTY= "PerformanceMeterFactory";

	private static Performance fgDefault;
	
	private PerformanceMeterFactory fPerformanceMeterFactory;
	
	private Performance() {
	}
	
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
	
	/**
	 * Asserts default properties of the measurements captured by the given
	 * performance meter.
	 * 
	 * @param performanceMeter the performance meter
	 * @throws RuntimeException if the properties do not hold
	 */
	public void assertPerformance(PerformanceMeter performanceMeter) {
		Evaluator.getDefaultEvaluator().evaluate(performanceMeter);
	}
	
	/**
	 * Creates a performance meter for the given scenario id.
	 * 
	 * @param scenarioId the scenario id
	 * @return a performance meter for the given scenario id
	 * @throws IllegalArgumentException if a performance meter for the given
	 *                 scenario id has already been created
	 */
	public PerformanceMeter createPerformanceMeter(String scenarioId) {
		return getPeformanceMeterFactory().createPerformanceMeter(scenarioId);
	}

	/**
	 * Returns a default scenario id for the given test. The test's name
	 * must have been set, such that <code>test.getName()</code> is not
	 * <code>null</code>.
	 * 
	 * @param test the test
	 * @return the default scenario id for the test
	 */
	public String getDefaultScenarioId(TestCase test) {
		return test.getClass().getName() + "#" + test.getName() + "()";
	}
	
	/**
	 * Returns a default scenario id for the given test and id. The test's
	 * name must have been set, such that <code>test.getName()</code> is
	 * not <code>null</code>. The id distinguishes multiple scenarios in
	 * the same test.
	 * 
	 * @param test the test
	 * @param id the id
	 * @return the default scenario id for the test and the id
	 */
	public String getDefaultScenarioId(TestCase test, String id) {
		return getDefaultScenarioId(test) + "-" + id;
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
