package crowler.model;

import java.net.URL;
import java.util.Date;

/**
 * Created by vasily on 12.04.17.
 */
public class Page {

    private int pageId;
    private URL url;
    private Site site;
    private Date modified;
    private Date lastScanDate;
    private String text;

    public Page() {
    }

    public Page(URL url, Date modified, String text, Site site) {
        this.url = url;
        this.modified = modified;
        this.text = text;
        this.site = site;
    }

    public int getPageId() {
        return pageId;
    }

    public void setPageId(int pageId) {
        this.pageId = pageId;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public Site getSite() {
        return site;
    }

    public void setSite(Site site) {
        this.site = site;
    }

    public Date getModified() {
        return modified;
    }

    public void setModified(Date modified) {
        this.modified = modified;
    }

    public Date getLastScanDate() {
        return lastScanDate;
    }

    public void setLastScanDate(Date lastScanDate) {
        this.lastScanDate = lastScanDate;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
