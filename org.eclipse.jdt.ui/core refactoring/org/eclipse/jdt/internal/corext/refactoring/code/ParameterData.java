/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.code;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.refactoring.code.flow.FlowInfo;

/* package */ class ParameterData {

	public static final String PROPERTY= ParameterData.class.getName();

	private SingleVariableDeclaration fDeclaration;
	private int fAccessMode;
	private List fReferences;

	public ParameterData(SingleVariableDeclaration decl) {
		super();
		fDeclaration= decl;
		fAccessMode= FlowInfo.UNUSED;
		fReferences= new ArrayList(2);
	}

	public String getName() {
		return fDeclaration.getName().getIdentifier();
	}
	
	public String getTypeName() {
		ITypeBinding binding= getTypeBinding();
		if (binding != null)
			return binding.getName();
		return ASTNodes.asString(fDeclaration.getType());
	}
	
	public ITypeBinding getTypeBinding() {
		return fDeclaration.getType().resolveBinding();
	}
	
	public void addReference(ASTNode node) {
		fReferences.add(node);
	}
	
	public List references() {
		return fReferences;
	}
	
	public void setAccessMode(int mode) {
		fAccessMode= mode;
	}

	public boolean isUnused() {
		return fAccessMode == FlowInfo.UNUSED;
	}
	
	public boolean isReadOnly() {
		return (fAccessMode & (FlowInfo.READ | FlowInfo.READ_POTENTIAL)) != 0;
	}
	
	public boolean isWrite() {
		return (fAccessMode & (FlowInfo.WRITE | FlowInfo.WRITE_POTENTIAL | FlowInfo.UNKNOWN)) != 0;
	}
	
	public int getSimplifiedAccessMode() {
		if (isWrite())
			return FlowInfo.WRITE;
		if (isReadOnly())
			return FlowInfo.READ;
		return FlowInfo.UNUSED;
	}
	
	public int getNumberOfAccesses() {
		return fReferences.size();
	}
	
	public boolean needsEvaluation() {
		if (fReferences.size() <= 1)
			return false;
		return true;
	}
}
