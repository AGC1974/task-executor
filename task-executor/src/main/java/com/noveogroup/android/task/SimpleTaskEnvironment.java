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

/**
 * {@link SimpleTaskEnvironment} is an default implementation of
 * the {@link TaskEnvironment} interface. A subclass may implement
 * an additional functionality.
 *
 * @param <Input>  type of task input.
 * @param <Output> type of task output.
 */
public class SimpleTaskEnvironment<Input, Output> implements TaskEnvironment<Input, Output> {

    /**
     * {@link TaskHandler} object to delegate functionality.
     */
    protected final TaskHandler<Input, Output> handler;

    /**
     * Creates new instance of {@link SimpleTaskEnvironment}.
     *
     * @param handler {@link TaskHandler} object to delegate functionality.
     */
    public SimpleTaskEnvironment(TaskHandler<Input, Output> handler) {
        this.handler = handler;
    }

    @Override
    public TaskExecutor executor() {
        return handler.executor();
    }

    @Override
    public TaskSet owner() {
        return handler.owner();
    }

    @Override
    public TaskHandler<Input, Output> handler() {
        return handler;
    }

    @Override
    public Object lock() {
        return handler.lock();
    }

    @Override
    public Pack<Input, Output> vars() {
        return handler.vars();
    }

    @Override
    public void interruptSelf() {
        handler.interrupt();
    }

    @Override
    public boolean isInterrupted() {
        return handler.isInterrupted();
    }

    @Override
    public void checkInterrupted() throws InterruptedException {
        if (isInterrupted()) {
            throw new InterruptedException();
        }
    }

}
