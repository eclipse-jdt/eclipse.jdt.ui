/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
	
	private static native int getInstanceCount0(Class clazz, Class[] excludedClasses);
	private static native boolean isInitialized0();

	public static boolean isInitialized() {
		return fgIsNativeLoaded;
	}
	
	public static int getInstanceCount(Class clazz, Class[] excludedClasses) throws ProfileException {
		int instanceCount= getInstanceCount0(clazz, excludedClasses);
		if (instanceCount < 0)
			throw new ProfileException("Could not get instance count");
		return instanceCount;
	}
	
	public static int getInstanceCount(Class clazz) throws ProfileException {
		return getInstanceCount(clazz, new Class[0]);
	}
	
}
