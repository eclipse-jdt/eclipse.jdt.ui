package org.eclipse.jdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.LocalVariableIndex;
import org.eclipse.jdt.internal.corext.dom.ModifierRewrite;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.refactoring.code.flow.FlowContext;
import org.eclipse.jdt.internal.corext.refactoring.code.flow.FlowInfo;
import org.eclipse.jdt.internal.corext.refactoring.code.flow.InOutFlowAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.surround.SurroundWithAnalyzer;

import org.eclipse.jdt.ui.text.java.IInvocationContext;

public class SurroundWithRunnableProposal extends LinkedCorrectionProposal {
	
	private static final class SplitSelectedOperator implements ISplitOperation {
		
		private List fAccessedInside;
		private ListRewrite fStatementRewrite;
		private List fAccessedAfter;
		private ASTRewrite fRewrite;
		private ListRewrite fBlockRewrite;
		private VariableDeclarationStatement fLastStatement= null;

		private SplitSelectedOperator(List inside, List after, ListRewrite blockRewrite, ASTRewrite rewrite, ListRewrite statementRewrite) {
			super();
			fAccessedInside= inside;
			fStatementRewrite= statementRewrite;
			fAccessedAfter= after;
			fRewrite= rewrite;
			fBlockRewrite= blockRewrite;
		}

		public boolean needsSplit(VariableDeclarationFragment last, VariableDeclarationFragment current) {
			return 
				fAccessedInside.contains(last) != fAccessedInside.contains(current) ||
				fAccessedAfter.contains(last) != fAccessedAfter.contains(current);
		}

		public void initializeStatement(VariableDeclarationStatement statement, VariableDeclarationFragment current) {
			if (fAccessedAfter.contains(current)) {
				if (fAccessedInside.contains(current))
					makeFinal(statement, fRewrite);
				splitOffInitializer(fStatementRewrite, current, fRewrite);
				
				if (fLastStatement != null) {
					fBlockRewrite.insertAfter(statement, fLastStatement, null);
				}
				fLastStatement= statement;
			} else {
				if (fLastStatement != null) {
					fStatementRewrite.insertLast(statement, null);
				} else {
					fStatementRewrite.insertLast(fRewrite.createMoveTarget(statement), null);
					fLastStatement= statement;
				}
			}
		}
	}

	private static final class SplitUnselectedOperator implements ISplitOperation {
		
		private List fAccessedInside;
		private ListRewrite fBlockRewrite;
		private ASTRewrite fRewrite;
		private VariableDeclarationStatement fLastStatement;

		private SplitUnselectedOperator(List accessedInside, ListRewrite blockRewrite, ASTRewrite rewrite) {
			super();
			fAccessedInside= accessedInside;
			fBlockRewrite= blockRewrite;
			fRewrite= rewrite;
			fLastStatement= null;
		}

		public boolean needsSplit(VariableDeclarationFragment last, VariableDeclarationFragment current) {
			return fAccessedInside.contains(last) != fAccessedInside.contains(current);
		}

		public void initializeStatement(VariableDeclarationStatement statement, VariableDeclarationFragment current) {
			if (fAccessedInside.contains(current))
				makeFinal(statement, fRewrite);
		
			if (fLastStatement != null)
				fBlockRewrite.insertAfter(statement, fLastStatement, null);
			fLastStatement= statement;
		}
	}

	private interface ISplitOperation {
		boolean needsSplit(VariableDeclarationFragment last, VariableDeclarationFragment current);
		void initializeStatement(VariableDeclarationStatement statement, VariableDeclarationFragment current);
	}

	private static final String PROPOSED_RUNNABLE_VAR_NAME= "runnable"; //$NON-NLS-1$
	private static final String TYPE_NAME= "Runnable"; //$NON-NLS-1$
	private static final String METHOD_NAME= "run"; //$NON-NLS-1$

	private CompilationUnit fRootNode;
	private Statement[] fSelectedNodes;
	private ASTRewrite fRewrite;
	
