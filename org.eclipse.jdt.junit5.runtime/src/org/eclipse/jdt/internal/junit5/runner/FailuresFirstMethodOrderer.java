/*******************************************************************************
 * Copyright (c) 2022 SpringSource and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - based on FailuresFirstSorter.java
 *******************************************************************************/
package org.eclipse.jdt.internal.junit5.runner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.MethodDescriptor;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.MethodOrdererContext;

public class FailuresFirstMethodOrderer implements MethodOrderer {

	private List<String> failuresList= new ArrayList<>();

	public FailuresFirstMethodOrderer() {
	}

	private final Comparator<MethodDescriptor> comparator= new Comparator<MethodDescriptor>() {

		/**
		 * Compares two descriptions based on the failure list.
		 * @param d1 the first MethodDescriptor to compare with
		 * @param d2 the second MethodDescriptor to compare with
		 * @return -1 if only d1 has failures, 1 if only d2 has failures, 0 otherwise
		 */
		@Override
		public int compare(MethodDescriptor d1, MethodDescriptor d2) {
			boolean d1HasFailures = hasFailures(d1);
			boolean d2HasFailures = hasFailures(d2);

			if (d1HasFailures) {
				return -1;
			} else if (d2HasFailures) {
				return 1;
			} else { // ((d1HasFailures && d2HasFailures) || (!d1HasFailures && !d2HasFailures))
				return 0;
			}
		}

		private boolean hasFailures(MethodDescriptor d) {
			// failure names are of form METHOD_NAME(CLASS_NAME) so translate MethodDescriptor
			String methodId= d.getMethod().getName() + "(" + d.getMethod().getDeclaringClass().getName() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
			if (failuresList.contains(methodId)) {
				return true;
			}
			return false;
		}

	};

	/**
	 * Sort the methods encapsulated in the supplied
	 * {@link MethodOrdererContext} with failures first
	 * and formal parameter lists.
	 */
	@Override
	public void orderMethods(MethodOrdererContext context) {
		Optional<String> failureNamesParm= context.getConfigurationParameter(JUnit5TestLoader.FAILURE_NAMES);
		String failureNamesString= failureNamesParm.orElse(null);
		if (failureNamesString != null) {
			String[] failureNames= failureNamesString.split(";"); //$NON-NLS-1$
			failuresList= Arrays.asList(failureNames);
		}
		context.getMethodDescriptors().sort(comparator);
	}

}

