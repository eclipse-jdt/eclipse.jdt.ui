/*******************************************************************************
 * Copyright (c) 2015, 2019 GK Software AG and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Stephan Herrmann - Initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import static org.eclipse.jdt.core.util.ExternalAnnotationUtil.NONNULL;
import static org.eclipse.jdt.core.util.ExternalAnnotationUtil.NO_ANNOTATION;
import static org.eclipse.jdt.core.util.ExternalAnnotationUtil.NULLABLE;
import static org.eclipse.jdt.core.util.ExternalAnnotationUtil.annotateMember;
import static org.eclipse.jdt.core.util.ExternalAnnotationUtil.annotateMethodParameterType;
import static org.eclipse.jdt.core.util.ExternalAnnotationUtil.annotateMethodReturnType;
import static org.eclipse.jdt.core.util.ExternalAnnotationUtil.annotateMethodTypeParameter;
import static org.eclipse.jdt.core.util.ExternalAnnotationUtil.extractGenericSignature;
import static org.eclipse.jdt.core.util.ExternalAnnotationUtil.extractGenericTypeParametersSignature;
import static org.eclipse.jdt.core.util.ExternalAnnotationUtil.extractGenericTypeSignature;
import static org.eclipse.jdt.core.util.ExternalAnnotationUtil.getAnnotationFile;
import static org.eclipse.jdt.internal.ui.text.spelling.WordCorrectionProposal.getHtmlRepresentation;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
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
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Dimension;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
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
import org.eclipse.jdt.internal.ui.JavaUIStatus;
import org.eclipse.jdt.internal.ui.text.correction.ExternalNullAnnotationQuickAssistProcessor;
import org.eclipse.jdt.internal.ui.text.correction.IProposalRelevance;

/**
 * Proposals for null annotations that modify external annotations, rather than Java source files.
 *
 * @see <a href="https://bugs.eclipse.org/458200">[null] "Annotate" proposals for adding external
 *      null annotations to library classes</a>
 * @since 3.11
 */
public class ExternalNullAnnotationChangeProposals {

	static final String CONSTRUCTOR_SELECTOR= "<init>"; //$NON-NLS-1$

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

		@Override
		public Point getSelection(IDocument document) {
			return null; // nothing to reveal in the current editor.
		}

		@Override
		public String getDisplayString() {
			return fLabel;
		}

