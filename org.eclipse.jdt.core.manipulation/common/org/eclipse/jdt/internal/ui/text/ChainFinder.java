/**
 * Copyright (c) 2010, 2020 Darmstadt University of Technology and others.
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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.ui.text.ChainElement.ElementType;

public class ChainFinder {

	private final List<ChainType> expectedTypes;

	private final List<String> excludedTypes;

	private final IType receiverType;

	private final List<Chain> chains= new LinkedList<>();

	private final Map<IJavaElement, ChainElement> edgeCache= new HashMap<>();

	private final Map<String, List<IJavaElement>> fieldsAndMethodsCache= new HashMap<>();

	private final Map<String, Boolean> assignableCache= new HashMap<>();

	private volatile boolean isCanceled;

	public ChainFinder(final List<ChainType> expectedTypes, final List<String> excludedTypes,
			final IType receiverType) {
		this.expectedTypes= expectedTypes;
		this.excludedTypes= excludedTypes;
		this.receiverType= receiverType;
	}

	public void startChainSearch(final List<ChainElement> entrypoints, final int maxChains, final int minDepth,
			final int maxDepth) {
		for (final ChainType expected : expectedTypes) {
			if (expected != null && !ChainFinder.isFromExcludedType(excludedTypes, expected)) {
				ChainType expectedType= expected;
				int expectedDimension= 0;
				if (expectedType.getDimension() > 0) {
					expectedDimension= expectedType.getDimension();
				}
				searchChainsForExpectedType(expectedType, expectedDimension, entrypoints, maxChains, minDepth,
						maxDepth);
			}
		}
	}

	public void cancel() {
		isCanceled= true;
	}

	private void searchChainsForExpectedType(final ChainType expectedType, final int expectedDimensions,
			final List<ChainElement> entrypoints, final int maxChains, final int minDepth, final int maxDepth) {
		final LinkedList<LinkedList<ChainElement>> incompleteChains= prepareQueue(entrypoints);

		while (!incompleteChains.isEmpty() && !isCanceled) {
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

	public static boolean isFromExcludedType(final List<String> excluded, final IJavaElement element) {
		if (element instanceof IType) {
			return excluded.contains(((IType) element).getFullyQualifiedName());
		} else {
			return excluded.contains(element.getElementName());
		}
	}

	public static boolean isFromExcludedType(final List<String> excluded, final ChainType element) {
		if (element.getType() != null) {
			return isFromExcludedType(excluded, element.getType());
		}
		return excluded.contains(element.getPrimitiveType());
	}

	private boolean isValidEndOfChain(final ChainElement edge, final ChainType expectedType,
			final int expectedDimension) {
		if (edge.getElementType() == ElementType.TYPE) {
			return false;
		}
		if ((edge.getReturnType().getPrimitiveType() != null)) {
			return edge.getReturnType().getPrimitiveType().equals(expectedType.getPrimitiveType());
		}
		if (expectedType.getPrimitiveType() != null) {
			return expectedType.getPrimitiveType().equals(edge.getReturnType().getPrimitiveType());
		}
		Boolean isAssignable= assignableCache.get(edge.toString() + expectedType.toString());
		if (isAssignable == null) {
			isAssignable= ChainElementAnalyzer.isAssignable(edge, expectedType.getType(), expectedDimension);
			assignableCache.put(edge.toString() + expectedType.toString(), isAssignable);
		}
		return isAssignable;
	}

	private void searchDeeper(final LinkedList<ChainElement> chain,
			final List<LinkedList<ChainElement>> incompleteChains, final ChainType currentlyVisitedType) {
		boolean staticOnly= false;
		if (chain.getLast().getElementType() == ElementType.TYPE) {
			staticOnly= true;
		}

		for (final IJavaElement element : findAllFieldsAndMethods(currentlyVisitedType, staticOnly)) {
			final ChainElement newEdge= createEdge(element);
			if (newEdge.getElementType() != null && !chain.contains(newEdge)) {
				incompleteChains.add(cloneChainAndAppendEdge(chain, newEdge));
			}
		}
	}

	private List<IJavaElement> findAllFieldsAndMethods(final ChainType chainElementType, boolean staticOnly) {
		List<IJavaElement> cached= fieldsAndMethodsCache.get(chainElementType.toString() + Boolean.toString(staticOnly));
		if (cached == null) {
			cached= new LinkedList<>();
			Collection<IJavaElement> candidates= staticOnly
					? ChainElementAnalyzer.findAllPublicStaticFieldsAndNonVoidNonPrimitiveStaticMethods(chainElementType, new ChainType(receiverType))
					: ChainElementAnalyzer.findVisibleInstanceFieldsAndRelevantInstanceMethods(chainElementType, new ChainType(receiverType));
			for (final IJavaElement e : candidates) {
				if (!ChainFinder.isFromExcludedType(excludedTypes, e)) {
					cached.add(e);
				}
			}
			fieldsAndMethodsCache.put(chainElementType.toString() + Boolean.toString(staticOnly), cached);
		}
		return cached;
	}

	private ChainElement createEdge(final IJavaElement member) {
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
