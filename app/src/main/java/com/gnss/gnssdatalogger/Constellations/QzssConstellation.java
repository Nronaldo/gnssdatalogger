package com.gnss.gnssdatalogger.Constellations;

import android.location.GnssClock;
import android.location.GnssMeasurement;
import android.location.GnssMeasurementsEvent;
import android.location.GnssStatus;
import android.os.Build;

import com.gnss.gnssdatalogger.GNSSConstants;

import java.util.ArrayList;
import java.util.List;

public class QzssConstellation extends Constellation {
    private final static char satType = 'J';
    private static final String NAME = "QZSS L1";
    private static final String TAG = "QzssConstellation";
    private static double L1_FREQUENCY = 1.57542e9;
    private static double FREQUENCY_MATCH_RANGE = 0.1e9;

    private boolean fullBiasNanosInitialized = false;
    private long FullBiasNanos;

    //private Coordinates rxPos;
    protected double tRxGPS;
    protected double weekNumberNanos;
    private List<SatelliteParameters> unusedSatellites = new ArrayList<>();

    public double getWeekNumber(){
        return weekNumberNanos;
    }

    public double gettRxGPS(){
        return tRxGPS;
    }

    private static final int constellationId = GnssStatus.CONSTELLATION_QZSS;
    private static double MASK_ELEVATION = 20; // degrees
    private static double MASK_CN0 = 10; // dB-Hz

    /**
     * Time of the measurement
     */
    //private Time timeRefMsec;

    protected int visibleButNotUsed = 0;

    // Condition for the pseudoranges that takes into account a maximum uncertainty for the TOW
    // (as done in gps-measurement-tools MATLAB code)
    private static final int MAXTOWUNCNS = 50;                                     // [nanoseconds]



    /**
     * List holding observed satellites
     */
    protected List<SatelliteParameters> observedSatellites = new ArrayList<>();





    @Override
    public String getName() {
        synchronized (this) {
            return NAME;
        }
    }

    public static boolean approximateEqual(double a, double b, double eps){
        return Math.abs(a-b)<eps;
    }

