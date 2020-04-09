package com.gnss.gnssdatalogger.Constellations;

import android.annotation.SuppressLint;
import android.location.GnssClock;
import android.location.GnssMeasurement;
import android.location.GnssMeasurementsEvent;
import android.location.GnssStatus;

import com.gnss.gnssdatalogger.GNSSConstants;

import java.util.ArrayList;
import java.util.List;

public class GlonassR1Constellation extends Constellation {
    private final static char satType = 'R';
    private static final String NAME = "Glonass R1";
    private static final String TAG = "GlonassConstellation";
    private long FullBiasNanos;

    protected double tRxGlonass;
    protected double weekNumberNanos;
    protected double tRx;
    private List<SatelliteParameters> unusedSatellites = new ArrayList<>();

    private static final int constellationId = GnssStatus.CONSTELLATION_GLONASS;


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

    public static boolean approximateEqual(double a, double b, double eps) {
        return Math.abs(a - b) < eps;
    }

    @SuppressLint("DefaultLocale")
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
//            if (!fullBiasNanosInitialized) {
            FullBiasNanos = gnssClock.getFullBiasNanos();
//                fullBiasNanosInitialized = true;
//            }+

            // Start computing the pseudoranges using the raw data from the phone's GNSS receiver
            for (GnssMeasurement measurement : event.getMeasurements()) {

                if (measurement.getConstellationType() != constellationId)
                    continue;

//                if (measurement.hasCarrierFrequencyHz())
//                    if (!approximateEqual(measurement.getCarrierFrequencyHz(), L1_FREQUENCY, FREQUENCY_MATCH_RANGE))
//                        continue;

                //&& (measurement.getState() & (1L << 3)) != 0

                if (measurement.getCn0DbHz() >= 18) {
                    int measState = measurement.getState();

                    long ReceivedSvTimeNanos = measurement.getReceivedSvTimeNanos();
                    double TimeOffsetNanos = measurement.getTimeOffsetNanos();

                    // GPS Time generation (GSA White Paper - page 20)
                    gpsTime = TimeNanos + TimeOffsetNanos - (FullBiasNanos + BiasNanos); // TODO intersystem bias?

                    // Measurement time in full GPS time without taking into account weekNumberNanos(the number of
                    // nanoseconds that have occurred from the beginning of GPS time to the current
                    // week number)
                    double dayNumberNanos = Math.floor((-1. * FullBiasNanos) / GNSSConstants.NUMBER_NANO_SECONDS_PER_DAY)
                            * GNSSConstants.NUMBER_NANO_SECONDS_PER_DAY;
                    weekNumberNanos = Math.floor((-1. * FullBiasNanos) / GNSSConstants.NUMBER_NANO_SECONDS_PER_WEEK)
                            * GNSSConstants.NUMBER_NANO_SECONDS_PER_WEEK;

                    double tRxNanos = gnssClock.getTimeNanos() - FullBiasNanos - weekNumberNanos;
                    tRx = gpsTime;
                    tRxGlonass = gpsTime - dayNumberNanos + 10800e9 - 18e9;


                    double tRxSeconds = (tRxNanos - measurement.getTimeOffsetNanos()) * 1e-9;
                    double tTxSeconds = measurement.getReceivedSvTimeNanos() * 1e-9;
                    double tRxSeconds_GLO = tRxSeconds % 86400;
                    double tTxSeconds_GLO = tTxSeconds - 10800 + 18;

                    if(tTxSeconds_GLO < 0){
                        tTxSeconds_GLO = tTxSeconds_GLO + 86400;
                    }
                    tRxSeconds = tRxSeconds_GLO;
                    tTxSeconds = tTxSeconds_GLO;
//                    double txSeconds = measurement.getReceivedSvTimeNanos() * 1e-9;
//                    double txSeconds_GLO = txSeconds - 10800 + gnssClock.getLeapSecond();
//                    if (txSeconds_GLO < 0) {
//                        tRxGlonass -= 86400e9;
//                    }



                    // GPS pseudorange computation
                    pseudorange = (tRxSeconds-tTxSeconds)
                            * GNSSConstants.SPEED_OF_LIGHT;
//
//                    // Bitwise AND to identify the states
                    boolean codeLock = (measState & GnssMeasurement.STATE_CODE_LOCK) != 0;

                    if (codeLock && pseudorange < 1e9 && pseudorange > 0) { // && towUncertainty
                        SatelliteParameters satelliteParameters = new SatelliteParameters(new GpsTime(gnssClock),
                                measurement.getSvid(),
                                new Pseudorange(pseudorange, 0.0));

                        satelliteParameters.setUniqueSatId(String.format("R%02d_R1", satelliteParameters.getSatId()));

                        satelliteParameters.setSignalStrength(measurement.getCn0DbHz());

                        satelliteParameters.setConstellationType(measurement.getConstellationType());

                        if (measurement.hasCarrierFrequencyHz())
                            satelliteParameters.setCarrierFrequency(measurement.getCarrierFrequencyHz());

                        /**
                         * 获取载波相位观测值
                         */

                        if (measurement.hasCarrierFrequencyHz()) {
                            double ADR = measurement.getAccumulatedDeltaRangeMeters();
                            double λ = GNSSConstants.SPEED_OF_LIGHT / measurement.getCarrierFrequencyHz();
                            double phase = ADR / λ;
                            satelliteParameters.setPhase(phase);
                        }

                        /**
                         获取SNR
                         */
                        if (measurement.hasSnrInDb()) {
                            satelliteParameters.setSnr(measurement.getSnrInDb());
                        }
                        /**
                         获取多普勒值
                         */
                        if (measurement.hasCarrierFrequencyHz()) {
                            double λ = GNSSConstants.SPEED_OF_LIGHT / measurement.getCarrierFrequencyHz();
                            double doppler = measurement.getPseudorangeRateMetersPerSecond() / λ;
                            satelliteParameters.setDoppler(doppler);

                        }

                        observedSatellites.add(satelliteParameters);
//                        Log.d(TAG, "updateConstellations(" + measurement.getSvid() + "): " + weekNumberNanos + ", " + tRxGlonass + ", " + pseudorange);
//                        Log.d(TAG, "updateConstellations: Passed with measurement state: " + measState);
//                        Log.d(TAG, "Time: " + satelliteParameters.getGpsTime().getGpsTimeString()+",phase"+satelliteParameters.getPhase()+",snr"+satelliteParameters.getSnr()+",doppler"+satelliteParameters.getDoppler());
                    }
                } else {
                    SatelliteParameters satelliteParameters = new SatelliteParameters(new GpsTime(gnssClock),
                            measurement.getSvid(),
                            null);

                    satelliteParameters.setUniqueSatId(String.format("R%02d_R1", satelliteParameters.getSatId()));

                    satelliteParameters.setSignalStrength(measurement.getCn0DbHz());

                    satelliteParameters.setConstellationType(measurement.getConstellationType());

                    if (measurement.hasCarrierFrequencyHz())
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
