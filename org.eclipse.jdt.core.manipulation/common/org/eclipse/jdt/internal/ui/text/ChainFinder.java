/**
 * Copyright (c) 2010, 2019 Darmstadt University of Technology and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marcel Bruch - initial API and implementation.
 *    Stefan Henss - re-implementation in response to https://bugs.eclipse.org/bugs/show_bug.cgi?id=376796.
 */
package org.eclipse.jdt.internal.ui.text;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

public class ChainFinder {

	private final List<ITypeBinding> expectedTypes;

	private final List<String> excludedTypes;

	private final IJavaElement invocationSite;

	private final List<Chain> chains= new LinkedList<>();

	private final Map<IBinding, ChainElement> edgeCache= new HashMap<>();

	private final Map<Map<ITypeBinding, Boolean>, List<IBinding>> fieldsAndMethodsCache= new HashMap<>();

	private final Map<Map<ChainElement, ITypeBinding>, Boolean> assignableCache= new HashMap<>();

	public ChainFinder(final List<ITypeBinding> expectedTypes, final List<String> excludedTypes,
			final IJavaElement invocationSite) {
		this.expectedTypes= expectedTypes;
		this.excludedTypes= excludedTypes;
		this.invocationSite= invocationSite;
	}

	public void startChainSearch(final List<ChainElement> entrypoints, final int maxChains, final int minDepth,
			final int maxDepth) {
		for (final ITypeBinding expected : expectedTypes) {
			if (expected != null && !ChainFinder.isFromExcludedType(excludedTypes, expected)) {
				ITypeBinding expectedType= expected;
				int expectedDimension= 0;
				if (expectedType.isArray()) {
					expectedDimension= expectedType.getDimensions();
					expectedType= TypeBindingAnalyzer.removeArrayWrapper(expectedType);
				}
				searchChainsForExpectedType(expectedType, expectedDimension, entrypoints, maxChains, minDepth,
						maxDepth);
			}
		}
	}

	private void searchChainsForExpectedType(final ITypeBinding expectedType, final int expectedDimensions,
			final List<ChainElement> entrypoints, final int maxChains, final int minDepth, final int maxDepth) {
		final LinkedList<LinkedList<ChainElement>> incompleteChains= prepareQueue(entrypoints);

		while (!incompleteChains.isEmpty()) {
			final LinkedList<ChainElement> chain= incompleteChains.poll();
			final ChainElement edge= chain.getLast();
			if (isValidEndOfChain(edge, expectedType, expectedDimensions)) {
				if (chain.size() >= minDepth) {
					chains.add(new Chain(chain, expectedDimensions));
					if (chains.size() == maxChains) {
						break;
					}
				}
				continue;
			}
			if (chain.size() < maxDepth && incompleteChains.size() <= 50000) {
				searchDeeper(chain, incompleteChains, edge.getReturnType());
			}
		}
	}

	/**
	 * Returns the potentially incomplete list of call chains that could be found before a time out
	 * happened. The contents of this list are mutable and may change as the search makes progress.
	 *
	 * @return The list of call chains
	 */
	public List<Chain> getChains() {
		return chains;
	}

	private static LinkedList<LinkedList<ChainElement>> prepareQueue(final List<ChainElement> entrypoints) {
		final LinkedList<LinkedList<ChainElement>> incompleteChains= new LinkedList<>();
		for (final ChainElement entrypoint : entrypoints) {
			final LinkedList<ChainElement> chain= new LinkedList<>();
			chain.add(entrypoint);
			incompleteChains.add(chain);
		}
		return incompleteChains;
	}

	public static boolean isFromExcludedType(final List<String> excluded, final IBinding binding) {
		String tmp= String.valueOf(binding.getKey());
		int index= tmp.indexOf(";"); //$NON-NLS-1$
		final String key= index == -1 ? tmp : tmp.substring(0, index);
		return excluded.contains(key);
	}

	private boolean isValidEndOfChain(final ChainElement edge, final ITypeBinding expectedType,
			final int expectedDimension) {
		if (edge.getElementBinding().getKind() == IBinding.TYPE) {
			return false;
		}
		Boolean isAssignable= assignableCache.get(Collections.singletonMap(edge, expectedType));
		if (isAssignable == null) {
			isAssignable= TypeBindingAnalyzer.isAssignable(edge, expectedType, expectedDimension);
			assignableCache.put(Collections.singletonMap(edge, expectedType), isAssignable);
		}
		return isAssignable.booleanValue();
	}

	private void searchDeeper(final LinkedList<ChainElement> chain,
			final List<LinkedList<ChainElement>> incompleteChains, final ITypeBinding currentlyVisitedType) {
		boolean staticOnly= false;
		if (chain.getLast().getElementBinding().getKind() == IBinding.TYPE) {
			staticOnly= true;
		}
		for (final IBinding element : findAllFieldsAndMethods(currentlyVisitedType, staticOnly)) {
			final ChainElement newEdge= createEdge(element);
			if (!chain.contains(newEdge)) {
				incompleteChains.add(cloneChainAndAppendEdge(chain, newEdge));
			}
		}
	}

	private List<IBinding> findAllFieldsAndMethods(final ITypeBinding chainElementType, boolean staticOnly) {
		List<IBinding> cached= fieldsAndMethodsCache.get(Collections.singletonMap(chainElementType, staticOnly));
		if (cached == null) {
			cached= new LinkedList<>();
			Collection<IBinding> candidates= staticOnly
					? TypeBindingAnalyzer.findAllPublicStaticFieldsAndNonVoidNonPrimitiveStaticMethods(chainElementType, invocationSite)
					: TypeBindingAnalyzer.findVisibleInstanceFieldsAndRelevantInstanceMethods(chainElementType, invocationSite);
			for (final IBinding binding : candidates) {
				if (!ChainFinder.isFromExcludedType(excludedTypes, binding)) {
					cached.add(binding);
				}
			}
			fieldsAndMethodsCache.put(Collections.singletonMap(chainElementType, staticOnly), cached);
		}
		return cached;
	}

	private ChainElement createEdge(final IBinding member) {
		ChainElement cached= edgeCache.get(member);
		if (cached == null) {
			cached= new ChainElement(member, false);
			edgeCache.put(member, cached);
		}
		return cached;
	}

	private static LinkedList<ChainElement> cloneChainAndAppendEdge(final LinkedList<ChainElement> chain,
			final ChainElement newEdge) {
		@SuppressWarnings("unchecked")
		final LinkedList<ChainElement> chainCopy= (LinkedList<ChainElement>) chain.clone();
		chainCopy.add(newEdge);
		return chainCopy;
	}
}
