package de.widdix.awss3virusscan;

import com.evanlennick.retry4j.CallExecutor;
import com.evanlennick.retry4j.CallResults;
import com.evanlennick.retry4j.RetryConfig;
import com.evanlennick.retry4j.RetryConfigBuilder;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.Callable;

public abstract class ATest {

    protected final <T> T retry(Callable<T> callable) {
        final Callable<T> wrapper = () -> {
            try {
                return callable.call();
            } catch (final Exception e) {
                System.out.println("retry[] exception: " + e.getMessage());
                e.printStackTrace();
                throw e;
            }
        };
        final RetryConfig config = new RetryConfigBuilder()
                .retryOnAnyException()
                .withMaxNumberOfTries(30)
                .withDelayBetweenTries(10, ChronoUnit.SECONDS)
                .withFixedBackoff()
                .build();
        final CallResults<Object> results = new CallExecutor(config).execute(wrapper);
        return (T) results.getResult();
    }

}