	/**
	 * Set up a <code>SurroundWithRunnableProposal</code> which generates a @see <code>Runnable</code> arround
	 * <code>selectedNodes</code>
	 * @param name The display name of the proposal.
	 * @param context The context in which the proposal is applyed.
	 * @param relevance The relevance of this proposal.
	 * @param image The image that is displayed for this proposal or <code>null</code> if no
	 * image is desired.
	 * @param selectedNodes The selected nodes to enclose with a Runnable. Returned by <code>isValidSelection</code>.
	 */
	public SurroundWithRunnableProposal(String name, IInvocationContext context, int relevance, Image image, Statement[] selectedNodes) {
		super(name, context.getCompilationUnit(), null, relevance, image);
		fRootNode= context.getASTRoot();
		fSelectedNodes= selectedNodes;
	}

	/**
	 * Selected nodes in <code>context</code> under <code>selection</code> or null if no valid selection.
	 * @param context The context in which the proposal is applyed.
	 * @return Selected nodes or null if no valid selection.
	 * @throws JavaModelException
	 */
	public static Statement[] getSelectedStatements(IInvocationContext context) throws JavaModelException {

		Selection selection= Selection.createFromStartLength(context.getSelectionOffset(), context.getSelectionLength());
		SurroundWithAnalyzer analyzer= new SurroundWithAnalyzer(context.getCompilationUnit(), selection);
		context.getASTRoot().accept(analyzer);

		if (!analyzer.getStatus().isOK() || !analyzer.hasSelectedNodes()) {
			return null;
		} else {
			return analyzer.getSelectedStatements();
		}
	}
	
	/**
	 * Returns the rewriter to be used.
	 * @return Returns the rewriter to be used.
	 * @throws CoreException A core exception is thrown when the could not be created.
	 */
	protected ASTRewrite getRewrite() throws CoreException {
		if (fRewrite == null) {
			
			Statement[] selectedNodes= fSelectedNodes;
			AST ast= getAst();
			
			fRewrite= ASTRewrite.create(ast);
			Block newBody= ast.newBlock();
			Statement insertedCode= generateCodeSkeleton(newBody, fRewrite);
			
			BodyDeclaration enclosingBodyDeclaration= (BodyDeclaration)ASTNodes.getParent(selectedNodes[0], BodyDeclaration.class);
			int maxVariableId= LocalVariableIndex.perform(enclosingBodyDeclaration) + 1;
			
			List accessedAfter= getVariableDeclarationsAccessedAfter(selectedNodes[selectedNodes.length - 1], maxVariableId);
			List readInside= getVariableDeclarationReadsInside(selectedNodes, maxVariableId);
			
			moveToBlock(selectedNodes, newBody, accessedAfter, readInside, fRewrite);
			
			getListRewrite(selectedNodes[0], fRewrite).insertAfter(insertedCode, selectedNodes[selectedNodes.length - 1], null);
			
			setEndPosition(fRewrite.track(insertedCode));
		}
		return fRewrite;
	}
	
	/**
	 * Generate a new code skeleton.
	 * @param newBody The new body which will be filled with code.
	 * @param rewrite The rewrite to use to change the ast.
	 * @return The root of the new code.
	 */
	private Statement generateCodeSkeleton(Block newBody, ASTRewrite rewrite) {

		AST ast= getAst();
		
		MethodDeclaration runMethod= ast.newMethodDeclaration();
		runMethod.setName(ast.newSimpleName(METHOD_NAME));
		runMethod.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
		runMethod.setBody(newBody);
		
		AnonymousClassDeclaration runnableClassDeclaration= ast.newAnonymousClassDeclaration();
		runnableClassDeclaration.bodyDeclarations().add(runMethod);
		
		ClassInstanceCreation runnableInstanziator= ast.newClassInstanceCreation();
		runnableInstanziator.setType(ast.newSimpleType(ast.newName(TYPE_NAME)));
		runnableInstanziator.setAnonymousClassDeclaration(runnableClassDeclaration);
		
		VariableDeclarationFragment variableDeclarationFragment= ast.newVariableDeclarationFragment();
		SimpleName variableName= ast.newSimpleName(PROPOSED_RUNNABLE_VAR_NAME);
		variableDeclarationFragment.setName(variableName);
		variableDeclarationFragment.setInitializer(runnableInstanziator);
		addLinkedPosition(rewrite.track(variableName), true, variableName.getFullyQualifiedName());
		
		VariableDeclarationStatement variableDeclaration= ast.newVariableDeclarationStatement(variableDeclarationFragment);
		variableDeclaration.setType(ast.newSimpleType(ast.newName(TYPE_NAME)));
		
		return variableDeclaration;
	}
	
