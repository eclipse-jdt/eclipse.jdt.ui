/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.text.spelling.engine;

/**
 * Default phonetic distance algorithm for English words.
 * <p>
 * This algorithm implements the Levenshtein text edit distance.
 * </p>
 *
 * @since 3.0
 */
public final class DefaultPhoneticDistanceAlgorithm implements IPhoneticDistanceAlgorithm {

	/** The change case cost */
	public static final int COST_CASE= 10;

	/** The insert character cost */
	public static final int COST_INSERT= 95;

	/** The remove character cost */
	public static final int COST_REMOVE= 95;

	/** The substitute characters cost */
	public static final int COST_SUBSTITUTE= 100;

	/** The swap characters cost */
	public static final int COST_SWAP= 90;

	@Override
	public int getDistance(final String from, final String to) {

		return getDistance(from, to, Integer.MAX_VALUE);
	}

	@Override
	public int getDistance(final String from, final String to, final int threshold) {

		// If this already meets or exceeds the threshold we can skip the full
		// computation entirely.  With DISTANCE_THRESHOLD=160 and min-cost=95 this
		// rejects every candidate whose length differs by two or more characters
		int lengthDiff= Math.abs(from.length() - to.length());
		if (lengthDiff > 0) {
			int lowerBound= lengthDiff * Math.min(COST_INSERT, COST_REMOVE);
			if (lowerBound >= threshold)
				return lowerBound;
		}

		final char[] first= (" " + from).toCharArray(); //$NON-NLS-1$
		final char[] second= (" " + to).toCharArray(); //$NON-NLS-1$

		final int rows= first.length;
		final int columns= second.length;

		// Use three rolling 1-D arrays instead of a full n×m matrix
		int[] prev2= new int[columns]; // row - 2  (needed for swap cost)
		int[] prev1= new int[columns]; // row - 1
		int[] curr=  new int[columns]; // row  (current)

		// Initialise row 0: only COST_REMOVE accumulates along columns.
		for (int column= 1; column < columns; column++)
			prev1[column]= prev1[column - 1] + COST_REMOVE;

		char source, target;
		int swap, change, minimum, diagonal, insert, remove;

		for (int row= 1; row < rows; row++) {

			source= first[row];
			curr[0]= prev1[0] + COST_INSERT;
			int rowMin= Integer.MAX_VALUE;

			for (int column= 1; column < columns; column++) {

				target= second[column];
				diagonal= prev1[column - 1];

				if (source == target) {
					curr[column]= diagonal;
					if (diagonal < rowMin)
						rowMin= diagonal;
					continue;
				}

				change= Integer.MAX_VALUE;
				if (Character.toLowerCase(source) == Character.toLowerCase(target))
					change= COST_CASE + diagonal;

				swap= Integer.MAX_VALUE;
				if (row != 1 && column != 1 && source == second[column - 1] && first[row - 1] == target)
					swap= COST_SWAP + prev2[column - 2];

				minimum= COST_SUBSTITUTE + diagonal;
				if (swap < minimum)
					minimum= swap;

				remove= curr[column - 1];
				if (COST_REMOVE + remove < minimum)
					minimum= COST_REMOVE + remove;

				insert= prev1[column];
				if (COST_INSERT + insert < minimum)
					minimum= COST_INSERT + insert;
				if (change < minimum)
					minimum= change;

				curr[column]= minimum;

				if (minimum < rowMin)
					rowMin= minimum;
			}
			
			// If the smallest value in this row already meets or exceeds the
			// threshold, the final distance cannot be below it (all remaining
			// edit costs are non-negative), so abort early
			if (rowMin >= threshold)
				return rowMin;

			// Rotate the three row references without allocating new arrays
			int[] tmp= prev2;
			prev2= prev1;
			prev1= curr;
			curr= tmp;
		}
		return prev1[columns - 1];
	}
}
