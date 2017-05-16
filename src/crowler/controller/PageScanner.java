package crowler.controller;

import crowler.model.Page;
import crowler.model.Rank;
import crowler.model.Site;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Maxim on 12.04.2017.
 */
public class PageScanner {

    private static final String USER_AGENT = "GeekBrainsCrowler/0.1 Java/Jsoup (Educational project. Contact e-mail: piatachki@gmail.com, https://github.com/piatachki/GB/tree/vasilys_vers)";
    private static final String REFERRER = "";

    //Отвечает за количество полученных страниц до отключения программы
    //private static final int NUMBER_OF_URLS_TO_DOWN = 30000;

    //Количество полученных страниц (не статей) = MAIN_LOOP_CYCLES * NUMBER_OF_URLS_TO_END_CRAWL;
    private static final int MAIN_LOOP_CYCLES = 6;
    private static final int NUMBER_OF_URLS_TO_END_CRAWL = 3000;
    private static final int ARTICLES_CHECK_BEFORE_END = 200;
    private static final String IN_BASE_TAG_DELIMITER = ";+";

    private ArrayList<Site> sites = null;
    private URL currentLink;
    private Page currentPage = new Page();
    private Document html;

    private LinkExtractor le = LinkExtractor.getInstance();
    private LinkBank lb = LinkBank.getInstance();

    private static Logger logger = Logger.getLogger(PageScanner.class.getName());

    public PageScanner(int siteID) {

        //Подготовка к работе
        prepare(siteID);

        //Основной цикл
        int loops = MAIN_LOOP_CYCLES;
        while (loops > 0) {

            loops--;

            crawlPages();

            getStatistic();

            checkModify();
        }
    }

    private void prepare(int siteID) {

        // Получаем из справочника сайты
        sites = new ArrayList();
        if (siteID > 0) {
            Site site = DBController.getInstance().getSite(siteID);
            if (site != null) {
                sites.add(DBController.getInstance().getSite(siteID));
            } else {
                logger.log(Level.WARNING, "В базе нет сайта с таким ID: " + siteID);
                System.exit(0);
            }
        } else if (siteID == 0) {
            sites = DBController.getInstance().getSites();
            if (sites.size() == 0) {
                logger.log(Level.WARNING, "В базе нет ни одного сайта");
                System.exit(0);
            }
        }
        /**
         * Добавляем первые ссылки в банк, чтобы
         * было с чего начнать парсить
         */
        for (Site site : sites) {
            lb.addLinks(site.getUrl());
        }
    }

