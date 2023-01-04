package com.inscada.modbus.client.services;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;

import java.util.concurrent.atomic.AtomicInteger;

public class Statistics {

    private static final AtomicInteger successCounter = new AtomicInteger(0);
    private static final AtomicInteger errorCounter = new AtomicInteger(0);
    private static final ReadOnlyIntegerWrapper successCount = new ReadOnlyIntegerWrapper();
    private static final ReadOnlyIntegerWrapper errorCount = new ReadOnlyIntegerWrapper();

    public synchronized static void success() {
        Platform.runLater(() -> successCount.set(successCounter.incrementAndGet()));
    }

    public synchronized static void error() {
        Platform.runLater(() -> errorCount.set(errorCounter.incrementAndGet()));
    }

    public synchronized static void clear() {
        successCounter.set(0);
        errorCounter.set(0);
        successCount.set(successCounter.get());
        errorCount.set(errorCounter.get());
    }

    public static ReadOnlyIntegerProperty successCountProperty() {
        return successCount.getReadOnlyProperty();
    }

    public static ReadOnlyIntegerProperty errorCountProperty() {
        return errorCount.getReadOnlyProperty();
    }
}
