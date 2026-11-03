package com.carry.app.models;

public enum LocationArea {
    NEW_MARKET("New Market"),
    SONADANGA("Sonadanga"),
    GOLLAMARI("Gollamari"),
    DAULATPUR("Daulatpur"),
    KUET_AREA("KUET Campus Area"),
    ELECTRONICS_MARKET("Electronics Market"),
    SHIB_BARI("Shib Bari"),
    OTHER("Other (Khulna)");

    private final String displayName;

    LocationArea(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
