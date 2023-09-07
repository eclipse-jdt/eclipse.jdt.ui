package org.eclipse.jdt.internal.ui.text.correction;

import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;

import org.eclipse.jdt.ui.text.java.correction.ASTRewriteCorrectionProposalCore;

public class SuppressWarningsProposalCore extends ASTRewriteCorrectionProposalCore {

	private final String fWarningToken;
	private final ASTNode fNode;
	private final ChildListPropertyDescriptor fProperty;

	public SuppressWarningsProposalCore(String warningToken, String label, ICompilationUnit cu, ASTNode node, ChildListPropertyDescriptor property, int relevance) {
		super(label, cu, null, relevance);
		fWarningToken= warningToken;
		fNode= node;
		fProperty= property;
		setCommandId(SuppressWarningsSubProcessorCore.ADD_SUPPRESSWARNINGS_ID);
	}

	/**
	 * @return Returns the warningToken.
	 */
	public String getWarningToken() {
		return fWarningToken;
	}

	@Override
	protected ASTRewrite getRewrite() throws CoreException {
		AST ast= fNode.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);

		StringLiteral newStringLiteral= ast.newStringLiteral();
		newStringLiteral.setLiteralValue(fWarningToken);

		Annotation existing= findExistingAnnotation(ASTNodes.getChildListProperty(fNode, fProperty));
		if (existing == null) {
			ListRewrite listRewrite= rewrite.getListRewrite(fNode, fProperty);

			SingleMemberAnnotation newAnnot= ast.newSingleMemberAnnotation();
			String importString= createImportRewrite((CompilationUnit) fNode.getRoot()).addImport("java.lang.SuppressWarnings"); //$NON-NLS-1$
			newAnnot.setTypeName(ast.newName(importString));

			newAnnot.setValue(newStringLiteral);

			listRewrite.insertFirst(newAnnot, null);
		} else if (existing instanceof SingleMemberAnnotation) {
			SingleMemberAnnotation annotation= (SingleMemberAnnotation) existing;
			Expression value= annotation.getValue();
			if (!addSuppressArgument(rewrite, value, newStringLiteral)) {
				rewrite.set(existing, SingleMemberAnnotation.VALUE_PROPERTY, newStringLiteral, null);
			}
		} else if (existing instanceof NormalAnnotation) {
			NormalAnnotation annotation= (NormalAnnotation) existing;
			Expression value= findValue(annotation.values());
			if (!addSuppressArgument(rewrite, value, newStringLiteral)) {
				ListRewrite listRewrite= rewrite.getListRewrite(annotation, NormalAnnotation.VALUES_PROPERTY);
				MemberValuePair pair= ast.newMemberValuePair();
				pair.setName(ast.newSimpleName("value")); //$NON-NLS-1$
				pair.setValue(newStringLiteral);
				listRewrite.insertFirst(pair, null);
			}
		}
		return rewrite;
	}

	private static boolean addSuppressArgument(ASTRewrite rewrite, Expression value, StringLiteral newStringLiteral) {
		if (value instanceof ArrayInitializer) {
			ListRewrite listRewrite= rewrite.getListRewrite(value, ArrayInitializer.EXPRESSIONS_PROPERTY);
			listRewrite.insertLast(newStringLiteral, null);
		} else if (value instanceof StringLiteral) {
			ArrayInitializer newArr= rewrite.getAST().newArrayInitializer();
			newArr.expressions().add(rewrite.createMoveTarget(value));
			newArr.expressions().add(newStringLiteral);
			rewrite.replace(value, newArr, null);
		} else {
			return false;
		}
		return true;
	}

	private static Expression findValue(List<MemberValuePair> keyValues) {
		for (MemberValuePair curr : keyValues) {
			if ("value".equals(curr.getName().getIdentifier())) { //$NON-NLS-1$
				return curr.getValue();
			}
		}
		return null;
	}

	private static Annotation findExistingAnnotation(List<? extends ASTNode> modifiers) {
		for (ASTNode curr : modifiers) {
			if (curr instanceof NormalAnnotation || curr instanceof SingleMemberAnnotation) {
				Annotation annotation= (Annotation) curr;
				ITypeBinding typeBinding= annotation.resolveTypeBinding();
				if (typeBinding != null) {
					if ("java.lang.SuppressWarnings".equals(typeBinding.getQualifiedName())) { //$NON-NLS-1$
						return annotation;
					}
				} else {
					String fullyQualifiedName= annotation.getTypeName().getFullyQualifiedName();
					if ("SuppressWarnings".equals(fullyQualifiedName) || "java.lang.SuppressWarnings".equals(fullyQualifiedName)) { //$NON-NLS-1$ //$NON-NLS-2$
						return annotation;
					}
				}
			}
		}
		return null;
	}
}