package com.example.reliability;

public enum Role {
    VIEWER, EDITOR, ADMIN;

    public boolean atLeast(Role other) {
        return this.ordinal() >= other.ordinal();
    }
}