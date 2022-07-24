package main;

import main.model.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

import javax.persistence.EntityManager;
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

    private int code;

    private Set<String> hrefElements;

    private List<String> paths;

    private List<Page> pages = new ArrayList<>();

    private Field title;

    private Field body;

    private Lemmitisator lemmitisator = new Lemmitisator();

    private List<String> titleContent;

    private List<String> bodyContent;

    private List<Lemma> lemmsFromTitle;

    private List<Lemma> lemmsFromBody;

    private float rank;

    private List<Lemma> lemmaList = new ArrayList<>();

    private List<Search_index> search_indexList = new ArrayList<>();

    public List<Search_index> getSearch_indexList() {
        return search_indexList;
    }

    public void setSearch_indexList(List<Search_index> search_indexList) {
        this.search_indexList = search_indexList;
    }

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

    public Parser(String URL) {
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

    }

    public void subpageUrlParse() throws IOException {

        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                .referrer("http://www.google.com")
                .get();
        hrefElements = new HashSet<>();
        paths = new ArrayList<>();
        Elements elements = doc.getElementsByTag("a");
        for (Element element : elements){
            if (element.attr("href").matches("/\\S+")){
                hrefElements.add(element.attr("href"));
            }
        }
        paths.addAll(hrefElements);
        Collections.sort(paths);
    }


    public void parse() throws IOException {
        bodyContent = new ArrayList<>();
        titleContent = new ArrayList<>();
        lemmsFromTitle = new ArrayList<>();
        lemmsFromBody = new ArrayList<>();

        Document doc = Jsoup.connect(subpageUrl)
                .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                .referrer("http://www.google.com")
                .get();

        URL urlForHttp = new URL(subpageUrl);
        HttpURLConnection connection = (HttpURLConnection)urlForHttp.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();
        code = connection.getResponseCode();

        matcher();

        Page page = new Page();
        page.setPath(path);
        page.setCode(code);
        page.setContent(doc.toString());
        pages.add(page);

        Elements titleEl = doc.select(title.getSelector());
        for (Element element : titleEl){
            String content = htmlToText(element.html());
            content = contentFormatting(content);
            titleContent.addAll(lemmitisator.getRusLemm(content));
            titleContent.addAll(lemmitisator.getEngLemm(content));
        }
        Elements bodyEl = doc.select(body.getSelector());
        for (Element element : bodyEl){
            String content = htmlToText(element.html());
            content = contentFormatting(content);
            bodyContent.addAll(lemmitisator.getRusLemm(content));
            bodyContent.addAll(lemmitisator.getEngLemm(content));
        }

        for (String s : titleContent){
            Lemma lemma = new Lemma();
            lemma.setLemma(s);
            if (lemmsFromTitle.isEmpty()){
                lemma.setFrequency(1);
                lemmsFromTitle.add(lemma);
            }
            if (lemmaContains(lemma, lemmsFromTitle)){
                updateFrequency(lemma, lemmsFromTitle);
            }else {
                lemma.setFrequency(1);
                lemmsFromTitle.add(lemma);
            }
        }

        for (String s: bodyContent){
            Lemma lemma = new Lemma();
            lemma.setLemma(s);
            if (lemmsFromBody.isEmpty()){
                lemma.setFrequency(1);
                lemmsFromBody.add(lemma);
            }
            if (lemmaContains(lemma, lemmsFromBody)){
                updateFrequency(lemma, lemmsFromBody);
            }else {
                lemma.setFrequency(1);
                lemmsFromBody.add(lemma);
            }
        }

        for (Lemma lemma : lemmsFromTitle){
            if(lemmaContains(lemma, lemmsFromBody)) {
                rank = (lemma.getFrequency() * 1.0f) + (frequencyFromBody(lemma) * 0.8f);
                lemma.setFrequency(frequencyAddition(lemma, lemmsFromBody));
                lemmaList.add(lemma);
                Search_index search_index = new Search_index();
                search_index.setLemma_id(lemma.getId());
                search_index.setPage_id(page.getId());
                search_index.setRating(rank);
                search_indexList.add(search_index);
                continue;
            }
            rank = (lemma.getFrequency() * 1.0f) + 0.8f;
            lemmaList.add(lemma);
            Search_index search_index = new Search_index();
            search_index.setLemma_id(lemma.getId());
            search_index.setPage_id(page.getId());
            search_index.setRating(rank);
            search_indexList.add(search_index);
        }
        for (Lemma lemma : lemmsFromBody){
            if (lemmaContains(lemma,lemmaList)){
                continue;
            }
            rank = 1.0f + (lemma.getFrequency() * 0.8f);
            lemmaList.add(lemma);
            Search_index search_index = new Search_index();
            search_index.setLemma_id(lemma.getId());
            search_index.setPage_id(page.getId());
            search_index.setRating(rank);
            search_indexList.add(search_index);
        }
    }

    public int frequencyFromBody(Lemma lemma){
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
