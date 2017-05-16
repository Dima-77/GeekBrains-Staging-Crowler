package crowler.model;

import java.net.URL;

/**
 * Created by vasily on 12.04.17.
 */
public class Site {

    private String name;
    private int id;
    private URL url;
    private String openTag;
    private String closeTag;

    public Site() {
    }

    public Site(String name, int id, URL url, String openTag, String closeTag) {
        this.name = name;
        this.id = id;
        this.url = url;
        this.openTag = openTag;
        this.closeTag = closeTag;
    }

    public String getOpenTag() {
        return openTag;
    }

    public void setOpenTag(String openTag) {
        this.openTag = openTag;
    }

    public String getCloseTag() {
        return closeTag;
    }

    public void setCloseTag(String closeTag) {
        this.closeTag = closeTag;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
