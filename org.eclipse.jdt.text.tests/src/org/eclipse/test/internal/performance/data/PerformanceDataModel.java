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
package org.eclipse.test.internal.performance.data;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


/**
 * @since 3.1
 */
public class PerformanceDataModel {
	private static Map fgModels;
	
	private Map fSessions;
	private final String fLocation;
	
	public synchronized Sample[] getMeteringSessions() {
		if (fSessions == null)
			reload();
		return (Sample[]) fSessions.values().toArray(new Sample[fSessions.size()]);
	}
	
	public PerformanceDataModel(String location) {
		fLocation= location;
	}
	
	public synchronized void reload() {
		PerformanceFileParser parser= new PerformanceFileParser();
		Sample[] files= parser.parseLocation(fLocation);
		Map map= new HashMap();
		for (int i= 0; i < files.length; i++) {
			map.put(files[i].getId(), files[i]);
		}
		fSessions= map;
	}
	
	public synchronized void refresh() {
		File dir= new File(fLocation);
		if (!dir.isDirectory()) {
			reload();
			return;
		}
		
		PerformanceFileParser parser= new PerformanceFileParser();
		String[] files= dir.list();
		for (int i= 0; i < files.length; i++) {
			File file= new File(dir, files[i]);
			try {
				String path= file.getCanonicalPath();
				if (fSessions.containsKey(path)) {
					Sample xmlfile= parser.parse(new BufferedInputStream(new FileInputStream(file)));
					fSessions.put(xmlfile.getId(), xmlfile);
				}
			} catch (IOException e) {
				// ignore and continue
			}
		}
	}

	public static PerformanceDataModel getInstance(String string) {
		if (fgModels == null)
			fgModels= new HashMap();
		PerformanceDataModel model= (PerformanceDataModel) fgModels.get(string);
		if (model == null) {
			model= new PerformanceDataModel(string);
			fgModels.put(string, model);
		}
		return model;
	}
}
