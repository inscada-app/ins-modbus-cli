package com.inscada.modbus.client.utils;

import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

import java.text.ParseException;
import java.util.regex.Pattern;

public class Constraints {

    public static void setIPTextField(TextField txt) {
        final Pattern pattern = Pattern.compile(getIPRegex());
        TextFormatter<?> formatter = new TextFormatter<>(change -> {
            if (pattern.matcher(change.getControlNewText()).matches()) {
                return change; // allow this change to happen
            } else {
                return null; // prevent change
            }
        });
        txt.setTextFormatter(formatter);
    }

    private static String getIPRegex() {
        String partialBlock = "(([01]?[0-9]{0,2})|(2[0-4][0-9])|(25[0-5]))";
        String subsequentPartialBlock = "(\\." + partialBlock + ")";
        String ipAddress = partialBlock + "?" + subsequentPartialBlock + "{0,3}";
        return "^" + ipAddress;
    }

    public static void setPortTextField(TextField txt) {
        final Pattern pattern = Pattern.compile("^((6553[0-5])|(655[0-2][0-9])|(65[0-4][0-9]{2})|(6[0-4][0-9]{3})|([1-5][0-9]{4})|([0-5]{0,5})|([0-9]{1,4}))$");
        TextFormatter<?> formatter = new TextFormatter<>(change -> {
            if (pattern.matcher(change.getControlNewText()).matches()) {
                return change; // allow this change to happen
            } else {
                return null; // prevent change
            }
        });
        txt.setTextFormatter(formatter);
    }

    public static void setIntTextField(TextField txt) {
        final Pattern pattern = Pattern.compile("\\d*");
        TextFormatter<?> formatter = new TextFormatter<>(change -> {
            String controlNewText = change.getControlNewText();
            if (pattern.matcher(controlNewText).matches()) {
                if (!controlNewText.isEmpty()) {
                    try {
                        Integer.parseInt(controlNewText);
                        return change; // allow this change to happen
                    } catch (NumberFormatException e) {
                        return null;
                    }
                }
                return change;
            } else {
                return null; // prevent change
            }
        });
        txt.setTextFormatter(formatter);
    }
}
