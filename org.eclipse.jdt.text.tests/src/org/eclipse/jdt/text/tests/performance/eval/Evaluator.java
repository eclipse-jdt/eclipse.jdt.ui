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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import junit.framework.Assert;

import org.eclipse.jdt.text.tests.performance.PerfMsrConstants;
import org.eclipse.jdt.text.tests.performance.data.MeteringSession;
import org.eclipse.jdt.text.tests.performance.data.PerfMsrDimensions;
import org.eclipse.jdt.text.tests.performance.data.PerformanceDataModel;


/**
 * @since 3.1
 */
public class Evaluator implements IEvaluator {

	private static final String DRIVER_PROPERTY= "eclipse.performance.reference.driver"; //$NON-NLS-1$
	private static final String TIMESTAMP_PROPERTY= "eclipse.performance.reference.timestamp"; //$NON-NLS-1$
	
	private static IEvaluator fgDefaultEvaluator;
	
	private Map fFilterProperties;
	private AssertChecker[] fCheckers;

	public void evaluate(MeteringSession session) {
		MeteringSession reference= getReferenceSession(session.getProperty(PerfMsrConstants.TESTNAME_PROPERTY), session.getProperty(PerfMsrConstants.HOSTNAME_PROPERTY));
		Assert.assertTrue("reference is null", reference != null); //$NON-NLS-1$
		
		StatisticsSession referenceStats= new StatisticsSession(reference);
		StatisticsSession measuredStats= new StatisticsSession(session);
		
		StringBuffer failMesg= new StringBuffer("Performance criteria not met when compared to driver '" + reference.getProperty(PerfMsrConstants.DRIVER_PROPERTY) + "' from " + reference.getProperty(PerfMsrConstants.RUN_TS_PROPERTY) + ":");
		boolean pass= true;
		for (int i= 0; i < fCheckers.length; i++) {
			pass &= fCheckers[i].test(referenceStats, measuredStats, failMesg);
		}
		
		Assert.assertTrue(failMesg.toString(), pass);
	}

	private MeteringSession getReferenceSession(String testname, String hostname) {
		String specificHost= (String) fFilterProperties.get(PerfMsrConstants.HOSTNAME_PROPERTY);
		boolean useSessionsHostname= true;
		if (specificHost != null)
			useSessionsHostname= false;
		
		PerformanceDataModel model= getDataModel();
		MeteringSession[] sessions= model.getMeteringSessions();
		for (int i= 0; i < sessions.length; i++) {
			MeteringSession session= sessions[i];
			boolean match= true;
			
			// check filter properties
			for (Iterator it= fFilterProperties.keySet().iterator(); match && it.hasNext();) {
				String property= (String) it.next();
				String value= session.getProperty(property);
				String filterValue= (String) fFilterProperties.get(property);
				if (!value.equals(filterValue))
					match= false;
			}
			// check properties by specific hostname
			if (match 
					&& testname.equals(session.getProperty(PerfMsrConstants.TESTNAME_PROPERTY))
					&& (!useSessionsHostname || hostname.equals(session.getProperty(PerfMsrConstants.HOSTNAME_PROPERTY))))
				return session;
		}
		return null;
	}

	protected PerformanceDataModel getDataModel() {
		return PerformanceDataModel.getInstance("/home/tei/tmp/perfmsr");
	}

	public void setAssertCheckers(AssertChecker[] asserts) {
		fCheckers= asserts;
	}

	public void setReferenceFilterProperties(String driver, String timestamp) {
		fFilterProperties= new HashMap();
		Assert.assertNotNull(driver); // must specify driver;
		fFilterProperties.put(PerfMsrConstants.DRIVER_PROPERTY, driver);
		if (timestamp != null)
			fFilterProperties.put(PerfMsrConstants.RUN_TS_PROPERTY, timestamp);
	}
	
	public static synchronized IEvaluator getDefaultEvaluator() {
		if (fgDefaultEvaluator == null) {
			fgDefaultEvaluator= new Evaluator();
			String driver= System.getProperty(DRIVER_PROPERTY, "3.0"); //$NON-NLS-1$
			String timestamp= System.getProperty(TIMESTAMP_PROPERTY, "20040625"); //$NON-NLS-1$
			fgDefaultEvaluator.setReferenceFilterProperties(driver, timestamp);
			
			AssertChecker cpu= new RelativeBandChecker(PerfMsrDimensions.CPU_TIME, 0.0F, 1.0F);
			AssertChecker mem= new RelativeBandChecker(PerfMsrDimensions.WORKING_SET, 0.0F, 1.0F);
			
			fgDefaultEvaluator.setAssertCheckers(new AssertChecker[] { cpu, mem });
		}

		return fgDefaultEvaluator;
	}

}
