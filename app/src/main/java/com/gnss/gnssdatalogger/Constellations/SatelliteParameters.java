/*
 * Copyright 2018 TFI Systems

 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 */

package com.gnss.gnssdatalogger.Constellations;


/**
 * Container class for storing satellite related parameters
 */
public class SatelliteParameters {

    public double getPseudorange() {
        return pseudorange.getPseudorange();
    }

    public void setPseudorange(Pseudorange pseudorange) {
        this.pseudorange = pseudorange;
    }


    /**
     * Pseudorange to the satellite
     */
    protected Pseudorange pseudorange;

    private double carrierFrequency;



    /**
     * Satellite id in the constellation
     */
    private int satId;

    /**
     * Strength of the signal received from the satellite
     */
    private double signalStrength;

    /**
     * Type of the constellation as defined by the GnssStatus, extended by the definitions
     * in the Constellation class
     */
    private int constellationType;

    private GpsTime gpsTime;

    public GpsTime getGpsTime() {
        return gpsTime;
    }

    public void setGpsTime(GpsTime gpsTime) {
        this.gpsTime = gpsTime;
    }

    /**
     * @param satelliteId Id of the newly created satellite
     * @param pseudorange pseudorange to this satellite
     */
    public SatelliteParameters(GpsTime gpsTime,int satelliteId, Pseudorange pseudorange){
        this.gpsTime=gpsTime;
        this.satId = satelliteId;
        this.pseudorange = pseudorange;
        setSignalStrength(0.0);
    }




    public int getSatId() {
        return satId;
    }

    public void setSatId(int satId) {
        this.satId = satId;
    }

    /**
     * Stores the clock bias of the satellite
     */
    private double clockBias;

    public double getClockBias() {
        return clockBias;
    }

    public void setClockBias(double clockBias) {
        this.clockBias = clockBias;
    }

    /**
     *
     * @return used pseudorange object
     */
    public Pseudorange getPseudorangeObject() {
        return pseudorange;
    }

    /**
     * @return Signal strength for the satellite [dB]
     */
    public double getSignalStrength() {
        return signalStrength;
    }

    /**
     *
     * @param signalStrength assigned signal strength of the satellite
     */
    public void setSignalStrength(double signalStrength) {
        this.signalStrength = signalStrength;
    }

    /**
     * Unique ID of the satellite
     */
    private String uniqueSatId;

    /**
     * @return unique id of the satellite
     */
    public String getUniqueSatId(){
        return uniqueSatId;
    }

    /**
     * sets the unique id of the satellite
     * @param uniqueSatId
     */
    public void setUniqueSatId(String uniqueSatId){
        this.uniqueSatId = uniqueSatId;
    }

    /**
     *
     * @return constellation type as defined by the {@code GnssSatus} or {@code Constellation}
     */
    public int getConstellationType() {
        return constellationType;
    }

    /**
     * Sets constellation type
     * @param constellationType
     */
    public void setConstellationType(int constellationType) {
        this.constellationType = constellationType;
    }

    public double getCarrierFrequency() {
        return carrierFrequency;
    }

    public void setCarrierFrequency(double carrierFrequency) {
        this.carrierFrequency = carrierFrequency;
    }


    /**
     * 载波相位观测值  phase
     */
    private double phase;

    public double getPhase() {
        return phase;
    }

    public void setPhase(double phase) {
        this.phase = phase;
    }
    /**
     * SNR,信噪比
     */
    private double snr;

    public double getSnr() {
        return snr;
    }

    public void setSnr(double snr) {
        this.snr = snr;
    }
    /**
     * 多普勒观测值
     */
    private double doppler;

    public double getDoppler() {
        return doppler;
    }

    public void setDoppler(double doppler) {
        this.doppler = doppler;
    }


}
