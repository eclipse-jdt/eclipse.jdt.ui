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

package org.eclipse.test.internal.performance;

import java.util.StringTokenizer;

import org.eclipse.perfmsr.core.IPerformanceMonitor;
import org.eclipse.perfmsr.core.LoadValueConstants;
import org.eclipse.perfmsr.core.PerfMsrCorePlugin;
import org.eclipse.perfmsr.core.Upload;
import org.eclipse.test.internal.performance.data.DataPoint;
import org.eclipse.test.internal.performance.data.Dimension;
import org.eclipse.test.internal.performance.data.PerfMsrDimensions;
import org.eclipse.test.internal.performance.data.PerformanceFileParser;
import org.eclipse.test.internal.performance.data.Sample;
import org.eclipse.test.internal.performance.data.Scalar;


/**
 * Performance meter that makes its measurements with OS functionality.
 */
public class OSPerformanceMeter extends InternalPerformanceMeter {

	/**
	 * The perfmsr plug-in's performance monitor
	 */
	private IPerformanceMonitor fPerformanceMonitor;
	
	/**
	 * The log file
	 */
	private String fLogFile;

	private static final String VERBOSE_PERFORMANCE_METER_PROPERTY= "InternalPrintPerformanceResults";

	private String fScenarioId;
	
	/**
	 * @param scenarioId the scenario id
	 */
	public OSPerformanceMeter(String scenarioId) {
		fPerformanceMonitor= PerfMsrCorePlugin.getPerformanceMonitor(false);
		fLogFile= getLogFile(scenarioId);
		fPerformanceMonitor.setLogFile(fLogFile);
		fPerformanceMonitor.setTestName(scenarioId);
		fScenarioId= scenarioId;
	}
	
	/*
	 * @see org.eclipse.test.performance.PerformanceMeter#start()
	 */
	public void start() {
		fPerformanceMonitor.snapshot(1);
	}
	
	/*
	 * @see org.eclipse.test.performance.PerformanceMeter#stop()
	 */
	public void stop() {
		fPerformanceMonitor.snapshot(2);
	}
	
	/*
	 * @see org.eclipse.test.performance.PerformanceMeter#commit()
	 */
	public void commit() {
		Upload.Status status= fPerformanceMonitor.upload(null);
		if (status.fileRenamed)
			fLogFile= status.message.substring(status.message.indexOf("Measurement file has been renamed to: ") + 38);
		System.out.println(status.message);
		
		if (System.getProperty(VERBOSE_PERFORMANCE_METER_PROPERTY) != null) {
			System.out.println(fScenarioId + ":");
			Sample sample= getSample();
			if (sample != null) {
				DataPoint[] dataPoints= sample.getDataPoints();
				for (int i= 0, n= dataPoints.length; i < n - 1; i += 2) {
					System.out.println("Iteration " + (i / 2 + 1) + ":");
					Scalar[] before= dataPoints[i].getScalars();
					Scalar[] after= dataPoints[i + 1].getScalars();
					for (int j= 0, m= Math.min(before.length, after.length); j < m; j++) {
						long valueBefore= before[j].getMagnitude();
						long valueAfter= after[j].getMagnitude();
						String dimensionId= before[j].getDimension();
						Dimension dimension= PerfMsrDimensions.getDimension(dimensionId);
						String name= dimension != null ? dimension.getName() + " [" + dimension.getUnit().getShortName() + "]" : dimensionId;
						System.out.println(name + ":\t" + valueBefore + "\t" + valueAfter + "\t" + (valueAfter - valueBefore));
					}
				}
			}
		}
	}

	/*
	 * @see org.eclipse.test.performance.PerformanceMeter#dispose()
	 */
	public void dispose() {
		fPerformanceMonitor= null;
		fLogFile= null;
	}

	/*
	 * @see org.eclipse.test.internal.performance.InternalPerformanceMeter#getSample()
	 */
	public Sample getSample() {
		Sample[] parsed= new PerformanceFileParser().parseLocation(fLogFile);
		if (parsed.length > 0)
			return parsed[0];
		else
			return null;
	}
	
	/**
	 * Returns the log file name including its path.
	 * 
	 * @param scenarioId the scenario id
	 * @return the log file name
	 */
	private String getLogFile(String scenarioId) {
		String logFile= "timer.xml-" + scenarioId;
		String ctrl= System.getProperty(LoadValueConstants.ENV_PERF_CTRL);
		if (ctrl == null)
			return logFile;
		
		StringTokenizer st= new StringTokenizer(ctrl, ";");
		while(st.hasMoreTokens()) {
			String token= st.nextToken();
			int i= token.indexOf('=');
			if (i < 1)
				continue;
			String value= token.substring(i+1);
			String parm= token.substring(0,i);
			if (parm.equals(LoadValueConstants.PerfCtrl.log))
				return value + "/" + logFile;
		}
		return logFile;
	}
}
