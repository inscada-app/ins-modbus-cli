package com.inscada.modbus.client.services;

import ch.qos.logback.classic.Level;
import com.inscada.modbus.client.model.ConsoleLogLevel;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Timer;
import java.util.TimerTask;

public class Console extends OutputStream {

    private static final int MAX_LINES = 1000;

    private static final int DELAY = 0;
    private static final int PERIOD = 200;
    private final PrintStream printStream;
    private final ObservableList<String> lines;
    private final FilteredList<String> filteredLines;
    private final StringBuffer stringBuffer;
    private final String lineSeparator;
    private final Timer timer;
    private Runnable changeListener;

    public Console() {
        this.printStream = new PrintStream(this, true);
        System.setOut(printStream);
        System.setErr(printStream);

        this.lines = FXCollections.observableArrayList();
        this.filteredLines = new FilteredList<>(lines);
        this.lineSeparator = System.lineSeparator();
        this.stringBuffer = new StringBuffer();
        this.timer = new Timer("Console Appender", true);
        this.timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    synchronized (stringBuffer) {
                        if (stringBuffer.length() == 0) {
                            return;
                        }
                        int index = stringBuffer.lastIndexOf(lineSeparator);
                        if (index == -1) {
                            return;
                        }
                        String substr = stringBuffer.substring(0, index + lineSeparator.length());
                        String[] lineArr = substr.split(lineSeparator);
                        if (lines.size() + lineArr.length > MAX_LINES) {
                            int overflow = lineArr.length - (MAX_LINES - lines.size());
                            lines.remove(0, overflow);
                        }
                        lines.addAll(lineArr);
                        stringBuffer.delete(0, index + lineSeparator.length());
                        if (changeListener != null) {
                            changeListener.run();
                        }
                    }
                });
            }
        }, DELAY, PERIOD);
    }

    public ObservableList<String> getLogs() {
        return filteredLines;
    }

    public void setLogLevel(ConsoleLogLevel logLevel) {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);

        switch (logLevel) {
            case ALL:
                root.setLevel(Level.ALL);
                break;
            case ERROR:
                root.setLevel(Level.ERROR);
                break;
            case WARN:
                root.setLevel(Level.WARN);
                break;
            case INFO:
                root.setLevel(Level.INFO);
                break;
            case DEBUG:
                root.setLevel(Level.DEBUG);
                break;
            case TRACE:
                root.setLevel(Level.TRACE);
                break;
            case OFF:
                root.setLevel(Level.OFF);
                break;
        }

    }

    public void clear() {
        lines.clear();
    }

    public void setFilter(String filterText) {
        if (filterText == null || filterText.isBlank()) {
            filteredLines.setPredicate(null);
        } else {
            filteredLines.setPredicate(s -> s.toLowerCase().contains(filterText.toLowerCase()));
        }
    }

    public void setChangeListener(Runnable changeListener) {
        this.changeListener = changeListener;
    }

    @Override
    public void write(int b) {
        stringBuffer.append((char) b);
    }

    @Override
    public void close() {
        timer.cancel();
        printStream.close();
    }
}