    public void crawlPages() {

        // Начинаем в цикле работать по известным ссылкам
        // Выходим из цикла для перехода к следующей процедуре
        // после некоторого числа обработанных урлов
        int urlFuncCountdown = NUMBER_OF_URLS_TO_END_CRAWL;
        while (urlFuncCountdown > 0) {

            urlFuncCountdown--;
            // Получаем следующую непройденную ссылку
            currentLink = lb.getNextLink();
            //Для разовой загрузки
            /*try {
                urlFuncCountdown = 0;
                currentLink = new URL("https://lenta.ru/news/2017/05/02/peskovv/");

            } catch (MalformedURLException e) {
                e.printStackTrace();
            }*/
            logger.log(Level.INFO, "Getting nextLink(): " + currentLink);

            // Если банк вернул null, значит ссылки кончились
            // Выходим из цикла
            if (currentLink == null) {
                logger.log(Level.INFO, "Linkbank return NULL. Breaking");
                break;
            }

            //Получаем текущий сайт из URL
            Site site = null;
            for (Site s : sites) {
                if (s.getName().equals("http://" + currentLink.getHost())) {
                    site = s;
                    break;
                }
            }

            // Идём дальше. Получаем документ по url, отслеживаем
            // возможный ексепшн в логгере
            try {
                html = Jsoup.connect(currentLink.toString()).userAgent(USER_AGENT).referrer(REFERRER).get();
                logger.log(Level.FINE, "Get page " + currentLink);
                // Дальше всё происходит, только есть парсер вернул как-то текст.
                // Если объект отсутсвует (например, просканили rss без текста),
                // то пропускаем сохранение и прочие действия

                if (html.body() != null) {
                    // Отправляем html-текст страницы межуд тегами <body>
                    // в линкэкстрактор, что бы он выдернул ссылки из тела
                    // страницы и мы могли двигаться дальше
                    logger.log(Level.INFO, "Sending page HTML-text to linkExtractor");
                    le.sendPage(html.body().html(), site.getUrl());
                    logger.log(Level.INFO, "...ok");
                    // System.out.println(html.body().html());

                    // Заполняем поля у объекта currentPage и готовим
                    // его к отправке в БД

                    // Заполняем URL страницы
                    currentPage.setUrl(currentLink);

                    // Добавляем ссылку на текущий сайт
                    currentPage.setSite(site);

                    // Получаем от сервера дату последнего изменения

                    String[] dateTags = site.getCloseTag().split(IN_BASE_TAG_DELIMITER);
                    Instant modified;
                    //Не используем здесь Instant.MIN, или получим ошибку портирования в Date
                    Instant maxDate = Instant.parse("-10000-01-01T00:00:00Z");
                    for (int i = 0; i < dateTags.length; i++) {
                        if (html.select(dateTags[i]).hasText()) {
                            String dateText = html.select(dateTags[i]).text();
                            System.out.println("Date actual: " + dateText);
                            String[] dateTextTerms = dateText.toLowerCase().split("обнов(л([её](н([ои]е?)?)?)?)?");
                            for (int j = 0; j < dateTextTerms.length; j++) {
                                modified = Instant.parse(reformatDate(dateTextTerms[j]));
                                System.out.println("Date reformatted: " + reformatDate(dateTextTerms[j]));
                                if (modified.isAfter(maxDate)) {
                                    maxDate = Instant.ofEpochMilli(modified.toEpochMilli());
                                }
                            }
                        }
                    }
                    currentPage.setModified(Date.from(maxDate));

                    // Готовим чистый текст статьи
                    StringBuilder article = new StringBuilder();
                    String[] openTags = site.getOpenTag().split(IN_BASE_TAG_DELIMITER);
                    if (!html.select(openTags[openTags.length - 1]).hasText()) {     //Нет тела статьи
                        logger.log(Level.INFO, "Page isn't article page!");
                        continue;
                    }
                    for (int i = 0; i < openTags.length; i++) {
                        article.append(html.select(openTags[i].trim()).text());
                        article.append("\n");
                    }
                    currentPage.setText(article.toString().trim());

                    // В БД кладём страницу. DBController сам определяет по url,
                    // есть такая страница в БД или нет. Если страницы нет,
                    // то она добавляется, если страница есть, то нет и получим
                    // некритичную SQLException
                    logger.log(Level.INFO, "Putting " + currentLink + " to DB...");
                    DBController.getInstance().putPage(currentPage);
                    logger.log(Level.INFO, "...ok");

                } else { // Иначе получаем другую ссылки и работаем дальше
                    logger.log(Level.INFO, "Can't get HTML-body from " + currentLink);
                }

            } catch (Exception e) {
                logger.log(Level.WARNING, "Can't get page " + currentLink, e);
            }

            // Повторяем в цикле
        }
    }

    public void getStatistic() {

        ArrayList<Page> pages = new ArrayList<>();
        //Получаем статьи только для сайтов с которыми работаем
        for (Site site : sites) {
            pages.addAll(DBController.getInstance().getPagesByNullLastScanDate(site.getId()));
        }

        HashMap<String, Integer> keywords = (HashMap<String, Integer>) DBController.getInstance().getKeywords();
        ArrayList<Rank> ranks = new ArrayList<>();

        for (Page page : pages) {
            //Получаем часть списка рангов, относящуюся к странице
            ranks.addAll(countRanksOnPage(page.getPageId(), page.getText(), keywords));
            //После подсчёта устанавливаем дату сканирования
            page.setLastScanDate(new Date());
            DBController.getInstance().changeLastScanDateInPages(page);
        }

        for (Rank rank : ranks) {
            DBController.getInstance().setPersonPageRank(rank);
        }
    }

