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
import org.eclipse.jdt.text.tests.performance.data.PerformanceDataModel;


/**
 * @since 3.1
 */
public class Evaluator implements IEvaluator {

	private Map fFilterProperties;
	private AssertChecker[] fPredicates;

	public void evaluate(MeteringSession session) {
		MeteringSession reference= getReferenceSession();
		Assert.assertTrue("reference is null", reference != null); //$NON-NLS-1$
		
		StatisticsSession referenceStats= new StatisticsSession(reference);
		StatisticsSession measuredStats= new StatisticsSession(session);
		
		StringBuffer failMesg= new StringBuffer("Performance criteria not met when compared to driver '" + reference.getProperty(PerfMsrConstants.DRIVER_PROPERTY) + "' from " + reference.getProperty(PerfMsrConstants.RUN_TS_PROPERTY) + ":");
		boolean pass= true;
		for (int i= 0; i < fPredicates.length; i++) {
			pass &= fPredicates[i].test(referenceStats, measuredStats, failMesg);
		}
		
		Assert.assertTrue(failMesg.toString(), pass);
	}

	private MeteringSession getReferenceSession() {
		PerformanceDataModel model= getDataModel();
		MeteringSession[] sessions= model.getMeteringSessions();
		for (int i= 0; i < sessions.length; i++) {
			MeteringSession session= sessions[i];
			boolean match= true;
			for (Iterator it= fFilterProperties.keySet().iterator(); match && it.hasNext();) {
				String property= (String) it.next();
				String value= session.getProperty(property);
				String filterValue= (String) fFilterProperties.get(property);
				if (!value.equals(filterValue))
					match= false;
			}
			if (match)
				return session;
		}
		return null;
	}

	protected PerformanceDataModel getDataModel() {
		return PerformanceDataModel.getInstance("/home/tei/tmp/perfmsr");
	}

	public void setPredicates(AssertChecker[] predicates) {
		fPredicates= predicates;
	}

	public void setReferenceFilterProperties(String driver, String testname, String host, String timestamp) {
		fFilterProperties= new HashMap();
		Assert.assertNotNull(driver); // must specify driver;
		fFilterProperties.put(PerfMsrConstants.DRIVER_PROPERTY, driver);
		Assert.assertNotNull(testname); // must specify testname;
		fFilterProperties.put(PerfMsrConstants.TESTNAME_PROPERTY, testname);
		if (host != null)
			fFilterProperties.put(PerfMsrConstants.HOSTNAME_PROPERTY, host);
		if (timestamp != null)
			fFilterProperties.put(PerfMsrConstants.RUN_TS_PROPERTY, timestamp);
	}

}
