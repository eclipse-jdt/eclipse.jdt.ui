/*******************************************************************************
 * Copyright (c) 2026 Carsten Hammer and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.packageview;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.swt.widgets.Display;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

public class StartupClasspathContainerInitializer extends ClasspathContainerInitializer {
	static final IPath PATH= new Path("org.eclipse.jdt.ui.tests.startupContainer");
	static final List<String> UI_CALLS= new CopyOnWriteArrayList<>();
	static final AtomicInteger CALLS= new AtomicInteger();
	static final AtomicBoolean TIMED_OUT= new AtomicBoolean();
	static volatile String watchedProject;
	static volatile CountDownLatch entered;
	static volatile CountDownLatch release;

	@Override
	public void initialize(IPath containerPath, IJavaProject project) throws CoreException {
		if (project.getElementName().equals(watchedProject)) {
			CALLS.incrementAndGet();
			if (Display.getCurrent() != null) {
				StringBuilder trace= new StringBuilder();
				for (StackTraceElement frame : Thread.currentThread().getStackTrace())
					trace.append(frame).append('\n');
				UI_CALLS.add(trace.toString());
			}
			CountDownLatch started= entered;
			CountDownLatch gate= release;
			if (started != null)
				started.countDown();
			if (gate != null) {
				try {
					if (!gate.await(10, TimeUnit.SECONDS))
						TIMED_OUT.set(true);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					TIMED_OUT.set(true);
				}
			}
		}
		IClasspathContainer container= new IClasspathContainer() {
			@Override
			public IClasspathEntry[] getClasspathEntries() {
				return new IClasspathEntry[0];
			}
			@Override
			public String getDescription() {
				return "Startup test container";
			}
			@Override
			public int getKind() {
				return K_APPLICATION;
			}
			@Override
			public IPath getPath() {
				return containerPath;
			}
		};
		JavaCore.setClasspathContainer(containerPath, new IJavaProject[] { project }, new IClasspathContainer[] { container }, null);
	}
}
