/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.dialogs;

import java.util.Comparator;
import org.eclipse.jface.util.Assert;

/**
 * Quick sort to sort two arrays in parallel.
 */
public class TwoArrayQuickSorter {
	
	private Comparator fComparator;

	/**
	 * Default comparator
	 */
	final class StringComparator implements Comparator {
	
		private boolean fIgnoreCase;
	
		StringComparator(boolean ignoreCase) {
			fIgnoreCase= ignoreCase;
		}
	
		public int compare(Object left, Object right) {
			return fIgnoreCase
				? ((String) left).compareToIgnoreCase((String) right)
				: ((String) left).compareTo((String) right);
		}
		
	}		
				
	public TwoArrayQuickSorter(boolean ignoreCase) {
		fComparator= new StringComparator(ignoreCase);
	}

	public TwoArrayQuickSorter(Comparator comparator) {
		fComparator= comparator;
	}
	
	/**
	 * Sorts keys and values in parallel.
	 */
	public void sort(String[] keys, Object[] values) {
		if ((keys == null) || (values == null)) {
			Assert.isTrue(false, "Either keys or values in null"); //$NON-NLS-1$
			return;
		}

		if (keys.length <= 1)
			return;
			
		internalSort(keys, values, 0, keys.length - 1);	
	}

	private void internalSort(String[] keys, Object[] values, int left, int right) {
		int original_left= left;
		int original_right= right;
		
		String mid= keys[(left + right) / 2]; 
		do { 
			while (fComparator.compare(keys[left], mid) < 0)
				left++; 

			while (fComparator.compare(mid, keys[right]) < 0)
				right--; 

			if (left <= right) { 
				String tmp= keys[left]; 
				keys[left]= keys[right]; 
				keys[right]= tmp;
				
				Object tmp2= values[left]; 
				values[left]= values[right]; 
				values[right]= tmp2;
				
				left++; 
				right--; 
			} 
		} while (left <= right);
		
		if (original_left < right)
			internalSort(keys , values, original_left, right); 

		if (left < original_right)
			internalSort(keys, values, left, original_right); 
	}

}