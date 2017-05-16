package crowler.controller;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by vasily on 17.04.17.
 */
public class LinkExtractor implements LinkExtractorInterface {
    private static LinkExtractor ourInstance = new LinkExtractor();
    private LinkBank lb = LinkBank.getInstance();
    //Кэш для запрещённых роботам путей
    private HashMap<String, Set<Pattern>> disallowListCache = new HashMap();

    public static LinkExtractor getInstance() {
        return ourInstance;
    }

    private LinkExtractor() {
    }


    @Override
    public void sendPage(String htmlText, URL baseURL) {

        // Готовим переменные для извлечения ссылок
        Document page = Jsoup.parse(htmlText);
        Elements link = page.select("a[href]");
        Collection<URL> tmp = new Vector<>();

        // Прогбегаемся по всей коллекции найденных ссылок
        try {
            for (Element e : link) {

                // Если ссылка относительная (начинается с "/"),
                // то она гарантированно внутренняя. Добавляем к
                // ней домен префиксом и отправляем во временный
                // контейнер
                if (e.attr("href").startsWith("/")) {
                    tmp.add(new URL(baseURL.toString() + e.attr("href")));
                    // System.out.println(e.attr("href"));
                }

                // Если ссылка абсолютная и начинается с имени
                // домена, то так же отправляем её во временный
                // контейнер
                if (e.attr("href").startsWith(baseURL.toString())) {
                    tmp.add(new URL(e.attr("href")));
                    // System.out.println(e.attr("href"));
                }

                // Остальные ссылки игнорируются
            }
        } catch (MalformedURLException e) {
            System.out.println("Какая-то дичь случилась");
            e.printStackTrace();
        }

        // Ещё небольшая проверка, что ссылка не является якорем
        // или ретёрном на другую страницу. В будущем список уловий можно
        // уточнять. В противном случае возникнут дубликаты ссылок.
        // Якоря в ссылках детерминируются решеткой "#". Такие ссылки
        // убираем

        // TODO Вынести список стоп-слов в БД в след. версии!

        // TODO Возможно эти проверки следует делать после проверки на посещённость
        // во избежание лишних сравнений. Для этого набор посещённых ссылок надо перенести сюда

        tmp.removeIf(new Predicate<URL>() {
            @Override
            public boolean test(URL url) {
                if (url.toString().contains("#") ||
                        url.toString().contains("destination") ||
                        url.toString().contains("tag") ||
                        url.toString().contains("sort") ||
                        !isRobotAllowed(url, "http://")) {
                    return true;
                }
                return false;
            }
        });

        // После формирования и фильтрации отправляем временный
        // сонтейнер ссылок в линкбанк. Там он сложится как надо
        lb.addLinks(tmp);
    }

    // TODO Остаётся вопрос как быть с запретами которые содержат паттерны
    // к примеру: *-print.html$, /sys_*, /ria_70*
    private boolean isRobotAllowed(URL urlToCheck, String protocol) {
        String host = urlToCheck.getHost().toLowerCase();

        Set<Pattern> disallowSet;
        // Получаем список запретов для текущего сайта
        if (disallowListCache.containsKey(host)) {
            disallowSet = disallowListCache.get(host);
        } else {
            // Если списка запретов нет - кэшируем его
            disallowSet = new HashSet();
            Pattern starPattern = Pattern.compile("\\*");

            try {
                URL robotsFileUrl = new URL(protocol + host + "/robots.txt");

                // Открываем соединение с файлом робота
                BufferedReader reader = new BufferedReader(new InputStreamReader(robotsFileUrl.openStream()));

                // Читаем файл робота, создаём список запрещённых путей
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.indexOf("Disallow:") == 0) {
                        String disallowPath = line.substring("Disallow:".length());

                        // Проверяем путь на наличие коммента и удаляем его
                        int commentIndex = disallowPath.indexOf("#");
                        if (commentIndex != -1) {
                            disallowPath = disallowPath.substring(0, commentIndex);
                        }

                        // Удаляем ведущие и завершающие пробелы из запрещённого пути
                        disallowPath = disallowPath.trim();
                        // Заменяем звёздочки из пути на Java regex \S*
                        Matcher starMatcher = starPattern.matcher(disallowPath);
                        disallowPath = starMatcher.replaceAll("\\\\S*");

                        // Добавляем запрещённый путь к списку как паттерн
                        disallowSet.add(Pattern.compile(disallowPath));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                // Положим робот всё разрешает так как выскочит
                // исключение, если файла робота нет
                return true;
            }

            // А всё из-за ТАСС и РИА которым нужен конкретный протокол для робота (http и https соответственно)
            // При открытии соединения чтец не выбрасывает исключения, а создаёт поток с несколькими пустыми строками
            if (disallowSet.isEmpty() && protocol.equals("http://")) {
                return isRobotAllowed(urlToCheck, "https://");
            }

            // Добавляем новый список запретов к кэшу
            disallowListCache.put(host, disallowSet);
        }

        //Пробегаем список запретов для проверки данного URL
        String file = urlToCheck.getFile();
        Iterator<Pattern> it = disallowSet.iterator();
        while (it.hasNext()) {
            Pattern disallow = it.next();
            Matcher m = disallow.matcher(file);
            if (m.find() && m.start() == 0) {
                System.out.println("Robot disallow " + urlToCheck.toString() + ". Link contain " + disallow.toString());
                return false;
            }
        }
        return true;
    }

}
