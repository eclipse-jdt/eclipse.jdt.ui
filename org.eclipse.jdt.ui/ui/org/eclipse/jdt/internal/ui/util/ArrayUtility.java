/* * (c) Copyright IBM Corp. 2000, 2001. * All Rights Reserved. */package org.eclipse.jdt.internal.ui.util;import java.util.Collection;

public class ArrayUtility {
	
	private static Object[] fgEmptyArray= new Object[0];
	
	public static Object[] getEmptyArray() {
		return fgEmptyArray;
	}
	
	
	public static Object[] merge(Object[] a1, Object[] a2) {
		int a1Len= a1.length;
		int a2Len= a2.length;
		Object[] res= new Object[a1Len + a2Len];
		System.arraycopy(a1, 0, res, 0, a1Len);
		System.arraycopy(a2, 0, res, a1Len, a2Len); 
		return res;
	}
	
	/**
	 * Fills the contents of an array to a list
	 */	
	public static void addAll(Object[] objs, Collection dest) {
		for (int i= 0; i < objs.length; i++) {
			dest.add(objs[i]);
		}
	}
}