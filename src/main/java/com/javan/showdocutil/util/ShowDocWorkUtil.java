package com.javan.showdocutil.util;

import org.springframework.http.*;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * @Desc 远程调用
 * @Author Javan Feng
 * @Date 2019 06 2019/6/29 19:59
 */
public class ShowDocWorkUtil {

    private static final Logger logger = Logger.getLogger("ShowDocWorkUtil");

    public static String API_HTTP_PREFIX = "http://";

    private static final String REMOTE_ADDRESS_PREFIX = "http://";

    private static final String REMOTE_ADDRESS_SUFFIX_1 = "/server/index.php?s=/api/item/updateByApi";

    private static final String REMOTE_ADDRESS_SUFFIX_2 = "/server/api/item/updateByApi";

    private static final String OFFICAL_DOMAIN = "www.showdoc.cc";
    private static final String OFFICAL_DOMAIN2 = "www.showdoc.com.cn";

    private static final RestTemplate TEMPLATE = new RestTemplate();
    /**
     * 控制台打印
     */
    private boolean consolePrint = false;
    /**
     * 更新远程showdoc
     */
    private boolean updateRemote = false;

    private String addressSuffix;

    /**
     * 更新远程, catalog : 目录1/目录2/
     */
    private String remoteDodmin, apiKey, apiToken, catalog;

    private ShowDocWorkUtil() {
    }


    public static ShowDocWorkUtil getInstance() {
        return new ShowDocWorkUtil();
    }

    public ShowDocWorkUtil withConsolePrint() {
        consolePrint = true;
        return this;
    }

    /**
     * 目录
     *
     * @param catalog 格式： 目录1/目录2
     * @return
     */
    public ShowDocWorkUtil withInCatalog(String catalog) {
        this.catalog = catalog;
        return this;
    }

    public ShowDocWorkUtil withApiHttpPrefix(String apiHttpPrefix) {
        API_HTTP_PREFIX = apiHttpPrefix;
        return this;
    }

    public ShowDocWorkUtil withUpdateShowDoc(String domain, String apiKey, String apiToken) {
        Assert.hasText(domain, "domian must not be null");
        Assert.hasText(apiKey, "apiKey must not be null");
        Assert.hasText(apiToken, "apiToken must not be null");
        updateRemote = true;
        setDomain(domain);
        this.apiKey = apiKey;
        this.apiToken = apiToken;
        return this;
    }

    private void setDomain(String domain) {
        if (OFFICAL_DOMAIN.equals(domain) || OFFICAL_DOMAIN2.equals(domain)) {
            addressSuffix = REMOTE_ADDRESS_SUFFIX_2;
        } else {
            addressSuffix = REMOTE_ADDRESS_SUFFIX_1;
        }
        this.remoteDodmin = domain;
    }

    /**
     * 基础包，扫描下面所有的controller层
     **/
    public void doWork(String basePackage) throws Exception {
        List<ShowDocModel> text = ControllerShowDocUtils.getText(basePackage);
        if (consolePrint) {
            doConsolePrint(text);
        }

        if (updateRemote) {
            doUpdateRemote(text);
        }
    }


    /**
     * 扫描controller类
     **/
    public void doWork(Class<?> controllerClass) throws Exception {
        List<ShowDocModel> text = ControllerShowDocUtils.getText(controllerClass);
        if (consolePrint) {
            doConsolePrint(text);
        }

        if (updateRemote) {
            doUpdateRemote(text);
        }
    }

    /**
     * 扫描controller类中的public 类
     **/
    public void doWork(Class<?> controllerClass, String simpleMethodName) throws Exception {
        ShowDocModel text = ControllerShowDocUtils.getText(controllerClass, simpleMethodName);
        if (consolePrint) {
            doConsolePrint(Collections.singletonList(text));
        }

        if (updateRemote) {
            doUpdateRemote(Collections.singletonList(text));
        }
    }


    private void doUpdateRemote(List<ShowDocModel> text) throws IOException {
        String urlStr;
        if (remoteDodmin != null && remoteDodmin.contains("http")) {
            urlStr = remoteDodmin + addressSuffix;
        } else {
            urlStr = REMOTE_ADDRESS_PREFIX + remoteDodmin + addressSuffix;
        }
        String catalog = this.getCorrectCatalog();
        for (ShowDocModel showDocModel : text) {
            String folder = catalog + showDocModel.getFolder();
            String title = showDocModel.getTitle();
            String content = showDocModel.getContent();
            connect(urlStr, folder, title, content);
        }
    }

    private void connect(String url, String folder, String title, String content) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
        params.add("api_key", apiKey);
        params.add("api_token", apiToken);
        params.add("cat_name", folder);
        params.add("page_content", content);
        params.add("page_title", title);
        logger.info("请求报文：" + params);
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(params, headers);
        ResponseEntity<String> response = TEMPLATE.exchange(url, HttpMethod.POST, requestEntity, String.class);
        HttpStatus statusCode = response.getStatusCode();
        if (statusCode == HttpStatus.OK) {
            logger.info("更新结果：" + response.getBody());
        } else {
            logger.warning("请求错误,HTTP_CODE:" + response.getStatusCodeValue() + ",信息：" + response.getBody());
        }
    }

    private String convert2String(HttpURLConnection connection) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)); // 实例化输入流，并获取网页代码
        String s; // 依次循环，至到读的值为空
        StringBuilder sb = new StringBuilder();
        while ((s = reader.readLine()) != null) {
            sb.append(s);
        }
        reader.close();
        return sb.toString();
    }

    private void doConsolePrint(List<ShowDocModel> text) {
        String catalog = this.getCorrectCatalog();
        for (ShowDocModel showDocModel : text) {
            String folder = catalog + showDocModel.getFolder();
            String title = showDocModel.getTitle();
            String content = showDocModel.getContent();
            System.out.println("目录名：" + folder);
            System.out.println("页面名：" + title);
            System.out.println("markdown页：");
            System.out.println(content);
            System.out.println(System.lineSeparator());
        }
    }

    private String getCorrectCatalog() {
        String cata = catalog;
        if (cata != null) {
            if (!cata.endsWith("/")) {
                cata = cata + "/";
            }
        } else {
            cata = "";
        }
        return cata;
    }
}
