package org.eclipse.jdt.text.tests.spelling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import org.eclipse.jdt.internal.ui.text.spelling.engine.DefaultPhoneticDistanceAlgorithm;


public class DefaultPhoneticDistanceAlgorithmTest {

	private static final DefaultPhoneticDistanceAlgorithm ALGO= new DefaultPhoneticDistanceAlgorithm();

	/** {@code AbstractSpellDictionary.DISTANCE_THRESHOLD} */
	private static final int DISTANCE_THRESHOLD= 160;


	@Test
	void testThresholdMaxValueMatchesUnbounded() {
		String[] words= { "cat", "cats", "catch", "kitten", "sitting", "algorithm" };
		for (String a : words) {
			for (String b : words) {
				int unbounded= ALGO.getDistance(a, b);
				int bounded= ALGO.getDistance(a, b, Integer.MAX_VALUE);
				assertEquals(unbounded, bounded,
						"getDistance with MAX threshold must equal unbounded result for (" + a + ", " + b + ")");
			}
		}
	}

	@Test
	void testThresholdJustAboveTrueDistanceReturnsCorrectResult() {
		int real= ALGO.getDistance("kitten", "sitting");
		int result= ALGO.getDistance("kitten", "sitting", real + 1);
		assertEquals(real, result, "Threshold just above true distance must not truncate result");
	}

	@Test
	void testIdenticalStringsHaveZeroDistance() {
		assertEquals(0, ALGO.getDistance("hello", "hello", DISTANCE_THRESHOLD));
		assertEquals(0, ALGO.getDistance("hello", "hello"));
	}

	@Test
	void testEmptyStringDistance() {
		int expected= ALGO.getDistance("", "abc");
		assertEquals(expected, ALGO.getDistance("", "abc", Integer.MAX_VALUE));
	}

	@Test
	void testLengthDiffTwoReturnsLowerBound() {
		int minCost= Math.min(DefaultPhoneticDistanceAlgorithm.COST_INSERT,
				DefaultPhoneticDistanceAlgorithm.COST_REMOVE);
		int result= ALGO.getDistance("cat", "catch", DISTANCE_THRESHOLD);

		assertTrue(result >= DISTANCE_THRESHOLD,
				"Length-diff-2 pair must be rejected (got " + result + ")");
		assertEquals(2 * minCost, result,
				"Must return the length-difference lower bound " + (2 * minCost));
	}

	@Test
	void testLengthDiffLargeReturnsLowerBound() {
		int minCost= Math.min(DefaultPhoneticDistanceAlgorithm.COST_INSERT,
				DefaultPhoneticDistanceAlgorithm.COST_REMOVE);

		int diff= "algorithm".length() - "a".length();
		int result= ALGO.getDistance("a", "algorithm", DISTANCE_THRESHOLD);
		assertTrue(result >= DISTANCE_THRESHOLD);
		assertEquals(diff * minCost, result);
	}


	@Test
	void testLengthDiffOneIsNotRejectedByLengthBound() {
		int result= ALGO.getDistance("cat", "cats", DISTANCE_THRESHOLD);
		assertTrue(result < DISTANCE_THRESHOLD,
				"Length-diff-1 must not be rejected by length bound; got " + result);
		assertEquals(ALGO.getDistance("cat", "cats"), result,
				"Result must match unbounded overload");
	}

	@Test
	void testEqualLengthWordsNotAffectedByLengthBound() {
		assertEquals(ALGO.getDistance("kitten", "bitten"),
				ALGO.getDistance("kitten", "bitten", DISTANCE_THRESHOLD));
	}

	@Test
	void testEqualLengthWordsWithBigDistanceExitsEarlier() {
		int distance = ALGO.getDistance("Elephant", "Sunshine");
		int distanceThreshold = ALGO.getDistance("Elephant", "Sunshine", DISTANCE_THRESHOLD);

		assertTrue(distance > DISTANCE_THRESHOLD);

		assertTrue(distanceThreshold > DISTANCE_THRESHOLD, "Distance must be greater than threshold");
		assertTrue(distanceThreshold < distance, "Distance must be smaller than unbounded distance");
	}
}