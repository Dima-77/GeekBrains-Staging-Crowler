package test;

import crowler.controller.PageScanner;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by Dima on 27.04.2017.
 */
public class dateReformatTest {

    static PageScanner pg;

    @Test
    public void reformatTest1() {
        String string = "15 Ноября 2006";
        String expected = "2006.11.15T00:00:00Z";
        String actual = pg.reformatDate(string);
        Assert.assertEquals(expected, actual);
    }
}
