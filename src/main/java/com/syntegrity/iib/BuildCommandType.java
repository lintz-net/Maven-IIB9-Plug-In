/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2018.
 */
package com.syntegrity.iib;

/**
 *
 * 
 *
 * @author Izac (user_vorname user_nachname)
 * @version $Id: $
 * @since pom_version, 2018
 */
public enum BuildCommandType {
    ECLIPSE_JAVA_BUILDER("org.eclipse.jdt.core.javabuilder"), IBM_JAVA_BUILDER("com.ibm.etools.mft.java.builder.javabuilder");

    private String fullname;

    private BuildCommandType(String name) {
        fullname = name;
    }

    public String getFullName() {
        return fullname;
    }

}
