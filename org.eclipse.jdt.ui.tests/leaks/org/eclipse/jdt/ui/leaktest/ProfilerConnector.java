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

import java.util.Vector;

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
	
	
	private static Vector fgReference;
	
	public static void main(String[] str) {
		try {
			ProfilerConnector pc= new ProfilerConnector(); //$NON-NLS-1$
			System.out.println("Connection installed.\n"); //$NON-NLS-1$
			System.out.println("instance count Vector: " + pc.getInstanceCount(Vector.class));
			fgReference= new Vector();
			fgReference.add(fgReference);
			
			System.out.println("instance count Vector: " + pc.getInstanceCount(Vector.class));
			fgReference= null;
			System.out.println("instance count Vector: " + pc.getInstanceCount(Vector.class));
			
			pc.close();
		} catch (ProfileException e) {
			e.printStackTrace();
		}
	}

}
