package org.eclipse.jdt.ui.tests;


public class ProfileNatives {
	
	private static boolean fgLibraryConnected;
	
	static {
		try {
			System.loadLibrary("ProfileNatives");
			fgLibraryConnected= true;
		} catch (Throwable e) {
			System.out.println("Connecting to library failed: " + e.getMessage());
			e.printStackTrace();
			fgLibraryConnected= false;
		}
	}
	
	public static boolean isLibraryConnected() {
		return fgLibraryConnected;
	}
	
	
	public static native int getInstanceCount(Class clazz);
	
}

