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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IBundleGroup;
import org.eclipse.core.runtime.IBundleGroupProvider;
import org.eclipse.core.runtime.Platform;

import org.eclipse.test.performance.PerformanceMeter;

import org.eclipse.jdt.text.tests.performance.data.Assert;
import org.eclipse.jdt.text.tests.performance.data.DataPoint;
import org.eclipse.jdt.text.tests.performance.data.MeteringSession;
import org.eclipse.jdt.text.tests.performance.data.Scalar;

public class SystemTimePerformanceMeter extends PerformanceMeter {

	private static final String VERSION_SUFFIX= "-runtime";
	
	private static final String UNKNOWN_BUILDID= "unknownBuildId";
	
	private static final String BUILDID_PROPERTY= "eclipse.buildId";

	private static final String LOCALHOST= "localhost";
	
	private static final String DIMENSION_NAME= "System Time";

	private static final int DEFAULT_INITIAL_CAPACITY= 3;
	
	private String fScenario;
	
	private List fStartTime;
	
	private List fStopTime;

	private static final String SDK_BUNDLE_GROUP_IDENTIFIER= "org.eclipse.sdk";
	
	public SystemTimePerformanceMeter(String scenario) {
		this(scenario, DEFAULT_INITIAL_CAPACITY);
	}
	
	public SystemTimePerformanceMeter(String scenario, int initalCapacity) {
		fScenario= scenario;
		fStartTime= new ArrayList(initalCapacity);
		fStopTime= new ArrayList(initalCapacity);
	}
	
	public void start() {
		fStartTime.add(new Long(System.currentTimeMillis()));
	}
	
	public void stop() {
		fStopTime.add(new Long(System.currentTimeMillis()));
	}
	
	public void commit() {
		Assert.isTrue(fStartTime.size() == fStopTime.size());
		System.out.println("Scenario: " + fScenario);
		int maxOccurenceLength= String.valueOf(fStartTime.size()).length();
		for (int i= 0; i < fStartTime.size(); i++) {
			String occurence= String.valueOf(i + 1);
			System.out.println("Occurence " + replicate(" ", maxOccurenceLength - occurence.length()) + occurence + ": " + (((Long) fStopTime.get(i)).longValue() - ((Long) fStartTime.get(i)).longValue()));
		}
	}
	
	private String replicate(String s, int n) {
		StringBuffer buf= new StringBuffer(n * s.length());
		for (int i= 0; i < n; i++)
			buf.append(s);
		return buf.toString();
	}

	public MeteringSession getSessionData() {
		Assert.isTrue(fStartTime.size() == fStopTime.size());
		Map properties= new HashMap();
		properties.put(PerfMsrConstants.DRIVER_PROPERTY, getBuildId());
		properties.put(PerfMsrConstants.HOSTNAME_PROPERTY, getHostName());
		properties.put(PerfMsrConstants.RUN_TS_PROPERTY, String.valueOf(System.currentTimeMillis()));
		properties.put(PerfMsrConstants.TESTNAME_PROPERTY, getScenarioName());
		
		DataPoint[] data= new DataPoint[2*fStartTime.size()];
		for (int i= 0; i < fStartTime.size(); i++) {
			data[2*i]= createDataPoint(PerfMsrConstants.BEFORE, DIMENSION_NAME, ((Long) fStartTime.get(i)).longValue());
			data[2*i + 1]= createDataPoint(PerfMsrConstants.AFTER, DIMENSION_NAME, ((Long) fStopTime.get(i)).longValue());
		}
		
		return new MeteringSession(properties, data);
	}

	private String getBuildId() {
		String buildId= System.getProperty(BUILDID_PROPERTY);
		if (buildId != null)
			return buildId;
		IBundleGroupProvider[] providers= Platform.getBundleGroupProviders();
		for (int i= 0; i < providers.length; i++) {
			IBundleGroup[] groups= providers[i].getBundleGroups();
			for (int j= 0; j < groups.length; j++)
				if (SDK_BUNDLE_GROUP_IDENTIFIER.equals(groups[j].getIdentifier()))
					return groups[j].getVersion() + VERSION_SUFFIX;
		}
		return UNKNOWN_BUILDID;
	}

	private String getHostName() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			return LOCALHOST;
		}
	}

	private DataPoint createDataPoint(String kind, String dimension, long value) {
		Map scalars= new HashMap();
		scalars.put(dimension, new Scalar(dimension, value));
		return new DataPoint(kind, scalars);
	}

	private String getScenarioName() {
		return fScenario;
	}
}
