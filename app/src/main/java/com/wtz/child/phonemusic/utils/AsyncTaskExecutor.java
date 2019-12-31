package com.wtz.child.phonemusic.utils;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class AsyncTaskExecutor {

    private volatile static AsyncTaskExecutor mInstance;

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
    private static final int KEEP_ALIVE_SECONDS = 30;

    private ExecutorService mExecutorService;

    public static AsyncTaskExecutor getInstance() {
        if (mInstance == null) {
            synchronized (AsyncTaskExecutor.class) {
                if (mInstance == null)
                    mInstance = new AsyncTaskExecutor();
            }
        }
        return mInstance;
    }

    private AsyncTaskExecutor() {
        mExecutorService = new ThreadPoolExecutor(MAXIMUM_POOL_SIZE, MAXIMUM_POOL_SIZE,
                KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(),
                new ThreadPoolExecutor.DiscardPolicy());
    }

    public <T> Future<T> submit(Callable<T> task) {
        return mExecutorService.submit(task);
    }

    public Future<?> submit(Runnable task) {
        return mExecutorService.submit(task);
    }

    public void shutdown() {
        synchronized (AsyncTaskExecutor.class) {
            mInstance = null;
        }
        mExecutorService.shutdown();
    }

}
