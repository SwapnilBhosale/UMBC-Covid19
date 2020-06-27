package edu.umbc.covid19.viewmodel;

import java.util.Collection;
import java.util.List;

public class TracingStatus {

    private int numberOfContacts;
    private boolean advertising;
    private boolean receiving;
    private long lastSyncDate;
    private InfectionStatus infectionStatus;
    private List<ExposureDay> exposureDays;


    public TracingStatus(int numberOfContacts, boolean advertising, boolean receiving,
                         long lastSyncDate,
                         InfectionStatus infectionStatus, List<ExposureDay> exposureDays) {
        this.numberOfContacts = numberOfContacts;
        this.advertising = advertising;
        this.receiving = receiving;
        this.lastSyncDate = lastSyncDate;
        this.infectionStatus = infectionStatus;
        this.exposureDays = exposureDays;

    }

    public int getNumberOfContacts() {
        return numberOfContacts;
    }

    public boolean isAdvertising() {
        return advertising;
    }

    public boolean isReceiving() {
        return receiving;
    }

    public long getLastSyncDate() {
        return lastSyncDate;
    }

    public InfectionStatus getInfectionStatus() {
        return infectionStatus;
    }

    public List<ExposureDay> getExposureDays() {
        return exposureDays;
    }


}
