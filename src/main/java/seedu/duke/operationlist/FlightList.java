package seedu.duke.operationlist;

import seedu.duke.terminalinfo.FlightInfo;
import seedu.duke.exceptions.SkyControlException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FlightList extends OperationList {
    private static final int FLIGHT_NUMBER_LETTER_LENGTH = 2;
    public static int flightIndex = 0;
    private static final String FLIGHT_ADD_COMMAND = "flight add";
    private static final String FLIGHT_ADD_DELIMITER = "flight add ";
    private static final String FLIGHT_DELETE_COMMAND = "flight delete";
    private static final String FLIGHT_NUMBER_DELIMITER = " fn/";
    private static final String AIRLINE_DELIMITER = " a/";
    private static final String DESTINATION_DELIMITER = " d/";
    private static final String DEPARTURE_TIME_DELIMITER = " dt/";
    private static final String GATE_NUMBER_DELIMITER = " gn/";
    private static final String CHECK_IN_ROW_DELIMITER = " c/";
    protected static final String EMPTY_STRING = "";
    protected static final int CHECK_MISSING_DETAIL = 0;
    protected static final int AIRLINE_LENGTH_LIMIT = 22;
    protected static final int DESTINATION_LENGTH_LIMIT = 22;
    protected static boolean isFlightNumberPresent = false;
    protected static boolean isDepartureTimePresent = false;
    protected static boolean isGateNumberPresent = false;
    protected static final String TIME_REGEX = "([01]\\d|2[0-3])[0-5]\\d";
    protected static final String CHECK_IN_REGEX = "[0-9]{2}-[0-9]{2}";
    protected static final String GATE_NUM_REGEX = "^([0-9]{2}$)";
    protected String flightNumber;
    protected String airline;
    protected String destination;
    protected String departureTime;
    protected String gateNumber;
    protected String checkIn;
    protected String oldDepartureTime;

    protected static int numOfFlights = 0;

    //@@author Franky4566
    public static void checkCommandLength(String description) throws SkyControlException {
        if (description.isEmpty()) {
            throw new SkyControlException(ui.showEmptyDescriptionMessage());
        }
    }

    public static String extractDetail(String command, String start, String end) throws SkyControlException {
        String extractedDetail;
        int startIndex = command.indexOf(start) + start.length();
        int endIndex = command.lastIndexOf(end);
        checkNoDetailsMissing(startIndex, endIndex);
        extractedDetail = command.substring(startIndex, endIndex).trim();
        if (extractedDetail.equals(EMPTY_STRING)) {
            throw new SkyControlException(ui.getMissingDetailsError());
        }
        return extractedDetail;
    }

    public static void checkNoDetailsMissing(int startIndex, int endIndex) throws SkyControlException {
        if (missingDetailsChecker(startIndex, endIndex)) {
            throw new SkyControlException(ui.getErrorMessage());
        }
    }

    public static boolean missingDetailsChecker(int startIndex, int endIndex) {
        boolean isMissing = false;
        if (endIndex <= startIndex || endIndex < CHECK_MISSING_DETAIL || startIndex < CHECK_MISSING_DETAIL) {
            isMissing = true;
        }
        return isMissing;
    }

    @Override
    public void addOperation(String command) throws SkyControlException {
        getNumberOfFlights();
        checkCommandLength(command.substring(FLIGHT_ADD_COMMAND.length()));
        getFlightDetails(command.substring(FLIGHT_ADD_DELIMITER.length()));
        validateDetailFormat();
        checkFlightNumberDuplicates();
        checkAvailableGateNumber();
        FlightInfo flight = new FlightInfo(flightNumber, airline, destination,
                departureTime, gateNumber, checkIn);
        flights.add(flightIndex, flight);
        flightIndex++;
        ui.showFlightAddedMessage();
    }

    //@@author JordanKwua
    @Override
    public void deleteOperation(String detail) throws SkyControlException {
        checkCommandLength(detail.substring(FLIGHT_DELETE_COMMAND.length()));
        checkValidFlightNumber(detail.substring("flight delete ".length()));
        String flightNum = detail.substring("flight delete ".length()).toUpperCase();
        findAndRemoveFlight(flightNum);
    }

    private void checkValidFlightNumber(String substring) throws SkyControlException {
        String[] letters = substring.split("");
        for (int i = 0; i < FLIGHT_NUMBER_LETTER_LENGTH; i++) {
            if (!Character.isLetter(substring.charAt(i))) {
                throw new SkyControlException(ui.getWrongFlightFormatErrorMessage());
            }
        }
        for (int i = FLIGHT_NUMBER_LETTER_LENGTH; i < letters.length; i++) {
            if (!Character.isDigit(substring.charAt(i))) {
                throw new SkyControlException(ui.getWrongFlightFormatErrorMessage());
            }
        }
    }

    //@@author shengiv
    @Override
    public void modifyFlightNum(String flightNum, String newFlightNum) throws SkyControlException {
        getNumberOfFlights();
        FlightInfo flight = findFlightInfo(flightNum);
        getFlightAttributes(flight);
        flightNumber = newFlightNum;
        validateModificationDetails(flight);
        flight.setFlightNum(newFlightNum);
        flights.add(flight);
        flightIndex++;
        ui.showUpdatedFlightNumber(flightNum, newFlightNum);
    }

    @Override
    public void modifyGateNum(String flightNum, String newGateNum) throws SkyControlException {
        getNumberOfFlights();
        FlightInfo flight = findFlightInfo(flightNum);
        getFlightAttributes(flight);
        gateNumber = newGateNum;
        validateModificationDetails(flight);
        flight.setGateNum(newGateNum);
        flights.add(flight);
        flightIndex++;
        ui.showUpdatedGateNumber(flightNum, newGateNum);
    }

    private void getFlightAttributes(FlightInfo flight) {
        flightNumber = flight.getFlightNumber();
        airline = flight.getAirline();
        destination = flight.getDestination();
        departureTime = flight.getDepartureTime();
        gateNumber = flight.getGateNum();
        checkIn = flight.getCheckLn();
    }

    private static FlightInfo findFlightInfo(String flightNum) throws SkyControlException {
        FlightInfo modifiedFlight = null;
        for (FlightInfo flight : flights) {
            if (flight.getFlightNumber().equals(flightNum)) {
                modifiedFlight = flight;
                flights.remove(flight);
                flightIndex--;
                break;
            }
        }
        if (modifiedFlight == null) {
            throw new SkyControlException(ui.getFlightNotFoundMessage(flightNum));
        } else {
            return modifiedFlight;
        }
    }

    private void validateModificationDetails(FlightInfo flight) throws SkyControlException {
        try {
            validateDetailFormat();
            checkFlightNumberDuplicates();
            checkAvailableGateNumber();
            if (isDelay) {
                checkDelayTime();
            }
        } catch (Exception e) {
            flights.add(flight);
            flightIndex++;
            throw new SkyControlException(e.getMessage());
        }
    }

    //@@author Franky4566
    @Override
    public void listOperation() {
        ui.showListOfFlights(flights);
    }

    //@@author JordanKwua
    private void findAndRemoveFlight(String flightNumber) throws SkyControlException {
        getNumberOfFlights();
        boolean isFlightFound = false;
        assert !flights.isEmpty();
        for (FlightInfo flight : flights) {
            if (flight.getFlightNumber().equals(flightNumber)) {
                isFlightFound = true;
                flights.remove(flight);
                flightIndex--;
                ui.showFlightRemovedMessage(flightNumber);
                break;
            }
        }
        if (!isFlightFound) {
            throw new SkyControlException(ui.getFlightNotFoundMessage(flightNumber));
        }
    }

    //@@author Franky4566
    @Override
    public void delayFlightDeparture(String flightNum, String newDepartureTime) throws SkyControlException {
        getNumberOfFlights();
        FlightInfo flight = findFlightInfo(flightNum);
        getFlightAttributes(flight);
        oldDepartureTime = departureTime;
        departureTime = newDepartureTime;
        validateModificationDetails(flight);
        flight.setDepartureTime(newDepartureTime);
        flights.add(flight);
        flightIndex++;
        ui.showUpdatedDepartureTime(flightNum, oldDepartureTime, newDepartureTime);
    }

    private void checkDelayTime() throws SkyControlException {
        if (Integer.parseInt(oldDepartureTime) > Integer.parseInt(departureTime)) {
            throw new SkyControlException(ui.getWrongDelayTimeError(flightNumber, oldDepartureTime));
        }
    }

    public void getNumberOfFlights() {
        assert numOfFlights >= 0;
        flightIndex = flights.size();
    }

    public void getFlightDetails(String flightDetail) throws SkyControlException {
        getFlightNumber(flightDetail);
        getAirline(flightDetail);
        getDestination(flightDetail);
        getDepartureTime(flightDetail);
        getGateNumber(flightDetail);
        getCheckIn(flightDetail);
    }

    private void getFlightNumber(String detail) throws SkyControlException {
        if (isAdd) {
            flightNumber = extractDetail(detail, FLIGHT_NUMBER_DELIMITER, AIRLINE_DELIMITER).toUpperCase();
        }
    }

    private void getAirline(String detail) throws SkyControlException {
        airline = extractDetail(detail, AIRLINE_DELIMITER, DESTINATION_DELIMITER).toUpperCase();
    }

    private void getDestination(String detail) throws SkyControlException {
        destination = extractDetail(detail, DESTINATION_DELIMITER, DEPARTURE_TIME_DELIMITER).toUpperCase();
    }

    private void getDepartureTime(String detail) throws SkyControlException {
        departureTime = extractDetail(detail, DEPARTURE_TIME_DELIMITER, GATE_NUMBER_DELIMITER).toUpperCase();
    }

    private void getGateNumber(String detail) throws SkyControlException {
        gateNumber = extractDetail(detail, GATE_NUMBER_DELIMITER, CHECK_IN_ROW_DELIMITER).toUpperCase();
    }

    private void getCheckIn(String detail) throws SkyControlException {
        int indexOfCheckIn = detail.indexOf(CHECK_IN_ROW_DELIMITER);
        int startIndex = indexOfCheckIn + CHECK_IN_ROW_DELIMITER.length();
        checkIn = detail.substring(startIndex).toUpperCase();
        if (checkIn.equals(EMPTY_STRING)) {
            throw new SkyControlException(ui.getMissingDetailsError());
        }
    }

    private void validateDetailFormat() throws SkyControlException {
        validateAirlineLength(airline);
        validateDestinationLength(destination);
        validateTime(departureTime);
        validateCheckIn(checkIn);
        validateGateNumber(gateNumber);
    }

    private void checkFlightNumberDuplicates() throws SkyControlException {
        getNumberOfFlights();
        for (int i = 0; i < flightIndex; i++) {
            validateFlight(i);
            if (isFlightDuplicate()) {
                resetChecks();
                if (isModify) {
                    throw new SkyControlException(ui.getDuplicateModifyFlightError());
                } else {
                    throw new SkyControlException(ui.getDuplicateFlightError());
                }
            }
        }
    }

    private void checkAvailableGateNumber() throws SkyControlException {
        getNumberOfFlights();
        for (int i = 0; i < flightIndex; i++) {
            validateFlight(i);
            if (isGateOccupied()) {
                resetChecks();
                if (isModify) {
                    throw new SkyControlException(ui.getDuplicateModifyGateError());
                } else {
                    throw new SkyControlException(ui.getGateOccupiedError());
                }
            }
        }
    }

    private void validateFlight(int index) {
        getNumberOfFlights();
        assert index < numOfFlights;
        checkFlightNumberExists(index);
        checkDepartureTimeExists(index);
        checkGateNumberExists(index);
    }

    private void checkFlightNumberExists(int index) {
        isFlightNumberPresent = flights.get(index).getFlightNumber().equals(flightNumber);
    }

    private void checkDepartureTimeExists(int index) {
        isDepartureTimePresent = flights.get(index).getDepartureTime().contains(departureTime);
    }

    private void checkGateNumberExists(int index) {
        isGateNumberPresent = flights.get(index).getGateNum().contains(gateNumber);
    }

    private boolean isFlightDuplicate() {
        boolean isFlightDuplicate;
        isFlightDuplicate = isFlightNumberPresent;
        return isFlightDuplicate;
    }

    private boolean isGateOccupied() {
        boolean isGateOccupied;
        isGateOccupied = isDepartureTimePresent && isGateNumberPresent;
        return isGateOccupied;
    }

    private void validateAirlineLength(String airline) throws SkyControlException {
        if (airline.length() > AIRLINE_LENGTH_LIMIT) {
            throw new SkyControlException(ui.getExceedAirlineLengthError(airline));
        }
    }

    private void validateDestinationLength(String destination) throws SkyControlException {
        if (destination.length() > DESTINATION_LENGTH_LIMIT) {
            throw new SkyControlException(ui.getExceedDestinationLengthError(destination));
        }
    }

    private void validateTime(String time) throws SkyControlException {
        Pattern p = Pattern.compile(TIME_REGEX);
        Matcher m = p.matcher(time);
        if (!m.matches()) {
            throw new SkyControlException(ui.getDepartureTimeError());
        }
    }

    private void validateCheckIn(String checkIn) throws SkyControlException {
        Pattern p = Pattern.compile(CHECK_IN_REGEX);
        Matcher m = p.matcher(checkIn);
        if (!m.matches()) {
            throw new SkyControlException(ui.getCheckInFormatError());
        }
    }

    private void validateGateNumber(String gateNumber) throws SkyControlException {
        Pattern p = Pattern.compile(GATE_NUM_REGEX);
        Matcher m = p.matcher(gateNumber);
        if (!m.matches()) {
            throw new SkyControlException(ui.getGateNumberError());
        }
    }

    private void resetChecks() {
        isFlightNumberPresent = false;
        isGateNumberPresent = false;
        isDepartureTimePresent = false;
    }

}