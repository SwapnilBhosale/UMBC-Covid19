package edu.umbc.covid19.main;

import android.os.Parcel;
import android.os.Parcelable;

public class InfectStatus implements Parcelable {

    private String id;
    private String eid;
    private String lat;
    private String lng;
    private Long timestamp;
    private String rssi;

    public InfectStatus(){

    }
    protected InfectStatus(Parcel in) {
        id = in.readString();
        eid = in.readString();
        lat = in.readString();
        lng = in.readString();
        if (in.readByte() == 0) {
            timestamp = null;
        } else {
            timestamp = in.readLong();
        }
        rssi = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(eid);
        dest.writeString(lat);
        dest.writeString(lng);
        if (timestamp == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(timestamp);
        }
        dest.writeString(rssi);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<InfectStatus> CREATOR = new Creator<InfectStatus>() {
        @Override
        public InfectStatus createFromParcel(Parcel in) {
            return new InfectStatus(in);
        }

        @Override
        public InfectStatus[] newArray(int size) {
            return new InfectStatus[size];
        }
    };

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEid() {
        return eid;
    }

    public void setEid(String eid) {
        this.eid = eid;
    }

    public String getLat() {
        return lat;
    }

    public void setLat(String lat) {
        this.lat = lat;
    }

    public String getLng() {
        return lng;
    }

    public void setLng(String lng) {
        this.lng = lng;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getRssi() {
        return rssi;
    }

    public void setRssi(String rssi) {
        this.rssi = rssi;
    }
}
