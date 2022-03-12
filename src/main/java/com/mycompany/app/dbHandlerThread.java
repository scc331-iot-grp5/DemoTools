package com.mycompany.app;
import java.sql.*;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class dbHandlerThread implements Runnable {
    Data data;
    String key;
    ArrayList<ArrayList<Double>> points;
    ArrayList<Integer> directions;
    ArrayList<Integer> accelerations;
    Connection conn;
    public dbHandlerThread(Data data, String key, ArrayList<ArrayList<Double>> points, ArrayList<Integer> directions, ArrayList<Integer> accelerations, Connection conn){
        this.data = data;
        this.key = key;
        this.points = points;
        this.directions = directions;
        this.accelerations = accelerations;
        this.conn = conn;
    }
    public void run() {
        for(int i=0; i<points.size(); i++){

            int temperature = data.microbitTemperature.get(key).get(i);
            int speed = data.microbitSpeed.get(key).get(i);
            int volume = data.microbitVolume.get(key).get(i);
            int direction = directions.get(i);
            int acceleration = accelerations.get(i);
            uploadData(key, i, temperature, acceleration, direction, points.get(i), volume, speed);
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    public void uploadData(String deviceID, int heartbeat, int temperature, int accelerometer, int compass, ArrayList<Double> location, int volume, int speed) {
        try {
            conn.setAutoCommit(false);
            String readingsQuery = "INSERT INTO readings(device_id, heartbeat, reported_at) VALUES(" + deviceID + ", " + heartbeat + ", " + new Timestamp(System.currentTimeMillis()) + ");";
            PreparedStatement ps = conn.prepareStatement(readingsQuery, PreparedStatement.RETURN_GENERATED_KEYS);
            ps.executeUpdate();
            int generatedID = 0;
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                generatedID = rs.getInt(1);
            }

            String temperatureQuery = "INSERT into temperature_readings(reading_id, degrees) VALUES(" + generatedID + ", " + temperature + ");";
            String speedQuery = "INSERT into speed_readings(reading_id, speed) VALUES(" + generatedID + ", " + speed + ");";
            String volumeQuery = "INSERT into volume_readings(reading_id, decibels) VALUES(" + generatedID + ", " + volume + ");";
            String accelerationQuery = "INSERT into acceleration_readings(reading_id, x) VALUES(" + generatedID + ", " + accelerometer + ");";
            String directionQuery = "INSERT into compass_readings(reading_id, heading) VALUES(" + generatedID + ", " + compass + ");";
            String locationQuery = "INSERT into location_readings(reading_id, latitude, longitude) VALUES(" + generatedID + ", " + location.get(0) + ", " + location.get(1) + ");";
            Statement statement = conn.createStatement();
            statement.addBatch(temperatureQuery);
            statement.addBatch(speedQuery);
            statement.addBatch(volumeQuery);
            statement.addBatch(accelerationQuery);
            statement.addBatch(directionQuery);
            statement.addBatch(locationQuery);

            statement.executeBatch();

            conn.commit();
        } catch (Exception e) {

        }
    }
}

