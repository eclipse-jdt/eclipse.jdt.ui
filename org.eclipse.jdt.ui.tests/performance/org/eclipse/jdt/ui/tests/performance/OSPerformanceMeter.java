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

package org.eclipse.jdt.ui.tests.performance;

import java.util.StringTokenizer;

import org.eclipse.perfmsr.core.IPerformanceMonitor;
import org.eclipse.perfmsr.core.LoadValueConstants;
import org.eclipse.perfmsr.core.PerfMsrCorePlugin;
import org.eclipse.perfmsr.core.Upload;

public class OSPerformanceMeter extends PerformanceMeter {

	private IPerformanceMonitor fPerformanceMonitor;
	
	public OSPerformanceMeter(String scenario) {
		fPerformanceMonitor= PerfMsrCorePlugin.getPerformanceMonitor(false);
		fPerformanceMonitor.setLogFile(getLogFile(scenario));
		fPerformanceMonitor.setTestName(scenario);
	}
	
	public void start() {
		fPerformanceMonitor.snapshot(1);
	}
	
	public void stop() {
		fPerformanceMonitor.snapshot(2);
	}
	
	public void commit() {
		Upload.Status status= fPerformanceMonitor.upload(null);
		System.out.println(status.message);
	}
	
	private String getLogFile(String scenario) {
		String logFile= "timer-" + scenario + ".xml";
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
