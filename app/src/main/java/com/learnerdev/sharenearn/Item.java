package com.learnerdev.sharenearn;

import android.location.Location;

import com.backendless.Geo;
import com.backendless.geo.GeoPoint;
import com.google.android.gms.maps.model.LatLng;

import java.util.Date;

/**
 * Created by uttam on 27-Jan-18.
 */

class Item {

    private Date created;
    private Date updated;
    private String itemName;
    private String itemDetails;
    private String objectId;
    private GeoPoint itemLocation;
    public Item(){

    }

    public Item(String itemName, String itemDetails){
        this.itemName=itemName;
        this.itemDetails=itemDetails;

    }
    public GeoPoint getLocation() {
        return itemLocation;
    }

    public void setLocation(GeoPoint location) {
        this.itemLocation = location;
    }



    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

//    public String getItemLocation() {
//        return itemLocation;
//    }
//
//    public void setItemLocation(String itemLocation) {
//        this.itemLocation = itemLocation;
//    }

    public String getItemDetails() {
        return itemDetails;
    }

    public void setItemDetails(String itemDetails) {
        this.itemDetails = itemDetails;
    }



}
