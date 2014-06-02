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

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public interface TaskSet extends Iterable<TaskHandler<?, ?>> {

    public TaskExecutor executor();

    public Object lock();

    /**
     * Returns an unmodifiable set of task's tags.
     * <p/>
     * The set of tags defines a set of conditions to filter a whole set of
     * tasks from the queue. The task set contains only tasks which are
     * labeled by these tags.
     *
     * @return the set of task's tags.
     */
    public Set<String> tags();

    public TaskSet sub(String... tags);

    public TaskSet sub(Collection<String> tags);

    public Set<TaskHandler.State> states();

    public TaskSet filter(TaskHandler.State... states);

    public TaskSet filter(Collection<TaskHandler.State> states);

    public <Input, Output> TaskHandler<Input, Output> execute(Task<Input, Output> task, Pack<Input, Output> args, TaskListener<Input, Output>... taskListeners);

    public <Input, Output> TaskHandler<Input, Output> execute(Task<Input, Output> task, Input input, TaskListener<Input, Output>... taskListeners);

    public <Input, Output> TaskHandler<Input, Output> execute(Task<Input, Output> task, TaskListener<Input, Output>... taskListeners);

    public int size();

    public boolean isEmpty();

    @Override
    public Iterator<TaskHandler<?, ?>> iterator();

    public void interrupt();

    public void join() throws InterruptedException;

    public boolean join(long timeout) throws InterruptedException;

}