	/**
	 * List of VariableDeclaration of variables which are read in <code>selectedNodes</code>.
	 * 
	 * @param maxVariableId Maximum number of variable declarations block
	 * @param selectedNodes The selectedNodes
	 * @return	List of VariableDeclaration
	 */
	private List/*<VariableDeclaration>*/ getVariableDeclarationReadsInside(Statement[] selectedNodes, int maxVariableId) {
		
		ArrayList result= new ArrayList();
		
		IVariableBinding[] reads= getReads(selectedNodes, maxVariableId);
		for (int i= 0; i < reads.length; i++) {
			IVariableBinding read= reads[i];
			if (!read.isField()) {
				ASTNode readDecl= fRootNode.findDeclaringNode(read);
				if (readDecl instanceof VariableDeclaration) {
					result.add(readDecl);
				}
			}
		}
		
		return result;
	}
	
	/**
	 * List of VariableDeclarationFragments which are accessed after <code>startNode</code>.
	 * 
	 * @param startNode The node after to inspect
	 * @param maxVariableId The maximum number of variable declarations
	 * @return List of VariableDeclarationFragments which can't be moved to the new block
	 */
	private List/*<VariableDeclarationFragment>*/ getVariableDeclarationsAccessedAfter(ASTNode startNode, int maxVariableId) {

		List statements;
		if (startNode.getLocationInParent() == SwitchStatement.STATEMENTS_PROPERTY) {
			SwitchStatement block= (SwitchStatement)ASTNodes.getParent(startNode, SwitchStatement.class);
			statements= block.statements();
		} else {
			Block block= (Block)ASTNodes.getParent(startNode, Block.class);
			statements= block.statements();
		}
		List bodyAfterSelection= statements.subList(statements.indexOf(startNode) + 1, statements.size());
		
		List result= new ArrayList();
		if (!bodyAfterSelection.isEmpty()) {
		
			IVariableBinding[] accesses= getAccesses((ASTNode[]) bodyAfterSelection.toArray(new ASTNode[bodyAfterSelection.size()]), maxVariableId);
			
			for (int i= 0; i < accesses.length; i++) {
				IVariableBinding curVar= accesses[i];
				if (!curVar.isField()) {
					ASTNode readDecl= ASTNodes.findDeclaration(curVar, fRootNode);
					if (readDecl instanceof VariableDeclarationFragment) {
						result.add(readDecl);
					}
				}
			}
		}
		return result;
	}
	
	/**
	 * @param region The region to inspect
	 * @param maxVariableId Max number of variables in region
	 * @return All variables with read access in region
	 */
	private IVariableBinding[] getReads(ASTNode[] region, int maxVariableId) {
		FlowContext flowContext= new FlowContext(0, maxVariableId);
		flowContext.setConsiderAccessMode(true);
		flowContext.setComputeMode(FlowContext.ARGUMENTS);
		FlowInfo argInfo= new InOutFlowAnalyzer(flowContext).perform(region);
		IVariableBinding[] reads= argInfo.get(flowContext, FlowInfo.READ | FlowInfo.READ_POTENTIAL | FlowInfo.UNKNOWN);
		return reads;
	}

