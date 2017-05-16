package crowler.controller;

import java.net.URL;
import java.util.*;

/**
 * Created by vasily on 17.04.17.
 */
public class LinkBank implements LinkBankInterface {

    private static LinkBank ourInstance = new LinkBank();
    private ArrayList<URL> bank = new ArrayList<>();
    private int iterator = 0;

    public static LinkBank getInstance() {
        return ourInstance;
    }

    private LinkBank() {
    }

    @Override
    public void addLinks(Collection<URL> links) {
        for (URL url : links) {
            if (!bank.contains(url)) {
                bank.add(url);
                // System.out.println(url);
            }
        }
    }

    @Override
    public URL getNextLink() {

//        for (URL u : bank) {
//            System.out.println(u);
//        }

        if (iterator < bank.size()) {
            URL url = bank.get(iterator);
            iterator ++;
            return url;
        } else {
            return null;
        }
    }

    @Override
    public void addLinks(URL url) {
        if (!bank.contains(url)) {
            bank.add(url);
        }
    }
}
