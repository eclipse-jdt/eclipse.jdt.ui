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
import org.eclipse.test.internal.performance.data.PerformanceFileParser;
import org.eclipse.test.internal.performance.data.Sample;


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
	
	/**
	 * @param scenarioId the scenario id
	 */
	public OSPerformanceMeter(String scenarioId) {
		fPerformanceMonitor= PerfMsrCorePlugin.getPerformanceMonitor(false);
		fLogFile= getLogFile(scenarioId);
		fPerformanceMonitor.setLogFile(fLogFile);
		fPerformanceMonitor.setTestName(scenarioId);
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
