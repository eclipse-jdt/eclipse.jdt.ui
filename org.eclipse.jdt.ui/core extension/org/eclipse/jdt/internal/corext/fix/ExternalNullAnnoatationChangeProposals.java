/*******************************************************************************
 * Copyright (c) 2015 GK Software AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Stephan Herrmann - Initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import static org.eclipse.jdt.core.util.ExternalAnnotationUtil.NONNULL;
import static org.eclipse.jdt.core.util.ExternalAnnotationUtil.NO_ANNOTATION;
import static org.eclipse.jdt.core.util.ExternalAnnotationUtil.NULLABLE;
import static org.eclipse.jdt.core.util.ExternalAnnotationUtil.annotateMethodParameterType;
import static org.eclipse.jdt.core.util.ExternalAnnotationUtil.annotateMethodReturnType;
import static org.eclipse.jdt.core.util.ExternalAnnotationUtil.extractGenericSignature;
import static org.eclipse.jdt.core.util.ExternalAnnotationUtil.getAnnotationFile;
import static org.eclipse.jdt.internal.ui.text.spelling.WordCorrectionProposal.getHtmlRepresentation;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;

import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Dimension;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.WildcardType;
import org.eclipse.jdt.core.util.ExternalAnnotationUtil;
import org.eclipse.jdt.core.util.ExternalAnnotationUtil.MergeStrategy;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.correction.ICommandAccess;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.correction.ExternalNullAnnotationQuickAssistProcessor;
import org.eclipse.jdt.internal.ui.text.correction.IProposalRelevance;

/**
 * Proposals for null annotations that modify external annotations, rather than Java source files.
 * 
 * @see <a href="https://bugs.eclipse.org/458200">[null] "Annotate" proposals for adding external
 *      null annotations to library classes</a>
 * @since 3.11
 */
public class ExternalNullAnnoatationChangeProposals {

	static abstract class SignatureAnnotationChangeProposal implements IJavaCompletionProposal, ICommandAccess {

		protected String fLabel;

		protected ICompilationUnit fCU; // cu where the assist was invoked

		protected String fAffectedTypeName;

		protected IFile fAnnotationFile;

		protected String fSelector;

		protected String fSignature;

		protected String fCurrentAnnotated;

		protected String fAnnotatedSignature;

		protected MergeStrategy fMergeStrategy;

		protected String[] fDryRun; // result from a dry-run signature update; structure: { prefix, old-type, new-type, postfix }


		/* return true if the operation is available. */
		protected boolean initializeOperation(ICompilationUnit cu, ITypeBinding declaringClass, String selector,
				String plainSignature, String annotatedSignature, String label, MergeStrategy mergeStrategy) {
			IJavaProject project= (IJavaProject) cu.getAncestor(IJavaElement.JAVA_PROJECT);
			IFile file= null;
			try {
				file= getAnnotationFile(project, declaringClass, new NullProgressMonitor());
			} catch (CoreException e) {
				return false;
			}
			if (file == null)
				return false;

			fCU= cu;
			fAffectedTypeName= declaringClass.getErasure().getBinaryName().replace('.', '/');
			fAnnotationFile= file;
			fSelector= selector;
			fAnnotatedSignature= annotatedSignature;
			fSignature= plainSignature;

			fLabel= label;
			fMergeStrategy= mergeStrategy;

			fCurrentAnnotated= ExternalAnnotationUtil.getAnnotatedSignature(fAffectedTypeName, file, fSelector, fSignature);
			if (fCurrentAnnotated == null)
				fCurrentAnnotated= fSignature;
			dryRun();
			return fDryRun != null && !fDryRun[1].equals(fDryRun[2]);
		}

		/**
		 * Perform a dry-run annotation update, to check if we have any update, indeed. If
		 * successful, the result should be available in {@link #fDryRun}.
		 */
		protected abstract void dryRun();

		public Point getSelection(IDocument document) {
			return null; // nothing to reveal in the current editor.
		}

		public String getDisplayString() {
			return fLabel;
		}

		public Image getImage() {
			return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_ANNOTATION);
		}

		public IContextInformation getContextInformation() {
			return null;
		}

		public void apply(IDocument document) {
			try {
				doAnnotateMember(new NullProgressMonitor());
			} catch (CoreException e) {
				JavaPlugin.log(e);
			} catch (IOException e) {
				JavaPlugin.log(e);
			}
		}

