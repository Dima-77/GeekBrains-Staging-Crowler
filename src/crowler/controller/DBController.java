package crowler.controller;

import crowler.model.DBCredentials;
import crowler.model.Page;
import crowler.model.Rank;
import crowler.model.Site;

import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.Date;

/**
 * Created by vasily on 13.04.17.
 */
public class DBController implements DBControllerInterfce {

    private static DBCredentials dbc = DBCredentials.getInstance();

    private static Connection connection = null;
    //Для кравлинга текста
    private static PreparedStatement setPageInPagesStt = null;
    private static PreparedStatement getSiteStatement = null;
    private static PreparedStatement getSitesStt = null;
    //Для сбора статистики
    private static PreparedStatement getKeywordsStt = null;
    private static PreparedStatement getPagesByNullLastScanDateStt = null;
    private static PreparedStatement isExistRankByPersonAndPageStt = null;
    private static PreparedStatement setPersonPageRankStt = null;
    private static PreparedStatement changePersonPageRankStt = null;
    //Для проверки модификации статьи
    private static PreparedStatement getPagesByMinLastScanDateStt = null;
    private static PreparedStatement changePageTextStt = null;
    //Общие
    private static PreparedStatement changeLastScanDateInPagesStt = null;
    private static PreparedStatement getPersonByKeyStt = null;

    private static DBController ourInstance = new DBController();

