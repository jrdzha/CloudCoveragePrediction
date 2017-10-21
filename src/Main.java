import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import utils.CoordinateObject;
import utils.DistanceCalculator;
import utils.HTTPSAccessor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Main {
    static String configFileLocation = "";
    static String configFileType = ".ccp";
    static String gpsFileLocation = "";
    static String gpsFileType = ".coor";
    static String apiKey = "";
    static HTTPSAccessor httpsAccessor = new HTTPSAccessor();
    static JSONParser jsonParser = new JSONParser();

    public static void main(String[] args){
        //launch from console
        consoleLaunch(args);

        //read config file
        JSONObject configJSONObject = new JSONObject();
        try {
            configJSONObject = (JSONObject)jsonParser.parse(new String(Files.readAllBytes(Paths.get(configFileLocation))));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        //set API key
        apiKey = (String)configJSONObject.get("api_key");

        //read gps file
        gpsFileLocation = (String)configJSONObject.get("gps_coordinates_file_location");
        String[] rawStringCoordinates = new String[0];
        try {
            rawStringCoordinates = new String(Files.readAllBytes(Paths.get(gpsFileLocation))).split("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        CoordinateObject[] rawCoordinates = new CoordinateObject[rawStringCoordinates.length];
        for(int i = 0; i < rawStringCoordinates.length; i++){
            String[] coordinate = rawStringCoordinates[i].split(",");
            rawCoordinates[i] = new CoordinateObject(Double.parseDouble(coordinate[0]), Double.parseDouble(coordinate[1]));
        }

        //generate new gps points with given resolution
        ArrayList<CoordinateObject> coordinateArrayList = new ArrayList<CoordinateObject>();
        for(int i = 0; i < rawCoordinates.length - 1; i++){
            coordinateArrayList.add(rawCoordinates[i]);
            double distance = DistanceCalculator.calculate(rawCoordinates[i], rawCoordinates[i + 1]);
            int numNewPointsNeeded = (int)(distance / (double)configJSONObject.get("resolution")) + 1;
            double deltaLongitude = rawCoordinates[i + 1].longitude - rawCoordinates[i].longitude;
            double deltaLatitude = rawCoordinates[i + 1].latitude - rawCoordinates[i].latitude;
            for(int j = 0; j < numNewPointsNeeded; j++){
                double ratio = (double)j / (double)numNewPointsNeeded;
                coordinateArrayList.add(new CoordinateObject(rawCoordinates[i].longitude + (deltaLongitude * ratio), rawCoordinates[i].latitude + (deltaLatitude * ratio)));
            }
        }
        coordinateArrayList.add(rawCoordinates[rawCoordinates.length - 1]);

        for(CoordinateObject coordinateObject : coordinateArrayList){
            System.out.println(coordinateObject.longitude + ", " + coordinateObject.latitude);
        }
        System.out.println(coordinateArrayList.size());
        //System.out.println(DistanceCalculator.calculate(new CoordinateObject(47.60451943589744, 122.33211492307692), new CoordinateObject(47.6062, 122.3321)));

        //make api calls for each gps point and calculate score according to current car location
        try {
            System.out.println(httpsAccessor.sendGet("https://api.darksky.net/forecast/" + apiKey + "/37.8267,-122.4233"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void consoleLaunch(String[] args){
        boolean isError = false;
        if(args.length == 2){
            if(args[0].equals("--config") && args[1].contains(configFileType)){
                configFileLocation = args[1];
            } else {
                isError = true;
            }
        } else if(args.length == 1){
            if(args[0].equals("--help")){
                System.out.println("Usage:");
                System.out.println("\t--config <FILE LOCATION>");
            } else {
                isError = true;
            }
        } else {
            isError = true;
        }

        if(isError){
            System.out.println("Usage: java ccp.jar [args]");
            System.out.println("\tjava ccp.jar --help for more info");
        }
    }
}