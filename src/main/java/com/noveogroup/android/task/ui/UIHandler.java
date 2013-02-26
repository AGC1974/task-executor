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

package com.noveogroup.android.task.ui;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.*;

/**
 * {@link UIHandler} provides you an interface to process {@link Runnable}
 * callbacks using usual {@link Handler}.
 * <p/>
 * Scheduling callbacks is accomplished with the {@link #post(Runnable)},
 * {@link #postDelayed(Runnable, long)}, {@link #postSingle(Runnable)},
 * {@link #postSingleDelayed(Runnable, long)} and {@link #postSync(Runnable)}
 * methods.
 * <p/>
 * Joining callbacks can cause a blocking. To ensure all threads will be resumed call {@link #removeCallbacks()}
 * when handler is no longer needed.
 */
public class UIHandler {

    private final Object lock = new Object();
    private final Handler handler;
    private final Set<WaitCallback> set = new HashSet<WaitCallback>();
    private final Map<Runnable, Set<WaitCallback>> map = new HashMap<Runnable, Set<WaitCallback>>();

    private class WaitCallback implements Runnable {

        private final Runnable callback;
        private final Object waitObject = new Object();
        private volatile boolean finished = false;

        public WaitCallback(Runnable callback) {
            this.callback = callback;
        }

        private void addCallback() {
            set.add(this);

            Set<WaitCallback> waitCallbacks = map.get(callback);
            if (waitCallbacks == null) {
                waitCallbacks = new HashSet<WaitCallback>();
                map.put(callback, waitCallbacks);
            }
            waitCallbacks.add(this);
        }

        private void removeCallback() {
            set.remove(this);

            Set<WaitCallback> waitCallbacks = map.get(callback);
            if (waitCallbacks != null) {
                waitCallbacks.remove(this);
                if (waitCallbacks.isEmpty()) {
                    map.remove(callback);
                }
            }
        }

        public boolean post() {
            synchronized (lock) {
                if (!handler.post(this)) {
                    return false;
                }
                addCallback();
                return true;
            }
        }

        public boolean postDelayed(long delay) {
            synchronized (lock) {
                if (!handler.postDelayed(this, delay)) {
                    return false;
                }
                addCallback();
                return true;
            }
        }

        @Override
        public final void run() {
            try {
                if (callback != null) {
                    callback.run();
                }
            } finally {
                release();
            }
        }

        public void release() {
            synchronized (lock) {
                handler.removeCallbacks(this);
                removeCallback();

                synchronized (waitObject) {
                    finished = true;
                    waitObject.notifyAll();
                }
            }
        }

        public void join() throws InterruptedException {
            synchronized (waitObject) {
                while (!finished) {
                    waitObject.wait();
                }
            }
        }

    }

    /**
     * Default constructor associates this handler with the queue for
     * the current thread. If there isn't one, this handler won't be able
     * to receive messages.
     */
    public UIHandler() {
        this(new Handler());
    }

    /**
     * Uses main looper of the context to initialize the handler.
     */
    public UIHandler(Context context) {
        this(context.getMainLooper());
    }

    /**
     * Use the provided queue instead of the default one.
     *
     * @param looper the custom queue.
     */
    public UIHandler(Looper looper) {
        this(new Handler(looper));
    }

    /**
     * Use the specified handler to delegate callbacks to.
     *
     * @param handler the delegate.
     */
    public UIHandler(Handler handler) {
        this.handler = handler;
    }

    /**
     * Causes the callback to be added to the queue.
     *
     * @param callback the callback that will be executed.
     * @return Returns true if the {@link Runnable} was successfully placed
     *         in to the queue. Returns false otherwise.
     */
    public boolean post(Runnable callback) {
        return new WaitCallback(callback).post();
    }

