/*******************************************************************************
 * Copyright (c) 2014, 2021 Moritz Eysholdt and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Moritz Eysholdt <moritz.eysholdt@itemis.de> - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.junit4.runner;

import java.util.HashSet;
import java.util.Set;

import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.NoTestsRemainException;

/**
 * This filter keeps all matched {@link Description}s in a tree, including the children and
 * container of the matched Description.
 *
 * It is allowed to match more than one Description.
 */
public class SubForestFilter extends Filter {

	private Set<Description> fIncluded= null;

	private final DescriptionMatcher fMatcher;

	public SubForestFilter(DescriptionMatcher matcher) {
		fMatcher= matcher;
	}

	@Override
	public void apply(Object child) throws NoTestsRemainException {
		if (child instanceof Runner && fIncluded == null) {
			fIncluded= new HashSet<>();
			collectIncludedDescriptions(((Runner)child).getDescription());
			if (fIncluded.isEmpty())
				throw new NoTestsRemainException();
		}
		super.apply(child);
	}

	private boolean collectIncludedDescriptions(Description description) {
		if (fMatcher.matches(description)) {
			includeWithChildren(description);
			return true;
		}
		boolean hasIncludedChild= false;
		for (Description child : description.getChildren()) {
			hasIncludedChild|= collectIncludedDescriptions(child);
		}
		if (hasIncludedChild) {
			this.fIncluded.add(description);
		}
		return hasIncludedChild;
	}

	@Override
	public String describe() {
		return fMatcher.toString();
	}

	public Set<Description> getIncludedDescriptions() {
		return fIncluded;
	}

	private void includeWithChildren(Description description) {
		fIncluded.add(description);
		for (Description child : description.getChildren()) {
			includeWithChildren(child);
		}
	}

	@Override
	public boolean shouldRun(Description description) {
		return fIncluded.contains(description);
	}

}
