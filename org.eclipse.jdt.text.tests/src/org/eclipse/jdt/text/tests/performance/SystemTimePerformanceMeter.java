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

import java.util.ArrayList;
import java.util.List;

public class SystemTimePerformanceMeter extends PerformanceMeter {

	private String fScenario;
	
	private long fStartTime;
	
	private List fTime= new ArrayList();
	
	public SystemTimePerformanceMeter(String scenario) {
		fScenario= scenario;
	}
	
	public void start() {
		fStartTime= System.currentTimeMillis();
	}
	
	public void stop() {
		fTime.add(new Long(System.currentTimeMillis() - fStartTime));
	}
	
	public void commit() {
		System.out.println("Scenario: " + fScenario);
		
		int maxOccurenceLength= String.valueOf(fTime.size()).length();
		for (int i= 0; i < fTime.size(); i++) {
			long time= ((Long) fTime.get(i)).longValue();
			String occurence= String.valueOf(i + 1);
			System.out.println("Occurence " + replicate(" ", maxOccurenceLength - occurence.length()) + occurence + ": " + time);
		}
	}
	
	private String replicate(String s, int n) {
		StringBuffer buf= new StringBuffer(n * s.length());
		for (int i= 0; i < n; i++)
			buf.append(s);
		return buf.toString();
	}
}
