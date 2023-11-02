/*******************************************************************************
 * Copyright (c) 2023 Andrey Loskutov and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Andrey Loskutov - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.bcoview.asm;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.objectweb.asm.Attribute;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.ASMifier;
import org.objectweb.asm.util.TraceMethodVisitor;

import org.eclipse.jdt.bcoview.preferences.BCOConstants;

public class CommentedASMifierClassVisitor extends ASMifier implements ICommentedClassVisitor {

	protected final boolean showLines;

	protected final boolean showLocals;

	protected final boolean showStackMap;

	private final DecompilerOptions options;

	private JavaVersion javaVersion;

	private int accessFlags;

	private LabelNode currentLabel;

	private int currentInsn;

	private ASMifier dummyAnnVisitor;

	private DecompiledMethod currMethod;

	private String className;

	private final ClassNode classNode;

	private CommentedASMifierClassVisitor(ClassNode classNode, final DecompilerOptions options, String name, int id) {
		super(DecompilerOptions.LATEST_ASM_VERSION, name, id);
		this.classNode = classNode;
		this.options = options;
		showLines = options.modes.get(BCOConstants.F_SHOW_LINE_INFO);
		showLocals = options.modes.get(BCOConstants.F_SHOW_VARIABLES);
		showStackMap = options.modes.get(BCOConstants.F_SHOW_STACKMAP);
	}

	public CommentedASMifierClassVisitor(ClassNode classNode, final DecompilerOptions options) {
		this(classNode, options, "cw", 0); //$NON-NLS-1$
	}


	@Override
	protected ASMifier createASMifier(String name1, int id1) {
		CommentedASMifierClassVisitor classVisitor = new CommentedASMifierClassVisitor(classNode, options, name1, id1);
		classVisitor.currMethod = currMethod;
		return classVisitor;
	}

	private void addIndex(final int opcode) {
		text.add(new Index(currentLabel, currentInsn++, opcode));
	}

	void setCurrentLabel(LabelNode currentLabel) {
		this.currentLabel = currentLabel;
	}

	private boolean decompilingEntireClass() {
		return options.methodFilter == null && options.fieldFilter == null;
	}

	@Override
	public void visit(int version, int access, String name1, String signature, String superName, String[] interfaces) {
		if (decompilingEntireClass()) {
			super.visit(version, access, name1, signature, superName, interfaces);
		}
		this.className = name;
		javaVersion = new JavaVersion(version);
		this.accessFlags = access;
	}

	@Override
	public ASMifier visitClassAnnotation(String desc, boolean visible) {
		if (decompilingEntireClass()) {
			return super.visitClassAnnotation(desc, visible);
		}
		return getDummyVisitor();
	}

	@Override
	public void visitClassAttribute(Attribute attr) {
		if (decompilingEntireClass()) {
			super.visitClassAttribute(attr);
		}
	}

	@Override
	public void visitClassEnd() {
		if (decompilingEntireClass()) {
			super.visitClassEnd();
		}
	}

	@Override
	public void visitInnerClass(String name1, String outerName, String innerName, int access) {
		if (decompilingEntireClass()) {
			super.visitInnerClass(name1, outerName, innerName, access);
		}
	}

	@Override
	public void visitOuterClass(String owner, String name1, String desc) {
		if (decompilingEntireClass()) {
			super.visitOuterClass(owner, name1, desc);
		}
	}

	@Override
	public void visitSource(String file, String debug) {
		if (decompilingEntireClass()) {
			super.visitSource(file, debug);
		}
	}

	@Override
	public ASMifier visitMethod(int access, String name1, String desc, String signature, String[] exceptions) {
		if (options.fieldFilter != null || options.methodFilter != null && !(name1 + desc).equals(options.methodFilter)) {
			return getDummyVisitor();
		}

		MethodNode meth = null;
		List<String> exList = Arrays.asList(exceptions);
		for (MethodNode mn : classNode.methods) {
			if (mn.name.equals(name1) && mn.desc.equals(desc) && mn.exceptions.equals(exList)) {
				meth = mn;
				break;
			}
		}
		assert meth != null;

		currMethod = new DecompiledMethod(className, new HashMap<>(), meth, options, access);
		ASMifier textifier = super.visitMethod(access, name1, desc, signature, exceptions);
		TraceMethodVisitor tm = new TraceMethodVisitor(textifier);
		meth.accept(tm);

		Object methodEnd = text.remove(text.size() - 1);
		Object methodtext = text.remove(text.size() - 1);
		currMethod.setText((List<?>) methodtext);
		text.add(currMethod);
		text.add(methodEnd);
		return textifier;
	}

	@Override
	public ASMifier visitField(int access, String name1, String desc, String signature, Object value) {
		if (options.methodFilter != null) {
			return getDummyVisitor();
		}
		if (options.fieldFilter != null && !name1.equals(options.fieldFilter)) {
			return getDummyVisitor();
		}
		return super.visitField(access, name1, desc, signature, value);
	}

	@Override
	public void visitFieldInsn(final int opcode, final String owner1, final String name1, final String desc) {
		addIndex(opcode);
		super.visitFieldInsn(opcode, owner1, name1, desc);
	}


	@Override
	public void visitFrame(final int type, final int nLocal, final Object[] local, final int nStack, final Object[] stack) {
		if (showStackMap) {
			addIndex(-1);
			super.visitFrame(type, nLocal, local, nStack, stack);
		}
	}

	@Override
	public void visitInsn(final int opcode) {
		addIndex(opcode);
		super.visitInsn(opcode);
	}

	@Override
	public void visitIntInsn(int opcode, int operand) {
		addIndex(opcode);
		super.visitIntInsn(opcode, operand);
	}


	@Override
	public void visitJumpInsn(final int opcode, final Label label) {
		addIndex(opcode);
		super.visitJumpInsn(opcode, label);
	}

	@Override
	public void visitLabel(Label label) {
		addIndex(-1);
		super.visitLabel(label);

		InsnList instructions = currMethod.meth.instructions;
		LabelNode currLabel = null;
		for (int i = 0; i < instructions.size(); i++) {
			AbstractInsnNode insnNode = instructions.get(i);
			if (insnNode instanceof LabelNode) {
				LabelNode labelNode = (LabelNode) insnNode;
				if (labelNode.getLabel() == label) {
					currLabel = labelNode;
				}
			}
		}
		setCurrentLabel(currLabel);
	}

	@Override
	public void visitLdcInsn(final Object cst) {
		addIndex(Opcodes.LDC);
		super.visitLdcInsn(cst);
	}

	@Override
	public void visitInvokeDynamicInsn(String name1, String desc, Handle bsm, Object... bsmArgs) {
		addIndex(Opcodes.INVOKEDYNAMIC);
		super.visitInvokeDynamicInsn(name1, desc, bsm, bsmArgs);
	}

	@Override
	public void visitIincInsn(final int var, final int increment) {
		addIndex(Opcodes.IINC);
		super.visitIincInsn(var, increment);
	}

	@Override
	public void visitLineNumber(int line, Label start) {
		if (showLines) {
			addIndex(-1);
			currMethod.addLineNumber(start, Integer.valueOf(line));
			super.visitLineNumber(line, start);
		}
	}

	@Override
	public void visitLookupSwitchInsn(final Label dflt, final int[] keys, final Label[] labels) {
		addIndex(Opcodes.LOOKUPSWITCH);
		super.visitLookupSwitchInsn(dflt, keys, labels);
	}

	@Override
	public void visitLocalVariable(String name1, String desc, String signature, Label start, Label end, int index) {
		if (showLocals) {
			super.visitLocalVariable(name1, desc, signature, start, end, index);
		}
	}

	@Override
	public void visitMaxs(int maxStack, int maxLocals) {
		super.visitMaxs(maxStack, maxLocals);
	}

	@Override
	public void visitMethodInsn(final int opcode, final String owner, final String name1, final String desc, boolean itf) {
		addIndex(opcode);
		super.visitMethodInsn(opcode, owner, name1, desc, itf);
	}

	@Override
	public void visitMultiANewArrayInsn(final String desc, final int dims) {
		addIndex(Opcodes.MULTIANEWARRAY);
		super.visitMultiANewArrayInsn(desc, dims);
	}

	@Override
	public void visitTableSwitchInsn(final int min, final int max, final Label dflt, final Label... labels) {
		addIndex(Opcodes.TABLESWITCH);
		super.visitTableSwitchInsn(min, max, dflt, labels);
	}

	@Override
	public void visitTypeInsn(final int opcode, final String desc) {
		addIndex(opcode);
		super.visitTypeInsn(opcode, desc);
	}

	@Override
	public void visitVarInsn(final int opcode, final int var) {
		addIndex(opcode);
		super.visitVarInsn(opcode, var);
	}

	@Override
	public DecompiledClassInfo getClassInfo() {
		return new DecompiledClassInfo(javaVersion, accessFlags);
	}

	private ASMifier getDummyVisitor() {
		if (dummyAnnVisitor == null) {
			dummyAnnVisitor = new ASMifier(DecompilerOptions.LATEST_ASM_VERSION, "", -1) { //$NON-NLS-1$
				@Override
				public void visitAnnotationEnd() {
					text.clear();
				}

				@Override
				public void visitClassEnd() {
					text.clear();
				}

				@Override
				public void visitFieldEnd() {
					text.clear();
				}

				@Override
				public void visitMethodEnd() {
					text.clear();
				}
			};
		}
		return dummyAnnVisitor;
	}
}
