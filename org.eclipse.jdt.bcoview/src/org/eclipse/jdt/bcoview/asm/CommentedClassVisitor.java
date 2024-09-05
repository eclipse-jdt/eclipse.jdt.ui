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
 *     Eric Bruneton - initial API and implementation
 *     Andrey Loskutov - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.bcoview.asm;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import org.objectweb.asm.Attribute;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

import org.eclipse.jdt.bcoview.preferences.BCOConstants;

public class CommentedClassVisitor extends Textifier implements ICommentedClassVisitor {

	protected final boolean raw;

	protected final boolean showLines;

	protected final boolean showLocals;

	protected final boolean showStackMap;

	protected final boolean showHex;

	private final DecompilerOptions options;

	private DecompiledMethod currMethod;

	private String className;

	private JavaVersion javaVersion;

	private int accessFlags;

	private Textifier dummyAnnVisitor;

	private final ClassNode classNode;

	private LabelNode currentLabel;

	private int currentInsn;

	public CommentedClassVisitor(ClassNode classNode, final DecompilerOptions options) {
		super(DecompilerOptions.LATEST_ASM_VERSION);
		this.classNode = classNode;
		this.options = options;
		raw = !options.modes.get(BCOConstants.F_SHOW_RAW_BYTECODE);
		showLines = options.modes.get(BCOConstants.F_SHOW_LINE_INFO);
		showLocals = options.modes.get(BCOConstants.F_SHOW_VARIABLES);
		showStackMap = options.modes.get(BCOConstants.F_SHOW_STACKMAP);
		showHex = options.modes.get(BCOConstants.F_SHOW_HEX_VALUES);
		javaVersion = new JavaVersion(0);
	}

	private boolean decompilingEntireClass() {
		return options.methodFilter == null && options.fieldFilter == null;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		if (decompilingEntireClass()) {
			super.visit(version, access, name, signature, superName, interfaces);
		}
		this.className = name;
		javaVersion = new JavaVersion(version);
		this.accessFlags = access;
	}

	@Override
	public Textifier visitClassAnnotation(String desc, boolean visible) {
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
	public Textifier visitField(int access, String name, String desc, String signature, Object value) {
		if (options.methodFilter != null) {
			return getDummyVisitor();
		}
		if (options.fieldFilter != null && !name.equals(options.fieldFilter)) {
			return getDummyVisitor();
		}
		return super.visitField(access, name, desc, signature, value);
	}

	@Override
	public void visitInnerClass(String name, String outerName, String innerName, int access) {
		if (decompilingEntireClass()) {
			super.visitInnerClass(name, outerName, innerName, access);
		}
	}

	@Override
	public void visitOuterClass(String owner, String name, String desc) {
		if (decompilingEntireClass()) {
			super.visitOuterClass(owner, name, desc);
		}
	}

	@Override
	public void visitSource(String file, String debug) {
		if (decompilingEntireClass()) {
			super.visitSource(file, debug);
		}
	}

	@Override
	public Textifier visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		if (options.fieldFilter != null || options.methodFilter != null && !(name + desc).equals(options.methodFilter)) {
			return getDummyVisitor();
		}

		MethodNode meth = null;
		List<String> exList = Arrays.asList(exceptions);
		for (MethodNode mn : classNode.methods) {
			if (mn.name.equals(name) && mn.desc.equals(desc) && mn.exceptions.equals(exList)) {
				meth = mn;
				break;
			}
		}
		Objects.requireNonNull(meth);

		currMethod = new DecompiledMethod(className, new HashMap<>(), meth, options, access);
		Textifier textifier = super.visitMethod(access, name, desc, signature, exceptions);
		TraceMethodVisitor tm = new TraceMethodVisitor(textifier);
		meth.accept(tm);

		Object methodtext = text.remove(text.size() - 1);
		currMethod.setText((List<?>) methodtext);
		text.add(currMethod);
		return textifier;
	}

	@Override
	protected void appendDescriptor(int type, String desc) {
		appendDescriptor(stringBuilder, type, desc, raw);
	}

