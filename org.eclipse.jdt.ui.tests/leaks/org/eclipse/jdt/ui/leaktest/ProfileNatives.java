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


public class ProfileNatives {
	private static boolean fgIsNativeLoaded= false;
	static {
		try {
			System.loadLibrary("ProfileNatives");
			fgIsNativeLoaded= isInitialized0();
		} catch (UnsatisfiedLinkError e) {
			e.printStackTrace();
		}
	}
	
	private static native int getInstanceCount0(Class clazz);
	private static native boolean isInitialized0();

	public static boolean isInitialized() {
		return fgIsNativeLoaded;
	}
	
	public static int getInstanceCount(Class clazz) throws ProfileException {
		int instanceCount= getInstanceCount0(clazz);
		if (instanceCount < 0)
			throw new ProfileException("Could not get instance count");
		return instanceCount;
	}
}
	