	/**
	 * @param region The region to inspect
	 * @param maxVariableId Max number of variables in region
	 * @return All variables with read or write access in region
	 */
	private IVariableBinding[] getAccesses(ASTNode[] region, int maxVariableId) {
		FlowContext flowContext= new FlowContext(0, maxVariableId);
		flowContext.setConsiderAccessMode(true);
		flowContext.setComputeMode(FlowContext.ARGUMENTS);
		FlowInfo argInfo= new InOutFlowAnalyzer(flowContext).perform(region);
		IVariableBinding[] varsAccessedAfter= argInfo.get(flowContext, FlowInfo.READ | FlowInfo.READ_POTENTIAL  | FlowInfo.WRITE | FlowInfo.WRITE_POTENTIAL | FlowInfo.UNKNOWN);
		return varsAccessedAfter;
	}

	/**
	 * Moves the nodes in toMove to <code>block</block> except the VariableDeclarationFragments
	 * in <code>accessedAfter</code>. The initializers (if any) of variable declarations
	 * in <code>accessedAfter</code> are moved to the block if the variable declaration is
	 * part of <code>toMove</code>. VariableDeclarations in <code>accessedInside</code> are 
	 * made final unless they are moved to <code>block</code>.
	 * 
	 * i.e. (if <code>i</code> is element of <code>accessedAfter</code>):
	 * <code>int i= 10;</code> ---> <code>int i;</code> and <code>{i= 10;}</code>
	 * 
	 * Declarations with more then one fragments are splited if required. i.e.:
	 * <code>int i,j,k;</code> ---> <code>int i,j; final int k;</code> 
	 * 
	 * @param toMove Nodes to be moved to block
	 * @param block The block to move to
	 * @param accessedAfter VariableDeclarationFragments which can not be moved to block
	 * @param accessedInside VariableDeclaration which can be made final
	 * @param rewrite The rewrite to use.
	 */
	private void moveToBlock(Statement[] toMove, Block block, final List/*<VariableDeclarationFragment>*/ accessedAfter, final List/*<VariableDeclaration>*/ accessedInside, final ASTRewrite rewrite) {
		
		final ListRewrite statements= rewrite.getListRewrite(block, Block.STATEMENTS_PROPERTY);
		
		for (int i= 0; i < toMove.length; i++) {
			ASTNode node= toMove[i];
			if (node instanceof VariableDeclarationStatement) {
				VariableDeclarationStatement statement= (VariableDeclarationStatement)node;
				final ListRewrite blockRewrite= getListRewrite(statement, rewrite);
				
				splitVariableDeclarationStatement(statement, new SplitSelectedOperator(accessedInside, accessedAfter, blockRewrite, rewrite, statements), rewrite);
				
				for (Iterator iter= statement.fragments().iterator(); iter.hasNext();) {
					accessedInside.remove(iter.next());
				}
			} else {
				statements.insertLast(rewrite.createMoveTarget(node), null);
			}
		}
		
		while (!accessedInside.isEmpty()) {
			VariableDeclaration variableDeclaration= (VariableDeclaration)accessedInside.get(0);
			if (variableDeclaration instanceof SingleVariableDeclaration) {
				if (ASTNodes.findModifierNode(Modifier.FINAL, ASTNodes.getModifiers(variableDeclaration)) == null) {
					ModifierRewrite.create(rewrite, variableDeclaration).setModifiers(Modifier.FINAL, Modifier.NONE, null);
				}
				accessedInside.remove(0);
			} else {
				VariableDeclarationStatement statement= (VariableDeclarationStatement)variableDeclaration.getParent();
				final ListRewrite blockRewrite= getListRewrite(statement, rewrite);
				
				splitVariableDeclarationStatement(statement, new SplitUnselectedOperator(accessedInside, blockRewrite, rewrite), rewrite);
				
				for (Iterator iter= statement.fragments().iterator(); iter.hasNext();) {
					VariableDeclarationFragment fragment= (VariableDeclarationFragment)iter.next();
					accessedInside.remove(fragment);
				}
			}
		}
	}

