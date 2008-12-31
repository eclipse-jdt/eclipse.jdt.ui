/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Dmitry Stalnov (dstalnov@fusionone.com) - contributed fix for
 *       bug "inline method - doesn't handle implicit cast" (see
 *       https://bugs.eclipse.org/bugs/show_bug.cgi?id=24941).
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.dom;

import org.eclipse.jdt.core.dom.ITypeBinding;

/**
 * The interface is used in type binding visiting algorithms.
 *
 * @see Bindings#visitHierarchy(ITypeBinding, TypeBindingVisitor)
 */
public interface TypeBindingVisitor {

	public boolean visit(ITypeBinding node);
}
