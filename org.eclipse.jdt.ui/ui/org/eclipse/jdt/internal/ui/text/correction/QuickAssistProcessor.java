package org.eclipse.jdt.internal.ui.text.correction;

import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.WhileStatement;

import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
  */
public class QuickAssistProcessor implements ICorrectionProcessor {

	/**
	 * Constructor for CodeManipulationProcessor.
	 */
	public QuickAssistProcessor() {
		super();
	}
	
	public void process(ICorrectionContext context, List resultingCollections) throws CoreException {
		int id= context.getProblemId();
		if (id != 0) { // no proposals for problem locations
			return;
		}
		getAssignToVariableProposals(context, resultingCollections);
		getCatchClauseToThrowsProposals(context, resultingCollections);
		getRenameLocalProposals(context, resultingCollections);
		getUnWrapProposals(context, resultingCollections);
	}
	
	private void getAssignToVariableProposals(ICorrectionContext context, List resultingCollections) throws CoreException {
		ASTNode node= context.getCoveringNode();
		
		Statement statement= ASTResolving.findParentStatement(node);
		if (!(statement instanceof ExpressionStatement)) {
			return;
		}
		ExpressionStatement expressionStatement= (ExpressionStatement) statement;

		Expression expression= expressionStatement.getExpression();
		if (expression.getNodeType() == ASTNode.ASSIGNMENT) {
			return; // too confusing and not helpful
		}
		
		
		ITypeBinding typeBinding= expression.resolveTypeBinding();
		typeBinding= ASTResolving.normalizeTypeBinding(typeBinding);
		if (typeBinding == null) {
			return;
		}
		ICompilationUnit cu= context.getCompilationUnit();
		
		AssignToVariableAssistProposal localProposal= new AssignToVariableAssistProposal(cu, AssignToVariableAssistProposal.LOCAL, expressionStatement, typeBinding, 2);
		resultingCollections.add(localProposal);	
		
		ASTNode type= ASTResolving.findParentType(expression);
		if (type != null) {
			AssignToVariableAssistProposal fieldProposal= new AssignToVariableAssistProposal(cu, AssignToVariableAssistProposal.FIELD, expressionStatement, typeBinding, 1);
			resultingCollections.add(fieldProposal);
		}				
	}
	
