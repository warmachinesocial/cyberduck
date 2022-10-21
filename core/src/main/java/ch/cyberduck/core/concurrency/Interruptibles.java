package ch.cyberduck.core.concurrency;

/*
 * Copyright (c) 2002-2022 iterate GmbH. All rights reserved.
 * https://cyberduck.io/
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.ConnectionCanceledException;
import ch.cyberduck.core.threading.CancelCallback;
import ch.cyberduck.core.worker.DefaultExceptionMappingService;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.base.Throwables;

import static java.lang.Thread.currentThread;

public class Interruptibles {
    private static final Logger log = LogManager.getLogger(Interruptibles.class);

    /**
     * Await count down and reset interrupt flag on thread prior throwing checked exception
     *
     * @param latch     Count down
     * @param throwable Checked exception type
     * @param <E>       Type of exception
     * @throws E Exception type when interrupted
     */
    public static <E extends Throwable> void await(final CountDownLatch latch, final Class<E> throwable) throws E {
        await(latch, throwable, CancelCallback.noop);
    }

    public static <E extends Throwable> void await(final CountDownLatch latch, Class<E> throwable, final CancelCallback cancel) throws E {
        try {
            while(!latch.await(1, TimeUnit.SECONDS)) {
                try {
                    cancel.verify();
                }
                catch(ConnectionCanceledException e) {
                    throw ExceptionUtils.throwableOfType(e, throwable);
                }
            }
        }
        catch(InterruptedException e) {
            final Thread thread = currentThread();
            if(log.isWarnEnabled()) {
                log.warn(String.format("Interrupted %s while waiting for %s", thread, latch));
            }
            thread.interrupt();
            throw ExceptionUtils.throwableOfType(e, throwable);
        }
    }

    public static <T, E extends BackgroundException> T await(final Future<T> future, final Class<E> throwable) throws BackgroundException {
        return await(future, throwable, CancelCallback.noop);
    }

    public static <T, E extends BackgroundException> T await(final Future<T> future, final Class<E> throwable,
                                                             final CancelCallback cancel) throws BackgroundException {
        try {
            while(true) {
                try {
                    return future.get(1L, TimeUnit.SECONDS);
                }
                catch(TimeoutException e) {
                    cancel.verify();
                }
            }
        }
        catch(ExecutionException e) {
            if(log.isWarnEnabled()) {
                log.warn(String.format("Execution of %s failed with %s", future, e));
            }
            Throwables.throwIfInstanceOf(Throwables.getRootCause(e), BackgroundException.class);
            throw new DefaultExceptionMappingService().map(Throwables.getRootCause(e));
        }
        catch(InterruptedException e) {
            final Thread thread = currentThread();
            if(log.isWarnEnabled()) {
                log.warn(String.format("Interrupted %s while waiting for %s", thread, future));
            }
            thread.interrupt();
            throw ExceptionUtils.throwableOfType(e, throwable);
        }
    }

    public static <T, E extends BackgroundException> List<T> awaitAll(final List<Future<T>> futures, final Class<E> throwable) throws BackgroundException {
        return awaitAll(futures, throwable, CancelCallback.noop);
    }

    public static <T, E extends BackgroundException> List<T> awaitAll(final List<Future<T>> futures, final Class<E> throwable,
                                                                      final CancelCallback cancel) throws BackgroundException {
        final List<T> results = new ArrayList<>();
        final AtomicReference<ConnectionCanceledException> canceled = new AtomicReference<>();
        for(Future<T> f : futures) {
            try {
                results.add(await(f, throwable, cancel));
            }
            catch(ConnectionCanceledException e) {
                canceled.set(e);
            }
        }
        if(canceled.get() != null) {
            throw canceled.get();
        }
        return results;
    }

    public static class ThreadAliveCancelCallback implements CancelCallback {
        private final Thread parent;

        public ThreadAliveCancelCallback() {
            this(Thread.currentThread());
        }

        public ThreadAliveCancelCallback(final Thread parent) {
            this.parent = parent;
        }

        @Override
        public void verify() throws ConnectionCanceledException {
            if(!parent.isAlive()) {
                if(log.isWarnEnabled()) {
                    log.warn(String.format("Cancel waiting with parent thread %s dead", parent));
                }
                throw new ConnectionCanceledException();
            }
        }
    }
}
