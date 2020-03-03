import java.io.FileInputStream;
import java.io.FileNotFoundException;

import com.sun.rowset.internal.Row;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.text.Text;


import java.io.*;

import static jdk.nashorn.internal.objects.Global.Infinity;

// OK this is not best practice - maybe you'd like to create
// a volume data class?
// I won't give extra marks for that though.

/**
 * @author Chukwuka Ajeh 991129
 *
 * I Chukwuka Ajeh declare that my work is my own and is not a copy
*  and does not infringe of the copyright of any other owner.
 *
 * */


public class Example extends Application {
    short[][][] cthead; //store the 3D volume data set
    short min, max; //min/max value in the 3D volume data set
    short equalisedCTHead[][][];
    short tempMax, tempMin;
    short[][][] tempCTHead;
    int width = 256;
    int height = 256;
    int depth = 113;






    @Override
    public void start(Stage stage) throws IOException {
        stage.setTitle("CThead Viewer");


        ReadData();
        tempMax = max;
        tempMin = min;
        tempCTHead = cthead;
        equalisation();


        WritableImage medical_image = new WritableImage(width, height);
        ImageView imageView = new ImageView(medical_image);

        WritableImage medical_front = new WritableImage(width, depth);
        ImageView imageView1 = new ImageView(medical_front);

        WritableImage medical_side = new WritableImage(width, depth);
        ImageView imageView2 = new ImageView(medical_side);


        Button mip_button = new Button("MIP"); //an example button to switch to MIP mode

        ToggleButton equalise = new ToggleButton("Equalise");

        //BarCharts for the histograms:

        //sliders to step through the slices (z and y directions) (remember 113 slices in z direction 0-112)
        Label top_slider = new Label("Top Slider");
        Label front_slider = new Label("Front Slider");
        Label side_slider = new Label("Side Slide");
        Label resize = new Label("Nearest Neighbour Resize");
        Label biLinearResize = new Label("Bilinear Interpolation Resize Slider");
        Label top_view = new Label("Top View");
        Label front_view = new Label("Front View");
        Label side_view = new Label("Side View");

        Slider zslider = new Slider(0, 112, 0);
        Slider yslider = new Slider(0, 255, 0);
        Slider xslider = new Slider(0, 255, 0);
        Slider resizer = new Slider(1, 512, 1);
        Slider bilinearResizer = new Slider(1,4,1);

        mip_button.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                MIP(medical_image, "z");
                MIP(medical_front, "y");
                MIP(medical_side, "x");
            }
        });

        equalise.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (equalise.isSelected()) {
                    cthead = equalisedCTHead;
                    max = 255;
                    min = 0;
                } else {
                    cthead = tempCTHead;
                    min = tempMin;
                    max = tempMax;
                }
            }
        });


        zslider.valueProperty().addListener(
                new ChangeListener<Number>() {
                    public void changed(ObservableValue<? extends Number>
                                                observable, Number oldValue, Number newValue) {

                        System.out.println(newValue.intValue());
                        writeImageZ(medical_image, (int) zslider.getValue());
                    }
                });

        xslider.valueProperty().addListener(
                new ChangeListener<Number>() {
                    public void changed(ObservableValue<? extends Number>
                                                observable, Number oldValue, Number newValue) {

                        System.out.println(newValue.intValue());
                        writeImageX(medical_side, (int) xslider.getValue());
                    }
                });

        yslider.valueProperty().addListener(
                new ChangeListener<Number>() {
                    public void changed(ObservableValue<? extends Number>
                                                observable, Number oldValue, Number newValue) {

                        System.out.println(newValue.intValue());
                        writeImageY(medical_front, (int) yslider.getValue());
                    }
                });

        resizer.valueProperty().addListener(
                new ChangeListener<Number>() {
                    public void changed(ObservableValue<? extends Number> observableValue, Number oldValue, Number newValue) {
                        WritableImage top = nearestNeighbour(medical_image,newValue.intValue(),newValue.intValue());
                        imageView.setImage(top);
                        WritableImage front = nearestNeighbour(medical_front, newValue.intValue(),newValue.intValue());
                        imageView1.setImage(front);
                        WritableImage side = nearestNeighbour(medical_side, newValue.intValue(),newValue.intValue());
                        imageView2.setImage(side);
                    }
                });

        bilinearResizer.valueProperty().addListener(
                new ChangeListener<Number>() {
                    public void changed(ObservableValue<? extends Number> observableValue, Number oldValue, Number newValue) {
                        WritableImage top = biLinear(medical_image,newValue.intValue());
                        imageView.setImage(top);
                        WritableImage front = biLinear(medical_front, newValue.intValue());
                        imageView1.setImage(front);
                        WritableImage side = biLinear(medical_side, newValue.intValue());
                        imageView2.setImage(side);
                    }
                });


        GridPane root = new GridPane();
        //debugging purposes only
        root.setGridLinesVisible(true);
        // column constraints

        ColumnConstraints col0 = new ColumnConstraints();
        col0.setPercentWidth(33);
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(33);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(33);
        root.getColumnConstraints().addAll(col0, col1, col2);

        //row constraints
        RowConstraints row0 = new RowConstraints();
        row0.setPercentHeight(80);
        RowConstraints row2 = new RowConstraints();
        row2.setPercentHeight(10);
        RowConstraints row3 = new RowConstraints();
        row0.setPercentHeight(10);
        root.getRowConstraints().addAll(row0, row2, row3);

        GridPane.setConstraints(imageView, 0, 0);
        root.setConstraints(zslider, 0, 2);
        root.setConstraints(imageView1, 1, 1);
        root.setConstraints(yslider, 1, 2);
        root.setConstraints(equalise, 0, 3);
        root.setConstraints(mip_button, 1, 3);
        root.setConstraints(resizer, 1, 4);
        root.setConstraints(imageView2, 2, 1);
        root.setConstraints(xslider, 2, 2);

        root.getChildren().addAll(imageView, imageView1, imageView2, mip_button, equalise, zslider, yslider, xslider, resizer);

        // other attempt
        //create anchorPane and dimensions
        AnchorPane rootAnchorPane = new AnchorPane();
        rootAnchorPane.setPrefWidth(946);
        rootAnchorPane.setPrefHeight(501);
        //create splitplane and dimensions
        SplitPane splitPane = new SplitPane();
        splitPane.setPrefWidth(946);
        splitPane.setPrefHeight(501);
        splitPane.setDividerPositions(0.2319915254237288);
        splitPane.setLayoutY(-4.0);
        splitPane.setMaxHeight(-Infinity);
        splitPane.setMaxWidth(-Infinity);
        splitPane.setMinHeight(-Infinity);
        splitPane.setMinWidth(-Infinity);
        rootAnchorPane.setBottomAnchor(splitPane,0.0);
        rootAnchorPane.setLeftAnchor(splitPane,0.0);
        rootAnchorPane.setRightAnchor(splitPane,0.0);
        rootAnchorPane.setTopAnchor(splitPane,0.0);
        //add splitpane to anchorplane
        rootAnchorPane.getChildren().addAll(splitPane);
        //Create the two anchorPanes for the splitpane
        AnchorPane anchLeft = new AnchorPane();
        anchLeft.setMinHeight(0);
        anchLeft.setMinWidth(0);
        anchLeft.setPrefHeight(160);
        anchLeft.setPrefWidth(100);
        anchLeft.setStyle("-fx-background-color: gray");
        AnchorPane anchRight = new AnchorPane();
        anchRight.setMinHeight(0);
        anchRight.setMinWidth(0);
        anchRight.setPrefHeight(160);
        anchRight.setPrefWidth(100);
        anchRight.setStyle("-fx-background-color: darkgrey");
        //add the anchorPanes:
        splitPane.getItems().addAll(anchLeft,anchRight);
        //populate the left anchorPane:
        VBox menu = new VBox();
        menu.setLayoutY(84);
        menu.setLayoutX(29);
        menu.setPrefHeight(501);
        menu.setPrefWidth(216);
        anchLeft.setBottomAnchor(menu,0.0);
        anchLeft.setLeftAnchor(menu,0.0);
        anchLeft.setRightAnchor(menu,0.0);
        anchLeft.setTopAnchor(menu,0.0);
        anchLeft.getChildren().addAll(menu);
        //populate the menu with borderpanes
        BorderPane pane = new BorderPane();
        pane.setPrefHeight(118.0);
        pane.setPrefWidth(216);
        //pane.setStyle("-fx-background-color: green");
        BorderPane pane1 = new BorderPane();
        pane1.setPrefHeight(118.0);
        pane1.setPrefWidth(216);
        //pane1.setStyle("-fx-background-color: pink");
        BorderPane pane2 = new BorderPane();
        pane2.setPrefHeight(118.0);
        pane2.setPrefWidth(216);
        //pane2.setStyle("-fx-background-color: yellow");
        BorderPane pane3 = new BorderPane();
        pane3.setPrefHeight(118.0);
        pane3.setPrefWidth(216);
        //pane3.setStyle("-fx-background-color: gray");
        BorderPane pane4 = new BorderPane();
        pane4.setPrefHeight(118.0);
        pane4.setPrefWidth(216);
        //pane4.setStyle("-fx-background-color: violet");
        BorderPane pane5 = new BorderPane();
        pane5.setPrefHeight(118.0);
        pane5.setPrefWidth(216);
        BorderPane pane6 = new BorderPane();
        pane6.setPrefHeight(118.0);
        pane6.setPrefWidth(216);
        //pane5.setStyle("-fx-background-color: cyan");
        menu.getChildren().addAll(pane,pane1,pane2,pane3,pane6,pane4,pane5);
        //populate the panes:
        pane.setTop(top_slider);
        top_slider.setPrefHeight(33);
        top_slider.setPrefWidth(187);
        top_slider.setTextAlignment(TextAlignment.CENTER);
        pane.setAlignment(top_slider,Pos.CENTER);
        pane.setCenter(zslider);
        pane.setAlignment(top_slider,Pos.CENTER);
        //pane2:
        pane1.setTop(front_slider);
        front_slider.setPrefHeight(33);
        front_slider.setPrefWidth(187);
        front_slider.setTextAlignment(TextAlignment.CENTER);
        pane1.setAlignment(front_slider,Pos.CENTER);
        pane1.setCenter(yslider);
        pane1.setAlignment(front_slider,Pos.CENTER);
        //pane3:
        pane2.setTop(side_slider);
        side_slider.setPrefHeight(33);
        side_slider.setPrefWidth(187);
        side_slider.setTextAlignment(TextAlignment.CENTER);
        pane2.setAlignment(side_slider,Pos.CENTER);
        pane2.setCenter(xslider);
        pane2.setAlignment(side_slider,Pos.CENTER);
        //pane 3:
        pane3.setTop(resize);
        resize.setPrefHeight(33);
        resize.setPrefWidth(187);
        resize.setTextAlignment(TextAlignment.CENTER);
        pane3.setAlignment(resize,Pos.CENTER);
        pane3.setCenter(resizer);
        pane3.setAlignment(resize,Pos.CENTER);

        //pane 6:
        pane6.setTop(biLinearResize);
        biLinearResize.setPrefHeight(33);
        biLinearResize.setPrefWidth(187);
        biLinearResize.setTextAlignment(TextAlignment.CENTER);
        pane6.setAlignment(biLinearResize,Pos.CENTER);
        pane6.setCenter(bilinearResizer);
        pane6.setAlignment(biLinearResize,Pos.CENTER);
        //pane 4:
        mip_button.setPrefHeight(51);
        mip_button.setPrefWidth(192);
        mip_button.setTextAlignment(TextAlignment.CENTER);
        pane4.setAlignment(mip_button,Pos.CENTER);
        pane4.setCenter(mip_button);
        pane4.setAlignment(mip_button,Pos.CENTER);
        //Pane 5:
        equalise.setPrefHeight(51);
        equalise.setPrefWidth(192);
        equalise.setTextAlignment(TextAlignment.CENTER);
        pane5.setAlignment(equalise,Pos.CENTER);
        pane5.setCenter(equalise);
        pane5.setAlignment(equalise,Pos.CENTER);

        //Populate the Left anchorPane:
        GridPane gridPane = new GridPane();
        gridPane.setPrefWidth(722.0);
        gridPane.setPrefHeight(507.0);
        gridPane.setGridLinesVisible(true);

        anchRight.setBottomAnchor(gridPane,0.0);
        anchRight.setLeftAnchor(gridPane,0.0);
        anchRight.setRightAnchor(gridPane,0.0);
        anchRight.setTopAnchor(gridPane,0.0);
        anchRight.getChildren().addAll(gridPane);
        //Define constraints:
        ColumnConstraints columnConstraints0 = new ColumnConstraints();
        columnConstraints0.setHgrow(Priority.SOMETIMES);
        columnConstraints0.setMinWidth(10.0);
        columnConstraints0.setPrefWidth(100);
        ColumnConstraints columnConstraints1 = new ColumnConstraints();
        columnConstraints1.setHgrow(Priority.SOMETIMES);
        columnConstraints1.setMinWidth(10.0);
        columnConstraints1.setPrefWidth(100);
        ColumnConstraints columnConstraints2 = new ColumnConstraints();
        columnConstraints2.setHgrow(Priority.SOMETIMES);
        columnConstraints2.setMinWidth(10.0);
        columnConstraints2.setPrefWidth(100);
        //Row Constraints:
        RowConstraints rowConstraints0 = new RowConstraints();
        rowConstraints0.setMaxHeight(230);
        rowConstraints0.setMinHeight(10);
        rowConstraints0.setMinHeight(217);
        rowConstraints0.setVgrow(Priority.SOMETIMES);
        RowConstraints rowConstraints1 = new RowConstraints();
        rowConstraints1.setMaxHeight(163.5);
        rowConstraints1.setMinHeight(0);
        rowConstraints1.setMinHeight(29.5);
        rowConstraints1.setVgrow(Priority.SOMETIMES);
        RowConstraints rowConstraints2 = new RowConstraints();
        rowConstraints2.setMaxHeight(287.0);
        rowConstraints2.setMinHeight(10);
        rowConstraints2.setMinHeight(260.5);
        rowConstraints2.setVgrow(Priority.SOMETIMES);

        //populate grid plane:
        top_view.setAlignment(Pos.CENTER);
        top_view.setPrefHeight(26.0);
        top_view.setPrefWidth(241.0);
        gridPane.setHalignment(top_view,HPos.CENTER);
        gridPane.setRowIndex(top_view,0);
        gridPane.setValignment(top_view,VPos.CENTER);
        gridPane.getChildren().addAll(top_view);
        //front view table:
        front_view.setAlignment(Pos.CENTER);
        front_view.setPrefHeight(26.0);
        front_view.setPrefWidth(241.0);
        gridPane.setHalignment(front_view,HPos.CENTER);
        gridPane.setRowIndex(front_view,0);
        gridPane.setColumnIndex(front_view,1);
        gridPane.setValignment(front_view,VPos.CENTER);
        gridPane.getChildren().addAll(front_view);
        //side view table:
        side_view.setAlignment(Pos.CENTER);
        side_view.setPrefHeight(26.0);
        side_view.setPrefWidth(241.0);
        gridPane.setHalignment(side_view,HPos.CENTER);
        gridPane.setRowIndex(side_view,0);
        gridPane.setColumnIndex(side_view,2);
        gridPane.setValignment(side_view,VPos.CENTER);
        gridPane.getChildren().addAll(side_view);

        //image view:

        gridPane.setRowIndex(imageView,1);
        gridPane.setColumnIndex(imageView,0);
        gridPane.setHalignment(imageView,HPos.CENTER);
        gridPane.getChildren().addAll(imageView);
        //front view



        gridPane.setColumnIndex(imageView1,1);
        gridPane.setHalignment(imageView1,HPos.CENTER);
        gridPane.setValignment(imageView1,VPos.TOP);
        gridPane.getChildren().addAll(imageView1);
        //side view


        gridPane.setColumnIndex(imageView2,2);
        gridPane.setRowIndex(imageView2,1);
        gridPane.setHalignment(imageView2,HPos.CENTER);
        gridPane.setValignment(imageView2,VPos.TOP);
        gridPane.getChildren().addAll(imageView2);


        Scene scene = new Scene(rootAnchorPane , 946, 501);
        stage.setScene(scene);
        stage.show();
    }





    //Function to read in the cthead data set
    public void ReadData() throws IOException {
        //File name is hardcoded here - much nicer to have a dialog to select it and capture the size from the user
        File file = new File("CThead");
        //Read the data quickly via a buffer (in C++ you can just do a single fread - I couldn't find if there is an equivalent in Java)
        DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));

        int i, j, k; //loop through the 3D data set

        min = Short.MAX_VALUE;
        max = Short.MIN_VALUE;
        //set to extreme values
        short read; //value read in
        int b1, b2; //data is wrong Endian (check wikipedia) for Java so we need to swap the bytes around

        cthead = new short[113][256][256]; //allocate the memory - note this is fixed for this data set
        //loop through the data reading it in
        for (k = 0; k < 113; k++) {
            for (j = 0; j < 256; j++) {
                for (i = 0; i < 256; i++) {
                    //because the Endianess is wrong, it needs to be read byte at a time and swapped
                    b1 = ((int) in.readByte()) & 0xff; //the 0xff is because Java does not have unsigned types
                    b2 = ((int) in.readByte()) & 0xff; //the 0xff is because Java does not have unsigned types
                    read = (short) ((b2 << 8) | b1); //and swizzle the bytes around
                    if (read < min) min = read; //update the minimum
                    if (read > max) max = read; //update the maximum
                    cthead[k][j][i] = read; //put the short into memory (in C++ you can replace all this code with one fread)
                }
            }
        }
        System.out.println(min + " " + max); //diagnostic - for CThead this should be -1117, 2248
        //(i.e. there are 3366 levels of grey (we are trying to display on 256 levels of grey)
        //therefore histogram equalization would be a good thing
    }

	
	 /*
        This function shows how to carry out an operation on an image.
        It obtains the dimensions of the image, and then loops through
        the image carrying out the copying of a slice of data into the
		image.
    */



    /**
    * @param image  parse in the image to apply maximum intensity Projection
    * @param plane parse in the axis plane for which top apply MIP
    * */
    public void MIP(WritableImage image, String plane) {
        //Get image dimensions, and declare loop variables
        int w = (int) image.getWidth(), h = (int) image.getHeight(), i, j, c, k;
        PixelWriter image_writer = image.getPixelWriter();
        short maximum = this.min;
        float col;
        //Shows how to loop through each pixel and colour
        //Try to always use j for loops in y, and i for loops in x
        //as this makes the code more readable
        //plane affects the way we loop through to apply MIP
        switch (plane) {
            case "z":
                for (j = 0; j < h; j++) {
                    for (i = 0; i < w; i++) {
                        //at this point (i,j) is a single pixel in the image
                        //here you would need to do something to (i,j) if the image size
                        //does not match the slice size (e.g. during an image resizing operation
                        //If you don't do this, your j,i could be outside the array bounds
                        //In the framework, the image is 256x256 and the data set slices are 256x256
                        //so I don't do anything - this also leaves you something to do for the assignment

                        //set the maximum will be updated finding the max insensity through
                        maximum = this.min;
                        for (k = 0; k <= 112; k++) {
                            maximum = (short) Math.max(cthead[k][j][i], maximum);
                        }
                        //get values from slice 76 (change this in your assignment)
                        //calculate the colour by performing a mapping from [min,max] -> [0,255]
                        col = (((float) maximum - (float) min) / ((float) (max - min)));
                        for (c = 0; c < 3; c++) {
                            //and now we are looping through the bgr components of the pixel
                            //set the colour component c of pixel (i,j)
                            image_writer.setColor(i, j, Color.color(col, col, col, 1.0));
                            //					data[c+3*i+3*j*w]=(byte) col;
                        } // colour loop
                    } // column loop
                } // row loop
                break;
            case "y":
                for (k = 0; k < h; k++) {
                    for (i = 0; i < w; i++) {
                        //at this point (i,j) is a single pixel in the image
                        //here you would need to do something to (i,j) if the image size
                        //does not match the slice size (e.g. during an image resizing operation
                        //If you don't do this, your j,i could be outside the array bounds
                        //In the framework, the image is 256x256 and the data set slices are 256x256
                        //so I don't do anything - this also leaves you something to do for the assignment
                        maximum = this.min;
                        for (j = 0; j < 256; j++) {
                            maximum = (short) Math.max(cthead[k][j][i], maximum);
                        }
                        //get values from slice 76 (change this in your assignment)
                        //calculate the colour by performing a mapping from [min,max] -> [0,255]
                        col = (((float) maximum - (float) min) / ((float) (max - min)));
                        for (c = 0; c < 3; c++) {
                            //and now we are looping through the bgr components of the pixel
                            //set the colour component c of pixel (i,j)
                            image_writer.setColor(i, k, Color.color(col, col, col, 1.0));
                            //					data[c+3*i+3*j*w]=(byte) col;
                        } // colour loop
                    } // column loop
                } // row loop
                break;
            case "x":
                for (k = 0; k < h; k++) {
                    for (j = 0; j < w; j++) {
                        //at this point (i,j) is a single pixel in the image
                        //here you would need to do something to (i,j) if the image size
                        //does not match the slice size (e.g. during an image resizing operation
                        //If you don't do this, your j,i could be outside the array bounds
                        //In the framework, the image is 256x256 and the data set slices are 256x256
                        //so I don't do anything - this also leaves you something to do for the assignment
                        maximum = this.min;
                        for (i = 0; i < 256; i++) {
                            maximum = (short) Math.max(cthead[k][j][i], maximum);
                        }
                        //get values from slice 76 (change this in your assignment)
                        //calculate the colour by performing a mapping from [min,max] -> [0,255]
                        col = (((float) maximum - (float) min) / ((float) (max - min)));
                        for (c = 0; c < 3; c++) {
                            //and now we are looping through the bgr components of the pixel
                            //set the colour component c of pixel (i,j)
                            image_writer.setColor(j, k, Color.color(col, col, col, 1.0));
                            //					data[c+3*i+3*j*w]=(byte) col;
                        } // colour loop
                    } // column loop
                } // row loop
                break;
        }

    }


    /*Writing the slices */

    /*Method to create images and cycle through with slider */
    public void writeImageZ(WritableImage image, int zIndex) {
        int w = (int) image.getWidth(), h = (int) image.getHeight(), i, j, c;
        PixelWriter image_writer = image.getPixelWriter();
        float col;
        short datum;
        //Shows how to loop through each pixel and colour
        //Try to always use j for loops in y, and i for loops in x
        //as this makes the code more readable
        for (j = 0; j < h; j++) {
            for (i = 0; i < w; i++) {
                //at this point (i,j) is a single pixel in the image
                //here you would need to do something to (i,j) if the image size
                //does not match the slice size (e.g. during an image resizing operation
                //If you don't do this, your j,i could be outside the array bounds
                //In the framework, the image is 256x256 and the data set slices are 256x256
                //so I don't do anything - this also leaves you something to do for the assignment
                datum = cthead[zIndex][j][i]; //get values from slice 76 (change this in your assignment)
                //calculate the colour by performing a mapping from [min,max] -> [0,255]
                col = (((float) datum - (float) min) / ((float) (max - min)));
                for (c = 0; c < 3; c++) {
                    //and now we are looping through the bgr components of the pixel
                    //set the colour component c of pixel (i,j)
                    image_writer.setColor(i, j, Color.color(col, col, col, 1.0));
                    //					data[c+3*i+3*j*w]=(byte) col;
                } // colour loop
            } // column loop
        } // row loop
    }
    //front view
    public void writeImageY(WritableImage image, int yIndex) {
        //Get image dimensions, and declare loop variables
        int w = (int) image.getWidth(), h = (int) image.getHeight(), i, j, c, k;
        PixelWriter image_writer = image.getPixelWriter();

        float col;
        short datum;
        //Shows how to loop through each pixel and colour
        //Try to always use j for loops in y, and i for loops in x
        //as this makes the code more readable
        for (k = 0; k < h; k++) {
            for (i = 0; i < w; i++) {
                //at this point (i,j) is a single pixel in the image
                //here you would need to do something to (i,j) if the image size
                //does not match the slice size (e.g. during an image resizing operation
                //If you don't do this, your j,i could be outside the array bounds
                //In the framework, the image is 256x256 and the data set slices are 256x256
                //so I don't do anything - this also leaves you something to do for the assignment

                datum = cthead[k][yIndex][i]; //get values from slice 76 (change this in your assignment)
                //calculate the colour by performing a mapping from [min,max] -> [0,255]
                col = (((float) datum - (float) min) / ((float) (max - min)));
                for (c = 0; c < 3; c++) {
                    //and now we are looping through the bgr components of the pixel
                    //set the colour component c of pixel (i,j)
                    image_writer.setColor(i, k, Color.color(col, col, col, 1.0));
                    //					data[c+3*i+3*j*w]=(byte) col;
                } // colour loop
            } // column loop
        } // row loop
    }

    //side view
    public void writeImageX(WritableImage image, int xIndex) {
        //Get image dimensions, and declare loop variables
        int w = (int) image.getWidth(), h = (int) image.getHeight(), i, j, c, k;
        PixelWriter image_writer = image.getPixelWriter();

        float col;
        short datum;
        //Shows how to loop through each pixel and colour
        //Try to always use j for loops in y, and i for loops in x
        //as this makes the code more readable
        for (k = 0; k < h; k++) {
            for (j = 0; j < w; j++) {
                //at this point (i,j) is a single pixel in the image
                //here you would need to do something to (i,j) if the image size
                //does not match the slice size (e.g. during an image resizing operation
                //If you don't do this, your j,i could be outside the array bounds
                //In the framework, the image is 256x256 and the data set slices are 256x256
                //so I don't do anything - this also leaves you something to do for the assignment

                datum = cthead[k][j][xIndex]; //get values from slice 76 (change this in your assignment)
                //calculate the colour by performing a mapping from [min,max] -> [0,255]
                col = (((float) datum - (float) min) / ((float) (max - min)));
                for (c = 0; c < 3; c++) {
                    //and now we are looping through the bgr components of the pixel
                    //set the colour component c of pixel (i,j)
                    image_writer.setColor(j, k, Color.color(col, col, col, 1.0));
                    //					data[c+3*i+3*j*w]=(byte) col;
                } // colour loop
            } // column loop
        } // row loop
    }


    // Option A Part 1: Nearest Neighbour Resize
    public WritableImage nearestNeighbour(Image image, int newWidth, int newHeight) {
        WritableImage newImage = new WritableImage(newWidth, newHeight); //new Writable image initialized for resized image
        PixelWriter writer = newImage.getPixelWriter();    //writing to newImage
        PixelReader reader = image.getPixelReader();    //Reading from original image

        //For each pixel i and j is a value such that i or j multiplied by the ratio
        for (int j = 0; j < newHeight; j++) {
            int y = (int) (j * (image.getHeight() / newHeight));

            for (int i = 0; i < newWidth; i++) {
                int x = (int) (i * (image.getWidth() / newWidth)); //placeholder x and y to pass to reader to store read in values for x and y from original image

                writer.setColor(i, j, reader.getColor(x, y));
            }
        }
        return newImage;
    }



    //Option A Part 2: Bilinear Interpolation
    public WritableImage biLinear (WritableImage image, double scaling){
        int finalHeight = (int) (image.getHeight() * scaling);
        int finalWidth = (int) (image.getWidth() * scaling);
        int height = (int) image.getHeight();
        int width = (int) image.getWidth();
        WritableImage newImage = new WritableImage(finalWidth,finalHeight); //new Writable image initialized for resized image
        PixelWriter writer = newImage.getPixelWriter();	//writing to newImage
        PixelReader reader = image.getPixelReader();	//Reading from original image

        for (int x=0; x< finalWidth; x++){
            double pixelX = (double) x / finalWidth * (width-1);
            for(int y=0; y<finalHeight; y++){
                double pixelY = (double)y / finalHeight * (height-1);

                int x1  = (int) Math.floor(pixelX);
                int y1 =(int) Math.floor(pixelY);
                //System.out.println("diffInX: "+ diffInX + "  -  diffInY: " + diffInY);
                double a = reader.getColor(x1,y1).getGreen();
                double b = reader.getColor(x1 + 1, y1).getGreen();
                double c = reader.getColor(x1,y1+1).getGreen();
                double d = reader.getColor(x1+1,y1+1).getGreen();

                double deltX = pixelX - x1;
                double deltY = pixelY - y1;

                double top = a * (1-deltX) + (b * deltX);
                double bottom = c * (1-deltX) +  (d * deltX);
                double finY = (top *(1-deltY) + (bottom * deltY)) ;

                writer.setColor(x,y,Color.color(finY,finY,finY,1));

            }
        }
        return newImage;
    }


    //option B: Histogram Equalisation:
    /*create the histogram*/
    public int[] createHist() {
        int levels = 1 + (this.max - this.min);
        int[] histArr = new int[levels];
        int k, j, i;
        for (k = 0; k < 113; k++) {
            for (j = 0; j < 256; j++) {
                for (i = 0; i < 256; i++) {
                    //to shift everything by -1117 as you cannot have negative indices.
                    int index = cthead[k][j][i] - min;
                    histArr[index]++;
                }
            }

        }
        return histArr;
    }
    //Create a culmulative distribution summig up at i and i-1.
    public int[] culmulativeDistAndMapping(int[] histogram) {
        double size = 113 * 256 * 256;
        int[] culmDist = new int[histogram.length];
        int[] mapping = new int[culmDist.length];
        culmDist[0] = histogram[0];
        for (int i = 1; i < mapping.length; i++) {
            culmDist[i] = culmDist[i - 1] + histogram[i];
            double division = culmDist[i] / size;
            mapping[i] = (int) (255.0 * division);

        }
        return mapping;
    }

    //create the equalisation finding the colour to place in the equalised CTHead:
    public void equalisation() {
        equalisedCTHead = new short[cthead.length][cthead[0].length][cthead[0][0].length];
        int[] mapping = culmulativeDistAndMapping(createHist());
        for (int k = 0; k < 113; k++) {
            for (int j = 0; j < 256; j++) {
                for (int i = 0; i < 256; i++) {
                    short datum = cthead[k][j][i];
                    int col = mapping[datum - min];
                    equalisedCTHead[k][j][i] = (short) col;

                }
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

}