    public void checkModify() {

        ArrayList<Page> pages = new ArrayList<>(ARTICLES_CHECK_BEFORE_END);
        int limitOnSite = ARTICLES_CHECK_BEFORE_END / sites.size() == 0 ? 1 : ARTICLES_CHECK_BEFORE_END / sites.size();

        for (Site site : sites) {
            pages.addAll(DBController.getInstance().getPagesByMinLastScanDate(limitOnSite, site.getId()));
        }

        // Дальнейшая работа похожа на сокращённый метод crawlPages()
        for (Page page : pages) {

            currentLink = page.getUrl();
            logger.log(Level.INFO, "Getting nextLink(): " + currentLink);

            // Получаем текущий сайт из URL
            Site site = null;
            for (Site s : sites) {
                if (s.getName().equals("http://" + currentLink.getHost())) {
                    site = s;
                    break;
                }
            }

            try {
                html = Jsoup.connect(currentLink.toString()).userAgent(USER_AGENT).referrer(REFERRER).get();
                logger.log(Level.FINE, "Get page " + currentLink);

                if (html.body() != null) {

                    // Получаем от сервера дату последнего изменения
                    String[] dateTags = site.getCloseTag().split(IN_BASE_TAG_DELIMITER);
                    Instant modified;
                    // Не используем здесь Instant.MIN, или получим ошибку портирования в Date
                    Instant maxDate = Instant.parse("-10000-01-01T00:00:00Z");
                    for (int i = 0; i < dateTags.length; i++) {
                        if (html.select(dateTags[i]).hasText()) {
                            String dateText = html.select(dateTags[i]).text();
                            System.out.println("Date actual: " + dateText);
                            String[] dateTextTerms = dateText.toLowerCase().split("обнов(л([её](н([ои]е?)?)?)?)?");
                            for (int j = 0; j < dateTextTerms.length; j++) {
                                modified = Instant.parse(reformatDate(dateTextTerms[j]));
                                System.out.println("Date reformatted: " + reformatDate(dateTextTerms[j]));
                                if (modified.isAfter(maxDate)) {
                                    maxDate = Instant.ofEpochMilli(modified.toEpochMilli());
                                }
                            }
                        }
                    }

                    // Если на сервере более поздняя дата статьи, то
                    // отправляем в БД новый текст и последнюю дату
                    if (maxDate.isAfter((page.getModified()).toInstant())) {

                        page.setModified(Date.from(maxDate));

                        // Готовим чистый текст статьи
                        StringBuilder article = new StringBuilder();
                        String[] openTags = site.getOpenTag().split(IN_BASE_TAG_DELIMITER);
                        if (!html.select(openTags[openTags.length - 1]).hasText()) {     //Нет тела статьи
                            logger.log(Level.INFO, "Page isn't article page!");
                            continue;
                        }
                        for (int i = 0; i < openTags.length; i++) {
                            article.append(html.select(openTags[i].trim()).text());
                            article.append("\n");
                        }
                        page.setText(article.toString().trim());

                        //Отправляем новый текст в БД. Last_Scan_Date устанавливаем в null,
                        //чтобы в следующей итерации была пересмотрена статистика
                        logger.log(Level.INFO, "Putting new text from " + currentLink + " to DB...");
                        DBController.getInstance().changePageText(page);
                        logger.log(Level.INFO, "...ok");
                    } else {    //Дата статьи с сервера совпадает с базовой
                        //Обновляем LastScanDate. В следующей итерации будут проверены статьи
                        //с более ранней LastScanDate
                        logger.log(Level.INFO, "Putting last scan date " + currentLink + " to DB...");
                        page.setLastScanDate(new Date());
                        DBController.getInstance().changeLastScanDateInPages(page);
                    }
                } else { // Иначе получаем другую ссылку и работаем дальше
                    logger.log(Level.INFO, "Can't get HTML-body from " + currentLink);
                }

            } catch (Exception e) {
                logger.log(Level.WARNING, "Can't get page " + currentLink, e);
            }
        }
    }

