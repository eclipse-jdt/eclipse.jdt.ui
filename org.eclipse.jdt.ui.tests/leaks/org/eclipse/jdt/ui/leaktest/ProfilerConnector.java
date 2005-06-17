/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.leaktest;


/**
 *
 */
public class ProfilerConnector {

	public ProfilerConnector() throws ProfileException {
		if (!ProfileNatives.isInitialized()) {
			throw new ProfileException("Not connected to a profiler");
		}
	}
	
	public int getInstanceCount(Class cl) throws ProfileException {
		return ProfileNatives.getInstanceCount(cl);
	}

	public int getInstanceCount(Class cl, Class[] excludedClasses) throws ProfileException {
		return ProfileNatives.getInstanceCount(cl, excludedClasses);
	}
	
	public void close() {
	}

}
