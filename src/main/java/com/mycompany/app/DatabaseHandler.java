package com.mycompany.app;
import com.esri.arcgisruntime.geometry.CoordinateFormatter;
import com.esri.arcgisruntime.geometry.SpatialReference;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

import com.esri.arcgisruntime.geometry.Point;

public class DatabaseHandler{
    private String dbName = "iota";
    private String serverName = "dodecahedron.noah.katapult.cloud";
    private String username = "root";
    private String password = "AdaLovelace1815";
    private int port = 3306;
    private Connection conn;



    private double angleFromCoordinate(double lat1, double long1, double lat2,
                                       double long2) {

        double dLon = (long2 - long1);

        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1)
                * Math.cos(lat2) * Math.cos(dLon);

        double brng = Math.atan2(y, x);

        brng = Math.toDegrees(brng);
        brng = (brng + 360) % 360;
        brng = 360 - brng; // count degrees counter-clockwise - remove to make clockwise

        return brng;
    }

    public ArrayList<Integer> getDirections(ArrayList<ArrayList<Double>> points){
        ArrayList<Integer> directions = new ArrayList<>();

        for(int i=0; i<points.size()-1; i++){
            directions.add((int) angleFromCoordinate(points.get(i).get(0), points.get(i).get(1), points.get(i+1).get(0), points.get(i+1).get(1)));
        }
        directions.add(0);
        return directions;
    }

    public ArrayList<ArrayList<Double>> convertToLatLong(ArrayList<ArrayList<Double>> points){
        ArrayList<ArrayList<Double>> latLongPoints = new ArrayList<>();
        ArrayList<Integer> direction = getDirections(latLongPoints);
        for(int i=0; i<points.size(); i++){
            ArrayList<Double> location = points.get(i);
            Point point = new Point(location.get(0), location.get(1), SpatialReference.create(4326));
            String latlong = CoordinateFormatter.toLatitudeLongitude(point, CoordinateFormatter.LatitudeLongitudeFormat.DEGREES_DECIMAL_MINUTES, 7);
            String[] latlongSplit = latlong.split(" ");
            String lat = latlongSplit[0].substring(0, latlongSplit[0].length()-1);
            String lon = latlongSplit[1].substring(0, latlongSplit[1].length()-1);
            location.set(0, Double.valueOf(lat));
            location.set(1, Double.valueOf(lon));
            latLongPoints.add(location);
        }
        return latLongPoints;
    }

    public ArrayList<Integer> getAcceleration(ArrayList<Integer> speed){
        ArrayList<Integer> acceleration = new ArrayList<>();
        acceleration.add(0);
        for(int i=1; i<speed.size(); i++){
            double accel = ((speed.get(i) - speed.get(i-1)/0.00055555555555556))/35.30394;
            acceleration.add((int) accel);
        }
        return acceleration;
    }

    public DatabaseHandler (Data data) {
//        int counter = 0;
        for(String key : data.microbitPoints.keySet()){
            ArrayList<ArrayList<Double>> points = data.microbitPoints.get(key);
            points = convertToLatLong(points);
            ArrayList<Integer> directions = getDirections(points);
            ArrayList<Integer> accelerations = getAcceleration(data.microbitSpeed.get(key));

            Connection conn = dbCon();
            Runnable task = new dbHandlerThread(data, key, points, directions, accelerations, conn);
            Thread worker = new Thread(task);
        }
    }

    public void uploadData(String deviceID, int heartbeat, int temperature, int accelerometer, int compass, ArrayList<Double> location, int volume, int speed){
        try{
            conn.setAutoCommit(false);
            String readingsQuery = "INSERT INTO readings(device_id, heartbeat, reported_at) VALUES(" + deviceID + ", " + heartbeat + ", " + new Timestamp(System.currentTimeMillis()) + ");";
            PreparedStatement ps = conn.prepareStatement(readingsQuery, PreparedStatement.RETURN_GENERATED_KEYS);
            ps.executeUpdate();
            int generatedID = 0;
            ResultSet rs = ps.getGeneratedKeys();
            if(rs.next()){
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
        } catch (Exception e){

        }

    }
    public Connection dbCon () {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection("jdbc:mysql://" + serverName + ":" + port + "/" + dbName, username, password);
            return conn;
        } catch (Exception e) {
            System.err.println("Caught Exception: " + e);
            e.printStackTrace();
        }
        return null;
    }
}
