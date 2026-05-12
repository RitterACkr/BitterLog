module dev.ritterackr.bitterlog {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires javafx.swing;
    requires org.xerial.sqlitejdbc;
    requires org.fxmisc.richtext;
    requires com.fasterxml.jackson.databind;
    requires java.prefs;
    requires java.desktop;
    requires jdk.jsobject;
    requires reactfx;
    requires wellbehavedfx;

    requires flyway.core;
    requires flexmark;
    requires flexmark.util.ast;
    requires flexmark.util.data;
    requires flexmark.ext.tables;
    requires flexmark.ext.gfm.strikethrough;
    requires jdk.compiler;

    opens dev.ritterackr.bitterlog to javafx.fxml;
    opens dev.ritterackr.bitterlog.controller to javafx.fxml;
    opens dev.ritterackr.bitterlog.model to javafx.fxml;

    exports dev.ritterackr.bitterlog;
}