    @Override
    public void updateMeasurements(GnssMeasurementsEvent event) {

        synchronized (this) {
            visibleButNotUsed = 0;
            observedSatellites.clear();
            unusedSatellites.clear();
            GnssClock gnssClock = event.getClock();






            long TimeNanos = gnssClock.getTimeNanos();
            //timeRefMsec = new Time(System.currentTimeMillis());
            double BiasNanos = gnssClock.getBiasNanos();
            double gpsTime, pseudorange;

            // Use only the first instance of the FullBiasNanos (as done in gps-measurement-tools)
            if (!fullBiasNanosInitialized) {
                FullBiasNanos = gnssClock.getFullBiasNanos();
                fullBiasNanosInitialized = true;
            }


            // Start computing the pseudoranges using the raw data from the phone's GNSS receiver
            for (GnssMeasurement measurement : event.getMeasurements()) {

                if (measurement.getConstellationType() != constellationId)
                    continue;

                if (measurement.hasCarrierFrequencyHz())
                    if (!approximateEqual(measurement.getCarrierFrequencyHz(), L1_FREQUENCY, FREQUENCY_MATCH_RANGE))
                        continue;

                // excluding satellites which don't have the L5 component
//                if(measurement.getSvid() == 2 || measurement.getSvid() == 4
//                        || measurement.getSvid() == 5 || measurement.getSvid() == 7
//                        || measurement.getSvid() == 11 || measurement.getSvid() == 12
//                        || measurement.getSvid() == 13 || measurement.getSvid() == 14
//                        || measurement.getSvid() == 15 || measurement.getSvid() == 16
//                        || measurement.getSvid() == 17 || measurement.getSvid() == 18
//                        || measurement.getSvid() == 19 || measurement.getSvid() == 20
//                        || measurement.getSvid() == 21 || measurement.getSvid() == 22
//                        || measurement.getSvid() == 23 || measurement.getSvid() == 28
//                        || measurement.getSvid() == 29 || measurement.getSvid() == 31)
//                    continue;


                long ReceivedSvTimeNanos = measurement.getReceivedSvTimeNanos();
                double TimeOffsetNanos = measurement.getTimeOffsetNanos();


                // GPS Time generation (GSA White Paper - page 20)
                gpsTime =
                        TimeNanos - (FullBiasNanos + BiasNanos); // TODO intersystem bias?

                // Measurement time in full GPS time without taking into account weekNumberNanos(the number of
                // nanoseconds that have occurred from the beginning of GPS time to the current
                // week number)
                tRxGPS =
                        gpsTime + TimeOffsetNanos;


                weekNumberNanos =
                        Math.floor((-1. * FullBiasNanos) / GNSSConstants.NUMBER_NANO_SECONDS_PER_WEEK)
                                * GNSSConstants.NUMBER_NANO_SECONDS_PER_WEEK;

                // GPS pseudorange computation
                pseudorange =
                        (tRxGPS - weekNumberNanos - ReceivedSvTimeNanos) / 1.0E9
                                * GNSSConstants.SPEED_OF_LIGHT;

                // TODO Check that the measurement have a valid state such that valid pseudoranges are used in the PVT algorithm

                /*

                According to https://developer.android.com/ the GnssMeasurements States required
                for GPS valid pseudoranges are:

                int STATE_CODE_LOCK         = 1      (1 << 0)
                int int STATE_TOW_DECODED   = 8      (1 << 3)

                */

                int measState = measurement.getState();

                // Bitwise AND to identify the states
                boolean codeLock = (measState & GnssMeasurement.STATE_CODE_LOCK) != 0;
                boolean towDecoded = (measState & GnssMeasurement.STATE_TOW_DECODED) != 0;
                boolean towKnown      = false;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    towKnown = (measState & GnssMeasurement.STATE_TOW_KNOWN) != 0;
                }
//                boolean towUncertainty = measurement.getReceivedSvTimeUncertaintyNanos() <  MAXTOWUNCNS;


                if (codeLock && (towDecoded || towKnown)  && pseudorange < 1e9) { // && towUncertainty
                    SatelliteParameters satelliteParameters = new SatelliteParameters(new GpsTime(gnssClock),
                            measurement.getSvid(),
                            new Pseudorange(pseudorange, 0.0));

                    satelliteParameters.setUniqueSatId("J" + satelliteParameters.getSatId() + "_L1");

                    satelliteParameters.setSignalStrength(measurement.getCn0DbHz());

                    satelliteParameters.setConstellationType(measurement.getConstellationType());

                    if(measurement.hasCarrierFrequencyHz())
                        satelliteParameters.setCarrierFrequency(measurement.getCarrierFrequencyHz());

                    /**
                     * 获取载波相位观测值
                     */

                    double ADR = measurement.getAccumulatedDeltaRangeMeters();
                    double λ = GNSSConstants.SPEED_OF_LIGHT / L1_FREQUENCY;
                    double phase = ADR / λ;
                    satelliteParameters.setPhase(phase);


                    /**
                     获取SNR
                     */
                    if (measurement.hasSnrInDb()) {
                        satelliteParameters.setSnr(measurement.getSnrInDb());
                    }
                    /**
                     获取多普勒值
                     */
                    double doppler = measurement.getPseudorangeRateMetersPerSecond() / λ;
                    satelliteParameters.setDoppler(doppler);

                    observedSatellites.add(satelliteParameters);
//
//                    Log.d(TAG, "updateConstellations(" + measurement.getSvid() + "): " + weekNumberNanos + ", " + tRxGPS + ", " + pseudorange);
//                    Log.d(TAG, "updateConstellations: Passed with measurement state: " + measState);
//                    Log.d(TAG, "Time: " + satelliteParameters.getGpsTime().getGpsTimeString()+",phase"+satelliteParameters.getPhase()+",snr"+satelliteParameters.getSnr()+",doppler"+satelliteParameters.getDoppler());

                } else {
                    SatelliteParameters satelliteParameters = new SatelliteParameters(new GpsTime(gnssClock),
                            measurement.getSvid(),
                            null);

                    satelliteParameters.setUniqueSatId("J" + satelliteParameters.getSatId() + "_L1");

                    satelliteParameters.setSignalStrength(measurement.getCn0DbHz());

                    satelliteParameters.setConstellationType(measurement.getConstellationType());

                    if(measurement.hasCarrierFrequencyHz())
                        satelliteParameters.setCarrierFrequency(measurement.getCarrierFrequencyHz());

                    unusedSatellites.add(satelliteParameters);
                    visibleButNotUsed++;
                }
            }
        }
    }

    @Override
    public double getSatelliteSignalStrength(int index) {
        synchronized (this) {
            return observedSatellites.get(index).getSignalStrength();
        }
    }

    @Override
    public int getConstellationId() {
        synchronized (this) {
            return constellationId;
        }
    }


    @Override
    public SatelliteParameters getSatellite(int index) {
        synchronized (this) {
            return observedSatellites.get(index);
        }
    }

    @Override
    public List<SatelliteParameters> getSatellites() {
        synchronized (this) {
            return observedSatellites;
        }
    }

    @Override
    public int getVisibleConstellationSize() {
        synchronized (this) {
            return getUsedConstellationSize() + visibleButNotUsed;
        }
    }

    @Override
    public int getUsedConstellationSize() {
        synchronized (this) {
            return observedSatellites.size();
        }
    }
}
