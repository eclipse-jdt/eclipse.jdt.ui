/*******************************************************************************
 * Copyright (c) 2023 Vector Informatik GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Vector Informatik GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.junit.launcher;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This class has the necessary logic to calculate the cache of all test methods that belong to a
 * JUnit configuration.
 */
class TestMethodsCache {
	/**
	 * An <code>AutoCloseable</code> that can contain nested instances and can run a
	 * <code>Runnable</code> upon creating the <i>outer</i> instance. <br/>
	 * <br/>
	 * Example:
	 *
	 * <pre>
	 *
	 * try (var outer= new NestedAutoCloseable(() -> System.out.println("Hello outer"))) { // prints "Hello outer"
	 * 	try (var inner= new NestedAutoCloseable(() -> System.out.println("Hello inner"))) { // doesn't print anything
	 * 		// ...
	 * 	}
	 * }
	 * </pre>
	 */
	private static class NestedAutoCloseable implements AutoCloseable {
		private static int fgDepth;

		NestedAutoCloseable(Runnable onCreateOuterBlock) {
			if (fgDepth == 0) {
				onCreateOuterBlock.run();
			}
			fgDepth++;
		}

		@Override
		public final void close() {
			fgDepth--;
		}
	}

	private boolean fCanceled;

	private final Map<String, Set<String>> fCacheMap= new HashMap<>();

	void put(String key, Set<String> value) {
		fCacheMap.put(key, value);
	}

	Set<String> get(String key) {
		return fCacheMap.get(key);
	}

	boolean containsKey(String key) {
		return fCacheMap.containsKey(key);
	}

	boolean isCanceled() {
		return fCanceled;
	}

	void setCanceled(boolean canceled) {
		fCanceled= canceled;
	}

	/**
	 * @return an <code>AutoCloseable</code> that guarantees that searching for test methods needs
	 *         to be canceled only once even in nested calls.
	 */
	NestedAutoCloseable runNestedCancelable() {
		return new NestedAutoCloseable(() -> fCanceled= false);
	}
}