	protected void appendDescriptor(StringBuilder buf1, int type, String desc, boolean raw1) {
		if (desc == null) {
			return;
		}
		if (raw1) {
			if (type == CLASS_SIGNATURE || type == FIELD_SIGNATURE || type == METHOD_SIGNATURE) {
				buf1.append("// signature ").append(desc).append('\n'); //$NON-NLS-1$
			} else {
				buf1.append(desc);
			}
		} else {
			switch (type) {
				case INTERNAL_NAME:
					buf1.append(eatPackageNames(desc, '/'));
					break;
				case METHOD_DESCRIPTOR:
				case HANDLE_DESCRIPTOR:
					if (!desc.startsWith("(")) { //$NON-NLS-1$
						// Handle to access record fields
						buf1.append(getSimpleName(Type.getType(desc)));
					} else {
						buf1.append("("); //$NON-NLS-1$
						Type[] types = Type.getArgumentTypes(desc);
						for (int i = 0; i < types.length; ++i) {
							if (i > 0) {
								buf1.append(", "); //$NON-NLS-1$
							}
							buf1.append(getSimpleName(types[i]));
						}
						buf1.append(") : "); //$NON-NLS-1$
						Type returnType = Type.getReturnType(desc);
						buf1.append(getSimpleName(returnType));
					}
					break;
				case FIELD_DESCRIPTOR:
					switch (desc) {
						case "T": //$NON-NLS-1$
							buf1.append("top"); //$NON-NLS-1$
							break;
						case "N": //$NON-NLS-1$
							buf1.append("null"); //$NON-NLS-1$
							break;
						case "U": //$NON-NLS-1$
							buf1.append("uninitialized_this"); //$NON-NLS-1$
							break;
						default:
							buf1.append(getSimpleName(Type.getType(desc)));
							break;
					}
					break;

				case METHOD_SIGNATURE:
				case FIELD_SIGNATURE:
					// fine tuning of identation - we have two tabs in this case
					if (stringBuilder.lastIndexOf(tab) == stringBuilder.length() - tab.length()) {
						stringBuilder.delete(stringBuilder.lastIndexOf(tab), stringBuilder.length());
					}
					break;

				case CLASS_SIGNATURE:
					// ignore - show only in "raw" mode
					break;
				default:
					buf1.append(desc);
			}
		}
	}

	/**
	 * @param t non null
	 * @return simply class name without any package/outer class information
	 */
	public static String getSimpleName(Type t) {
		String name = t.getClassName();
		return eatPackageNames(name, '.');
	}

	/**
	 * @param name Java type name(s).
	 * @param separator package name separator
	 * @return simply class name(s) without any package/outer class information, but with "generics"
	 *         information from given name parameter.
	 */
	private static String eatPackageNames(String name, char separator) {
		int lastPoint = name.lastIndexOf(separator);
		if (lastPoint < 0) {
			return name;
		}
		StringBuffer sb = new StringBuffer(name);
		do {
			int start = getPackageStartIndex(sb, separator, lastPoint);
			sb.delete(start, lastPoint + 1);
			lastPoint = lastIndexOf(sb, separator, start);
		} while (lastPoint > 0);

		return sb.toString();
	}

	private static int lastIndexOf(StringBuffer chars, char c, int lastPoint) {
		for (int i = lastPoint - 1; i > 0; i--) {
			if (chars.charAt(i) == c) {
				return i;
			}
		}
		return -1;
	}

	private static int getPackageStartIndex(StringBuffer chars, char c, int firstPoint) {
		for (int i = firstPoint - 1; i >= 0; i--) {
			char curr = chars.charAt(i);
			if (curr != c && !Character.isJavaIdentifierPart(curr)) {
				return i + 1;
			}
		}
		return 0;
	}


