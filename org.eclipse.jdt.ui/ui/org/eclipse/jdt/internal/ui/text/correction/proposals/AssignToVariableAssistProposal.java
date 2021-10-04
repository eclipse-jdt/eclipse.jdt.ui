/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
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
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.text.correction.proposals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.eclipse.core.resources.ProjectScope;

import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.NamingConventions;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EmptyStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.UnionType;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.TypeLocation;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.core.manipulation.dom.ASTResolving;
import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.codemanipulation.ContextSensitiveImportRewriteContext;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.CodeScopeBuilder;
import org.eclipse.jdt.internal.corext.dom.LinkedNodeFinder;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.dom.TokenScanner;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CleanUpPostSaveListener;
import org.eclipse.jdt.internal.corext.fix.CleanUpPreferenceUtil;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalModel;
import org.eclipse.jdt.internal.corext.refactoring.surround.ExceptionAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.surround.SurroundWithTryWithResourcesAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.surround.SurroundWithTryWithResourcesRefactoringCore;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.cleanup.CleanUpOptions;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.javaeditor.saveparticipant.AbstractSaveParticipantPreferenceConfiguration;
import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;
import org.eclipse.jdt.internal.ui.text.correction.ModifierCorrectionSubProcessorCore;
import org.eclipse.jdt.internal.ui.viewsupport.BindingLabelProvider;

/**
 * Proposals for 'Assign to variable' quick assist
 * - Assign an expression from an ExpressionStatement to a local or field
 * - Assign single or all parameter(s) to field(s)
 * */
public class AssignToVariableAssistProposal extends LinkedCorrectionProposal {

	public static final int LOCAL= 1;
	public static final int FIELD= 2;
	public static final int TRY_WITH_RESOURCES= 3;

	private final String KEY_NAME= "name";  //$NON-NLS-1$
	private final String KEY_TYPE= "type";  //$NON-NLS-1$
	private final String GROUP_EXC_TYPE= "exc_type"; //$NON-NLS-1$
	private final String GROUP_EXC_NAME= "exc_name"; //$NON-NLS-1$
	private final String VAR_TYPE= "var";  //$NON-NLS-1$

	private final int  fVariableKind;
	private final List<ASTNode> fNodesToAssign; // ExpressionStatement or SingleVariableDeclaration(s)
	private final ITypeBinding fTypeBinding;
	private final ICompilationUnit fCUnit;
	private final List<String> fParamNames;

	private VariableDeclarationFragment fExistingFragment;