	private void getCatchClauseToThrowsProposals(ICorrectionContext context, List resultingCollections) throws CoreException {
		ASTNode node= context.getCoveringNode();
		CatchClause catchClause= (CatchClause) ASTResolving.findAncestor(node, ASTNode.CATCH_CLAUSE);
		if (catchClause == null) {
			return;
		}
		Type type= catchClause.getException().getType();
		if (!type.isSimpleType()) {
			return;
		}
		
		
		BodyDeclaration bodyDeclaration= ASTResolving.findParentBodyDeclaration(catchClause);
		if (!(bodyDeclaration instanceof MethodDeclaration)) {
			return;
		}
		
		MethodDeclaration methodDeclaration= (MethodDeclaration) bodyDeclaration;
		
		ASTRewrite rewrite= new ASTRewrite(methodDeclaration);
		AST ast= methodDeclaration.getAST();
		
		TryStatement tryStatement= (TryStatement) catchClause.getParent();
		if (tryStatement.catchClauses().size() > 1 || tryStatement.getFinally() != null) {
			rewrite.markAsRemoved(catchClause);
		} else {
			List statements= tryStatement.getBody().statements();
			if (statements.size() > 0) {
				ASTNode placeholder= rewrite.createCopy((ASTNode) statements.get(0), (ASTNode) statements.get(statements.size() - 1));
				rewrite.markAsReplaced(tryStatement, placeholder);
			} else {
				rewrite.markAsRemoved(tryStatement);
			}
		}
		ITypeBinding binding= type.resolveBinding();
		if (binding == null || isNotYetThrown(binding, methodDeclaration.thrownExceptions())) {
			Name name= ((SimpleType) type).getName();
			Name newName= (Name) ASTNode.copySubtree(ast, name);
			rewrite.markAsInserted(newName);
			methodDeclaration.thrownExceptions().add(newName);
		}			
	
		String label= CorrectionMessages.getString("QuickAssistProcessor.catchclausetothrows.description"); //$NON-NLS-1$
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 1, image);
		proposal.ensureNoModifications();
		resultingCollections.add(proposal);

	}
	
	private boolean isNotYetThrown(ITypeBinding binding, List thrownExcpetions) {
		for (int i= 0; i < thrownExcpetions.size(); i++) {
			Name name= (Name) thrownExcpetions.get(i);
			ITypeBinding elem= (ITypeBinding) name.resolveBinding();
			if (elem != null) {
				ITypeBinding curr= binding;
				if (curr != null && curr != elem) {
					curr= curr.getSuperclass();
				}
				if (curr != null) {
					return false;
				}
			}
		}
		return true;
	
	}
	
	
	private void getRenameLocalProposals(ICorrectionContext context, List resultingCollections) throws CoreException {
		ASTNode node= context.getCoveringNode();
		if (!(node instanceof SimpleName)) {
			return;
		}
		SimpleName name= (SimpleName) node;
		IBinding binding= name.resolveBinding();
		if (binding == null || binding.getKind() == IBinding.PACKAGE) {
			return;
		}
		LinkedNamesAssistProposal proposal= new LinkedNamesAssistProposal(name);
		resultingCollections.add(proposal);
	}
	
	private ASTNode getCopyOfInner(ASTRewrite rewrite, Statement statement) {
		if (statement.getNodeType() == ASTNode.BLOCK) {
			Block block= (Block) statement;
			List innerStatements= block.statements();
			int nStatements= innerStatements.size();
			if (nStatements == 1) {
				return rewrite.createCopy((ASTNode) innerStatements.get(0));
			} else if (nStatements > 1) {
				return rewrite.createCopy((ASTNode) innerStatements.get(0), (ASTNode) innerStatements.get(nStatements - 1));
			}
			return null;
		} else {
			return rewrite.createCopy(statement);
		}
	}
	
	
	private void getUnWrapProposals(ICorrectionContext context, List resultingCollections) throws CoreException {
		ASTNode node= context.getCoveringNode();
		if (node == null) {
			return;
		}

		ASTRewrite rewrite= new ASTRewrite(context.getASTRoot());
		
		ASTNode outer= node;
			
		Block block= null;
		if (outer.getNodeType() == ASTNode.BLOCK) {
			block= (Block) outer;
			outer= block.getParent();
		}
		
		Statement body= null;
		if (outer instanceof IfStatement) {
			IfStatement ifStatement= (IfStatement) outer;
			Statement elseBlock= ifStatement.getElseStatement();
			if (elseBlock == null || ((elseBlock instanceof Block) && ((Block) elseBlock).statements().isEmpty())) {
				body= ifStatement.getThenStatement();
			}				
		} else if (outer instanceof WhileStatement) {
			body=((WhileStatement) outer).getBody();
		} else if (outer instanceof ForStatement) {
			body=((ForStatement) outer).getBody();
		} else if (outer instanceof DoStatement) {
			body=((DoStatement) outer).getBody();
		} else if (outer instanceof TryStatement) {
			TryStatement tryStatement= (TryStatement) outer;
			if (tryStatement.catchClauses().isEmpty()) {
				body= tryStatement.getBody();
			}
		} else if (outer instanceof AnonymousClassDeclaration) {
			List decls= ((AnonymousClassDeclaration) outer).bodyDeclarations();
			for (int i= 0; i < decls.size(); i++) {
				ASTNode elem= (ASTNode) decls.get(i);
				if (elem instanceof MethodDeclaration) {
					Block curr= ((MethodDeclaration) elem).getBody();
					if (curr != null && !curr.statements().isEmpty()) {
						if (body != null) {
							return;
						}
						body= curr;
					}
				} else if (elem instanceof TypeDeclaration) {
					return;
				}
			}
			outer= ASTResolving.findParentStatement(outer);
		} else if (outer instanceof Block) {
			//	-> a block in a block
			body= block;
			outer= block;
		}
		if (body == null) {
			return; 
		}
		ASTNode inner= getCopyOfInner(rewrite, body);
		if (inner != null) {
			rewrite.markAsReplaced(outer, inner);
			String label= CorrectionMessages.getString("QuickAssistProcessor.extrude.description"); //$NON-NLS-1$
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);
			ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 1, image);
			proposal.ensureNoModifications();
			resultingCollections.add(proposal);
		}				
	}	
}
