/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.leaktest;

import junit.extensions.TestSetup;
import junit.framework.Test;


/**
 *
 */
public class LeakTestSetup extends TestSetup {
	
	private static LeakTestSetup fgCurrentSetup= null;
	private ProfilerConnector fConnection;
	
	/**
	 * @return Returns the profiler Connection or <code>null</code> if no profiler connection
	 * could be established (e.g. because not supported on the platform)
	 */
	public static ProfilerConnector getProfilerConnector() {
		return fgCurrentSetup.getConnection();
	}
		
		
	public LeakTestSetup(Test test) {
		super(test);
		if (fgCurrentSetup == null) {
			fgCurrentSetup= this;
		}
	}
	
	protected void setUp() throws Exception {
		if (fgCurrentSetup != this) {
			return;
		}
		try {
			fConnection= new ProfilerConnector();
		} catch (ProfileException e) {
			fConnection= null;
		}
	}

	protected void tearDown() throws Exception {
		if (fgCurrentSetup != this) {
			return;
		}
		fConnection.close();
	}

	/**
	 * @return Returns the connection.
	 */
	public ProfilerConnector getConnection() {
		return fConnection;
	}

}
