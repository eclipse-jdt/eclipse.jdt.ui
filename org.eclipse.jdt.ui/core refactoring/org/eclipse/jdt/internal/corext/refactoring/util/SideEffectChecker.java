package org.eclipse.jdt.internal.corext.refactoring.util;

import java.util.ArrayList;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTMatcher;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

public class SideEffectChecker extends ASTVisitor {

	public SideEffectChecker(ASTNode astNode) {
		fExpression= astNode;
		fSideEffect= false;
	}

	boolean fSideEffect;

	ASTNode fExpression;

	public boolean hasSideEffect() {
		return fSideEffect;
	}

	@Override
	public boolean preVisit2(ASTNode node) {
		if (hasSideEffect()) {
			return false;
		}
		if (selfModied(node)) {
			fSideEffect= true;
			return false;
		}
		if (node instanceof MethodInvocation) {
			MethodInvocation node2= (MethodInvocation) node;
			final IMethodBinding resolveMethodBinding= node2.resolveMethodBinding();
			MethodDeclaration md= findFunctionDefinition(resolveMethodBinding.getDeclaringClass(), resolveMethodBinding);
			if (md != null) {
				MethodVisitor mv= new MethodVisitor();
				md.accept(mv);
				fSideEffect= mv.hasUpdateNoTemp() == true ? true : fSideEffect;
			}
		}
		return super.preVisit2(node);
	}