		public int getRelevance() {
			return IProposalRelevance.CHANGE_METHOD;
		}

		public String getCommandId() {
			return ExternalNullAnnotationQuickAssistProcessor.ANNOTATE_MEMBER_ID;
		}

		public String getAdditionalProposalInfo() {
			StringBuffer buffer= new StringBuffer();
			buffer.append("<dl>"); //$NON-NLS-1$
			buffer.append("<dt>").append(fSelector).append("</dt>"); //$NON-NLS-1$ //$NON-NLS-2$
			buffer.append("<dd>").append(getHtmlRepresentation(fSignature)).append("</dd>"); //$NON-NLS-1$ //$NON-NLS-2$
			buffer.append("<dd>").append(getFullAnnotatedSignatureHTML()).append("</dd>"); //$NON-NLS-1$ //$NON-NLS-2$
			buffer.append("</dl>"); //$NON-NLS-1$
			return buffer.toString();
		}

		protected String getFullAnnotatedSignatureHTML() {
			String[] parts= fDryRun;

			// search the difference:
			int pos= 0;
			while (pos < parts[1].length() && pos < parts[2].length()) {
				if (parts[1].charAt(pos) != parts[2].charAt(pos))
					break;
				pos++;
			}

			// prefix up-to the difference:
			StringBuilder buf= new StringBuilder();
			buf.append(getHtmlRepresentation(parts[0]));
			buf.append(getHtmlRepresentation(parts[2].substring(0, pos)));

			// highlight the difference:
			switch (parts[2].charAt(pos)) {
				case NULLABLE:
				case NONNULL:
					// added annotation in parts[2]: bold:
					buf.append("<b>").append(parts[2].charAt(pos)).append("</b>"); //$NON-NLS-1$ //$NON-NLS-2$
					break;
				default:
					// removed annotation in parts[1]: strike:
					buf.append("<del>").append(parts[1].charAt(pos)).append("</del>"); //$NON-NLS-1$ //$NON-NLS-2$
					pos--; // char in parts[2] is not yet consumed
			}

			// everything else:
			buf.append(getHtmlRepresentation(parts[2].substring(pos + 1)));
			buf.append(getHtmlRepresentation(parts[3]));
			return buf.toString();
		}

