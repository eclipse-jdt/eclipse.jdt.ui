/* * (c) Copyright IBM Corp. 2000, 2001. * All Rights Reserved. */package org.eclipse.jdt.internal.ui.snippeteditor;import java.lang.reflect.InvocationTargetException;import java.lang.reflect.Method;import java.net.MalformedURLException;import java.net.URL;import java.net.URLClassLoader;
 
/**
 * Support class for launching a snippet evaluation
 */
public class ScrapbookMain {
	
	public static void main(String[] args) {
		URL url= null;
		
		try {
			url = new URL(args[0]);
		} catch (java.net.MalformedURLException e) {
			return;
		}
		ScrapbookMain s= new ScrapbookMain();
		while (true) {
			try {
				evalLoop(url, s);
			} catch (ClassNotFoundException e) {
				return;
			} catch (NoSuchMethodException e) {
				return;
			} catch (InvocationTargetException e) {
				return;
			} catch (IllegalAccessException e) {
				return;
			}
		}
	
	}
	
	
	
	public static void evalLoop(URL url, ScrapbookMain s) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException{
		ClassLoader cl= new URLClassLoader(new URL[] {url});
		Class clazz= cl.loadClass("org.eclipse.jdt.internal.ui.snippeteditor.ScrapbookMain");
		Method method= clazz.getDeclaredMethod("eval", new Class[] {ScrapbookMain.class});
		method.invoke(null, new Object[] {s});
	}
	
	public static void eval(ScrapbookMain s) {
		s.nop();
	}

	public static void nop() {
		try {
			Thread.sleep(100);
		} catch(InterruptedException e) {
		}
	}
}