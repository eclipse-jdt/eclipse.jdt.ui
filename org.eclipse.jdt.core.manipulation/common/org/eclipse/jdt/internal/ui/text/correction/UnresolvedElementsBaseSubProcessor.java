/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
 *     Renaud Waldura &lt;renaud+eclipse@waldura.com&gt; - New class/interface with wizard
 *     Rabea Gransberger <rgransberger@gmx.de> - [quick fix] Fix several visibility issues - https://bugs.eclipse.org/394692
 *     Jens Reimann <jreimann@redhat.com> Bug 38201: [quick assist] Allow creating abstract method - https://bugs.eclipse.org/38201
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Optional;

import org.osgi.framework.Bundle;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.resource.DeleteResourceChange;
import org.eclipse.ltk.core.refactoring.resource.ResourceChange;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IModuleDescription;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTMatcher;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionMethodReference;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NameQualifiedType;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.ProvidesDirective;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchExpression;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.TypeLocation;
import org.eclipse.jdt.core.manipulation.ChangeCorrectionProposalCore;
import org.eclipse.jdt.core.manipulation.JavaManipulation;
import org.eclipse.jdt.core.manipulation.TypeKinds;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.jdt.core.search.IJavaSearchConstants;

import org.eclipse.jdt.internal.core.manipulation.BindingLabelProviderCore;
import org.eclipse.jdt.internal.core.manipulation.JavaElementLabelsCore;
import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.core.manipulation.dom.ASTResolving;
import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.codemanipulation.ContextSensitiveImportRewriteContext;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.corext.dom.ScopeAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.changes.ClasspathChange;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;
import org.eclipse.jdt.internal.corext.util.TypeFilter;

import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.correction.ASTRewriteCorrectionProposalCore;

import org.eclipse.jdt.internal.ui.text.correction.proposals.AddArgumentCorrectionProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.AddImportCorrectionProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.AddModuleRequiresCorrectionProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.AddTypeParameterProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.CastCorrectionProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ChangeMethodSignatureProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ChangeMethodSignatureProposalCore.ChangeDescription;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ChangeMethodSignatureProposalCore.EditDescription;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ChangeMethodSignatureProposalCore.InsertDescription;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ChangeMethodSignatureProposalCore.RemoveDescription;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ChangeMethodSignatureProposalCore.SwapDescription;
import org.eclipse.jdt.internal.ui.text.correction.proposals.LinkedCorrectionProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.NewAbstractMethodCorrectionProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.NewAnnotationMemberProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.NewMethodCorrectionProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.NewVariableCorrectionProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.QualifyTypeProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.RenameNodeCorrectionProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ReplaceCorrectionProposalCore;

public abstract class UnresolvedElementsBaseSubProcessor<T> {
	protected static final int CompositeChangeProposal= 1;
	protected static final int NewVariableProposal1= 100;
	protected static final int NewVariableProposal2= 101;
	protected static final int NewVariableProposal3= 102;
	protected static final int NewFieldForTypeProposal1= 200;
	protected static final int NewFieldForTypeProposal2= 210;
	protected static final int NewFieldForTypeProposal3= 211;
	protected static final int NewFieldForTypeProposal4= 220;
	protected static final int NewFieldForTypeProposal5= 221;
	protected static final int SimilarVariableProposal1= 300;
	protected static final int SimilarVariableProposal2= 310;
	protected static final int SimilarVariableProposal3= 320;
	protected static final int EnhancedForWithoutTypeProposal1= 400;
	protected static final int CopyAnnotationsJarProposal1= 500;
	protected static final int TypeRefChangeProposal1= 820;
	protected static final int TypeRefChangeProposal2= 830;
	protected static final int TypeRefChangeFullProposal1= 910;
	protected static final int NewTypeProposalsParams1= 1080;
	protected static final int RequiresModuleProposal1= 1180;
	protected static final int MethodProposal1= 1210;
	protected static final int MethodProposal2= 1280;
	protected static final int StaticImportFavoriteProposal1= 1310;
	protected static final int NewMethodProposal1= 1410;
	protected static final int NewMethodProposal2= 1420;
	protected static final int NewMethodProposal3= 1430;
	protected static final int NewMethodProposal4= 1450;
	protected static final int NewMethodProposal5= 1475;
	protected static final int NewMethodProposal6= 1480;
	protected static final int NewMethodProposal7= 1490;
	protected static final int MissingCastParentsProposal1= 1550;
	protected static final int ExistingParentCastProposal1= 1650;
	protected static final int MoreParametersProposal1= 1700;
	protected static final int MoreParametersProposal2= 1750;
	protected static final int MoreArgumentsProposal1= 1810;
	protected static final int MoreArgumentsProposal2= 1850;
	protected static final int EqualNumberOfParameters1= 1950;
	protected static final int EqualNumberOfParameters2= 1970;
	protected static final int EqualNumberOfParameters3= 1980;
	protected static final int AddQualifierToOuterProposal1= 2000;
	protected static final int ConstructorProposal1= 2100;
	protected static final int AmbiguosTypeReferenceProposal1= 2200;
	protected static final int ArrayAccessProposal1= 2300;
	protected static final int ArrayAccessProposal2= 2301;
	protected static final int AnnotationMemberProposal1= 2400;
	protected static final int AnnotationMemberProposal2= 2401;

	protected UnresolvedElementsBaseSubProcessor() {
	}

	private final String ADD_IMPORT_ID= "org.eclipse.jdt.ui.correction.addImport"; //$NON-NLS-1$

	public void collectVariableProposals(IInvocationContext context, IProblemLocation problem, IVariableBinding resolvedField, Collection<T> proposals) throws CoreException {

		ICompilationUnit cu= context.getCompilationUnit();

		CompilationUnit astRoot= context.getASTRoot();
		ASTNode selectedNode= problem.getCoveredNode(astRoot);
		if (selectedNode == null) {
			return;
		}

		// type that defines the variable
		ITypeBinding binding= null;
		ITypeBinding declaringTypeBinding= Bindings.getBindingOfParentTypeContext(selectedNode);
		if (declaringTypeBinding == null) {
			return;
		}

		// possible type kind of the node
		boolean suggestVariableProposals= true;
		int typeKind= 0;

		selectedNode= ASTNodes.getUnparenthesedExpression(selectedNode);


		Name node= null;

		switch (selectedNode.getNodeType()) {
			case ASTNode.SIMPLE_NAME:
				node= (SimpleName) selectedNode;
				ASTNode parent= node.getParent();
				StructuralPropertyDescriptor locationInParent= node.getLocationInParent();
				if (locationInParent == ExpressionMethodReference.EXPRESSION_PROPERTY) {
					typeKind= TypeKinds.REF_TYPES;
				} else if (locationInParent == MethodInvocation.EXPRESSION_PROPERTY) {
					if (JavaModelUtil.is1d8OrHigher(cu.getJavaProject())) {
						typeKind= TypeKinds.CLASSES | TypeKinds.INTERFACES | TypeKinds.ENUMS;
					} else {
						typeKind= TypeKinds.CLASSES;
					}
				} else if (locationInParent == FieldAccess.NAME_PROPERTY) {
					Expression expression= ((FieldAccess) parent).getExpression();
					if (expression != null) {
						binding= expression.resolveTypeBinding();
						if (binding == null) {
							node= null;
						}
					}
				} else if (parent instanceof SimpleType || parent instanceof NameQualifiedType) {
					suggestVariableProposals= false;
					typeKind= TypeKinds.REF_TYPES_AND_VAR;
				} else if (parent instanceof QualifiedName qualifiedParent) {
					Name qualifier= qualifiedParent.getQualifier();
					if (qualifier != node) {
						binding= qualifier.resolveTypeBinding();
					} else {
						typeKind= TypeKinds.REF_TYPES;
					}
					ASTNode outerParent= parent.getParent();
					while (outerParent instanceof QualifiedName) {
						outerParent= outerParent.getParent();
					}
					if (outerParent instanceof SimpleType || outerParent instanceof NameQualifiedType) {
						typeKind= TypeKinds.REF_TYPES;
						suggestVariableProposals= false;
					}
				} else if (locationInParent == SwitchCase.EXPRESSION_PROPERTY || locationInParent == SwitchCase.EXPRESSIONS2_PROPERTY) {
					ASTNode caseParent= node.getParent().getParent();
					ITypeBinding switchExp= null;
					if (caseParent instanceof SwitchStatement stmt) {
						switchExp= stmt.getExpression().resolveTypeBinding();
					} else if (caseParent instanceof SwitchExpression stmt) {
						switchExp= stmt.getExpression().resolveTypeBinding();
					}
					if (switchExp != null && switchExp.isEnum()) {
						binding= switchExp;
					}
				} else if (locationInParent == SuperFieldAccess.NAME_PROPERTY) {
					binding= declaringTypeBinding.getSuperclass();
				}
				break;
			case ASTNode.QUALIFIED_NAME:
				QualifiedName qualifierName= (QualifiedName) selectedNode;
				ITypeBinding qualifierBinding= qualifierName.getQualifier().resolveTypeBinding();
				if (qualifierBinding != null) {
					node= qualifierName.getName();
					binding= qualifierBinding;
				} else {
					node= qualifierName.getQualifier();
					typeKind= TypeKinds.REF_TYPES;
					suggestVariableProposals= node.isSimpleName();
				}
				if (selectedNode.getParent() instanceof SimpleType || selectedNode.getParent() instanceof NameQualifiedType) {
					typeKind= TypeKinds.REF_TYPES;
					suggestVariableProposals= false;
				}
				break;
			case ASTNode.FIELD_ACCESS:
				FieldAccess access= (FieldAccess) selectedNode;
				Expression expression= access.getExpression();
				if (expression != null) {
					binding= expression.resolveTypeBinding();
					if (binding != null) {
						node= access.getName();
					}
				}
				break;
			case ASTNode.SUPER_FIELD_ACCESS:
				binding= declaringTypeBinding.getSuperclass();
				node= ((SuperFieldAccess) selectedNode).getName();
				break;
			default:
		}

		if (node == null) {
			return;
		}

		// add type proposals
		if (typeKind != 0) {
			if (!JavaModelUtil.is50OrHigher(cu.getJavaProject())) {
				typeKind &= ~(TypeKinds.ANNOTATIONS | TypeKinds.ENUMS | TypeKinds.VARIABLES);
			}

			int relevance= Character.isUpperCase(ASTNodes.getSimpleNameIdentifier(node).charAt(0)) ? IProposalRelevance.VARIABLE_TYPE_PROPOSAL_1 : IProposalRelevance.VARIABLE_TYPE_PROPOSAL_2;
			addSimilarTypeProposals(typeKind, cu, node, relevance + 1, proposals);

			typeKind &= ~TypeKinds.ANNOTATIONS;
			collectNewTypeProposals(cu, node, typeKind, relevance, proposals);
			getReorgSubProcessor().addProjectSetupFixProposals(context, problem, node.getFullyQualifiedName(), proposals);
		}

		if (!suggestVariableProposals) {
			return;
		}

		SimpleName simpleName= node.isSimpleName() ? (SimpleName) node : ((QualifiedName) node).getName();
		boolean isWriteAccess= ASTResolving.isWriteAccess(node);

		// similar variables
		addSimilarVariableProposals(cu, astRoot, binding, resolvedField, simpleName, isWriteAccess, proposals);

		if (binding == null) {
			addStaticImportFavoriteProposals(context, simpleName, false, proposals);
		}

		if (resolvedField == null || binding == null || resolvedField.getDeclaringClass() != binding.getTypeDeclaration() && Modifier.isPrivate(resolvedField.getModifiers())) {

			// new fields
			addNewFieldProposals(cu, astRoot, binding, declaringTypeBinding, simpleName, isWriteAccess, proposals);

			// new parameters and local variables
			if (binding == null && !isParentSwitchCase(simpleName)) {
				addNewVariableProposals(cu, node, simpleName, proposals);
			}
		}
	}

