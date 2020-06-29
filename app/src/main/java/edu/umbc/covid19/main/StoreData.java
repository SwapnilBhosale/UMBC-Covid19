package edu.umbc.covid19.main;

class StoreData{
    Long COUNT;
    String STORE;

    public Long getCOUNT() {
        return COUNT;
    }

    public void setCOUNT(Long COUNT) {
        this.COUNT = COUNT;
    }

    public String getSTORE() {
        return STORE;
    }

    public void setSTORE(String STORE) {
        this.STORE = STORE;
    }

    @Override
    public String toString() {
        return "StoreData{" +
                "COUNT='" + COUNT + '\'' +
                ", STORE='" + STORE + '\'' +
                '}';
    }
}