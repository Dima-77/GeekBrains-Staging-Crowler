package baze;

import com.mysql.jdbc.Driver;

import java.sql.*;

/**
 * Created by Dima on 20.04.2017.
 * Программа предназначена для первичного создания таблиц в базе данных и их
 * заполнения данными для тестирования краулера на локальной машине
 * Использовать с осторожностью - при запуске удаляет существующие таблицы
 * <p>
 * Перед запуском следует установить и запустить на компьютере MySQL сервер,
 * создать пользователя mysql с именем gb, паролем 1qasde32w. Базу gb создаёт
 * программа. Последние три значения можно поменять отредактировав
 * соответственно класс DBCredentials.
 */

public class CreateQueries {

    private DBCredentials dbc = DBCredentials.getInstance();
    private Connection connection = null;
    private PreparedStatement dropBaseStt = null;
    private PreparedStatement createBaseStt = null;
    private PreparedStatement useBaseStt = null;
    private PreparedStatement dropTableStt = null;
    private PreparedStatement preparedStatement = null;
    private PreparedStatement insertPersonStt = null;
    private PreparedStatement insertKeywordsStt = null;
    private PreparedStatement selectPersonStt = null;
    private PreparedStatement insertSiteStt = null;

    //Если делать поле Name в Persons уникальным (что желательно) при его длине 2048 и CHARSET=utf8 получим ошибку:
    //com.mysql.jdbc.exceptions.jdbc4.MySQLSyntaxErrorException: Specified key was too long; max key length is 3072 bytes
    //пока ограничил длиной 1024.

    private final String[] tablesParam = {
            "Persons (ID INT NOT NULL AUTO_INCREMENT, Name VARCHAR(1024) UNIQUE, PRIMARY KEY (ID))",
            "Keywords (ID INT NOT NULL AUTO_INCREMENT PRIMARY KEY, Name VARCHAR(2048), " +
                    "Person_ID INT, FOREIGN KEY (Person_ID) REFERENCES Persons (ID) ON DELETE CASCADE ON UPDATE CASCADE)",
            "Sites (ID INT NOT NULL AUTO_INCREMENT PRIMARY KEY, Name VARCHAR(256) UNIQUE, " +
                    "Open_Tag VARCHAR(512), Close_Tag VARCHAR(512), Date_Tag VARCHAR(512))",
            "Pages (ID INT NOT NULL AUTO_INCREMENT PRIMARY KEY, URL VARCHAR(512) UNIQUE, " +
                    "Site_ID INT, FOREIGN KEY (Site_ID) REFERENCES Sites (ID) ON DELETE CASCADE ON UPDATE CASCADE, " +
                    "Modified DATETIME, Last_Scan_Date DATETIME, Text TEXT(65536))",
            "Person_Page_Rank (Person_ID INT, Page_ID INT, Rank INT, " +
                    "FOREIGN KEY (Person_ID) REFERENCES Persons (ID) ON DELETE CASCADE ON UPDATE CASCADE, " +
                    "FOREIGN KEY (Page_ID) REFERENCES Pages (ID) ON DELETE CASCADE ON UPDATE CASCADE)"
    };

    private final String[][] persons = {
            {"Путин", "Путина", "Путину", "Путиным", "Путине"},
            {"Медведев", "Медведева", "Медведеву", "Медведевым", "Медведеве"},
            {"Навальный", "Навального", "Навальному", "Навальным", "Навальном"}
    };

    //Массив {сайт, теги статьи, теги даты}, несколько тегов разделяются ";".
    //Последний тег статьи содержит тело статьи (его наличие может говорить, что страница содержит статью),
    //первые могут встречаться на других типах страниц.
    //Пожалуй, нужно соглашение, чтобы последним был тег статьи.
    private final String[][] sites = {
            {"http://ria.ru", "h1.b-article__title;div.b-article__body p",
                    "b-article__info-date-update span;div.b-article__info-date span"},
            {"http://lenta.ru", "h1.b-topic__title;h2.b-topic__rightcol;div p", "div.b-topic__info time.g-date"},
            {"http://tass.ru",
                    "h1.b-material__title;div.b-material__info span.b-material__preview;div.b-material-text p",
                    "div.b-material__info span.b-material__date"},
            {"http://www.newsru.com", "h1.article-title;div.article-text p", "div.article-date"},
            {"http://www.rbc.ru", "div.article__header__title span;div.article__text__overview;div.article__text p",
                    "div.article__header__info-block span.article__header__date"},
    };

