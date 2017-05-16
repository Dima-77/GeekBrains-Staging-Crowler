package crowler;

import crowler.controller.PageScanner;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by vasily on 17.04.17.
 * Added command line parsing by Dima-77 on 26.04.17
 */
public class MainCrawlerClass {
    private static Logger logger = Logger.getLogger(MainCrawlerClass.class.getName());

    public static void main(String[] args) {
        int siteID;
        if (args.length == 0) {
            //Если нет параметров - работаем со всеми сайтами
            PageScanner ps = new PageScanner(0);
        } else {
            try {
                siteID = Integer.parseInt(args[0]);
                PageScanner ps = new PageScanner(siteID);
            } catch (NumberFormatException e) {
                logger.log(Level.WARNING, "Аргумент командной строки должен быть числом или отсутствовать.");
            }
        }
    }
}
