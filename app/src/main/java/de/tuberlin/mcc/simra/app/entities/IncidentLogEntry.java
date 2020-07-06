package de.tuberlin.mcc.simra.app.entities;

public class IncidentLogEntry {
    public final Integer key;
    public final Double latitude;
    public final Double longitude;
    public final Long timestamp;
    public final Integer bikeType;
    public final Boolean childOnBoard;
    public final Boolean bikeWithTrailer;
    public final Integer phoneLocation;
    public final Integer incidentType;
    public final InvolvedRoadUser involvedRoadUser;
    public final Boolean scarySituation;
    public final String description;

    private IncidentLogEntry(Builder builder) {
        this.key = builder.key;
        this.latitude = builder.latitude;
        this.longitude = builder.longitude;
        this.bikeType = builder.bikeType != null ? builder.bikeType : 0;
        this.childOnBoard = builder.childOnBoard != null ? builder.childOnBoard : false;
        this.bikeWithTrailer = builder.bikeWithTrailer != null ? builder.bikeWithTrailer : false;
        this.phoneLocation = builder.phoneLocation != null ? builder.phoneLocation : 0;
        this.incidentType = builder.incidentType != null ? builder.incidentType : 0;
        this.involvedRoadUser = builder.involvedRoadUser != null ? builder.involvedRoadUser : new InvolvedRoadUser(false, false, false, false, false, false, false, false, false, false);
        this.scarySituation = builder.scarySituation != null ? builder.scarySituation : false;
        this.description = builder.description != null ? builder.description : "";
        this.timestamp = builder.timestamp;
    }

    public static IncidentLogEntry parseEntryFromLine(String string) {
        String[] dataLogLine = string.split(",", -1);
        IncidentLogEntry.Builder dataLogEntry = IncidentLogEntry.newBuilder();

        if (dataLogLine.length >= 0 && !dataLogLine[0].isEmpty()) {
            dataLogEntry.withKey(
                    Integer.valueOf(dataLogLine[0])
            );
        }

        if (dataLogLine.length >= 2 && !dataLogLine[0].isEmpty() && !dataLogLine[1].isEmpty()) {
            dataLogEntry.withGPS(
                    Double.parseDouble(dataLogLine[1]),
                    Double.parseDouble(dataLogLine[2])
            );
        }

        if (dataLogLine.length >= 3 && !dataLogLine[3].isEmpty()) {
            dataLogEntry.withTimestamp(
                    Long.parseLong(dataLogLine[3])
            );
        }

        dataLogEntry.withRideInformation(
                dataLogLine.length > 4 ? (!dataLogLine[4].isEmpty() ? Integer.parseInt(dataLogLine[4]) : 0) : 0,
                dataLogLine.length > 5 ? (!dataLogLine[5].isEmpty() ? dataLogLine[5].equals("1") : false) : false,
                dataLogLine.length > 6 ? (!dataLogLine[6].isEmpty() ? dataLogLine[6].equals("1") : false) : false,
                dataLogLine.length > 7 ? (!dataLogLine[7].isEmpty() ? Integer.parseInt(dataLogLine[7]) : 0) : 0,
                dataLogLine.length > 8 ? (!dataLogLine[8].isEmpty() ? Integer.parseInt(dataLogLine[8]) : 0) : 0,
                new InvolvedRoadUser(
                        dataLogLine.length > 9 ? (!dataLogLine[9].isEmpty() ? dataLogLine[9].equals("1") : false) : false,
                        dataLogLine.length > 10 ? (!dataLogLine[10].isEmpty() ? dataLogLine[10].equals("1") : false) : false,
                        dataLogLine.length > 11 ? (!dataLogLine[11].isEmpty() ? dataLogLine[11].equals("1") : false) : false,
                        dataLogLine.length > 12 ? (!dataLogLine[12].isEmpty() ? dataLogLine[12].equals("1") : false) : false,
                        dataLogLine.length > 13 ? (!dataLogLine[13].isEmpty() ? dataLogLine[13].equals("1") : false) : false,
                        dataLogLine.length > 14 ? (!dataLogLine[14].isEmpty() ? dataLogLine[14].equals("1") : false) : false,
                        dataLogLine.length > 15 ? (!dataLogLine[15].isEmpty() ? dataLogLine[12].equals("1") : false) : false,
                        dataLogLine.length > 16 ? (!dataLogLine[16].isEmpty() ? dataLogLine[16].equals("1") : false) : false,
                        dataLogLine.length > 17 ? (!dataLogLine[17].isEmpty() ? dataLogLine[17].equals("1") : false) : false,
                        dataLogLine.length > 20 ? (!dataLogLine[20].isEmpty() ? dataLogLine[20].equals("1") : false) : false
                ),
                dataLogLine.length > 18 ? (!dataLogLine[18].isEmpty() ? dataLogLine[18].equals("1") : false) : false,
                dataLogLine.length > 19 ? (!dataLogLine[19].isEmpty() ? dataLogLine[19] : "") : ""
        );

        return dataLogEntry.build();
    }