    //Возвращает дату в формате 'ГГГГ-ММ-ДДTЧЧ:ММ:ССZ' для класса Instant
    public String reformatDate(String dateString) {

        String fullDate;
        String time;
        String date;

        Pattern time24pat = Pattern.compile("([0-2]?\\d)(:[0-6]\\d){1,2}");

        //Проверка наличия паттерна времени (сравнение от менее обобщённых паттернов к более)
        Matcher m24 = time24pat.matcher(dateString);
        String[] timeSeq = null;
        if (m24.find()) {
            time = m24.group().trim();
            timeSeq = time.split(":");
        } else {
            time = "00:00:00";
            timeSeq = time.split(":");
        }
        //Форматируем время
        for (int i = 0; i < timeSeq.length; i++) {
            timeSeq[i] = String.format("%02d", Integer.parseInt(timeSeq[i]));
        }
        //Объединяем в строку
        time = "";
        for (int idx = 0; idx < timeSeq.length - 1; idx++) {
            time = time + timeSeq[idx] + ":";
        }
        time = time + timeSeq[timeSeq.length - 1];
        if (time.length() == 5) time = time + ":00";

        //Работа с датой
        Pattern dateWordDMY = Pattern.compile("([0-2]?\\d|3[0-1])\\s+([а-яА-Я]|\\w){3,9}\\s+(\\d{4}|\\d{2})",
                Pattern.CASE_INSENSITIVE);
        Pattern dateWordDM = Pattern.compile("([0-2]?\\d|3[0-1])\\s+([а-яА-Я]|\\w){3,9}", Pattern.CASE_INSENSITIVE);
        Pattern dateNumberDMY = Pattern.compile("([0-2]?\\d|3[0-1])(/|\\\\|\\.|-)" +
                "((0?[1-9])|(1[0-2]))(/|\\\\|\\.|-)(\\d{4}|\\d{2})");

        Matcher mWDMY = dateWordDMY.matcher(dateString);
        Matcher mWDM = dateWordDM.matcher(dateString);
        Matcher mNDMY = dateNumberDMY.matcher(dateString);

        //Проверка наличия паттерна даты
        String[] dateSeq;
        if (mWDMY.find()) {
            date = mWDMY.group().trim();
            dateSeq = date.split("\\s+");
            dateSeq[1] = getMonthNumByName(dateSeq[1]);
            String t = dateSeq[0];
            dateSeq[0] = dateSeq[2];
            dateSeq[2] = t;
        } else if (mWDM.find()) {
            date = mWDM.group().trim() + " 0";
            System.out.println("Reform " + date);
            dateSeq = date.split("\\s+");
            dateSeq[1] = getMonthNumByName(dateSeq[1]);
            dateSeq[2] = dateSeq[0];
            dateSeq[0] = String.valueOf(LocalDateTime.now().getYear());
        } else if (mNDMY.find()) {
            date = mNDMY.group().trim();
            System.out.println("Reform " + date);
            dateSeq = date.split("(/|\\\\|\\.|-)");
            String t = dateSeq[0];
            dateSeq[0] = dateSeq[2];
            dateSeq[2] = t;
        } else {
            dateSeq = new String[3];
            //Если не найдена дата, но есть время
            if (!time.equals("00:00:00") && dateString.toLowerCase().contains("вчера")) {
                dateSeq[0] = String.valueOf(LocalDateTime.now().minusDays(1L).getYear());
                dateSeq[1] = String.format("%02d", LocalDateTime.now().minusDays(1L).getMonth().getValue());
                dateSeq[2] = String.format("%02d", LocalDateTime.now().minusDays(1L).getDayOfMonth());
            } else if (!time.equals("00:00:00")) {
                dateSeq[0] = String.valueOf(LocalDateTime.now().getYear());
                dateSeq[1] = String.format("%02d", LocalDateTime.now().getMonth().getValue());
                dateSeq[2] = String.format("%02d", LocalDateTime.now().getDayOfMonth());
            } else {
                dateSeq[0] = "0000";
                dateSeq[1] = dateSeq[2] = "00";
            }
        }
        //Форматируем дату
        if (dateSeq[0].length() == 2) {
            if (Integer.parseInt(dateSeq[0]) >= 66) {
                dateSeq[0] = String.valueOf((Integer.parseInt(dateSeq[0]) + 1900));
            } else {
                dateSeq[0] = String.valueOf((Integer.parseInt(dateSeq[0]) + 2000));
            }
        }
        dateSeq[1] = String.format("%02d", Integer.parseInt(dateSeq[1]));
        dateSeq[2] = String.format("%02d", Integer.parseInt(dateSeq[2]));
        date = "";
        for (int i = 0; i < dateSeq.length - 1; i++) {
            date = date + dateSeq[i] + "-";
        }
        date = date + dateSeq[dateSeq.length - 1];
        fullDate = date + "T" + time + "Z";
        return fullDate;
    }

    private ArrayList<Rank> countRanksOnPage(int pageID, String text, HashMap<String, Integer> keywords) {
        ArrayList<Rank> ranks = new ArrayList<>();
        //Хранит <person id, rank> для переданной страницы
        HashMap<Integer, Integer> ranksMap = new HashMap<>();

        //Ищем каждое ключевое слово в тексте
        for (Map.Entry<String, Integer> keyword : keywords.entrySet()) {
            System.out.println(keyword.getKey());
            Integer prevLength = text.length();
            //Текст уменьшается, это поможет избежать повторного счёта, если
            //одно ключевое слово является подстрокой другого (при неудачном подборе)
            text = text.replace(keyword.getKey(), "");
            Integer newLength = text.length();
            Integer matches = (prevLength - newLength) / keyword.getKey().length();
            if (matches > 0) {
                if (ranksMap.containsKey(keyword.getValue())) {
                    ranksMap.replace(keyword.getValue(), ranksMap.get(keyword.getValue()) + matches);
                } else {
                    ranksMap.put(keyword.getValue(), matches);
                }
            }
        }

        //Добавляем полученные в этой статье ранги к списку рангов
        for (Map.Entry<Integer, Integer> rm : ranksMap.entrySet()) {
            Rank rank = new Rank();
            rank.setPageId(pageID);
            rank.setPersonId(rm.getKey());
            rank.setRank(rm.getValue());
            ranks.add(rank);
        }

        return ranks;
    }

    private String getMonthNumByName(String monthName) {
        String[][] months = {
                {"янв", "фев", "мар", "апр", "мая", "июн", "июл", "авг", "сен", "окт", "ноя", "дек"},
                {"янв", "фев", "мар", "апр", "май", "июн", "июл", "авг", "сен", "окт", "ноя", "дек"}
        };
        for (int i = 0; i < months[0].length; i++) {
            if (monthName.toLowerCase().startsWith(months[0][i]) ||
                    monthName.startsWith(months[1][i])) {
                return String.format("%02d", i + 1);
            }
        }
        return null;
    }
}
