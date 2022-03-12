/**
 * Copyright 2019 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.mycompany.app;

import com.esri.arcgisruntime.UnitSystem;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.*;


import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.esri.arcgisruntime.geometry.*;
import com.esri.arcgisruntime.geoanalysis.LocationDistanceMeasurement;
import com.esri.arcgisruntime.UnitSystem;
import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.mapping.BasemapStyle;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.ColorUtil;


import javafx.application.Application;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.shape.Line;
import javafx.scene.paint.Color;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ColorPicker;
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import javax.swing.*;
import java.awt.event.*;
import javax.swing.filechooser.*;

public class App extends Application implements Serializable{

    private MapView mapView;
    private List<SimpleMarkerSymbol> markers;
    private Button addMicrobitButton;
    private ComboBox<String> symbolBox;
    private TextField volumeField;
    private TextField temperatureField;
    private Button openDemoButton;
    private Button saveDemoButton;
    private Button runDemoButton;

    private Polyline line;
    private Multipoint points;
    private LocationDistanceMeasurement distanceMeasurement;
    LinearUnit unitOfMeasurement = new LinearUnit(LinearUnitId.METERS);

    private String activeMicrobit;
    private boolean removeActive;
    private boolean drawingActive = true;
    private HashMap<String, PointCollection> microbitPoints = new HashMap();
    private HashMap<String, Integer> microbitPrimaryColor = new HashMap<>();
    private HashMap<String, Integer> microbitSecondaryColor = new HashMap<>();
    private HashMap<String, GraphicsOverlay> microbitPointOverlay = new HashMap<>();
    private HashMap<String, GraphicsOverlay> microbitLineOverlay = new HashMap<>();
    private HashMap<String, ArrayList<Integer>> microbitVolume = new HashMap<>();
    private HashMap<String, ArrayList<Integer>> microbitTemperature = new HashMap<>();
    private HashMap<String, ArrayList<Integer>> microbitSpeed = new HashMap<>();
    private Graphic identifiedGraphic;



    public HashMap<String, ArrayList<ArrayList<Double>>> pointCollectionToArray(HashMap<String, PointCollection> microbitPoints){
        HashMap<String, ArrayList<ArrayList<Double>>> outputPoints = new HashMap<>();
        for(Map.Entry<String, PointCollection> entry : microbitPoints.entrySet()){
            ArrayList<ArrayList<Double>> pointsArray = new ArrayList<>();
            for(Iterator<Point> iterator = entry.getValue().iterator(); iterator.hasNext();){
                ArrayList<Double> pointArray = new ArrayList<>();
                Point point = iterator.next();
                pointArray.add(point.getX());
                pointArray.add(point.getY());
                pointsArray.add(pointArray);
            }
            outputPoints.put(entry.getKey(), pointsArray);
        }
        return outputPoints;
    }

    public HashMap<String, PointCollection> pointArrayToCollection(HashMap<String, ArrayList<ArrayList<Double>>> microbitPoints){
        HashMap<String, PointCollection> outputPoints = new HashMap<>();
        for(Map.Entry<String, ArrayList<ArrayList<Double>>> entry : microbitPoints.entrySet()){
            PointCollection points = new PointCollection(mapView.getSpatialReference());
            for(ArrayList<Double> point: entry.getValue()){
                points.add(new Point(point.get(0), point.get(1)));
            }
            outputPoints.put(entry.getKey(), points);
        }
        return outputPoints;
    }

    public static void main(String[] args) {
        Application.launch(args);
    }

    public SimpleMarkerSymbol simplePointSymbol(int primaryColor, int secondaryColor){
        System.out.println(primaryColor);
        SimpleMarkerSymbol simpleMarkerSymbol =
                new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, primaryColor, 10);
        SimpleLineSymbol outlineSymbol =
                new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, secondaryColor, 2);

        simpleMarkerSymbol.setOutline(outlineSymbol);
        return simpleMarkerSymbol;
    }

    public SimpleLineSymbol simpleLineSymbol(int primaryColor){
        SimpleLineSymbol lineSymbol =
                new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, primaryColor, 3);
        return lineSymbol;
    }

    public void updatePoints(GraphicsOverlay graphicsOverlay){
        System.out.println(microbitPoints.get(activeMicrobit));
        MultipointBuilder pointBuilder = new MultipointBuilder(microbitPoints.get(activeMicrobit));
        points = pointBuilder.toGeometry();
        System.out.println(microbitPrimaryColor.get(activeMicrobit));
        Graphic pointGraphic = new Graphic(points, simplePointSymbol(microbitPrimaryColor.get(activeMicrobit), microbitSecondaryColor.get(activeMicrobit)));
        pointGraphic.setZIndex(10);
        graphicsOverlay.getGraphics().clear();
        graphicsOverlay.getGraphics().add(pointGraphic);
    }

    public void updateLine(GraphicsOverlay graphicsOverlay){
        PolylineBuilder polylineBuilder = new PolylineBuilder(microbitPoints.get(activeMicrobit));
        line = polylineBuilder.toGeometry();
        SimpleLineSymbol lineSymbol = simpleLineSymbol(microbitPrimaryColor.get(activeMicrobit));
        Graphic lineGraphic = new Graphic(line, lineSymbol);
        graphicsOverlay.getGraphics().clear();
        graphicsOverlay.getGraphics().add(lineGraphic);
    }



    @Override
    public void start(Stage stage) {
        Screen screen = Screen.getPrimary();
        Rectangle2D bounds = screen.getVisualBounds();

        // set the title and size of the stage and show it
        stage.setTitle("My Map App");
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());
        stage.show();

        // create a JavaFX scene with a stack pane as the root node and add it to the scene
        StackPane stackPane = new StackPane();
        Scene scene = new Scene(stackPane);
        stage.setScene(scene);

        VBox controlsVBox = new VBox(6);
        controlsVBox.setBackground(new Background(new BackgroundFill(Paint.valueOf("rgba(0,0,0,0.3)"), CornerRadii.EMPTY,
                Insets.EMPTY)));
        controlsVBox.setPadding(new Insets(10.0));
        controlsVBox.setMaxSize(200, 400);
        controlsVBox.getStyleClass().add("panel-region");

        // create buttons for user interaction
        addMicrobitButton = new Button("Add Microbit");
        addMicrobitButton.setMaxWidth(Double.MAX_VALUE);

        // create combo box for the UI
        Label symbolLabel = new Label("Microbits:");
        symbolLabel.getStyleClass().add("panel-label");
        symbolBox = new ComboBox<>();
        symbolBox.getSelectionModel().selectFirst();
        symbolBox.setMaxWidth(Double.MAX_VALUE);

        Label primaryColorLabel = new Label("Select Primary Color:");
        ColorPicker primaryColorPicker = new ColorPicker();

        Label secondaryColorLabel = new Label("Select Secondary Color:");
        ColorPicker secondaryColorPicker = new ColorPicker();

        Label volumeLabel = new Label("Volume:");
        volumeField = new TextField();
        volumeField.setText("30");

        Label temperatureLabel = new Label("Temperature:");
        temperatureField = new TextField();
        temperatureField.setText("18");

        openDemoButton = new Button("Open Demo");
        openDemoButton.setMaxWidth(Double.MAX_VALUE);

        saveDemoButton = new Button("Save Demo");
        saveDemoButton.setMaxWidth(Double.MAX_VALUE);

        runDemoButton = new Button("Run Demo");
        runDemoButton.setMaxWidth(Double.MAX_VALUE);

        controlsVBox.getChildren().addAll(openDemoButton, addMicrobitButton, symbolLabel, symbolBox, primaryColorLabel,
                primaryColorPicker, secondaryColorLabel, secondaryColorPicker, volumeLabel, volumeField,
                temperatureLabel, temperatureField, saveDemoButton, runDemoButton);

        VBox speedVBox = new VBox(6);
        speedVBox.setBackground(new Background(new BackgroundFill(Paint.valueOf("rgba(0,0,0,0.3)"), CornerRadii.EMPTY,
                Insets.EMPTY)));
        speedVBox.setPadding(new Insets(10.0));
        speedVBox.setMaxSize(30, 50);
        speedVBox.getStyleClass().add("panel-region");

        Text text = new Text();
        text.setText("0");
        text.setFont(Font.font("verdana", FontWeight.BOLD, FontPosture.REGULAR, 25));

        speedVBox.getChildren().addAll(text);


        EventHandler<MouseEvent> addMicrobitEventHandler = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent e) {
                TextInputDialog dialog = new TextInputDialog();
                dialog.setHeaderText(null);
                dialog.setGraphic(null);
                dialog.setTitle("Add Microbit");
                dialog.setContentText("MicrobitID:");

// Traditional way to get the response value.
                Optional<String> result = dialog.showAndWait();
                if (result.isPresent()){
                    activeMicrobit = result.get();
                    if(symbolBox.getItems().contains(result.get())){
                        symbolBox.getSelectionModel().select(result.get());
                        return;
                    }
                    symbolBox.getItems().add(result.get());
                    symbolBox.getSelectionModel().selectLast();
                    PointCollection pointCoords = new PointCollection(mapView.getSpatialReference());
//                    activeMicrobit = result.get();
                    microbitPoints.put(result.get(), pointCoords);
                }

            }
        };

        addMicrobitButton.addEventHandler(MouseEvent.MOUSE_CLICKED, addMicrobitEventHandler);

        String yourApiKey = "AAPKc20e20fa4c3140e491f5e5e5b94a0a9fG8KqpPlOQWK3W7xcwUlqj3GNQIw-L7CqKiZSmN56I-rp13LqqwfWyTv2VFNgshdc";
        ArcGISRuntimeEnvironment.setApiKey(yourApiKey);

        // create a MapView to display the map and add it to the stack pane
        mapView = new MapView();
        stackPane.getChildren().add(mapView);

        // create an ArcGISMap with an imagery basemap
        ArcGISMap map = new ArcGISMap(BasemapStyle.OSM_STANDARD);

        // display the map by setting the map on the map view
        mapView.setMap(map);
        mapView.setViewpoint(new Viewpoint(54.009513, -2.786062, 5000));



        EventHandler<MouseEvent> lineHandler = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent e) {
                if(microbitPoints.get(activeMicrobit).size()>1){
                    microbitPoints.get(activeMicrobit).remove(microbitPoints.get(activeMicrobit).size()-1);
                }
                if(!drawingActive){

                    updateLine(microbitLineOverlay.get(activeMicrobit));

                    mapView.removeEventHandler(MouseEvent.MOUSE_MOVED, this);
                    return;
                }
                try{
                    Point mapLocation = mapView.screenToLocation(new Point2D(e.getX(), e.getY()));
                    microbitPoints.get(activeMicrobit).add(mapLocation.getX(), mapLocation.getY());

                    updateLine(microbitLineOverlay.get(activeMicrobit));


                    Point point1 = new Point(microbitPoints.get(activeMicrobit).get(microbitPoints.get(activeMicrobit).size()-1).getX(), microbitPoints.get(activeMicrobit).get(microbitPoints.get(activeMicrobit).size()-1).getY(), mapView.getSpatialReference());
                    String point1UTM = CoordinateFormatter.toUtm(point1, CoordinateFormatter.UtmConversionMode.NORTH_SOUTH_INDICATORS, true);
                    Point point2 = new Point(microbitPoints.get(activeMicrobit).get(microbitPoints.get(activeMicrobit).size()-2).getX(), microbitPoints.get(activeMicrobit).get(microbitPoints.get(activeMicrobit).size()-2).getY(), mapView.getSpatialReference());
                    String point2UTM = CoordinateFormatter.toUtm(point2, CoordinateFormatter.UtmConversionMode.NORTH_SOUTH_INDICATORS, true);

                    String[] point1Coords = point1UTM.split(" ");
                    String[] point2Coords = point2UTM.split(" ");
                    double distance = Math.hypot(Integer.valueOf(point1Coords[1])- Integer.valueOf(point2Coords[1]), Integer.valueOf(point1Coords[2])- Integer.valueOf(point2Coords[2]));
                    long speed = Math.round(distance*3.6/2);
                    text.setText(String.valueOf(speed));
                } catch (Exception error){
//                    System.out.println(error);
                }

            }
        };

        EventHandler<MouseEvent> pointHandler = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent e) {
                if(e.isSecondaryButtonDown()){
//                    updateLine(microbitLineOverlay.get(activeMicrobit));
//                    mapView.removeEventHandler(MouseEvent.MOUSE_PRESSED, this);
                    if(drawingActive){
                        drawingActive = false;
                        return;
                    }
                    Point mapLocation = mapView.screenToLocation(new Point2D(e.getX(), e.getY()));
                    microbitPoints.get(activeMicrobit).add(mapLocation.getX(), mapLocation.getY());
                    mapView.addEventHandler(MouseEvent.MOUSE_MOVED, lineHandler);
                    drawingActive = true;
                }
                if(!drawingActive){
                    return;
                }
                if(e.isStillSincePress())
                {
                    mapView.addEventHandler(MouseEvent.MOUSE_MOVED, lineHandler);

                    Point mapLocation = mapView.screenToLocation(new Point2D(e.getX(), e.getY()));
                    String mapLocationLatLng = CoordinateFormatter.toLatitudeLongitude(mapLocation, CoordinateFormatter.LatitudeLongitudeFormat.DECIMAL_DEGREES, 7);
                    System.out.println(mapLocationLatLng);
                    microbitPoints.get(activeMicrobit).add(mapLocation.getX(), mapLocation.getY());
                    updatePoints(microbitPointOverlay.get(activeMicrobit));

                    if(!microbitVolume.containsKey(activeMicrobit)){
                        microbitVolume.put(activeMicrobit, new ArrayList<>());
                        microbitSpeed.put(activeMicrobit, new ArrayList<>());
                        microbitTemperature.put(activeMicrobit, new ArrayList<>());
                    }
                    microbitVolume.get(activeMicrobit).add(Integer.valueOf(volumeField.getCharacters().toString()));
                    microbitTemperature.get(activeMicrobit).add(Integer.valueOf(temperatureField.getCharacters().toString()));
                    microbitSpeed.get(activeMicrobit).add(Integer.valueOf(text.getText()));
                }



            }
        };

        symbolBox.valueProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue observable, String oldValue, String newValue) {
                activeMicrobit = newValue;
                drawingActive = true;
                removeActive = false;
                mapView.addEventHandler(MouseEvent.MOUSE_PRESSED, pointHandler);


                GraphicsOverlay lineOverlay = new GraphicsOverlay();
                GraphicsOverlay pointOverlay = new GraphicsOverlay();
                mapView.getGraphicsOverlays().add(lineOverlay);
                mapView.getGraphicsOverlays().add(pointOverlay);

                microbitPointOverlay.put(activeMicrobit, pointOverlay);
                microbitLineOverlay.put(activeMicrobit, lineOverlay);
            }
        });

        primaryColorPicker.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                microbitPrimaryColor.put(activeMicrobit, ColorUtil.colorToArgb(primaryColorPicker.getValue()));
            }
        });
        secondaryColorPicker.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                microbitSecondaryColor.put(activeMicrobit, ColorUtil.colorToArgb(secondaryColorPicker.getValue()));
            }
        });

        EventHandler<MouseEvent> openDemoHandler = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent e) {
                try{
                    JFileChooser j = new JFileChooser(new File("C:\\Users\\User\\IdeaProjects\\java-maven-starter-project\\demos"));
                    int r = j.showOpenDialog(null);

                    if (r == JFileChooser.APPROVE_OPTION)
                    {
                        FileInputStream fi = new FileInputStream(j.getSelectedFile().getAbsolutePath());
                        ObjectInputStream oi = new ObjectInputStream(fi);
                        Data data = (Data) oi.readObject();

                        for(GraphicsOverlay overlay : microbitLineOverlay.values()){
                            overlay.getGraphics().clear();
                        }
                        for(GraphicsOverlay overlay : microbitPointOverlay.values()){
                            overlay.getGraphics().clear();
                        }
                        System.out.println(data.microbitPrimaryColor);
                        microbitPrimaryColor = data.microbitPrimaryColor;
                        microbitSecondaryColor = data.microbitSecondaryColor;
                        microbitVolume = data.microbitVolume;
                        microbitSpeed = data.microbitSpeed;
                        microbitPoints = pointArrayToCollection(data.microbitPoints);

                        symbolBox.getItems().clear();

                        for(String key : data.microbitPoints.keySet()){
                            symbolBox.getItems().add(key);
                            activeMicrobit = key;
                            GraphicsOverlay pointOverlay = new GraphicsOverlay();
                            GraphicsOverlay lineOverlay = new GraphicsOverlay();
                            mapView.getGraphicsOverlays().add(lineOverlay);
                            mapView.getGraphicsOverlays().add(pointOverlay);
                            microbitLineOverlay.put(activeMicrobit, lineOverlay);
                            microbitPointOverlay.put(activeMicrobit, pointOverlay);

                            updatePoints(microbitPointOverlay.get(activeMicrobit));
                            updateLine(microbitLineOverlay.get(activeMicrobit));
                        }
                        System.out.println(activeMicrobit);
                        symbolBox.getSelectionModel().select(activeMicrobit);


                    }
                }catch(Exception error){
                    System.out.println(error);
                }
            }
        };
        openDemoButton.addEventHandler(MouseEvent.MOUSE_CLICKED, openDemoHandler);

        EventHandler<MouseEvent> saveDemoHandler = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent e) {
                System.out.println("hi2");
                try{
                    JFileChooser j = new JFileChooser(new File("C:\\Users\\User\\IdeaProjects\\java-maven-starter-project\\demos"));
                    int r = j.showSaveDialog(null);

                    if (r == JFileChooser.APPROVE_OPTION)
                    {
                        Data data = new Data(pointCollectionToArray(microbitPoints), microbitPrimaryColor, microbitSecondaryColor, microbitVolume, microbitSpeed, microbitTemperature);
                        FileOutputStream fileOut = new FileOutputStream(j.getSelectedFile().getAbsolutePath());
                        ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
                        objectOut.writeObject(data);
                        objectOut.close();
                    }
                }catch(Exception error){}
            }
        };
        saveDemoButton.addEventHandler(MouseEvent.MOUSE_CLICKED, saveDemoHandler);

        EventHandler<MouseEvent> runDemoHandler = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent e) {
                System.out.println("hi");
                Data data = new Data(pointCollectionToArray(microbitPoints), microbitPrimaryColor, microbitSecondaryColor, microbitVolume, microbitSpeed, microbitTemperature);
                new DatabaseHandler(data);
            }
        };
        runDemoButton.addEventHandler(MouseEvent.MOUSE_CLICKED, runDemoHandler);

        stackPane.getChildren().add(controlsVBox);
        StackPane.setAlignment(controlsVBox, Pos.TOP_LEFT);
        StackPane.setMargin(controlsVBox, new Insets(10, 0, 0, 10));

        stackPane.getChildren().add(speedVBox);
        StackPane.setAlignment(speedVBox, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(speedVBox, new Insets(0, 10, 20, 0));

    }

    /**
     * Stops and releases all resources used in application.
     */
    @Override
    public void stop() {

        if (mapView != null) {
            mapView.dispose();
            System.exit(0);
        }
    }
}

