/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package org.eclipse.jdt.core.manipulation.internal.javadoc;

import java.util.ArrayList;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

/**
 * Implements the "Algorithm for Inheriting Method Comments" as specified for <a href=
 * "http://download.oracle.com/javase/1.4.2/docs/tooldocs/solaris/javadoc.html#inheritingcomments"
 * >1.4.2</a>, <a href=
 * "http://download.oracle.com/javase/1.5.0/docs/tooldocs/windows/javadoc.html#inheritingcomments"
 * >1.5</a>, and <a href=
 * "http://download.oracle.com/javase/6/docs/technotes/tools/windows/javadoc.html#inheritingcomments"
 * >1.6</a>.
 *
 * <p>
 * Unfortunately, the implementation is broken in Javadoc implementations since 1.5, see
 * <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6376959">Sun's bug</a>.
 * </p>
 *
 * <p>
 * We adhere to the spec.
 * </p>
 */
abstract class InheritDocVisitor {
	public static final Object STOP_BRANCH= new Object() {
		@Override
		public String toString() {
			return "STOP_BRANCH"; //$NON-NLS-1$
		}
	};

	public static final Object CONTINUE= new Object() {
		@Override
		public String toString() {
			return "CONTINUE"; //$NON-NLS-1$
		}
	};

	/**
	 * Visits a type and decides how the visitor should proceed.
	 *
	 * @param currType the current type
	 * @return
	 *         <ul>
	 *         <li>{@link #STOP_BRANCH} to indicate that no Javadoc has been found and visiting
	 *         super types should stop here</li>
	 *         <li>{@link #CONTINUE} to indicate that no Javadoc has been found and visiting super
	 *         types should continue</li>
	 *         <li>an {@link Object} or <code>null</code>, to indicate that visiting should be
	 *         cancelled immediately. The returned value is the result of
	 *         {@link #visitInheritDoc(IType, ITypeHierarchy)}</li>
	 *         </ul>
	 * @throws JavaModelException unexpected problem
	 * @see #visitInheritDoc(IType, ITypeHierarchy)
	 */
	public abstract Object visit(IType currType) throws JavaModelException;

	/**
	 * Visits the super types of the given <code>currentType</code>.
	 *
	 * @param currentType the starting type
	 * @param typeHierarchy a super type hierarchy that contains <code>currentType</code>
	 * @return the result from a call to {@link #visit(IType)}, or <code>null</code> if none of the
	 *         calls returned a result
	 * @throws JavaModelException unexpected problem
	 */
	public Object visitInheritDoc(IType currentType, ITypeHierarchy typeHierarchy) throws JavaModelException {
		ArrayList<IType> visited= new ArrayList<>();
		visited.add(currentType);
		Object result= visitInheritDocInterfaces(visited, currentType, typeHierarchy);
		if (result != InheritDocVisitor.CONTINUE)
			return result;

		IType superClass;
		if (currentType.isInterface())
			superClass= currentType.getJavaProject().findType("java.lang.Object"); //$NON-NLS-1$
		else
			superClass= typeHierarchy.getSuperclass(currentType);

		while (superClass != null && !visited.contains(superClass)) {
			result= visit(superClass);
			if (result == InheritDocVisitor.STOP_BRANCH) {
				return null;
			} else if (result == InheritDocVisitor.CONTINUE) {
				visited.add(superClass);
				result= visitInheritDocInterfaces(visited, superClass, typeHierarchy);
				if (result != InheritDocVisitor.CONTINUE)
					return result;
				else
					superClass= typeHierarchy.getSuperclass(superClass);
			} else {
				return result;
			}
		}

		return null;
	}

	/**
	 * Visits the super interfaces of the given type in the given hierarchy, thereby skipping
	 * already visited types.
	 *
	 * @param visited set of visited types
	 * @param currentType type whose super interfaces should be visited
	 * @param typeHierarchy type hierarchy (must include <code>currentType</code>)
	 * @return the result, or {@link #CONTINUE} if no result has been found
	 * @throws JavaModelException unexpected problem
	 */
	private Object visitInheritDocInterfaces(ArrayList<IType> visited, IType currentType, ITypeHierarchy typeHierarchy) throws JavaModelException {
		ArrayList<IType> toVisitChildren= new ArrayList<>();
		for (IType superInterface : typeHierarchy.getSuperInterfaces(currentType)) {
			if (visited.contains(superInterface))
				continue;
			visited.add(superInterface);
			Object result= visit(superInterface);
			if (result == InheritDocVisitor.STOP_BRANCH) {
				//skip
			} else if (result == InheritDocVisitor.CONTINUE) {
				toVisitChildren.add(superInterface);
			} else {
				return result;
			}
		}
		for (IType child : toVisitChildren) {
			Object result= visitInheritDocInterfaces(visited, child, typeHierarchy);
			if (result != InheritDocVisitor.CONTINUE)
				return result;
		}
		return InheritDocVisitor.CONTINUE;
	}
}
