package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.util.Arrays;

import org.eclipse.jdt.core.ISourceRange;

import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.textmanipulation.MoveTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.MultiTextEdit;

/**
 * A <code>PermuteSourceRangesTextEdit</code> permutes a set of text ranges.
 */
class PermuteSourceRangesTextEdit extends MultiTextEdit {

	/**
	 * Create a new <code>PermuteSourceRangesTextEdit</code>
	 * 
	 * @param ranges source ranges to be permuted
	 * @param permutation pemutation (describing the new element position in the array)
	 */
	public PermuteSourceRangesTextEdit(ISourceRange[] ranges, int[] permutation) {
		validateArguments(ranges, permutation);
		for (int i= 0; i < ranges.length; i++) {
			add(new MoveTextEdit(ranges[i].getOffset(), ranges[i].getLength(), ranges[permutation[i]].getOffset()));
		}
	}

	private static void validateArguments(ISourceRange[] ranges, int[] permutation){
		Assert.isTrue(ranges.length == permutation.length);
		int[] copy= createCopy(permutation);
		Arrays.sort(copy);
		for (int i= 0; i < copy.length; i++) {
			Assert.isTrue(copy[i] == i);
		}
	}
	
	private static int[] createCopy(int[] orig){
		int[] result= new int[orig.length];
		for (int i= 0; i < result.length; i++) {
			result[i]= orig[i];
		}
		return result;
	}
}
