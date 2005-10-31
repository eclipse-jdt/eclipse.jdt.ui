/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.ltk.core.refactoring.TextChange;

/**
 * Fix which introduce new language constructs to pre Java50 code.
 * Requires a compiler level setting of 5.0+
 * Supported:
 * 		Add missing @Override annotation
 * 		Add missing @Deprecated annotation
 */
public class Java50Fix extends AbstractFix {
	
	private final AnnotationTuple[] fAnnotationTuples;

	public static class AnnotationTuple {
		private final BodyDeclaration fBodyDeclaration;
		private final String[] fAnnotations;

		public AnnotationTuple(BodyDeclaration bodyDeclaration, String[] annotations) {
			fBodyDeclaration= bodyDeclaration;
			fAnnotations= annotations;
		}

		public BodyDeclaration getBodyDeclaration() {
			return fBodyDeclaration;
		}

		public String[] getAnnotations() {
			return fAnnotations;
		}
	}
	
	public static Java50Fix createFix(CompilationUnit compilationUnit, IProblemLocation problem, boolean addOverrideAnnotation, boolean addDepricatedAnnotation) {
		ICompilationUnit cu= (ICompilationUnit)compilationUnit.getJavaElement();
		if (!JavaModelUtil.is50OrHigher(cu.getJavaProject()))
			return null;
		
		if (!addOverrideAnnotation && !addDepricatedAnnotation)
			return null;
		
		ASTNode selectedNode= problem.getCoveringNode(compilationUnit);
		if (selectedNode == null)
			return null;
		
		ASTNode declaringNode= getDeclaringNode(selectedNode);
		if (!(declaringNode instanceof BodyDeclaration)) 
			return null;
		
		List/*<String>*/ annotations= new ArrayList();
		
		String name= addAnnotations(problem, addOverrideAnnotation, addDepricatedAnnotation, annotations);
		
		if (annotations.isEmpty())
			return null;
		
		BodyDeclaration declaration= (BodyDeclaration) declaringNode;
		
		AnnotationTuple tuple= new AnnotationTuple(declaration, (String[])annotations.toArray(new String[annotations.size()]));
		
		return new Java50Fix(name, cu, new AnnotationTuple[] {tuple});
	}


	public static String addAnnotations(IProblemLocation problem, boolean addOverrideAnnotation, boolean addDepricatedAnnotation, List annotations) {
		String name= ""; //$NON-NLS-1$
		if (addOverrideAnnotation && isMissingOverride(problem)) {
			annotations.add("Override"); //$NON-NLS-1$
			name= FixMessages.Java50Fix_AddOverride_description;
		}
		
		if (addDepricatedAnnotation && isMissingDeprecated(problem)) {
			annotations.add("Deprecated"); //$NON-NLS-1$
			name= FixMessages.Java50Fix_AddDeprecated_description;
		}
		
		if (annotations.size() == 2) {
			name= FixMessages.Java50Fix_AddMissingAnnotations_description;
		}
		return name;
	}

	public static boolean isMissingOverride(IProblemLocation problem) {
		return problem.getProblemId() == IProblem.MissingOverrideAnnotation;
	}

	public static boolean isMissingDeprecated(IProblemLocation problem) {
		return problem.getProblemId() == IProblem.FieldMissingDeprecatedAnnotation ||
		problem.getProblemId() == IProblem.MethodMissingDeprecatedAnnotation ||
		problem.getProblemId() == IProblem.TypeMissingDeprecatedAnnotation;
	}
	
	public static ASTNode getDeclaringNode(ASTNode selectedNode) {
		ASTNode declaringNode= null;		
		if (selectedNode instanceof MethodDeclaration) {
			declaringNode= selectedNode;
		} else if (selectedNode instanceof SimpleName) {
			StructuralPropertyDescriptor locationInParent= selectedNode.getLocationInParent();
			if (locationInParent == MethodDeclaration.NAME_PROPERTY || locationInParent == TypeDeclaration.NAME_PROPERTY) {
				declaringNode= selectedNode.getParent();
			} else if (locationInParent == VariableDeclarationFragment.NAME_PROPERTY) {
				declaringNode= selectedNode.getParent().getParent();
			}
		}
		return declaringNode;
	}

	public Java50Fix(String name, ICompilationUnit compilationUnit, AnnotationTuple[] annotationTuples) {
		super(name, compilationUnit);
		fAnnotationTuples= annotationTuples;
	}

	public TextChange createChange() throws CoreException {
		if (fAnnotationTuples != null && fAnnotationTuples.length > 0) {
			AST ast= fAnnotationTuples[0].getBodyDeclaration().getAST();
			ASTRewrite rewrite= ASTRewrite.create(ast);
			List/*<TextEditGroup>*/ groups= new ArrayList();
			for (int i= 0; i < fAnnotationTuples.length; i++) {
				AnnotationTuple tuple= fAnnotationTuples[i];
				addAnnotation(tuple.getBodyDeclaration(), tuple.getAnnotations(), ast, rewrite, groups);	
			}
			
			TextEdit edit= applyEdits(getCompilationUnit(), rewrite, null);
			
			CompilationUnitChange result= new CompilationUnitChange(FixMessages.Java50Fix_AddMissingAnnotations_description, getCompilationUnit());
			result.setEdit(edit);
			
			for (Iterator iter= groups.iterator(); iter.hasNext();) {
				TextEditGroup group= (TextEditGroup)iter.next();
				result.addTextEditGroup(group);
			}
			
			return result;
			
		}
		return null;
	}
	
	private void addAnnotation(BodyDeclaration declaration, String[] annotationNames, AST ast, ASTRewrite rewrite, List textEditGroups) {
		ListRewrite listRewrite= rewrite.getListRewrite(declaration, declaration.getModifiersProperty());

		for (int i= 0; i < annotationNames.length; i++) {
			Annotation newAnnotation= ast.newMarkerAnnotation();
			newAnnotation.setTypeName(ast.newSimpleName(annotationNames[i]));
			TextEditGroup group= new TextEditGroup(MessageFormat.format(FixMessages.Java50Fix_AddMissingAnnotation_description, new Object[] {annotationNames[i]}));
			textEditGroups.add(group);
			listRewrite.insertFirst(newAnnotation, group);	
		}
	}

}