    /**
     * Causes the callback to be added to the queue.
     *
     * @param callback the callback that will be executed.
     * @param delay    the delay (in milliseconds) until the callback will be
     *                 executed.
     * @return Returns true if the {@link Runnable} was successfully placed
     *         in to the queue. Returns false otherwise.
     */
    public boolean postDelayed(Runnable callback, long delay) {
        return new WaitCallback(callback).postDelayed(delay);
    }

    private void joinCallbacks(Set<WaitCallback> waitCallbacks) throws InterruptedException {
        if (Thread.currentThread() == handler.getLooper().getThread()) {
            throw new RuntimeException("current thread blocks the callback");
        }

        for (WaitCallback waitCallback : waitCallbacks) {
            waitCallback.join();
        }
    }

    private void removeCallbacks(Set<WaitCallback> waitCallbacks) {
        for (WaitCallback waitCallback : waitCallbacks) {
            waitCallback.release();
        }
    }

    private Set<WaitCallback> getCallbacks(Runnable callback) {
        synchronized (lock) {
            Set<WaitCallback> waitCallbacks = map.get(callback);
            return waitCallbacks == null ? Collections.<WaitCallback>emptySet() : new HashSet<WaitCallback>(waitCallbacks);
        }
    }

    private Set<WaitCallback> getCallbacks() {
        synchronized (lock) {
            return new HashSet<WaitCallback>(set);
        }
    }

    /**
     * Joins the specified callback. If the callback will be removed before
     * finish this method successfully ends.
     *
     * @param callback the callback to join to.
     * @throws InterruptedException if any thread interrupted the current one.
     */
    public void joinCallbacks(Runnable callback) throws InterruptedException {
        joinCallbacks(getCallbacks(callback));
    }

    /**
     * Joins all callbacks of the handler. If the callbacks will be removed
     * before finish this method successfully ends.
     *
     * @throws InterruptedException if any thread interrupted the current one.
     */
    public void joinCallbacks() throws InterruptedException {
        joinCallbacks(getCallbacks());
    }

    /**
     * Removes any pending posts of the specified callback that are in
     * the message queue. All waiting threads joining to this callback
     * will be notified and resumed.
     *
     * @param callback the callback to remove.
     */
    public void removeCallbacks(Runnable callback) {
        removeCallbacks(getCallbacks(callback));
    }

    /**
     * Removes any callbacks from the queue. All waiting threads joining
     * to callbacks of this handler will be notified and resumed.
     */
    public void removeCallbacks() {
        removeCallbacks(getCallbacks());
    }

    /**
     * Similar to {@link #post(Runnable)} but removes callbacks first before
     * adding to queue.
     *
     * @param callback the callback that will be executed.
     * @return Returns true if the {@link Runnable} was successfully placed
     *         in to the queue. Returns false otherwise.
     */
    public boolean postSingle(Runnable callback) {
        synchronized (lock) {
            removeCallbacks(callback);
            return post(callback);
        }
    }

    /**
     * Similar to {@link #postDelayed(Runnable, long)} but removes callbacks
     * first before adding to queue.
     *
     * @param callback the callback that will be executed.
     * @param delay    the delay (in milliseconds) until the callback
     *                 will be executed.
     * @return Returns true if the {@link Runnable} was successfully placed
     *         in to the queue. Returns false otherwise.
     */
    public boolean postSingleDelayed(Runnable callback, long delay) {
        synchronized (lock) {
            removeCallbacks(callback);
            return postDelayed(callback, delay);
        }
    }

    /**
     * Similar to {@link #postSingle(Runnable)} but joins the callbacks
     * after adding to queue.
     *
     * @param callback the callback that will be executed.
     * @return Returns true if the {@link Runnable} was successfully placed
     *         in to the queue. Returns false otherwise.
     */
    public boolean postSync(Runnable callback) throws InterruptedException {
        WaitCallback waitCallback;
        synchronized (lock) {
            removeCallbacks(callback);
            waitCallback = new WaitCallback(callback);
            if (!waitCallback.post()) {
                return false;
            }
        }

        waitCallback.join();
        return true;
    }

}