    public static DBController getInstance() {
        try {
            if (connection == null || connection.isClosed()) {

                connection = DriverManager.getConnection(dbc.getConnectionURL(), dbc.getProperties());
                setPageInPagesStt = connection.prepareStatement("INSERT INTO pages (Site_ID, URL, Modified, Text) VALUES (?, ?, ?, ?);");
                getSiteStatement = connection.prepareStatement("SELECT * FROM sites WHERE id=?");
                getSitesStt = connection.prepareStatement("SELECT * FROM sites");
                getKeywordsStt = connection.prepareStatement("SELECT * FROM keywords;");
                getPagesByNullLastScanDateStt = connection.prepareStatement("SELECT id,text FROM pages WHERE last_scan_date IS NULL AND site_id=?;");
                isExistRankByPersonAndPageStt = connection.prepareStatement("SELECT * FROM person_page_rank WHERE person_id=? AND page_id=?;");
                setPersonPageRankStt = connection.prepareStatement("INSERT INTO person_page_rank (person_id, page_id, rank) VALUES (?, ?, ?);");
                changePersonPageRankStt = connection.prepareStatement("UPDATE person_page_rank SET rank=? WHERE person_id=? AND page_id=?;");
                changeLastScanDateInPagesStt = connection.prepareStatement("UPDATE pages SET last_scan_date=? WHERE id=?;");
                getPersonByKeyStt = connection.prepareStatement("SELECT name FROM persons WHERE id=?;");
                getPagesByMinLastScanDateStt = connection.prepareStatement("SELECT id,url,modified,text FROM pages WHERE last_scan_date IS NOT NULL AND site_id=? ORDER BY last_scan_date ASC LIMIT ?;");
                changePageTextStt = connection.prepareStatement("UPDATE pages SET modified=?, last_scan_date=NULL, text=? WHERE id=?;");
                changeLastScanDateInPagesStt = connection.prepareStatement("UPDATE pages SET last_scan_date=? WHERE id=?;");

            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            System.out.println("DB connection error. Please send information below to support:");
            e.printStackTrace();
        }

        return ourInstance;
    }


    @Override
    public void putPage(Page page) {

        try {
            setPageInPagesStt.setInt(1, page.getSite().getId());
            setPageInPagesStt.setString(2, page.getUrl().toString());
            //Передаём в БД дату, время в виде текста 'YYYY-MM-DD HH:MM:SS.MMM'
            setPageInPagesStt.setString(3, formatDateForDB(page.getModified()));
            //Next statement set the hours, minutes, seconds, and milliseconds to zero
            //preparedStatement.setDate(3, new Date(page.getModified().getTime()));
            //Deprecated
            //preparedStatement.setDate(3, new Date(page.getModified().getYear(), page.getModified().getMonth(), page.getModified().getDay()));
            setPageInPagesStt.setString(4, page.getText());

            // System.out.println(page.getText());

            setPageInPagesStt.execute();

        } catch (SQLException e) {
            System.out.println(e.toString());
        }

    }

    @Override
    public Site getSite(int siteId) {
        try {
            getSiteStatement.setInt(1, siteId);

            ResultSet resultSet = getSiteStatement.executeQuery();

            if (resultSet.next()) {
                return new Site(resultSet.getString("name"),
                        resultSet.getInt("id"),
                        resultSet.getURL("Name"),
                        resultSet.getString("open_tag"),
                        resultSet.getString("Date_Tag")
                );
            } else {
                return null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public ArrayList<Site> getSites() {
        ArrayList<Site> sites = new ArrayList<>();
        try {

            ResultSet resultSet = getSitesStt.executeQuery();

            while (resultSet.next()) {
                sites.add(new Site(
                        resultSet.getString("Name"),
                        resultSet.getInt("id"),
                        resultSet.getURL("Name"),
                        resultSet.getString("open_tag"),
                        resultSet.getString("Date_Tag")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sites;
    }

    public Map<String, Integer> getKeywords() {
        HashMap<String, Integer> keywords = new HashMap();
        try {
            ResultSet rs = getKeywordsStt.executeQuery();
            while (rs.next()) {
                keywords.put(
                        rs.getString("name"),
                        rs.getInt("person_id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return keywords;
    }

    public ArrayList<Page> getPagesByNullLastScanDate(int siteId) {
        ArrayList<Page> pages = new ArrayList<>();
        try {
            getPagesByNullLastScanDateStt.setInt(1, siteId);
            ResultSet rs = getPagesByNullLastScanDateStt.executeQuery();
            while (rs.next()) {
                Page page = new Page();
                page.setPageId(rs.getInt("id"));
                page.setText(rs.getString("text"));
                pages.add(page);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return pages;
    }

    public boolean isExistRankByPersonAndPage(int personId, int pageId) {
        try {
            isExistRankByPersonAndPageStt.setInt(1, personId);
            isExistRankByPersonAndPageStt.setInt(2, pageId);
            ResultSet rs = isExistRankByPersonAndPageStt.executeQuery();
            if (rs.last()) {
                if (rs.getRow() == 1) {
                    return true;
                } else if (rs.getRow() > 1) {
                    System.out.println(String.format("Сочетание личности (%s) и страницы (%s) " +
                            "в таблице рангов должно быть уникальным.", personId, pageId));
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private int changePersonPageRank(Rank rank) {
        try {
            changePersonPageRankStt.setInt(1, rank.getRank());
            changePersonPageRankStt.setInt(2, rank.getPersonId());
            changePersonPageRankStt.setInt(3, rank.getPageId());
            return changePersonPageRankStt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int setPersonPageRank(Rank rank) {
        if (isExistRankByPersonAndPage(rank.getPersonId(), rank.getPageId())) {
            return changePersonPageRank(rank);
        } else {
            try {
                setPersonPageRankStt.setInt(1, rank.getPersonId());
                setPersonPageRankStt.setInt(2, rank.getPageId());
                setPersonPageRankStt.setInt(3, rank.getRank());
                return setPersonPageRankStt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    public int changeLastScanDateInPages(Page page) {
        try {
            changeLastScanDateInPagesStt.setString(1, formatDateForDB(page.getLastScanDate()));
            changeLastScanDateInPagesStt.setInt(2, page.getPageId());
            return changeLastScanDateInPagesStt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public ArrayList<Page> getPagesByMinLastScanDate(int limit, int siteId) {
        ArrayList<Page> pages = new ArrayList<>(limit);
        try {
            getPagesByMinLastScanDateStt.setInt(1, siteId);
            getPagesByMinLastScanDateStt.setInt(2, limit);
            ResultSet resultSet = getPagesByMinLastScanDateStt.executeQuery();
            while (resultSet.next()) {
                Page page = new Page();
                page.setPageId(resultSet.getInt("id"));
                page.setUrl(resultSet.getURL("url"));
                page.setModified(convertDBStringToDate(resultSet.getString("modified")));
                page.setText(resultSet.getString("text"));
                pages.add(page);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return pages;
    }

    public int changePageText(Page page) {
        try {
            changePageTextStt.setString(1, formatDateForDB(page.getModified()));
            changePageTextStt.setString(2, page.getText());
            changePageTextStt.setInt(3, page.getPageId());
            return changePageTextStt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public String getPersonByKey(int personId) {
        try {
            getPersonByKeyStt.setInt(1, personId);
            ResultSet rs = getPersonByKeyStt.executeQuery();
            if (rs.first()) {
                return rs.getString("name");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String formatDateForDB(Date date) {
        return date.toInstant().toString().split("[TZ]")[0] +
                " " + date.toInstant().toString().split("[TZ]")[1];
    }

    private Date convertDBStringToDate(String sqlDate) {
        sqlDate = sqlDate.replace("\\.", "-");
        sqlDate = sqlDate.replace(" ", "T");
        return Date.from(Instant.parse(sqlDate + "Z"));
    }
}
