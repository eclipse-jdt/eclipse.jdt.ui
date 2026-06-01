/*******************************************************************************
 * Copyright (c) 2026 SAP SE and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javaeditor.codemining;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Provides a dedicated {@link Executor} for Java code mining resolution.
 * <p>
 * Code mining resolve tasks issue blocking JDT search calls. Running them on
 * {@link java.util.concurrent.ForkJoinPool#commonPool()} can starve the JDT indexer (which itself
 * submits work to the common pool), leading to a deadlock between code mining workers and the
 * indexer. Routing these tasks through a separate, fixed-size pool keeps them off the common pool.
 * <p>
 * The pool's thread count is capped (2-4 threads); the work queue itself is unbounded. This is
 * acceptable because the number of in-flight code mining tasks is naturally limited by the visible
 * minings in open editors, and superseded resolutions are cancelled by the platform.
 */
public final class JavaCodeMiningExecutor {

	private static ExecutorService instance;

	private JavaCodeMiningExecutor() {
	}

	public static synchronized Executor get() {
		if (instance == null || instance.isShutdown()) {
			instance= createExecutor();
		}
		return instance;
	}

	/**
	 * Shuts down the executor. Called from {@code JavaPlugin#stop} so that worker threads do not
	 * outlive the plug-in across bundle restarts/updates. A subsequent {@link #get()} call will
	 * create a fresh executor, so the bundle can be restarted within the same JVM.
	 */
	public static synchronized void shutdown() {
		if (instance != null) {
			instance.shutdownNow();
			instance= null;
		}
	}

	private static ExecutorService createExecutor() {
		int parallelism= Math.max(2, Math.min(4, Runtime.getRuntime().availableProcessors() / 2));
		ThreadFactory factory= new ThreadFactory() {
			private final AtomicInteger counter= new AtomicInteger(1);

			@Override
			public Thread newThread(Runnable r) {
				Thread t= new Thread(r, "Java Code Mining Worker-" + counter.getAndIncrement()); //$NON-NLS-1$
				t.setDaemon(true);
				t.setPriority(Thread.NORM_PRIORITY - 1);
				return t;
			}
		};
		return Executors.newFixedThreadPool(parallelism, factory);
	}
}
