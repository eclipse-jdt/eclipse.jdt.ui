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

package org.eclipse.jdt.internal.corext.refactoring.typeconstraints2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

public class FullConstraintCreator2 extends ConstraintCreator2 {

	private TypeConstraintFactory2 fTypeConstraintFactory;

	public FullConstraintCreator2(TypeConstraintFactory2 tFactory) {
		fTypeConstraintFactory= tFactory;
	}
	
	public ITypeConstraint2[] create(SingleVariableDeclaration svd){
		ITypeConstraint2[] defines= fTypeConstraintFactory.createEqualsConstraint(
				fTypeConstraintFactory.makeExpressionOrTypeVariable(svd.getName()),
				fTypeConstraintFactory.makeTypeVariable(svd.getType()));
		if (svd.getInitializer() == null)
			return defines;	
		ITypeConstraint2[] constraints = fTypeConstraintFactory.createSubtypeConstraint(
				fTypeConstraintFactory.makeExpressionOrTypeVariable(svd.getInitializer()),
				fTypeConstraintFactory.makeExpressionOrTypeVariable(svd.getName()));
		if (defines.length == 0 && constraints.length == 0){
			return new ITypeConstraint2[0];
		} else if (defines.length == 0){
			return constraints;
		} else if (constraints.length == 0){
			return defines;
		} else {
			List all= new ArrayList();
			all.addAll(Arrays.asList(defines));
			all.addAll(Arrays.asList(constraints));
			return (ITypeConstraint2[])all.toArray();
		}
	}

	public ITypeConstraint2[] create(VariableDeclarationFragment vdf){
		if (vdf.getInitializer() == null)
			return new ITypeConstraint2[0];	
		return fTypeConstraintFactory.createSubtypeConstraint(
				fTypeConstraintFactory.makeExpressionOrTypeVariable(vdf.getInitializer()),
				fTypeConstraintFactory.makeExpressionOrTypeVariable(vdf.getName()));
	}

	public ITypeConstraint2[] create(VariableDeclarationExpression vde){
		return getConstraintsFromFragmentList(vde.fragments(), vde.getType());
	}
	
	public ITypeConstraint2[] create(VariableDeclarationStatement node) {
		return getConstraintsFromFragmentList(node.fragments(), node.getType());
	}
	
	//--------- private helpers ----------------//
	
	private ITypeConstraint2[] getConstraintsFromFragmentList(List fragments, Type type) {
		int size= fragments.size();
		ConstraintVariable2 typeVariable= fTypeConstraintFactory.makeTypeVariable(type);
		List result= new ArrayList((size * (size - 1))/2);
		for (int i= 0; i < size; i++) {
			VariableDeclarationFragment fragment1= (VariableDeclarationFragment) fragments.get(i);
			SimpleName fragment1Name= fragment1.getName();
			ITypeConstraint2[] fragment1DefinesConstraints= fTypeConstraintFactory.createEqualsConstraint(
					fTypeConstraintFactory.makeExpressionOrTypeVariable(fragment1Name),
					typeVariable);
			result.addAll(Arrays.asList(fragment1DefinesConstraints));
			for (int j= i + 1; j < size; j++) {
				VariableDeclarationFragment fragment2= (VariableDeclarationFragment) fragments.get(j);
				ITypeConstraint2[] fragment12equalsConstraints= fTypeConstraintFactory.createEqualsConstraint(
						fTypeConstraintFactory.makeExpressionOrTypeVariable(fragment1Name),
						fTypeConstraintFactory.makeExpressionOrTypeVariable(fragment2.getName()));
				result.addAll(Arrays.asList(fragment12equalsConstraints));
			}
		}
		return (ITypeConstraint2[]) result.toArray(new ITypeConstraint2[result.size()]);
	}
	
}
