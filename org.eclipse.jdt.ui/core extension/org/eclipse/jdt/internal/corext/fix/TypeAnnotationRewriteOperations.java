/*******************************************************************************
 * Copyright (c) 2016 Till Brychcy and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Till Brychcy - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnnotatableType;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Dimension;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NameQualifiedType;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.ui.text.java.IProblemLocation;

public class TypeAnnotationRewriteOperations {
	static class MoveTypeAnnotationRewriteOperation extends CompilationUnitRewriteOperation {

		private IProblemLocation fProblem;

		private CompilationUnit fCompilationUnit;

		private ASTNode fNewAnnotationTarget;

		private Annotation fAnnotation;

		public MoveTypeAnnotationRewriteOperation(CompilationUnit compilationUnit, IProblemLocation problem) {
			fCompilationUnit= compilationUnit;
			fProblem= problem;
			CompilationUnit astRoot= fCompilationUnit;
			ASTNode selectedNode= fProblem.getCoveringNode(astRoot);

			if (selectedNode instanceof Annotation) {
				fAnnotation= (Annotation) selectedNode;
				fNewAnnotationTarget= determineNewAnnotationTarget(fAnnotation);
			} else {
				fAnnotation= null;
				fNewAnnotationTarget= null;
			}
		}

		public boolean isMove() {
			return fNewAnnotationTarget != null;
		}


		@Override
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModel linkedModel) throws CoreException {
			TextEditGroup group= createTextEditGroup(isMove() ? FixMessages.TypeAnnotationFix_move : FixMessages.TypeAnnotationFix_remove, cuRewrite);
			ASTRewrite astRewrite= cuRewrite.getASTRewrite();

			if (fAnnotation != null) {
				if (fNewAnnotationTarget != null) {
					Annotation newAnnotation= (Annotation) astRewrite.createMoveTarget(fAnnotation);
					addTypeAnnotation(astRewrite, fNewAnnotationTarget, newAnnotation, group);
				}
				astRewrite.remove(fAnnotation, group);
			}
		}

		/**
		 * @param annotation the Annotation that should be moved.
		 * @return The ASTNode that the annotation should be moved to: Either null or an
		 *         {@link AnnotatableType} or a {@link Dimension}
		 */
		public static ASTNode determineNewAnnotationTarget(Annotation annotation) {
			Type type;
			ASTNode parent= annotation.getParent();
			if (parent instanceof MethodDeclaration) {
				MethodDeclaration methodDeclaration= (MethodDeclaration) parent;
				List<Dimension> extraDimensions= methodDeclaration.extraDimensions();
				if (!extraDimensions.isEmpty()) {
					return extraDimensions.get(0);
				}
				type= methodDeclaration.getReturnType2();
			} else if (parent instanceof FieldDeclaration) {
				type= ((FieldDeclaration) parent).getType();
			} else if (parent instanceof SingleVariableDeclaration) {
				type= ((SingleVariableDeclaration) parent).getType();
			} else if (parent instanceof VariableDeclarationStatement) {
				type= ((VariableDeclarationStatement) parent).getType();
			} else if (parent instanceof SimpleType && parent.getParent() instanceof QualifiedType) {
				type= (Type) parent.getParent();
			} else {
				type= null;
			}
			if (type == null) {
				return null;
			}
			if (type.isPrimitiveType()) {
				// can only happen IProblem.IllegalAnnotationForBaseType
				return null;
			}

			if (type.isParameterizedType()) {
				ParameterizedType parameterizedType= (ParameterizedType) type;
				type= parameterizedType.getType();
			}
			if (type.isAnnotatable()) {
				return type;
			} else if (type.isArrayType()) {
				List<Dimension> dimensions= ((ArrayType) type).dimensions();
				if (!dimensions.isEmpty()) {
					return dimensions.get(0);
				}
			}
			return null;
		}
	}

	/**
	 * @param astRewrite the AstRewrite
	 * @param target must be either an AnnotatableType or a dimension
	 * @param newAnnotation the Annotation to be added
	 * @param group the TextEditGroup
	 */
	public static void addTypeAnnotation(ASTRewrite astRewrite, ASTNode target, Annotation newAnnotation, TextEditGroup group) {
		if (target instanceof AnnotatableType) {
			AnnotatableType annotatableType= (AnnotatableType) target;
			if(annotatableType.isSimpleType()) {
				// for cleanups, if another type annotation has been moved.
				annotatableType = (AnnotatableType) astRewrite.get(annotatableType.getParent(), annotatableType.getLocationInParent());
			}
			if (annotatableType.isSimpleType()) {
				SimpleType simpleType= (SimpleType) annotatableType;
				Name name2= simpleType.getName();
				assert name2.isQualifiedName();
				QualifiedName qualifiedName= (QualifiedName) name2;
				qualifiedName.getName();
				Name qualifier= (Name) astRewrite.createMoveTarget(qualifiedName.getQualifier());
				SimpleName name= (SimpleName) astRewrite.createMoveTarget(qualifiedName.getName());
				NameQualifiedType nameQualifiedType= astRewrite.getAST().newNameQualifiedType(qualifier, name);
				nameQualifiedType.annotations().add(newAnnotation);
				astRewrite.replace(annotatableType, nameQualifiedType, group);
			} else if (annotatableType.isNameQualifiedType()) {
				ListRewrite listRewrite= astRewrite.getListRewrite(annotatableType, NameQualifiedType.ANNOTATIONS_PROPERTY);
				listRewrite.insertLast(newAnnotation, group);
			} else if (annotatableType.isQualifiedType()) {
				ListRewrite listRewrite= astRewrite.getListRewrite(annotatableType, QualifiedType.ANNOTATIONS_PROPERTY);
				listRewrite.insertLast(newAnnotation, group);
			}
		} else if (target instanceof Dimension) {
			Dimension dimension= (Dimension) target;
			ListRewrite listRewrite= astRewrite.getListRewrite(dimension, Dimension.ANNOTATIONS_PROPERTY);
			listRewrite.insertLast(newAnnotation, group);
		}
	}

	private TypeAnnotationRewriteOperations() {
	}
}
