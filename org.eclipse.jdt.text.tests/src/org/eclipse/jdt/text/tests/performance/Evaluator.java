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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import junit.framework.Assert;

import org.eclipse.jdt.text.tests.performance.data.DataPoint;
import org.eclipse.jdt.text.tests.performance.data.Dimension;
import org.eclipse.jdt.text.tests.performance.data.MeteringSession;
import org.eclipse.jdt.text.tests.performance.data.PerformanceDataModel;
import org.eclipse.jdt.text.tests.performance.data.Scalar;


/**
 * @since 3.1
 */
public class Evaluator implements IEvaluator {

	private Dimension[] fDimensions;
	private Map fFilterProperties;

	public void evaluate(MeteringSession session) {
		MeteringSession reference= getReferenceSession();
		Assert.assertTrue("reference is null", reference != null); //$NON-NLS-1$
		
		Scalar[] averages= getAverages(reference.getDataPoints());
		Scalar[] measured= getAverages(session.getDataPoints());
		
		StringBuffer failMesg= new StringBuffer("Performance criteria not met:"); //$NON-NLS-1$
		boolean pass= true;
		for (int i= 0; i < measured.length; i++) {
			if (measured[i].getMagnitude() > averages[i].getMagnitude()) {
				failMesg.append("\n " + measured[i] + " > " + averages[i] + " from " + fFilterProperties.get("driver")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				pass= false;
			}
		}
		
		Assert.assertTrue(failMesg.toString(), pass);
	}

	private Scalar[] getAverages(DataPoint[] datapoints) {
		Scalar[] averages= new Scalar[fDimensions.length];
		
		for (int j= 0; j < fDimensions.length; j++) {
			int measurements= 0;
			long sum= 0;
			String dim= null;
			for (int i= 0; i < datapoints.length - 1; i += 2) {
				Assert.assertTrue("order of datapoints makes no sense", datapoints[i].getKind().equals("1")); //$NON-NLS-1$
				Assert.assertTrue("order of datapoints makes no sense", datapoints[i + 1].getKind().equals("2")); //$NON-NLS-1$
				
				Scalar delta= getDelta(datapoints[i], datapoints[i + 1], fDimensions[j]);
				sum += delta.getMagnitude();
				measurements++;
				dim= delta.getDimension();
			}
			
			long average= sum / measurements;
			averages[j]= new Scalar(dim, average);
		}
		return averages;
	}

	private Scalar getDelta(DataPoint before, DataPoint after, Dimension dimension) {
		Scalar one= before.getScalar(dimension);
		Assert.assertTrue("reference has no value for dimension " + dimension, one != null); //$NON-NLS-1$

		Scalar two= after.getScalar(dimension);
		Assert.assertTrue("reference has no value for dimension " + dimension, two != null); //$NON-NLS-1$
		
		Scalar delta= new Scalar(one.getDimension(), two.getMagnitude() - one.getMagnitude());
		return delta;
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

	public void setDimensions(Dimension[] dimensions) {
		fDimensions= dimensions;
	}

	public void setReferenceFilterProperties(String driver, String testname, String host, String timestamp) {
		fFilterProperties= new HashMap();
		Assert.assertNotNull(driver); // must specify driver;
		fFilterProperties.put("driver", driver);
		Assert.assertNotNull(testname); // must specify testname;
		fFilterProperties.put("testname", testname);
		if (host != null)
			fFilterProperties.put("host", host);
		if (timestamp != null)
			fFilterProperties.put("runTS", timestamp);
	}

}
