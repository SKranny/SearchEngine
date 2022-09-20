package main;

import main.model.*;
import main.repository.IndexRepository;
import main.repository.LemmaRepository;
import main.repository.PageRepository;
import main.repository.SiteRepository;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser extends RecursiveAction{

    private final String url;
    private String subpageUrl;
    private String path = "/";
    private List<String> paths;
    private PageRepository pageRepository;
    private LemmaRepository lemmaRepository;
    private IndexRepository indexRepository;
    private Field title;
    private Field body;
    private Lemmitisator lemmitisator = new Lemmitisator();
    private List<Page> pages = new ArrayList<>();
    private List<Lemma> lemmaList = new ArrayList<>();
    private List<Search_index> search_indexList = new ArrayList<>();
    private List<Site> sites;
    private SiteRepository siteRepository;
    private String errorStatus;

    public List<Lemma> getLemmaList() {
        return lemmaList;
    }

    public Field getTitle() {
        return title;
    }

    public Field getBody(){
        return body;
    }

    public List<Page> getPages() {
        return pages;
    }

    public List<Search_index> getSearch_indexList() {
        return search_indexList;
    }

    public Parser(String URL, PageRepository pageRepository, LemmaRepository lemmaRepository, IndexRepository indexRepository, List<Site> sites, SiteRepository siteRepository) {
        this.url = URL;
        subpageUrl = URL;

        title = new Field();
        title.setName("title");
        title.setSelector("title");
        title.setWeight(1.0f);

        body = new Field();
        body.setName("body");
        body.setSelector("body");
        body.setWeight(0.8f);
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.sites = sites;
        this.siteRepository = siteRepository;
    }

    public void subpageUrlParse() throws IOException {
        Set<String> hrefElements = new HashSet<>();
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                    .referrer("http://www.google.com")
                    .get();
            paths = new ArrayList<>();
            Elements elements = doc.getElementsByTag("a");
            for (Element element : elements){
                if (element.attr("href").matches("/\\S+")){
                    hrefElements.add(element.attr("href"));
                }
            }

            paths.addAll(hrefElements);
            Collections.sort(paths);
        }catch (HttpStatusException ex){
           errorStatus = "Страница сайта недоступна. " + "Статус: " + ex.getStatusCode();
           for (Site site : sites){
               if (site.getUrl().contains(url)){
                   site.setLast_error(errorStatus);
                   siteRepository.saveAndFlush(site);
               }
           }
        }
    }

    public String getUrl() {
        return url;
    }

    public void parse() throws IOException {
        List<String> bodyContent = new ArrayList<>();
        List<String> titleContent = new ArrayList<>();
        List<Lemma> lemmsFromTitle = new ArrayList<>();
        List<Lemma> lemmsFromBody = new ArrayList<>();
        float rank;

        URL urlForHttp = new URL(subpageUrl);
        HttpURLConnection connection = (HttpURLConnection)urlForHttp.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();
        int code = connection.getResponseCode();
        try {
            Document doc = Jsoup.connect(subpageUrl)
                    .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                    .referrer("http://www.google.com")
                    .get();
            matcher();

            Page page = new Page();
            page.setPath(path);
            page.setCode(code);
            page.setContent(doc.toString());
            page.setSite_id(findSiteIdByUrl());
            pages.add(page);
            pageRepository.save(page);

            getTextForLemms(doc,title,titleContent);
            getTextForLemms(doc,body,bodyContent);

            textToLemma(titleContent, lemmsFromTitle);
            textToLemma(bodyContent, lemmsFromBody);

            for (Lemma lemma : lemmsFromTitle){
                if(lemmaContains(lemma, lemmsFromBody)) {
                    rank = (lemma.getFrequency() * 1.0f) + (frequencyFromBody(lemma, lemmsFromBody) * 0.8f);
                    lemma.setFrequency(frequencyAddition(lemma, lemmsFromBody));
                    lemma.setSite_id(findSiteIdByUrl());
                    lemmaList.add(lemma);
                    lemmaRepository.save(lemma);
                    Search_index search_index = new Search_index();
                    search_index.setLemma_id(lemma.getId());
                    search_index.setPage_id(page.getId());
                    search_index.setRating(rank);
                    search_indexList.add(search_index);
                    indexRepository.save(search_index);
                    continue;
                }
                rank = (lemma.getFrequency() * 1.0f);
                lemma.setSite_id(findSiteIdByUrl());
                lemmaList.add(lemma);
                lemmaRepository.save(lemma);
                Search_index search_index = new Search_index();
                search_index.setLemma_id(lemma.getId());
                search_index.setPage_id(page.getId());
                search_index.setRating(rank);
                search_indexList.add(search_index);
                indexRepository.save(search_index);
            }
            for (Lemma lemma : lemmsFromBody){
                if (lemmaContains(lemma,lemmaList)){
                    continue;
                }
                rank = (lemma.getFrequency() * 0.8f);
                lemma.setSite_id(findSiteIdByUrl());
                lemmaList.add(lemma);
                lemmaRepository.save(lemma);
                Search_index search_index = new Search_index();
                search_index.setLemma_id(lemma.getId());
                search_index.setPage_id(page.getId());
                search_index.setRating(rank);
                search_indexList.add(search_index);
                indexRepository.save(search_index);
            }
        }catch (HttpStatusException ex){
            errorStatus = "Одна из страниц сайта недоступна. " + "Статус: " + ex.getStatusCode();
            for (Site site : sites){
                if (site.getUrl().contains(url)){
                    site.setLast_error(errorStatus);
                    siteRepository.saveAndFlush(site);
                }
            }
        }
    }

    public int findSiteIdByUrl(){
        for (Site site : sites){
            if (url.contains(site.getUrl())){
                return site.getId();
            }
        }
        return 0;
    }

    public void getTextForLemms(Document doc, Field field, List<String> list) throws IOException {
        Elements el = doc.select(field.getSelector());
        for (Element element : el){
            String content = htmlToText(element.html());
            content = contentFormatting(content);
            list.addAll(lemmitisator.getRusLemm(content));
            list.addAll(lemmitisator.getEngLemm(content));
        }
    }

    public void textToLemma(List<String> content,List<Lemma> lemms){
        for (String s : content){
            Lemma lemma = new Lemma();
            lemma.setLemma(s);
            if (lemms.isEmpty()){
                lemma.setFrequency(1);
                lemms.add(lemma);
            }
            if (lemmaContains(lemma, lemms)){
                updateFrequency(lemma, lemms);
            }else {
                lemma.setFrequency(1);
                lemms.add(lemma);
            }
        }
    }

    public int frequencyFromBody(Lemma lemma, List<Lemma> lemmsFromBody){
        for (Lemma l : lemmsFromBody){
            if (l.getLemma().contains(lemma.getLemma())){
                return l.getFrequency();
            }
        }
        return 0;
    }

    public int frequencyAddition(Lemma lemma, List<Lemma> list){
        for (Lemma l: list){
            if (l.getLemma().contains(lemma.getLemma())){
                return lemma.getFrequency() + l.getFrequency();
            }
        }
        return lemma.getFrequency();
    }

    public boolean lemmaContains(Lemma lemma, List<Lemma> list){
        for (Lemma l : list){
            if (l.getLemma().contains(lemma.getLemma())){
                return true;
            }
        }
        return false;
    }

    public void updateFrequency(Lemma lemma, List<Lemma> list){
        for (Lemma l : list){
            if (l.getLemma().contains(lemma.getLemma())){
                l.setFrequency(l.getFrequency() + 1);
            }
        }
    }

    public String htmlToText(String html){
        return Jsoup.parse(html).text();
    }

    public String contentFormatting(String content){
        content = content.toLowerCase();
        content = content.replaceAll("/", " ");
        return content;
    }

    public void matcher(){
        Pattern urlPat = Pattern.compile(".[a-z]+/\\S+");
        Matcher urlMat = urlPat.matcher(subpageUrl);
        while (urlMat.find()){
            int start = urlMat.start();
            int end = urlMat.end();
            path = subpageUrl.substring(start, end);
        }
        Pattern pathPat = Pattern.compile("/\\S+");
        Matcher pathMat = pathPat.matcher(path);
        while (pathMat.find()){
            int start = pathMat.start();
            int end = pathMat.end();
            path = path.substring(start, end);
        }
    }

    @Override
    protected void compute() {
        try {
            subpageUrlParse();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            for (int i = -1; i < paths.size(); i++){
                if (i<0){
                    subpageUrlParse();
                    parse();
                }else {
                    subpageUrl = url + paths.get(i);
                    parse();
                }
            }
            for (Site site: sites){
                if (site.getUrl().contains(url)){
                    site.setStatus(SiteStatus.INDEXED.toString());
                    site.setStatus_time(new Date());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
