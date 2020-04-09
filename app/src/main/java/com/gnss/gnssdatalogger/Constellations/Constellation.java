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

import android.location.GnssMeasurementsEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Set;


/**
 * Created by Mateusz Krainski on 1/28/2018.
 * This class implements an abstract class Constellation, which should be extended by each
 * implemented satellite constellation which is to be used.
 */

public abstract class Constellation {

    /**
     * Additional definition of an ID for a new constellation type
     */
    public static final int CONSTELLATION_GALILEO_IonoFree = 998; //todo is there a better way to define this?
    public static final int CONSTELLATION_GPS_IonoFree = 997; //todo is there a better way to define this?


    /**
     *
     * @param index id
     * @return satellite of that id
     */
    public abstract SatelliteParameters getSatellite(int index);

    /**
     *
     * @return all satellites registered in the object
     */
    public abstract List<SatelliteParameters> getSatellites();


    /**
     *
     * @return size of the visible constellation
     */
    public abstract int getVisibleConstellationSize();

    /**
     *
     * @return size of the used constellation
     */
    public abstract int getUsedConstellationSize();




    /**
     * Returns signal strength to a satellite given by an index.
     * Warning: index is the index of the satellite as stored in internal list, not it's id.
     * @param index index of satellite
     * @return signal strength for the satellite given by {@code index}.
     */
    public abstract double getSatelliteSignalStrength(int index);

    /**
     * @return ID of the constellation
     */
    public abstract int getConstellationId();


    /**
     *
     * @return name of the constellation
     */
    public abstract String getName();

    /**
     * method invoked on every GNSS measurement event update. It should update satellite's internal
     * parameters.
     * @param event GNSS event
     */
    public abstract void updateMeasurements(GnssMeasurementsEvent event);
}
