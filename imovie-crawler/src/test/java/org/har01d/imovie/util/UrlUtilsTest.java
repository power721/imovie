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

    @Test
    public void testFindED2K() throws Exception {
        assertEquals(0, UrlUtils.findED2K("感谢楼主分享！！！").size());
        assertEquals(0, UrlUtils.findED2K("magnet:?xt=urn:btih:FF259A586E61FA2233277DECB5FA508293CDB626").size());
        assertEquals(0,
            UrlUtils.findED2K("ed2k://|file|苦涩的收割.BD高清1280中英双字.mp4|2325980725|h=GQQRJ57X5QLPH5BGK2GEIWRZTRQRQKWH|/")
                .size());

        String ed2k = "ed2k://|file|苦涩的收割.BD高清1280中英双字.mp4|2325980725|0F33BE098F32CF28210F57DEA3C29C25|h=GQQRJ57X5QLPH5BGK2GEIWRZTRQRQKWH|/";
        assertEquals(ed2k, UrlUtils.findED2K(ed2k).get(0));

        ed2k = "ed2k://|file|苦涩的收割.BD高清1280中英双字.mp4|2325980725|0f33be098f32cf28210f57dea3c29c25|h=gqqrj57x5qlph5bgk2geiwrztrqrqkwh|/";
        assertEquals(ed2k, UrlUtils.findED2K(ed2k).get(0));
    }

    @Test
    public void testFindMagnet() throws Exception {
        assertEquals(0, UrlUtils.findMagnet("感谢楼主分享！！！").size());
        assertEquals(0, UrlUtils.findMagnet("magnet:?xt=urn:btih:&dn=123").size());

        String magnet = "magnet:?xt=urn:btih:FF259A586E61FA2233277DECB5FA508293CDB626";
        assertEquals(magnet, UrlUtils.findMagnet(magnet).get(0));

        magnet = "magnet:?xt=urn:btih:FF259A586E61FA2233277DECB5FA508293CDB626&dn=123";
        assertEquals(magnet, UrlUtils.findMagnet(magnet).get(0));

        magnet = "magnet:?xt=urn:btih:bfda20691ff6f5ad4eefcbc0b23476f286aa3080&xl=612837521";
        assertEquals(magnet, UrlUtils.findMagnet(magnet).get(0));

        magnet = "magnet:?xt=urn:btih:cb19d34ebd82c581255b3b651a69c1af68103bdb&tr=http://bt.mp4ba.com:2710/announce";
        assertEquals(magnet, UrlUtils.findMagnet(magnet).get(0));

        magnet = "magnet:?xt=urn:btih:20ac0438c0dcfab037a1f5461b898db8ec47d8ed&dn=There%5C%27s+Something+About+Mary%281998%29DVDRip.AC3%28ENG%29-DROCK&tr=udp%3A%2F%2Ftracker.openbittorrent.com%3A80&tr=udp%3A%2F%2Ftracker.publicbt.com%3A80&tr=udp%3A%2F%2Ftracker.istole.it%3A6969&tr=udp%3A%2F%2Ftracker.ccc.de%3A80";
        assertEquals(magnet, UrlUtils.findMagnet(magnet).get(0));

        magnet = "magnet:?xt=urn:btih:4D9FA761D69964B00DF0B3B0C9C1F968EA6C47D0&xt=urn:ed2k:7655dbacff9395e579c4c9cb49cbec0e&dn=bbb_sunflower_2160p_30fps_stereo_abl.mp4&tr=udp%3a%2f%2ftracker.openbittorrent.com%3a80%2fannounce&tr=udp%3a%2f%2ftracker.publicbt.com%3a80%2fannounce&ws=http%3a%2f%2fdistribution.bbb3d.renderfarming.net%2fvideo%2fmp4%2fbbb_sunflower_2160p_30fps_stereo_abl.mp4";
        assertEquals(magnet, UrlUtils.findMagnet(magnet).get(0));
    }
}