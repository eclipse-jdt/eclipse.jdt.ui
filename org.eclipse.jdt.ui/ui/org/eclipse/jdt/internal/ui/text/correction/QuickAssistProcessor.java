/*******************************************************************************
 * Copyright (c) 2000, 2024 IBM Corporation and others.
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
 *     Sebastian Davids <sdavids@gmx.de> - Bug 37432 getInvertEqualsProposal
 *     Benjamin Muskalla <b.muskalla@gmx.net> - Bug 36350 convertToStringBufferPropsal
 *     Chris West (Faux) <eclipse@goeswhere.com> - [quick assist] "Use 'StringBuilder' for string concatenation" could fix existing misuses - https://bugs.eclipse.org/bugs/show_bug.cgi?id=282755
 *     Lukas Hanke <hanke@yatta.de> - Bug 241696 [quick fix] quickfix to iterate over a collection - https://bugs.eclipse.org/bugs/show_bug.cgi?id=241696
 *     Eugene Lucash <e.lucash@gmail.com> - [quick assist] Add key binding for Extract method Quick Assist - https://bugs.eclipse.org/424166
 *     Lukas Hanke <hanke@yatta.de> - Bug 430818 [1.8][quick fix] Quick fix for "for loop" is not shown for bare local variable/argument/field - https://bugs.eclipse.org/bugs/show_bug.cgi?id=430818
 *     Jeremie Bresson <dev@jmini.fr> - Bug 439912: [1.8][quick assist] Add quick assists to add and remove parentheses around single lambda parameter - https://bugs.eclipse.org/439912
 *     Jens Reimann <jens.reimann@ibh-systems.com>, Fabian Pfaff <fabian.pfaff@vogella.com> - Bug 197850: [quick assist] Add import static field/method - https://bugs.eclipse.org/bugs/show_bug.cgi?id=197850
 *     Pierre-Yves B. <pyvesdev@gmail.com> - [inline] Allow inlining of local variable initialized to null. - https://bugs.eclipse.org/93850
 *     Red Hat Inc. - refactor some static methods to QuickAssistProcessorUtil class in jdt.core.manipulation
 *     Red Hat Inc - separate core logic from UI images
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.osgi.util.NLS;

import org.eclipse.swt.graphics.Image;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.eclipse.jface.text.link.LinkedPositionGroup;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.preferences.WorkingCopyManager;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.Refactoring;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.NamingConventions;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Dimension;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.MethodReference;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NameQualifiedType;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchExpression;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.UnionType;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.TypeLocation;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.core.manipulation.dom.ASTResolving;
import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.core.manipulation.util.Strings;
import org.eclipse.jdt.internal.corext.codemanipulation.ContextSensitiveImportRewriteContext;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.CodeScopeBuilder;
import org.eclipse.jdt.internal.corext.dom.DimensionRewrite;
import org.eclipse.jdt.internal.corext.dom.ModifierRewrite;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.dom.SelectionAnalyzer;
import org.eclipse.jdt.internal.corext.dom.TokenScanner;
import org.eclipse.jdt.internal.corext.fix.AddInferredLambdaParameterTypesFixCore;
import org.eclipse.jdt.internal.corext.fix.AddMissingMethodDeclarationFixCore;
import org.eclipse.jdt.internal.corext.fix.AddVarLambdaParameterTypesFixCore;
import org.eclipse.jdt.internal.corext.fix.ChangeLambdaBodyToBlockFixCore;
import org.eclipse.jdt.internal.corext.fix.ChangeLambdaBodyToExpressionFixCore;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.ControlStatementsFix;
import org.eclipse.jdt.internal.corext.fix.ConvertLambdaToMethodReferenceFixCore;
import org.eclipse.jdt.internal.corext.fix.ConvertLoopFixCore;
import org.eclipse.jdt.internal.corext.fix.DoWhileRatherThanWhileFixCore;
import org.eclipse.jdt.internal.corext.fix.FixMessages;
import org.eclipse.jdt.internal.corext.fix.IProposableFix;
import org.eclipse.jdt.internal.corext.fix.InlineMethodFixCore;
import org.eclipse.jdt.internal.corext.fix.InvertEqualsExpressionFixCore;
import org.eclipse.jdt.internal.corext.fix.JoinVariableFixCore;
import org.eclipse.jdt.internal.corext.fix.LambdaExpressionsFixCore;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalModelCore;
import org.eclipse.jdt.internal.corext.fix.RemoveVarOrInferredLambdaParameterTypesFixCore;
import org.eclipse.jdt.internal.corext.fix.SplitTryResourceFixCore;
import org.eclipse.jdt.internal.corext.fix.SplitVariableFixCore;
import org.eclipse.jdt.internal.corext.fix.StringConcatToTextBlockFixCore;
import org.eclipse.jdt.internal.corext.fix.SwitchExpressionsFixCore;
import org.eclipse.jdt.internal.corext.fix.TypeParametersFixCore;
import org.eclipse.jdt.internal.corext.fix.UnnecessaryArrayCreationFix;
import org.eclipse.jdt.internal.corext.fix.VariableDeclarationFixCore;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringAvailabilityTesterCore;
import org.eclipse.jdt.internal.corext.refactoring.code.ConvertAnonymousToNestedRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.code.ExtractConstantRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.code.ExtractMethodRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.code.ExtractTempRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.code.InlineTempRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.code.PromoteTempToFieldRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.structure.ImportRemover;
import org.eclipse.jdt.internal.corext.refactoring.surround.SurroundWithTryWithResourcesAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.surround.SurroundWithTryWithResourcesRefactoringCore;
import org.eclipse.jdt.internal.corext.refactoring.util.TightSourceRangeComputer;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.cleanup.ICleanUp;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickAssistProcessor;
import org.eclipse.jdt.ui.text.java.correction.ASTRewriteCorrectionProposal;
import org.eclipse.jdt.ui.text.java.correction.ChangeCorrectionProposal;
import org.eclipse.jdt.ui.text.java.correction.ICommandAccess;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.fix.ControlStatementsCleanUp;
import org.eclipse.jdt.internal.ui.fix.ConvertLoopCleanUp;
import org.eclipse.jdt.internal.ui.fix.DoWhileRatherThanWhileCleanUp;
import org.eclipse.jdt.internal.ui.fix.LambdaExpressionsCleanUp;
import org.eclipse.jdt.internal.ui.fix.StringConcatToTextBlockCleanUp;
import org.eclipse.jdt.internal.ui.fix.SwitchExpressionsCleanUp;
import org.eclipse.jdt.internal.ui.fix.TypeParametersCleanUp;
import org.eclipse.jdt.internal.ui.fix.UnnecessaryArrayCreationCleanUp;
import org.eclipse.jdt.internal.ui.fix.VariableDeclarationCleanUp;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock;
import org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock.Key;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ASTRewriteRemoveImportsCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.AddStaticFavoriteProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.AssignToVariableAssistProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ConvertFieldNamingConventionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.FixCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.GenerateForLoopAssistProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.LinkedCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.LinkedNamesAssistProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.NewDefiningMethodProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.NewInterfaceImplementationProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.RefactoringCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.RefactoringCorrectionProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.RenameRefactoringProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.TypeChangeCorrectionProposal;
import org.eclipse.jdt.internal.ui.util.ASTHelper;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;

public class QuickAssistProcessor implements IQuickAssistProcessor {

	public static final String SPLIT_JOIN_VARIABLE_DECLARATION_ID= "org.eclipse.jdt.ui.correction.splitJoinVariableDeclaration.assist"; //$NON-NLS-1$

	public static final String CONVERT_FOR_LOOP_ID= "org.eclipse.jdt.ui.correction.convertForLoop.assist"; //$NON-NLS-1$

	public static final String ASSIGN_TO_LOCAL_ID= "org.eclipse.jdt.ui.correction.assignToLocal.assist"; //$NON-NLS-1$

	public static final String ASSIGN_IN_TRY_WITH_RESOURCES_ID= "org.eclipse.jdt.ui.correction.assignInTryWithResources.assist"; //$NON-NLS-1$

	public static final String ASSIGN_TO_FIELD_ID= "org.eclipse.jdt.ui.correction.assignToField.assist"; //$NON-NLS-1$

	public static final String ASSIGN_PARAM_TO_FIELD_ID= "org.eclipse.jdt.ui.correction.assignParamToField.assist"; //$NON-NLS-1$

	public static final String ASSIGN_ALL_PARAMS_TO_NEW_FIELDS_ID= "org.eclipse.jdt.ui.correction.assignAllParamsToNewFields.assist"; //$NON-NLS-1$

	public static final String ADD_BLOCK_ID= "org.eclipse.jdt.ui.correction.addBlock.assist"; //$NON-NLS-1$

	public static final String EXTRACT_LOCAL_ID= "org.eclipse.jdt.ui.correction.extractLocal.assist"; //$NON-NLS-1$

	public static final String EXTRACT_LOCAL_NOT_REPLACE_ID= "org.eclipse.jdt.ui.correction.extractLocalNotReplaceOccurrences.assist"; //$NON-NLS-1$

	public static final String EXTRACT_CONSTANT_ID= "org.eclipse.jdt.ui.correction.extractConstant.assist"; //$NON-NLS-1$

	public static final String INLINE_LOCAL_ID= "org.eclipse.jdt.ui.correction.inlineLocal.assist"; //$NON-NLS-1$

	public static final String CONVERT_LOCAL_TO_FIELD_ID= "org.eclipse.jdt.ui.correction.convertLocalToField.assist"; //$NON-NLS-1$

	public static final String CONVERT_ANONYMOUS_TO_LOCAL_ID= "org.eclipse.jdt.ui.correction.convertAnonymousToLocal.assist"; //$NON-NLS-1$

	public static final String CONVERT_TO_STRING_BUFFER_ID= "org.eclipse.jdt.ui.correction.convertToStringBuffer.assist"; //$NON-NLS-1$

	public static final String CONVERT_TO_MESSAGE_FORMAT_ID= "org.eclipse.jdt.ui.correction.convertToMessageFormat.assist"; //$NON-NLS-1$

	public static final String CONVERT_TO_STRING_FORMAT_ID= "org.eclipse.jdt.ui.correction.convertToStringFormat.assist"; //$NON-NLS-1$

	public static final String EXTRACT_METHOD_INPLACE_ID= "org.eclipse.jdt.ui.correction.extractMethodInplace.assist"; //$NON-NLS-1$

	public static final String REMOVE_UNNECESSARY_ARRAY_CREATION_ID= "org.eclipse.jdt.ui.correction.removeArrayCreation.assist"; //$NON-NLS-1$

	public static final String FIELD_NAMING_CONVENTION_ID= "org.eclipse.jdt.ui.correction.convertToConstantNamingConvention.assist"; //$NON-NLS-1$

	public static final Pattern FIELD_NAMING_PATTERN= Pattern.compile("^[A-Z0-9]+(_[A-Z0-9]+)*$"); //$NON-NLS-1$

	public QuickAssistProcessor() {
		super();
	}

	@Override
	public boolean hasAssists(IInvocationContext context) throws CoreException {
		ASTNode coveringNode= context.getCoveringNode();
		if (coveringNode != null) {
			ArrayList<ASTNode> coveredNodes= AdvancedQuickAssistProcessor.getFullyCoveredNodes(context, coveringNode);
			return getCatchClauseToThrowsProposals(context, coveringNode, null)
					|| getPickoutTypeFromMulticatchProposals(context, coveringNode, coveredNodes, null)
					|| getConvertToMultiCatchProposals(context, coveringNode, null)
					|| getUnrollMultiCatchProposals(context, coveringNode, null)
					|| getRenameLocalProposals(context, coveringNode, null, null)
					|| getRenameRefactoringProposal(context, coveringNode, null, null)
					|| getAssignToVariableProposals(context, coveringNode, null, null)
					|| getUnWrapProposals(context, coveringNode, null)
					|| getAssignParamToFieldProposals(context, coveringNode, null)
					|| getAssignAllParamsToFieldsProposals(context, coveringNode, null)
					|| getJoinVariableProposals(context, coveringNode, null)
					|| getAddFinallyProposals(context, coveringNode, null)
					|| getAddElseProposals(context, coveringNode, null)
					|| getSplitVariableProposals(context, coveringNode, null)
					|| getAddBlockProposals(context, coveringNode, null)
					|| getTryWithResourceProposals(context, coveringNode, null, null)
					|| getArrayInitializerToArrayCreation(context, coveringNode, null)
					|| getCreateInSuperClassProposals(context, coveringNode, null)
					|| getInvertEqualsProposal(context, coveringNode, null)
					|| getConvertForLoopProposal(context, coveringNode, null)
					|| getConvertIterableLoopProposal(context, coveringNode, null)
					|| getConvertEnhancedForLoopProposal(context, coveringNode, null)
					|| getGenerateForLoopProposals(context, coveringNode, null, null)
					|| getUnnecessaryArrayCreationProposal(context, coveringNode, null)
					|| getExtractVariableProposal(context, false, null)
					|| getExtractMethodProposal(context, coveringNode, false, null)
					|| getExtractMethodFromLambdaProposal(context, coveringNode, false, null)
					|| getInlineLocalProposal(context, coveringNode, null)
					|| getConvertFieldNamingConventionProposal(context, coveringNode, null)
					|| getConvertLocalToFieldProposal(context, coveringNode, null)
					|| getConvertAnonymousToNestedProposal(context, coveringNode, null)
					|| getConvertAnonymousClassCreationsToLambdaProposals(context, coveringNode, null)
					|| getConvertLambdaToAnonymousClassCreationsProposals(context, coveringNode, null)
					|| getChangeLambdaBodyToBlockProposal(context, coveringNode, null)
					|| getChangeLambdaBodyToExpressionProposal(context, coveringNode, null)
					|| getAddInferredLambdaParameterTypes(context, coveringNode, null)
					|| getAddVarLambdaParameterTypes(context, coveringNode, null)
					|| getAddMethodDeclaration(context, coveringNode, null)
					|| getRemoveVarOrInferredLambdaParameterTypes(context, coveringNode, null)
					|| getConvertMethodReferenceToLambdaProposal(context, coveringNode, null)
					|| getConvertLambdaToMethodReferenceProposal(context, coveringNode, null)
					|| getConvertToSwitchExpressionProposals(context, coveringNode, null)
					|| getFixParenthesesInLambdaExpression(context, coveringNode, null)
					|| getRemoveBlockProposals(context, coveringNode, null)
					|| getMakeVariableDeclarationFinalProposals(context, null)
					|| getMissingCaseStatementProposals(context, coveringNode, null)
					|| ConvertStringConcatenationProposals.getProposals(context, null)
					|| getInferDiamondArgumentsProposal(context, coveringNode, null, null)
					|| getJUnitTestCaseProposal(context, coveringNode, null)
					|| getNewImplementationProposal(context, coveringNode, null)
					|| getNewInterfaceImplementationProposal(context, coveringNode, null)
					|| getAddStaticImportProposals(context, coveringNode, null)
					|| getDoWhileRatherThanWhileProposal(context, coveringNode, null)
					|| getStringConcatToTextBlockProposal(context, coveringNode, null)
					|| getAddStaticMemberFavoritesProposals(coveringNode, null)
					|| getSplitSwitchLabelProposal(context, coveringNode, null)
					|| getSplitTryResourceProposal(context, coveringNode, null)
					|| getDeprecatedProposal(context, coveringNode, null, null);
		}
		return false;
	}

	@Override
	public IJavaCompletionProposal[] getAssists(IInvocationContext context, IProblemLocation[] locations) throws CoreException {
		ASTNode coveringNode= context.getCoveringNode();
		if (coveringNode != null) {
			ArrayList<ASTNode> coveredNodes= AdvancedQuickAssistProcessor.getFullyCoveredNodes(context, coveringNode);
			ArrayList<ICommandAccess> resultingCollections= new ArrayList<>();
			boolean noErrorsAtLocation= noErrorsAtLocation(locations);

			// quick assists that show up also if there is an error/warning
			getRenameLocalProposals(context, coveringNode, locations, resultingCollections);
			getRenameRefactoringProposal(context, coveringNode, locations, resultingCollections);
			getAssignToVariableProposals(context, coveringNode, locations, resultingCollections);
			getAssignParamToFieldProposals(context, coveringNode, resultingCollections);
			getAssignAllParamsToFieldsProposals(context, coveringNode, resultingCollections);
			getInferDiamondArgumentsProposal(context, coveringNode, locations, resultingCollections);
			getGenerateForLoopProposals(context, coveringNode, locations, resultingCollections);
			getJUnitTestCaseProposal(context, coveringNode, resultingCollections);
			getNewImplementationProposal(context, coveringNode, resultingCollections);
			getNewInterfaceImplementationProposal(context, coveringNode, resultingCollections);
			getSplitSwitchLabelProposal(context, coveringNode, resultingCollections);
			getAddMethodDeclaration(context, coveringNode, resultingCollections);
			getDeprecatedProposal(context, coveringNode, locations, resultingCollections);

			if (noErrorsAtLocation) {
				boolean problemsAtLocation= locations.length != 0;
				getCatchClauseToThrowsProposals(context, coveringNode, resultingCollections);
				getPickoutTypeFromMulticatchProposals(context, coveringNode, coveredNodes, resultingCollections);
				getConvertToMultiCatchProposals(context, coveringNode, resultingCollections);
				getUnrollMultiCatchProposals(context, coveringNode, resultingCollections);
				getTryWithResourceAssistProposals(locations, context, coveringNode, coveredNodes, resultingCollections);
				getUnWrapProposals(context, coveringNode, resultingCollections);
				getJoinVariableProposals(context, coveringNode, resultingCollections);
				getSplitVariableProposals(context, coveringNode, resultingCollections);
				getAddFinallyProposals(context, coveringNode, resultingCollections);
				getAddElseProposals(context, coveringNode, resultingCollections);
				getAddBlockProposals(context, coveringNode, resultingCollections);
				getInvertEqualsProposal(context, coveringNode, resultingCollections);
				getArrayInitializerToArrayCreation(context, coveringNode, resultingCollections);
				getCreateInSuperClassProposals(context, coveringNode, resultingCollections);
				getExtractVariableProposal(context, problemsAtLocation, resultingCollections);
				getExtractMethodProposal(context, coveringNode, problemsAtLocation, resultingCollections);
				getExtractMethodFromLambdaProposal(context, coveringNode, problemsAtLocation, resultingCollections);
				getInlineLocalProposal(context, coveringNode, resultingCollections);
				getConvertFieldNamingConventionProposal(context, coveringNode, resultingCollections);
				getConvertLocalToFieldProposal(context, coveringNode, resultingCollections);
				getConvertAnonymousToNestedProposal(context, coveringNode, resultingCollections);
				getConvertAnonymousClassCreationsToLambdaProposals(context, coveringNode, resultingCollections);
				getConvertLambdaToAnonymousClassCreationsProposals(context, coveringNode, resultingCollections);
				getChangeLambdaBodyToBlockProposal(context, coveringNode, resultingCollections);
				getChangeLambdaBodyToExpressionProposal(context, coveringNode, resultingCollections);
				getAddInferredLambdaParameterTypes(context, coveringNode, resultingCollections);
				getAddVarLambdaParameterTypes(context, coveringNode, resultingCollections);
				getRemoveVarOrInferredLambdaParameterTypes(context, coveringNode, resultingCollections);
				getConvertMethodReferenceToLambdaProposal(context, coveringNode, resultingCollections);
				getConvertLambdaToMethodReferenceProposal(context, coveringNode, resultingCollections);
				getFixParenthesesInLambdaExpression(context, coveringNode, resultingCollections);
				if (!getConvertForLoopProposal(context, coveringNode, resultingCollections))
					getConvertIterableLoopProposal(context, coveringNode, resultingCollections);
				getUnnecessaryArrayCreationProposal(context, coveringNode, resultingCollections);
				getConvertEnhancedForLoopProposal(context, coveringNode, resultingCollections);
				getRemoveBlockProposals(context, coveringNode, resultingCollections);
				getMakeVariableDeclarationFinalProposals(context, resultingCollections);
				ConvertStringConcatenationProposals.getProposals(context, resultingCollections);
				getMissingCaseStatementProposals(context, coveringNode, resultingCollections);
				getConvertVarTypeToResolvedTypeProposal(context, coveringNode, resultingCollections);
				getConvertResolvedTypeToVarTypeProposal(context, coveringNode, resultingCollections);
				getAddStaticImportProposals(context, coveringNode, resultingCollections);
				getAddStaticMemberFavoritesProposals(coveringNode, resultingCollections);
				getConvertToSwitchExpressionProposals(context, coveringNode, resultingCollections);
				getDoWhileRatherThanWhileProposal(context, coveringNode, resultingCollections);
				getStringConcatToTextBlockProposal(context, coveringNode, resultingCollections);
				getSplitTryResourceProposal(context, coveringNode, resultingCollections);
			}
			return resultingCollections.toArray(new IJavaCompletionProposal[resultingCollections.size()]);
		}
		return null;
	}

	static boolean noErrorsAtLocation(IProblemLocation[] locations) {
		if (locations != null) {
			for (IProblemLocation location : locations) {
				if (location.isError()) {
					if (IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER.equals(location.getMarkerType())
							&& JavaCore.getOptionForConfigurableSeverity(location.getProblemId()) != null) {
						// continue (only drop out for severe (non-optional) errors)
					} else {
						return false;
					}
				}
			}
		}
		return true;
	}

	private static boolean getExtractMethodProposal(IInvocationContext context, ASTNode coveringNode, boolean problemsAtLocation, Collection<ICommandAccess> proposals) throws CoreException {
		if (!(coveringNode instanceof Expression) && !(coveringNode instanceof Statement) && !(coveringNode instanceof Block)) {
			return false;
		}
		if (coveringNode instanceof Block) {
			List<Statement> statements= ((Block) coveringNode).statements();
			int startIndex= QuickAssistProcessorUtil.getIndex(context.getSelectionOffset(), statements);
			if (startIndex == -1)
				return false;
			int endIndex= QuickAssistProcessorUtil.getIndex(context.getSelectionOffset() + context.getSelectionLength(), statements);
			if (endIndex == -1 || endIndex <= startIndex)
				return false;
		}

		final ICompilationUnit cu= context.getCompilationUnit();
		final ExtractMethodRefactoring extractMethodRefactoring= new ExtractMethodRefactoring(context.getASTRoot(), context.getSelectionOffset(), context.getSelectionLength());
		extractMethodRefactoring.setMethodName("extracted"); //$NON-NLS-1$
		if (extractMethodRefactoring.checkInitialConditions(new NullProgressMonitor()).isOK()) {
			if (proposals == null) {
				return true;
			}
			String label= CorrectionMessages.QuickAssistProcessor_extractmethod_description;
			LinkedProposalModelCore linkedProposalModel= createProposalModel();
			extractMethodRefactoring.setLinkedProposalModel(linkedProposalModel);

			Image image= JavaPluginImages.get(JavaPluginImages.IMG_MISC_PUBLIC);
			int relevance= problemsAtLocation ? IProposalRelevance.EXTRACT_METHOD_ERROR : IProposalRelevance.EXTRACT_METHOD;
			RefactoringCorrectionProposal proposal= new RefactoringCorrectionProposal(label, cu, extractMethodRefactoring, relevance, image);
			proposal.setCommandId(EXTRACT_METHOD_INPLACE_ID);
			proposal.setLinkedProposalModel(linkedProposalModel);
			proposals.add(proposal);
			return true;
		}
		return false;
	}

	private static boolean getExtractMethodFromLambdaProposal(IInvocationContext context, ASTNode coveringNode, boolean problemsAtLocation, Collection<ICommandAccess> proposals) throws CoreException {
		if (coveringNode instanceof Block && coveringNode.getLocationInParent() == LambdaExpression.BODY_PROPERTY) {
			return false;
		}
		ASTNode node= ASTNodes.getFirstAncestorOrNull(coveringNode, LambdaExpression.class, BodyDeclaration.class);
		if (!(node instanceof LambdaExpression)) {
			return false;
		}
		ASTNode body= ((LambdaExpression) node).getBody();
		final ICompilationUnit cu= context.getCompilationUnit();
		final ExtractMethodRefactoring extractMethodRefactoring= new ExtractMethodRefactoring(context.getASTRoot(), body.getStartPosition(), body.getLength());
		extractMethodRefactoring.setMethodName("extracted"); //$NON-NLS-1$
		if (extractMethodRefactoring.checkInitialConditions(new NullProgressMonitor()).isOK()) {
			if (proposals == null) {
				return true;
			}
			String label= CorrectionMessages.QuickAssistProcessor_extractmethod_from_lambda_description;
			LinkedProposalModelCore linkedProposalModel= createProposalModel();
			extractMethodRefactoring.setLinkedProposalModel(linkedProposalModel);

			Image image= JavaPluginImages.get(JavaPluginImages.IMG_MISC_PUBLIC);
			int relevance= problemsAtLocation ? IProposalRelevance.EXTRACT_METHOD_ERROR : IProposalRelevance.EXTRACT_LAMBDA_BODY_TO_METHOD;
			RefactoringCorrectionProposal proposal= new RefactoringCorrectionProposal(label, cu, extractMethodRefactoring, relevance, image);
			proposal.setLinkedProposalModel(linkedProposalModel);
			proposals.add(proposal);
			return true;
		}
		return false;
	}

	private static boolean getExtractVariableProposal(IInvocationContext context, boolean problemsAtLocation, Collection<ICommandAccess> proposals) throws CoreException {

		ASTNode node= context.getCoveredNode();

		if (!(node instanceof Expression)) {
			if (context.getSelectionLength() != 0) {
				return false;
			}
			node= context.getCoveringNode();
			if (!(node instanceof Expression)) {
				return false;
			}
		}
		final Expression expression= (Expression) node;

		ITypeBinding binding= expression.resolveTypeBinding();
		if (binding == null || Bindings.isVoidType(binding)) {
			return false;
		}
		if (proposals == null) {
			return true;
		}

		final ICompilationUnit cu= context.getCompilationUnit();

		ExtractTempRefactoring extractTempRefactoringSelectedOnly= new ExtractTempRefactoring(context.getASTRoot(), context.getSelectionOffset(), context.getSelectionLength());
		extractTempRefactoringSelectedOnly.setReplaceAllOccurrences(false);
		if (extractTempRefactoringSelectedOnly.checkInitialConditions(new NullProgressMonitor()).isOK()) {
			LinkedProposalModelCore linkedProposalModel= createProposalModel();
			extractTempRefactoringSelectedOnly.setLinkedProposalModel(linkedProposalModel);
			extractTempRefactoringSelectedOnly.setCheckResultForCompileProblems(false);

			String label= CorrectionMessages.QuickAssistProcessor_extract_to_local_description;
			String preview= CorrectionMessages.QuickAssistProcessor_extract_to_local_preview;
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL);
			int relevance;
			if (context.getSelectionLength() == 0) {
				relevance= IProposalRelevance.EXTRACT_LOCAL_ZERO_SELECTION;
			} else if (problemsAtLocation) {
				relevance= IProposalRelevance.EXTRACT_LOCAL_ERROR;
			} else {
				relevance= IProposalRelevance.EXTRACT_LOCAL;
			}
			ExtractTempRefactoringProposalCore core= new ExtractTempRefactoringProposalCore(label, cu, extractTempRefactoringSelectedOnly, relevance, preview);
			RefactoringCorrectionProposal proposal= new RefactoringCorrectionProposalExtension(label, cu, relevance, image, core);

			proposal.setCommandId(EXTRACT_LOCAL_NOT_REPLACE_ID);
			proposal.setLinkedProposalModel(linkedProposalModel);
			proposals.add(proposal);
		}

		ExtractTempRefactoring extractTempRefactoring= new ExtractTempRefactoring(context.getASTRoot(), context.getSelectionOffset(), context.getSelectionLength());
		if (extractTempRefactoring.checkInitialConditions(new NullProgressMonitor()).isOK()) {
			extractTempRefactoring.setReplaceAllOccurrences(true);
			LinkedProposalModelCore linkedProposalModel= createProposalModel();
			extractTempRefactoring.setLinkedProposalModel(linkedProposalModel);
			extractTempRefactoring.setCheckResultForCompileProblems(false);

			String label= CorrectionMessages.QuickAssistProcessor_extract_to_local_all_description;
			String preview= CorrectionMessages.QuickAssistProcessor_extract_to_local_all_preview;
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL);
			int relevance;
			if (context.getSelectionLength() == 0) {
				relevance= IProposalRelevance.EXTRACT_LOCAL_ALL_ZERO_SELECTION;
			} else if (problemsAtLocation) {
				relevance= IProposalRelevance.EXTRACT_LOCAL_ALL_ERROR;
			} else {
				relevance= IProposalRelevance.EXTRACT_LOCAL_ALL;
			}
			ExtractTempRefactoringProposalCore core= new ExtractTempRefactoringProposalCore(label, cu,
					extractTempRefactoring, relevance, preview);
			RefactoringCorrectionProposal proposal= new RefactoringCorrectionProposalExtension(label, cu, relevance, image, core);

			proposal.setCommandId(EXTRACT_LOCAL_ID);
			proposal.setLinkedProposalModel(linkedProposalModel);
			proposals.add(proposal);
		}

		ExtractConstantRefactoring extractConstRefactoring= new ExtractConstantRefactoring(context.getASTRoot(), context.getSelectionOffset(), context.getSelectionLength());
		if (extractConstRefactoring.checkInitialConditions(new NullProgressMonitor()).isOK() && extractConstRefactoring.selectionAllStaticFinal()) {
			LinkedProposalModelCore linkedProposalModel= createProposalModel();
			extractConstRefactoring.setLinkedProposalModel(linkedProposalModel);
			extractConstRefactoring.setCheckResultForCompileProblems(false);

			String label= CorrectionMessages.QuickAssistProcessor_extract_to_constant_description;
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL);
			int relevance;
			if (context.getSelectionLength() == 0) {
				relevance= IProposalRelevance.EXTRACT_CONSTANT_ZERO_SELECTION;
			} else if (problemsAtLocation) {
				relevance= IProposalRelevance.EXTRACT_CONSTANT_ERROR;
			} else {
				relevance= IProposalRelevance.EXTRACT_CONSTANT;
			}

			ExtractConstantRefactoringProposalCore core= new ExtractConstantRefactoringProposalCore(label, cu, extractConstRefactoring, relevance);
			RefactoringCorrectionProposal proposal= new RefactoringCorrectionProposalExtension(label, cu, relevance, image, core);
			proposal.setCommandId(EXTRACT_CONSTANT_ID);
			proposal.setLinkedProposalModel(linkedProposalModel);
			proposals.add(proposal);
		}
		return false;
	}

	private static class ExtractTempRefactoringProposalCore extends RefactoringCorrectionProposalCore {
		private final String fPreview;

		public ExtractTempRefactoringProposalCore(String name, ICompilationUnit cu, Refactoring refactoring, int relevance, String preview) {
			super(name, cu, refactoring, relevance);
			fPreview= preview;
		}

		@Override
		protected void init(Refactoring refactoring) throws CoreException {
			ExtractTempRefactoring etr= (ExtractTempRefactoring) refactoring;
			etr.setTempName(etr.guessTempName()); // expensive
		}

		@Override
		public Object getAdditionalProposalInfo(IProgressMonitor monitor) {
			return fPreview;
		}
	}


	private static class ExtractConstantRefactoringProposalCore extends RefactoringCorrectionProposalCore {
		public ExtractConstantRefactoringProposalCore(String name, ICompilationUnit cu, Refactoring refactoring, int relevance) {
			super(name, cu, refactoring, relevance);
		}

		@Override
		protected void init(Refactoring refactoring) throws CoreException {
			ExtractConstantRefactoring etr= (ExtractConstantRefactoring) refactoring;
			etr.setConstantName(etr.guessConstantName()); // expensive
		}
	}

	private static class RefactoringCorrectionProposalExtension extends RefactoringCorrectionProposal {
		public RefactoringCorrectionProposalExtension(String name, ICompilationUnit cu, int relevance, Image image, RefactoringCorrectionProposalCore delegate) {
			super(name, cu, relevance, image, delegate);
		}
	}

	private static boolean getDeprecatedProposal(IInvocationContext context, ASTNode node, IProblemLocation[] locations, Collection<ICommandAccess> proposals) {
		// don't add if already added as quick fix
		if (containsMatchingProblem(locations, IProblem.UsingDeprecatedMethod))
			return false;

		if (!(node instanceof MethodInvocation)) {
			node= node.getParent();
			if (!(node instanceof MethodInvocation)) {
				return false;
			}
		}
		MethodInvocation methodInvocation= (MethodInvocation) node;
		if (!QuickAssistProcessorUtil.isDeprecatedMethodCallWithReplacement(methodInvocation)) {
			return false;
		}
		if (proposals != null) {
			IProposableFix fix= InlineMethodFixCore.create(FixMessages.InlineDeprecatedMethod_msg, (CompilationUnit) methodInvocation.getRoot(), methodInvocation);
			if (fix != null) {
				Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
				proposals.add(new FixCorrectionProposal(fix, null, IProposalRelevance.INLINE_DEPRECATED_METHOD, image, context));
			}
		}
		return true;
	}

	private static boolean getConvertAnonymousToNestedProposal(IInvocationContext context, final ASTNode node, Collection<ICommandAccess> proposals) throws CoreException {
		if (!(node instanceof Name))
			return false;

		ASTNode normalized= ASTNodes.getNormalizedNode(node);
		if (normalized.getLocationInParent() != ClassInstanceCreation.TYPE_PROPERTY)
			return false;

		final AnonymousClassDeclaration anonymTypeDecl= ((ClassInstanceCreation) normalized.getParent()).getAnonymousClassDeclaration();
		if (anonymTypeDecl == null || anonymTypeDecl.resolveBinding() == null) {
			return false;
		}

		if (proposals == null) {
			return true;
		}

		final ICompilationUnit cu= context.getCompilationUnit();
		final ConvertAnonymousToNestedRefactoring refactoring= new ConvertAnonymousToNestedRefactoring(anonymTypeDecl);

		String extTypeName= ASTNodes.getSimpleNameIdentifier((Name) node);
		ITypeBinding anonymTypeBinding= anonymTypeDecl.resolveBinding();
		String className;
		if (anonymTypeBinding.getInterfaces().length == 0) {
			className= Messages.format(CorrectionMessages.QuickAssistProcessor_name_extension_from_interface, extTypeName);
		} else {
			className= Messages.format(CorrectionMessages.QuickAssistProcessor_name_extension_from_class, extTypeName);
		}
		String[][] existingTypes= ((IType) anonymTypeBinding.getJavaElement()).resolveType(className);
		int i= 1;
		while (existingTypes != null) {
			i++;
			existingTypes= ((IType) anonymTypeBinding.getJavaElement()).resolveType(className + i);
		}
		refactoring.setClassName(i == 1 ? className : className + i);

		if (refactoring.checkInitialConditions(new NullProgressMonitor()).isOK()) {
			LinkedProposalModelCore linkedProposalModel= createProposalModel();
			refactoring.setLinkedProposalModel(linkedProposalModel);

			String label= CorrectionMessages.QuickAssistProcessor_convert_anonym_to_nested;
			Image image= JavaPlugin.getImageDescriptorRegistry().get(JavaElementImageProvider.getTypeImageDescriptor(true, false, Flags.AccPrivate, false));
			RefactoringCorrectionProposal proposal= new RefactoringCorrectionProposal(label, cu, refactoring, IProposalRelevance.CONVERT_ANONYMOUS_TO_NESTED, image);
			proposal.setLinkedProposalModel(linkedProposalModel);
			proposal.setCommandId(CONVERT_ANONYMOUS_TO_LOCAL_ID);
			proposals.add(proposal);
		}
		return false;
	}

	private static boolean getConvertAnonymousClassCreationsToLambdaProposals(IInvocationContext context, ASTNode covering, Collection<ICommandAccess> resultingCollections) {
		while (covering instanceof Name
				|| covering instanceof Type
				|| covering instanceof Dimension
				|| covering.getParent() instanceof MethodDeclaration
				|| covering.getLocationInParent() == AnonymousClassDeclaration.BODY_DECLARATIONS_PROPERTY) {
			covering= covering.getParent();
		}

		ClassInstanceCreation cic;
		if (covering instanceof ClassInstanceCreation) {
			cic= (ClassInstanceCreation) covering;
		} else if (covering.getLocationInParent() == ClassInstanceCreation.ANONYMOUS_CLASS_DECLARATION_PROPERTY) {
			cic= (ClassInstanceCreation) covering.getParent();
		} else if (covering instanceof Name) {
			ASTNode normalized= ASTNodes.getNormalizedNode(covering);
			if (normalized.getLocationInParent() != ClassInstanceCreation.TYPE_PROPERTY)
				return false;
			cic= (ClassInstanceCreation) normalized.getParent();
		} else {
			return false;
		}

		IProposableFix fix= LambdaExpressionsFixCore.createConvertToLambdaFix(cic);
		if (fix == null)
			return false;

		if (resultingCollections == null)
			return true;

		// add correction proposal
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		Map<String, String> options= new Hashtable<>();
		options.put(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES, CleanUpOptions.TRUE);
		options.put(CleanUpConstants.USE_LAMBDA, CleanUpOptions.TRUE);
		FixCorrectionProposal proposal= new FixCorrectionProposal(fix, new LambdaExpressionsCleanUp(options), IProposalRelevance.CONVERT_TO_LAMBDA_EXPRESSION, image, context);
		resultingCollections.add(proposal);
		return true;
	}

	public static boolean getConvertLambdaToAnonymousClassCreationsProposals(IInvocationContext context, ASTNode covering, Collection<ICommandAccess> resultingCollections) {
		LambdaExpression lambda;
		if (covering instanceof LambdaExpression) {
			lambda= (LambdaExpression) covering;
		} else if (covering.getLocationInParent() == LambdaExpression.BODY_PROPERTY) {
			lambda= (LambdaExpression) covering.getParent();
		} else {
			return false;
		}

		IProposableFix fix= LambdaExpressionsFixCore.createConvertToAnonymousClassCreationsFix(lambda);
		if (fix == null)
			return false;

		if (resultingCollections == null)
			return true;

		// add correction proposal
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		Map<String, String> options= new Hashtable<>();
		options.put(CleanUpConstants.CONVERT_FUNCTIONAL_INTERFACES, CleanUpOptions.TRUE);
		options.put(CleanUpConstants.USE_ANONYMOUS_CLASS_CREATION, CleanUpOptions.TRUE);
		FixCorrectionProposal proposal= new FixCorrectionProposal(fix, new LambdaExpressionsCleanUp(options), IProposalRelevance.CONVERT_TO_SWITCH_EXPRESSION, image, context);
		resultingCollections.add(proposal);
		return true;
	}

	private static boolean getConvertMethodReferenceToLambdaProposal(IInvocationContext context, ASTNode covering, Collection<ICommandAccess> resultingCollections) throws JavaModelException {
		MethodReference methodReference;
		if (covering instanceof MethodReference) {
			methodReference= (MethodReference) covering;
		} else if (covering.getParent() instanceof MethodReference) {
			methodReference= (MethodReference) covering.getParent();
		} else {
			return false;
		}

		IMethodBinding functionalMethod= QuickAssistProcessorUtil.getFunctionalMethodForMethodReference(methodReference);
		if (functionalMethod == null || functionalMethod.isGenericMethod()) { // generic lambda expressions are not allowed
			return false;
		}

		if (resultingCollections == null)
			return true;

		ASTRewrite rewrite= ASTRewrite.create(methodReference.getAST());
		LinkedProposalModelCore linkedProposalModel= createProposalModel();

		LambdaExpression lambda= QuickAssistProcessorUtil.convertMethodRefernceToLambda(methodReference, functionalMethod, context.getASTRoot(), rewrite, linkedProposalModel, false);

		// add proposal
		String label= CorrectionMessages.QuickAssistProcessor_convert_to_lambda_expression;
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		LinkedCorrectionProposal proposal= new LinkedCorrectionProposal(label, context.getCompilationUnit(), rewrite, IProposalRelevance.CONVERT_METHOD_REFERENCE_TO_LAMBDA, image);
		proposal.setLinkedProposalModel(linkedProposalModel);
		proposal.setEndPosition(rewrite.track(lambda));
		resultingCollections.add(proposal);
		return true;
	}

	private static boolean getConvertToSwitchExpressionProposals(IInvocationContext context, ASTNode covering, Collection<ICommandAccess> resultingCollections) {
		if (covering instanceof Block) {
			List<Statement> statements= ((Block) covering).statements();
			int startIndex= QuickAssistProcessorUtil.getIndex(context.getSelectionOffset(), statements);
			if (startIndex == -1 || startIndex >= statements.size()) {
				return false;
			}
			covering= statements.get(startIndex);
		} else {
			while (covering instanceof SwitchCase
					|| covering instanceof SwitchExpression) {
				covering= covering.getParent();
			}
		}

		SwitchStatement switchStatement;
		if (covering instanceof SwitchStatement) {
			switchStatement= (SwitchStatement) covering;
		} else {
			return false;
		}

		IProposableFix fix= SwitchExpressionsFixCore.createConvertToSwitchExpressionFix(switchStatement);
		if (fix == null)
			return false;

		if (resultingCollections == null)
			return true;

		// add correction proposal
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		Map<String, String> options= new Hashtable<>();
		options.put(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_TO_SWITCH_EXPRESSIONS, CleanUpOptions.TRUE);
		FixCorrectionProposal proposal= new FixCorrectionProposal(fix, new SwitchExpressionsCleanUp(options), IProposalRelevance.CONVERT_TO_SWITCH_EXPRESSION, image, context);
		resultingCollections.add(proposal);
		return true;
	}

	private static boolean getChangeLambdaBodyToBlockProposal(IInvocationContext context, ASTNode covering, Collection<ICommandAccess> resultingCollections) {
		if (resultingCollections == null) {
			return true;
		}
		ChangeLambdaBodyToBlockFixCore fix= ChangeLambdaBodyToBlockFixCore.createChangeLambdaBodyToBlockFix(context.getASTRoot(), covering);
		if (fix != null) {
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			FixCorrectionProposal proposal= new FixCorrectionProposal(fix, null, IProposalRelevance.CHANGE_LAMBDA_BODY_TO_BLOCK, image, context);
			resultingCollections.add(proposal);
			return true;
		}
		return false;
	}

	private static boolean getChangeLambdaBodyToExpressionProposal(IInvocationContext context, ASTNode covering, Collection<ICommandAccess> resultingCollections) {
		if (resultingCollections == null) {
			return true;
		}
		ChangeLambdaBodyToExpressionFixCore fix= ChangeLambdaBodyToExpressionFixCore.createChangeLambdaBodyToBlockFix(context.getASTRoot(), covering);
		if (fix != null) {
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			FixCorrectionProposal proposal= new FixCorrectionProposal(fix, null, IProposalRelevance.CHANGE_LAMBDA_BODY_TO_EXPRESSION, image, context);
			resultingCollections.add(proposal);
			return true;
		}
		return false;
	}

	private static boolean getConvertLambdaToMethodReferenceProposal(IInvocationContext context, ASTNode coveringNode, Collection<ICommandAccess> resultingCollections) {
		if (resultingCollections == null) {
			return true;
		}
		ConvertLambdaToMethodReferenceFixCore fix= ConvertLambdaToMethodReferenceFixCore.createConvertLambdaToMethodReferenceFix(context.getASTRoot(), coveringNode);
		if (fix != null) {
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			FixCorrectionProposal proposal= new FixCorrectionProposal(fix, null, IProposalRelevance.CONVERT_TO_METHOD_REFERENCE, image, context);
			resultingCollections.add(proposal);
			return true;
		}
		return false;
	}

	private static boolean getFixParenthesesInLambdaExpression(IInvocationContext context, ASTNode coveringNode, Collection<ICommandAccess> resultingCollections) {
		LambdaExpression enclosingLambda= null;
		if (coveringNode instanceof LambdaExpression) {
			enclosingLambda= (LambdaExpression) coveringNode;
		} else if (coveringNode.getLocationInParent() == VariableDeclarationFragment.NAME_PROPERTY
				&& ((VariableDeclarationFragment) coveringNode.getParent()).getLocationInParent() == LambdaExpression.PARAMETERS_PROPERTY) {
			enclosingLambda= (LambdaExpression) coveringNode.getParent().getParent();
		} else {
			return false;
		}

		List<VariableDeclaration> lambdaParameters= enclosingLambda.parameters();
		if (lambdaParameters.size() != 1)
			return false;

		if (lambdaParameters.get(0) instanceof SingleVariableDeclaration)
			return false;

		if (resultingCollections == null) {
			return true;
		}

		String label;
		Boolean parenthesesPropertyNewValue;
		String imageKey;

		if (enclosingLambda.hasParentheses()) {
			label= CorrectionMessages.QuickAssistProcessor_removeParenthesesInLambda;
			parenthesesPropertyNewValue= Boolean.FALSE;
			imageKey= JavaPluginImages.IMG_CORRECTION_REMOVE;
		} else {
			label= CorrectionMessages.QuickAssistProcessor_addParenthesesInLambda;
			parenthesesPropertyNewValue= Boolean.TRUE;
			imageKey= JavaPluginImages.IMG_CORRECTION_CAST;
		}

		// Create the rewrite:
		ASTRewrite rewrite= ASTRewrite.create(enclosingLambda.getAST());
		rewrite.set(enclosingLambda, LambdaExpression.PARENTHESES_PROPERTY, parenthesesPropertyNewValue, null);

		// add correction proposal
		Image image= JavaPluginImages.get(imageKey);
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, IProposalRelevance.ADD_PARENTHESES_FOR_EXPRESSION, image);
		resultingCollections.add(proposal);
		return true;
	}

	public static boolean getAddInferredLambdaParameterTypes(IInvocationContext context, ASTNode covering, Collection<ICommandAccess> resultingCollections) {
		if (resultingCollections == null) {
			return true;
		}
		AddInferredLambdaParameterTypesFixCore fix= AddInferredLambdaParameterTypesFixCore.createAddInferredLambdaParameterTypesFix(context.getASTRoot(), covering);
		if (fix != null) {
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			FixCorrectionProposal proposal= new FixCorrectionProposal(fix, null, IProposalRelevance.ADD_INFERRED_LAMBDA_PARAMETER_TYPES, image, context);
			resultingCollections.add(proposal);
			return true;
		}
		return false;
	}

	public static boolean getAddVarLambdaParameterTypes(IInvocationContext context, ASTNode covering, Collection<ICommandAccess> resultingCollections) {
		if (resultingCollections == null) {
			return true;
		}
		AddVarLambdaParameterTypesFixCore fix= AddVarLambdaParameterTypesFixCore.createAddVarLambdaParameterTypesFix(context.getASTRoot(), covering);
		if (fix != null) {
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			FixCorrectionProposal proposal= new FixCorrectionProposal(fix, null, IProposalRelevance.ADD_INFERRED_LAMBDA_PARAMETER_TYPES, image, context);
			resultingCollections.add(proposal);
			return true;
		}
		return false;
	}

	public static boolean getRemoveVarOrInferredLambdaParameterTypes(IInvocationContext context, ASTNode covering, Collection<ICommandAccess> resultingCollections) {
		if (resultingCollections == null) {
			return true;
		}
		RemoveVarOrInferredLambdaParameterTypesFixCore fix= RemoveVarOrInferredLambdaParameterTypesFixCore.createRemoveVarOrInferredLambdaParameterTypesFix(context.getASTRoot(), covering);
		if (fix != null) {
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			FixCorrectionProposal proposal= new FixCorrectionProposal(fix, null, IProposalRelevance.ADD_INFERRED_LAMBDA_PARAMETER_TYPES, image, context);
			resultingCollections.add(proposal);
			return true;
		}
		return false;
	}

	private static boolean getAddMethodDeclaration(IInvocationContext context, ASTNode covering, Collection<ICommandAccess> resultingCollections) {
		AddMissingMethodDeclarationFixCore fix= AddMissingMethodDeclarationFixCore.createAddMissingMethodDeclaration(context.getASTRoot(), covering);
		if (fix != null) {
			if (resultingCollections == null) {
				return true;
			}

			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			FixCorrectionProposal proposal= new FixCorrectionProposal(fix, null, IProposalRelevance.ADD_INFERRED_LAMBDA_PARAMETER_TYPES, image, context);
			resultingCollections.add(proposal);
			return true;
		}
		return false;
	}

	public static boolean getInferDiamondArgumentsProposal(IInvocationContext context, ASTNode node, IProblemLocation[] locations, Collection<ICommandAccess> resultingCollections) {
		// don't add if already added as quick fix
		if (containsMatchingProblem(locations, IProblem.DiamondNotBelow17))
			return false;
		ParameterizedType createdType= null;

		if (node instanceof Name) {
			Name name= ASTNodes.getTopMostName((Name) node);
			if (name.getLocationInParent() == SimpleType.NAME_PROPERTY ||
					name.getLocationInParent() == NameQualifiedType.NAME_PROPERTY) {
				ASTNode type= name.getParent();
				if (type.getLocationInParent() == ParameterizedType.TYPE_PROPERTY) {
					createdType= (ParameterizedType) type.getParent();
					if (createdType.getLocationInParent() != ClassInstanceCreation.TYPE_PROPERTY) {
						return false;
					}
				}
			}
		} else if (node instanceof ParameterizedType) {
			createdType= (ParameterizedType) node;
			if (createdType.getLocationInParent() != ClassInstanceCreation.TYPE_PROPERTY) {
				return false;
			}
		} else if (node instanceof ClassInstanceCreation) {
			ClassInstanceCreation creation= (ClassInstanceCreation) node;
			Type type= creation.getType();
			if (type instanceof ParameterizedType) {
				createdType= (ParameterizedType) type;
			}
		}

		IProposableFix fix= TypeParametersFixCore.createInsertInferredTypeArgumentsFix(context.getASTRoot(), createdType);
		if (fix != null && resultingCollections != null) {
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			int relevance= locations == null ? IProposalRelevance.INSERT_INFERRED_TYPE_ARGUMENTS : IProposalRelevance.INSERT_INFERRED_TYPE_ARGUMENTS_ERROR; // if error -> higher than ReorgCorrectionsSubProcessor.getNeedHigherComplianceProposals()
			Map<String, String> options= new HashMap<>();
			options.put(CleanUpConstants.INSERT_INFERRED_TYPE_ARGUMENTS, CleanUpOptions.TRUE);
			FixCorrectionProposal proposal= new FixCorrectionProposal(fix, new TypeParametersCleanUp(options), relevance, image, context);
			resultingCollections.add(proposal);
		} else {
			return false;
		}
		return true;
	}

	private static boolean getJoinVariableProposals(IInvocationContext context, ASTNode node, Collection<ICommandAccess> resultingCollections) {
		if (resultingCollections == null) {
			return true;
		}
		JoinVariableFixCore fix= JoinVariableFixCore.createJoinVariableFix(context.getASTRoot(), node);
		if (fix != null) {
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL);
			FixCorrectionProposal proposal= new FixCorrectionProposal(fix, null, IProposalRelevance.JOIN_VARIABLE_DECLARATION, image, context);
			proposal.setCommandId(SPLIT_JOIN_VARIABLE_DECLARATION_ID);
			resultingCollections.add(proposal);
			return true;
		}
		return false;
	}

	@SuppressWarnings("unused")
	private static boolean getSplitVariableProposals(IInvocationContext context, ASTNode node, Collection<ICommandAccess> resultingCollections) throws JavaModelException {
		if (resultingCollections == null) {
			return true;
		}
		boolean commandConflict= false;
		for (ICommandAccess completionProposal : resultingCollections) {
			if (completionProposal instanceof ChangeCorrectionProposal ccp) {
				if (SPLIT_JOIN_VARIABLE_DECLARATION_ID.equals(ccp.getCommandId())) {
					commandConflict= true;
				}
			}
		}
		SplitVariableFixCore fix= SplitVariableFixCore.createSplitVariableFix(context.getASTRoot(), node);
		if (fix != null) {
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL);
			FixCorrectionProposal proposal= new FixCorrectionProposal(fix, null, IProposalRelevance.SPLIT_VARIABLE_DECLARATION, image, context);
			if (!commandConflict) {
				proposal.setCommandId(SPLIT_JOIN_VARIABLE_DECLARATION_ID);
			}
			resultingCollections.add(proposal);
			return true;
		}
		return false;
	}

	public static boolean getAssignToVariableProposals(IInvocationContext context, ASTNode node, IProblemLocation[] locations, Collection<ICommandAccess> resultingCollections) {
		// don't add if already added as quick fix
		if (containsMatchingProblem(locations, IProblem.ParsingErrorInsertToComplete))
			return false;
		Statement statement= ASTResolving.findParentStatement(node);
		if (!(statement instanceof ExpressionStatement)) {
			return false;
		}
		ExpressionStatement expressionStatement= (ExpressionStatement) statement;

		Expression expression= expressionStatement.getExpression();
		if (expression.getNodeType() == ASTNode.ASSIGNMENT) {
			return false; // too confusing and not helpful
		}

		ITypeBinding typeBinding= expression.resolveTypeBinding();
		typeBinding= Bindings.normalizeTypeBinding(typeBinding);
		if (typeBinding == null) {
			return false;
		}
		if (resultingCollections == null) {
			return true;
		}

		// don't add if already added as quick fix
		if (containsMatchingProblem(locations, IProblem.UnusedObjectAllocation))
			return false;

		ICompilationUnit cu= context.getCompilationUnit();

		AssignToVariableAssistProposal localProposal= new AssignToVariableAssistProposal(cu, AssignToVariableAssistProposal.LOCAL, expressionStatement, typeBinding,
				IProposalRelevance.ASSIGN_TO_LOCAL);
		localProposal.setCommandId(ASSIGN_TO_LOCAL_ID);
		resultingCollections.add(localProposal);

		if (QuickAssistProcessorUtil.isAutoClosable(typeBinding)) {
			AssignToVariableAssistProposal tryWithResourcesProposal= new AssignToVariableAssistProposal(cu, AssignToVariableAssistProposal.TRY_WITH_RESOURCES, expressionStatement, typeBinding,
					IProposalRelevance.ASSIGN_IN_TRY_WITH_RESOURCES);
			tryWithResourcesProposal.setCommandId(ASSIGN_IN_TRY_WITH_RESOURCES_ID);
			resultingCollections.add(tryWithResourcesProposal);
		}

		ASTNode type= ASTResolving.findParentType(expression);
		if (type != null) {
			AssignToVariableAssistProposal fieldProposal= new AssignToVariableAssistProposal(cu, AssignToVariableAssistProposal.FIELD, expressionStatement, typeBinding,
					IProposalRelevance.ASSIGN_TO_FIELD);
			fieldProposal.setCommandId(ASSIGN_TO_FIELD_ID);
			resultingCollections.add(fieldProposal);
		}
		return true;

	}

	private static boolean containsMatchingProblem(IProblemLocation[] locations, int problemId) {
		if (locations != null) {
			for (IProblemLocation location : locations) {
				if (IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER.equals(location.getMarkerType())
						&& location.getProblemId() == problemId) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean getAssignParamToFieldProposals(IInvocationContext context, ASTNode node, Collection<ICommandAccess> resultingCollections) {
		node= ASTNodes.getNormalizedNode(node);
		ASTNode parent= node.getParent();
		if (!(parent instanceof SingleVariableDeclaration) || !(parent.getParent() instanceof MethodDeclaration)) {
			return false;
		}
		SingleVariableDeclaration paramDecl= (SingleVariableDeclaration) parent;
		IVariableBinding binding= paramDecl.resolveBinding();

		MethodDeclaration methodDecl= (MethodDeclaration) parent.getParent();
		if (binding == null || methodDecl.getBody() == null) {
			return false;
		}
		ITypeBinding typeBinding= binding.getType();
		if (typeBinding == null) {
			return false;
		}

		if (resultingCollections == null) {
			return true;
		}

		ITypeBinding parentType= Bindings.getBindingOfParentType(node);
		if (parentType != null) {
			if (parentType.isInterface()) {
				return false;
			}
			// assign to existing fields
			CompilationUnit root= context.getASTRoot();
			boolean isStaticContext= ASTResolving.isInStaticContext(node);
			boolean hasFieldWithSameName= false;
			for (IVariableBinding curr : parentType.getDeclaredFields()) {
				if (isStaticContext == Modifier.isStatic(curr.getModifiers()) && typeBinding.isAssignmentCompatible(curr.getType())) {
					ASTNode fieldDeclFrag= root.findDeclaringNode(curr);
					if (fieldDeclFrag instanceof VariableDeclarationFragment) {
						VariableDeclarationFragment fragment= (VariableDeclarationFragment) fieldDeclFrag;
						if (fragment.getInitializer() == null) {
							if (curr.getName().equals(paramDecl.getName().getFullyQualifiedName())) {
								hasFieldWithSameName= true;
							}
						}
					}
				}
			}
			for (IVariableBinding curr : parentType.getDeclaredFields()) {
				if (isStaticContext == Modifier.isStatic(curr.getModifiers()) && typeBinding.isAssignmentCompatible(curr.getType())) {
					ASTNode fieldDeclFrag= root.findDeclaringNode(curr);
					if (fieldDeclFrag instanceof VariableDeclarationFragment) {
						VariableDeclarationFragment fragment= (VariableDeclarationFragment) fieldDeclFrag;
						if (fragment.getInitializer() == null) {
							if (curr.getName().equals(paramDecl.getName().getFullyQualifiedName())) {
								resultingCollections
								.add(new AssignToVariableAssistProposal(context.getCompilationUnit(), paramDecl, fragment, typeBinding, IProposalRelevance.ASSIGN_PARAM_TO_MATCHING_FIELD));
							} else {
								if (!hasFieldWithSameName) {
									resultingCollections
									.add(new AssignToVariableAssistProposal(context.getCompilationUnit(), paramDecl, fragment, typeBinding, IProposalRelevance.ASSIGN_PARAM_TO_EXISTING_FIELD));
								}
							}
						}
					}
				}
			}
		}

		AssignToVariableAssistProposal fieldProposal= new AssignToVariableAssistProposal(context.getCompilationUnit(), paramDecl, null, typeBinding, IProposalRelevance.ASSIGN_PARAM_TO_NEW_FIELD);
		fieldProposal.setCommandId(ASSIGN_PARAM_TO_FIELD_ID);
		resultingCollections.add(fieldProposal);
		return true;
	}

	private static boolean getAssignAllParamsToFieldsProposals(IInvocationContext context, ASTNode node, Collection<ICommandAccess> resultingCollections) {
		node= ASTNodes.getNormalizedNode(node);
		ASTNode parent= node.getParent();
		if (!(parent instanceof SingleVariableDeclaration) || !(parent.getParent() instanceof MethodDeclaration)) {
			return false;
		}
		MethodDeclaration methodDecl= (MethodDeclaration) parent.getParent();
		if (methodDecl.getBody() == null) {
			return false;
		}
		List<SingleVariableDeclaration> parameters= methodDecl.parameters();
		if (parameters.size() <= 1) {
			return false;
		}
		ITypeBinding parentType= Bindings.getBindingOfParentType(node);
		if (parentType == null || parentType.isInterface()) {
			return false;
		}
		for (SingleVariableDeclaration param : parameters) {
			IVariableBinding binding= param.resolveBinding();
			if (binding == null || binding.getType() == null) {
				return false;
			}
		}
		if (resultingCollections == null) {
			return true;
		}

		AssignToVariableAssistProposal fieldProposal= new AssignToVariableAssistProposal(context.getCompilationUnit(), parameters, IProposalRelevance.ASSIGN_ALL_PARAMS_TO_NEW_FIELDS);
		fieldProposal.setCommandId(ASSIGN_ALL_PARAMS_TO_NEW_FIELDS_ID);
		resultingCollections.add(fieldProposal);
		return true;
	}

	private static boolean getAddFinallyProposals(IInvocationContext context, ASTNode node, Collection<ICommandAccess> resultingCollections) {
		TryStatement tryStatement= ASTResolving.findParentTryStatement(node);
		if (tryStatement == null || tryStatement.getFinally() != null) {
			return false;
		}
		Statement statement= ASTResolving.findParentStatement(node);
		if (tryStatement != statement && tryStatement.getBody() != statement) {
			return false; // an node inside a catch or finally block
		}

		if (resultingCollections == null) {
			return true;
		}

		AST ast= tryStatement.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);
		Block finallyBody= ast.newBlock();

		rewrite.set(tryStatement, TryStatement.FINALLY_PROPERTY, finallyBody, null);

		String label= CorrectionMessages.QuickAssistProcessor_addfinallyblock_description;
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_ADD);
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, IProposalRelevance.ADD_FINALLY_BLOCK, image);
		resultingCollections.add(proposal);
		return true;
	}

	private static boolean getAddElseProposals(IInvocationContext context, ASTNode node, Collection<ICommandAccess> resultingCollections) {
		if (!(node instanceof IfStatement)) {
			return false;
		}
		IfStatement ifStatement= (IfStatement) node;
		if (ifStatement.getElseStatement() != null) {
			return false;
		}

		if (resultingCollections == null) {
			return true;
		}

		AST ast= node.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);
		Block body= ast.newBlock();

		rewrite.set(ifStatement, IfStatement.ELSE_STATEMENT_PROPERTY, body, null);

		String label= CorrectionMessages.QuickAssistProcessor_addelseblock_description;
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_ADD);
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, IProposalRelevance.ADD_ELSE_BLOCK, image);
		resultingCollections.add(proposal);
		return true;
	}

	public static boolean getCatchClauseToThrowsProposals(IInvocationContext context, ASTNode node, Collection<ICommandAccess> resultingCollections) {
		CatchClause catchClause= (CatchClause) ASTResolving.findAncestor(node, ASTNode.CATCH_CLAUSE);
		if (catchClause == null) {
			return false;
		}

		Statement statement= ASTResolving.findParentStatement(node);
		if (statement != catchClause.getParent() && statement != catchClause.getBody()) {
			return false; // selection is in a statement inside the body
		}

		Type type= catchClause.getException().getType();
		if (!type.isSimpleType() && !type.isUnionType() && !type.isNameQualifiedType()) {
			return false;
		}

		BodyDeclaration bodyDeclaration= ASTResolving.findParentBodyDeclaration(catchClause);
		if (!(bodyDeclaration instanceof MethodDeclaration) && !(bodyDeclaration instanceof Initializer)) {
			return false;
		}

		if (resultingCollections == null) {
			return true;
		}

		AST ast= bodyDeclaration.getAST();
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);

		Type selectedMultiCatchType= null;
		if (type.isUnionType() && node instanceof Name) {
			Name topMostName= ASTNodes.getTopMostName((Name) node);
			ASTNode parent= topMostName.getParent();
			if (parent instanceof SimpleType) {
				selectedMultiCatchType= (SimpleType) parent;
			} else if (parent instanceof NameQualifiedType) {
				selectedMultiCatchType= (NameQualifiedType) parent;
			}
		}

		if (bodyDeclaration instanceof MethodDeclaration) {
			MethodDeclaration methodDeclaration= (MethodDeclaration) bodyDeclaration;

			ASTRewrite rewrite= ASTRewrite.create(ast);
			if (selectedMultiCatchType != null) {
				removeException(rewrite, (UnionType) type, selectedMultiCatchType);
				addExceptionToThrows(ast, methodDeclaration, rewrite, selectedMultiCatchType);
				String label= CorrectionMessages.QuickAssistProcessor_exceptiontothrows_description;
				ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, IProposalRelevance.REPLACE_EXCEPTION_WITH_THROWS, image);
				resultingCollections.add(proposal);
			} else {
				removeCatchBlock(rewrite, catchClause);
				if (type.isUnionType()) {
					UnionType unionType= (UnionType) type;
					List<Type> types= unionType.types();
					for (Type elementType : types) {
						if (!(elementType instanceof SimpleType)
								&& !(elementType instanceof NameQualifiedType))
							return false;
						addExceptionToThrows(ast, methodDeclaration, rewrite, elementType);
					}
				} else {
					addExceptionToThrows(ast, methodDeclaration, rewrite, type);
				}
				String label= CorrectionMessages.QuickAssistProcessor_catchclausetothrows_description;
				ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, IProposalRelevance.REPLACE_CATCH_CLAUSE_WITH_THROWS, image);
				resultingCollections.add(proposal);
			}
		}
		{ // for initializers or method declarations
			ASTRewrite rewrite= ASTRewrite.create(ast);
			if (selectedMultiCatchType != null) {
				removeException(rewrite, (UnionType) type, selectedMultiCatchType);
				String label= CorrectionMessages.QuickAssistProcessor_removeexception_description;
				ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, IProposalRelevance.REMOVE_EXCEPTION, image);
				resultingCollections.add(proposal);
			} else {
				removeCatchBlock(rewrite, catchClause);
				String label= CorrectionMessages.QuickAssistProcessor_removecatchclause_description;
				ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, IProposalRelevance.REMOVE_CATCH_CLAUSE, image);
				resultingCollections.add(proposal);
			}
		}

		return true;
	}

	private static void removeException(ASTRewrite rewrite, UnionType unionType, Type exception) {
		ListRewrite listRewrite= rewrite.getListRewrite(unionType, UnionType.TYPES_PROPERTY);
		List<Type> types= unionType.types();
		for (Type type : types) {
			if (type.equals(exception)) {
				listRewrite.remove(type, null);
			}
		}
	}

	private static void addExceptionToThrows(AST ast, MethodDeclaration methodDeclaration, ASTRewrite rewrite, Type type2) {
		ITypeBinding binding= type2.resolveBinding();
		if (binding == null || isNotYetThrown(binding, methodDeclaration.thrownExceptionTypes())) {
			Type newType= (Type) ASTNode.copySubtree(ast, type2);

			ListRewrite listRewriter= rewrite.getListRewrite(methodDeclaration, MethodDeclaration.THROWN_EXCEPTION_TYPES_PROPERTY);
			listRewriter.insertLast(newType, null);
		}
	}

	private static void removeCatchBlock(ASTRewrite rewrite, CatchClause catchClause) {
		TryStatement tryStatement= (TryStatement) catchClause.getParent();
		if (tryStatement.catchClauses().size() > 1 || tryStatement.getFinally() != null || !tryStatement.resources().isEmpty()) {
			rewrite.remove(catchClause, null);
		} else {
			Block block= tryStatement.getBody();
			List<Statement> statements= block.statements();
			int nStatements= statements.size();
			if (nStatements == 1) {
				ASTNode first= statements.get(0);
				rewrite.replace(tryStatement, rewrite.createCopyTarget(first), null);
			} else if (nStatements > 1) {
				ListRewrite listRewrite= rewrite.getListRewrite(block, Block.STATEMENTS_PROPERTY);
				ASTNode first= statements.get(0);
				ASTNode last= statements.get(statements.size() - 1);
				ASTNode newStatement= listRewrite.createCopyTarget(first, last);
				if (ASTNodes.isControlStatementBody(tryStatement.getLocationInParent())) {
					Block newBlock= rewrite.getAST().newBlock();
					newBlock.statements().add(newStatement);
					newStatement= newBlock;
				}
				rewrite.replace(tryStatement, newStatement, null);
			} else {
				rewrite.remove(tryStatement, null);
			}
		}
	}

	private static boolean isNotYetThrown(ITypeBinding binding, List<Type> thrownExceptions) {
		for (Type name : thrownExceptions) {
			ITypeBinding elem= name.resolveBinding();
			if (elem != null) {
				if (Bindings.isSuperType(elem, binding)) { // existing exception is base class of new
					return false;
				}
			}
		}
		return true;
	}

	private static boolean getPickoutTypeFromMulticatchProposals(IInvocationContext context, ASTNode node, ArrayList<ASTNode> coveredNodes, Collection<ICommandAccess> resultingCollections) {
		CatchClause catchClause= (CatchClause) ASTResolving.findAncestor(node, ASTNode.CATCH_CLAUSE);
		if (catchClause == null) {
			return false;
		}

		Statement statement= ASTResolving.findParentStatement(node);
		if (statement != catchClause.getParent() && statement != catchClause.getBody()) {
			return false; // selection is in a statement inside the body
		}

		Type type= catchClause.getException().getType();
		if (!type.isUnionType()) {
			return false;
		}

		Type selectedMultiCatchType= null;
		if (type.isUnionType() && node instanceof Name) {
			Name topMostName= ASTNodes.getTopMostName((Name) node);
			ASTNode parent= topMostName.getParent();
			if (parent instanceof SimpleType || parent instanceof NameQualifiedType) {
				selectedMultiCatchType= (Type) parent;
			}
		}

		boolean multipleExceptions= coveredNodes.size() > 1;
		if ((selectedMultiCatchType == null) && (!(node instanceof UnionType) || !multipleExceptions)) {
			return false;
		}

		if (!multipleExceptions) {
			coveredNodes.add(selectedMultiCatchType);
		}

		BodyDeclaration bodyDeclaration= ASTResolving.findParentBodyDeclaration(catchClause);
		if (!(bodyDeclaration instanceof MethodDeclaration) && !(bodyDeclaration instanceof Initializer)) {
			return false;
		}

		if (resultingCollections == null) {
			return true;
		}

		AST ast= bodyDeclaration.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);

		CatchClause newCatchClause= ast.newCatchClause();
		SingleVariableDeclaration newSingleVariableDeclaration= ast.newSingleVariableDeclaration();
		UnionType newUnionType= ast.newUnionType();
		List<Type> types= newUnionType.types();
		for (ASTNode typeNode : coveredNodes) {
			types.add((Type) rewrite.createCopyTarget(typeNode));
			rewrite.remove(typeNode, null);
		}
		newSingleVariableDeclaration.setType(newUnionType);
		newSingleVariableDeclaration.setName((SimpleName) rewrite.createCopyTarget(catchClause.getException().getName()));
		newCatchClause.setException(newSingleVariableDeclaration);

		setCatchClauseBody(newCatchClause, rewrite, catchClause);

		TryStatement tryStatement= (TryStatement) catchClause.getParent();
		ListRewrite listRewrite= rewrite.getListRewrite(tryStatement, TryStatement.CATCH_CLAUSES_PROPERTY);
		listRewrite.insertAfter(newCatchClause, catchClause, null);

		Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);
		String label= !multipleExceptions
				? CorrectionMessages.QuickAssistProcessor_move_exception_to_separate_catch_block
				: CorrectionMessages.QuickAssistProcessor_move_exceptions_to_separate_catch_block;
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, IProposalRelevance.MOVE_EXCEPTION_TO_SEPERATE_CATCH_BLOCK, image);
		resultingCollections.add(proposal);
		return true;
	}

	private static boolean getConvertToMultiCatchProposals(IInvocationContext context, ASTNode covering, Collection<ICommandAccess> resultingCollections) {
		if (!JavaModelUtil.is1d7OrHigher(context.getCompilationUnit().getJavaProject()))
			return false;

		CatchClause catchClause= (CatchClause) ASTResolving.findAncestor(covering, ASTNode.CATCH_CLAUSE);
		if (catchClause == null) {
			return false;
		}

		Statement statement= ASTResolving.findParentStatement(covering);
		if (statement != catchClause.getParent() && statement != catchClause.getBody()) {
			return false; // selection is in a statement inside the body
		}

		Type type1= catchClause.getException().getType();
		Type selectedMultiCatchType= null;
		if (type1.isUnionType() && covering instanceof Name) {
			Name topMostName= ASTNodes.getTopMostName((Name) covering);
			ASTNode parent= topMostName.getParent();
			if (parent instanceof SimpleType || parent instanceof NameQualifiedType) {
				selectedMultiCatchType= (Type) parent;
			}
		}
		if (selectedMultiCatchType != null)
			return false;

		TryStatement tryStatement= (TryStatement) catchClause.getParent();
		List<CatchClause> catchClauses= tryStatement.catchClauses();
		if (catchClauses.size() <= 1)
			return false;

		String commonSource= null;
		try {
			IBuffer buffer= context.getCompilationUnit().getBuffer();
			for (CatchClause catchClause1 : catchClauses) {
				Block body= catchClause1.getBody();
				String source= buffer.getText(body.getStartPosition(), body.getLength());
				if (commonSource == null) {
					commonSource= source;
				} else {
					if (!commonSource.equals(source))
						return false;
				}
			}
		} catch (JavaModelException e) {
			return false;
		}

		if (resultingCollections == null)
			return true;

		AST ast= covering.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);
		TightSourceRangeComputer sourceRangeComputer= new TightSourceRangeComputer();
		sourceRangeComputer.addTightSourceNode(catchClauses.get(catchClauses.size() - 1));
		rewrite.setTargetSourceRangeComputer(sourceRangeComputer);

		CatchClause firstCatchClause= catchClauses.get(0);

		UnionType newUnionType= ast.newUnionType();
		List<Type> types= newUnionType.types();
		for (CatchClause catchClause1 : catchClauses) {
			Type type= catchClause1.getException().getType();
			if (type instanceof UnionType) {
				List<Type> types2= ((UnionType) type).types();
				for (Type type2 : types2) {
					types.add((Type) rewrite.createCopyTarget(type2));
				}
			} else {
				types.add((Type) rewrite.createCopyTarget(type));
			}
		}

		SingleVariableDeclaration newExceptionDeclaration= ast.newSingleVariableDeclaration();
		newExceptionDeclaration.setType(newUnionType);
		newExceptionDeclaration.setName((SimpleName) rewrite.createCopyTarget(firstCatchClause.getException().getName()));
		rewrite.replace(firstCatchClause.getException(), newExceptionDeclaration, null);

		for (int i= 1; i < catchClauses.size(); i++) {
			rewrite.remove(catchClauses.get(i), null);
		}

		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		String label= CorrectionMessages.QuickAssistProcessor_convert_to_single_multicatch_block;
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, IProposalRelevance.COMBINE_CATCH_BLOCKS, image);
		resultingCollections.add(proposal);
		return true;
	}

	private static boolean getUnrollMultiCatchProposals(IInvocationContext context, ASTNode covering, Collection<ICommandAccess> resultingCollections) {
		if (!JavaModelUtil.is1d7OrHigher(context.getCompilationUnit().getJavaProject()))
			return false;

		CatchClause catchClause= (CatchClause) ASTResolving.findAncestor(covering, ASTNode.CATCH_CLAUSE);
		if (catchClause == null) {
			return false;
		}

		Statement statement= ASTResolving.findParentStatement(covering);
		if (statement != catchClause.getParent() && statement != catchClause.getBody()) {
			return false; // selection is in a statement inside the body
		}

		Type type1= catchClause.getException().getType();
		Type selectedMultiCatchType= null;
		if (type1.isUnionType() && covering instanceof Name) {
			Name topMostName= ASTNodes.getTopMostName((Name) covering);
			ASTNode parent= topMostName.getParent();
			if (parent instanceof SimpleType || parent instanceof NameQualifiedType) {
				selectedMultiCatchType= (Type) parent;
			}
		}
		if (selectedMultiCatchType != null)
			return false;

		SingleVariableDeclaration singleVariableDeclaration= catchClause.getException();
		Type type= singleVariableDeclaration.getType();
		if (!(type instanceof UnionType))
			return false;

		if (resultingCollections == null)
			return true;

		AST ast= covering.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);

		TryStatement tryStatement= (TryStatement) catchClause.getParent();
		ListRewrite listRewrite= rewrite.getListRewrite(tryStatement, TryStatement.CATCH_CLAUSES_PROPERTY);

		UnionType unionType= (UnionType) type;
		List<Type> types= unionType.types();
		for (int i= types.size() - 1; i >= 0; i--) {
			Type type2= types.get(i);
			CatchClause newCatchClause= ast.newCatchClause();

			SingleVariableDeclaration newSingleVariableDeclaration= ast.newSingleVariableDeclaration();
			newSingleVariableDeclaration.setType((Type) rewrite.createCopyTarget(type2));
			newSingleVariableDeclaration.setName((SimpleName) rewrite.createCopyTarget(singleVariableDeclaration.getName()));
			newCatchClause.setException(newSingleVariableDeclaration);
			setCatchClauseBody(newCatchClause, rewrite, catchClause);
			listRewrite.insertAfter(newCatchClause, catchClause, null);
		}
		rewrite.remove(catchClause, null);

		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		String label= CorrectionMessages.QuickAssistProcessor_convert_to_multiple_singletype_catch_blocks;
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, IProposalRelevance.USE_SEPARATE_CATCH_BLOCKS, image);
		resultingCollections.add(proposal);
		return true;
	}

	private static void setCatchClauseBody(CatchClause newCatchClause, ASTRewrite rewrite, CatchClause catchClause) {
		// Workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=350285

//		newCatchClause.setBody((Block) rewrite.createCopyTarget(catchClause.getBody()));

		//newCatchClause#setBody() destroys the formatting, hence copy statement by statement.
		List<Statement> statements= catchClause.getBody().statements();
		for (Statement statement : statements) {
			newCatchClause.getBody().statements().add(rewrite.createCopyTarget(statement));
		}
	}

	private static boolean getRenameLocalProposals(IInvocationContext context, ASTNode node, IProblemLocation[] locations, Collection<ICommandAccess> resultingCollections) {
		if (!(node instanceof SimpleName)) {
			return false;
		}
		SimpleName name= (SimpleName) node;
		if (name.getAST().apiLevel() >= ASTHelper.JLS10 && name.isVar()) {
			return false;
		}
		IBinding binding= name.resolveBinding();
		if (binding != null && (binding.getKind() == IBinding.PACKAGE || binding.getKind() == IBinding.MODULE)) {
			return false;
		}

		if (containsQuickFixableRenameLocal(locations)) {
			return false;
		}

		if (resultingCollections == null) {
			return true;
		}

		LinkedNamesAssistProposal proposal= new LinkedNamesAssistProposal(context, name);
		if (locations.length != 0) {
			proposal.setRelevance(IProposalRelevance.LINKED_NAMES_ASSIST_ERROR);
		}

		resultingCollections.add(proposal);
		return true;
	}

	private static boolean getRenameRefactoringProposal(IInvocationContext context, ASTNode node, IProblemLocation[] locations, Collection<ICommandAccess> resultingCollections)
			throws CoreException {
		if (!(context instanceof AssistContext)) {
			return false;
		}
		IEditorPart editor= ((AssistContext) context).getEditor();
		if (!(editor instanceof JavaEditor))
			return false;

		if (!(node instanceof SimpleName)) {
			return false;
		}
		SimpleName name= (SimpleName) node;
		if (name.getAST().apiLevel() >= ASTHelper.JLS10 && name.isVar()) {
			return false;
		}
		IBinding binding= name.resolveBinding();
		if (binding == null) {
			return false;
		}

		IJavaElement javaElement= binding.getJavaElement();
		if (javaElement == null ? !isRecordComponentAccessorMethod(binding) : !RefactoringAvailabilityTesterCore.isRenameElementAvailable(javaElement)) {
			return false;
		}

		if (resultingCollections == null) {
			return true;
		}

		RenameRefactoringProposal proposal= new RenameRefactoringProposal((JavaEditor) editor);
		if (locations.length != 0) {
			proposal.setRelevance(IProposalRelevance.RENAME_REFACTORING_ERROR);
		} else if (containsQuickFixableRenameLocal(locations)) {
			proposal.setRelevance(IProposalRelevance.RENAME_REFACTORING_QUICK_FIX);
		}


		resultingCollections.add(proposal);
		return true;
	}

	private static boolean isRecordComponentAccessorMethod(IBinding binding) {
		boolean isAccessor= false;
		if (binding instanceof IMethodBinding && ((IMethodBinding) binding).isSyntheticRecordMethod()) {
			IMethodBinding mBinding= (IMethodBinding) binding;
			ITypeBinding tBinding= mBinding.getDeclaringClass();
			if (tBinding.isRecord() && mBinding.getParameterTypes().length == 0) {
				IVariableBinding[] bindings= tBinding.getDeclaredFields();
				if (bindings != null && bindings.length > 0) {
					for (IVariableBinding varBinding : bindings) {
						int modifiers= varBinding.getModifiers();
						if (!Flags.isStatic(modifiers) && varBinding.getName().equals(mBinding.getName())) {
							isAccessor= true;
							break;
						}
					}
				}
			}
		}
		return isAccessor;
	}

	private static boolean containsQuickFixableRenameLocal(IProblemLocation[] locations) {
		if (locations != null) {
			for (IProblemLocation location : locations) {
				if (IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER.equals(location.getMarkerType())) {
					switch (location.getProblemId()) {
						case IProblem.LocalVariableHidingLocalVariable:
						case IProblem.LocalVariableHidingField:
						case IProblem.FieldHidingLocalVariable:
						case IProblem.FieldHidingField:
						case IProblem.ArgumentHidingLocalVariable:
						case IProblem.ArgumentHidingField:
							return true;
					}
				}
			}
		}
		return false;
	}

	public static ASTNode getCopyOfInner(ASTRewrite rewrite, ASTNode statement, boolean toControlStatementBody) {
		if (statement.getNodeType() == ASTNode.BLOCK) {
			Block block= (Block) statement;
			List<Statement> innerStatements= block.statements();
			int nStatements= innerStatements.size();
			if (nStatements == 1) {
				return rewrite.createCopyTarget(innerStatements.get(0));
			} else if (nStatements > 1) {
				if (toControlStatementBody) {
					return rewrite.createCopyTarget(block);
				}
				ListRewrite listRewrite= rewrite.getListRewrite(block, Block.STATEMENTS_PROPERTY);
				ASTNode first= innerStatements.get(0);
				ASTNode last= innerStatements.get(nStatements - 1);
				return listRewrite.createCopyTarget(first, last);
			}
			return null;
		} else {
			return rewrite.createCopyTarget(statement);
		}
	}


	private static boolean getUnWrapProposals(IInvocationContext context, ASTNode node, Collection<ICommandAccess> resultingCollections) {
		ASTNode outer= node;

		Block block= null;
		if (outer.getNodeType() == ASTNode.BLOCK) {
			block= (Block) outer;
			outer= block.getParent();
		}

		ASTNode body= null;
		String label= null;
		if (outer instanceof IfStatement) {
			IfStatement ifStatement= (IfStatement) outer;
			Statement elseBlock= ifStatement.getElseStatement();
			if (elseBlock == null || elseBlock instanceof Block && ((Block) elseBlock).statements().isEmpty()) {
				body= ifStatement.getThenStatement();
			}
			label= CorrectionMessages.QuickAssistProcessor_unwrap_ifstatement;
		} else if (outer instanceof WhileStatement) {
			body= ((WhileStatement) outer).getBody();
			label= CorrectionMessages.QuickAssistProcessor_unwrap_whilestatement;
		} else if (outer instanceof ForStatement) {
			body= ((ForStatement) outer).getBody();
			label= CorrectionMessages.QuickAssistProcessor_unwrap_forstatement;
		} else if (outer instanceof EnhancedForStatement) {
			body= ((EnhancedForStatement) outer).getBody();
			label= CorrectionMessages.QuickAssistProcessor_unwrap_forstatement;
		} else if (outer instanceof SynchronizedStatement) {
			body= ((SynchronizedStatement) outer).getBody();
			label= CorrectionMessages.QuickAssistProcessor_unwrap_synchronizedstatement;
		} else if (outer instanceof SimpleName && outer.getParent() instanceof LabeledStatement) {
			LabeledStatement labeledStatement= (LabeledStatement) outer.getParent();
			outer= labeledStatement;
			body= labeledStatement.getBody();
			label= CorrectionMessages.QuickAssistProcessor_unwrap_labeledstatement;
		} else if (outer instanceof LabeledStatement) {
			body= ((LabeledStatement) outer).getBody();
			label= CorrectionMessages.QuickAssistProcessor_unwrap_labeledstatement;
		} else if (outer instanceof DoStatement) {
			body= ((DoStatement) outer).getBody();
			label= CorrectionMessages.QuickAssistProcessor_unwrap_dostatement;
		} else if (outer instanceof TryStatement) {
			TryStatement tryStatement= (TryStatement) outer;
			if (tryStatement.catchClauses().isEmpty() && tryStatement.resources().isEmpty()) {
				body= tryStatement.getBody();
			}
			label= CorrectionMessages.QuickAssistProcessor_unwrap_trystatement;
		} else if (outer instanceof AnonymousClassDeclaration) {
			List<BodyDeclaration> decls= ((AnonymousClassDeclaration) outer).bodyDeclarations();
			for (BodyDeclaration elem : decls) {
				if (elem instanceof MethodDeclaration) {
					Block curr= ((MethodDeclaration) elem).getBody();
					if (curr != null && !curr.statements().isEmpty()) {
						if (body != null) {
							return false;
						}
						body= curr;
					}
				} else if (elem instanceof TypeDeclaration) {
					return false;
				}
			}
			label= CorrectionMessages.QuickAssistProcessor_unwrap_anonymous;
			outer= ASTResolving.findParentStatement(outer);
			if (outer == null) {
				return false; // private Object o= new Object() { ... };
			}
		} else if (outer instanceof Block) {
			//	-> a block in a block
			body= block;
			outer= block;
			label= CorrectionMessages.QuickAssistProcessor_unwrap_block;
		} else if (outer instanceof ParenthesizedExpression) {
			//ParenthesizedExpression expression= (ParenthesizedExpression) outer;
			//body= expression.getExpression();
			//label= CorrectionMessages.getString("QuickAssistProcessor.unwrap.parenthesis");	 //$NON-NLS-1$
		} else if (outer instanceof MethodInvocation) {
			MethodInvocation invocation= (MethodInvocation) outer;
			if (invocation.arguments().size() == 1) {
				body= (ASTNode) invocation.arguments().get(0);
				if (invocation.getParent().getNodeType() == ASTNode.EXPRESSION_STATEMENT) {
					int kind= body.getNodeType();
					if (kind != ASTNode.ASSIGNMENT && kind != ASTNode.PREFIX_EXPRESSION && kind != ASTNode.POSTFIX_EXPRESSION
							&& kind != ASTNode.METHOD_INVOCATION && kind != ASTNode.SUPER_METHOD_INVOCATION) {
						body= null;
					}
				}
				label= CorrectionMessages.QuickAssistProcessor_unwrap_methodinvocation;
			}
		}
		if (body == null || outer == null) {
			return false;
		}
		ASTRewrite rewrite= ASTRewrite.create(outer.getAST());
		ASTNode inner= getCopyOfInner(rewrite, body, ASTNodes.isControlStatementBody(outer.getLocationInParent()));
		if (inner == null) {
			return false;
		}
		if (resultingCollections == null) {
			return true;
		}

		rewrite.replace(outer, inner, null);
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, IProposalRelevance.UNWRAP_STATEMENTS, image);
		resultingCollections.add(proposal);
		return true;
	}

	private static boolean isControlStatementWithBlock(ASTNode node) {
		switch (node.getNodeType()) {
			case ASTNode.IF_STATEMENT:
			case ASTNode.WHILE_STATEMENT:
			case ASTNode.FOR_STATEMENT:
			case ASTNode.ENHANCED_FOR_STATEMENT:
			case ASTNode.DO_STATEMENT:
				return true;
			default:
				return false;
		}
	}

	private static boolean getRemoveBlockProposals(IInvocationContext context, ASTNode coveringNode, Collection<ICommandAccess> resultingCollections) {
		IProposableFix[] fixes= ControlStatementsFix.createRemoveBlockFix(context.getASTRoot(), coveringNode);
		if (fixes != null) {
			if (resultingCollections == null) {
				return true;
			}
			Map<String, String> options= new Hashtable<>();
			options.put(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS, CleanUpOptions.TRUE);
			options.put(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_NEVER, CleanUpOptions.TRUE);
			ICleanUp cleanUp= new ControlStatementsCleanUp(options);
			for (IProposableFix fix : fixes) {
				Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
				FixCorrectionProposal proposal= new FixCorrectionProposal(fix, cleanUp, IProposalRelevance.REMOVE_BLOCK_FIX, image, context);
				resultingCollections.add(proposal);
			}
			return true;
		}
		return false;
	}

	public static boolean getTryWithResourceAssistProposals(IProblemLocation[] locations, IInvocationContext context, ASTNode node,
			ArrayList<ASTNode> coveredNodes, Collection<ICommandAccess> resultingCollections) throws IllegalArgumentException, CoreException {
		for (IProblemLocation location : locations) {
			if ((location.getProblemId() == IProblem.UnclosedCloseable ||
					location.getProblemId() == IProblem.PotentiallyUnclosedCloseable)) {
				return false;
			}
		}
		return getTryWithResourceProposals(context, node, coveredNodes, resultingCollections);
	}

	@SuppressWarnings({ "null" })
	public static boolean getTryWithResourceProposals(IInvocationContext context, ASTNode node, ArrayList<ASTNode> coveredNodes, Collection<ICommandAccess> resultingCollections)
			throws IllegalArgumentException, CoreException {
		if (!JavaModelUtil.is1d8OrHigher(context.getCompilationUnit().getJavaProject()))
			return false;

		ASTNode parentStatement= ASTResolving.findAncestor(node, ASTNode.VARIABLE_DECLARATION_STATEMENT);
		if (!(parentStatement instanceof VariableDeclarationStatement) &&
				!(parentStatement instanceof ExpressionStatement) &&
				!(node instanceof SimpleName)
				&& (coveredNodes == null || coveredNodes.isEmpty())) {
			return false;
		}
		List<ASTNode> coveredStatements= new ArrayList<>();
		if (coveredNodes == null || coveredNodes.isEmpty() && parentStatement != null) {
			coveredStatements.add(parentStatement);
		} else {
			for (ASTNode coveredNode : coveredNodes) {
				Statement statement= ASTResolving.findParentStatement(coveredNode);
				if (statement == null) {
					continue;
				}
				if (!coveredStatements.contains(statement)) {
					coveredStatements.add(statement);
				}
			}
		}
		List<ASTNode> coveredAutoClosableNodes= QuickAssistProcessorUtil.getCoveredAutoClosableNodes(coveredStatements);
		if (coveredAutoClosableNodes.isEmpty()) {
			return false;
		}

		ASTNode parentBodyDeclaration= (node instanceof Block || node instanceof BodyDeclaration)
				? node
				: ASTNodes.getFirstAncestorOrNull(node, Block.class, BodyDeclaration.class);

		int start= coveredAutoClosableNodes.get(0).getStartPosition();
		int end= start;

		for (ASTNode astNode : coveredAutoClosableNodes) {
			int endPosition= QuickAssistProcessorUtil.findEndPostion(astNode);
			end= Math.max(end, endPosition);
		}

		// recursive loop to find all nodes affected by wrapping in try block
		List<ASTNode> nodesInRange= SurroundWithTryWithResourcesRefactoringCore.findNodesInRange(parentBodyDeclaration, start, end);
		int oldEnd= end;
		while (true) {
			int newEnd= oldEnd;
			for (ASTNode astNode : nodesInRange) {
				int endPosition= QuickAssistProcessorUtil.findEndPostion(astNode);
				newEnd= Math.max(newEnd, endPosition);
			}
			if (newEnd > oldEnd) {
				oldEnd= newEnd;
				nodesInRange= SurroundWithTryWithResourcesRefactoringCore.findNodesInRange(parentBodyDeclaration, start, newEnd);
				continue;
			}
			break;
		}
		nodesInRange.removeAll(coveredAutoClosableNodes);

		CompilationUnit cu= (CompilationUnit) node.getRoot();
		IBuffer buffer= context.getCompilationUnit().getBuffer();
		AST ast= node.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);
		boolean modifyExistingTry= false;
		TryStatement newTryStatement= null;
		Block newTryBody= null;
		TryStatement enclosingTry= (TryStatement) ASTResolving.findAncestor(node, ASTNode.TRY_STATEMENT);
		ListRewrite resourcesRewriter= null;
		ListRewrite clausesRewriter= null;
		if (needNewTryBlock(coveredStatements, enclosingTry)) {
			newTryStatement= ast.newTryStatement();
			newTryBody= ast.newBlock();
			newTryStatement.setBody(newTryBody);
		} else {
			modifyExistingTry= true;
			resourcesRewriter= rewrite.getListRewrite(enclosingTry, TryStatement.RESOURCES2_PROPERTY);
			clausesRewriter= rewrite.getListRewrite(enclosingTry, TryStatement.CATCH_CLAUSES_PROPERTY);
		}
		ICompilationUnit icu= context.getCompilationUnit();
		ASTNode lastNode= nodesInRange.isEmpty()
				? coveredAutoClosableNodes.get(coveredAutoClosableNodes.size() - 1)
				: nodesInRange.get(nodesInRange.size() - 1);
		Selection selection= Selection.createFromStartLength(start, lastNode.getStartPosition() - start + lastNode.getLength());
		SurroundWithTryWithResourcesAnalyzer analyzer= new SurroundWithTryWithResourcesAnalyzer(icu, selection);
		cu.accept(analyzer);
		ITypeBinding[] exceptions= analyzer.getExceptions(analyzer.getSelection());
		List<ITypeBinding> allExceptions= new ArrayList<>(Arrays.asList(exceptions));
		int resourceCount= 0;
		for (ASTNode coveredNode : coveredAutoClosableNodes) {
			ASTNode findAncestor= ASTResolving.findAncestor(coveredNode, ASTNode.VARIABLE_DECLARATION_STATEMENT);
			if (findAncestor == null) {
				findAncestor= ASTResolving.findAncestor(coveredNode, ASTNode.ASSIGNMENT);
			}
			if (findAncestor instanceof VariableDeclarationStatement) {
				VariableDeclarationStatement vds= (VariableDeclarationStatement) findAncestor;
				String commentToken= null;
				int extendedStatementStart= cu.getExtendedStartPosition(vds);
				if (vds.getStartPosition() > extendedStatementStart) {
					commentToken= buffer.getText(extendedStatementStart, vds.getStartPosition() - extendedStatementStart);
				}
				Type type= vds.getType();
				ITypeBinding typeBinding= type.resolveBinding();
				if (typeBinding != null) {
					IMethodBinding close= SurroundWithTryWithResourcesRefactoringCore.findAutocloseMethod(typeBinding);
					if (close != null) {
						for (ITypeBinding exceptionType : close.getExceptionTypes()) {
							if (!allExceptions.contains(exceptionType)) {
								allExceptions.add(exceptionType);
							}
						}
					}
				}
				String typeName= buffer.getText(type.getStartPosition(), type.getLength());

				for (Object object : vds.fragments()) {
					VariableDeclarationFragment variableDeclarationFragment= (VariableDeclarationFragment) object;
					VariableDeclarationFragment newVariableDeclarationFragment= ast.newVariableDeclarationFragment();
					SimpleName name= variableDeclarationFragment.getName();

					if (commentToken == null) {
						int extendedStart= cu.getExtendedStartPosition(variableDeclarationFragment);
						commentToken= buffer.getText(extendedStart, variableDeclarationFragment.getStartPosition() - extendedStart);
					}
					commentToken= Strings.trimTrailingTabsAndSpaces(commentToken);

					newVariableDeclarationFragment.setName(ast.newSimpleName(name.getIdentifier()));
					Expression newExpression= null;
					Expression initializer= variableDeclarationFragment.getInitializer();
					if (initializer == null) {
						rewrite.remove(coveredNode, null);
						continue;
					} else {
						newExpression= (Expression) rewrite.createMoveTarget(initializer);
					}
					newVariableDeclarationFragment.setInitializer(newExpression);
					VariableDeclarationExpression newVariableDeclarationExpression= ast.newVariableDeclarationExpression(newVariableDeclarationFragment);
					newVariableDeclarationExpression.setType(
							(Type) rewrite.createStringPlaceholder(commentToken + typeName, type.getNodeType()));
					resourceCount++;
					if (modifyExistingTry) {
						resourcesRewriter.insertLast(newVariableDeclarationExpression, null);
					} else {
						newTryStatement.resources().add(newVariableDeclarationExpression);
					}
					commentToken= null;
				}
			}
		}

		if (resourceCount == 0) {
			return false;
		}

		String label= CorrectionMessages.QuickAssistProcessor_convert_to_try_with_resource;
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		LinkedCorrectionProposal proposal= new LinkedCorrectionProposal(label, context.getCompilationUnit(), rewrite, IProposalRelevance.SURROUND_WITH_TRY_CATCH, image);

		ImportRewrite imports= proposal.createImportRewrite(context.getASTRoot());
		ImportRewriteContext importRewriteContext= new ContextSensitiveImportRewriteContext(node, imports);

		CatchClause catchClause= ast.newCatchClause();
		SingleVariableDeclaration decl= ast.newSingleVariableDeclaration();
		String varName= StubUtility.getExceptionVariableName(icu.getJavaProject());
		parentBodyDeclaration.getRoot().accept(analyzer);
		CodeScopeBuilder.Scope scope= CodeScopeBuilder.perform(analyzer.getEnclosingBodyDeclaration(), selection).findScope(selection.getOffset(), selection.getLength());
		scope.setCursor(selection.getOffset());
		String name= scope.createName(varName, false);
		decl.setName(ast.newSimpleName(name));

		List<ITypeBinding> mustRethrowList= new ArrayList<>();
		List<ITypeBinding> catchExceptions= analyzer.calculateCatchesAndRethrows(ASTNodes.filterSubtypes(allExceptions), mustRethrowList);
		List<ITypeBinding> filteredExceptions= ASTNodes.filterSubtypes(catchExceptions);

		if (catchExceptions.size() > 0) {
			final String GROUP_EXC_NAME= "exc_name"; //$NON-NLS-1$
			final String GROUP_EXC_TYPE= "exc_type"; //$NON-NLS-1$
			LinkedProposalModelCore linkedProposalModel= createProposalModel();

			int i= 0;
			if (!modifyExistingTry) {
				for (ITypeBinding mustThrow : mustRethrowList) {
					CatchClause newClause= ast.newCatchClause();
					SingleVariableDeclaration newDecl= ast.newSingleVariableDeclaration();
					newDecl.setName(ast.newSimpleName(name));
					Type importType= imports.addImport(mustThrow, ast, importRewriteContext, TypeLocation.EXCEPTION);
					newDecl.setType(importType);
					newClause.setException(newDecl);
					ThrowStatement newThrowStatement= ast.newThrowStatement();
					newThrowStatement.setExpression(ast.newSimpleName(name));
					linkedProposalModel.getPositionGroup(GROUP_EXC_NAME + i, true).addPosition(rewrite.track(decl.getName()), false);
					newClause.getBody().statements().add(newThrowStatement);
					newTryStatement.catchClauses().add(newClause);
					++i;
				}
			}
			UnionType unionType= ast.newUnionType();
			List<Type> types= unionType.types();
			for (ITypeBinding exception : filteredExceptions) {
				Type type= imports.addImport(exception, ast, importRewriteContext, TypeLocation.EXCEPTION);
				types.add(type);
				linkedProposalModel.getPositionGroup(GROUP_EXC_TYPE + i, true).addPosition(rewrite.track(type), i == 0);
				i++;
			}

			decl.setType(unionType);
			catchClause.setException(decl);
			linkedProposalModel.getPositionGroup(GROUP_EXC_NAME + 0, true).addPosition(rewrite.track(decl.getName()), false);
			Statement st= null;
			String s= StubUtility.getCatchBodyContent(icu, "Exception", name, coveredStatements.get(0), icu.findRecommendedLineSeparator()); //$NON-NLS-1$
			if (s != null) {
				st= (Statement) rewrite.createStringPlaceholder(s, ASTNode.RETURN_STATEMENT);
			}
			if (st != null) {
				catchClause.getBody().statements().add(st);
			}
			if (modifyExistingTry) {
				clausesRewriter.insertLast(catchClause, null);
			} else {
				newTryStatement.catchClauses().add(catchClause);
			}
		}

		if (modifyExistingTry) {
			for (int i= 0; i < coveredAutoClosableNodes.size(); i++) {
				rewrite.remove(coveredAutoClosableNodes.get(i), null);
			}
		} else {
			if (!nodesInRange.isEmpty()) {
				ASTNode firstNode= nodesInRange.get(0);
				ASTNode methodDeclaration= ASTResolving.findAncestor(firstNode, ASTNode.BLOCK);
				ListRewrite listRewrite= rewrite.getListRewrite(methodDeclaration, Block.STATEMENTS_PROPERTY);
				ASTNode createCopyTarget= listRewrite.createMoveTarget(firstNode, nodesInRange.get(nodesInRange.size() - 1));
				rewrite.getListRewrite(newTryBody, Block.STATEMENTS_PROPERTY).insertFirst(createCopyTarget, null);
			}

			// replace first node and delete the rest of selected nodes
			rewrite.replace(coveredAutoClosableNodes.get(0), newTryStatement, null);
			for (int i= 1; i < coveredAutoClosableNodes.size(); i++) {
				rewrite.remove(coveredAutoClosableNodes.get(i), null);
			}
		}

		resultingCollections.add(proposal);
		return true;
	}

	private static boolean needNewTryBlock(List<ASTNode> coveredStatements, TryStatement enclosingTry) {
		if (enclosingTry == null || enclosingTry.getBody() == null) {
			return true;
		}
		List<?> statements= enclosingTry.getBody().statements();
		return statements.size() > 0 && coveredStatements.size() > 0 && statements.get(0) != coveredStatements.get(0);
	}

	private static boolean getAddBlockProposals(IInvocationContext context, ASTNode node, Collection<ICommandAccess> resultingCollections) {
		if (!(node instanceof Statement)) {
			return false;
		}

		/*
		 * only show the quick assist when the selection is of the control statement keywords (if, else, while,...)
		 * but not inside the statement or the if expression.
		 */
		if (!isControlStatementWithBlock(node) && isControlStatementWithBlock(node.getParent())) {
			int statementStart= node.getStartPosition();
			int statementEnd= statementStart + node.getLength();

			int offset= context.getSelectionOffset();
			int length= context.getSelectionLength();
			if (length == 0) {
				if (offset != statementEnd) { // cursor at end
					return false;
				}
			} else {
				if (offset > statementStart || offset + length < statementEnd) { // statement selected
					return false;
				}
			}
			node= node.getParent();
		}

		StructuralPropertyDescriptor childProperty= null;
		ASTNode child= null;
		switch (node.getNodeType()) {
			case ASTNode.IF_STATEMENT:
				ASTNode then= ((IfStatement) node).getThenStatement();
				ASTNode elseStatement= ((IfStatement) node).getElseStatement();
				if ((then instanceof Block) && (elseStatement instanceof Block || elseStatement == null)) {
					break;
				}
				int thenEnd= then.getStartPosition() + then.getLength();
				int selectionEnd= context.getSelectionOffset() + context.getSelectionLength();
				if (!(then instanceof Block)) {
					if (selectionEnd <= thenEnd) {
						childProperty= IfStatement.THEN_STATEMENT_PROPERTY;
						child= then;
						break;
					} else if (elseStatement != null && selectionEnd < elseStatement.getStartPosition()) {
						// find out if we are before or after the 'else' keyword
						try {
							TokenScanner scanner= new TokenScanner(context.getCompilationUnit());
							int elseTokenStart= scanner.getNextStartOffset(thenEnd, true);
							if (selectionEnd < elseTokenStart) {
								childProperty= IfStatement.THEN_STATEMENT_PROPERTY;
								child= then;
								break;
							}
						} catch (CoreException e) {
							// ignore
						}
					}
				}
				if (elseStatement != null && !(elseStatement instanceof Block) && context.getSelectionOffset() >= thenEnd) {
					childProperty= IfStatement.ELSE_STATEMENT_PROPERTY;
					child= elseStatement;
				}
				break;
			case ASTNode.WHILE_STATEMENT:
				ASTNode whileBody= ((WhileStatement) node).getBody();
				if (!(whileBody instanceof Block)) {
					childProperty= WhileStatement.BODY_PROPERTY;
					child= whileBody;
				}
				break;
			case ASTNode.FOR_STATEMENT:
				ASTNode forBody= ((ForStatement) node).getBody();
				if (!(forBody instanceof Block)) {
					childProperty= ForStatement.BODY_PROPERTY;
					child= forBody;
				}
				break;
			case ASTNode.ENHANCED_FOR_STATEMENT:
				ASTNode enhancedForBody= ((EnhancedForStatement) node).getBody();
				if (!(enhancedForBody instanceof Block)) {
					childProperty= EnhancedForStatement.BODY_PROPERTY;
					child= enhancedForBody;
				}
				break;
			case ASTNode.DO_STATEMENT:
				ASTNode doBody= ((DoStatement) node).getBody();
				if (!(doBody instanceof Block)) {
					childProperty= DoStatement.BODY_PROPERTY;
					child= doBody;
				}
				break;
			default:
		}
		if (child == null) {
			return false;
		}

		if (resultingCollections == null) {
			return true;
		}
		AST ast= node.getAST();
		{
			ASTRewrite rewrite= ASTRewrite.create(ast);

			ASTNode childPlaceholder= rewrite.createMoveTarget(child);
			Block replacingBody= ast.newBlock();
			replacingBody.statements().add(childPlaceholder);
			rewrite.set(node, childProperty, replacingBody, null);

			String label;
			if (childProperty == IfStatement.THEN_STATEMENT_PROPERTY) {
				label= CorrectionMessages.QuickAssistProcessor_replacethenwithblock_description;
			} else if (childProperty == IfStatement.ELSE_STATEMENT_PROPERTY) {
				label= CorrectionMessages.QuickAssistProcessor_replaceelsewithblock_description;
			} else {
				label= CorrectionMessages.QuickAssistProcessor_replacebodywithblock_description;
			}

			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			LinkedCorrectionProposal proposal= new LinkedCorrectionProposal(label, context.getCompilationUnit(), rewrite, IProposalRelevance.ADD_BLOCK, image);
			proposal.setCommandId(ADD_BLOCK_ID);
			proposal.setEndPosition(rewrite.track(child));
			resultingCollections.add(proposal);
		}

		if (node.getNodeType() == ASTNode.IF_STATEMENT) {
			ASTRewrite rewrite= ASTRewrite.create(ast);

			while (node.getLocationInParent() == IfStatement.ELSE_STATEMENT_PROPERTY) {
				node= node.getParent();
			}

			boolean missingBlockFound= false;
			boolean foundElse= false;

			IfStatement ifStatement;
			Statement thenStatment;
			Statement elseStatment;
			do {
				ifStatement= (IfStatement) node;
				thenStatment= ifStatement.getThenStatement();
				elseStatment= ifStatement.getElseStatement();

				if (!(thenStatment instanceof Block)) {
					ASTNode childPlaceholder1= rewrite.createMoveTarget(thenStatment);
					Block replacingBody1= ast.newBlock();
					replacingBody1.statements().add(childPlaceholder1);
					rewrite.set(ifStatement, IfStatement.THEN_STATEMENT_PROPERTY, replacingBody1, null);
					if (thenStatment != child) {
						missingBlockFound= true;
					}
				}
				if (elseStatment != null) {
					foundElse= true;
				}
				node= elseStatment;
			} while (elseStatment instanceof IfStatement);

			if (elseStatment != null && !(elseStatment instanceof Block)) {
				ASTNode childPlaceholder2= rewrite.createMoveTarget(elseStatment);

				Block replacingBody2= ast.newBlock();
				replacingBody2.statements().add(childPlaceholder2);
				rewrite.set(ifStatement, IfStatement.ELSE_STATEMENT_PROPERTY, replacingBody2, null);
				if (elseStatment != child) {
					missingBlockFound= true;
				}
			}

			if (missingBlockFound && foundElse) {
				String label= CorrectionMessages.QuickAssistProcessor_replacethenelsewithblock_description;
				Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
				ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, IProposalRelevance.CHANGE_IF_ELSE_TO_BLOCK, image);
				resultingCollections.add(proposal);
			}
		}
		return true;
	}

	private static boolean getInvertEqualsProposal(IInvocationContext context, ASTNode node, Collection<ICommandAccess> resultingCollections) {
		if (resultingCollections == null) {
			return true;
		}
		InvertEqualsExpressionFixCore fix= InvertEqualsExpressionFixCore.createInvertEqualsFix(context.getASTRoot(), node);
		if (fix != null) {
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			FixCorrectionProposal proposal= new FixCorrectionProposal(fix, null, IProposalRelevance.INVERT_EQUALS, image, context);
			resultingCollections.add(proposal);
			return true;
		}
		return false;
	}

	private static boolean getAddStaticMemberFavoritesProposals(ASTNode node, Collection<ICommandAccess> resultingCollections) {
		if (!(node instanceof ImportDeclaration)) {
			node= ASTNodes.getFirstAncestorOrNull(node, ImportDeclaration.class);
		}
		if (!(node instanceof ImportDeclaration)) {
			return false;
		}
		ImportDeclaration decl= (ImportDeclaration) node;
		if (!decl.isStatic()) {
			return false;
		}
		Name name= decl.getName();
		String importName= name.getFullyQualifiedName();
		OptionsConfigurationBlock.Key prefKey= new Key(JavaUI.ID_PLUGIN, PreferenceConstants.CODEASSIST_FAVORITE_STATIC_MEMBERS);
		String favoritesRaw= prefKey.getStoredValue(InstanceScope.INSTANCE, new WorkingCopyManager());
		String[] existingFavorites= new String[0];
		if (favoritesRaw != null) {
			existingFavorites= deserializeFavorites(favoritesRaw);
		}
		boolean foundMember= false;
		for (String favorite : existingFavorites) {
			if (favorite.endsWith(".*")) { //$NON-NLS-1$
				String trimmedFavorite= favorite.substring(0, favorite.length() - 2);
				if (importName.startsWith(trimmedFavorite) && (trimmedFavorite.length() == importName.length() ||
						importName.charAt(trimmedFavorite.length()) == '.')) {
					return false;
				}
			} else {
				if (favorite.equals(importName)) {
					foundMember= true;
				}
			}
		}
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_ADD);
		String desc= CorrectionMessages.QuickAssistProcessor_modify_favorites_desc;
		if (decl.isOnDemand()) {
			String favorite= importName + ".*"; //$NON-NLS-1$
			String label= NLS.bind(CorrectionMessages.QuickAssistProcessor_modify_favorites, favorite);
			resultingCollections.add(new AddStaticFavoriteProposal(favorite, label, desc, image, IProposalRelevance.ADD_STATIC_FAVORITE));
		} else {
			String label= ""; //$NON-NLS-1$
			if (!foundMember) {
				label= NLS.bind(CorrectionMessages.QuickAssistProcessor_modify_favorites, importName);
				resultingCollections.add(new AddStaticFavoriteProposal(importName, label, desc, image, IProposalRelevance.ADD_STATIC_FAVORITE));
			}
			int lastDot= importName.lastIndexOf('.');
			String favorite= importName.substring(0, lastDot) + ".*"; //$NON-NLS-1$
			label= NLS.bind(CorrectionMessages.QuickAssistProcessor_modify_favorites, favorite);
			resultingCollections.add(new AddStaticFavoriteProposal(favorite, label, desc, image, IProposalRelevance.ADD_STATIC_FAVORITE));
		}
		return true;
	}

	private static String[] deserializeFavorites(String str) {
		return str.split(";"); //$NON-NLS-1$
	}

	private static boolean getArrayInitializerToArrayCreation(IInvocationContext context, ASTNode node, Collection<ICommandAccess> resultingCollections) {
		if (!(node instanceof ArrayInitializer)) {
			return false;
		}
		ArrayInitializer initializer= (ArrayInitializer) node;

		ASTNode parent= initializer.getParent();
		while (parent instanceof ArrayInitializer) {
			initializer= (ArrayInitializer) parent;
			parent= parent.getParent();
		}
		ITypeBinding typeBinding= initializer.resolveTypeBinding();
		if (!(parent instanceof VariableDeclaration) || typeBinding == null || !typeBinding.isArray()) {
			return false;
		}
		if (resultingCollections == null) {
			return true;
		}

		AST ast= node.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);

		String label= CorrectionMessages.QuickAssistProcessor_typetoarrayInitializer_description;
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);

		LinkedCorrectionProposal proposal= new LinkedCorrectionProposal(label, context.getCompilationUnit(), rewrite, IProposalRelevance.ADD_TYPE_TO_ARRAY_INITIALIZER, image);

		ImportRewrite imports= proposal.createImportRewrite(context.getASTRoot());
		ImportRewriteContext importRewriteContext= new ContextSensitiveImportRewriteContext(node, imports);
		String typeName= imports.addImport(typeBinding, importRewriteContext);

		ArrayCreation creation= ast.newArrayCreation();
		creation.setInitializer((ArrayInitializer) rewrite.createMoveTarget(initializer));
		creation.setType((ArrayType) ASTNodeFactory.newType(ast, typeName));

		rewrite.replace(initializer, creation, null);

		resultingCollections.add(proposal);
		return true;
	}


	public static boolean getCreateInSuperClassProposals(IInvocationContext context, ASTNode node, Collection<ICommandAccess> resultingCollections) throws CoreException {
		return getCreateInSuperClassProposals(context, node, resultingCollections, true);
	}

	public static boolean getCreateInSuperClassProposals(IInvocationContext context, ASTNode node, Collection<ICommandAccess> resultingCollections, boolean addOverride) throws CoreException {
		if (!(node instanceof SimpleName) || !(node.getParent() instanceof MethodDeclaration)) {
			return false;
		}
		MethodDeclaration decl= (MethodDeclaration) node.getParent();
		if (decl.getName() != node || decl.resolveBinding() == null || Modifier.isPrivate(decl.getModifiers())) {
			return false;
		}

		ICompilationUnit cu= context.getCompilationUnit();
		CompilationUnit astRoot= context.getASTRoot();

		IMethodBinding binding= decl.resolveBinding();
		ITypeBinding[] paramTypes= binding.getParameterTypes();

		ITypeBinding[] superTypes= Bindings.getAllSuperTypes(binding.getDeclaringClass());
		if (resultingCollections == null) {
			for (ITypeBinding curr : superTypes) {
				if (curr.isFromSource() && Bindings.findOverriddenMethodInType(curr, binding) == null) {
					return true;
				}
			}
			return false;
		}
		List<SingleVariableDeclaration> params= decl.parameters();
		String[] paramNames= new String[paramTypes.length];
		for (int i= 0; i < params.size(); i++) {
			SingleVariableDeclaration param= params.get(i);
			paramNames[i]= param.getName().getIdentifier();
		}

		for (ITypeBinding curr : superTypes) {
			if (curr.isFromSource()) {
				IMethodBinding method= Bindings.findOverriddenMethodInType(curr, binding);
				if (method == null) {
					ITypeBinding typeDecl= curr.getTypeDeclaration();
					ICompilationUnit targetCU= ASTResolving.findCompilationUnitForBinding(cu, astRoot, typeDecl);
					if (targetCU != null) {
						String label= Messages.format(CorrectionMessages.QuickAssistProcessor_createmethodinsuper_description,
								new String[] { BasicElementLabels.getJavaElementName(curr.getName()), BasicElementLabels.getJavaElementName(binding.getName()) });
						resultingCollections.add(new NewDefiningMethodProposal(label, targetCU, astRoot, typeDecl, binding, paramNames, addOverride, IProposalRelevance.CREATE_METHOD_IN_SUPER));
					}
				}
			}
		}
		return true;
	}

	private static boolean getConvertEnhancedForLoopProposal(IInvocationContext context, ASTNode node, Collection<ICommandAccess> resultingCollections) {
		EnhancedForStatement enhancedForStatement= getEnclosingHeader(node, EnhancedForStatement.class, EnhancedForStatement.PARAMETER_PROPERTY, EnhancedForStatement.EXPRESSION_PROPERTY);
		if (enhancedForStatement == null) {
			return false;
		}
		SingleVariableDeclaration parameter= enhancedForStatement.getParameter();
		IVariableBinding parameterBinding= parameter.resolveBinding();
		if (parameterBinding == null) {
			return false;
		}
		Expression initializer= enhancedForStatement.getExpression();
		ITypeBinding initializerTypeBinding= initializer.resolveTypeBinding();
		if (initializerTypeBinding == null) {
			return false;
		}

		if (resultingCollections == null) {
			return true;
		}

		Statement topLabelStatement= enhancedForStatement;
		while (topLabelStatement.getLocationInParent() == LabeledStatement.BODY_PROPERTY) {
			topLabelStatement= (Statement) topLabelStatement.getParent();
		}

		IJavaProject project= context.getCompilationUnit().getJavaProject();
		AST ast= node.getAST();
		Statement enhancedForBody= enhancedForStatement.getBody();
		Collection<String> usedVarNames= Arrays.asList(ASTResolving.getUsedVariableNames(enhancedForBody));

		boolean initializerIsArray= initializerTypeBinding.isArray();
		ITypeBinding initializerListType= Bindings.findTypeInHierarchy(initializerTypeBinding, "java.util.List"); //$NON-NLS-1$
		ITypeBinding initializerIterableType= Bindings.findTypeInHierarchy(initializerTypeBinding, "java.lang.Iterable"); //$NON-NLS-1$

		if (initializerIterableType != null) {
			String label= CorrectionMessages.QuickAssistProcessor_convert_to_iterator_for_loop;
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);

			String iterNameKey= "iterName"; //$NON-NLS-1$
			ASTRewrite rewrite= ASTRewrite.create(ast);
			LinkedCorrectionProposal proposal= new LinkedCorrectionProposal(label, context.getCompilationUnit(), rewrite, IProposalRelevance.CONVERT_TO_ITERATOR_FOR_LOOP, image);

			// convert 'for' statement
			ForStatement forStatement= ast.newForStatement();

			// create initializer
			MethodInvocation iterInitializer= ast.newMethodInvocation();
			iterInitializer.setName(ast.newSimpleName("iterator")); //$NON-NLS-1$
			ImportRewrite imports= proposal.createImportRewrite(context.getASTRoot());
			ImportRewriteContext importRewriteContext= new ContextSensitiveImportRewriteContext(node, imports);
			Type iterType= ast.newSimpleType(ast.newName(imports.addImport("java.util.Iterator", importRewriteContext))); //$NON-NLS-1$
			if (initializerIterableType.getTypeArguments().length == 1) {
				Type iterTypeArgument= imports.addImport(Bindings.normalizeTypeBinding(initializerIterableType.getTypeArguments()[0]), ast, importRewriteContext, TypeLocation.TYPE_ARGUMENT);
				ParameterizedType parameterizedIterType= ast.newParameterizedType(iterType);
				parameterizedIterType.typeArguments().add(iterTypeArgument);
				iterType= parameterizedIterType;
			}
			String[] iterNames= StubUtility.getVariableNameSuggestions(NamingConventions.VK_LOCAL, project, iterType, iterInitializer, usedVarNames);
			String iterName= iterNames[0];
			SimpleName initializerIterName= ast.newSimpleName(iterName);

			VariableDeclarationFragment iterFragment= ast.newVariableDeclarationFragment();
			iterFragment.setName(initializerIterName);
			proposal.addLinkedPosition(rewrite.track(initializerIterName), 0, iterNameKey);
			for (String name : iterNames) {
				proposal.addLinkedPositionProposal(iterNameKey, name, null);
			}

			Expression initializerExpression= (Expression) rewrite.createCopyTarget(initializer);
			iterInitializer.setExpression(initializerExpression);
			iterFragment.setInitializer(iterInitializer);

			VariableDeclarationExpression iterVariable= ast.newVariableDeclarationExpression(iterFragment);
			iterVariable.setType(iterType);
			forStatement.initializers().add(iterVariable);

			// create condition
			MethodInvocation condition= ast.newMethodInvocation();
			condition.setName(ast.newSimpleName("hasNext")); //$NON-NLS-1$
			SimpleName conditionExpression= ast.newSimpleName(iterName);
			proposal.addLinkedPosition(rewrite.track(conditionExpression), LinkedPositionGroup.NO_STOP, iterNameKey);
			condition.setExpression(conditionExpression);
			forStatement.setExpression(condition);

			// create 'for' body element variable
			VariableDeclarationFragment elementFragment= ast.newVariableDeclarationFragment();
			elementFragment.extraDimensions().addAll(DimensionRewrite.copyDimensions(parameter.extraDimensions(), rewrite));
			elementFragment.setName((SimpleName) rewrite.createCopyTarget(parameter.getName()));

			SimpleName elementIterName= ast.newSimpleName(iterName);
			proposal.addLinkedPosition(rewrite.track(elementIterName), LinkedPositionGroup.NO_STOP, iterNameKey);

			MethodInvocation getMethodInvocation= ast.newMethodInvocation();
			getMethodInvocation.setName(ast.newSimpleName("next")); //$NON-NLS-1$
			getMethodInvocation.setExpression(elementIterName);
			elementFragment.setInitializer(getMethodInvocation);

			VariableDeclarationStatement elementVariable= ast.newVariableDeclarationStatement(elementFragment);
			ModifierRewrite.create(rewrite, elementVariable).copyAllModifiers(parameter, null);
			elementVariable.setType((Type) rewrite.createCopyTarget(parameter.getType()));

			Block newBody= ast.newBlock();
			List<Statement> statements= newBody.statements();
			statements.add(elementVariable);
			if (enhancedForBody instanceof Block) {
				List<Statement> oldStatements= ((Block) enhancedForBody).statements();
				if (oldStatements.size() > 0) {
					ListRewrite statementsRewrite= rewrite.getListRewrite(enhancedForBody, Block.STATEMENTS_PROPERTY);
					Statement oldStatementsCopy= (Statement) statementsRewrite.createCopyTarget(oldStatements.get(0), oldStatements.get(oldStatements.size() - 1));
					statements.add(oldStatementsCopy);
				}
			} else {
				statements.add((Statement) rewrite.createCopyTarget(enhancedForBody));
			}

			forStatement.setBody(newBody);
			rewrite.replace(enhancedForStatement, forStatement, null);

			resultingCollections.add(proposal);
		}

		if (initializerIsArray || initializerListType != null) {
			String label= CorrectionMessages.QuickAssistProcessor_convert_to_indexed_for_loop;
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);

			String varNameKey= "varName"; //$NON-NLS-1$
			String indexNameKey= "indexName"; //$NON-NLS-1$
			ASTRewrite rewrite= ASTRewrite.create(ast);
			LinkedCorrectionProposal proposal= new LinkedCorrectionProposal(label, context.getCompilationUnit(), rewrite, IProposalRelevance.CONVERT_TO_INDEXED_FOR_LOOP, image);

			// create temp variable from initializer if necessary
			String varName;
			boolean varNameGenerated;
			if (initializer instanceof SimpleName) {
				varName= ((SimpleName) initializer).getIdentifier();
				varNameGenerated= false;
			} else {
				VariableDeclarationFragment varFragment= ast.newVariableDeclarationFragment();
				String[] varNames= StubUtility.getVariableNameSuggestions(NamingConventions.VK_LOCAL, project, initializerTypeBinding, initializer, usedVarNames);
				varName= varNames[0];
				usedVarNames= new ArrayList<>(usedVarNames);
				usedVarNames.add(varName);
				varNameGenerated= true;
				SimpleName varNameNode= ast.newSimpleName(varName);
				varFragment.setName(varNameNode);
				proposal.addLinkedPosition(rewrite.track(varNameNode), 0, varNameKey);
				for (String name : varNames) {
					proposal.addLinkedPositionProposal(varNameKey, name, null);
				}

				varFragment.setInitializer((Expression) rewrite.createCopyTarget(initializer));

				VariableDeclarationStatement varDeclaration= ast.newVariableDeclarationStatement(varFragment);
				Type varType;
				if (initializerIsArray) {
					if (parameter.getType().isVar()) {
						varType= ASTNodeFactory.newType(ast, "var"); //$NON-NLS-1$
					} else {
						Type copiedType= DimensionRewrite.copyTypeAndAddDimensions(parameter.getType(), parameter.extraDimensions(), rewrite);
						varType= ASTNodeFactory.newArrayType(copiedType);
					}
				} else {
					ImportRewrite imports= proposal.createImportRewrite(context.getASTRoot());
					ImportRewriteContext importRewriteContext= new ContextSensitiveImportRewriteContext(node, imports);
					varType= imports.addImport(Bindings.normalizeForDeclarationUse(initializerTypeBinding, ast), ast, importRewriteContext, TypeLocation.TYPE_ARGUMENT);
				}
				varDeclaration.setType(varType);

				if (!(topLabelStatement.getParent() instanceof Block)) {
					Block block= ast.newBlock();
					List<Statement> statements= block.statements();
					statements.add(varDeclaration);
					statements.add((Statement) rewrite.createCopyTarget(topLabelStatement));
					rewrite.replace(topLabelStatement, block, null);
				} else {
					rewrite.getListRewrite(topLabelStatement.getParent(), Block.STATEMENTS_PROPERTY).insertBefore(varDeclaration, topLabelStatement, null);
				}
			}

			// convert 'for' statement
			ForStatement forStatement= ast.newForStatement();

			// create initializer
			VariableDeclarationFragment indexFragment= ast.newVariableDeclarationFragment();
			NumberLiteral indexInitializer= ast.newNumberLiteral();
			indexFragment.setInitializer(indexInitializer);
			PrimitiveType indexType= ast.newPrimitiveType(PrimitiveType.INT);
			String[] indexNames= StubUtility.getVariableNameSuggestions(NamingConventions.VK_LOCAL, project, indexType, indexInitializer, usedVarNames);
			String indexName= indexNames[0];
			SimpleName initializerIndexName= ast.newSimpleName(indexName);
			indexFragment.setName(initializerIndexName);
			proposal.addLinkedPosition(rewrite.track(initializerIndexName), 0, indexNameKey);
			for (String name : indexNames) {
				proposal.addLinkedPositionProposal(indexNameKey, name, null);
			}
			VariableDeclarationExpression indexVariable= ast.newVariableDeclarationExpression(indexFragment);
			indexVariable.setType(indexType);
			forStatement.initializers().add(indexVariable);

			// create condition
			InfixExpression condition= ast.newInfixExpression();
			condition.setOperator(InfixExpression.Operator.LESS);
			SimpleName conditionLeft= ast.newSimpleName(indexName);
			proposal.addLinkedPosition(rewrite.track(conditionLeft), LinkedPositionGroup.NO_STOP, indexNameKey);
			condition.setLeftOperand(conditionLeft);
			SimpleName conditionRightName= ast.newSimpleName(varName);
			if (varNameGenerated) {
				proposal.addLinkedPosition(rewrite.track(conditionRightName), LinkedPositionGroup.NO_STOP, varNameKey);
			}
			Expression conditionRight;
			if (initializerIsArray) {
				conditionRight= ast.newQualifiedName(conditionRightName, ast.newSimpleName("length")); //$NON-NLS-1$
			} else {
				MethodInvocation sizeMethodInvocation= ast.newMethodInvocation();
				sizeMethodInvocation.setName(ast.newSimpleName("size")); //$NON-NLS-1$
				sizeMethodInvocation.setExpression(conditionRightName);
				conditionRight= sizeMethodInvocation;
			}
			condition.setRightOperand(conditionRight);
			forStatement.setExpression(condition);

			// create updater
			SimpleName indexUpdaterName= ast.newSimpleName(indexName);
			proposal.addLinkedPosition(rewrite.track(indexUpdaterName), LinkedPositionGroup.NO_STOP, indexNameKey);
			PostfixExpression indexUpdater= ast.newPostfixExpression();
			indexUpdater.setOperator(PostfixExpression.Operator.INCREMENT);
			indexUpdater.setOperand(indexUpdaterName);
			forStatement.updaters().add(indexUpdater);

			// create 'for' body element variable
			VariableDeclarationFragment elementFragment= ast.newVariableDeclarationFragment();
			elementFragment.extraDimensions().addAll(DimensionRewrite.copyDimensions(parameter.extraDimensions(), rewrite));
			elementFragment.setName((SimpleName) rewrite.createCopyTarget(parameter.getName()));

			SimpleName elementVarName= ast.newSimpleName(varName);
			if (varNameGenerated) {
				proposal.addLinkedPosition(rewrite.track(elementVarName), LinkedPositionGroup.NO_STOP, varNameKey);
			}
			SimpleName elementIndexName= ast.newSimpleName(indexName);
			proposal.addLinkedPosition(rewrite.track(elementIndexName), LinkedPositionGroup.NO_STOP, indexNameKey);

			Expression elementAccess;
			if (initializerIsArray) {
				ArrayAccess elementArrayAccess= ast.newArrayAccess();
				elementArrayAccess.setArray(elementVarName);
				elementArrayAccess.setIndex(elementIndexName);
				elementAccess= elementArrayAccess;
			} else {
				MethodInvocation getMethodInvocation= ast.newMethodInvocation();
				getMethodInvocation.setName(ast.newSimpleName("get")); //$NON-NLS-1$
				getMethodInvocation.setExpression(elementVarName);
				getMethodInvocation.arguments().add(elementIndexName);
				elementAccess= getMethodInvocation;
			}
			elementFragment.setInitializer(elementAccess);

			VariableDeclarationStatement elementVariable= ast.newVariableDeclarationStatement(elementFragment);
			ModifierRewrite.create(rewrite, elementVariable).copyAllModifiers(parameter, null);
			elementVariable.setType((Type) rewrite.createCopyTarget(parameter.getType()));

			Block newBody= ast.newBlock();
			List<Statement> statements= newBody.statements();
			statements.add(elementVariable);
			if (enhancedForBody instanceof Block) {
				List<Statement> oldStatements= ((Block) enhancedForBody).statements();
				if (oldStatements.size() > 0) {
					ListRewrite statementsRewrite= rewrite.getListRewrite(enhancedForBody, Block.STATEMENTS_PROPERTY);
					Statement oldStatementsCopy= (Statement) statementsRewrite.createCopyTarget(oldStatements.get(0), oldStatements.get(oldStatements.size() - 1));
					statements.add(oldStatementsCopy);
				}
			} else {
				statements.add((Statement) rewrite.createCopyTarget(enhancedForBody));
			}

			forStatement.setBody(newBody);
			rewrite.replace(enhancedForStatement, forStatement, null);

			resultingCollections.add(proposal);
		}

		return true;
	}

	private static boolean getUnnecessaryArrayCreationProposal(IInvocationContext context, ASTNode node, Collection<ICommandAccess> resultingCollections) {
		Expression methodInvocation= null;
		if (node instanceof MethodInvocation || node instanceof SuperMethodInvocation) {
			methodInvocation= (Expression) node;
		} else if (node.getParent() instanceof MethodInvocation || node.getParent() instanceof SuperMethodInvocation) {
			methodInvocation= (Expression) node.getParent();
		} else {
			ASTNode n= node;
			while (n != null && !(n instanceof CompilationUnit)) {
				if (n instanceof ArrayCreation) {
					ASTNode parent= n.getParent();
					if (parent instanceof MethodInvocation || parent instanceof SuperMethodInvocation) {
						methodInvocation= (Expression) parent;
						break;
					}
				}
				n= n.getParent();
			}
		}
		if (methodInvocation == null)
			return false;

		if (resultingCollections == null)
			return true;

		IProposableFix fix= UnnecessaryArrayCreationFix.createUnnecessaryArrayCreationFix(context.getASTRoot(), methodInvocation);
		if (fix == null)
			return false;

		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		Map<String, String> options= new HashMap<>();
		options.put(CleanUpConstants.REMOVE_UNNECESSARY_ARRAY_CREATION, CleanUpOptions.TRUE);
		ICleanUp cleanUp= new UnnecessaryArrayCreationCleanUp(options);
		FixCorrectionProposal proposal= new FixCorrectionProposal(fix, cleanUp, IProposalRelevance.REMOVE_UNNECESSARY_ARRAY_CREATION, image, context);
		proposal.setCommandId(REMOVE_UNNECESSARY_ARRAY_CREATION_ID);

		resultingCollections.add(proposal);
		return true;
	}

	private static boolean getDoWhileRatherThanWhileProposal(IInvocationContext context, ASTNode node, Collection<ICommandAccess> resultingCollections) {
		WhileStatement whileStatement= null;
		if (node instanceof WhileStatement) {
			whileStatement= (WhileStatement) node;
		} else if (node.getParent() instanceof WhileStatement) {
			whileStatement= (WhileStatement) node.getParent();
		}
		if (whileStatement == null)
			return false;

		if (resultingCollections == null)
			return true;

		IProposableFix fix= DoWhileRatherThanWhileFixCore.createDoWhileFix(whileStatement);
		if (fix == null)
			return false;

		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		Map<String, String> options= new HashMap<>();
		options.put(CleanUpConstants.DO_WHILE_RATHER_THAN_WHILE, CleanUpOptions.TRUE);
		ICleanUp cleanUp= new DoWhileRatherThanWhileCleanUp(options);
		FixCorrectionProposal proposal= new FixCorrectionProposal(fix, cleanUp, IProposalRelevance.DO_WHILE_RATHER_THAN_WHILE, image, context);
		resultingCollections.add(proposal);
		return true;
	}

	private static boolean getStringConcatToTextBlockProposal(IInvocationContext context, ASTNode node, Collection<ICommandAccess> resultingCollections) {
		ASTNode exp= null;
		if (node instanceof Assignment
				|| node instanceof VariableDeclarationFragment
				|| node instanceof FieldDeclaration
				|| node instanceof VariableDeclarationStatement
				|| node instanceof InfixExpression) {
			exp= node;
		} else {
			ASTNode parent= node.getParent();
			if (parent instanceof Assignment
					|| parent instanceof VariableDeclarationFragment
					|| parent instanceof FieldDeclaration
					|| parent instanceof VariableDeclarationStatement
					|| parent instanceof InfixExpression) {
				exp= parent;
			}
		}

		if (exp == null) {
			exp= ASTNodes.getFirstAncestorOrNull(node, FieldDeclaration.class, VariableDeclarationStatement.class);
		}

		if (exp == null)
			return false;

		if (resultingCollections == null)
			return true;

		IProposableFix fix= StringConcatToTextBlockFixCore.createStringConcatToTextBlockFix(exp);
		if (fix == null)
			return false;

		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		Map<String, String> options= new HashMap<>();
		options.put(CleanUpConstants.STRINGCONCAT_TO_TEXTBLOCK, CleanUpOptions.TRUE);
		options.put(CleanUpConstants.STRINGCONCAT_STRINGBUFFER_STRINGBUILDER, CleanUpOptions.TRUE);
		ICleanUp cleanUp= new StringConcatToTextBlockCleanUp(options);
		FixCorrectionProposal proposal= new FixCorrectionProposal(fix, cleanUp, IProposalRelevance.CONVERT_TO_TEXT_BLOCK, image, context);
		resultingCollections.add(proposal);
		return true;
	}

	private static boolean getConvertForLoopProposal(IInvocationContext context, ASTNode node, Collection<ICommandAccess> resultingCollections) {
		ForStatement forStatement= getEnclosingForStatementHeader(node);
		if (forStatement == null)
			return false;

		if (resultingCollections == null)
			return true;

		IProposableFix fix= ConvertLoopFixCore.createConvertForLoopToEnhancedFix(context.getASTRoot(), forStatement);
		if (fix == null)
			return false;

		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		Map<String, String> options= new HashMap<>();
		options.put(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED, CleanUpOptions.TRUE);
		ICleanUp cleanUp= new ConvertLoopCleanUp(options);
		FixCorrectionProposal proposal= new FixCorrectionProposal(fix, cleanUp, IProposalRelevance.CONVERT_FOR_LOOP_TO_ENHANCED, image, context);
		proposal.setCommandId(CONVERT_FOR_LOOP_ID);

		resultingCollections.add(proposal);
		return true;
	}

	private static boolean getConvertIterableLoopProposal(IInvocationContext context, ASTNode node, Collection<ICommandAccess> resultingCollections) {
		ForStatement forStatement= getEnclosingForStatementHeader(node);
		if (forStatement == null)
			return false;

		if (resultingCollections == null)
			return true;

		IProposableFix fix= ConvertLoopFixCore.createConvertIterableLoopToEnhancedFix(context.getASTRoot(), forStatement);
		if (fix == null)
			return false;

		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		Map<String, String> options= new HashMap<>();
		options.put(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED, CleanUpOptions.TRUE);
		ICleanUp cleanUp= new ConvertLoopCleanUp(options);
		FixCorrectionProposal proposal= new FixCorrectionProposal(fix, cleanUp, IProposalRelevance.CONVERT_ITERABLE_LOOP_TO_ENHANCED, image, context);
		proposal.setCommandId(CONVERT_FOR_LOOP_ID);

		resultingCollections.add(proposal);
		return true;
	}

	public static boolean getGenerateForLoopProposals(IInvocationContext context, ASTNode coveringNode, @SuppressWarnings("unused") IProblemLocation[] locations, Collection<ICommandAccess> resultingCollections) {
//		if (containsMatchingProblem(locations, IProblem.ParsingErrorInsertToComplete))
//			return false;

		Statement statement= ASTResolving.findParentStatement(coveringNode);
		if (!(statement instanceof ExpressionStatement)) {
			return false;
		}

		ExpressionStatement expressionStatement= (ExpressionStatement) statement;
		Expression expression= expressionStatement.getExpression();
		if (expression instanceof Assignment) {
			Assignment assignment= (Assignment) expression;
			Expression leftHandSide= assignment.getLeftHandSide();
			if (leftHandSide instanceof FieldAccess && leftHandSide.getStartPosition() == assignment.getStartPosition() && leftHandSide.getLength() == assignment.getLength()) {
				// "this.fieldname" recovered as "this.fieldname = $missing$"
				expression= leftHandSide;
			}
		}
		ITypeBinding expressionType= null;
		if (expression instanceof MethodInvocation
				|| expression instanceof SimpleName
				|| expression instanceof FieldAccess
				|| expression instanceof QualifiedName) {
			expressionType= expression.resolveTypeBinding();
		} else {
			return false;
		}

		if (expressionType == null)
			return false;

		ICompilationUnit cu= context.getCompilationUnit();
		if (Bindings.findTypeInHierarchy(expressionType, "java.lang.Iterable") != null) { //$NON-NLS-1$
			if (resultingCollections == null)
				return true;
			resultingCollections.add(new GenerateForLoopAssistProposal(cu, expressionStatement, GenerateForLoopAssistProposal.GENERATE_ITERATOR_FOR));
			if (Bindings.findTypeInHierarchy(expressionType, "java.util.List") != null) { //$NON-NLS-1$
				resultingCollections.add(new GenerateForLoopAssistProposal(cu, expressionStatement, GenerateForLoopAssistProposal.GENERATE_ITERATE_LIST));
			}
		} else if (expressionType.isArray()) {
			if (resultingCollections == null)
				return true;
			resultingCollections.add(new GenerateForLoopAssistProposal(cu, expressionStatement, GenerateForLoopAssistProposal.GENERATE_ITERATE_ARRAY));
		} else {
			return false;
		}

		if (JavaModelUtil.is50OrHigher(cu.getJavaProject())) {
			resultingCollections.add(new GenerateForLoopAssistProposal(cu, expressionStatement, GenerateForLoopAssistProposal.GENERATE_FOREACH));
		}

		return true;
	}

	private static ForStatement getEnclosingForStatementHeader(ASTNode node) {
		return getEnclosingHeader(node, ForStatement.class, ForStatement.INITIALIZERS_PROPERTY, ForStatement.EXPRESSION_PROPERTY, ForStatement.UPDATERS_PROPERTY);
	}

	private static <T extends ASTNode> T getEnclosingHeader(ASTNode node, Class<T> headerType, StructuralPropertyDescriptor... headerProperties) {
		if (headerType.isInstance(node))
			return headerType.cast(node);

		while (node != null) {
			ASTNode parent= node.getParent();
			if (headerType.isInstance(parent)) {
				StructuralPropertyDescriptor locationInParent= node.getLocationInParent();
				for (StructuralPropertyDescriptor property : headerProperties) {
					if (locationInParent == property)
						return headerType.cast(parent);
				}
				return null;
			}
			node= parent;
		}
		return null;
	}

	private static boolean getMakeVariableDeclarationFinalProposals(IInvocationContext context, Collection<ICommandAccess> resultingCollections) {
		SelectionAnalyzer analyzer= new SelectionAnalyzer(Selection.createFromStartLength(context.getSelectionOffset(), context.getSelectionLength()), false);
		context.getASTRoot().accept(analyzer);
		ASTNode[] selectedNodes= analyzer.getSelectedNodes();
		if (selectedNodes.length == 0)
			return false;

		IProposableFix fix= VariableDeclarationFixCore.createChangeModifierToFinalFix(context.getASTRoot(), selectedNodes);
		if (fix == null)
			return false;

		if (resultingCollections == null)
			return true;

		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		Map<String, String> options= new Hashtable<>();
		options.put(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL, CleanUpOptions.TRUE);
		options.put(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES, CleanUpOptions.TRUE);
		options.put(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PARAMETERS, CleanUpOptions.TRUE);
		options.put(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS, CleanUpOptions.TRUE);
		VariableDeclarationCleanUp cleanUp= new VariableDeclarationCleanUp(options);
		FixCorrectionProposal proposal= new FixCorrectionProposal(fix, cleanUp, IProposalRelevance.MAKE_VARIABLE_DECLARATION_FINAL, image, context);
		resultingCollections.add(proposal);
		return true;
	}

	private static boolean getInlineLocalProposal(IInvocationContext context, final ASTNode node, Collection<ICommandAccess> proposals) throws CoreException {
		if (!(node instanceof SimpleName))
			return false;

		SimpleName name= (SimpleName) node;
		IBinding binding= name.resolveBinding();
		if (!(binding instanceof IVariableBinding))
			return false;
		IVariableBinding varBinding= (IVariableBinding) binding;
		if (varBinding.isField() || varBinding.isParameter())
			return false;
		ASTNode decl= context.getASTRoot().findDeclaringNode(varBinding);
		if (!(decl instanceof VariableDeclarationFragment) || decl.getLocationInParent() != VariableDeclarationStatement.FRAGMENTS_PROPERTY)
			return false;

		if (proposals == null) {
			return true;
		}

		InlineTempRefactoring refactoring= new InlineTempRefactoring((VariableDeclaration) decl);
		if (refactoring.checkInitialConditions(new NullProgressMonitor()).isOK()) {
			refactoring.setCheckResultForCompileProblems(false);
			String label= CorrectionMessages.QuickAssistProcessor_inline_local_description;
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			RefactoringCorrectionProposal proposal= new RefactoringCorrectionProposal(label, context.getCompilationUnit(), refactoring, IProposalRelevance.INLINE_LOCAL, image);
			proposal.setCommandId(INLINE_LOCAL_ID);
			proposals.add(proposal);

		}
		return true;
	}

	private static boolean getMissingCaseStatementProposals(IInvocationContext context, ASTNode node, Collection<ICommandAccess> proposals) {
		if (node instanceof SwitchCase) {
			node= node.getParent();
		}
		if (!(node instanceof SwitchStatement))
			return false;

		SwitchStatement switchStatement= (SwitchStatement) node;
		ITypeBinding expressionBinding= switchStatement.getExpression().resolveTypeBinding();
		if (expressionBinding == null || !expressionBinding.isEnum())
			return false;

		ArrayList<String> missingEnumCases= new ArrayList<>();
		boolean hasDefault= LocalCorrectionsSubProcessor.evaluateMissingSwitchCases(expressionBinding, switchStatement.statements(), missingEnumCases);
		if (missingEnumCases.isEmpty() && hasDefault)
			return false;

		if (proposals == null)
			return true;

		LocalCorrectionsSubProcessor.createMissingCaseProposals(context, switchStatement, missingEnumCases, proposals);
		return true;
	}

	private static boolean getConvertVarTypeToResolvedTypeProposal(IInvocationContext context, ASTNode node, Collection<ICommandAccess> proposals) {
		CompilationUnit astRoot= context.getASTRoot();
		boolean isValid= ASTNodes.isVarType(node, astRoot);
		if (!isValid) {
			return false;
		}

		if (!(node instanceof SimpleName)) {
			return false;
		}

		SimpleName name= (SimpleName) node;
		IBinding binding= name.resolveBinding();
		if (!(binding instanceof IVariableBinding)) {
			return false;
		}
		IVariableBinding varBinding= (IVariableBinding) binding;
		if (varBinding.isField() || varBinding.isParameter()) {
			return false;
		}

		ITypeBinding typeBinding= varBinding.getType();
		if (typeBinding == null || typeBinding.isAnonymous() || typeBinding.isIntersectionType() || typeBinding.isWildcardType()) {
			return false;
		}

		proposals.add(new TypeChangeCorrectionProposal(context.getCompilationUnit(), varBinding, astRoot, typeBinding, false, IProposalRelevance.CHANGE_TYPE_FROM_VAR));
		return true;
	}

	private static boolean getConvertResolvedTypeToVarTypeProposal(IInvocationContext context, ASTNode node, Collection<ICommandAccess> proposals) {
		CompilationUnit astRoot= context.getASTRoot();
		IJavaElement root= astRoot.getJavaElement();
		if (root == null) {
			return false;
		}
		IJavaProject javaProject= root.getJavaProject();
		if (javaProject == null) {
			return false;
		}
		if (!JavaModelUtil.is10OrHigher(javaProject)) {
			return false;
		}

		if (!(node instanceof SimpleName)) {
			return false;
		}
		SimpleName name= (SimpleName) node;
		IBinding binding= name.resolveBinding();
		if (!(binding instanceof IVariableBinding)) {
			return false;
		}
		IVariableBinding varBinding= (IVariableBinding) binding;
		if (varBinding.isField() || varBinding.isParameter()) {
			return false;
		}

		ASTNode varDeclaration= astRoot.findDeclaringNode(varBinding);
		if (varDeclaration == null) {
			return false;
		}

		Type type= null;
		Expression expression= null;

		ITypeBinding typeBinding= varBinding.getType();
		if (typeBinding == null) {
			return false;
		}
		ITypeBinding expressionTypeBinding= null;

		if (varDeclaration instanceof SingleVariableDeclaration) {
			SingleVariableDeclaration svDecl= (SingleVariableDeclaration) varDeclaration;
			type= svDecl.getType();
			expression= svDecl.getInitializer();
			if (expression != null) {
				expressionTypeBinding= expression.resolveTypeBinding();
			} else {
				ASTNode parent= svDecl.getParent();
				if (parent instanceof EnhancedForStatement) {
					EnhancedForStatement efStmt= (EnhancedForStatement) parent;
					expression= efStmt.getExpression();
					if (expression != null) {
						ITypeBinding expBinding= expression.resolveTypeBinding();
						if (expBinding != null) {
							if (expBinding.isArray()) {
								expressionTypeBinding= expBinding.getElementType();
							} else {
								ITypeBinding iterable= Bindings.findTypeInHierarchy(expBinding, "java.lang.Iterable"); //$NON-NLS-1$
								if (iterable != null) {
									ITypeBinding[] typeArguments= iterable.getTypeArguments();
									if (typeArguments.length == 1) {
										expressionTypeBinding= typeArguments[0];
										expressionTypeBinding= Bindings.normalizeForDeclarationUse(expressionTypeBinding, context.getASTRoot().getAST());
									}
								}
							}
						}
					}
				}
			}
		} else if (varDeclaration instanceof VariableDeclarationFragment) {
			ASTNode parent= varDeclaration.getParent();
			expression= ((VariableDeclarationFragment) varDeclaration).getInitializer();
			if (expression != null) {
				expressionTypeBinding= expression.resolveTypeBinding();
			}
			if (parent instanceof VariableDeclarationStatement) {
				type= ((VariableDeclarationStatement) parent).getType();
			} else if (parent instanceof VariableDeclarationExpression) {
				VariableDeclarationExpression varDecl= (VariableDeclarationExpression) parent;
				// cannot convert a VariableDeclarationExpression with multiple fragments to var.
				if (varDecl.fragments().size() > 1) {
					return false;
				}
				type= varDecl.getType();
			}
		}

		if (type == null || type.isVar()) {
			return false;
		}
		if (expression == null || expression instanceof ArrayInitializer || expression instanceof LambdaExpression || expression instanceof MethodReference) {
			return false;
		}
		if (expressionTypeBinding == null || !expressionTypeBinding.isEqualTo(typeBinding)) {
			return false;
		}

		proposals.add(new TypeChangeCorrectionProposal(context.getCompilationUnit(), varBinding, astRoot, typeBinding, IProposalRelevance.CHANGE_TYPE_TO_VAR));
		return true;
	}

	private static boolean getConvertLocalToFieldProposal(IInvocationContext context, final ASTNode node, Collection<ICommandAccess> proposals) throws CoreException {
		if (!(node instanceof SimpleName))
			return false;

		SimpleName name= (SimpleName) node;
		IBinding binding= name.resolveBinding();
		if (!(binding instanceof IVariableBinding))
			return false;
		IVariableBinding varBinding= (IVariableBinding) binding;
		if (varBinding.isField() || varBinding.isParameter())
			return false;
		ASTNode decl= context.getASTRoot().findDeclaringNode(varBinding);
		if (decl == null || decl.getLocationInParent() != VariableDeclarationStatement.FRAGMENTS_PROPERTY)
			return false;

		if (proposals == null) {
			return true;
		}

		PromoteTempToFieldRefactoring refactoring= new PromoteTempToFieldRefactoring((VariableDeclaration) decl);
		if (refactoring.checkInitialConditions(new NullProgressMonitor()).isOK()) {
			String label= CorrectionMessages.QuickAssistProcessor_convert_local_to_field_description;
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			LinkedProposalModelCore linkedProposalModel= createProposalModel();
			refactoring.setLinkedProposalModel(linkedProposalModel);

			RefactoringCorrectionProposal proposal= new RefactoringCorrectionProposal(label, context.getCompilationUnit(), refactoring, IProposalRelevance.CONVERT_LOCAL_TO_FIELD, image);
			proposal.setLinkedProposalModel(linkedProposalModel);
			proposal.setCommandId(CONVERT_LOCAL_TO_FIELD_ID);
			proposals.add(proposal);
		}
		return true;
	}

	private static LinkedProposalModelCore createProposalModel() {
		return new LinkedProposalModelCore();
	}

	/**
	 * Create static import proposal
	 *
	 * @param context the invocation context
	 * @param node the node to work on
	 * @param proposals the receiver of proposals, may be {@code null}
	 * @return {@code true} if the operation could or has been performed, {@code false otherwise}
	 */
	private static boolean getAddStaticImportProposals(IInvocationContext context, ASTNode node, Collection<ICommandAccess> proposals) {
		if (!(node instanceof SimpleName)) {
			return false;
		}

		final SimpleName name= (SimpleName) node;
		final IBinding binding;
		final ITypeBinding declaringClass;

		// get bindings for method invocation or variable access

		if (name.getParent() instanceof MethodInvocation) {
			MethodInvocation mi= (MethodInvocation) name.getParent();

			Expression expression= mi.getExpression();
			if (expression == null || expression.equals(name)) {
				return false;
			}

			binding= mi.resolveMethodBinding();
			if (binding == null) {
				return false;
			}

			declaringClass= ((IMethodBinding) binding).getDeclaringClass();
		} else if (name.getParent() instanceof QualifiedName) {
			QualifiedName qn= (QualifiedName) name.getParent();

			if (name.equals(qn.getQualifier()) || qn.getParent() instanceof ImportDeclaration) {
				return false;
			}

			binding= qn.resolveBinding();
			if (!(binding instanceof IVariableBinding)) {
				return false;
			}
			declaringClass= ((IVariableBinding) binding).getDeclaringClass();
		} else {
			return false;
		}

		// at this point binding cannot be null

		if (!Modifier.isStatic(binding.getModifiers())) {
			// only work with static bindings
			return false;
		}

		boolean needImport= false;
		if (!isDirectlyAccessible(name, declaringClass)) {
			if (Modifier.isPrivate(declaringClass.getModifiers())) {
				return false;
			}
			needImport= true;
		}

		if (proposals == null) {
			return true; // return early, just testing if we could do it
		}

		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);

		try {
			ImportRewrite importRewrite= StubUtility.createImportRewrite(context.getCompilationUnit(), true);
			ASTRewrite astRewrite= ASTRewrite.create(node.getAST());
			ASTRewrite astRewriteReplaceAllOccurrences= ASTRewrite.create(node.getAST());

			ImportRemover remover= new ImportRemover(context.getCompilationUnit().getJavaProject(), context.getASTRoot());
			ImportRemover removerAllOccurences= new ImportRemover(context.getCompilationUnit().getJavaProject(), context.getASTRoot());
			MethodInvocation mi= null;
			QualifiedName qn= null;
			if (name.getParent() instanceof MethodInvocation) {
				mi= (MethodInvocation) name.getParent();
				// convert the method invocation
				astRewrite.remove(mi.getExpression(), null);
				remover.registerRemovedNode(mi.getExpression());
				removerAllOccurences.registerRemovedNode(mi.getExpression());
				mi.typeArguments().forEach(typeObject -> {
					Type type= (Type) typeObject;
					astRewrite.remove(type, null);
					remover.registerRemovedNode(type);
					removerAllOccurences.registerRemovedNode(type);
				});
			} else if (name.getParent() instanceof QualifiedName) {
				qn= (QualifiedName) name.getParent();
				// convert the field access
				astRewrite.replace(qn, ASTNodeFactory.newName(node.getAST(), name.getFullyQualifiedName()), null);
				remover.registerRemovedNode(qn);
				removerAllOccurences.registerRemovedNode(qn);
			} else {
				return false;
			}

			MethodInvocation miFinal= mi;
			name.getRoot().accept(new ASTVisitor() {
				@Override
				public boolean visit(MethodInvocation methodInvocation) {
					Expression methodInvocationExpression= methodInvocation.getExpression();
					if (methodInvocationExpression == null) {
						return super.visit(methodInvocation);
					}

					if (methodInvocationExpression instanceof Name) {
						String fullyQualifiedName= ((Name) methodInvocationExpression).getFullyQualifiedName();
						if (miFinal != null &&
								miFinal.getExpression() instanceof Name && ((Name) miFinal.getExpression()).getFullyQualifiedName().equals(fullyQualifiedName)
								&& miFinal.getName().getIdentifier().equals(methodInvocation.getName().getIdentifier())) {
							methodInvocation.typeArguments().forEach(type -> {
								astRewriteReplaceAllOccurrences.remove((Type) type, null);
								removerAllOccurences.registerRemovedNode((Type) type);
							});
							astRewriteReplaceAllOccurrences.remove(methodInvocationExpression, null);
							removerAllOccurences.registerRemovedNode(methodInvocationExpression);
						}
					}

					return super.visit(methodInvocation);
				}
			});
			QualifiedName qnFinal= qn;
			name.getRoot().accept(new ASTVisitor() {
				@Override
				public boolean visit(QualifiedName qualifiedName) {
					if (qnFinal != null &&
							qualifiedName.getFullyQualifiedName().equals(qnFinal.getFullyQualifiedName())) {
						astRewriteReplaceAllOccurrences.replace(qualifiedName, ASTNodeFactory.newName(node.getAST(), name.getFullyQualifiedName()), null);
						removerAllOccurences.registerRemovedNode(qualifiedName);
					}
					return super.visit(qualifiedName);
				}
			});

			if (needImport) {
				importRewrite.addStaticImport(binding);
			}
			ASTRewriteRemoveImportsCorrectionProposal proposal= new ASTRewriteRemoveImportsCorrectionProposal(CorrectionMessages.QuickAssistProcessor_convert_to_static_import,
					context.getCompilationUnit(), astRewrite,
					IProposalRelevance.ADD_STATIC_IMPORT, image);
			proposal.setImportRewrite(importRewrite);
			proposal.setImportRemover(remover);
			proposals.add(proposal);
			ASTRewriteRemoveImportsCorrectionProposal proposalReplaceAllOccurrences= new ASTRewriteRemoveImportsCorrectionProposal(
					CorrectionMessages.QuickAssistProcessor_convert_to_static_import_replace_all, context.getCompilationUnit(), astRewriteReplaceAllOccurrences,
					IProposalRelevance.ADD_STATIC_IMPORT, image);
			proposalReplaceAllOccurrences.setImportRewrite(importRewrite);
			proposalReplaceAllOccurrences.setImportRemover(removerAllOccurences);
			proposals.add(proposalReplaceAllOccurrences);
		} catch (IllegalArgumentException e) {
			// Wrong use of ASTRewrite or ImportRewrite API, see bug 541586
			JavaPlugin.log(e);
			return false;
		} catch (JavaModelException e) {
			return false;
		}

		return true;
	}

	private static boolean isDirectlyAccessible(ASTNode nameNode, ITypeBinding declaringClass) {
		ASTNode node= nameNode.getParent();
		while (node != null) {

			if (node instanceof AbstractTypeDeclaration) {
				ITypeBinding binding= ((AbstractTypeDeclaration) node).resolveBinding();
				if (binding != null && binding.isSubTypeCompatible(declaringClass)) {
					return true;
				}
			} else if (node instanceof AnonymousClassDeclaration) {
				ITypeBinding binding= ((AnonymousClassDeclaration) node).resolveBinding();
				if (binding != null && binding.isSubTypeCompatible(declaringClass)) {
					return true;
				}
			}

			node= node.getParent();
		}
		return false;
	}

	private boolean getJUnitTestCaseProposal(IInvocationContext context, ASTNode coveringNode, ArrayList<ICommandAccess> resultingCollections) {
		if (coveringNode instanceof SimpleName && coveringNode.getParent() instanceof AbstractTypeDeclaration) {
			SimpleName name= (SimpleName) coveringNode;
			String idName= name.getIdentifier() + JavaModelUtil.DEFAULT_CU_SUFFIX;
			String unitName= context.getCompilationUnit().getElementName();
			if (unitName.equals(idName)) {
				if (resultingCollections != null) {
					Image image= JavaPlugin.getImageDescriptorRegistry().get(JavaPluginImages.DESC_OBJS_TEST_CASE);
					String label= Messages.format(CorrectionMessages.QuickAssistProcessor_create_new_junit_test_case, unitName);
					Change change= new NullChange(CorrectionMessages.QuickAssistProcessor_create_new_junit_test_case_desc);
					NewJUnitTestCaseProposal proposal= new NewJUnitTestCaseProposal(label, change, IProposalRelevance.CREATE_JUNIT_TEST_CASE, image, context.getASTRoot());
					resultingCollections.add(proposal);
				}
				return true;
			}
		}
		return false;
	}

	private boolean getNewImplementationProposal(IInvocationContext context, ASTNode coveringNode, ArrayList<ICommandAccess> resultingCollections) {
		if (coveringNode instanceof SimpleName && coveringNode.getParent() instanceof TypeDeclaration) {
			TypeDeclaration typeDecl= ((TypeDeclaration) coveringNode.getParent());
			boolean isInterface= typeDecl.isInterface();
			boolean isAbstract= Modifier.isAbstract(typeDecl.getModifiers());

			if (!isInterface && !isAbstract) {
				return false;
			}

			SimpleName name= (SimpleName) coveringNode;
			String idName= name.getIdentifier() + JavaModelUtil.DEFAULT_CU_SUFFIX;
			String unitName= context.getCompilationUnit().getElementName();
			if (unitName.equals(idName)) {
				if (resultingCollections != null) {
					Image image= JavaPlugin.getImageDescriptorRegistry().get(JavaPluginImages.DESC_TOOL_NEWCLASS);
					String label= Messages.format(CorrectionMessages.QuickAssistProcessor_create_new_impl, unitName);
					Change change= new NullChange(CorrectionMessages.QuickAssistProcessor_create_new_impl_desc);
					NewImplementationProposal proposal= new NewImplementationProposal(label, change, IProposalRelevance.CREATE_IMPLEMENTATION_FROM_INTERFACE, image, context.getCompilationUnit());
					resultingCollections.add(proposal);
				}
				return true;
			}
		}
		return false;
	}

	private boolean getNewInterfaceImplementationProposal(IInvocationContext context, ASTNode coveringNode, ArrayList<ICommandAccess> resultingCollections) {
		if (coveringNode instanceof SimpleName && coveringNode.getParent() instanceof TypeDeclaration) {
			TypeDeclaration typeDecl= ((TypeDeclaration) coveringNode.getParent());
			boolean isInterface= typeDecl.isInterface();

			if (!isInterface) {
				return false;
			}

			SimpleName name= (SimpleName) coveringNode;
			String idName= name.getIdentifier() + JavaModelUtil.DEFAULT_CU_SUFFIX;
			String unitName= context.getCompilationUnit().getElementName();
			if (unitName.equals(idName)) {
				if (resultingCollections != null) {
					Image image= JavaPlugin.getImageDescriptorRegistry().get(JavaPluginImages.DESC_TOOL_NEWCLASS);
					String label= Messages.format(CorrectionMessages.QuickAssistProcessor_create_new_interface_impl, unitName);
					Change change= new NullChange(CorrectionMessages.QuickAssistProcessor_create_new_interface_impl_desc);
					NewInterfaceImplementationProposal proposal= new NewInterfaceImplementationProposal(label, change, IProposalRelevance.CREATE_IMPLEMENTATION_FROM_INTERFACE, image,
							context.getCompilationUnit());
					resultingCollections.add(proposal);
				}
				return true;
			}
		}
		return false;
	}

	private boolean getSplitSwitchLabelProposal(IInvocationContext context, ASTNode coveringNode, Collection<ICommandAccess> proposals) {
		AST ast= coveringNode.getAST();
		// Only continue if AST has preview enabled and selected node, or its parent is a SwitchCase
		if (!ASTHelper.isSwitchCaseExpressionsSupportedInAST(ast) ||
				(!(coveringNode instanceof SwitchCase) && !(coveringNode.getParent() instanceof SwitchCase))) {
			return false;
		}

		SwitchCase scase= null;
		ASTNode parent= null;

		// If selected node not a SwitchCase, its parent must be a SwitchCase
		if (coveringNode instanceof SwitchCase) {
			scase= (SwitchCase) coveringNode;
			parent= coveringNode.getParent();
		} else {
			scase= (SwitchCase) coveringNode.getParent();
			parent= coveringNode.getParent().getParent();
		}

		if (proposals != null && scase.expressions().size() > 1) {
			ASTRewrite astRewrite= ASTRewrite.create(ast);
			ChildListPropertyDescriptor descriptor;
			List<Statement> statements;
			if (parent instanceof SwitchStatement) {
				descriptor= SwitchStatement.STATEMENTS_PROPERTY;
				statements= ((SwitchStatement) parent).statements();
			} else {
				descriptor= SwitchExpression.STATEMENTS_PROPERTY;
				statements= ((SwitchExpression) parent).statements();
			}
			ListRewrite listRewrite= astRewrite.getListRewrite(parent, descriptor);

			// Figure out the list index of the switch case in the statement list
			// We care about duplicating the statement(s) occuring immediately after it
			int statementIndex= 0;
			for (Statement s : statements) {
				if (scase.equals(s)) {
					break;
				}
				statementIndex++;
			}
			statementIndex++;

			// Switch Case Statement(s)
			List<Statement> caseStatements= new ArrayList<>();
			for (int i= statementIndex; i < statements.size(); i++) {
				Statement curr= statements.get(i);
				if (curr instanceof SwitchCase) {
					break;
				}
				caseStatements.add(curr);
			}

			for (int i= 0; i < scase.expressions().size(); i++) {
				Expression elem= (Expression) scase.expressions().get(i);
				// SwitchCase
				SwitchCase newSwitchCase= ast.newSwitchCase();
				Expression newExpr= (Expression) astRewrite.createCopyTarget(elem);
				newSwitchCase.setSwitchLabeledRule(scase.isSwitchLabeledRule());
				newSwitchCase.expressions().add(newExpr);

				// Preserve order from left -> right, top -> bottom
				listRewrite.insertBefore(newSwitchCase, scase, null);
				for (Statement statement : caseStatements) {
					listRewrite.insertBefore(astRewrite.createCopyTarget(statement), scase, null);
				}
			}

			listRewrite.remove(scase, null);
			for (Statement statement : caseStatements) {
				listRewrite.remove(statement, null);
			}

			String label= CorrectionMessages.QuickAssistProcessor_split_case_labels;
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			proposals.add(new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), astRewrite, IProposalRelevance.ADD_MISSING_CASE_STATEMENTS, image));
		}

		return true;
	}

	private static boolean getSplitTryResourceProposal(IInvocationContext context, ASTNode coveringNode, Collection<ICommandAccess> proposals) {
		if (proposals == null) {
			return SplitTryResourceFixCore.initialConditionsCheck(context.getCompilationUnit(), coveringNode);
		}
		SplitTryResourceFixCore fix= SplitTryResourceFixCore.createSplitVariableFix(context.getASTRoot(), coveringNode);
		if (fix != null) {
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			FixCorrectionProposal proposal= new FixCorrectionProposal(fix, null, IProposalRelevance.INVERT_EQUALS, image, context);
			proposals.add(proposal);
			return true;
		}
		return false;
	}

	private boolean getConvertFieldNamingConventionProposal(IInvocationContext context, ASTNode node, Collection<ICommandAccess> resultingCollections) {
		if (!(node instanceof SimpleName)) {
			return false;
		}
		SimpleName simpleName= (SimpleName) node;
		String selectedField= simpleName.toString();
		int offset= node.getStartPosition();
		if (simpleName.getAST().apiLevel() >= ASTHelper.JLS10 && simpleName.isVar()) {
			return false;
		}

		IEditorPart editor= ((AssistContext) context).getEditor();
		if (!(editor instanceof JavaEditor))
			return false;

		if (!(simpleName.resolveBinding() instanceof IVariableBinding binding) || !binding.isField()) {
			return false;
		}

		CompilationUnit cu= context.getASTRoot();
		ASTNode decl= ASTNodes.findDeclaration(binding, cu);
		if (!(decl instanceof VariableDeclarationFragment vdf)) {
			return false;
		}
		FieldDeclaration fd= (FieldDeclaration) vdf.getParent();
		int modifier= fd.getModifiers();
		if (!Flags.isStatic(modifier) || !Flags.isFinal(modifier)) {
			return false;
		}
		if (isValidConstantName(selectedField)) {
			return false;
		}

		String newName= convertToConstantName(selectedField);
		AbstractTypeDeclaration typeDecl= (AbstractTypeDeclaration) fd.getParent();
		if (typeDecl == null) {
			return false;
		}
		FieldDeclarationChecker fdChecker= new FieldDeclarationChecker();
		typeDecl.accept(fdChecker);
		if (fdChecker.getFieldDeclarationList().contains(newName)) {
			return false;
		}

		if (resultingCollections == null) {
			return true;
		}

		IField iField= (IField) binding.getJavaElement();
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_RENAME);
		ConvertFieldNamingConventionProposal proposal= new ConvertFieldNamingConventionProposal(newName, offset, offset, image, iField);
		proposal.setCommandId(FIELD_NAMING_CONVENTION_ID);
		resultingCollections.add(proposal);
		return true;
	}

	private boolean isValidConstantName(String identifier) {
		Pattern pattern= FIELD_NAMING_PATTERN;
		Matcher matcher= pattern.matcher(identifier);
		return matcher.matches();
	}

	public static String convertToConstantName(String identifier) {
		StringBuilder constantNaming= new StringBuilder();
		boolean lastCharWasUpperCase= false;
		for (char c : identifier.toCharArray()) {
			if (Character.isUpperCase(c)) {
				if (!lastCharWasUpperCase && constantNaming.length() > 0) {
					constantNaming.append("_"); //$NON-NLS-1$
				}
				constantNaming.append(Character.toUpperCase(c));
				lastCharWasUpperCase= true;
			} else {
				constantNaming.append(Character.toUpperCase(c));
				lastCharWasUpperCase= false;
			}
		}
		return constantNaming.toString().replaceAll("_{2,}", "_"); //$NON-NLS-1$ //$NON-NLS-2$
	}
}

final class FieldDeclarationChecker extends ASTVisitor {
	private final List<String> fieldDeclarationList= new ArrayList<>();

	@Override
	public boolean visit(FieldDeclaration fieldDecl) {
		for (Object fragment : fieldDecl.fragments()) {
			if (fragment instanceof VariableDeclarationFragment) {
				VariableDeclarationFragment varDecl= (VariableDeclarationFragment) fragment;
				fieldDeclarationList.add(varDecl.getName().toString());
			}
		}
		return true;
	}

	public List<String> getFieldDeclarationList() {
		return fieldDeclarationList;
	}
}
