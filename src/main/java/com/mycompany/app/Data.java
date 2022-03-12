package com.mycompany.app;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class Data implements Serializable {
    public HashMap<String, ArrayList<ArrayList<Double>>> microbitPoints;
    public HashMap<String, Integer> microbitPrimaryColor;
    public HashMap<String, Integer> microbitSecondaryColor;
    public HashMap<String, ArrayList<Integer>> microbitVolume;
    public HashMap<String, ArrayList<Integer>> microbitSpeed;
    public HashMap<String, ArrayList<Integer>> microbitTemperature;

    public Data(HashMap<String, ArrayList<ArrayList<Double>>> microbitPoints, HashMap<String, Integer> microbitPrimaryColor,
                HashMap<String, Integer> microbitSecondaryColor, HashMap<String, ArrayList<Integer>> microbitVolume,
                HashMap<String, ArrayList<Integer>> microbitSpeed, HashMap<String, ArrayList<Integer>> microbitTemperature) {
        this.microbitPoints = microbitPoints;
        this.microbitPrimaryColor = microbitPrimaryColor;
        this.microbitSecondaryColor = microbitSecondaryColor;
        this.microbitVolume = microbitVolume;
        this.microbitSpeed = microbitSpeed;
        this.microbitTemperature = microbitTemperature;
    }

}
