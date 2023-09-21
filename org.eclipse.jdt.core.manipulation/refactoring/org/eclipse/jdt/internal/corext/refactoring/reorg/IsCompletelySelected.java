/*******************************************************************************
 * Copyright (c) 2017 Simeon Andreev and others.
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
package org.eclipse.jdt.internal.corext.refactoring.reorg;

import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;

import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;

/**
 * Given a set of selected {@link IPackageFragment packages}, determines whether a specific package
 * and all of its sub-packages are selected.
 *
 * <p>
 * Constructs in {@code O(n)}, where {@code n} is the total number of packages in the
 * {@link IPackageFragmentRoot package roots} of the provided set. A single query runs in expected
 * {@code O(1)}.
 * </p>
 *
 * @author Simeon Andreev
 *
 * @see IPackageFragmentRoot
 * @see IPackageFragment
 */
public class IsCompletelySelected implements Predicate<IPackageFragment> {

	private final Set<IPackageFragment> completelySelectedPackages;


	/**
	 * @param selectedPackages the selected packages
	 * @throws JavaModelException if retrieving sub-packages failed
	 */
	public IsCompletelySelected(Collection<IPackageFragment> selectedPackages) throws JavaModelException {
		this(selectedPackages, new NullProgressMonitor());
	}

	/**
	 * @param selectedPackages the selected packages
	 * @param monitor may be null
	 * @throws JavaModelException if retrieving sub-packages failed
	 */
	IsCompletelySelected(Collection<IPackageFragment> selectedPackages, IProgressMonitor monitor) throws JavaModelException {
		completelySelectedPackages= completelySelectedPackages(selectedPackages, monitor);
	}

	private static Set<IPackageFragment> completelySelectedPackages(Collection<IPackageFragment> selectedPackages, IProgressMonitor monitor) throws JavaModelException {
		SubMonitor subMonitor= SubMonitor.convert(monitor, selectedPackages.size());

		// we group the selected packages by package root, to hopefully work with smaller containers
		Map<IPackageFragmentRoot, Set<IPackageFragment>> packagesByRoot= groupByPackageRoot(selectedPackages, subMonitor);
		Set<IPackageFragment> completelySelectedPackages= new HashSet<>(selectedPackages.size());
		for (Entry<IPackageFragmentRoot, Set<IPackageFragment>> packages : packagesByRoot.entrySet()) {
			subMonitor.checkCanceled();

			IPackageFragmentRoot root= packages.getKey();
			Set<IPackageFragment> selectedInRoot= packages.getValue();

			Set<IPackageFragment> completelySelectedPackagesOfRoot= completelySelectedPackages(root, selectedInRoot, subMonitor);
			completelySelectedPackages.addAll(completelySelectedPackagesOfRoot);

			subMonitor.worked(1);
		}

		return completelySelectedPackages;
	}

	/**
	 * Groups the specified packages by their package roots.
	 *
	 * @param packages the packages to group
	 * @param monitor may be null
	 * @return a mapping to the specified packages, from their package roots
	 */
	private static Map<IPackageFragmentRoot, Set<IPackageFragment>> groupByPackageRoot(Collection<IPackageFragment> packages, IProgressMonitor monitor) {
		SubMonitor subMonitor= SubMonitor.convert(monitor, packages.size());

		Map<IPackageFragmentRoot, Set<IPackageFragment>> packageRoots= new HashMap<>();
		for (IPackageFragment packageFragment : packages) {
			subMonitor.checkCanceled();

			IPackageFragmentRoot root= (IPackageFragmentRoot) packageFragment.getParent();
			Set<IPackageFragment> packagesOfRoot= packageRoots.get(root);

			if (packagesOfRoot == null) {
				packagesOfRoot= new HashSet<>();
				packageRoots.put(root, packagesOfRoot);
			}

			packagesOfRoot.add(packageFragment);

			subMonitor.worked(1);
		}

		return packageRoots;
	}

	/**
	 * For the specified set of selected packages computes the subset of <b>fully</b> selected packages,
	 * i.e. the resulting subset contains only packages for which all sub-packages are also selected.
	 *
	 * <p>
	 * Example:
	 * </p>
	 *
	 * <pre>
	 * a.b           -- selected
	 * a.b.c
	 * a.b.d         -- selected
	 * a.b.d.File1   -- selected
	 * a.b.d.File2   -- selected
	 * </pre>
	 *
	 * The resulting set will contain package {@code a.b.d} but not {@code a.b}, since the child
	 * {@code a.b.c} of {@code a.b} is not selected.
	 *
	 *
	 * @param root the package root of the selected packages
	 * @param selectedPackages the set of selected packages
	 * @param subMonitor may not be null
	 * @return the set of packages which have their entire sub-package structure selected
	 * @throws JavaModelException if accessing the packages of the root package fails
	 */
	private static Set<IPackageFragment> completelySelectedPackages(IPackageFragmentRoot root, Set<IPackageFragment> selectedPackages, SubMonitor subMonitor) throws JavaModelException {
		subMonitor.checkCanceled();
		Set<IPackageFragment> allPackages= allPackages(root);
		int numberOfPackages= allPackages.size();

		subMonitor.checkCanceled();
		Set<IPackageFragment> unselectedPackages= allPackages;
		unselectedPackages.removeAll(selectedPackages);

		subMonitor.checkCanceled();
		Set<IPackageFragment> completelySelectedPackages= new HashSet<>(selectedPackages);

		Set<IPackageFragment> visited= new HashSet<>(numberOfPackages);
		Deque<IPackageFragment> queue= new LinkedList<>(unselectedPackages);

		// go over all packages which are not selected, and remove their parents (recursively) from the set of fully selected packages
		while (!queue.isEmpty()) {
			subMonitor.checkCanceled();

			IPackageFragment unselectedOrPartiallySelectedFragment= queue.removeLast();
			completelySelectedPackages.remove(unselectedOrPartiallySelectedFragment);

			boolean notVisited= visited.add(unselectedOrPartiallySelectedFragment);
			if (notVisited) {
				IPackageFragment parentPackage= JavaElementUtil.getParentSubpackage(unselectedOrPartiallySelectedFragment);
				if (parentPackage != null) {
					queue.add(parentPackage);
				}
			}
		}

		return completelySelectedPackages;
	}

	/**
	 * Retrieves the packages of the specified package root.
	 *
	 * @param root the root for which to fetch packages
	 * @return all packages in the root
	 * @throws JavaModelException if access to the packages of {@code root} fails
	 */
	private static Set<IPackageFragment> allPackages(IPackageFragmentRoot root) throws JavaModelException {
		return Arrays.stream(root.getChildren()).map(x -> (IPackageFragment)x).collect(Collectors.toSet());
	}

	/**
	 * Returns true if all sub-packages of the specified selected package, are selected as well.
	 *
	 * @param packageFragment the package
	 * @return true if this initially selected package is completely selected
	 */
	@Override
	public boolean test(IPackageFragment packageFragment) {
		return completelySelectedPackages.contains(packageFragment);
	}
}