    public CreateQueries() {
        try {
            DriverManager.registerDriver(new Driver());
            connection = DriverManager.getConnection(dbc.getConnectionURL(), dbc.getProperties());
        } catch (SQLException e) {
            System.out.println("DB connection error. Please send information below to support:");
            e.printStackTrace();
        }

        try {
            //Так как модуль предназначен для первичного заполнения базы, удаляем существующие таблицы
            dropBase();
            //dropTables();
            createBase();
            useBase();
            createTables();
            insertPersons();
            insertKeywords();
            insertSites();
        } catch (SQLException e) {
            System.out.println("PreparedStatement error. Please send information below to support:");
            e.printStackTrace();
        }

        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void dropBase() throws SQLException {
        dropBaseStt = connection.prepareStatement("DROP SCHEMA IF EXISTS " + dbc.getDB_NAME() + ";");
        System.out.println(dropBaseStt.toString());
        dropBaseStt.execute();
    }

    private void createBase() throws SQLException {
        createBaseStt = connection.prepareStatement("CREATE DATABASE " +
                dbc.getDB_NAME() + " DEFAULT CHARACTER SET=utf8;");
        System.out.println(createBaseStt.toString());
        createBaseStt.execute();
    }

    private void useBase() throws SQLException {
        useBaseStt = connection.prepareStatement("USE " + dbc.getDB_NAME() + ";");
        System.out.println(useBaseStt.toString());
        useBaseStt.execute();
    }

    private void dropTables() throws SQLException {
        //Порядок таблиц важен - от дочерних к родительским, при создании наоборот
        String tables = "";
        for (int i = tablesParam.length - 1; i > 0; i--) {
            tables = tables + " " + tablesParam[i].split("\\s+")[0] + ",";
        }
        if (tablesParam.length > 0) tables = tables + " " + tablesParam[0].split("\\s+")[0];
        if (!tables.isEmpty()) {
            dropTableStt = connection.prepareStatement("DROP TABLE IF EXISTS" + tables + ";");
            System.out.println(dropTableStt.toString());
            dropTableStt.execute();
        }
    }

    private void createTables() throws SQLException {
        for (int i = 0; i < tablesParam.length; i++) {
            preparedStatement = connection.prepareStatement("CREATE TABLE " + tablesParam[i] + ";");
            System.out.println(preparedStatement.toString());
            preparedStatement.execute();
        }
    }

    private void insertPersons() throws SQLException {
        insertPersonStt = connection.prepareStatement("INSERT INTO Persons (Name) VALUES (?);");
        for (int i = 0; i < persons.length; i++) {
            insertPersonStt.setString(1, persons[i][0]);
            if (insertPersonStt.executeUpdate() == 0)
                System.out.println("Insert person " + persons[i][0] + ". No result");
        }
    }

    private void insertKeywords() throws SQLException {
        insertKeywordsStt = connection.prepareStatement("INSERT INTO Keywords (Name, Person_ID) VALUES (?, ?);");
        selectPersonStt = connection.prepareStatement("SELECT ID FROM Persons WHERE Name = ?;");
        ResultSet resultSet;
        int personID = 0;
        for (int i = 0; i < persons.length; i++) {
            selectPersonStt.setString(1, persons[i][0]);
            resultSet = selectPersonStt.executeQuery();
            if (resultSet.next()) {
                personID = resultSet.getInt("ID");
            } else {
                System.out.println("Select person " + persons[i][0] + ". No result!");
            }
            for (int j = 0; j < persons[i].length; j++) {
                insertKeywordsStt.setString(1, persons[i][j]);
                insertKeywordsStt.setInt(2, personID);
                insertKeywordsStt.execute();
            }
        }
    }

    private void insertSites() throws SQLException {
        insertSiteStt = connection.prepareStatement("INSERT INTO Sites (Name, Open_Tag, Date_Tag) VALUES (?, ?, ?);");
        for (int i = 0; i < sites.length; i++) {
            insertSiteStt.setString(1, sites[i][0]);
            insertSiteStt.setString(2, sites[i][1]);
            insertSiteStt.setString(3, sites[i][2]);
            insertSiteStt.execute();
        }
    }

}
