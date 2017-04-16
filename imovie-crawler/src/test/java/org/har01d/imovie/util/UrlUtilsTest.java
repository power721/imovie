package org.har01d.imovie.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class UrlUtilsTest {

    @Test
    public void test() throws Exception {
        String encodedUrl = "thunder://QUFmdHA6Ly9keTpkeUB4bGEuMnR1LmNjOjIwNTI2L1vRuMDXz8LU2Hd3dy4ydHUuY2Nd1MS687y0t9kuQkTW0NOiy6vX1jEyODC438flLnJtdmJaWg==";
        System.out.println(UrlUtils.convertUrl(encodedUrl));
    }

    @Test
    public void convertThunder() throws Exception {
        String encodedUrl = "thunder://QUFodHRwOi8vdG9vbC5sdS90ZXN0LnppcFpa";
        assertEquals("http://tool.lu/test.zip", UrlUtils.convertUrl(encodedUrl));
    }

    @Test
    public void convertQQ() throws Exception {
        String encodedUrl = "qqdl://aHR0cDovL3Rvb2wubHUvdGVzdC56aXA=";
        assertEquals("http://tool.lu/test.zip", UrlUtils.convertUrl(encodedUrl));
    }

    @Test
    public void convertFlashGet() throws Exception {
        String encodedUrl = "flashget://W0ZMQVNIR0VUXWh0dHA6Ly90b29sLmx1L3Rlc3QuemlwW0ZMQVNIR0VUXQ==";
        assertEquals("http://tool.lu/test.zip", UrlUtils.convertUrl(encodedUrl));
    }

    @Test
    public void testInvalid() throws Exception {
        String encodedUrl = "thunder://QUFodHRwOi8vdG9vbC5sdS90ZXN0LnppcFpa&srcid=119&verno=1";
        assertEquals("http://tool.lu/test.zip", UrlUtils.convertUrl(encodedUrl));
    }

    @Test
    public void testEndWithSlash() throws Exception {
        String encodedUrl = "thunder://QUFodHRwOi8vdG9vbC5sdS90ZXN0LnppcFpa/";
        assertEquals("http://tool.lu/test.zip", UrlUtils.convertUrl(encodedUrl));
    }

}