    public static IncidentLogEntry.Builder newBuilder() {
        return new IncidentLogEntry.Builder();
    }

    private static final Number booleanToInt(Boolean booleanValue) {
        if (booleanValue == null) {
            return 0;
        }
        return booleanValue ? 1 : 0;
    }

    /**
     * Stringifies the IncidentLogEntry Object to a CSV Log Line
     *
     * @return Log Line without new line separator
     */
    public String stringifyDataLogEntry() {
        return (key != null ? key : "") + "," +
                (latitude != null ? latitude : "") + "," +
                (longitude != null ? longitude : "") + "," +
                (timestamp != null ? timestamp : "") + "," +
                // TODO: Use enum mapping
                (bikeType != null ? bikeType : "") + "," +
                booleanToInt(childOnBoard) + "," +
                booleanToInt(bikeWithTrailer) + "," +
                (phoneLocation != null ? phoneLocation : "") + "," +
                (incidentType != null ? incidentType : "") + "," +
                booleanToInt(involvedRoadUser.bus) + "," +
                booleanToInt(involvedRoadUser.cyclist) + "," +
                booleanToInt(involvedRoadUser.pedestrian) + "," +
                booleanToInt(involvedRoadUser.deliveryVan) + "," +
                booleanToInt(involvedRoadUser.truck) + "," +
                booleanToInt(involvedRoadUser.motorcyclist) + "," +
                booleanToInt(involvedRoadUser.car) + "," +
                booleanToInt(involvedRoadUser.taxi) + "," +
                booleanToInt(involvedRoadUser.other) + "," +
                booleanToInt(scarySituation) + "," +
                booleanToInt(involvedRoadUser.electricScooter) + "," +
                description;
    }

    public static final class Builder {
        private Integer key;
        private Double latitude;
        private Double longitude;
        private Long timestamp;
        private Integer bikeType;
        private Boolean childOnBoard;
        private Boolean bikeWithTrailer;
        private Integer phoneLocation;
        private Integer incidentType;
        private InvolvedRoadUser involvedRoadUser;
        private Boolean scarySituation;
        private String description;

        public Builder withRideInformation(Integer bikeType, Boolean childOnBoard, Boolean bikeWithTrailer, Integer phoneLocation, Integer incidentType, InvolvedRoadUser involvedRoadUser, Boolean scarySituation, String description) {
            this.bikeType = bikeType;
            this.childOnBoard = childOnBoard;
            this.bikeWithTrailer = bikeWithTrailer;
            this.phoneLocation = phoneLocation;
            this.incidentType = incidentType;
            this.involvedRoadUser = involvedRoadUser;
            this.scarySituation = scarySituation;
            this.description = description;
            return this;
        }

        public Builder withTimestamp(Long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder withKey(Integer key) {
            this.key = key;
            return this;
        }

        public Builder withGPS(Double latitude, Double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
            return this;
        }

        public Builder withIncidentType(Integer incidentType) {
            this.incidentType = incidentType;
            return this;
        }


        public IncidentLogEntry build() {
            return new IncidentLogEntry(this);
        }

    }

    public static class InvolvedRoadUser {
        Boolean bus;
        Boolean cyclist;
        Boolean pedestrian;
        Boolean deliveryVan;
        Boolean truck;
        Boolean motorcyclist;
        Boolean car;
        Boolean taxi;
        Boolean other;
        Boolean electricScooter;

        public InvolvedRoadUser(Boolean bus, Boolean cyclist, Boolean pedestrian, Boolean deliveryVan, Boolean truck, Boolean motorcyclist, Boolean car, Boolean taxi, Boolean other, Boolean electricScooter) {
            this.bus = bus;
            this.cyclist = cyclist;
            this.pedestrian = pedestrian;
            this.deliveryVan = deliveryVan;
            this.truck = truck;
            this.motorcyclist = motorcyclist;
            this.car = car;
            this.taxi = taxi;
            this.other = other;
            this.electricScooter = electricScooter;
        }
    }

    public static class INCIDENT_TYPE {
        public static final int NOTHING = 0;
        public static final int CLOSE_PASS = 1;
        public static final int PULL_OUT = 2;
        public static final int HOOK = 3;
        public static final int HEAD_ON = 4;
        public static final int TAILGATING = 5;
        public static final int NEAR_DOORING = 6;
        public static final int OBSTACLE = 7;
        public static final int OTHER = 8;
    }
}