	/**
	 * Split the framents in <code>statement</code> to multiple VariableDeclarationStatements whenever
	 * <code>splitOperator.needsSplit</code> returns <code>true</code>.
	 * i.e.:
	 * int i, j; ---> int i; int j; (if splitOperator.needsSplit(i, j) == true)
	 * 
	 * @param statement The VariableDeclarationStatement to split
	 * @param splitOperator The operator to use to split
	 * @param rewrite The rewriter to use to generate new VariableDeclarationStatements.
	 */
	private void splitVariableDeclarationStatement(VariableDeclarationStatement statement, ISplitOperation splitOperator, ASTRewrite rewrite) {
		
		List fragments= statement.fragments();
		Iterator iter= fragments.iterator();
		VariableDeclarationFragment lastFragment= (VariableDeclarationFragment)iter.next();
		VariableDeclarationStatement lastStatement= statement;
		
		splitOperator.initializeStatement(lastStatement, lastFragment);
		
		ListRewrite fragmentsRewrite= null;
		while (iter.hasNext()) {
			VariableDeclarationFragment currentFragment= (VariableDeclarationFragment)iter.next();
			
			if (splitOperator.needsSplit(lastFragment, currentFragment)) {
					
					VariableDeclarationStatement newStatement= getAst().newVariableDeclarationStatement((VariableDeclarationFragment)rewrite.createMoveTarget(currentFragment));
					
					ListRewrite modifierRewrite= rewrite.getListRewrite(newStatement, VariableDeclarationStatement.MODIFIERS2_PROPERTY);
					for (Iterator iterator= statement.modifiers().iterator(); iterator.hasNext();) {
						modifierRewrite.insertLast(rewrite.createCopyTarget((ASTNode)iterator.next()), null);
					}
					
					newStatement.setType((Type)rewrite.createCopyTarget(statement.getType()));
					
					splitOperator.initializeStatement(newStatement, currentFragment);
					
					fragmentsRewrite= rewrite.getListRewrite(newStatement, VariableDeclarationStatement.FRAGMENTS_PROPERTY);
					
					lastStatement= newStatement;
			} else if (fragmentsRewrite != null) {
				ASTNode fragment0= rewrite.createMoveTarget(currentFragment);
				fragmentsRewrite.insertLast(fragment0, null);
			}
			lastFragment= currentFragment;
		}
	}

	/**
	 * Split off initializer in <code>fragment</code> (if any) and add it as a new expression at the end of <code>statements</code>.
	 * @param statements The home of the new expression.
	 * @param fragment Thr fragment to split.
	 * @param rewrite The rewrite to use.
	 */
	private static void splitOffInitializer(ListRewrite statements, VariableDeclarationFragment fragment, ASTRewrite rewrite) {
		Expression initializer= fragment.getInitializer();
		if (initializer != null) {
			AST ast= statements.getASTRewrite().getAST();
			Assignment assignment= ast.newAssignment();
			assignment.setLeftHandSide((Expression)rewrite.createCopyTarget(fragment.getName()));
			assignment.setRightHandSide((Expression)rewrite.createMoveTarget(initializer));
			statements.insertLast(ast.newExpressionStatement(assignment), null);
		}
	}
	
	/**
	 * Make statement final
	 * @param statement
	 * @param rewrite
	 */
	private static void makeFinal(VariableDeclarationStatement statement, ASTRewrite rewrite) {
		VariableDeclaration fragment= (VariableDeclaration)statement.fragments().get(0);
		if (ASTNodes.findModifierNode(Modifier.FINAL, ASTNodes.getModifiers(fragment)) == null) {
			ModifierRewrite.create(rewrite, statement).setModifiers(Modifier.FINAL, Modifier.NONE, null);
		}
	}
	
	/**
	 * Get a list rewrite for statement sequence node is element
	 * @param node
	 * @param rewrite
	 * @return The list rewrite
	 */
	private ListRewrite getListRewrite(ASTNode node, ASTRewrite rewrite) {
		if (node.getLocationInParent() == SwitchStatement.STATEMENTS_PROPERTY) {
			ASTNode block= ASTNodes.getParent(node, SwitchStatement.class);
			return rewrite.getListRewrite(block, SwitchStatement.STATEMENTS_PROPERTY);
		} else {
			ASTNode block= ASTNodes.getParent(node, Block.class);
			return fRewrite.getListRewrite(block, Block.STATEMENTS_PROPERTY);
		}
	}

	private AST getAst() {
		return fRootNode.getAST();
	}

}
