/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.junit.internal;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.launching.JavaRuntime;


public class LauncherUtil {
	
	private LauncherUtil() {
	}
		
	protected static String[] createClassPath(IType type) throws InvocationTargetException {
		URL url= JUnitPlugin.getDefault().getDescriptor().getInstallURL();
		try {
			String[] cp = JavaRuntime.computeDefaultRuntimeClassPath(type.getJavaProject());
			String[] classPath= new String[cp.length + 1];
			System.arraycopy(cp, 0, classPath, 1, cp.length);
			classPath[0]= Platform.asLocalURL(new URL(url, "junitsupport.jar")).getPath();
			return classPath;
		} catch (Exception e) {
			throw new InvocationTargetException(e);
		}
	}
}