	public AssignToVariableAssistProposal(ICompilationUnit cu, int variableKind, ExpressionStatement node, ITypeBinding typeBinding, int relevance) {
		super("", cu, null, relevance, null); //$NON-NLS-1$

		fCUnit= cu;
		fVariableKind= variableKind;
		fParamNames = null;
		fNodesToAssign= new ArrayList<>();
		fNodesToAssign.add(node);

		fTypeBinding= Bindings.normalizeForDeclarationUse(typeBinding, node.getAST());
		if (variableKind == LOCAL) {
			setDisplayName(CorrectionMessages.AssignToVariableAssistProposal_assigntolocal_description);
			setImage(JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL));
		} else if (variableKind == FIELD) {
			setDisplayName(CorrectionMessages.AssignToVariableAssistProposal_assigntofield_description);
			setImage(JavaPluginImages.get(JavaPluginImages.IMG_FIELD_PRIVATE));
		} else {
			setDisplayName(CorrectionMessages.AssignToVariableAssistProposal_assignintrywithresources_description);
			setImage(JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE));
		}
		createImportRewrite((CompilationUnit) node.getRoot());
	}

	public AssignToVariableAssistProposal(ICompilationUnit cu, SingleVariableDeclaration parameter, VariableDeclarationFragment existingFragment, ITypeBinding typeBinding, int relevance) {
		super("", cu, null, relevance, null); //$NON-NLS-1$

		fCUnit= cu;
		fVariableKind= FIELD;
		fNodesToAssign= new ArrayList<>();
		fNodesToAssign.add(parameter);
		fParamNames= null;
		fTypeBinding= typeBinding;
		fExistingFragment= existingFragment;

		if (existingFragment == null) {
			setDisplayName(CorrectionMessages.AssignToVariableAssistProposal_assignparamtofield_description);
		} else {
			setDisplayName(Messages.format(CorrectionMessages.AssignToVariableAssistProposal_assigntoexistingfield_description, BasicElementLabels.getJavaElementName(existingFragment.getName().getIdentifier())));
		}
		setImage(JavaPluginImages.get(JavaPluginImages.IMG_FIELD_PRIVATE));
	}

	public AssignToVariableAssistProposal(ICompilationUnit cu, List<SingleVariableDeclaration> parameters, int relevance) {
		super("", cu, null, relevance, null); //$NON-NLS-1$

		fCUnit= cu;
		fVariableKind= FIELD;
		fNodesToAssign= new ArrayList<>();
		fNodesToAssign.addAll(parameters);
		fTypeBinding= null;
		fParamNames= new ArrayList<>();
		populateNames(parameters);
		setDisplayName(CorrectionMessages.AssignToVariableAssistProposal_assignallparamstofields_description);
		setImage(JavaPluginImages.get(JavaPluginImages.IMG_FIELD_PRIVATE));
	}

	@Override
	protected ASTRewrite getRewrite() throws CoreException {
		if (fVariableKind == FIELD) {
			ASTRewrite rewrite= ASTRewrite.create(fNodesToAssign.get(0).getAST());
			if (fNodesToAssign.size() == 1) {
				return doAddField(rewrite, fNodesToAssign.get(0), fTypeBinding, 0);
			} else {
				return doAddAllFields(rewrite);
			}
		} else { // LOCAL or TRY_WITH_RESOURCES
			return doAddLocal();
		}
	}

	private void populateNames(List<SingleVariableDeclaration> parameters) {
		if (parameters != null && parameters.size() > 0) {
			for (SingleVariableDeclaration param : parameters) {
				if (param.getName() != null) {
					fParamNames.add(param.getName().getIdentifier());
				}
			}
		}
	}

	private ASTRewrite doAddLocal() throws CoreException {
		ASTNode nodeToAssign= fNodesToAssign.get(0);
		Expression expression= ((ExpressionStatement) nodeToAssign).getExpression();
		AST ast= nodeToAssign.getAST();

		ASTRewrite rewrite= ASTRewrite.create(ast);

		ImportRewrite importRewrite= createImportRewrite((CompilationUnit) nodeToAssign.getRoot());

		String[] varNames= suggestLocalVariableNames(fTypeBinding, expression);
		for (String varName : varNames) {
			addLinkedPositionProposal(KEY_NAME, varName, null);
		}

		VariableDeclarationFragment newDeclFrag= ast.newVariableDeclarationFragment();
		newDeclFrag.setName(ast.newSimpleName(varNames[0]));
		newDeclFrag.setInitializer((Expression) rewrite.createCopyTarget(expression));

		Type type= evaluateType(ast, nodeToAssign, fTypeBinding, KEY_TYPE, TypeLocation.LOCAL_VARIABLE);

		ICompilationUnit cu= getCompilationUnit();
		if (type != null && cu != null && JavaModelUtil.is10OrHigher(cu.getJavaProject())) {
			ImageDescriptor desc= BindingLabelProvider.getBindingImageDescriptor(fTypeBinding, BindingLabelProvider.DEFAULT_IMAGEFLAGS);
			addLinkedPositionProposal(KEY_TYPE, VAR_TYPE, (desc != null) ? JavaPlugin.getImageDescriptorRegistry().get(desc) : null);
		}

		if (fVariableKind == LOCAL) {
			if (ASTNodes.isControlStatementBody(nodeToAssign.getLocationInParent())) {
				Block block= ast.newBlock();
				block.statements().add(rewrite.createMoveTarget(nodeToAssign));
				rewrite.replace(nodeToAssign, block, null);
			}

			if (needsSemicolon(expression)) {
				VariableDeclarationStatement varStatement= ast.newVariableDeclarationStatement(newDeclFrag);
				varStatement.setType(type);
				rewrite.replace(expression, varStatement, null);
			} else {
				// trick for bug 43248: use an VariableDeclarationExpression and keep the ExpressionStatement
				VariableDeclarationExpression varExpression= ast.newVariableDeclarationExpression(newDeclFrag);
				varExpression.setType(type);
				rewrite.replace(expression, varExpression, null);
			}
			setEndPosition(rewrite.track(nodeToAssign)); // set cursor after expression statement
		} else {
			TryStatement tryStatement= null;
			boolean modifyExistingTry= false;
			TryStatement enclosingTry= (TryStatement)ASTResolving.findAncestor(nodeToAssign, ASTNode.TRY_STATEMENT);
			ListRewrite resourcesRewriter= null;
			ListRewrite clausesRewriter= null;
			if (enclosingTry == null || enclosingTry.getBody() == null || enclosingTry.getBody().statements().get(0) != nodeToAssign) {
				tryStatement= ast.newTryStatement();
			} else {
				modifyExistingTry= true;
				resourcesRewriter= rewrite.getListRewrite(enclosingTry, TryStatement.RESOURCES2_PROPERTY);
				clausesRewriter= rewrite.getListRewrite(enclosingTry, TryStatement.CATCH_CLAUSES_PROPERTY);
			}

			VariableDeclarationExpression varExpression= ast.newVariableDeclarationExpression(newDeclFrag);
			varExpression.setType(type);
			EmptyStatement blankLine= null;
			if (modifyExistingTry) {
				resourcesRewriter.insertLast(varExpression, null);
			} else {
				tryStatement.resources().add(varExpression);
				blankLine = (EmptyStatement) rewrite.createStringPlaceholder("", ASTNode.EMPTY_STATEMENT); //$NON-NLS-1$
				tryStatement.getBody().statements().add(blankLine);
			}


			CatchClause catchClause= ast.newCatchClause();
			SingleVariableDeclaration decl= ast.newSingleVariableDeclaration();
			Selection selection= Selection.createFromStartLength(expression.getStartPosition(), expression.getLength());
			String varName= StubUtility.getExceptionVariableName(fCUnit.getJavaProject());
			SurroundWithTryWithResourcesAnalyzer analyzer= new SurroundWithTryWithResourcesAnalyzer(fCUnit, selection);
			expression.getRoot().accept(analyzer);
			CodeScopeBuilder.Scope scope= CodeScopeBuilder.perform(analyzer.getEnclosingBodyDeclaration(), selection).
					findScope(selection.getOffset(), selection.getLength());
			scope.setCursor(selection.getOffset());
			String name= scope.createName(varName, false);
			decl.setName(ast.newSimpleName(name));
			ITypeBinding[] exceptions= 	ExceptionAnalyzer.perform(expression.getParent(), selection, false);
			List<ITypeBinding> allExceptions= new ArrayList<>(Arrays.asList(exceptions));
			List<ITypeBinding> mustRethrowList= new ArrayList<>();

			if (fTypeBinding != null) {
				IMethodBinding close= SurroundWithTryWithResourcesRefactoringCore.findAutocloseMethod(fTypeBinding);
				if (close != null) {
					for (ITypeBinding exceptionType : close.getExceptionTypes()) {
						if (!allExceptions.contains(exceptionType)) {
							allExceptions.add(exceptionType);
						}
					}
				}
			}
			List<ITypeBinding> catchExceptions= analyzer.calculateCatchesAndRethrows(ASTNodes.filterSubtypes(allExceptions), mustRethrowList);
			if (catchExceptions.size() > 0) {
				ImportRewriteContext context= new ContextSensitiveImportRewriteContext(analyzer.getEnclosingBodyDeclaration(), importRewrite);
				LinkedProposalModel linkedProposalModel= new LinkedProposalModel();
				int i= 0;
				if (!modifyExistingTry) {
					for (ITypeBinding mustThrow : mustRethrowList) {
						CatchClause newClause= ast.newCatchClause();
						SingleVariableDeclaration newDecl= ast.newSingleVariableDeclaration();
						newDecl.setName(ast.newSimpleName(name));
						Type importType= importRewrite.addImport(mustThrow, ast, context, TypeLocation.EXCEPTION);
						newDecl.setType(importType);
						newClause.setException(newDecl);
						ThrowStatement newThrowStatement= ast.newThrowStatement();
						newThrowStatement.setExpression(ast.newSimpleName(name));
						linkedProposalModel.getPositionGroup(GROUP_EXC_NAME + i, true).addPosition(rewrite.track(decl.getName()), false);
						newClause.getBody().statements().add(newThrowStatement);
						tryStatement.catchClauses().add(newClause);
						++i;
					}
				}
				List<ITypeBinding> filteredExceptions= ASTNodes.filterSubtypes(catchExceptions);
				UnionType unionType= ast.newUnionType();
				List<Type> types= unionType.types();
				for (ITypeBinding exception : filteredExceptions) {
					Type importType= importRewrite.addImport(exception, ast, context, TypeLocation.EXCEPTION);
					types.add(importType);
					linkedProposalModel.getPositionGroup(GROUP_EXC_TYPE + i, true).addPosition(rewrite.track(type), i == 0);
					i++;
				}
				decl.setType(unionType);
				catchClause.setException(decl);
				linkedProposalModel.getPositionGroup(GROUP_EXC_NAME + 0, true).addPosition(rewrite.track(decl.getName()), false);
				Statement st= getCatchBody(rewrite, expression, "Exception", name, fCUnit.findRecommendedLineSeparator()); //$NON-NLS-1$
				if (st != null) {
					catchClause.getBody().statements().add(st);
				}
				if (modifyExistingTry) {
					clausesRewriter.insertLast(catchClause, null);
				} else {
					tryStatement.catchClauses().add(catchClause);
				}
			}
			if (modifyExistingTry) {
				rewrite.remove(nodeToAssign, null);
			} else {
				rewrite.replace(expression, tryStatement, null);
				setEndPosition(rewrite.track(blankLine));
			}

		}

		addLinkedPosition(rewrite.track(newDeclFrag.getName()), true, KEY_NAME);
		addLinkedPosition(rewrite.track(type), false, KEY_TYPE);

		return rewrite;
	}

	private Statement getCatchBody(ASTRewrite rewrite, Expression expression, String type, String name, String lineSeparator) throws CoreException {
		String s= StubUtility.getCatchBodyContent(fCUnit, type, name, expression, lineSeparator);
		if (s == null) {
			return null;
		} else {
			return (Statement)rewrite.createStringPlaceholder(s, ASTNode.RETURN_STATEMENT);
		}
	}

	private boolean needsSemicolon(Expression expression) {
		if ((expression.getParent().getFlags() & ASTNode.RECOVERED) != 0) {
			try {
				TokenScanner scanner= new TokenScanner(getCompilationUnit());
				return scanner.readNext(expression.getStartPosition() + expression.getLength(), true) != ITerminalSymbols.TokenNameSEMICOLON;
			} catch (CoreException e) {
				// ignore
			}
		}
		return false;
	}

	private ASTRewrite doAddField(ASTRewrite rewrite, ASTNode nodeToAssign, ITypeBinding typeBinding, int index) {
		boolean isParamToField= nodeToAssign.getNodeType() == ASTNode.SINGLE_VARIABLE_DECLARATION;

		ASTNode newTypeDecl= ASTResolving.findParentType(nodeToAssign);
		if (newTypeDecl == null) {
			return null;
		}

		Expression expression= isParamToField ? ((SingleVariableDeclaration) nodeToAssign).getName() : ((ExpressionStatement) nodeToAssign).getExpression();

		AST ast= newTypeDecl.getAST();

		createImportRewrite((CompilationUnit) nodeToAssign.getRoot());

		BodyDeclaration bodyDecl= ASTResolving.findParentBodyDeclaration(nodeToAssign);
		Block body;
		if (bodyDecl instanceof MethodDeclaration) {
			body= ((MethodDeclaration) bodyDecl).getBody();
		} else if (bodyDecl instanceof Initializer) {
			body= ((Initializer) bodyDecl).getBody();
		} else {
			return null;
		}

		IJavaProject project= getCompilationUnit().getJavaProject();
		boolean isAnonymous= newTypeDecl.getNodeType() == ASTNode.ANONYMOUS_CLASS_DECLARATION;
		boolean isStatic= Modifier.isStatic(bodyDecl.getModifiers()) && !isAnonymous;
		boolean isConstructorParam= isParamToField && nodeToAssign.getParent() instanceof MethodDeclaration && ((MethodDeclaration) nodeToAssign.getParent()).isConstructor();
		int modifiers= Modifier.PRIVATE;
		if (isStatic) {
			modifiers |= Modifier.STATIC;
		} else if (isConstructorParam) {
			String saveActionsKey= AbstractSaveParticipantPreferenceConfiguration.EDITOR_SAVE_PARTICIPANT_PREFIX + CleanUpPostSaveListener.POSTSAVELISTENER_ID;
			IScopeContext[] scopes= { InstanceScope.INSTANCE, new ProjectScope(project.getProject()) };
			boolean safeActionsEnabled= Platform.getPreferencesService().getBoolean(JavaPlugin.getPluginId(), saveActionsKey, false, scopes);
			if (safeActionsEnabled
					&& CleanUpOptions.TRUE.equals(PreferenceConstants.getPreference(CleanUpPreferenceUtil.SAVE_PARTICIPANT_KEY_PREFIX + CleanUpConstants.CLEANUP_ON_SAVE_ADDITIONAL_OPTIONS, project))
					&& CleanUpOptions.TRUE.equals(PreferenceConstants.getPreference(CleanUpPreferenceUtil.SAVE_PARTICIPANT_KEY_PREFIX + CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL, project))
					&& CleanUpOptions.TRUE.equals(PreferenceConstants.getPreference(CleanUpPreferenceUtil.SAVE_PARTICIPANT_KEY_PREFIX + CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS, project))
					) {
				int constructors= 0;
				if (newTypeDecl instanceof AbstractTypeDeclaration) {
					List<BodyDeclaration> bodyDeclarations= ((AbstractTypeDeclaration) newTypeDecl).bodyDeclarations();
					for (BodyDeclaration decl : bodyDeclarations) {
						if (decl instanceof MethodDeclaration && ((MethodDeclaration) decl).isConstructor()) {
							constructors++;
						}
					}
				}
				if (constructors == 1) {
					modifiers |= Modifier.FINAL;
				}
			}
		}

		VariableDeclarationFragment newDeclFrag= addFieldDeclaration(rewrite, newTypeDecl, modifiers, expression, nodeToAssign, typeBinding, index);
		String varName= newDeclFrag.getName().getIdentifier();

		Assignment assignment= ast.newAssignment();
		assignment.setRightHandSide((Expression) rewrite.createCopyTarget(expression));

		boolean needsThis= StubUtility.useThisForFieldAccess(project);
		if (isParamToField) {
			needsThis |= varName.equals(((SimpleName) expression).getIdentifier());
		}

		SimpleName accessName= ast.newSimpleName(varName);
		if (needsThis) {
			FieldAccess fieldAccess= ast.newFieldAccess();
			fieldAccess.setName(accessName);
			if (isStatic) {
				String typeName= ((AbstractTypeDeclaration) newTypeDecl).getName().getIdentifier();
				fieldAccess.setExpression(ast.newSimpleName(typeName));
			} else {
				fieldAccess.setExpression(ast.newThisExpression());
			}
			assignment.setLeftHandSide(fieldAccess);
		} else {
			assignment.setLeftHandSide(accessName);
		}

		ASTNode selectionNode;
		if (isParamToField) {
			// assign parameter to field
			ExpressionStatement statement= ast.newExpressionStatement(assignment);
			int insertIdx= findAssignmentInsertIndex(body.statements(), nodeToAssign) + index;
			rewrite.getListRewrite(body, Block.STATEMENTS_PROPERTY).insertAt(statement, insertIdx, null);
			selectionNode= statement;
		} else {
			if (needsSemicolon(expression)) {
				rewrite.replace(expression, ast.newExpressionStatement(assignment), null);
			} else {
				rewrite.replace(expression, assignment, null);
			}
			selectionNode= nodeToAssign;
		}

		addLinkedPosition(rewrite.track(newDeclFrag.getName()), false, KEY_NAME + index);
		if (!isParamToField) {
			FieldDeclaration fieldDeclaration= (FieldDeclaration) newDeclFrag.getParent();
			addLinkedPosition(rewrite.track(fieldDeclaration.getType()), false, KEY_TYPE);
		}
		addLinkedPosition(rewrite.track(accessName), true, KEY_NAME + index);
		IVariableBinding variableBinding= newDeclFrag.resolveBinding();
		if (variableBinding != null) {
			for (SimpleName linkedNode : LinkedNodeFinder.findByBinding(nodeToAssign.getRoot(), variableBinding)) {
				addLinkedPosition(rewrite.track(linkedNode), false, KEY_NAME + index);
			}
		}
		setEndPosition(rewrite.track(selectionNode));

		return rewrite;
	}

	private ASTRewrite doAddAllFields(ASTRewrite rewrite) {
		for (int i= 0; rewrite != null && i < fNodesToAssign.size(); i++) {
			ASTNode nodeToAssign= fNodesToAssign.get(i);
			ITypeBinding typeBinding= ((SingleVariableDeclaration) nodeToAssign).resolveBinding().getType();
			rewrite= doAddField(rewrite, nodeToAssign, typeBinding, i);
		}
		return rewrite;
	}

	private VariableDeclarationFragment addFieldDeclaration(ASTRewrite rewrite, ASTNode newTypeDecl, int modifiers, Expression expression, ASTNode nodeToAssign, ITypeBinding typeBinding,
			int index) {
		if (fExistingFragment != null) {
			return fExistingFragment;
		}

		ChildListPropertyDescriptor property= ASTNodes.getBodyDeclarationsProperty(newTypeDecl);
		List<BodyDeclaration> decls= ASTNodes.getBodyDeclarations(newTypeDecl);
		AST ast= newTypeDecl.getAST();
		String[] varNames= suggestFieldNames(typeBinding, expression, modifiers, nodeToAssign);
		for (String varName : varNames) {
			addLinkedPositionProposal(KEY_NAME + index, varName, null);
		}
		String varName= varNames[0];

		VariableDeclarationFragment newDeclFrag= ast.newVariableDeclarationFragment();
		newDeclFrag.setName(ast.newSimpleName(varName));

		FieldDeclaration newDecl= ast.newFieldDeclaration(newDeclFrag);

		Type type= evaluateType(ast, nodeToAssign, typeBinding, KEY_TYPE + index, TypeLocation.FIELD);
		newDecl.setType(type);
		newDecl.modifiers().addAll(ASTNodeFactory.newModifiers(ast, modifiers));

		ModifierCorrectionSubProcessorCore.installLinkedVisibilityProposals(getLinkedProposalModel(), rewrite, newDecl.modifiers(), false, ModifierCorrectionSubProcessorCore.KEY_MODIFIER + index);

		int insertIndex= findFieldInsertIndex(decls, nodeToAssign.getStartPosition()) + index;
		rewrite.getListRewrite(newTypeDecl, property).insertAt(newDecl, insertIndex, null);

		return newDeclFrag;
	}

	private Type evaluateType(AST ast, ASTNode nodeToAssign, ITypeBinding typeBinding, String groupID, TypeLocation location) {
		for (ITypeBinding proposal : ASTResolving.getRelaxingTypes(ast, typeBinding)) {
			if (fVariableKind != TRY_WITH_RESOURCES || Bindings.findTypeInHierarchy(proposal, "java.lang.AutoCloseable") != null) { //$NON-NLS-1$
				addLinkedPositionProposal(groupID, proposal);
			}
		}
		ImportRewrite importRewrite= getImportRewrite();
		CompilationUnit cuNode= (CompilationUnit) nodeToAssign.getRoot();
		ImportRewriteContext context= new ContextSensitiveImportRewriteContext(cuNode, nodeToAssign.getStartPosition(), importRewrite);
		return importRewrite.addImport(typeBinding, ast, context, location);
	}

	private String[] suggestLocalVariableNames(ITypeBinding binding, Expression expression) {
		IJavaProject project= getCompilationUnit().getJavaProject();
		return StubUtility.getVariableNameSuggestions(NamingConventions.VK_LOCAL, project, binding, expression, getUsedVariableNames(fNodesToAssign.get(0)));
	}

	private String[] suggestFieldNames(ITypeBinding binding, Expression expression, int modifiers, ASTNode nodeToAssign) {
		IJavaProject project= getCompilationUnit().getJavaProject();
		int varKind= Modifier.isStatic(modifiers) ? NamingConventions.VK_STATIC_FIELD : NamingConventions.VK_INSTANCE_FIELD;
		return StubUtility.getVariableNameSuggestions(varKind, project, binding, expression, getUsedVariableNames(nodeToAssign));
	}

	private Collection<String> getUsedVariableNames(ASTNode nodeToAssign) {
		Collection<String> usedVarNames = Arrays.asList(ASTResolving.getUsedVariableNames(nodeToAssign));
		Collection<String> additionalVarNames= getRemainingParamNamed(nodeToAssign);
		if (additionalVarNames != null) {
			usedVarNames = new ArrayList<>(Arrays.asList(ASTResolving.getUsedVariableNames(nodeToAssign)));
			usedVarNames.addAll(additionalVarNames);
		}
		return usedVarNames;
	}

	private ArrayList<String> getRemainingParamNamed(ASTNode nodeToAssign) {
		ArrayList<String> paramNames = null;
		if (fParamNames != null) {
			paramNames = new ArrayList<>();
			paramNames.addAll(fParamNames);
			if (nodeToAssign instanceof SingleVariableDeclaration
					&& ((SingleVariableDeclaration)nodeToAssign).getName() != null) {
				int index= fNodesToAssign.indexOf(nodeToAssign);
				if (index >= 0 && index < paramNames.size()) {
					paramNames.remove(index);
				}
			}
		}
		return paramNames;
	}

	private int findAssignmentInsertIndex(List<Statement> statements, ASTNode nodeToAssign) {

		HashSet<String> paramsBefore= new HashSet<>();
		List<SingleVariableDeclaration> params = ((MethodDeclaration) nodeToAssign.getParent()).parameters();
		for (int i = 0; i < params.size() && (params.get(i) != nodeToAssign); i++) {
			SingleVariableDeclaration decl= params.get(i);
			paramsBefore.add(decl.getName().getIdentifier());
		}

		int i= 0;
		for (i = 0; i < statements.size(); i++) {
			Statement curr= statements.get(i);
			switch (curr.getNodeType()) {
				case ASTNode.CONSTRUCTOR_INVOCATION:
				case ASTNode.SUPER_CONSTRUCTOR_INVOCATION:
					break;
				case ASTNode.EXPRESSION_STATEMENT:
					Expression expr= ((ExpressionStatement) curr).getExpression();
					if (expr instanceof Assignment) {
						Assignment assignment= (Assignment) expr;
						Expression rightHand = assignment.getRightHandSide();
						if (rightHand instanceof SimpleName && paramsBefore.contains(((SimpleName) rightHand).getIdentifier())) {
							IVariableBinding binding = Bindings.getAssignedVariable(assignment);
							if (binding == null || binding.isField()) {
								break;
							}
						}
					}
					return i;
				default:
					return i;

			}
		}
		return i;

	}

	private int findFieldInsertIndex(List<BodyDeclaration> decls, int currPos) {
		for (int i= decls.size() - 1; i >= 0; i--) {
			ASTNode curr= decls.get(i);
			if (curr instanceof FieldDeclaration && currPos > curr.getStartPosition() + curr.getLength()) {
				return i + 1;
			}
		}
		return 0;
	}

	/**
	 * Returns the variable kind.
	 * @return int
	 */
	public int getVariableKind() {
		return fVariableKind;
	}


}
