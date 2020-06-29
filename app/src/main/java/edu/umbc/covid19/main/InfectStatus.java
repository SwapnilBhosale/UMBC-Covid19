package edu.umbc.covid19.main;

import android.os.Parcel;
import android.os.Parcelable;

public class InfectStatus implements Parcelable {

    private String id;
    private byte[] eid;
    private String lat;
    private String lng;
    private String timestamp;
    private String rssi;

    public InfectStatus(){

    }


    protected InfectStatus(Parcel in) {
        id = in.readString();
        eid = in.createByteArray();
        lat = in.readString();
        lng = in.readString();
        timestamp = in.readString();
        rssi = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeByteArray(eid);
        dest.writeString(lat);
        dest.writeString(lng);
        dest.writeString(timestamp);
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

    public byte[] getEid() {
        return eid;
    }

    public void setEid(byte[] eid) {
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

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getRssi() {
        return rssi;
    }

    public void setRssi(String rssi) {
        this.rssi = rssi;
    }

    @Override
    public String toString() {
        return "InfectStatus{" +
                "id='" + id + '\'' +
                ", eid='" + eid + '\'' +
                ", lat='" + lat + '\'' +
                ", lng='" + lng + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", rssi='" + rssi + '\'' +
                '}';
    }
}