		@Override
		public Image getImage() {
			return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_ANNOTATION);
		}

		@Override
		public IContextInformation getContextInformation() {
			return null;
		}

		@Override
		public void apply(IDocument document) {
			try {
				doAnnotateMember(new NullProgressMonitor());
			} catch (CoreException | IOException e) {
				JavaPlugin.log(e);
			}
		}

		@Override
		public int getRelevance() {
			return IProposalRelevance.CHANGE_METHOD;
		}

		@Override
		public String getCommandId() {
			return ExternalNullAnnotationQuickAssistProcessor.ANNOTATE_MEMBER_ID;
		}

		@Override
		public String getAdditionalProposalInfo() {
			StringBuilder buffer= new StringBuilder();
			buffer.append("<dl>"); //$NON-NLS-1$
			if (fSelector != null)
				buffer.append("<dt>").append(getHtmlRepresentation(fSelector)).append("</dt>"); //$NON-NLS-1$ //$NON-NLS-2$
			else
				buffer.append("<dt>class ").append(getHtmlRepresentation(fAffectedTypeName)).append("</dt>"); //$NON-NLS-1$ //$NON-NLS-2$
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

	static class FieldAnnotationRewriteProposal extends SignatureAnnotationChangeProposal {

		@Override
		protected void dryRun() {
			fDryRun= ExternalAnnotationUtil.annotateType(fCurrentAnnotated, fAnnotatedSignature, fMergeStrategy);
		}

		@Override
		protected void doAnnotateMember(IProgressMonitor monitor) throws CoreException, UnsupportedEncodingException, IOException {
			annotateMember(fAffectedTypeName, fAnnotationFile, fSelector, fSignature, fAnnotatedSignature, fMergeStrategy, monitor);
		}
	}

	static class TypeParameterAnnotationRewriteProposal extends SignatureAnnotationChangeProposal {

		int fParamIdx;

		TypeParameterAnnotationRewriteProposal(int paramIdx) {
			fParamIdx= paramIdx;
		}

		@Override
		protected void dryRun() {
			fDryRun= ExternalAnnotationUtil.annotateTypeParameter(fCurrentAnnotated, fAnnotatedSignature, fParamIdx, fMergeStrategy);
		}

		@Override
		protected void doAnnotateMember(IProgressMonitor monitor) throws CoreException, IOException {
			annotateMethodTypeParameter(fAffectedTypeName, fAnnotationFile, fSelector, fSignature, fAnnotatedSignature, fParamIdx, fMergeStrategy, monitor);
		}
	}

	static class MissingBindingException extends RuntimeException {
		private static final long serialVersionUID= 1L;
		ASTNode fNode;
		MissingBindingException(ASTNode/*Type or TypeParameter or MethodDeclaration*/ node) {
			fNode= node;
		}
		@Override
		public String getMessage() {
			// check if compilation may have been aborted due to classpath trouble / unreportable problem:
			ASTNode cu= ASTNodes.getParent(fNode, ASTNode.COMPILATION_UNIT);
			if (cu instanceof CompilationUnit) {
				for (IProblem problem : ((CompilationUnit) cu).getProblems()) {
					if (problem.getID() == IProblem.IsClassPathCorrect || problem.getOriginatingFileName() == null)
						return problem.getMessage();
				}
			}
			switch (fNode.getNodeType()) {
				case ASTNode.METHOD_DECLARATION:
					return "Could not resolve method "+fNode.toString(); //$NON-NLS-1$
				case ASTNode.VARIABLE_DECLARATION_FRAGMENT:
					return "Could not resolve field "+fNode.toString(); //$NON-NLS-1$
				default:
					return "Could not resolve type "+fNode.toString(); //$NON-NLS-1$
			}
		}
	}

	static ITypeBinding resolveBinding(TypeParameter type) {
		ITypeBinding binding= type.resolveBinding();
		if (binding == null || binding.isRecovered()) throw new MissingBindingException(type);
		return binding;
	}

	static ITypeBinding resolveBinding(Type type) {
		ITypeBinding binding= type.resolveBinding();
		if (binding == null || binding.isRecovered()) throw new MissingBindingException(type);
		return binding;
	}

	static IMethodBinding resolveBinding(MethodDeclaration method) {
		IMethodBinding binding= method.resolveBinding();
		if (binding == null || binding.isRecovered()) throw new MissingBindingException(method);
		return binding;
	}

	static IVariableBinding resolveBinding(VariableDeclaration variable) {
		IVariableBinding binding= variable.resolveBinding();
		if (binding == null || binding.isRecovered()) throw new MissingBindingException(variable);
		return binding;
	}

	static ITypeBinding resolveBinding(TypeDeclaration type) {
		ITypeBinding binding= type.resolveBinding();
		if (binding == null || binding.isRecovered()) throw new MissingBindingException(type);
		return binding;
	}

	/* Quick assist on class file, propose changes on any type detail. */
	public static void collectExternalAnnotationProposals(ICompilationUnit cu, ASTNode coveringNode, int offset, List<IJavaCompletionProposal> resultingCollection) {

		IJavaProject javaProject= cu.getJavaProject();
		if (JavaCore.DISABLED.equals(javaProject.getOption(JavaCore.COMPILER_ANNOTATION_NULL_ANALYSIS, true)))
			return;

		if (!hasAnnotationPathInWorkspace(javaProject, cu)) // refuse to update files outside the workspace
			return;

		ASTNode inner= null; // the innermost type or type parameter node (to be annotated, unless annotating a dimension)
		ASTNode outer= null; // will become the outermost type or type parameter node (to be traversed)
		SingleVariableDeclaration variable= null; // when annotating extra dimension or varars this is where we get that additional info from
		boolean annotateVarargs= false;
		int extraDims= 0; // total number of extra dimensions
		int outerExtraDims= 0; // number of outer extra dimension preceding the annotation position

		boolean coversDimension= coveringNode instanceof Dimension;
		if (coversDimension && coveringNode.getLocationInParent() == SingleVariableDeclaration.EXTRA_DIMENSIONS2_PROPERTY) {
			// annotating extra dimensions, remember dimension counts
			variable= (SingleVariableDeclaration) coveringNode.getParent();
			outer= variable.getType();
			inner= variable.getType();
			List<?> extraDimensions= variable.extraDimensions();
			extraDims= extraDimensions.size();
			outerExtraDims= extraDimensions.indexOf(coveringNode);
		} else if (coversDimension && coveringNode.getLocationInParent() == MethodDeclaration.EXTRA_DIMENSIONS2_PROPERTY) {
			// annotating extra dimensions, remember dimension counts
			MethodDeclaration method= (MethodDeclaration) coveringNode.getParent();
			outer= method.getReturnType2();
			inner= method.getReturnType2();
			List<?> extraDimensions= method.extraDimensions();
			extraDims= extraDimensions.size();
			outerExtraDims= extraDimensions.indexOf(coveringNode);
		} else if (coveringNode instanceof SingleVariableDeclaration) {
			// annotating varargs ellipsis?
			variable= (SingleVariableDeclaration) coveringNode;
			outer= variable.getType();
			inner= variable.getType();
			if (variable.isVarargs()) {
				Type type= variable.getType();
				if (offset < type.getStartPosition()+type.getLength())
					return;
				if (offset+3 > variable.getName().getStartPosition())
					return;
				annotateVarargs= true;
			} else {
				return;
			}
		} else {
			// annotating 'normal' type?
			while (true) {
				if (coveringNode instanceof Type || coveringNode instanceof TypeParameter) {
					inner= coveringNode;
					break;
				}
				coveringNode= coveringNode.getParent();
				if (coveringNode == null)
					return;
			}
			if (inner == null || inner.getNodeType() == ASTNode.PRIMITIVE_TYPE)
				return; // cannot be annotated
			outer= inner;
			ASTNode next;
			while (((next= outer.getParent()) instanceof Type) || (next instanceof TypeParameter))
				outer= next;
		}
		// may still need to detect varags:
		ASTNode currentNode = coveringNode;
		while (variable == null && currentNode instanceof Type) {
			currentNode = currentNode.getParent();
			if (currentNode instanceof SingleVariableDeclaration) {
				variable = (SingleVariableDeclaration) currentNode;
			}
		}

		// prepare three renderers for three proposals:
		ASTNode typeToAnnotate = (!annotateVarargs && extraDims == 0) ? inner : null;
		TypeRenderer rendererNonNull= new TypeRenderer(typeToAnnotate, offset, NONNULL);
		TypeRenderer rendererNullable= new TypeRenderer(typeToAnnotate, offset, NULLABLE);
		TypeRenderer rendererRemove= new TypeRenderer(typeToAnnotate, offset, NO_ANNOTATION);

		if (variable != null) {
			// prepend dimensions which are not covered by type traversal below
			if (variable.isVarargs()) {
				rendererNonNull.addDimension(annotateVarargs);
				rendererNullable.addDimension(annotateVarargs);
				rendererRemove.addDimension(annotateVarargs);
			}
		}
		for (int i= 0; i < extraDims; i++) {
			rendererNonNull.addDimension(i == outerExtraDims);
			rendererNullable.addDimension(i == outerExtraDims);
			rendererRemove.addDimension(i == outerExtraDims);
		}
		boolean useJava8= JavaModelUtil.is1d8OrHigher(javaProject.getOption(JavaCore.COMPILER_SOURCE, true));
		if (!useJava8 && (outer != inner || outerExtraDims > 0)) { // below 1.8 we can only annotate the top type (not type parameter)
			// still need to handle ParameterizedType (outer) with SimpleType (inner)
			if ((outer.getNodeType() != ASTNode.PARAMETERIZED_TYPE)
					|| (inner.getParent() != outer))
				return;
		}
		try {
			if (outer instanceof Type) {
				if (extraDims == 0 && !annotateVarargs) {
					ITypeBinding typeBinding= resolveBinding((Type) outer);
					if (typeBinding.isPrimitive())
						return;
				}
			}
			outer.accept(rendererNonNull);
			outer.accept(rendererNullable);
			outer.accept(rendererRemove);

			StructuralPropertyDescriptor locationInParent= outer.getLocationInParent();
			ProposalCreator creator= null;
			if (locationInParent == MethodDeclaration.RETURN_TYPE2_PROPERTY) {
				MethodDeclaration method= ASTNodes.getParent(coveringNode, MethodDeclaration.class);
				creator= new ReturnProposalCreator(cu, resolveBinding(method));
			} else if (locationInParent == SingleVariableDeclaration.TYPE_PROPERTY) {
				ASTNode param= outer.getParent();
				if (param.getLocationInParent() == MethodDeclaration.PARAMETERS_PROPERTY) {
					MethodDeclaration method= ASTNodes.getParent(coveringNode, MethodDeclaration.class);
					int paramIdx= method.parameters().indexOf(param);
					if (paramIdx != -1)
						creator= new ParameterProposalCreator(cu, resolveBinding(method), paramIdx);
				}
			} else if (locationInParent == FieldDeclaration.TYPE_PROPERTY) {
				FieldDeclaration field= ASTNodes.getParent(coveringNode, FieldDeclaration.class);
				if (field.fragments().size() > 0) {
					VariableDeclarationFragment fragment= (VariableDeclarationFragment) field.fragments().get(0);
					creator= new FieldProposalCreator(cu, resolveBinding(fragment));
				}
			} else if (locationInParent == MethodDeclaration.TYPE_PARAMETERS_PROPERTY) {
				MethodDeclaration method= ASTNodes.getParent(coveringNode, MethodDeclaration.class);
				creator= new TypeParameterProposalCreator(cu, resolveBinding(method), method.typeParameters().indexOf(outer));
			} else if (locationInParent == TypeDeclaration.TYPE_PARAMETERS_PROPERTY) {
				TypeDeclaration type= ASTNodes.getParent(coveringNode, TypeDeclaration.class);
				creator= new TypeParameterProposalCreator(cu, resolveBinding(type), type.typeParameters().indexOf(outer));
			}
			if (creator != null) {
				createProposalsForType(cu, inner, extraDims, outerExtraDims, annotateVarargs, offset,
						rendererNonNull, rendererNullable, rendererRemove, creator, resultingCollection);
			}
		} catch (MissingBindingException mbe) {
			JavaPlugin.log(JavaUIStatus.createError(IStatus.ERROR, "Error during computation of Annotate proposals: "+mbe.getMessage(), mbe)); //$NON-NLS-1$
			return;
		}
	}

	public static boolean hasAnnotationPathInWorkspace(IJavaProject javaProject, IJavaElement element) {
		IPackageFragmentRoot root= (IPackageFragmentRoot) element.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
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
			super(cu, methodBinding.getDeclaringClass(),
					methodBinding.isConstructor() ? CONSTRUCTOR_SELECTOR : methodBinding.getName(),
					extractGenericSignature(methodBinding));
			fParamIdx= paramIdx;
		}

		@Override
		SignatureAnnotationChangeProposal doCreate(String annotatedSignature, String label) {
			return new ParameterAnnotationRewriteProposal(fParamIdx);
		}
	}

	private static class FieldProposalCreator extends ProposalCreator {

		FieldProposalCreator(ICompilationUnit cu, IVariableBinding fieldBinding) {
			super(cu, fieldBinding.getDeclaringClass(), fieldBinding.getName(), extractGenericTypeSignature(fieldBinding.getType()));
		}

		@Override
		SignatureAnnotationChangeProposal doCreate(String annotatedSignature, String label) {
			return new FieldAnnotationRewriteProposal();
		}
	}

	private static class TypeParameterProposalCreator extends ProposalCreator {
		int fParamIdx;

		TypeParameterProposalCreator(ICompilationUnit cu, IMethodBinding methodBinding, int paramIdx) {
			super(cu, methodBinding.getDeclaringClass(),
					methodBinding.isConstructor() ? CONSTRUCTOR_SELECTOR : methodBinding.getName(),
					extractGenericSignature(methodBinding));
			fParamIdx= paramIdx;
		}

		TypeParameterProposalCreator(ICompilationUnit cu, ITypeBinding typeBinding, int paramIdx) {
			super(cu, typeBinding,
					null,
					extractGenericTypeParametersSignature(typeBinding));
			fParamIdx= paramIdx;
		}

		@Override
		SignatureAnnotationChangeProposal doCreate(String annotatedSignature, String label) {
			return new TypeParameterAnnotationRewriteProposal(fParamIdx);
		}
	}

	/* Create one proposal from each of the three given renderers. */
	static void createProposalsForType(ICompilationUnit cu, ASTNode type, int dims,
			int outerDims, boolean annotateVarargs, int offset,
			TypeRenderer rendererNonNull, TypeRenderer rendererNullable, TypeRenderer rendererRemove, ProposalCreator creator, List<IJavaCompletionProposal> resultingCollection) {
		SignatureAnnotationChangeProposal operation;
		String label;
		// propose adding @NonNull:
		label= getAddAnnotationLabel(NullAnnotationsFix.getNonNullAnnotationName(cu, true), type, dims, outerDims, annotateVarargs, offset);
		operation= creator.create(rendererNonNull.getResult(), label);
		if (operation != null)
			resultingCollection.add(operation);

		// propose adding @Nullable:
		label= getAddAnnotationLabel(NullAnnotationsFix.getNullableAnnotationName(cu, true), type, dims, outerDims, annotateVarargs, offset);
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

	static String getAddAnnotationLabel(String annotationName, ASTNode type, int dims, int outerDims, boolean annotateVarargs, int offset) {
		StringBuilder left= null;
		StringBuilder dimsRight= null;
		if (type.getNodeType() == ASTNode.ARRAY_TYPE) {
			// find the insertion point using the text offset:
			ArrayType arrayType= (ArrayType) type;
			left= new StringBuilder(arrayType.getElementType().toString());
			dimsRight= new StringBuilder();
			for (Object dimension2 : arrayType.dimensions()) {
				Dimension dimension= (Dimension) dimension2;
				if (dimension.getStartPosition() + dimension.getLength() <= offset)
					left.append("[]"); //$NON-NLS-1$
				else
					dimsRight.append("[]"); //$NON-NLS-1$
			}
		} else if (dims > 0) {
			// find then insertion point using the dimension counts:
			left= new StringBuilder(type.toString());
			dimsRight= new StringBuilder();
			for (int i= 0; i < dims; i++) {
				if (i < outerDims)
					left.append("[]"); //$NON-NLS-1$
				else
					dimsRight.append("[]"); //$NON-NLS-1$
			}
		} else if (annotateVarargs) {
			left = new StringBuilder(type.toString());
			dimsRight = new StringBuilder();
		}
		if (left != null && dimsRight != null) {
			if (annotateVarargs)
				dimsRight.append("..."); //$NON-NLS-1$
			// need to assemble special format with annotation attached to the selected dimension:
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
			for (Object dimension2 : arrayType.dimensions()) {
				Dimension dimension= (Dimension) dimension2;
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

		public void addDimension(boolean annotate) {
			fBuffer.append('[');
			if (annotate)
				fBuffer.append(fAnnotation);
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
			fBuffer.append(binaryName(resolveBinding(type)));
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
			List<?> dimensions= array.dimensions();
			boolean annotated= false;
			for (Object dimension2 : dimensions) {
				fBuffer.append('[');
				Dimension dimension= (Dimension) dimension2;
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
				if (resolveBinding(typeBound).isClass()) {
					classBound= typeBound;
					break;
				}
			}
			boolean boundAdded= false;
			fBuffer.append(':');
			if (classBound != null) {
				classBound.accept(this);
				boundAdded= true;
			}
			for (Object bound : parameter.typeBounds()) {
				if (bound == classBound)
					continue;
				Type typeBound= (Type) bound;
				fBuffer.append(':');
				typeBound.accept(this);
				boundAdded= true;
			}
			if (!boundAdded) {
				fBuffer.append("Ljava/lang/Object;"); //$NON-NLS-1$
			}
			return false;
		}

		@Override
		public boolean visit(SimpleType type) {
			ITypeBinding typeBinding= resolveBinding(type);
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
			fBuffer.append(resolveBinding(node).getBinaryName());
			return false;
		}

		String binaryName(ITypeBinding type) {
			return type.getBinaryName().replace('.', '/');
		}
	}

	private ExternalNullAnnotationChangeProposals() {
	}
}