	private static MethodDeclaration findFunctionDefinition(ITypeBinding iTypeBinding, IMethodBinding methodBinding) {
		if (methodBinding == null ||
				iTypeBinding == null || !(iTypeBinding.getJavaElement() instanceof IType)) {
			return null;
		}
		IType it= (IType) (iTypeBinding.getJavaElement());
		try {
			ITypeHierarchy ith= it.newTypeHierarchy(iTypeBinding.getJavaElement().getJavaProject(), null);
			IMethod iMethod= (IMethod) methodBinding.getJavaElement();
			if (iMethod == null) {
				return null;
			}

			ArrayList<IType> iTypes= new ArrayList<>();
			findTypes(it, ith, iTypes);
			MethodDeclaration res= null;
			for (IType t : iTypes) {
				IMethod tmp= JavaModelUtil.findMethod(iMethod.getElementName(),
						iMethod.getParameterTypes(), false, t);
				if (tmp != null) {
					ICompilationUnit icu= tmp.getCompilationUnit();
					if (icu == null || icu.getSource() == null) {
						return null;
					}
					ASTParser parser= ASTParser.newParser(AST.getJLSLatest());
					parser.setKind(ASTParser.K_COMPILATION_UNIT);
					parser.setSource(icu);
					parser.setResolveBindings(true);
					CompilationUnit compilationUnit= (CompilationUnit) parser.createAST(null);
					final ASTNode perform= NodeFinder.perform(compilationUnit, tmp.getSourceRange());
					if (perform instanceof MethodDeclaration) {
						MethodDeclaration md= (MethodDeclaration) perform;
						if (Modifier.isAbstract(md.resolveBinding().getModifiers()))
							continue;
						if (res != null)
							return null;
						res= md;
					}
				}
			}
			return res;
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static void findTypes(IType it, ITypeHierarchy ith, ArrayList<IType> iTypes) throws JavaModelException {
		iTypes.add(it);
		for (IType i : ith.getAllSubtypes(it)) {
			iTypes.add(i);
		}
		return;
	}

	private boolean selfModied(ASTNode node) {
		if (node instanceof Assignment) {
			Assignment node2= (Assignment) node;
			ASTMatcher match= new ASTMatcher();
			if (!getOriginalExpression(node2.getRightHandSide()).subtreeMatch(match, getOriginalExpression(node2.getLeftHandSide()))) {// like a==a
				AssignmentVisitor visitor= new AssignmentVisitor(node2.getLeftHandSide());
				node2.getRightHandSide().accept(visitor);
				if (visitor.hasDepend()) {
					return true;
				}
			}
		} else if (node instanceof PrefixExpression) {
			PrefixExpression node2= (PrefixExpression) node;
			if (ASTNodes.hasOperator(node2, PrefixExpression.Operator.INCREMENT, PrefixExpression.Operator.DECREMENT)) {
				return true;
			}
		} else if (node instanceof PostfixExpression) {
			return true;
		}
		return false;
	}

	class AssignmentVisitor extends ASTVisitor {
		boolean depend;

		Expression lValue;

		public boolean hasDepend() {
			return depend;
		}

		public AssignmentVisitor(Expression lValue) {
			this.lValue= lValue;
			depend= false;
		}

		@Override
		public boolean preVisit2(ASTNode node) {
			if (depend) {
				return false;
			}
			if (node.subtreeMatch(new ASTMatcher(), lValue)) {
				this.depend= true;
			}
			return super.preVisit2(node);
		}
	}

	class MethodVisitor extends ASTVisitor {
		boolean updateNoTemp;

		public boolean hasUpdateNoTemp() {
			return updateNoTemp;
		}

		public MethodVisitor() {
			updateNoTemp= false;
		}

		@Override
		public boolean preVisit2(ASTNode node) {
			if (updateNoTemp == true) {
				return false;
			}
			Expression operand= null;
			if (node instanceof Assignment) {
				Assignment node2= (Assignment) node;
				operand= getOriginalExpression(node2.getLeftHandSide());
				if (!isNoTemp(operand)) {
					return super.preVisit2(node);
				}
				if (!getOriginalExpression(node2.getRightHandSide()).subtreeMatch(new ASTMatcher(), operand)
						|| node2.getOperator()!= Assignment.Operator.ASSIGN) {
					AssignmentVisitor visitor= new AssignmentVisitor(node2.getLeftHandSide());
					node2.getRightHandSide().accept(visitor);
					if (visitor.hasDepend()) {
						return updateNoTemp= true;
					}
				}
				return super.preVisit2(node);
			}

			if (node instanceof PrefixExpression) {
				PrefixExpression node2= (PrefixExpression) node;
				if (ASTNodes.hasOperator(node2, PrefixExpression.Operator.INCREMENT, PrefixExpression.Operator.DECREMENT)) {
					operand= node2.getOperand();
				}
			} else if (node instanceof PostfixExpression) {
				PostfixExpression node2= (PostfixExpression) node;
				operand= node2.getOperand();
			}
			if (operand != null && isNoTemp(operand)) {
				updateNoTemp= true;
			}
			return super.preVisit2(node);
		}

		private boolean isNoTemp(Expression e) {
			IVariableBinding ivb= null;
			Expression expr= getOriginalExpression(e);
			if (expr instanceof SimpleName) {
				SimpleName sn= (SimpleName) expr;
				if (sn.resolveBinding() instanceof IVariableBinding) {
					ivb= (IVariableBinding) sn.resolveBinding();
				}
			} else if (expr instanceof FieldAccess) {
				FieldAccess fa= (FieldAccess) expr;
				ivb= fa.resolveFieldBinding();
			} else if (expr instanceof QualifiedName) {
				QualifiedName qn= (QualifiedName) expr;
				if (qn.resolveBinding() instanceof IVariableBinding) {
					ivb= (IVariableBinding) qn.resolveBinding();
				}
				return ivb != null && Modifier.isStatic(ivb.getModifiers())
						|| isNoTemp(qn.getQualifier());
			} else if (expr instanceof ArrayAccess) {
				ArrayAccess aa= (ArrayAccess) expr;
				return isNoTemp(aa.getArray());
			}
//			if (ivb != null && ivb.isParameter()) {
//				final StructuralPropertyDescriptor locationInParent= e.getLocationInParent();
//				if (locationInParent == Assignment.LEFT_HAND_SIDE_PROPERTY
//						|| locationInParent == PostfixExpression.OPERAND_PROPERTY
//						|| locationInParent == PrefixExpression.OPERAND_PROPERTY) {
//					return false;
//				}
//			}
//			|| ivb.isParameter()
			return ivb != null && (ivb.isField() || Modifier.isStatic(ivb.getModifiers()));
		}


	}

	private Expression getOriginalExpression(Expression expr) {
		while (expr instanceof ParenthesizedExpression || expr instanceof CastExpression) {
			if (expr instanceof ParenthesizedExpression) {
				ParenthesizedExpression pe= (ParenthesizedExpression) expr;
				expr= pe.getExpression();
			} else {
				CastExpression ce= (CastExpression) expr;
				expr= ce.getExpression();
			}
		}
		return expr;
	}
}
