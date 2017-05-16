package test.bazetest;

import java.sql.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import baze.DBCredentials;

/**
 * Created by Dima on 21.04.2017.
 */
public class QueryTest {
    private DBCredentials dbc = DBCredentials.getInstance();
    private Connection connection = null;
    private PreparedStatement preparedStatement = null;

    public QueryTest() {
        try {
            DriverManager.registerDriver(new com.mysql.jdbc.Driver());
            connection = DriverManager.getConnection(dbc.getConnectionURL() + dbc.getDB_NAME(), dbc.getProperties());
        } catch (SQLException e) {
            System.out.println("DB connection error. Please send information below to support:");
            e.printStackTrace();
        }

        try {
            selectPersons();
            selectKeywords();
            selectSites();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void selectPersons() throws SQLException {
        preparedStatement = connection.prepareStatement("SELECT * FROM Persons ORDER BY ID;");
        ResultSet resultSet;
        int personID;
        String person;
        resultSet = preparedStatement.executeQuery();
        System.out.println("┌----┬-------------┐");
        System.out.println("│ ID │   Name      │");
        System.out.println("├----┼-------------┤");
        while (resultSet.next()) {
            personID = resultSet.getInt("ID");
            person = resultSet.getString("Name");
            System.out.println(String.format("│%4s│%-13s│", personID, person));
        }
    }

    private void selectKeywords() throws SQLException {
        preparedStatement = connection.prepareStatement("SELECT * FROM Keywords ORDER BY ID DESC;");
        ResultSet resultSet;
        int keyID;
        int personID;
        String keyword;
        resultSet = preparedStatement.executeQuery();
        System.out.println("┌----┬---------┬-------------┐");
        System.out.println("│ ID │Person ID│    Name     │");
        System.out.println("├----┼---------┼-------------┤");
        while (resultSet.next()) {
            keyID = resultSet.getInt("ID");
            personID = resultSet.getInt("Person_ID");
            keyword = resultSet.getString("Name");
            System.out.println(String.format("│%4s│%9s│%-13s│", keyID, personID, " " + keyword));
        }
    }

    private void selectSites() throws SQLException {
        preparedStatement = connection.prepareStatement("SELECT * FROM Sites ORDER BY ID;");
        ResultSet resultSet;
        int siteID;
        String url;
        String dateTags;
        String articleTags;
        resultSet = preparedStatement.executeQuery();
        System.out.println("┌----┬------------------┬---------------------------------------------┬" +
                "------------------------------------------------------------------------------------------┐");
        System.out.println("│ ID │       URL        │              Date tag                       │" +
                "                                  Article tags                                            │");
        System.out.println("├----┼------------------┼---------------------------------------------┼" +
                "------------------------------------------------------------------------------------------┤");
        while (resultSet.next()) {
            siteID = resultSet.getInt("ID");
            url = resultSet.getString("Name");
            dateTags = resultSet.getString("Date_Tag");
            articleTags = resultSet.getString("Open_Tag");
            System.out.println(String.format("│%3s │ %-17s│ %-44s│ %-89s│", siteID, url, dateTags, articleTags));
        }
    }

}
