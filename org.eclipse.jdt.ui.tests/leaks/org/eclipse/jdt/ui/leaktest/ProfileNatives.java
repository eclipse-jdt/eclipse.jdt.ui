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
	
	public static void main(String[] args) {
		Vector v= new Vector();
		try {
			System.out.println("count1= "+getInstanceCount(Vector.class));
			MyThread t= new MyThread();
			t.setObject(new Vector());
			System.out.println("count2= "+getInstanceCount(Vector.class));
			t.getObject();
			t.doStop();
			
			System.out.println("count3= "+getInstanceCount(Vector.class));
			Object o= new TestClass(new Vector()).createInner();
			System.out.println("count4= "+getInstanceCount(Vector.class));
			o= null;
			System.out.println("count5= "+getInstanceCount(Vector.class));
			
			System.out.println("count6= "+getInstanceCount(Vector.class));
			o= new TestClass(new Vector()).createAnon();
			System.out.println("count7= "+getInstanceCount(Vector.class));
			o= null;
			System.out.println("count8= "+getInstanceCount(Vector.class));
			} catch (ProfileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	static class TestClass {
		int fFoo;
		Object fBar;
		
		TestClass(Object bar) {
			fBar= bar;
		}
		
		/**
		 * @return
		 */
		public Object createAnon() {
			return new Runnable() {

				public void run() {
					// TODO Auto-generated method stub
					
				}
				
			};
		}

		Object createInner() {
			return new Inner();
		}
		
		public class Inner {
			
		}
	}
	
	static class MyThread extends Thread {
		ThreadLocal fLocal;
		boolean fStop= false;
		MyThread() {
			fLocal= new ThreadLocal();
			this.start();
		}
		
		public void setObject(Object foo) {
			fLocal.set(foo);
		}
		
		public Object getObject() {
			return fLocal.get();
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Thread#run()
		 */
		public void run() {
			while (!fStop) {
				System.out.println("sleeping");
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			};
			System.out.println("thread stopping");
		}
		
		public void doStop() {
			fStop= true;
		}
	}
}
	