	private void addNewVariableProposals(ICompilationUnit cu, Name node, SimpleName simpleName, Collection<T> proposals) {
		String name= simpleName.getIdentifier();
		BodyDeclaration bodyDeclaration= ASTResolving.findParentBodyDeclaration(node, true);
		int type= bodyDeclaration.getNodeType();
		if (type == ASTNode.METHOD_DECLARATION) {
			int relevance= StubUtility.hasParameterName(cu.getJavaProject(), name) ? IProposalRelevance.CREATE_PARAMETER_PREFIX_OR_SUFFIX_MATCH : IProposalRelevance.CREATE_PARAMETER;
			String label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_createparameter_description, BasicElementLabels.getJavaElementName(name));
			NewVariableCorrectionProposalCore core= new NewVariableCorrectionProposalCore(label, cu, NewVariableCorrectionProposalCore.PARAM, simpleName, null, relevance, false);
			T p= newVariableCorrectionProposalToT(core, NewVariableProposal1);
			if (p != null)
				proposals.add(p);
		}
		if (type == ASTNode.INITIALIZER || type == ASTNode.METHOD_DECLARATION && !ASTResolving.isInsideConstructorInvocation((MethodDeclaration) bodyDeclaration, node)) {
			int relevance= StubUtility.hasLocalVariableName(cu.getJavaProject(), name) ? IProposalRelevance.CREATE_LOCAL_PREFIX_OR_SUFFIX_MATCH : IProposalRelevance.CREATE_LOCAL;
			String label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_createlocal_description, BasicElementLabels.getJavaElementName(name));
			NewVariableCorrectionProposalCore core= new NewVariableCorrectionProposalCore(label, cu, NewVariableCorrectionProposalCore.LOCAL, simpleName, null, relevance, false);
			T p= newVariableCorrectionProposalToT(core, NewVariableProposal2);
			if (p != null)
				proposals.add(p);
		}

		if (node.getParent().getNodeType() == ASTNode.ASSIGNMENT) {
			Assignment assignment= (Assignment) node.getParent();
			if (assignment.getLeftHandSide() == node && assignment.getParent().getNodeType() == ASTNode.EXPRESSION_STATEMENT) {
				ASTNode statement= assignment.getParent();
				ASTRewrite rewrite= ASTRewrite.create(statement.getAST());
				if (ASTNodes.isControlStatementBody(assignment.getParent().getLocationInParent())) {
					rewrite.replace(statement, rewrite.getAST().newBlock(), null);
				} else {
					rewrite.remove(statement, null);
				}
				String label= CorrectionMessages.UnresolvedElementsSubProcessor_removestatement_description;
				ASTRewriteCorrectionProposalCore core= new ASTRewriteCorrectionProposalCore(label, cu, rewrite, IProposalRelevance.REMOVE_ASSIGNMENT);
				T proposal= rewriteProposalToT(core, NewVariableProposal3);
				if (proposal != null)
					proposals.add(proposal);
			}
		}
	}

	private void addNewFieldProposals(ICompilationUnit cu, CompilationUnit astRoot, ITypeBinding binding, ITypeBinding declaringTypeBinding, SimpleName simpleName, boolean isWriteAccess, Collection<T> proposals) throws JavaModelException {
		// new variables
		ICompilationUnit targetCU;
		ITypeBinding senderDeclBinding;
		if (binding != null) {
			senderDeclBinding= binding.getTypeDeclaration();
			targetCU= ASTResolving.findCompilationUnitForBinding(cu, astRoot, senderDeclBinding);
		} else { // binding is null for accesses without qualifier
			senderDeclBinding= declaringTypeBinding;
			targetCU= cu;
		}

		if (!senderDeclBinding.isFromSource() || targetCU == null) {
			return;
		}

		boolean mustBeConst= (ASTResolving.isInsideModifiers(simpleName) || isParentSwitchCase(simpleName)) ;

		addNewFieldForType(targetCU, binding, senderDeclBinding, simpleName, isWriteAccess, mustBeConst, proposals);

		if (binding == null && senderDeclBinding.isNested()) {
			ASTNode anonymDecl= astRoot.findDeclaringNode(senderDeclBinding);
			if (anonymDecl != null) {
				ITypeBinding bind= Bindings.getBindingOfParentType(anonymDecl.getParent());
				if (!bind.isAnonymous()) {
					addNewFieldForType(targetCU, bind, bind, simpleName, isWriteAccess, mustBeConst, proposals);
				}
			}
		}
	}

	private boolean isParentSwitchCase(SimpleName simpleName) {
		if (simpleName != null) {
			return (simpleName.getParent() instanceof SwitchCase);
		}
		return false;
	}

	private void addNewFieldForType(ICompilationUnit targetCU, ITypeBinding binding, ITypeBinding senderDeclBinding, SimpleName simpleName, boolean isWriteAccess, boolean mustBeConst, Collection<T> proposals) {
		String name= simpleName.getIdentifier();
		String nameLabel= BasicElementLabels.getJavaElementName(name);
		String label;
		if (senderDeclBinding.isEnum() && !isWriteAccess) {
			label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_createenum_description, new Object[] { nameLabel, ASTResolving.getTypeSignature(senderDeclBinding) });
			NewVariableCorrectionProposalCore core= new NewVariableCorrectionProposalCore(label, targetCU, NewVariableCorrectionProposalCore.ENUM_CONST, simpleName, senderDeclBinding, 10, false);
			T p1= newVariableCorrectionProposalToT(core, NewFieldForTypeProposal1);
			if (p1 != null)
				proposals.add(p1);
		} else {
			if (!mustBeConst) {
				int fieldRelevance= StubUtility.hasFieldName(targetCU.getJavaProject(), name) ? IProposalRelevance.CREATE_FIELD_PREFIX_OR_SUFFIX_MATCH : IProposalRelevance.CREATE_FIELD;
				int uid;
				if (binding == null) {
					label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_createfield_description, nameLabel);
					uid= NewFieldForTypeProposal2;
				} else {
					label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_createfield_other_description, new Object[] { nameLabel, ASTResolving.getTypeSignature(senderDeclBinding) } );
					uid= NewFieldForTypeProposal3;
				}
				NewVariableCorrectionProposalCore core= new NewVariableCorrectionProposalCore(label, targetCU, NewVariableCorrectionProposalCore.FIELD, simpleName, senderDeclBinding, fieldRelevance, false);
				T prop= newVariableCorrectionProposalToT(core, uid);
				if (prop != null)
					proposals.add(prop);
			}

			if (!isWriteAccess && !senderDeclBinding.isAnonymous()) {
				int uid;
				if (binding == null) {
					label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_createconst_description, nameLabel);
					uid= NewFieldForTypeProposal4;
				} else {
					label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_createconst_other_description, new Object[] { nameLabel, ASTResolving.getTypeSignature(senderDeclBinding) } );
					uid= NewFieldForTypeProposal5;
				}
				int constRelevance= StubUtility.hasConstantName(targetCU.getJavaProject(), name) ? IProposalRelevance.CREATE_CONSTANT_PREFIX_OR_SUFFIX_MATCH : IProposalRelevance.CREATE_CONSTANT;
				NewVariableCorrectionProposalCore core= new NewVariableCorrectionProposalCore(label, targetCU, NewVariableCorrectionProposalCore.CONST_FIELD, simpleName, senderDeclBinding, constRelevance, false);
				T prop= newVariableCorrectionProposalToT(core, uid);
				if (prop != null)
					proposals.add(prop);
			}
		}
	}

	private void addSimilarVariableProposals(ICompilationUnit cu, CompilationUnit astRoot, ITypeBinding binding, IVariableBinding resolvedField, SimpleName node, boolean isWriteAccess, Collection<T> proposals) {
		int kind= ScopeAnalyzer.VARIABLES | ScopeAnalyzer.CHECK_VISIBILITY;
		if (!isWriteAccess) {
			kind |= ScopeAnalyzer.METHODS; // also try to find similar methods
		}

		IBinding[] varsAndMethodsInScope= (new ScopeAnalyzer(astRoot)).getDeclarationsInScope(node, kind);
		if (varsAndMethodsInScope.length > 0) {
			// avoid corrections like int i= i;
			String otherNameInAssign= null;

			// help with x.getString() -> y.getString()
			String methodSenderName= null;
			String fieldSenderName= null;

			ASTNode parent= node.getParent();
			switch (parent.getNodeType()) {
				case ASTNode.VARIABLE_DECLARATION_FRAGMENT:
					// node must be initializer
					otherNameInAssign= ((VariableDeclarationFragment) parent).getName().getIdentifier();
					break;
				case ASTNode.ASSIGNMENT:
					Assignment assignment= (Assignment) parent;
					if (isWriteAccess && assignment.getRightHandSide() instanceof SimpleName rightSide) {
						otherNameInAssign= rightSide.getIdentifier();
					} else if (!isWriteAccess && assignment.getLeftHandSide() instanceof SimpleName leftSide) {
						otherNameInAssign= leftSide.getIdentifier();
					}
					break;
				case ASTNode.METHOD_INVOCATION:
					MethodInvocation inv= (MethodInvocation) parent;
					if (inv.getExpression() == node) {
						methodSenderName= inv.getName().getIdentifier();
					}
					break;
				case ASTNode.QUALIFIED_NAME:
					QualifiedName qualName= (QualifiedName) parent;
					if (qualName.getQualifier() == node) {
						fieldSenderName= qualName.getName().getIdentifier();
					}
					break;
			}


			ITypeBinding guessedType= ASTResolving.guessBindingForReference(node);

			ITypeBinding objectBinding= astRoot.getAST().resolveWellKnownType("java.lang.Object"); //$NON-NLS-1$
			String identifier= node.getIdentifier();
			boolean isInStaticContext= ASTResolving.isInStaticContext(node);
			ArrayList<T> newProposals= new ArrayList<>(51);

			loop: for (int i= 0; i < varsAndMethodsInScope.length && newProposals.size() <= 50; i++) {
				IBinding varOrMeth= varsAndMethodsInScope[i];
				if (varOrMeth instanceof IVariableBinding curr) {
					String currName= curr.getName();
					if (currName.equals(otherNameInAssign)) {
						continue loop;
					}
					if (resolvedField != null && Bindings.equals(resolvedField, curr)) {
						continue loop;
					}
					boolean isFinal= Modifier.isFinal(curr.getModifiers());
					if (isFinal && curr.isField() && isWriteAccess) {
						continue loop;
					}
					if (isInStaticContext && !Modifier.isStatic(curr.getModifiers()) && curr.isField()) {
						continue loop;
					}

					int relevance= IProposalRelevance.SIMILAR_VARIABLE_PROPOSAL;
					if (NameMatcher.isSimilarName(currName, identifier)) {
						relevance += 3; // variable with a similar name than the unresolved variable
					}
					if (currName.equalsIgnoreCase(identifier)) {
						relevance+= 5;
					}
					ITypeBinding varType= curr.getType();
					if (varType != null) {
						if (guessedType != null && guessedType != objectBinding) { // too many result with object
							// variable type is compatible with the guessed type
							if (!isWriteAccess && canAssign(varType, guessedType)
									|| isWriteAccess && canAssign(guessedType, varType)) {
								relevance += 2; // unresolved variable can be assign to this variable
							}
						}
						if (methodSenderName != null && hasMethodWithName(varType, methodSenderName)) {
							relevance += 2;
						}
						if (fieldSenderName != null && hasFieldWithName(varType, fieldSenderName)) {
							relevance += 2;
						}
					}

					if (relevance > 0) {
						String label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_changevariable_description, BasicElementLabels.getJavaElementName(currName));
						RenameNodeCorrectionProposalCore core= new RenameNodeCorrectionProposalCore(label, cu, node.getStartPosition(), node.getLength(), currName, relevance);
						T prop= renameNodeCorrectionProposalToT(core, SimilarVariableProposal1);
						if (prop != null)
							newProposals.add(prop);
					}
				} else if (varOrMeth instanceof IMethodBinding curr) {
					if (!curr.isConstructor() && guessedType != null && canAssign(curr.getReturnType(), guessedType)) {
						if (NameMatcher.isSimilarName(curr.getName(), identifier)) {
							AST ast= astRoot.getAST();
							ASTRewrite rewrite= ASTRewrite.create(ast);
							String label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_changetomethod_description, ASTResolving.getMethodSignature(curr));
							LinkedCorrectionProposalCore proposal= new LinkedCorrectionProposalCore(label, cu, rewrite, IProposalRelevance.CHANGE_TO_METHOD);
							MethodInvocation newInv= ast.newMethodInvocation();
							newInv.setName(ast.newSimpleName(curr.getName()));
							for (ITypeBinding parameterType : curr.getParameterTypes()) {
								ASTNode arg= ASTNodeFactory.newDefaultExpression(ast, parameterType);
								newInv.arguments().add(arg);
								proposal.addLinkedPosition(rewrite.track(arg), false, null);
							}
							rewrite.replace(node, newInv, null);
							T proposalT= linkedProposalToT(proposal, SimilarVariableProposal2);
							if (proposalT != null)
								newProposals.add(proposalT);
						}
					}
				}
			}
			if (newProposals.size() <= 50)
				proposals.addAll(newProposals);
		}
		if (binding != null && binding.isArray()) {
			String idLength= "length"; //$NON-NLS-1$
			String label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_changevariable_description, idLength);
			RenameNodeCorrectionProposalCore core= new RenameNodeCorrectionProposalCore(label, cu, node.getStartPosition(), node.getLength(), idLength, IProposalRelevance.CHANGE_VARIABLE);
			T prop= renameNodeProposalToT(core, SimilarVariableProposal3);
			if (prop != null)
				proposals.add(prop);
		}
	}

	private boolean canAssign(ITypeBinding returnType, ITypeBinding guessedType) {
		return returnType.isAssignmentCompatible(guessedType);
	}

	private boolean hasMethodWithName(ITypeBinding typeBinding, String name) {
		for (IVariableBinding field : typeBinding.getDeclaredFields()) {
			if (field.getName().equals(name)) {
				return true;
			}
		}
		ITypeBinding superclass= typeBinding.getSuperclass();
		if (superclass != null) {
			return hasMethodWithName(superclass, name);
		}
		return false;
	}

	private boolean hasFieldWithName(ITypeBinding typeBinding, String name) {
		for (IMethodBinding method : typeBinding.getDeclaredMethods()) {
			if (method.getName().equals(name)) {
				return true;
			}
		}
		ITypeBinding superclass= typeBinding.getSuperclass();
		if (superclass != null) {
			return hasMethodWithName(superclass, name);
		}
		return false;
	}

	private int evauateTypeKind(ASTNode node, IJavaProject project) {
		int kind= ASTResolving.getPossibleTypeKinds(node, JavaModelUtil.is50OrHigher(project));
		return kind;
	}


	public void collectTypeProposals(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) throws CoreException {
		ICompilationUnit cu= context.getCompilationUnit();

		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		if (selectedNode == null) {
			return;
		}

		IJavaProject javaProject= cu.getJavaProject();
		int kind= evauateTypeKind(selectedNode, javaProject);

		if (kind == TypeKinds.REF_TYPES) {
			SimpleName s= addEnhancedForWithoutTypeProposals(cu, selectedNode, proposals);
			if (s != null && Character.isLowerCase(s.getFullyQualifiedName().charAt(0))) {
				return;
			}
		}

		while (selectedNode.getLocationInParent() == QualifiedName.NAME_PROPERTY) {
			selectedNode= selectedNode.getParent();
		}

		Name node= null;
		if (selectedNode instanceof SimpleType selectedType) {
			node= selectedType.getName();
		} else if (selectedNode instanceof NameQualifiedType nameQualified) {
			node= nameQualified.getName();
		} else if (selectedNode instanceof ArrayType arr) {
			Type elementType= arr.getElementType();
			if (elementType.isSimpleType()) {
				node= ((SimpleType) elementType).getName();
			} else if (elementType.isNameQualifiedType()) {
				node= ((NameQualifiedType) elementType).getName();
			} else {
				return;
			}
		} else if (selectedNode instanceof Name) {
			node= (Name) selectedNode;
		} else {
			return;
		}

		if (node instanceof SimpleName && !JavaModelUtil.is11OrHigher(javaProject)) {
			boolean isVarTypeProblem= false;
			if (problem.getProblemId() == IProblem.VarIsNotAllowedHere) {
				isVarTypeProblem= true;
			} else {
				String[] args= problem.getProblemArguments();
				if (args != null && args.length > 0) {
					String name= args[0];
					if ("var".equals(name)) { //$NON-NLS-1$
						isVarTypeProblem= true;
					}
				}
			}
			if (isVarTypeProblem) {
				// check if "var" is present as lambda parameter type
				boolean isVarInLambdaParamType= false;
				ASTNode parent= node.getParent();
				if (parent instanceof SimpleType && parent.getLocationInParent() == SingleVariableDeclaration.TYPE_PROPERTY) {
					parent= parent.getParent();
					if (parent.getLocationInParent() == LambdaExpression.PARAMETERS_PROPERTY) {
						isVarInLambdaParamType= true;
					}
				}

				ReorgCorrectionsBaseSubProcessor<T> reorg= getReorgSubProcessor();
				if (reorg != null) {
					if (isVarInLambdaParamType) {
						getReorgSubProcessor().addNeedHigherComplianceProposals(context, problem, proposals, JavaCore.VERSION_11);
					} else {
						getReorgSubProcessor().addNeedHigherComplianceProposals(context, problem, proposals, JavaCore.VERSION_10);
					}
				}
			}
		}

		// change to similar type proposals
		addSimilarTypeProposals(kind, cu, node, IProposalRelevance.SIMILAR_TYPE, proposals);

		while (node.getParent() instanceof QualifiedName) {
			node= (Name) node.getParent();
		}

		IModuleDescription moduleDescription= cu.getModule();
		if (moduleDescription != null && moduleDescription.exists()
				&& javaProject != null && JavaModelUtil.is9OrHigher(javaProject)) {
			ICompilationUnit moduleCompilationUnit= moduleDescription.getCompilationUnit();
			if (cu.equals(moduleCompilationUnit)) {
				collectRequiresModuleProposals(cu, node, kind, proposals, false);
				ASTNode parentNode= node.getParent();
				if (parentNode instanceof ProvidesDirective providesDir) {
					Name serviceName= providesDir.getName();
					IBinding binding= serviceName.resolveBinding();
					if (binding instanceof ITypeBinding typeBinding) {
						if (typeBinding.isClass()) {
							kind= TypeKinds.CLASSES;
						} else if (typeBinding.isInterface()) {
							kind= TypeKinds.CLASSES | TypeKinds.INTERFACES;
						}
					}
				}
			}
		}

		if (selectedNode != node) {
			kind= evauateTypeKind(node, javaProject);
		}
		if ((kind & (TypeKinds.CLASSES | TypeKinds.INTERFACES)) != 0) {
			kind &= ~TypeKinds.ANNOTATIONS; // only propose annotations when there are no other suggestions
		}
		collectNewTypeProposals(cu, node, kind, IProposalRelevance.NEW_TYPE, proposals);

		getReorgSubProcessor().addProjectSetupFixProposals(context, problem, node.getFullyQualifiedName(), proposals);
	}

	private SimpleName addEnhancedForWithoutTypeProposals(ICompilationUnit cu, ASTNode selectedNode, Collection<T> proposals) {
		if (selectedNode instanceof SimpleName simpleName && (selectedNode.getLocationInParent() == SimpleType.NAME_PROPERTY || selectedNode.getLocationInParent() == NameQualifiedType.NAME_PROPERTY)) {
			ASTNode type= selectedNode.getParent();
			if (type.getLocationInParent() == SingleVariableDeclaration.TYPE_PROPERTY) {
				SingleVariableDeclaration svd= (SingleVariableDeclaration) type.getParent();
				if (svd.getLocationInParent() == EnhancedForStatement.PARAMETER_PROPERTY) {
					if (svd.getName().getLength() == 0) {
						String name= simpleName.getIdentifier();
						int relevance= StubUtility.hasLocalVariableName(cu.getJavaProject(), name) ? 10 : 7;
						String label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_create_loop_variable_description, BasicElementLabels.getJavaElementName(name));
						NewVariableCorrectionProposalCore core= new NewVariableCorrectionProposalCore(label, cu, NewVariableCorrectionProposalCore.LOCAL, simpleName, null, relevance, false);
						T prop= newVariableCorrectionProposalToT(core, EnhancedForWithoutTypeProposal1);
						if (prop != null)
							proposals.add(prop);
						return simpleName;
					}
				}
			}
		}
		return null;
	}

	protected void addNullityAnnotationTypesProposals(ICompilationUnit cu, Name node, Collection<T> proposals) throws CoreException {
		ASTNode parent= node.getParent();
		boolean isAnnotationName= parent instanceof Annotation && ((Annotation) parent).getTypeNameProperty() == node.getLocationInParent();
		if (!isAnnotationName) {
			boolean isImportName= parent instanceof ImportDeclaration && ImportDeclaration.NAME_PROPERTY == node.getLocationInParent();
			if (!isImportName)
				return;
		}

		final IJavaProject javaProject= cu.getJavaProject();
		String name= node.getFullyQualifiedName();

		String nullityAnnotation= null;
		String[] annotationNameOptions= { JavaCore.COMPILER_NULLABLE_ANNOTATION_NAME, JavaCore.COMPILER_NONNULL_ANNOTATION_NAME, JavaCore.COMPILER_NONNULL_BY_DEFAULT_ANNOTATION_NAME };
		Hashtable<String, String> defaultOptions= JavaCore.getDefaultOptions();
		for (String annotationNameOption : annotationNameOptions) {
			String annotationName= javaProject.getOption(annotationNameOption, true);
			if (! annotationName.equals(defaultOptions.get(annotationNameOption)))
				return;
			if (JavaModelUtil.isMatchingName(name, annotationName)) {
				nullityAnnotation= annotationName;
			}
		}
		if (nullityAnnotation == null)
			return;
		if (javaProject.findType(defaultOptions.get(annotationNameOptions[0])) != null)
			return;
		String version= JavaModelUtil.is1d8OrHigher(javaProject) ? "2" : "[1.1.0,2.0.0)"; //$NON-NLS-1$ //$NON-NLS-2$
		Bundle[] annotationsBundles= JavaManipulationPlugin.getDefault().getBundles("org.eclipse.jdt.annotation", version); //$NON-NLS-1$
		if (annotationsBundles == null)
			return;

		if (! cu.getJavaProject().getProject().hasNature("org.eclipse.pde.PluginNature")) //$NON-NLS-1$
			addCopyAnnotationsJarProposal(cu, node, nullityAnnotation, annotationsBundles[0], proposals);
	}

	// 500
	private void addCopyAnnotationsJarProposal(final ICompilationUnit cu, final Name name, final String fullyQualifiedName, Bundle annotationsBundle, Collection<T> proposals) {
		final IJavaProject javaProject= cu.getJavaProject();
		Optional<File> bundleFileLocation= FileLocator.getBundleFileLocation(annotationsBundle);
		if (bundleFileLocation.isEmpty()) {
			return;
		}
		File bundleFile= bundleFileLocation.get();
		if (!bundleFile.isFile() || !bundleFile.canRead())
			return; // we only support a JAR'd bundle, so this won't work in the runtime if you have org.eclipse.jdt.annotation in source.

		final String changeName= CorrectionMessages.UnresolvedElementsSubProcessor_copy_annotation_jar_description;
		ChangeCorrectionProposalCore proposal= new ChangeCorrectionProposalCore(changeName, null, IProposalRelevance.COPY_ANNOTATION_JAR) {
			@Override
			protected Change createChange() throws CoreException {
				final IFile file= javaProject.getProject().getFile(bundleFile.getName());
				ResourceChange copyFileChange= new ResourceChange() {
					@Override
					public Change perform(IProgressMonitor pm) throws CoreException {
						try {
							if (file.exists()) {
								file.delete(false, pm);
							}
							file.create(new ByteArrayInputStream(Files.readAllBytes(bundleFile.toPath())), false, pm);
							return new DeleteResourceChange(file.getFullPath(), false);
						} catch (IOException e) {
							throw new CoreException(new Status(IStatus.ERROR, JavaManipulationPlugin.getPluginId(), e.getMessage()));
						}
					}
					@Override
					public String getName() {
						return changeName;
					}
					@Override
					protected IResource getModifiedResource() {
						return javaProject.getProject();
					}
				};
				ClasspathChange addEntryChange= ClasspathChange.addEntryChange(javaProject, JavaCore.newLibraryEntry(file.getFullPath(), null, null));
				CompilationUnitChange addImportChange= createAddImportChange(cu, name, fullyQualifiedName);
				return new CompositeChange(changeName, new Change[] { copyFileChange, addEntryChange, addImportChange});
			}

			@Override
			public Object getAdditionalProposalInfo(IProgressMonitor monitor) {
				return CorrectionMessages.UnresolvedElementsSubProcessor_copy_annotation_jar_info;
			}
		};
		T prop= changeCorrectionProposalToT(proposal, CopyAnnotationsJarProposal1);
		if (prop != null)
			proposals.add(prop);
	}

	static CompilationUnitChange createAddImportChange(ICompilationUnit cu, Name name, String fullyQualifiedName) throws CoreException {
		String[] args= { BasicElementLabels.getJavaElementName(Signature.getSimpleName(fullyQualifiedName)),
				BasicElementLabels.getJavaElementName(Signature.getQualifier(fullyQualifiedName)) };
		String label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_importtype_description, args);

		CompilationUnitChange cuChange= new CompilationUnitChange(label, cu);
		ImportRewrite importRewrite= StubUtility.createImportRewrite((CompilationUnit) name.getRoot(), true);
		importRewrite.addImport(fullyQualifiedName);
		cuChange.setEdit(importRewrite.rewriteImports(null));
		return cuChange;
	}

	// 600
	private void addSimilarTypeProposals(int kind, ICompilationUnit cu, Name node, int relevance, Collection<T> proposals) throws CoreException {
		SimilarElement[] elements= SimilarElementsRequestor.findSimilarElement(cu, node, kind);

		// try to resolve type in context -> highest severity
		String resolvedTypeName= null;
		ITypeBinding binding= ASTResolving.guessBindingForTypeReference(node);
		ITypeBinding simpleBinding= null;
		if (binding != null) {
			simpleBinding= binding;
			if (simpleBinding.isArray()) {
				simpleBinding= simpleBinding.getElementType();
			}
			simpleBinding= simpleBinding.getTypeDeclaration();

			if (!simpleBinding.isRecovered()) {
				resolvedTypeName= simpleBinding.getQualifiedName();
				T proposal= createTypeRefChangeProposal(cu, resolvedTypeName, node, relevance + 2, elements.length);
				ChangeCorrectionProposalCore original= getOriginalProposalFromT(proposal);
				T compositeProposal= getCompositeChangeProposal(proposal);
				ChangeCorrectionProposalCore compositeOriginal= getOriginalProposalFromT(compositeProposal);
				if (compositeProposal != null) {
					proposals.add(compositeProposal);
				} else if (proposal != null){
					proposals.add(proposal);
				}
				if (original instanceof AddImportCorrectionProposalCore) {
					int rel= relevance + elements.length + 2;
					original.setRelevance(rel);
					if (compositeProposal != null) {
						compositeOriginal.setRelevance(rel);
					}
				}

				if (binding.isParameterizedType()
						&& (node.getParent() instanceof SimpleType || node.getParent() instanceof NameQualifiedType)
						&& !(node.getParent().getParent() instanceof Type)) {
					T toAdd= createTypeRefChangeFullProposal(cu, binding, node, relevance + 5, TypeLocation.UNKNOWN);
					if (toAdd != null)
						proposals.add(toAdd);
				}
			}
		} else {
			ASTNode normalizedNode= ASTNodes.getNormalizedNode(node);
			if (!(normalizedNode.getParent() instanceof Type) && node.getParent() != normalizedNode) {
				ITypeBinding normBinding= ASTResolving.guessBindingForTypeReference(normalizedNode);
				if (normBinding != null && !normBinding.isRecovered()) {
					T toAdd= createTypeRefChangeFullProposal(cu, normBinding, normalizedNode, relevance + 5, TypeLocation.UNKNOWN);
					if (toAdd != null)
						proposals.add(toAdd);
				}
			}
		}

		// add all similar elements
		for (SimilarElement elem : elements) {
			if ((elem.getKind() & TypeKinds.ALL_TYPES) != 0) {
				String fullName= elem.getName();
				if (!fullName.equals(resolvedTypeName)) {
					if (simpleBinding != null && !simpleBinding.isPrimitive()) {
						// If we have an expected type, we should verify that any classes we suggest to import
						// inherit directly or indirectly from the type
						ITypeBinding qualifiedTypeBinding= null;
						try {
							IJavaProject focus= simpleBinding.getJavaElement().getJavaProject();
							IType javaElementType= focus.findType(fullName);
							if (javaElementType != null) {
								ASTParser parser= ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
								parser.setProject(focus);
								IBinding[] bindings= parser.createBindings(new IJavaElement[] {javaElementType} , null);
								qualifiedTypeBinding=(ITypeBinding)bindings[0];
							}
						} catch (JavaModelException e) {
							// fall through
						}

						if (qualifiedTypeBinding != null) {
							if (!qualifiedTypeBinding.getName().equals(simpleBinding.getName()) && !qualifiedTypeBinding.isGenericType() &&
									!qualifiedTypeBinding.isParameterizedType() && !qualifiedTypeBinding.isWildcardType() &&
									!qualifiedTypeBinding.isRawType() && !qualifiedTypeBinding.isRecord() &&
									!qualifiedTypeBinding.isRecovered() && !isInherited(qualifiedTypeBinding, simpleBinding)) {
								continue;
							}
						}
					}
					T cuProposal= createTypeRefChangeProposal(cu, fullName, node, relevance, elements.length);
					T compositeProposal= getCompositeChangeProposal(cuProposal);
					if (compositeProposal != null) {
						proposals.add(compositeProposal);
					} else if (cuProposal != null) {
						proposals.add(cuProposal);
					}
				}
			}
		}
		if (elements.length == 0) {
			collectRequiresModuleProposals(cu, node, IProposalRelevance.IMPORT_NOT_FOUND_ADD_REQUIRES_MODULE, proposals, true);
		}
	}

	private boolean isInherited(ITypeBinding binding, ITypeBinding ancestorBinding) {
		if (binding == null) {
			return false;
		}
		if (binding.isEqualTo(ancestorBinding)) {
			return true;
		}
		for (ITypeBinding interfaceBinding : binding.getInterfaces()) {
			if (isInherited(interfaceBinding, ancestorBinding)) {
				return true;
			}
		}
		if (isInherited(binding.getSuperclass(), ancestorBinding)) {
			return true;
		}
		return false;
	}

	// 700
	private T getCompositeChangeProposal(T proposal) throws CoreException {
		ChangeCorrectionProposalCore original= getOriginalProposalFromT(proposal);
		return getCompositeChangeProposal(original);
	}

	private T getCompositeChangeProposal(ChangeCorrectionProposalCore proposal) throws CoreException {
		ChangeCorrectionProposalCore compositeProposal= null;
		if (proposal instanceof AddImportCorrectionProposalCore aicpc) {
			AddModuleRequiresCorrectionProposalCore cp= aicpc.getAdditionalProposal();
			Change importChange= proposal.getChange();
			Change change= cp == null ? null : cp.getChange();
			if (change != null) {
				ImportRewrite importRewrite= aicpc.getImportRewrite();
				boolean importNeedsToBeAdded= false;
				if (importRewrite != null) {
					String[] imports= importRewrite.getAddedImports();
					if (imports != null && imports.length > 0) {
						importNeedsToBeAdded= true;
					}
				}
				if (importNeedsToBeAdded) {
					change.initializeValidationData(new NullProgressMonitor());
					String importChangeName= importChange.getName();
					String moduleRequiresChangeName= change.getName();
					moduleRequiresChangeName= moduleRequiresChangeName.substring(0, 1).toLowerCase() + moduleRequiresChangeName.substring(1);
					String changeName= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_combine_two_proposals_info, new String[] { importChangeName, moduleRequiresChangeName });
					compositeProposal= new ChangeCorrectionProposalCore(changeName, null, IProposalRelevance.IMPORT_NOT_FOUND_ADD_REQUIRES_MODULE) {
						@Override
						protected Change createChange() throws CoreException {
							return new CompositeChange(changeName, new Change[] { change, importChange });
						}

						@Override
						public Object getAdditionalProposalInfo(IProgressMonitor monitor) {
							return changeName;
						}
					};
				} else {
					compositeProposal= new ChangeCorrectionProposalCore(change.getName(), null, IProposalRelevance.IMPORT_NOT_FOUND_ADD_REQUIRES_MODULE) {
						@Override
						protected Change createChange() throws CoreException {
							return change;
						}

						@Override
						public Object getAdditionalProposalInfo(IProgressMonitor monitor) {
							return change.getName();
						}
					};
				}
			}
		}
		return compositeProposal == null ? null : compositeProposalToT(compositeProposal, -CompositeChangeProposal);
	}

	// 800
	private T createTypeRefChangeProposal(ICompilationUnit cu, String fullName, Name node, int relevance, int maxProposals) {
		ImportRewrite importRewrite= null;
		String simpleName= fullName;
		String packName= Signature.getQualifier(fullName);
		if (packName.length() > 0) { // no imports for primitive types, type variables
			importRewrite= StubUtility.createImportRewrite((CompilationUnit) node.getRoot(), true);
			BodyDeclaration scope= ASTResolving.findParentBodyDeclaration(node); // can be null in package-info.java
			ImportRewriteContext context= new ContextSensitiveImportRewriteContext(scope != null ? scope : node, importRewrite);
			simpleName= importRewrite.addImport(fullName, context);
		}

		if (!isLikelyTypeName(simpleName)) {
			relevance -= 2;
		}

		ASTRewriteCorrectionProposalCore proposal;
		int uid= -1;
		if (importRewrite != null && node.isSimpleName() && simpleName.equals(((SimpleName) node).getIdentifier())) { // import only
			// check first that we aren't doing an import of a nested class in this cu - bug 321464
			// in which case we should just change the reference to a qualified name
			try {
				IType[] types= cu.getAllTypes();
				for (IType type : types) {
					if (type.getFullyQualifiedName('.').equals(fullName)) {
						String label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_change_to_qualified_description, fullName);
						proposal= new QualifyTypeProposalCore(label, cu, relevance + 100, (SimpleName)node, fullName);
						return qualifyTypeProposalToT((QualifyTypeProposalCore)proposal, 810);
					}
				}
			} catch (JavaModelException e) {
				return null;
			}
			// import only
			String[] arg= { BasicElementLabels.getJavaElementName(simpleName), BasicElementLabels.getJavaElementName(packName) };
			String label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_importtype_description, arg);
			//Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_IMPDECL);
			int boost= getQualifiedTypeNameHistoryBoost(fullName, 0, maxProposals);
			//int boost= QualifiedTypeNameHistory.getBoost(fullName, 0, maxProposals);
			proposal= new AddImportCorrectionProposalCore(label, cu, relevance + 100 + boost, packName, simpleName, (SimpleName)node);
			proposal.setCommandId(ADD_IMPORT_ID);
			uid= TypeRefChangeProposal1;
		} else {
			String label;
			if (packName.length() == 0) {
				label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_changetype_nopack_description, BasicElementLabels.getJavaElementName(simpleName));
			} else {
				String[] arg= { BasicElementLabels.getJavaElementName(simpleName), BasicElementLabels.getJavaElementName(packName) };
				label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_changetype_description, arg);
			}
			ASTRewrite rewrite= ASTRewrite.create(node.getAST());
			rewrite.replace(node, rewrite.createStringPlaceholder(simpleName, ASTNode.SIMPLE_TYPE), null);
			//Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			proposal= new ASTRewriteCorrectionProposalCore(label, cu, rewrite, relevance);
			uid= TypeRefChangeProposal2;
		}
		if (importRewrite != null) {
			proposal.setImportRewrite(importRewrite);
		}
		return rewriteProposalToT(proposal, uid);
	}


	// 900
	protected T createTypeRefChangeFullProposal(ICompilationUnit cu, ITypeBinding binding, ASTNode node, int relevance, TypeLocation typeLocation) {
		ASTRewrite rewrite= ASTRewrite.create(node.getAST());
		String label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_change_full_type_description, BindingLabelProviderCore.getBindingLabel(binding, JavaElementLabelsCore.ALL_DEFAULT));
		ASTRewriteCorrectionProposalCore proposal= new ASTRewriteCorrectionProposalCore(label, cu, rewrite, relevance);

		ImportRewrite imports= proposal.createImportRewrite((CompilationUnit) node.getRoot());
		ImportRewriteContext context= new ContextSensitiveImportRewriteContext(node, imports);
		Type type= imports.addImport(binding, node.getAST(), context, typeLocation);

		rewrite.replace(node, type, null);
		return rewriteProposalToT(proposal, TypeRefChangeFullProposal1);
	}

	protected boolean isLikelyTypeName(String name) {
		return name.length() > 0 && Character.isUpperCase(name.charAt(0));
	}

	protected boolean isLikelyPackageName(String name) {
		if (name.length() != 0) {
			int i= 0;
			do {
				if (Character.isUpperCase(name.charAt(i))) {
					return false;
				}
				i= name.indexOf('.', i) + 1;
			} while (i != 0 && i < name.length());
		}
		return true;
	}

	private boolean isLikelyTypeParameterName(String name) {
		return name.length() == 1 && Character.isUpperCase(name.charAt(0));
	}

	private boolean isLikelyMethodTypeParameterName(String name) {
		if (name.length() == 1) {
			switch (name.charAt(0)) {
				case 'S':
				case 'T':
				case 'U':
					return true;
			}
		}
		return false;
	}

	// 1000
	public void collectNewTypeProposals(ICompilationUnit cu, Name refNode, int kind, int relevance, Collection<T> proposals) throws CoreException {
		addNewTypeProposalsInteractive(cu, refNode, kind, relevance, proposals);
		addNewTypeProposalsParams(cu, refNode, kind, relevance, proposals);
	}

	protected void addNewTypeProposalsInteractive(ICompilationUnit cu, Name refNode, int kind, int relevance, Collection<T> proposals) throws CoreException {
		Name node= refNode;
		do {
			String typeName= ASTNodes.getSimpleNameIdentifier(node);
			Name qualifier= null;
			// only propose to create types for qualifiers when the name starts with upper case
			boolean isPossibleName= isLikelyTypeName(typeName) || node == refNode;
			if (isPossibleName) {
				IPackageFragment enclosingPackage= null;
				IType enclosingType= null;
				if (node.isSimpleName()) {
					enclosingPackage= (IPackageFragment) cu.getParent();
					// don't suggest member type, user can select it in wizard
				} else {
					Name qualifierName= ((QualifiedName) node).getQualifier();
					IBinding binding= qualifierName.resolveBinding();
					if (binding != null && binding.isRecovered()) {
						binding= null;
					}
					if (binding instanceof ITypeBinding) {
						enclosingType=(IType) binding.getJavaElement();
					} else if (binding instanceof IPackageBinding) {
						qualifier= qualifierName;
						enclosingPackage= (IPackageFragment) binding.getJavaElement();
					} else {
						IJavaElement[] res= cu.codeSelect(qualifierName.getStartPosition(), qualifierName.getLength());
						if (res!= null && res.length > 0 && res[0] instanceof IType res0) {
							enclosingType= res0;
						} else {
							qualifier= qualifierName;
							enclosingPackage= JavaModelUtil.getPackageFragmentRoot(cu).getPackageFragment(ASTResolving.getFullName(qualifierName));
						}
					}
				}
				int rel= relevance;
				if (enclosingPackage != null && isLikelyPackageName(enclosingPackage.getElementName())) {
					rel += 3;
				}

				if (enclosingPackage != null && !enclosingPackage.getCompilationUnit(typeName + JavaModelUtil.DEFAULT_CU_SUFFIX).exists()
						|| enclosingType != null && !enclosingType.isReadOnly() && !enclosingType.getType(typeName).exists()) { // new member type
					IJavaElement enclosing= enclosingPackage != null ? (IJavaElement) enclosingPackage : enclosingType;
					addNewTypeProposalsInteractiveInnerLoop(cu, node, enclosing, rel, kind, refNode, proposals);
				}
			}
			node= qualifier;
		} while (node != null);
	}

	protected abstract void addNewTypeProposalsInteractiveInnerLoop(ICompilationUnit cu, Name node, IJavaElement enclosing, int rel, int kind, Name refNode, Collection<T> proposals) throws CoreException;

	protected void addNewTypeProposalsParams(ICompilationUnit cu, Name refNode, int kind, int relevance, Collection<T> proposals) {
		// type parameter proposals
		if (refNode.isSimpleName() && (kind & TypeKinds.VARIABLES)  != 0) {
			CompilationUnit root= (CompilationUnit) refNode.getRoot();
			String name= ((SimpleName) refNode).getIdentifier();
			BodyDeclaration declaration= ASTResolving.findParentBodyDeclaration(refNode);
			int baseRel= relevance;
			if (isLikelyTypeParameterName(name)) {
				baseRel += 8;
			}
			while (declaration != null) {
				IBinding binding= null;
				int rel= baseRel;
				if (declaration instanceof MethodDeclaration methodDecl) {
					binding= methodDecl.resolveBinding();
					if (isLikelyMethodTypeParameterName(name))
						rel+= 2;
				} else if (declaration instanceof TypeDeclaration typeDecl) {
					binding= typeDecl.resolveBinding();
					rel++;
				}
				if (binding != null) {
					AddTypeParameterProposalCore proposal= new AddTypeParameterProposalCore(cu, binding, root, name, null, rel);
					T t= addTypeParametersToT(proposal, NewTypeProposalsParams1);
					if (t != null)
						proposals.add(t);
				}
				// Do these lines do anything?
				if (!Modifier.isStatic(declaration.getModifiers())) {
					declaration= ASTResolving.findParentBodyDeclaration(declaration.getParent());
				} else {
					declaration= null;
				}
			}
		}
	}


	protected boolean canUseRecord(IJavaProject project, Name refNode) {
		boolean canUseRecord= false;
		if (project != null
				&& JavaModelUtil.is16OrHigher(project)
				&& refNode != null) {
			canUseRecord= true;
			Type type= ASTNodes.getParent(refNode, Type.class);
			TypeDeclaration typeDecl= ASTNodes.getParent(refNode, TypeDeclaration.class);
			if (type != null && typeDecl != null) {
				StructuralPropertyDescriptor locationInParent= type.getLocationInParent();
				if (locationInParent == TypeDeclaration.SUPERCLASS_TYPE_PROPERTY) {
					canUseRecord= false;
				} else if (locationInParent == TypeDeclaration.PERMITS_TYPES_PROPERTY
						&& !typeDecl.isInterface()) {
					canUseRecord= false;
				}
			}
		}
		return canUseRecord;
	}

	// 1100
	public void collectRequiresModuleProposals(ICompilationUnit cu, Name node, int relevance, Collection<T> proposals, boolean isOnDemand) throws CoreException {
		if (cu == null) {
			return;
		}
		IJavaProject currentJavaProject= cu.getJavaProject();
		if (currentJavaProject == null || !JavaModelUtil.is9OrHigher(currentJavaProject)) {
			return;
		}
		IModuleDescription currentModuleDescription= currentJavaProject.getModuleDescription();
		if (currentModuleDescription == null) {
			return;
		}
		ICompilationUnit currentModuleCompilationUnit= currentModuleDescription.getCompilationUnit();
		if (currentModuleCompilationUnit == null || !currentModuleCompilationUnit.exists()) {
			return;
		}
		int typeRule= IJavaSearchConstants.TYPE;
		if (isOnDemand) {
			typeRule= IJavaSearchConstants.PACKAGE;
		}

		List<IPackageFragment> matchingPackageFragments= new ArrayList<>();
		if (node.isSimpleName() && !isOnDemand) {
			matchingPackageFragments.add((IPackageFragment) cu.getParent());
		} else {
			String qualifiedName= node.getFullyQualifiedName();
			List<IPackageFragment> packageFragments= AddModuleRequiresCorrectionProposalCore.getPackageFragmentsOfMatchingTypesImpl(qualifiedName, typeRule, currentJavaProject);
			if (packageFragments.size() > 0) {
				matchingPackageFragments.addAll(packageFragments);
			} else if (isOnDemand) {
				packageFragments= AddModuleRequiresCorrectionProposalCore.getPackageFragmentsOfMatchingTypesImpl(qualifiedName, IJavaSearchConstants.TYPE, currentJavaProject);
				if (packageFragments.size() > 0) {
					matchingPackageFragments.addAll(packageFragments);
				}
			}
		}
		IModuleDescription projectModule= null;
		if (matchingPackageFragments.size() > 0) {
			HashSet<String> modules= new HashSet<>();
			for (IPackageFragment enclosingPackage : matchingPackageFragments) {
				if (enclosingPackage.isReadOnly()) { // This is to handle the case where the enclosingPackage belongs to a jar file
					IPackageFragmentRoot root= (IPackageFragmentRoot) enclosingPackage.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
					projectModule= AddModuleRequiresCorrectionProposalCore.getModuleDescription(root);
				} else {
					IJavaProject project= enclosingPackage.getJavaProject();
					projectModule= AddModuleRequiresCorrectionProposalCore.getModuleDescription(project);
				}
				if (projectModule != null && ((projectModule.exists() && !projectModule.equals(currentModuleDescription))
						|| projectModule.isAutoModule())) {
					String moduleName= projectModule.getElementName();
					if (!modules.contains(moduleName)) {
						String[] args= { moduleName };
						final String changeName= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_add_requires_module_info, args);
						final String changeDescription= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_add_requires_module_description, args);
						AddModuleRequiresCorrectionProposalCore proposal= new AddModuleRequiresCorrectionProposalCore(moduleName, changeName, changeDescription, currentModuleCompilationUnit, relevance);
						Change change= proposal.getChange();
						if (change != null) {
							T t= addModuleRequiresProposalToT(proposal, RequiresModuleProposal1);
							if (t != null) {
								proposals.add(t);
							}
							modules.add(moduleName);
						}
					}
				}
			}
		}
	}


	// 1200
	public void collectMethodProposals(IInvocationContext context, IProblemLocation problem, boolean isOnlyParameterMismatch, Collection<T> proposals) throws CoreException {

		ICompilationUnit cu= context.getCompilationUnit();

		CompilationUnit astRoot= context.getASTRoot();
		ASTNode selectedNode= problem.getCoveringNode(astRoot);

		if (!(selectedNode instanceof SimpleName nameNode)) {
			return;
		}

		List<Expression> arguments;
		Expression sender;
		boolean isSuperInvocation;

		ASTNode invocationNode= nameNode.getParent();
		if (invocationNode instanceof MethodInvocation methodImpl) {
			arguments= methodImpl.arguments();
			sender= methodImpl.getExpression();
			isSuperInvocation= false;
		} else if (invocationNode instanceof SuperMethodInvocation methodImpl) {
			arguments= methodImpl.arguments();
			sender= methodImpl.getQualifier();
			isSuperInvocation= true;
		} else {
			return;
		}

		String methodName= nameNode.getIdentifier();
		int nArguments= arguments.size();

		// corrections
		IBinding[] bindings= (new ScopeAnalyzer(astRoot)).getDeclarationsInScope(nameNode, ScopeAnalyzer.METHODS);

		HashSet<String> suggestedRenames= new HashSet<>();
		for (IBinding b : bindings) {
			IMethodBinding binding= (IMethodBinding) b;
			String curr= binding.getName();
			if (!curr.equals(methodName) && binding.getParameterTypes().length == nArguments && NameMatcher.isSimilarName(methodName, curr) && suggestedRenames.add(curr)) {
				String label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_changemethod_description, BasicElementLabels.getJavaElementName(curr));
				RenameNodeCorrectionProposalCore core= new RenameNodeCorrectionProposalCore(label, context.getCompilationUnit(), problem.getOffset(), problem.getLength(), curr, IProposalRelevance.CHANGE_METHOD);
				T t= renameNodeProposalToT(core, MethodProposal1);
				if (t != null)
					proposals.add(t);
			}
		}
		suggestedRenames= null;

		if (isOnlyParameterMismatch) {
			ArrayList<IMethodBinding> parameterMismatchs= new ArrayList<>();
			for (IBinding binding : bindings) {
				if (binding.getName().equals(methodName)) {
					parameterMismatchs.add((IMethodBinding) binding);
				}
			}
			addParameterMissmatchProposals(context, problem, parameterMismatchs, invocationNode, arguments, proposals);
		}

		if (sender == null) {
			addStaticImportFavoriteProposals(context, nameNode, true, proposals);
		}

		// new method
		addNewMethodProposals(cu, astRoot, sender, arguments, isSuperInvocation, invocationNode, methodName, proposals);

		if (!isOnlyParameterMismatch && !isSuperInvocation && sender != null) {
			addMissingCastParentsProposal(cu, (MethodInvocation) invocationNode, proposals);
		}

		if (!isSuperInvocation && sender == null && invocationNode.getParent() instanceof ThrowStatement) {
			String str= "new ";   //$NON-NLS-1$ // do it the manual way, copting all the arguments is nasty
			String label= CorrectionMessages.UnresolvedElementsSubProcessor_addnewkeyword_description;
			int relevance= Character.isUpperCase(methodName.charAt(0)) ? IProposalRelevance.ADD_NEW_KEYWORD_UPPERCASE : IProposalRelevance.ADD_NEW_KEYWORD;
			ReplaceCorrectionProposalCore proposal= new ReplaceCorrectionProposalCore(label, cu, invocationNode.getStartPosition(), 0, str, relevance);
			T t= replaceCorrectionProposalToT(proposal, MethodProposal2);
			if (t != null)
				proposals.add(t);
		}

	}


	// 1300
	private void addStaticImportFavoriteProposals(IInvocationContext context, SimpleName node, boolean isMethod, Collection<T> proposals) throws JavaModelException {
		IJavaProject project= context.getCompilationUnit().getJavaProject();
		if (JavaModelUtil.is50OrHigher(project)) {
			String pref= JavaManipulation.getPreference(JavaManipulationPlugin.CODEASSIST_FAVORITE_STATIC_MEMBERS, project);
			if (pref == null  || pref.isBlank()) {
				return;
			}
			String[] favourites= pref.split(";"); //$NON-NLS-1$
			if (favourites.length == 0) {
				return;
			}

			CompilationUnit root= context.getASTRoot();
			AST ast= root.getAST();

			String name= node.getIdentifier();
			for (String curr : JavaModelUtil.getStaticImportFavorites(context.getCompilationUnit(), name, isMethod, favourites)) {
				ImportRewrite importRewrite= StubUtility.createImportRewrite(root, true);
				ASTRewrite astRewrite= ASTRewrite.create(ast);

				String label;
				String qualifiedTypeName= Signature.getQualifier(curr);
				String elementLabel= BasicElementLabels.getJavaElementName(JavaModelUtil.concatenateName(Signature.getSimpleName(qualifiedTypeName), name));

				String res= importRewrite.addStaticImport(qualifiedTypeName, name, isMethod, new ContextSensitiveImportRewriteContext(root, node.getStartPosition(), importRewrite));
				int dot= res.lastIndexOf('.');
				if (dot != -1) {
					String usedTypeName= importRewrite.addImport(qualifiedTypeName);
					Name newName= ast.newQualifiedName(ast.newName(usedTypeName), ast.newSimpleName(name));
					astRewrite.replace(node, newName, null);
					label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_change_to_static_import_description, elementLabel);
				} else {
					label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_add_static_import_description, elementLabel);
				}

				//Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_IMPDECL);
				ASTRewriteCorrectionProposalCore proposal= new ASTRewriteCorrectionProposalCore(label, context.getCompilationUnit(), astRewrite, IProposalRelevance.ADD_STATIC_IMPORT);
				proposal.setImportRewrite(importRewrite);
				T t= rewriteProposalToT(proposal, StaticImportFavoriteProposal1);
				if (t != null)
					proposals.add(t);
			}
		}
	}

	// 1400
	private void addNewMethodProposals(ICompilationUnit cu, CompilationUnit astRoot, Expression sender, List<Expression> arguments, boolean isSuperInvocation, ASTNode invocationNode, String methodName, Collection<T> proposals) throws JavaModelException {
		ITypeBinding nodeParentType= Bindings.getBindingOfParentType(invocationNode);
		ITypeBinding binding= null;
		if (sender != null) {
			binding= sender.resolveTypeBinding();
		} else {
			binding= nodeParentType;
			if (isSuperInvocation && binding != null) {
				binding= binding.getSuperclass();
			}
		}
		if (binding != null && binding.isFromSource()) {
			ITypeBinding senderDeclBinding= binding.getErasure().getTypeDeclaration();

			ICompilationUnit targetCU= ASTResolving.findCompilationUnitForBinding(cu, astRoot, senderDeclBinding);
			if (targetCU != null) {
				String label;
				String labelAbstract;
				ITypeBinding[] parameterTypes= getParameterTypes(arguments);
				if (parameterTypes != null) {
					String sig= ASTResolving.getMethodSignature(methodName, parameterTypes, false);
					boolean is18OrHigher= JavaModelUtil.is1d8OrHigher(targetCU.getJavaProject());
					boolean isSenderTypeAbstractClass= (senderDeclBinding.getModifiers() &  Modifier.ABSTRACT) > 0;
					boolean isSenderBindingInterface= senderDeclBinding.isInterface();
					int firstProposalUid= -1;
					if (nodeParentType == senderDeclBinding) {
						label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_createmethod_description, sig);
						labelAbstract= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_createmethod_abstract_description, sig);
						if (isSenderBindingInterface) {
							firstProposalUid= NewMethodProposal1;
						} else {
							firstProposalUid= NewMethodProposal2;
						}
					} else {
						firstProposalUid= NewMethodProposal3;
						label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_createmethod_other_description, new Object[] { sig, BasicElementLabels.getJavaElementName(senderDeclBinding.getName()) } );
						labelAbstract= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_createmethod_abstract_other_description, new Object[] { sig, BasicElementLabels.getJavaElementName(senderDeclBinding.getName()) } );
					}

					boolean type1= is18OrHigher || !isSenderBindingInterface
							|| (nodeParentType != senderDeclBinding && (!(sender instanceof SimpleName) || !((SimpleName) sender).getIdentifier().equals(senderDeclBinding.getName())));
					if (type1) {
						NewMethodCorrectionProposalCore core= new NewMethodCorrectionProposalCore(label, targetCU, invocationNode, arguments, senderDeclBinding, IProposalRelevance.CREATE_METHOD);
						T t= newMethodProposalToT(core, firstProposalUid);
						if (t != null)
							proposals.add(t);
					}

					if ( type1 && isSenderTypeAbstractClass ) {
						NewAbstractMethodCorrectionProposalCore core= new NewAbstractMethodCorrectionProposalCore(labelAbstract, targetCU, invocationNode, arguments, senderDeclBinding, IProposalRelevance.CREATE_METHOD);
						T t= newMethodProposalToT(core, NewMethodProposal4);
						if (t != null)
							proposals.add(t);
					}


					if (senderDeclBinding.isNested() && cu.equals(targetCU) && sender == null && Bindings.findMethodInHierarchy(senderDeclBinding, methodName, (ITypeBinding[]) null) == null) { // no covering method
						ASTNode anonymDecl= astRoot.findDeclaringNode(senderDeclBinding);
						if (anonymDecl != null) {
							senderDeclBinding= Bindings.getBindingOfParentType(anonymDecl.getParent());
							isSenderBindingInterface= senderDeclBinding.isInterface();
							isSenderTypeAbstractClass= (senderDeclBinding.getModifiers() &  Modifier.ABSTRACT) > 0;
							if (!senderDeclBinding.isAnonymous()) {
								if (is18OrHigher || !isSenderBindingInterface) {
									String[] args= new String[] { sig, ASTResolving.getTypeSignature(senderDeclBinding) };
									label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_createmethod_other_description, args);
									labelAbstract= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_createmethod_abstract_other_description, args);
									int nextUid;
									if (isSenderBindingInterface) {
										nextUid= NewMethodProposal5;
									} else {
										nextUid= NewMethodProposal6;
									}
									NewMethodCorrectionProposalCore core= new NewMethodCorrectionProposalCore(label, targetCU, invocationNode, arguments, senderDeclBinding, IProposalRelevance.CREATE_METHOD);
									T t= newMethodProposalToT(core, nextUid);
									if (t != null)
										proposals.add(t);
									if ( isSenderTypeAbstractClass ) {
										NewAbstractMethodCorrectionProposalCore c2= new NewAbstractMethodCorrectionProposalCore(labelAbstract, targetCU, invocationNode, arguments, senderDeclBinding, IProposalRelevance.CREATE_METHOD);
										T t2= newMethodProposalToT(c2, NewMethodProposal7);
										if (t2 != null)
											proposals.add(t2);
									}
								}
							}
						}
					}
				}
			}
		}
	}

	// 1500
	private void addMissingCastParentsProposal(ICompilationUnit cu, MethodInvocation invocationNode, Collection<T> proposals) {
		Expression sender= invocationNode.getExpression();
		if (sender instanceof ThisExpression) {
			return;
		}

		ITypeBinding senderBinding= sender.resolveTypeBinding();
		if (senderBinding == null || Modifier.isFinal(senderBinding.getModifiers())) {
			return;
		}

		if (sender instanceof Name && ((Name) sender).resolveBinding() instanceof ITypeBinding) {
			return; // static access
		}

		ASTNode parent= invocationNode.getParent();
		while (parent instanceof Expression && parent.getNodeType() != ASTNode.CAST_EXPRESSION) {
			parent= parent.getParent();
		}
		boolean hasCastProposal= false;
		if (parent instanceof CastExpression) {
			//	(TestCase) x.getName() -> ((TestCase) x).getName
			hasCastProposal= useExistingParentCastProposal(cu, (CastExpression) parent, sender, invocationNode.getName(), getArgumentTypes(invocationNode.arguments()), proposals);
		}
		if (!hasCastProposal) {
			// x.getName() -> ((TestCase) x).getName

			Expression target= ASTNodes.getUnparenthesedExpression(sender);

			String label;
			if (target.getNodeType() != ASTNode.CAST_EXPRESSION) {
				String targetName= null;
				if (target.getLength() <= 18) {
					targetName= ASTNodes.asString(target);
				}
				if (targetName == null) {
					label= CorrectionMessages.UnresolvedElementsSubProcessor_methodtargetcast_description;
				} else {
					label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_methodtargetcast2_description, BasicElementLabels.getJavaCodeString(targetName));
				}
			} else {
				String targetName= null;
				if (target.getLength() <= 18) {
					targetName= ASTNodes.asString(((CastExpression)target).getExpression());
				}
				if (targetName == null) {
					label= CorrectionMessages.UnresolvedElementsSubProcessor_changemethodtargetcast_description;
				} else {
					label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_changemethodtargetcast2_description, BasicElementLabels.getJavaCodeString(targetName));
				}
			}
			CastCorrectionProposalCore c= new CastCorrectionProposalCore(label, cu, target, (ITypeBinding) null, IProposalRelevance.CHANGE_CAST);
			T t= castCorrectionProposalToT(c, MissingCastParentsProposal1);
			if (t != null)
				proposals.add(t);
		}
	}


	// 1600
	private boolean useExistingParentCastProposal(ICompilationUnit cu, CastExpression expression, Expression accessExpression, SimpleName accessSelector, ITypeBinding[] paramTypes, Collection<T> proposals) {
		ITypeBinding castType= expression.getType().resolveBinding();
		if (castType == null) {
			return false;
		}
		if (paramTypes != null) {
			if (Bindings.findMethodInHierarchy(castType, accessSelector.getIdentifier(), paramTypes) == null) {
				return false;
			}
		} else if (Bindings.findFieldInHierarchy(castType, accessSelector.getIdentifier()) == null) {
			return false;
		}
		ITypeBinding bindingToCast= accessExpression.resolveTypeBinding();
		if (bindingToCast != null && !bindingToCast.isCastCompatible(castType)) {
			return false;
		}

		IMethodBinding res= Bindings.findMethodInHierarchy(castType, accessSelector.getIdentifier(), paramTypes);
		if (res != null) {
			AST ast= expression.getAST();
			ASTRewrite rewrite= ASTRewrite.create(ast);
			CastExpression newCast= ast.newCastExpression();
			newCast.setType((Type) ASTNode.copySubtree(ast, expression.getType()));
			newCast.setExpression((Expression) rewrite.createCopyTarget(accessExpression));
			ParenthesizedExpression parents= ast.newParenthesizedExpression();
			parents.setExpression(newCast);

			ASTNode node= rewrite.createCopyTarget(expression.getExpression());
			rewrite.replace(expression, node, null);
			rewrite.replace(accessExpression, parents, null);

			String label= CorrectionMessages.UnresolvedElementsSubProcessor_missingcastbrackets_description;
			//Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CAST);
			ASTRewriteCorrectionProposalCore proposal= new ASTRewriteCorrectionProposalCore(label, cu, rewrite, IProposalRelevance.ADD_PARENTHESES_AROUND_CAST);
			T t= rewriteProposalToT(proposal, ExistingParentCastProposal1);
			if (t != null)
				proposals.add(t);
			return true;
		}
		return false;
	}

	private void addParameterMissmatchProposals(IInvocationContext context, IProblemLocation problem, List<IMethodBinding> similarElements, ASTNode invocationNode, List<Expression> arguments, Collection<T> proposals) throws CoreException {
		int nSimilarElements= similarElements.size();
		ITypeBinding[] argTypes= getArgumentTypes(arguments);
		if (argTypes == null || nSimilarElements == 0)  {
			return;
		}

		for (int i= 0; i < nSimilarElements; i++) {
			IMethodBinding elem= similarElements.get(i);
			int diff= elem.getParameterTypes().length - argTypes.length;
			if (diff == 0) {
				int nProposals= proposals.size();
				doEqualNumberOfParameters(context, invocationNode, problem, arguments, argTypes, elem, proposals);
				if (nProposals != proposals.size()) {
					return; // only suggest for one method (avoid duplicated proposals)
				}
			} else if (diff > 0) {
				doMoreParameters(context, invocationNode, argTypes, elem, proposals);
			} else {
				doMoreArguments(context, invocationNode, arguments, argTypes, elem, proposals);
			}
		}
	}

	// 1700
	private void doMoreParameters(IInvocationContext context, ASTNode invocationNode, ITypeBinding[] argTypes, IMethodBinding methodBinding, Collection<T> proposals) throws CoreException {
		ITypeBinding[] paramTypes= methodBinding.getParameterTypes();
		int k= 0, nSkipped= 0;
		int diff= paramTypes.length - argTypes.length;
		int[] indexSkipped= new int[diff];
		for (int i= 0; i < paramTypes.length; i++) {
			if (k < argTypes.length && canAssign(argTypes[k], paramTypes[i])) {
				k++; // match
			} else {
				if (nSkipped >= diff) {
					return; // too different
				}
				indexSkipped[nSkipped++]= i;
			}
		}
		ITypeBinding declaringType= methodBinding.getDeclaringClass();
		ICompilationUnit cu= context.getCompilationUnit();
		CompilationUnit astRoot= context.getASTRoot();

		// add arguments
		{
			String[] arg= new String[] { ASTResolving.getMethodSignature(methodBinding) };
			String label;
			if (diff == 1) {
				label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_addargument_description, arg);
			} else {
				label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_addarguments_description, arg);
			}
			AddArgumentCorrectionProposalCore proposal= new AddArgumentCorrectionProposalCore(label, context.getCompilationUnit(), invocationNode, indexSkipped, paramTypes, IProposalRelevance.ADD_ARGUMENTS);
			T proposalAsT= addArgumentCorrectionProposalToT(proposal, MoreParametersProposal1);
			if (proposalAsT != null)
				proposals.add(proposalAsT);
		}

		// remove parameters
		if (!declaringType.isFromSource() || (methodBinding.isConstructor() && declaringType.isRecord())) {
			return;
		}

		ICompilationUnit targetCU= ASTResolving.findCompilationUnitForBinding(cu, astRoot, declaringType);
		if (targetCU != null) {
			IMethodBinding methodDecl= methodBinding.getMethodDeclaration();
			ITypeBinding[] declParameterTypes= methodDecl.getParameterTypes();

			ChangeDescription[] changeDesc= new ChangeDescription[declParameterTypes.length];
			ITypeBinding[] changedTypes= new ITypeBinding[diff];
			for (int i= diff - 1; i >= 0; i--) {
				int idx= indexSkipped[i];
				changeDesc[idx]= new RemoveDescription();
				changedTypes[i]= declParameterTypes[idx];
			}
			String[] arg= new String[] { ASTResolving.getMethodSignature(methodDecl), getTypeNames(changedTypes) };
			String label;
			if (methodDecl.isConstructor()) {
				if (diff == 1) {
					label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_removeparam_constr_description, arg);
				} else {
					label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_removeparams_constr_description, arg);
				}
			} else {
				if (diff == 1) {
					label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_removeparam_description, arg);
				} else {
					label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_removeparams_description, arg);
				}
			}

			ChangeMethodSignatureProposalCore proposal= new ChangeMethodSignatureProposalCore(label, targetCU, invocationNode, methodDecl, changeDesc, null, IProposalRelevance.CHANGE_METHOD_REMOVE_PARAMETER);
			T t= changeMethodSignatureProposalToT(proposal, MoreParametersProposal2);
			if (t != null)
				proposals.add(t);
		}
	}

	private String getTypeNames(ITypeBinding[] types) {
		StringBuilder buf= new StringBuilder();
		for (int i= 0; i < types.length; i++) {
			if (i > 0) {
				buf.append(", "); //$NON-NLS-1$
			}
			buf.append(ASTResolving.getTypeSignature(types[i]));
		}
		return BasicElementLabels.getJavaElementName(buf.toString());
	}

	private String getArgumentName(List<Expression> arguments, int index) {
		String def= String.valueOf(index + 1);

		ASTNode expr= arguments.get(index);
		if (expr.getLength() > 18) {
			return def;
		}
		ASTMatcher matcher= new ASTMatcher();
		for (int i= 0; i < arguments.size(); i++) {
			if (i != index && matcher.safeSubtreeMatch(expr, arguments.get(i))) {
				return def;
			}
		}
		return '\'' + BasicElementLabels.getJavaElementName(ASTNodes.asString(expr)) + '\'';
	}

	// 1800
	private void doMoreArguments(IInvocationContext context, ASTNode invocationNode, List<Expression> arguments, ITypeBinding[] argTypes, IMethodBinding methodRef, Collection<T> proposals) throws CoreException {
		ITypeBinding[] paramTypes= methodRef.getParameterTypes();
		int k= 0, nSkipped= 0;
		int diff= argTypes.length - paramTypes.length;
		int[] indexSkipped= new int[diff];
		for (int i= 0; i < argTypes.length; i++) {
			if (k < paramTypes.length && canAssign(argTypes[i], paramTypes[k])) {
				k++; // match
			} else {
				if (nSkipped >= diff) {
					return; // too different
				}
				indexSkipped[nSkipped++]= i;
			}
		}

		ICompilationUnit cu= context.getCompilationUnit();
		CompilationUnit astRoot= context.getASTRoot();

		// remove arguments
		{
			ASTRewrite rewrite= ASTRewrite.create(astRoot.getAST());

			for (int i= diff - 1; i >= 0; i--) {
				rewrite.remove(arguments.get(indexSkipped[i]), null);
			}
			String[] arg= new String[] { ASTResolving.getMethodSignature(methodRef) };
			String label;
			if (diff == 1) {
				label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_removeargument_description, arg);
			} else {
				label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_removearguments_description, arg);
			}
			ASTRewriteCorrectionProposalCore proposal= new ASTRewriteCorrectionProposalCore(label, cu, rewrite, IProposalRelevance.REMOVE_ARGUMENTS);
			T proposalAsT= rewriteProposalToT(proposal, MoreArgumentsProposal1);
			if (proposalAsT != null)
				proposals.add(proposalAsT);
		}

		IMethodBinding methodDecl= methodRef.getMethodDeclaration();
		ITypeBinding declaringType= methodDecl.getDeclaringClass();

		// add parameters
		if (!declaringType.isFromSource() || (methodDecl.isConstructor() && declaringType.isRecord())) {
			return;
		}
		ICompilationUnit targetCU= ASTResolving.findCompilationUnitForBinding(cu, astRoot, declaringType);
		if (targetCU != null) {

			if (isImplicitConstructor(methodDecl)) {
				return;
			}

			ChangeDescription[] changeDesc= new ChangeDescription[argTypes.length];
			ITypeBinding[] changeTypes= new ITypeBinding[diff];
			for (int i= diff - 1; i >= 0; i--) {
				int idx= indexSkipped[i];
				Expression arg= arguments.get(idx);
				String name= getExpressionBaseName(arg);
				ITypeBinding newType= Bindings.normalizeTypeBinding(argTypes[idx]);
				if (newType == null) {
					newType= astRoot.getAST().resolveWellKnownType("java.lang.Object"); //$NON-NLS-1$
				}
				if (newType.isWildcardType()) {
					newType= ASTResolving.normalizeWildcardType(newType, true, astRoot.getAST());
				}
				if (!ASTResolving.isUseableTypeInContext(newType, methodDecl, false)) {
					return;
				}
				changeDesc[idx]= new InsertDescription(newType, name);
				changeTypes[i]= newType;
			}
			String[] arg= new String[] { ASTResolving.getMethodSignature(methodDecl), getTypeNames(changeTypes) };
			String label;
			if (methodDecl.isConstructor()) {
				if (diff == 1) {
					label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_addparam_constr_description, arg);
				} else {
					label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_addparams_constr_description, arg);
				}
			} else {
				if (diff == 1) {
					label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_addparam_description, arg);
				} else {
					label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_addparams_description, arg);
				}
			}
			ChangeMethodSignatureProposalCore proposal= new ChangeMethodSignatureProposalCore(label, targetCU, invocationNode, methodDecl, changeDesc, null, IProposalRelevance.CHANGE_METHOD_ADD_PARAMETER);
			T proposalAsT= changeMethodSignatureProposalToT(proposal, MoreArgumentsProposal2);
			if (proposalAsT != null)
				proposals.add(proposalAsT);
		}
	}

	private boolean isImplicitConstructor(IMethodBinding meth) {
		return meth.isDefaultConstructor();
	}

	private ITypeBinding[] getParameterTypes(List<Expression> args) {
		ITypeBinding[] params= new ITypeBinding[args.size()];
		for (int i= 0; i < args.size(); i++) {
			Expression expr= args.get(i);
			ITypeBinding curr= Bindings.normalizeTypeBinding(expr.resolveTypeBinding());
			if (curr != null && curr.isWildcardType()) {
				curr= ASTResolving.normalizeWildcardType(curr, true, expr.getAST());
			}
			if (curr == null) {
				curr= expr.getAST().resolveWellKnownType("java.lang.Object"); //$NON-NLS-1$
			}
			params[i]= curr;
		}
		return params;
	}

	// 1900
	private void doEqualNumberOfParameters(IInvocationContext context, ASTNode invocationNode, IProblemLocation problem, List<Expression> arguments, ITypeBinding[] argTypes, IMethodBinding methodBinding, Collection<T> proposals) throws CoreException {
		ITypeBinding[] paramTypes= methodBinding.getParameterTypes();
		int[] indexOfDiff= new int[paramTypes.length];
		int nDiffs= 0;
		for (int n= 0; n < argTypes.length; n++) {
			if (!canAssign(argTypes[n], paramTypes[n])) {
				indexOfDiff[nDiffs++]= n;
			}
		}
		ITypeBinding declaringTypeDecl= methodBinding.getDeclaringClass().getTypeDeclaration();

		ICompilationUnit cu= context.getCompilationUnit();
		CompilationUnit astRoot= context.getASTRoot();

		ASTNode nameNode= problem.getCoveringNode(astRoot);
		if (nameNode == null || ( methodBinding.isConstructor() && declaringTypeDecl.isRecord())) {
			return;
		}

		if (nDiffs == 0) {
			if (nameNode.getParent() instanceof MethodInvocation inv) {
				if (inv.getExpression() == null) {
					addQualifierToOuterProposal(context, inv, methodBinding, proposals);
				}
			}
			return;
		}

		if (nDiffs == 1) { // one argument mismatching: try to fix
			int idx= indexOfDiff[0];
			Expression nodeToCast= arguments.get(idx);
			ITypeBinding castType= paramTypes[idx];
			castType= Bindings.normalizeTypeBinding(castType);
			if (castType.isWildcardType()) {
				castType= ASTResolving.normalizeWildcardType(castType, false, nodeToCast.getAST());
			}
			if (castType != null) {
				ITypeBinding binding= nodeToCast.resolveTypeBinding();
				ITypeBinding castFixType= null;
				if (binding == null || castType.isCastCompatible(binding)) {
					castFixType= castType;
				} else if (JavaModelUtil.is50OrHigher(cu.getJavaProject())) {
					ITypeBinding boxUnboxedTypeBinding= TypeMismatchBaseSubProcessor.boxOrUnboxPrimitives(castType, binding, nodeToCast.getAST());
					if (boxUnboxedTypeBinding != castType && boxUnboxedTypeBinding.isCastCompatible(binding)) {
						castFixType= boxUnboxedTypeBinding;
					}
				}
				TypeMismatchBaseSubProcessor<T> sub= getTypeMismatchSubProcessor();
				if (castFixType != null) {
					String castTypeName= BindingLabelProviderCore.getBindingLabel(castFixType, JavaElementLabelsCore.ALL_DEFAULT);
					String[] arg= new String[] { getArgumentName(arguments, idx), castTypeName};
					String label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_addargumentcast_description, arg);
					T proposal= sub.collectCastProposals(label, context, castFixType, nodeToCast, IProposalRelevance.CAST_ARGUMENT_1);
					if (proposal != null)
						proposals.add(proposal);
				}

				sub.collectChangeSenderTypeProposals(context, nodeToCast, castType, false, IProposalRelevance.CAST_ARGUMENT_2, proposals);
			}
		}

		if (nDiffs == 2) { // try to swap
			int idx1= indexOfDiff[0];
			int idx2= indexOfDiff[1];
			boolean canSwap= canAssign(argTypes[idx1], paramTypes[idx2]) && canAssign(argTypes[idx2], paramTypes[idx1]);
			if (canSwap) {
				Expression arg1= arguments.get(idx1);
				Expression arg2= arguments.get(idx2);

				ASTRewrite rewrite= ASTRewrite.create(astRoot.getAST());
				rewrite.replace(arg1, rewrite.createCopyTarget(arg2), null);
				rewrite.replace(arg2, rewrite.createCopyTarget(arg1), null);
				{
					String[] arg= new String[] { getArgumentName(arguments, idx1), getArgumentName(arguments, idx2) };
					String label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_swaparguments_description, arg);
					ASTRewriteCorrectionProposalCore proposal= new ASTRewriteCorrectionProposalCore(label, context.getCompilationUnit(), rewrite, IProposalRelevance.SWAP_ARGUMENTS);
					T proposalToT= rewriteProposalToT(proposal, EqualNumberOfParameters1);
					if (proposalToT != null)
						proposals.add(proposalToT);
				}

				if (declaringTypeDecl.isFromSource()) {
					ICompilationUnit targetCU= ASTResolving.findCompilationUnitForBinding(cu, astRoot, declaringTypeDecl);
					if (targetCU != null) {
						ChangeDescription[] changeDesc= new ChangeDescription[paramTypes.length];
						for (int i= 0; i < nDiffs; i++) {
							changeDesc[idx1]= new SwapDescription(idx2);
						}
						IMethodBinding methodDecl= methodBinding.getMethodDeclaration();
						ITypeBinding[] declParamTypes= methodDecl.getParameterTypes();

						ITypeBinding[] swappedTypes= new ITypeBinding[] { declParamTypes[idx1], declParamTypes[idx2] };
						String[] args=  new String[] { ASTResolving.getMethodSignature(methodDecl), getTypeNames(swappedTypes) };
						String label;
						if (methodDecl.isConstructor()) {
							label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_swapparams_constr_description, args);
						} else {
							label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_swapparams_description, args);
						}
						ChangeMethodSignatureProposalCore proposal= new ChangeMethodSignatureProposalCore(label, targetCU, invocationNode, methodDecl, changeDesc, null, IProposalRelevance.CHANGE_METHOD_SWAP_PARAMETERS);
						T proposalToT= changeMethodSignatureProposalToT(proposal, EqualNumberOfParameters2);
						if (proposalToT != null)
							proposals.add(proposalToT);
					}
				}
				return;
			}
		}

		if (declaringTypeDecl.isFromSource()) {
			ICompilationUnit targetCU= ASTResolving.findCompilationUnitForBinding(cu, astRoot, declaringTypeDecl);
			if (targetCU != null) {
				ChangeDescription[] changeDesc= createSignatureChangeDescription(indexOfDiff, nDiffs, paramTypes, arguments, argTypes);
				if (changeDesc != null) {

					IMethodBinding methodDecl= methodBinding.getMethodDeclaration();
					ITypeBinding[] declParamTypes= methodDecl.getParameterTypes();

					ITypeBinding[] newParamTypes= new ITypeBinding[changeDesc.length];
					for (int i= 0; i < newParamTypes.length; i++) {
						newParamTypes[i]= changeDesc[i] == null ? declParamTypes[i] : ((EditDescription) changeDesc[i]).type;
					}
					if (methodDecl.isVarargs() && newParamTypes.length > 0 && !newParamTypes[newParamTypes.length - 1].isArray()) {
						List<ITypeBinding> newArgs= new ArrayList<>();
						newArgs.addAll(Arrays.asList(argTypes));
						newArgs.add(paramTypes[paramTypes.length - 1]);
						doMoreArguments(context, invocationNode, arguments, newArgs.toArray(new ITypeBinding[0]), methodBinding, proposals);
						return;
					}
					boolean isVarArgs= methodDecl.isVarargs() && newParamTypes.length > 0 && newParamTypes[newParamTypes.length - 1].isArray();
					String[] args=  new String[] { ASTResolving.getMethodSignature(methodDecl), ASTResolving.getMethodSignature(methodDecl.getName(), newParamTypes, isVarArgs) };
					String label;
					if (methodDecl.isConstructor()) {
						label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_changeparamsignature_constr_description, args);
					} else {
						label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_changeparamsignature_description, args);
					}
					//Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
					ChangeMethodSignatureProposalCore proposal= new ChangeMethodSignatureProposalCore(label, targetCU, invocationNode, methodDecl, changeDesc, null, IProposalRelevance.CHANGE_METHOD_SIGNATURE);
					T proposalToT= changeMethodSignatureProposalToT(proposal, EqualNumberOfParameters3);
					if (proposalToT != null)
						proposals.add(proposalToT);
				}
			}
		}
	}


	private ChangeDescription[] createSignatureChangeDescription(int[] indexOfDiff, int nDiffs, ITypeBinding[] paramTypes, List<Expression> arguments, ITypeBinding[] argTypes) {
		ChangeDescription[] changeDesc= new ChangeDescription[paramTypes.length];
		for (int i= 0; i < nDiffs; i++) {
			int diffIndex= indexOfDiff[i];
			Expression arg= arguments.get(diffIndex);
			String name= getExpressionBaseName(arg);
			ITypeBinding argType= argTypes[diffIndex];
			if (argType.isWildcardType()) {
				argType= ASTResolving.normalizeWildcardType(argType, true, arg.getAST());
				if (argType== null) {
					return null;
				}
			}
			changeDesc[diffIndex]= new EditDescription(argType, name);
		}
		return changeDesc;
	}

	private String getExpressionBaseName(Expression expr) {
		IBinding argBinding= Bindings.resolveExpressionBinding(expr, true);
		if (argBinding instanceof IVariableBinding varBinding) {
			IJavaProject project= null;
			ASTNode root= expr.getRoot();
			if (root instanceof CompilationUnit) {
				ITypeRoot typeRoot= ((CompilationUnit) root).getTypeRoot();
				if (typeRoot != null)
					project= typeRoot.getJavaProject();
			}
			return StubUtility.getBaseName(varBinding, project);
		}
		if (expr instanceof SimpleName sn)
			return sn.getIdentifier();
		return null;
	}

	private ITypeBinding[] getArgumentTypes(List<Expression> arguments) {
		ITypeBinding[] res= new ITypeBinding[arguments.size()];
		for (int i= 0; i < res.length; i++) {
			Expression expression= arguments.get(i);
			ITypeBinding curr= expression.resolveTypeBinding();
			if (curr == null) {
				return null;
			}
			if (!curr.isNullType()) {	// don't normalize null type
				curr= Bindings.normalizeTypeBinding(curr);
				if (curr == null) {
					curr= expression.getAST().resolveWellKnownType("java.lang.Object"); //$NON-NLS-1$
				}
			}
			res[i]= curr;
		}
		return res;
	}

	// 2000
	private void addQualifierToOuterProposal(IInvocationContext context, MethodInvocation invocationNode, IMethodBinding binding, Collection<T> proposals) {
		ITypeBinding declaringType= binding.getDeclaringClass();
		ITypeBinding parentType= Bindings.getBindingOfParentType(invocationNode);
		ITypeBinding currType= parentType;

		boolean isInstanceMethod= !Modifier.isStatic(binding.getModifiers());

		while (currType != null && !Bindings.isSuperType(declaringType, currType)) {
			if (isInstanceMethod && Modifier.isStatic(currType.getModifiers())) {
				return;
			}
			currType= currType.getDeclaringClass();
		}
		if (currType == null || currType == parentType) {
			return;
		}

		ASTRewrite rewrite= ASTRewrite.create(invocationNode.getAST());

		String label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_changetoouter_description, ASTResolving.getTypeSignature(currType));
		//Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		ASTRewriteCorrectionProposalCore coreProposal= new ASTRewriteCorrectionProposalCore(label, context.getCompilationUnit(), rewrite, IProposalRelevance.QUALIFY_WITH_ENCLOSING_TYPE);

		ImportRewrite imports= coreProposal.createImportRewrite(context.getASTRoot());
		ImportRewriteContext importRewriteContext= new ContextSensitiveImportRewriteContext(invocationNode, imports);
		AST ast= invocationNode.getAST();

		String qualifier= imports.addImport(currType, importRewriteContext);
		Name name= ASTNodeFactory.newName(ast, qualifier);

		Expression newExpression;
		if (isInstanceMethod) {
			ThisExpression expr= ast.newThisExpression();
			expr.setQualifier(name);
			newExpression= expr;
		} else {
			newExpression= name;
		}

		rewrite.set(invocationNode, MethodInvocation.EXPRESSION_PROPERTY, newExpression, null);
		T rewriteToT= rewriteProposalToT(coreProposal, AddQualifierToOuterProposal1);
		if (rewriteToT != null)
			proposals.add(rewriteToT);
	}

	// 2100
	public void collectConstructorProposals(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) throws CoreException {
		ICompilationUnit cu= context.getCompilationUnit();

		CompilationUnit astRoot= context.getASTRoot();
		ASTNode selectedNode= problem.getCoveringNode(astRoot);
		if (selectedNode == null) {
			return;
		}

		ITypeBinding targetBinding= null;
		List<Expression> arguments= null;
		IMethodBinding recursiveConstructor= null;

		int type= selectedNode.getNodeType();
		if (type == ASTNode.CLASS_INSTANCE_CREATION) {
			ClassInstanceCreation creation= (ClassInstanceCreation) selectedNode;

			ITypeBinding binding= creation.getType().resolveBinding();
			if (binding != null) {
				targetBinding= binding;
				arguments= creation.arguments();
			}
		} else if (type == ASTNode.SUPER_CONSTRUCTOR_INVOCATION) {
			ITypeBinding typeBinding= Bindings.getBindingOfParentType(selectedNode);
			if (typeBinding != null && !typeBinding.isAnonymous()) {
				targetBinding= typeBinding.getSuperclass();
				arguments= ((SuperConstructorInvocation) selectedNode).arguments();
			}
		} else if (type == ASTNode.CONSTRUCTOR_INVOCATION) {
			ITypeBinding typeBinding= Bindings.getBindingOfParentType(selectedNode);
			if (typeBinding != null && !typeBinding.isAnonymous()) {
				targetBinding= typeBinding;
				arguments= ((ConstructorInvocation) selectedNode).arguments();
				recursiveConstructor= ASTResolving.findParentMethodDeclaration(selectedNode).resolveBinding();
			}
		}

		if (selectedNode.getParent() instanceof EnumConstantDeclaration enumNode) {
			ITypeBinding typeBinding= Bindings.getBindingOfParentType(selectedNode);
			if (typeBinding != null && !typeBinding.isAnonymous()) {
				targetBinding= typeBinding;
				arguments= enumNode.arguments();
				selectedNode= enumNode;
			}
		}

		if (targetBinding == null) {
			return;
		}
		ArrayList<IMethodBinding> similarElements= new ArrayList<>();
		for (IMethodBinding curr : targetBinding.getDeclaredMethods()) {
			if (curr.isConstructor() && recursiveConstructor != curr) {
				similarElements.add(curr); // similar elements can contain a implicit default constructor
			}
		}

		addParameterMissmatchProposals(context, problem, similarElements, selectedNode, arguments, proposals);

		if (targetBinding.isFromSource()) {
			ITypeBinding targetDecl= targetBinding.getTypeDeclaration();

			ICompilationUnit targetCU= ASTResolving.findCompilationUnitForBinding(cu, astRoot, targetDecl);
			if (targetCU != null) {
				String[] args= new String[] { ASTResolving.getMethodSignature( ASTResolving.getTypeSignature(targetDecl), getParameterTypes(arguments), false) };
				String label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_createconstructor_description, args);
				NewMethodCorrectionProposalCore core= new NewMethodCorrectionProposalCore(label, targetCU, selectedNode, arguments, targetDecl, IProposalRelevance.CREATE_CONSTRUCTOR);
				T coreToT= newMethodProposalToT(core, ConstructorProposal1);
				if (coreToT != null)
					proposals.add(coreToT);
			}
		}
	}


	// 2200
	public void collectAmbiguosTypeReferenceProposals(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) throws CoreException {
		final ICompilationUnit cu= context.getCompilationUnit();
		int offset= problem.getOffset();
		int len= problem.getLength();

		for (IJavaElement curr : cu.codeSelect(offset, len)) {
			if (curr instanceof IType currType && !TypeFilter.isFiltered((IType) curr)) {
				String qualifiedTypeName= currType.getFullyQualifiedName('.');

				CompilationUnit root= context.getASTRoot();

				String label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_importexplicit_description, BasicElementLabels.getJavaElementName(qualifiedTypeName));
				ASTRewriteCorrectionProposalCore proposal= new ASTRewriteCorrectionProposalCore(label, cu, ASTRewrite.create(root.getAST()), IProposalRelevance.IMPORT_EXPLICIT);
				ImportRewrite imports= proposal.createImportRewrite(root);
				imports.addImport(qualifiedTypeName);
				T rewriteT= rewriteProposalToT(proposal, AmbiguosTypeReferenceProposal1);
				if (rewriteT != null)
					proposals.add(rewriteT);
			}
		}
	}


	// 2300
	public void collectArrayAccessProposals(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) {

		CompilationUnit root= context.getASTRoot();
		ASTNode selectedNode= problem.getCoveringNode(root);
		if (!(selectedNode instanceof MethodInvocation decl)) {
			return;
		}

		SimpleName nameNode= decl.getName();
		String methodName= nameNode.getIdentifier();

		for (IBinding binding : (new ScopeAnalyzer(root)).getDeclarationsInScope(nameNode, ScopeAnalyzer.METHODS)) {
			String currName= binding.getName();
			if (NameMatcher.isSimilarName(methodName, currName)) {
				String label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_arraychangetomethod_description, BasicElementLabels.getJavaElementName(currName));
				RenameNodeCorrectionProposalCore rename1= new RenameNodeCorrectionProposalCore(label, context.getCompilationUnit(), nameNode.getStartPosition(), nameNode.getLength(), currName, IProposalRelevance.ARRAY_CHANGE_TO_METHOD);
				T rename1t= renameNodeProposalToT(rename1, ArrayAccessProposal1);
				if (rename1t != null)
					proposals.add(rename1t);
			}
		}
		// always suggest 'length'
		String lengthId= "length"; //$NON-NLS-1$
		String label= CorrectionMessages.UnresolvedElementsSubProcessor_arraychangetolength_description;
		int offset= nameNode.getStartPosition();
		int length= decl.getStartPosition() + decl.getLength() - offset;
		RenameNodeCorrectionProposalCore rename2= new RenameNodeCorrectionProposalCore(label, context.getCompilationUnit(), offset, length, lengthId, IProposalRelevance.ARRAY_CHANGE_TO_LENGTH);
		T rename2t= renameNodeProposalToT(rename2, ArrayAccessProposal2);
		if (rename2t != null)
			proposals.add(rename2t);
	}

	// 2400
	public void collectAnnotationMemberProposals(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) throws CoreException {
		CompilationUnit astRoot= context.getASTRoot();
		ICompilationUnit cu= context.getCompilationUnit();
		ASTNode selectedNode= problem.getCoveringNode(astRoot);

		Annotation annotation;
		String memberName;
		if (selectedNode.getLocationInParent() == MemberValuePair.NAME_PROPERTY) {
			if (selectedNode.getParent().getLocationInParent() != NormalAnnotation.VALUES_PROPERTY) {
				return;
			}
			annotation= (Annotation) selectedNode.getParent().getParent();
			memberName= ((SimpleName) selectedNode).getIdentifier();
		} else if (selectedNode.getLocationInParent() == SingleMemberAnnotation.VALUE_PROPERTY) {
			annotation= (Annotation) selectedNode.getParent();
			memberName= "value"; //$NON-NLS-1$
		} else {
			return;
		}

		ITypeBinding annotBinding= annotation.resolveTypeBinding();
		if (annotBinding == null) {
			return;
		}


		if (annotation instanceof NormalAnnotation) {
			// similar names
			for (IMethodBinding binding : annotBinding.getDeclaredMethods()) {
				String curr= binding.getName();
				int relevance= NameMatcher.isSimilarName(memberName, curr) ? IProposalRelevance.CHANGE_TO_ATTRIBUTE_SIMILAR_NAME : IProposalRelevance.CHANGE_TO_ATTRIBUTE;
				String label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_UnresolvedElementsSubProcessor_changetoattribute_description, BasicElementLabels.getJavaElementName(curr));
				RenameNodeCorrectionProposalCore core= new RenameNodeCorrectionProposalCore(label, cu, problem.getOffset(), problem.getLength(), curr, relevance);
				T renameNodeCorrectionProposal= renameNodeProposalToT(core, AnnotationMemberProposal1);
				if (renameNodeCorrectionProposal != null)
					proposals.add(renameNodeCorrectionProposal);
			}
		}

		if (annotBinding.isFromSource()) {
			ICompilationUnit targetCU= ASTResolving.findCompilationUnitForBinding(cu, astRoot, annotBinding);
			if (targetCU != null) {
				String label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_UnresolvedElementsSubProcessor_createattribute_description, BasicElementLabels.getJavaElementName(memberName));
				NewAnnotationMemberProposalCore core= new NewAnnotationMemberProposalCore(label, targetCU, selectedNode, annotBinding, IProposalRelevance.CREATE_ATTRIBUTE);
				T prop= newAnnotationProposalToT(core, AnnotationMemberProposal2);
				if (prop != null)
					proposals.add(prop);
			}
		}
	}

	protected abstract ReorgCorrectionsBaseSubProcessor<T> getReorgSubProcessor();
	protected abstract TypeMismatchBaseSubProcessor<T> getTypeMismatchSubProcessor();
	protected abstract ChangeCorrectionProposalCore getOriginalProposalFromT(T proposal);
	protected abstract T newVariableCorrectionProposalToT(NewVariableCorrectionProposalCore core, int uid);
	protected abstract T renameNodeCorrectionProposalToT(RenameNodeCorrectionProposalCore core, int uid);
	protected abstract T compositeProposalToT(ChangeCorrectionProposalCore compositeProposal, int uid);
	protected abstract int getQualifiedTypeNameHistoryBoost(String qualifiedName, int min, int max);
	protected abstract T linkedProposalToT(LinkedCorrectionProposalCore core, int uid);
	protected abstract T changeCorrectionProposalToT(ChangeCorrectionProposalCore core, int uid);
	protected abstract T qualifyTypeProposalToT(QualifyTypeProposalCore core, int uid);
	protected abstract T addTypeParametersToT(AddTypeParameterProposalCore core, int uid);
	protected abstract T addModuleRequiresProposalToT(AddModuleRequiresCorrectionProposalCore core, int uid);
	protected abstract T replaceCorrectionProposalToT(ReplaceCorrectionProposalCore core, int uid);
	protected abstract T castCorrectionProposalToT(CastCorrectionProposalCore core, int uid);
	protected abstract T addArgumentCorrectionProposalToT(AddArgumentCorrectionProposalCore core, int uid);
	protected abstract T changeMethodSignatureProposalToT(ChangeMethodSignatureProposalCore core, int uid);
	protected abstract T newMethodProposalToT(NewMethodCorrectionProposalCore core, int uid);
	protected abstract T rewriteProposalToT(ASTRewriteCorrectionProposalCore core, int uid);
	protected abstract T newAnnotationProposalToT(NewAnnotationMemberProposalCore core, int uid);
	protected abstract T renameNodeProposalToT(RenameNodeCorrectionProposalCore core, int uid);

}
