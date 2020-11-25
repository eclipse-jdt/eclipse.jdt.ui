package junit.extensions;

/*-
 * #%L
 * org.eclipse.jdt.ui.junit.sampleproject
 * %%
 * Copyright (C) 2020 Eclipse Foundation
 * %%
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 * #L%
 */

import junit.framework.*;

/**
 * A Decorator that runs a test repeatedly.
 *
 */
public class RepeatedTest extends TestDecorator {
	private int fTimesRepeat;

	public RepeatedTest(Test test, int repeat) {
		super(test);
		if (repeat < 0)
			throw new IllegalArgumentException("Repetition count must be > 0");
		fTimesRepeat = repeat;
	}

	public int countTestCases() {
		return super.countTestCases() * fTimesRepeat;
	}

	public void run(TestResult result) {
		for (int i = 0; i < fTimesRepeat; i++) {
			if (result.shouldStop())
				break;
			super.run(result);
		}
	}

	public String toString() {
		return super.toString() + "(repeated)";
	}
}
