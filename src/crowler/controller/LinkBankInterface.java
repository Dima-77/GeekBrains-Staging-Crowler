package crowler.controller;

import java.net.URL;
import java.util.Collection;

/**
 * Created by vasily on 12.04.17.
 */
public interface LinkBankInterface {

    /**
     * Добавляет в конец коллекции уникальные ссылки
     * В случае, если ссылка уже присутсвует в БД,
     * то такая ссылка не добавляется
     *
     * @param links     коллекция из строк-ссылок
     */
    public void addLinks(Collection<URL> links);

    /**
     * То же самое, но на вход принимает объект URL
     * вместо коллекции
     *
     * @param link      URL-ссылка
     */
    public void addLinks(URL link);

    /**
     * Возвращает следующую ссылку из коллекции. Если коллекция
     * закончилась, то возвращает null
     *
     * @return      строка-ссылка
     */
    public URL getNextLink();
}
