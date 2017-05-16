package crowler.controller;

import java.net.URL;
import java.util.Collection;

/**
 * Created by vasily on 12.04.17.
 */
public interface LinkExtractorInterface {

    /**
     * Метод получает на вход страницу и выбирает из неё все ссылки на другие страницы
     * сайта, возвращает в виде коллекции
     *
     * @param htmlText      Текст страницы между тегами <body></body>
     * @return              Коллекция строк-ссылко на данной странице
     */
    public void sendPage(String htmlText, URL baseURL);
}
