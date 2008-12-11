/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.corext.dom.LinkedNodeFinder;
import org.eclipse.jdt.internal.corext.refactoring.generics.InferTypeArgumentsConstraintCreator;
import org.eclipse.jdt.internal.corext.refactoring.generics.InferTypeArgumentsConstraintsSolver;
import org.eclipse.jdt.internal.corext.refactoring.generics.InferTypeArgumentsRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.generics.InferTypeArgumentsTCModel;
import org.eclipse.jdt.internal.corext.refactoring.generics.InferTypeArgumentsUpdate;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.correction.ProblemLocation;
import org.eclipse.jdt.internal.ui.viewsupport.BasicElementLabels;

/**
 * Fix which introduce new language constructs to pre Java50 code.
 * Requires a compiler level setting of 5.0+
 * Supported:
 * 		Add missing @Override annotation
 * 		Add missing @Deprecated annotation
 * 		Convert for loop to enhanced for loop
 */
public class Java50Fix extends CompilationUnitRewriteOperationsFix {

	private static final String OVERRIDE= "Override"; //$NON-NLS-1$
	private static final String DEPRECATED= "Deprecated"; //$NON-NLS-1$

	private static class AnnotationRewriteOperation extends CompilationUnitRewriteOperation {
		private final BodyDeclaration fBodyDeclaration;
		private final String fAnnotation;

		public AnnotationRewriteOperation(BodyDeclaration bodyDeclaration, String annotation) {
			fBodyDeclaration= bodyDeclaration;
			fAnnotation= annotation;
		}