		protected abstract void doAnnotateMember(IProgressMonitor monitor) throws CoreException, UnsupportedEncodingException, IOException;
	}

	static class ReturnAnnotationRewriteProposal extends SignatureAnnotationChangeProposal {

		@Override
		protected void dryRun() {
			fDryRun= ExternalAnnotationUtil.annotateReturnType(fCurrentAnnotated, fAnnotatedSignature, fMergeStrategy);
		}

		@Override
		protected void doAnnotateMember(IProgressMonitor monitor) throws CoreException, IOException {
			annotateMethodReturnType(fAffectedTypeName, fAnnotationFile, fSelector, fSignature, fAnnotatedSignature, fMergeStrategy, monitor);
		}
	}

	static class ParameterAnnotationRewriteProposal extends SignatureAnnotationChangeProposal {

		int fParamIdx;

		ParameterAnnotationRewriteProposal(int paramIdx) {
			fParamIdx= paramIdx;
		}

		@Override
		protected void dryRun() {
			fDryRun= ExternalAnnotationUtil.annotateParameterType(fCurrentAnnotated, fAnnotatedSignature, fParamIdx, fMergeStrategy);
		}

		@Override
		protected void doAnnotateMember(IProgressMonitor monitor) throws CoreException, IOException {
			annotateMethodParameterType(fAffectedTypeName, fAnnotationFile, fSelector, fSignature, fAnnotatedSignature, fParamIdx, fMergeStrategy, monitor);
		}
	}

	/* Quick assist on class file, propose changes an any type detail. */
	public static void collectExternalAnnotationProposals(ICompilationUnit cu, ASTNode coveringNode, int offset, ArrayList<IJavaCompletionProposal> resultingCollection) {

		IJavaProject javaProject= cu.getJavaProject();
		if (JavaCore.DISABLED.equals(javaProject.getOption(JavaCore.COMPILER_ANNOTATION_NULL_ANALYSIS, true)))
			return;

		if (!hasAnnotationPathInWorkspace(javaProject, cu)) // refuse to update files outside the workspace
			return;

		ASTNode inner= null; // the innermost type or type parameter node
		while (true) {
			if (coveringNode instanceof Type || coveringNode instanceof TypeParameter) {
				inner= coveringNode;
				break;
			}
			coveringNode= coveringNode.getParent();
			if (coveringNode == null)
				return;
		}
		if (inner.getNodeType() == ASTNode.PRIMITIVE_TYPE)
			return; // cannot be annotated

		// prepare three renderers for three proposals:
		TypeRenderer rendererNonNull= new TypeRenderer(inner, offset, NONNULL);
		TypeRenderer rendererNullable= new TypeRenderer(inner, offset, NULLABLE);
		TypeRenderer rendererRemove= new TypeRenderer(inner, offset, NO_ANNOTATION);
		ASTNode outer= inner; // will become the outermost type or type parameter node
		{
			ASTNode next;
			while (((next= outer.getParent()) instanceof Type) || (next instanceof TypeParameter))
				outer= next;
		}
		boolean useJava8= JavaModelUtil.is18OrHigher(javaProject.getOption(JavaCore.COMPILER_SOURCE, true));
		if (!useJava8 && outer != inner) {
			return; // below 1.8 we can only annotate the top type (not type parameter)
		}
		if (outer instanceof Type) {
			ITypeBinding typeBinding= ((Type) outer).resolveBinding();
			if (typeBinding != null && typeBinding.isPrimitive())
				return;
			outer.accept(rendererNonNull);
			outer.accept(rendererNullable);
			outer.accept(rendererRemove);
		} else {
			List<?> siblingList= (List<?>) outer.getParent().getStructuralProperty(outer.getLocationInParent());
			rendererNonNull.visitTypeParameters(siblingList);
			rendererNullable.visitTypeParameters(siblingList);
			rendererRemove.visitTypeParameters(siblingList);
		}

		StructuralPropertyDescriptor locationInParent= outer.getLocationInParent();
		ProposalCreator creator= null;
		if (locationInParent == MethodDeclaration.RETURN_TYPE2_PROPERTY) {
			MethodDeclaration method= (MethodDeclaration) ASTNodes.getParent(coveringNode, MethodDeclaration.class);
			creator= new ReturnProposalCreator(cu, method.resolveBinding());
		} else if (locationInParent == SingleVariableDeclaration.TYPE_PROPERTY) {
			ASTNode param= outer.getParent();
			if (param.getLocationInParent() == MethodDeclaration.PARAMETERS_PROPERTY) {
				MethodDeclaration method= (MethodDeclaration) ASTNodes.getParent(coveringNode, MethodDeclaration.class);
				int paramIdx= method.parameters().indexOf(param);
				if (paramIdx != -1)
					creator= new ParameterProposalCreator(cu, method.resolveBinding(), paramIdx);
			}
		}
		if (creator != null) {
			createProposalsForType(cu, inner, offset, rendererNonNull, rendererNullable, rendererRemove, creator, resultingCollection);
		}
	}

	static boolean hasAnnotationPathInWorkspace(IJavaProject javaProject, ICompilationUnit cu) {
		IPackageFragmentRoot root= (IPackageFragmentRoot) cu.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
		if (root != null) {
			try {
				IClasspathEntry resolvedClasspathEntry= root.getResolvedClasspathEntry();
				for (IClasspathAttribute cpa : resolvedClasspathEntry.getExtraAttributes()) {
					if (IClasspathAttribute.EXTERNAL_ANNOTATION_PATH.equals(cpa.getName())) {
						Path annotationPath= new Path(cpa.getValue());
						IProject project= javaProject.getProject();
						if (project.exists(annotationPath))
							return true;
						IWorkspaceRoot wsRoot= project.getWorkspace().getRoot();
						return wsRoot.exists(annotationPath);
					}
				}
			} catch (JavaModelException jme) {
				return false;
			}
		}
		return false;
	}

	private static abstract class ProposalCreator {

		ICompilationUnit fCU;

		ITypeBinding fDeclaringClass;

		String fSelector;

		String fSignature;

		MergeStrategy fMergeStrategy= MergeStrategy.OVERWRITE_ANNOTATIONS;

		ProposalCreator(ICompilationUnit cu, ITypeBinding declaringClass, String selector, String signature) {
			fCU= cu;
			fDeclaringClass= declaringClass;
			fSelector= selector;
			fSignature= signature;
		}

		SignatureAnnotationChangeProposal create(String annotatedSignature, String label) {
			SignatureAnnotationChangeProposal operation= doCreate(annotatedSignature, label);
			if (!operation.initializeOperation(fCU, fDeclaringClass, fSelector, fSignature, annotatedSignature, label, fMergeStrategy))
				return null;
			return operation;
		}

		abstract SignatureAnnotationChangeProposal doCreate(String annotatedSignature, String label);
	}

	private static class ReturnProposalCreator extends ProposalCreator {

		ReturnProposalCreator(ICompilationUnit cu, IMethodBinding methodBinding) {
			super(cu, methodBinding.getDeclaringClass(), methodBinding.getName(), extractGenericSignature(methodBinding));
		}

		@Override
		SignatureAnnotationChangeProposal doCreate(String annotatedSignature, String label) {
			return new ReturnAnnotationRewriteProposal();
		}
	}

	private static class ParameterProposalCreator extends ProposalCreator {
		int fParamIdx;

		ParameterProposalCreator(ICompilationUnit cu, IMethodBinding methodBinding, int paramIdx) {
			super(cu, methodBinding.getDeclaringClass(), methodBinding.getName(), extractGenericSignature(methodBinding));
			fParamIdx= paramIdx;
		}

		@Override
		SignatureAnnotationChangeProposal doCreate(String annotatedSignature, String label) {
			return new ParameterAnnotationRewriteProposal(fParamIdx);
		}
	}

	/* Create one proposal from each of the three given renderers. */
	static void createProposalsForType(ICompilationUnit cu, ASTNode type, int offset,
			TypeRenderer rendererNonNull, TypeRenderer rendererNullable, TypeRenderer rendererRemove,
			ProposalCreator creator, ArrayList<IJavaCompletionProposal> resultingCollection) {
		SignatureAnnotationChangeProposal operation;
		String label;
		// propose adding @NonNull:
		label= getAddAnnotationLabel(NullAnnotationsFix.getNonNullAnnotationName(cu, true), type, offset);
		operation= creator.create(rendererNonNull.getResult(), label);
		if (operation != null)
			resultingCollection.add(operation);

		// propose adding @Nullable:
		label= getAddAnnotationLabel(NullAnnotationsFix.getNullableAnnotationName(cu, true), type, offset);
		operation= creator.create(rendererNullable.getResult(), label);
		if (operation != null)
			resultingCollection.add(operation);

		// propose removing annotation:
		label= Messages.format(FixMessages.ExternalNullAnnotationChangeProposals_remove_nullness_annotation,
				new String[] { type2String(type, offset) });
		operation= creator.create(rendererRemove.getResult(), label);
		if (operation != null)
			resultingCollection.add(operation);
	}

	static String getAddAnnotationLabel(String annotationName, ASTNode type, int offset) {
		if (type.getNodeType() == ASTNode.ARRAY_TYPE) {
			// need to assemble special format with annotation attached to the selected dimension:
			ArrayType arrayType= (ArrayType) type;
			StringBuilder left= new StringBuilder(arrayType.getElementType().toString());
			StringBuilder dimsRight= new StringBuilder();
			@SuppressWarnings("rawtypes")
			List dimensions= arrayType.dimensions();
			for (int i= 0; i < dimensions.size(); i++) {
				Dimension dimension= (Dimension) dimensions.get(i);
				if (dimension.getStartPosition() + dimension.getLength() <= offset)
					left.append("[]"); //$NON-NLS-1$
				else
					dimsRight.append("[]"); //$NON-NLS-1$
			}
			return Messages.format(FixMessages.ExternalNullAnnotationChangeProposals_add_nullness_array_annotation,
					new String[] { left.toString(), annotationName, dimsRight.toString() });
		}
		return Messages.format(FixMessages.ExternalNullAnnotationChangeProposals_add_nullness_annotation,
				new String[] { annotationName, type.toString() });
	}

	static String type2String(ASTNode type, int offset) {
		if (type.getNodeType() == ASTNode.ARRAY_TYPE) {
			ArrayType arrayType= (ArrayType) type;
			StringBuilder buf= new StringBuilder(arrayType.getElementType().toString());
			@SuppressWarnings("rawtypes")
			List dimensions= arrayType.dimensions();
			for (int i= 0; i < dimensions.size(); i++) {
				Dimension dimension= (Dimension) dimensions.get(i);
				if (dimension.getStartPosition() + dimension.getLength() > offset)
					buf.append("[]"); //$NON-NLS-1$
			}
			return buf.toString();
		}
		return type.toString();
	}

	/**
	 * A visitor that renders an AST snippet representing a type or type parameter. For rendering
	 * the Eclipse External Annotation format is used, i.e., class file signatures with additions
	 * for null annotations.
	 * <p>
	 * In particular a given null annotation is inserted for the given focusType.
	 * </p>
	 */
	static class TypeRenderer extends ASTVisitor {

		StringBuffer fBuffer;

		ASTNode fFocusType; // Type or TypeParameter

		int fOffset;

		char fAnnotation;

		public TypeRenderer(ASTNode focusType, int offset, char annotation) {
			fBuffer= new StringBuffer();
			fFocusType= focusType;
			fOffset= offset;
			fAnnotation= annotation;
		}

		public String getResult() {
			return fBuffer.toString();
		}

		/* Renders a type parameter list in angle brackets. */
		public void visitTypeParameters(@SuppressWarnings("rawtypes") List parameters) {
			fBuffer.append('<');
			for (Object p : parameters)
				((TypeParameter) p).accept(this);
			fBuffer.append('>');
		}

		@Override
		public boolean visit(ParameterizedType type) {
			fBuffer.append('L');
			if (type == fFocusType || type.getType() == fFocusType)
				fBuffer.append(fAnnotation);
			fBuffer.append(binaryName(type.resolveBinding()));
			fBuffer.append('<');
			for (Object arg : type.typeArguments())
				((Type) arg).accept(this);
			fBuffer.append('>');
			fBuffer.append(';');
			return false;
		}

		@Override
		public boolean visit(WildcardType wildcard) {
			Type bound= wildcard.getBound();
			if (bound == null) {
				fBuffer.append('*');
			} else if (wildcard.isUpperBound()) {
				fBuffer.append('+');
			} else {
				fBuffer.append('-');
			}
			if (wildcard == fFocusType)
				fBuffer.append(fAnnotation);
			if (bound != null)
				bound.accept(this);
			return false;
		}

		@Override
		public boolean visit(ArrayType array) {
			@SuppressWarnings("rawtypes")
			List dimensions= array.dimensions();
			boolean annotated= false;
			for (int i= 0; i < dimensions.size(); i++) {
				fBuffer.append('[');
				Dimension dimension= (Dimension) dimensions.get(i);
				if (!annotated && array == fFocusType && dimension.getStartPosition() + dimension.getLength() > fOffset) {
					fBuffer.append(fAnnotation);
					annotated= true;
				}
			}
			array.getElementType().accept(this);
			return false;
		}

		@Override
		public boolean visit(TypeParameter parameter) {
			if (parameter == fFocusType)
				fBuffer.append(fAnnotation);
			fBuffer.append(parameter.getName().getIdentifier());
			Type classBound= null;
			for (Object bound : parameter.typeBounds()) {
				Type typeBound= (Type) bound;
				if (typeBound.resolveBinding().isClass()) {
					classBound= typeBound;
					break;
				}
			}
			if (classBound != null) {
				fBuffer.append(':');
				classBound.accept(this);
			} else {
				ITypeBinding typeBinding= parameter.resolveBinding();
				fBuffer.append(":L").append(binaryName(typeBinding.getSuperclass())).append(';'); //$NON-NLS-1$
			}
			for (Object bound : parameter.typeBounds()) {
				if (bound == classBound)
					continue;
				Type typeBound= (Type) bound;
				fBuffer.append(':');
				typeBound.accept(this);
			}
			return false;
		}

		@Override
		public boolean visit(SimpleType type) {
			ITypeBinding typeBinding= type.resolveBinding();
			if (typeBinding.isTypeVariable()) {
				fBuffer.append('T');
				if (fFocusType == type)
					fBuffer.append(fAnnotation);
				fBuffer.append(typeBinding.getName()).append(';');
			} else {
				fBuffer.append('L');
				if (fFocusType == type)
					fBuffer.append(fAnnotation);
				fBuffer.append(binaryName(typeBinding)).append(';');
			}
			return false;
		}

		@Override
		public boolean visit(PrimitiveType node) {
			// not a legal focus type, but could be array element type
			fBuffer.append(node.resolveBinding().getBinaryName());
			return false;
		}

		String binaryName(ITypeBinding type) {
			return type.getBinaryName().replace('.', '/');
		}
	}
}
