/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.planthealthdoctor;

import java.util.HashMap;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class SampleGattAttributes {
    private static HashMap<String, String> attributes = new HashMap();
    public static String LIGHT_MEASUREMENT = "beb5483e-36e1-4688-b7f5-ea07361b26a8";
    public static String TEMPERATURE = "61737abc-d8ed-48c8-9cdc-4ac0850f6dbb";
    public static String MOISTURE = "fcf68970-caee-49e5-badf-bf0adf2d4fb5";
    public static String HUMIDITY = "26c6701d-8730-46ff-876c-bb181f5996c5";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    static {
        // Sample Services.
        attributes.put(LIGHT_MEASUREMENT, "Light Sensor");
        attributes.put(TEMPERATURE, "Temperature Sensor");
        // Sample Characteristics.
        attributes.put(MOISTURE, "Moisture Sensor");
        attributes.put("4fafc201-1fb5-459e-8fcc-c5c9c331914b", "Plant Health Doctor");
        attributes.put(HUMIDITY, "Humidity Sensor");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}