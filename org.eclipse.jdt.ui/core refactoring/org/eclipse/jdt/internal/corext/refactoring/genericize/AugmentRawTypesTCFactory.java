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

package org.eclipse.jdt.internal.corext.refactoring.genericize;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.TypeConstraintFactory2;

public class AugmentRawTypesTCFactory extends TypeConstraintFactory2 {

//	private TypeHandle fCollection;

	private ITypeBinding fCollectionBinding;


	public AugmentRawTypesTCFactory(IJavaProject project) {
		ASTParser parser= ASTParser.newParser(AST.JLS3);
		
		String source= "class X {java.util.Collection c;}"; //$NON-NLS-1$
		parser.setSource(source.toCharArray());
		parser.setUnitName("X.java"); //$NON-NLS-1$
		parser.setProject(project);
		parser.setResolveBindings(true);
		CompilationUnit unit= (CompilationUnit) parser.createAST(null);
		TypeDeclaration type= (TypeDeclaration) unit.types().get(0);
		FieldDeclaration field= (FieldDeclaration) type.bodyDeclarations().get(0);
		fCollectionBinding= field.getType().resolveBinding();
	}
	
	
	//TODO: filter makeDeclaringTypeVariable, since that's not used?
	//-> would need to adapt create...Constraint methods to deal with null
	
	public boolean filterConstraintVariableType(ITypeBinding typeBinding) {
		if (super.filterConstraintVariableType(typeBinding))
			return true;
		
		return typeBinding.isPrimitive();
//		return TypeRules.canAssign(fCollectionBinding, typeBinding);
	}
	
}
