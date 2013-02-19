/*
 * Copyright (c) 2013 Noveo Group
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * Except as contained in this notice, the name(s) of the above copyright holders
 * shall not be used in advertising or otherwise to promote the sale, use or
 * other dealings in this Software without prior written authorization.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.noveogroup.android.task;

import android.os.SystemClock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public abstract class AbstractTaskExecutor<E extends TaskEnvironment> implements TaskExecutor<E> {

    private final Object lock = new Object();
    private final ArrayList<TaskListener> listeners = new ArrayList<TaskListener>(8);
    private volatile boolean shutdown = false;

    @Override
    public Object lock() {
        return lock;
    }

    @Override
    public Pack newPack() {
        return new Pack(lock());
    }

    @Override
    public Pack newPack(Pack pack) {
        return new Pack(lock(), pack);
    }

    @Override
    public abstract TaskSet<E> queue();

    @Override
    public TaskSet<E> queue(String... tags) {
        return queue().sub(tags);
    }

    @Override
    public TaskSet<E> queue(Collection<String> tags) {
        return queue().sub(tags);
    }

    @Override
    public void addTaskListener(TaskListener... taskListeners) {
        synchronized (lock()) {
            for (TaskListener taskListener : taskListeners) {
                if (taskListener != null) {
                    listeners.add(listeners.size(), taskListener);
                }
            }
        }
    }

    @Override
    public void removeTaskListener(TaskListener... taskListeners) {
        synchronized (lock()) {
            for (TaskListener taskListener : taskListeners) {
                if (taskListener != null) {
                    int lastIndex = listeners.lastIndexOf(taskListener);
                    if (lastIndex != -1) {
                        listeners.remove(lastIndex);
                    }
                }
            }
        }
    }

    /**
     * Returns a copy of list of already added listeners and adds
     * all of listeners from the parameter.
     *
     * @param addTaskListeners an array of additional listeners to add.
     * @return a list containing all of listeners.
     */
    protected TaskListener[] copyTaskListeners(TaskListener... addTaskListeners) {
        synchronized (lock()) {
            TaskListener[] array = new TaskListener[listeners.size() + addTaskListeners.length];
            listeners.toArray(array);
            System.arraycopy(addTaskListeners, 0, array, listeners.size(), addTaskListeners.length);
            return array;
        }
    }

    /**
     * Returns a copy of list of already added listeners and adds
     * all of listeners from the parameter.
     *
     * @param addTaskListeners a list of additional listeners to add.
     * @return a list containing all of listeners.
     */
    protected TaskListener[] copyTaskListeners(List<TaskListener> addTaskListeners) {
        return copyTaskListeners(addTaskListeners.toArray(new TaskListener[addTaskListeners.size()]));
    }

    @Override
    public abstract <T extends Task<E>> TaskHandler<T, E> execute(T task, Pack args, List<TaskListener> taskListeners, String... tags);

    @Override
    public <T extends Task<E>> TaskHandler<T, E> execute(T task, Pack args, TaskListener taskListener, String... tags) {
        return execute(task, args, Arrays.asList(taskListener), tags);
    }

    @Override
    public <T extends Task<E>> TaskHandler<T, E> execute(T task, Pack args, TaskListener... taskListeners) {
        return execute(task, args, Arrays.asList(taskListeners));
    }

    @Override
    public <T extends Task<E>> TaskHandler<T, E> execute(T task, Pack args, String... tags) {
        return execute(task, args, new ArrayList<TaskListener>(0), tags);
    }

    @Override
    public <T extends Task<E>> TaskHandler<T, E> execute(T task, List<TaskListener> taskListeners, String... tags) {
        return execute(task, new Pack(), taskListeners, tags);
    }

    @Override
    public <T extends Task<E>> TaskHandler<T, E> execute(T task, TaskListener taskListener, String... tags) {
        return execute(task, new Pack(), Arrays.asList(taskListener), tags);
    }

    @Override
    public <T extends Task<E>> TaskHandler<T, E> execute(T task, TaskListener... taskListeners) {
        return execute(task, new Pack(), Arrays.asList(taskListeners));
    }

    @Override
    public <T extends Task<E>> TaskHandler<T, E> execute(T task, String... tags) {
        return execute(task, new Pack(), new ArrayList<TaskListener>(0), tags);
    }

    // todo review the code below

    @Override
    public void shutdown() {
        synchronized (lock()) {
            shutdown = true;
            queue().interrupt();
        }
    }

    @Override
    public boolean isShutdown() {
        synchronized (lock()) {
            return shutdown;
        }
    }

    @Override
    public boolean isTerminated() {
        synchronized (lock) {
            return shutdown && queue().size() <= 0;
        }
    }

    @Override
    public void awaitTermination() throws InterruptedException {
        awaitTermination(0);
    }

    @Override
    public boolean awaitTermination(long timeout) throws InterruptedException {
        if (timeout < 0) {
            throw new IllegalArgumentException();
        }

        // await shutdown
        synchronized (lock) {
            while (!shutdown) {
                if (timeout == 0) {
                    lock.wait();
                } else {
                    long time = SystemClock.uptimeMillis();
                    lock.wait(timeout);
                    timeout -= SystemClock.uptimeMillis() - time;

                    if (!shutdown || timeout <= 0) {
                        return false;
                    }
                }
            }
        }

        // await all threads destroyed
        while (true) {
            TaskSet taskSet = queue();
            if (taskSet.size() <= 0) {
                return true;
            } else {
                if (timeout == 0) {
                    taskSet.join();
                } else {
                    long time = SystemClock.uptimeMillis();
                    taskSet.join(timeout);
                    timeout -= SystemClock.uptimeMillis() - time;

                    if (timeout <= 0) {
                        return false;
                    }
                }
            }
        }
    }

}