	/**
	 * control chars names
	 */
	private static final String[] CHAR_NAMES = { "NUL", "SOH", "STX", "ETX", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			"EOT", "ENQ", "ACK", "BEL", "BS", "HT", "LF", "VT", "FF", "CR", "SO", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$
			"SI", "DLE", "DC1", "DC2", "DC3", "DC4", "NAK", "SYN", "ETB", "CAN", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$
			"EM", "SUB", "ESC", "FS", "GS", "RS", "US", // "Sp" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
	};

	private Index getIndex(Label label) {
		Index index;
		for (Object o : text) {
			if (o instanceof Index) {
				index = (Index) o;
				if (index.labelNode != null && index.labelNode.getLabel() == label) {
					return index;
				}
			}
		}
		return null;
	}

	@Override
	public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
		if (showStackMap) {
			addIndex(-1);
			super.visitFrame(type, nLocal, local, nStack, stack);
		}
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
		addIndex(opcode);
		stringBuilder.setLength(0);
		stringBuilder.append(tab2).append(OPCODES[opcode]).append(' ');
		appendDescriptor(INTERNAL_NAME, owner);
		stringBuilder.append('.').append(name);
		appendDescriptor(METHOD_DESCRIPTOR, desc);
		stringBuilder.append('\n');
		text.add(stringBuilder.toString());
	}

	@Override
	public void visitVarInsn(int opcode, int var) {
		addIndex(opcode);
		text.add(tab2 + OPCODES[opcode] + " " + var); //$NON-NLS-1$
		if (!raw) {
			text.add(Integer.valueOf(var));
		}
		text.add("\n"); //$NON-NLS-1$
	}

	@Override
	public void visitLabel(Label label) {
		addIndex(-1);
		stringBuilder.setLength(0);
		stringBuilder.append(ltab);
		appendLabel(label);
		Index index = getIndex(label);
		if (index != null) {
			stringBuilder.append(" (").append(index.insn).append(")"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		stringBuilder.append('\n');
		text.add(stringBuilder.toString());
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
	public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
		addIndex(Opcodes.INVOKEDYNAMIC);
		super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
	}

	@Override
	public void visitIincInsn(int var, int increment) {
		addIndex(Opcodes.IINC);
		text.add(tab2 + "IINC " + var); //$NON-NLS-1$
		if (!raw) {
			text.add(Integer.valueOf(var));
		}
		text.add(" " + increment + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public void visitIntInsn(int opcode, int operand) {
		addIndex(opcode);
		stringBuilder.setLength(0);
		stringBuilder.append(tab2).append(OPCODES[opcode]).append(' ').append(
				opcode == Opcodes.NEWARRAY
				? TYPES[operand]
						: formatValue(operand))
		.append('\n');
		text.add(stringBuilder.toString());
	}

	private String formatValue(int operand) {
		if (showHex) {
			String intStr = Integer.toHexString(operand).toUpperCase();
			return intStr + getAsCharComment(operand);
		}
		return Integer.toString(operand);
	}

	/**
	 * @param value some int
	 * @return char value from int, together with char name if it is a control char, or an empty
	 *         string
	 */
	private static String getAsCharComment(int value) {
		if (Character.MAX_VALUE < value || Character.MIN_VALUE > value) {
			return ""; //$NON-NLS-1$
		}
		StringBuffer sb = new StringBuffer("    // '"); //$NON-NLS-1$
		switch (value) {
			case '\t':
				sb.append("\\t"); //$NON-NLS-1$
				break;
			case '\r':
				sb.append("\\r"); //$NON-NLS-1$
				break;
			case '\n':
				sb.append("\\n"); //$NON-NLS-1$
				break;
			case '\f':
				sb.append("\\f"); //$NON-NLS-1$
				break;
			default:
				sb.append((char) value);
				break;
		}

		if (value >= CHAR_NAMES.length) {
			if (value == 127) {
				return sb.append("' (DEL)").toString(); //$NON-NLS-1$
			}
			return sb.append("'").toString(); //$NON-NLS-1$
		}
		return sb.append("' (").append(CHAR_NAMES[value]).append(")").toString(); //$NON-NLS-1$ //$NON-NLS-2$

	}

	private String formatValue(Object operand) {
		if (operand == null) {
			return "null"; //$NON-NLS-1$
		}
		if (showHex) {
			if (operand instanceof Integer) {
				String intStr = Integer.toHexString(((Integer) operand).intValue()).toUpperCase();
				return intStr + getAsCharComment(((Integer) operand).intValue());
			} else if (operand instanceof Long) {
				return Long.toHexString(((Long) operand).longValue()).toUpperCase();
			} else if (operand instanceof Double) {
				return Double.toHexString(((Double) operand).doubleValue());
			} else if (operand instanceof Float) {
				return Float.toHexString(((Float) operand).floatValue());
			}
		}
		return operand.toString();
	}

	@Override
	public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
		if (showLocals) {
			super.visitLocalVariable(name, desc, signature, start, end, index);
		}
	}

	@Override
	public void visitLdcInsn(Object cst) {
		addIndex(Opcodes.LDC);
		stringBuilder.setLength(0);
		stringBuilder.append(tab2).append("LDC "); //$NON-NLS-1$
		if (cst instanceof String) {
			Printer.appendString(stringBuilder, (String) cst);
		} else if (cst instanceof Type) {
			Type type = (Type) cst;
			String descriptor = type.getDescriptor();
			if (type.getSort() == Type.METHOD) {
				appendDescriptor(METHOD_DESCRIPTOR, descriptor);
			} else {
				String descr = raw ? descriptor : descriptor.substring(0, descriptor.length() - 1);
				appendDescriptor(INTERNAL_NAME, descr + ".class"); //$NON-NLS-1$
			}
		} else {
			stringBuilder.append(formatValue(cst));
		}
		stringBuilder.append('\n');
		text.add(stringBuilder.toString());
	}

	@Override
	public void visitMaxs(int maxStack, int maxLocals) {
		if (showLocals) {
			super.visitMaxs(maxStack, maxLocals);
		}
	}

	@Override
	public void visitInsn(int opcode) {
		addIndex(opcode);
		super.visitInsn(opcode);
	}

	@Override
	public void visitTypeInsn(int opcode, String desc) {
		addIndex(opcode);
		super.visitTypeInsn(opcode, desc);
	}

	@Override
	public void visitFieldInsn(int opcode, String owner1, String name, String desc) {
		addIndex(opcode);
		super.visitFieldInsn(opcode, owner1, name, desc);
	}

	@Override
	public void visitJumpInsn(int opcode, Label label) {
		addIndex(opcode);
		super.visitJumpInsn(opcode, label);
	}

	@Override
	public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
		addIndex(Opcodes.TABLESWITCH);
		super.visitTableSwitchInsn(min, max, dflt, labels);
	}

	@Override
	public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
		addIndex(Opcodes.LOOKUPSWITCH);
		super.visitLookupSwitchInsn(dflt, keys, labels);
	}

	@Override
	public void visitMultiANewArrayInsn(String desc, int dims) {
		addIndex(Opcodes.MULTIANEWARRAY);
		super.visitMultiANewArrayInsn(desc, dims);
	}

	@Override
	public void visitLineNumber(int line, Label start) {
		if (showLines) {
			addIndex(-1);
			currMethod.addLineNumber(start, Integer.valueOf(line));
			super.visitLineNumber(line, start);
		}
	}

	private void addIndex(int opcode) {
		text.add(new Index(currentLabel, currentInsn++, opcode));
	}

	void setCurrentLabel(LabelNode currentLabel) {
		this.currentLabel = currentLabel;
	}

	@Override
	protected Textifier createTextifier() {
		CommentedClassVisitor classVisitor = new CommentedClassVisitor(classNode, options);
		classVisitor.currMethod = currMethod;
		return classVisitor;
	}

	@Override
	public DecompiledClassInfo getClassInfo() {
		return new DecompiledClassInfo(javaVersion, accessFlags);
	}

	private Textifier getDummyVisitor() {
		if (dummyAnnVisitor == null) {
			dummyAnnVisitor = new Textifier(DecompilerOptions.LATEST_ASM_VERSION) {
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
