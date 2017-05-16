package crowler.controller;

import crowler.model.Page;
import crowler.model.Site;

/**
 * Created by vasily on 12.04.17.
 */
public interface DBControllerInterfce {

    /**
     * Метод сохранения страницы в БД. На вход подается объект типа страницы,
     * который нужно сохранить в БД. Если запись по странице уже есть в БД
     * (запись с таким же url), то обновить текст и атрибуты.
     *
     * Таблица pages
     *
     * @param page      страница для добавления в pages
     */
    public void putPage(Page page);

    /**
     * Возвращает из справочника объект site по его id
     *
     * @param siteId
     * @return
     */
    public Site getSite(int siteId);
}