		/**
		 * {@inheritDoc}
		 */
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModel model) throws CoreException {
			AST ast= cuRewrite.getRoot().getAST();
			ListRewrite listRewrite= cuRewrite.getASTRewrite().getListRewrite(fBodyDeclaration, fBodyDeclaration.getModifiersProperty());
			Annotation newAnnotation= ast.newMarkerAnnotation();
			newAnnotation.setTypeName(ast.newSimpleName(fAnnotation));
			TextEditGroup group= createTextEditGroup(Messages.format(FixMessages.Java50Fix_AddMissingAnnotation_description, BasicElementLabels.getJavaElementName(fAnnotation)), cuRewrite);
			listRewrite.insertFirst(newAnnotation, group);
		}
	}

	private static class AddTypeParametersOperation extends CompilationUnitRewriteOperation {

		private final SimpleType[] fTypes;

		public AddTypeParametersOperation(SimpleType[] types) {
			fTypes= types;
		}

		/**
		 * {@inheritDoc}
		 */
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModel positionGroups) throws CoreException {
			InferTypeArgumentsTCModel model= new InferTypeArgumentsTCModel();
			InferTypeArgumentsConstraintCreator creator= new InferTypeArgumentsConstraintCreator(model, true);

			CompilationUnit root= cuRewrite.getRoot();
			root.accept(creator);

			InferTypeArgumentsConstraintsSolver solver= new InferTypeArgumentsConstraintsSolver(model);
			InferTypeArgumentsUpdate update= solver.solveConstraints(new NullProgressMonitor());
			solver= null; //free caches

			ASTNode[] nodes= InferTypeArgumentsRefactoring.inferArguments(fTypes, update, model, cuRewrite);
			if (nodes.length == 0)
				return;

			ASTRewrite astRewrite= cuRewrite.getASTRewrite();
			for (int i= 0; i < nodes.length; i++) {
				if (nodes[i] instanceof ParameterizedType) {
					ParameterizedType type= (ParameterizedType)nodes[0];
					List args= (List)type.getStructuralProperty(ParameterizedType.TYPE_ARGUMENTS_PROPERTY);
					int j= 0;
					for (Iterator iter= args.iterator(); iter.hasNext();) {
						LinkedProposalPositionGroup group= new LinkedProposalPositionGroup("G" + i + "_" + j); //$NON-NLS-1$ //$NON-NLS-2$
						Type argType= (Type)iter.next();
						if (!positionGroups.hasLinkedPositions()) {
							group.addPosition(astRewrite.track(argType), true);
						} else {
							group.addPosition(astRewrite.track(argType), false);
						}
						positionGroups.addPositionGroup(group);
						j++;
					}
				}
			}
			positionGroups.setEndPosition(astRewrite.track(nodes[0]));
		}
	}

	public static Java50Fix createAddOverrideAnnotationFix(CompilationUnit compilationUnit, IProblemLocation problem) {
		if (problem.getProblemId() != IProblem.MissingOverrideAnnotation)
			return null;

		return createFix(compilationUnit, problem, OVERRIDE, FixMessages.Java50Fix_AddOverride_description);
	}

	public static Java50Fix createAddDeprectatedAnnotation(CompilationUnit compilationUnit, IProblemLocation problem) {
		if (!isMissingDeprecationProblem(problem))
			return null;

		return createFix(compilationUnit, problem, DEPRECATED, FixMessages.Java50Fix_AddDeprecated_description);
	}

	public static boolean isMissingDeprecationProblem(IProblemLocation problem) {
		int id= problem.getProblemId();
		if (id == IProblem.FieldMissingDeprecatedAnnotation)
			return true;

		if (id == IProblem.MethodMissingDeprecatedAnnotation)
			return true;

		if (id == IProblem.TypeMissingDeprecatedAnnotation)
			return true;

		return false;
	}

	private static Java50Fix createFix(CompilationUnit compilationUnit, IProblemLocation problem, String annotation, String label) {
		ICompilationUnit cu= (ICompilationUnit)compilationUnit.getJavaElement();
		if (!JavaModelUtil.is50OrHigher(cu.getJavaProject()))
			return null;

		ASTNode selectedNode= problem.getCoveringNode(compilationUnit);
		if (selectedNode == null)
			return null;

		ASTNode declaringNode= getDeclaringNode(selectedNode);
		if (!(declaringNode instanceof BodyDeclaration))
			return null;

		BodyDeclaration declaration= (BodyDeclaration) declaringNode;

		AnnotationRewriteOperation operation= new AnnotationRewriteOperation(declaration, annotation);

		return new Java50Fix(label, compilationUnit, new CompilationUnitRewriteOperation[] {operation});
	}

	public static Java50Fix createRawTypeReferenceFix(CompilationUnit compilationUnit, IProblemLocation problem) {
		List operations= new ArrayList();
		SimpleType node= createRawTypeReferenceOperations(compilationUnit, new IProblemLocation[] {problem}, operations);
		if (operations.size() == 0)
			return null;

		return new Java50Fix(Messages.format(FixMessages.Java50Fix_AddTypeArguments_description,  BasicElementLabels.getJavaElementName(node.getName().getFullyQualifiedName())), compilationUnit, (CompilationUnitRewriteOperation[])operations.toArray(new CompilationUnitRewriteOperation[operations.size()]));
	}

	public static ICleanUpFix createCleanUp(CompilationUnit compilationUnit,
			boolean addOverrideAnnotation,
			boolean addDeprecatedAnnotation,
			boolean rawTypeReference) {

		ICompilationUnit cu= (ICompilationUnit)compilationUnit.getJavaElement();
		if (!JavaModelUtil.is50OrHigher(cu.getJavaProject()))
			return null;

		if (!addOverrideAnnotation && !addDeprecatedAnnotation && !rawTypeReference)
			return null;

		List/*<CompilationUnitRewriteOperation>*/ operations= new ArrayList();

		IProblem[] problems= compilationUnit.getProblems();
		IProblemLocation[] locations= new IProblemLocation[problems.length];
		for (int i= 0; i < problems.length; i++) {
			locations[i]= new ProblemLocation(problems[i]);
		}

		if (addOverrideAnnotation)
			createAddOverrideAnnotationOperations(compilationUnit, locations, operations);

		if (addDeprecatedAnnotation)
			createAddDeprecatedAnnotationOperations(compilationUnit, locations, operations);

		if (rawTypeReference)
			createRawTypeReferenceOperations(compilationUnit, locations, operations);

		if (operations.size() == 0)
			return null;

		String fixName;
		if (rawTypeReference) {
			fixName= FixMessages.Java50Fix_add_type_parameters_change_name;
		} else {
			fixName= FixMessages.Java50Fix_add_annotations_change_name;
		}

		CompilationUnitRewriteOperation[] operationsArray= (CompilationUnitRewriteOperation[])operations.toArray(new CompilationUnitRewriteOperation[operations.size()]);
		return new Java50Fix(fixName, compilationUnit, operationsArray);
	}

	public static ICleanUpFix createCleanUp(CompilationUnit compilationUnit, IProblemLocation[] problems,
			boolean addOverrideAnnotation,
			boolean addDeprecatedAnnotation,
			boolean rawTypeReferences) {

		ICompilationUnit cu= (ICompilationUnit)compilationUnit.getJavaElement();
		if (!JavaModelUtil.is50OrHigher(cu.getJavaProject()))
			return null;

		if (!addOverrideAnnotation && !addDeprecatedAnnotation && !rawTypeReferences)
			return null;

		List/*<CompilationUnitRewriteOperation>*/ operations= new ArrayList();

		if (addOverrideAnnotation)
			createAddOverrideAnnotationOperations(compilationUnit, problems, operations);

		if (addDeprecatedAnnotation)
			createAddDeprecatedAnnotationOperations(compilationUnit, problems, operations);

		if (rawTypeReferences)
			createRawTypeReferenceOperations(compilationUnit, problems, operations);


		if (operations.size() == 0)
			return null;

		CompilationUnitRewriteOperation[] operationsArray= (CompilationUnitRewriteOperation[])operations.toArray(new CompilationUnitRewriteOperation[operations.size()]);
		return new Java50Fix(FixMessages.Java50Fix_add_annotations_change_name, compilationUnit, operationsArray);
	}

	private static void createAddDeprecatedAnnotationOperations(CompilationUnit compilationUnit, IProblemLocation[] locations, List result) {
		for (int i= 0; i < locations.length; i++) {
			int id= locations[i].getProblemId();

			if (id == IProblem.FieldMissingDeprecatedAnnotation ||
				id == IProblem.MethodMissingDeprecatedAnnotation ||
				id == IProblem.TypeMissingDeprecatedAnnotation) {

				IProblemLocation problem= locations[i];

				ASTNode selectedNode= problem.getCoveringNode(compilationUnit);
				if (selectedNode != null) {

					ASTNode declaringNode= getDeclaringNode(selectedNode);
					if (declaringNode instanceof BodyDeclaration) {
						BodyDeclaration declaration= (BodyDeclaration) declaringNode;
						AnnotationRewriteOperation operation= new AnnotationRewriteOperation(declaration, DEPRECATED);
						result.add(operation);
					}
				}
			}
		}
	}

	private static void createAddOverrideAnnotationOperations(CompilationUnit compilationUnit, IProblemLocation[] locations, List result) {
		for (int i= 0; i < locations.length; i++) {

			if (locations[i].getProblemId() == IProblem.MissingOverrideAnnotation) {

				IProblemLocation problem= locations[i];

				ASTNode selectedNode= problem.getCoveringNode(compilationUnit);
				if (selectedNode != null) {

					ASTNode declaringNode= getDeclaringNode(selectedNode);
					if (declaringNode instanceof BodyDeclaration) {
						BodyDeclaration declaration= (BodyDeclaration) declaringNode;
						AnnotationRewriteOperation operation= new AnnotationRewriteOperation(declaration, OVERRIDE);
						result.add(operation);
					}
				}
			}
		}
	}

	private static SimpleType createRawTypeReferenceOperations(CompilationUnit compilationUnit, IProblemLocation[] locations, List operations) {
		if (hasFatalError(compilationUnit))
			return null;

		List/*<SimpleType>*/ result= new ArrayList();
		for (int i= 0; i < locations.length; i++) {
			IProblemLocation problem= locations[i];

			if (isRawTypeReference(problem)) {
				ASTNode node= problem.getCoveredNode(compilationUnit);
				if (node instanceof ClassInstanceCreation) {
					ASTNode rawReference= (ASTNode)node.getStructuralProperty(ClassInstanceCreation.TYPE_PROPERTY);
					if (isRawTypeReference(rawReference)) {
						result.add(rawReference);
					}
				} else if (node instanceof SimpleName) {
					ASTNode rawReference= node.getParent();
					if (isRawTypeReference(rawReference)) {
						if (!(rawReference.getParent() instanceof ArrayType))
							result.add(rawReference);
					}
				} else if (node instanceof MethodInvocation) {
					MethodInvocation invocation= (MethodInvocation)node;

					ASTNode rawReference= getRawReference(invocation, compilationUnit);
					if (rawReference != null) {
						result.add(rawReference);
					}
				}
			}
		}

		if (result.size() == 0)
			return null;

		SimpleType[] types= (SimpleType[])result.toArray(new SimpleType[result.size()]);
		operations.add(new AddTypeParametersOperation(types));
		return types[0];
	}

	private static boolean hasFatalError(CompilationUnit compilationUnit) {
		try {
			if (!((ICompilationUnit) compilationUnit.getJavaElement()).isStructureKnown())
				return true;
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
			return true;
		}

		IProblem[] problems= compilationUnit.getProblems();
		for (int i= 0; i < problems.length; i++) {
			if (problems[i].isError()) {
				if (!(problems[i] instanceof CategorizedProblem))
					return true;

				CategorizedProblem categorizedProblem= (CategorizedProblem) problems[i];
				int categoryID= categorizedProblem.getCategoryID();

				if (categoryID == CategorizedProblem.CAT_BUILDPATH)
					return true;
				if (categoryID == CategorizedProblem.CAT_SYNTAX)
					return true;
				if (categoryID == CategorizedProblem.CAT_IMPORT)
					return true;
				if (categoryID == CategorizedProblem.CAT_TYPE)
					return true;
				if (categoryID == CategorizedProblem.CAT_MEMBER)
					return true;
				if (categoryID == CategorizedProblem.CAT_INTERNAL)
					return true;
			}
		}

		return false;
	}

	public static boolean isRawTypeReference(IProblemLocation problem) {
		int id= problem.getProblemId();
		return id == IProblem.UnsafeTypeConversion || id == IProblem.RawTypeReference || id == IProblem.UnsafeRawMethodInvocation;
	}

	private static ASTNode getRawReference(MethodInvocation invocation, CompilationUnit compilationUnit) {
		Name name1= (Name)invocation.getStructuralProperty(MethodInvocation.NAME_PROPERTY);
		if (name1 instanceof SimpleName) {
			ASTNode rawReference= getRawReference((SimpleName)name1, compilationUnit);
			if (rawReference != null) {
				return rawReference;
			}
		}

		Expression expr= (Expression)invocation.getStructuralProperty(MethodInvocation.EXPRESSION_PROPERTY);
		if (expr instanceof SimpleName) {
			ASTNode rawReference= getRawReference((SimpleName)expr, compilationUnit);
			if (rawReference != null) {
				return rawReference;
			}
		} else if (expr instanceof QualifiedName) {
			Name name= (Name)expr;
			while (name instanceof QualifiedName) {
				SimpleName simpleName= (SimpleName)name.getStructuralProperty(QualifiedName.NAME_PROPERTY);
				ASTNode rawReference= getRawReference(simpleName, compilationUnit);
				if (rawReference != null) {
					return rawReference;
				}
				name= (Name)name.getStructuralProperty(QualifiedName.QUALIFIER_PROPERTY);
			}
			if (name instanceof SimpleName) {
				ASTNode rawReference= getRawReference((SimpleName)name, compilationUnit);
				if (rawReference != null) {
					return rawReference;
				}
			}
		} else if (expr instanceof MethodInvocation) {
			ASTNode rawReference= getRawReference((MethodInvocation)expr, compilationUnit);
			if (rawReference != null) {
				return rawReference;
			}
		}
		return null;
	}

	private static ASTNode getRawReference(SimpleName name, CompilationUnit compilationUnit) {
		SimpleName[] names= LinkedNodeFinder.findByNode(compilationUnit, name);
		for (int j= 0; j < names.length; j++) {
			if (names[j].getParent() instanceof VariableDeclarationFragment) {
				VariableDeclarationFragment fragment= (VariableDeclarationFragment)names[j].getParent();
				if (fragment.getParent() instanceof VariableDeclarationStatement) {
					VariableDeclarationStatement statement= (VariableDeclarationStatement)fragment.getParent();
					ASTNode result= (ASTNode)statement.getStructuralProperty(VariableDeclarationStatement.TYPE_PROPERTY);
					if (isRawTypeReference(result))
						return result;
				} else if (fragment.getParent() instanceof FieldDeclaration) {
					FieldDeclaration declaration= (FieldDeclaration)fragment.getParent();
					ASTNode result= (ASTNode)declaration.getStructuralProperty(FieldDeclaration.TYPE_PROPERTY);
					if (isRawTypeReference(result))
						return result;
				}
			} else if (names[j].getParent() instanceof SingleVariableDeclaration) {
				SingleVariableDeclaration declaration= (SingleVariableDeclaration)names[j].getParent();
				ASTNode result= (ASTNode)declaration.getStructuralProperty(SingleVariableDeclaration.TYPE_PROPERTY);
				if (isRawTypeReference(result))
					return result;
			} else if (names[j].getParent() instanceof MethodDeclaration) {
				MethodDeclaration methodDecl= (MethodDeclaration)names[j].getParent();
				ASTNode result= (ASTNode)methodDecl.getStructuralProperty(MethodDeclaration.RETURN_TYPE2_PROPERTY);
				if (isRawTypeReference(result))
					return result;
			}
		}
		return null;
	}

	private static boolean isRawTypeReference(ASTNode node) {
		if (!(node instanceof SimpleType))
			return false;

		ITypeBinding typeBinding= ((SimpleType) node).resolveBinding();
		if (typeBinding == null)
			return false;

		ITypeBinding binding= typeBinding.getTypeDeclaration();
		if (binding == null)
			return false;

		ITypeBinding[] parameters= binding.getTypeParameters();
		if (parameters.length == 0)
			return false;

		return true;
	}

	private static ASTNode getDeclaringNode(ASTNode selectedNode) {
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

	private Java50Fix(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperation[] fixRewrites) {
		super(name, compilationUnit, fixRewrites);
	}

}
