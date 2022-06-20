/*******************************************************************************
 * Copyright (c) 2021, 2022 Carsten Hammer.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Carsten Hammer - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.common;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.SimpleName;

public class ExpectationTracer extends ConcurrentHashMap<ASTNode,SimpleName> implements HelperVisitorProvider<ASTNode,SimpleName,ExpectationTracer>{

	public Stack<Integer> stack = new Stack<>();

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	HelperVisitor<ExpectationTracer,ASTNode,SimpleName> hv;

	/**
	 *
	 */
	public ExpectationTracer() {
	}

	@Override
	public HelperVisitor<ExpectationTracer,ASTNode,SimpleName> getHelperVisitor() {
		return hv;
	}

	@Override
	public void setHelperVisitor(HelperVisitor<ExpectationTracer,ASTNode,SimpleName> hv) {
		this.hv=hv;
	}

	private void writeObject(ObjectOutputStream stream)
	        throws IOException {
	    stream.defaultWriteObject();
	}

	private void readObject(ObjectInputStream stream)
	        throws IOException, ClassNotFoundException {
	    stream.defaultReadObject();
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		return super.equals(o);
	}
}
