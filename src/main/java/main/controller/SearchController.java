package main.controller;

import main.model.FoundPage;
import main.Parser;
import main.model.*;
import main.properties.ApplicationProperties;
import main.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
public class SearchController {

    @Autowired
    public PageRepository pageRepository;
    @Autowired
    public FieldRepository fieldRepository;
    @Autowired
    public LemmaRepository lemmaRepository;
    @Autowired
    public IndexRepository indexRepository;
    @Autowired
    public SiteRepository siteRepository;
    @Autowired
    public ApplicationProperties applicationProperties;

    public List<Site> sites = new ArrayList<>();

    private int marker = 0;

    public Parser parser;

    public List<Parser> parsers = new ArrayList();

    public ForkJoinPool pool = new ForkJoinPool();
    public ForkJoinPool pool2 = new ForkJoinPool();

    @GetMapping("/search_engine/startIndexing")
    public ResponseEntity<Result> start(){
        if (sites.isEmpty()){
            getSites();
        }
        if (siteIsIndexing()){
            return ResponseEntity.ok().body(new Result(false,"Индексация уже запущена"));
        }
        indexingSites();
        siteRepository.saveAllAndFlush(sites);
        return ResponseEntity.ok().body(new Result(true));
    }

    public boolean siteIsIndexing(){
        for (Site site : sites){
            if (site.getStatus()!= null && site.getStatus().contains(SiteStatus.INDEXING.toString())){
                return true;
            }
        }
        return false;
    }

    @GetMapping("/search_engine/stopIndexing")
    public ResponseEntity<Result> stop(){
        if (pool.isShutdown() || pool.isTerminated()){
            return ResponseEntity.ok().body(new Result("Индексация не запущена"));
        }
        pool.shutdown();
        for (Site site : sites){
            if (siteIsIndexing()){
                site.setStatus(SiteStatus.INDEXED.toString());
                site.setStatus_time(new Date());
            }
        }
        return ResponseEntity.ok().body(new Result(true));
    }

    @PostMapping("/search_engine/indexPage")
    public ResponseEntity<Result> indexPage(@RequestParam String url){
        if (url.trim().isEmpty()){
            return ResponseEntity.ok().body(new Result("Задано пустое поле ссылки страницы"));
        }
        if (indexingPage(url)){
            return ResponseEntity.ok().body(new Result(true));
        }
        return ResponseEntity.ok().body(new Result("Данная страница находится за пределами сайтов, указанных в конфигурационном файле"));
    }

    @GetMapping("/search_engine/statistics")
    public ResponseEntity<StatisticsResult> statistics(){
        List<Detailed> detailed = new ArrayList<>();

        Total total = new Total();
        total.setSites(sites.size());
        for (Parser p : parsers){
            total.setPages(total.getPages() + p.getPages().size());
            total.setLemmas(total.getLemmas() + p.getLemmaList().size());
        }
        if (siteIsIndexing()){
            total.setIndexing(true);
        }

        for (Site site: sites){
            Detailed d = new Detailed();
            d.setUrl(site.getUrl());
            d.setName(site.getName());
            for (Parser p : parsers){
                if (d.getUrl() == p.getUrl()){
                    d.setLemmas(p.getLemmaList().size());
                    d.setPages(p.getPages().size());
                    detailed.add(d);
                }
            }
        }

        for (Site site : sites){
            for (Detailed d : detailed){
                if (d.getName().contains(site.getName())){
                    d.setStatus(site.getStatus());
                    d.setStatusTime(site.getStatus_time().getTime());
                    if (site.getLast_error() != null){
                        d.setError(site.getLast_error());
                    }
                }
            }
        }

        return ResponseEntity.ok().body(new StatisticsResult(true,new Statistics(total,detailed)));
    }

    @GetMapping("/search_engine/search")
    public ResponseEntity<FoundSite> search(@RequestParam String query, String site){
        if (!(query.trim().isEmpty())){
            if (searchParserForSite(site) != null){
                List<Data> data = new ArrayList<>();
                Query q = new Query(query, searchParserForSite(site));
                try {
                    q.textToLemma();
                    q.getPagesList();
                    q.relevancy();
                    for (FoundPage foundPage : q.getFoundPages()){
                        Data d = new Data();
                        d.setSite(site);
                        d.setSiteName(getSiteName(site));
                        d.setUri(foundPage.getUri());
                        d.setTitle(foundPage.getTitle());
                        d.setSnippet(foundPage.getSnippet());
                        d.setRelevance(foundPage.getRelevancy());
                        data.add(d);
                    }
                    return ResponseEntity.ok().body(new FoundSite(true,q.getPageList().size(),data));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else {
                List<Data> data = new ArrayList<>();
                long count = 0;
                for (Site s : sites){
                    Query q = new Query(query, searchParserForSite(s.getUrl()));
                    try {
                        q.textToLemma();
                        q.getPagesList();
                        q.relevancy();
                        for (FoundPage foundPage : q.getFoundPages()){
                            Data d = new Data();
                            d.setSite(site);
                            d.setSiteName(getSiteName(s.getUrl()));
                            d.setUri(foundPage.getUri());
                            d.setTitle(foundPage.getTitle());
                            d.setSnippet(foundPage.getSnippet());
                            d.setRelevance(foundPage.getRelevancy());
                            data.add(d);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    count += q.getPageList().size();
                }
                return ResponseEntity.ok().body(new FoundSite(true,count,data));
            }
        }
        return ResponseEntity.ok().body(new FoundSite(false, "Задан пустой поисковой запрос"));
    }

    public String getSiteName(String site){
        for (Site s : sites){
            if (s.getUrl().contains(site)){
                return s.getName();
            }
        }
        return "";
    }

    public Parser searchParserForSite(String site){
        for (Parser p : parsers){
            if (site!= null && p.getUrl().contains(site)){
                return p;
            }
        }
        return null;
    }

    public void getSites(){
        for (ApplicationProperties.Site s : applicationProperties.getSites()){
            Site site = new Site();
            site.setName(s.getName());
            site.setUrl(s.getUrl());
            sites.add(site);
        }
    }

    private boolean siteContainsPage(String url, Site site){
        String newUrl = "";
        Pattern urlPat = Pattern.compile(site.getUrl());
        Matcher urlMat = urlPat.matcher(url);
        while (urlMat.find()){
            int start = urlMat.start();
            int end = urlMat.end();
            newUrl = url.substring(start, end);
        }
        if (newUrl.contains(site.getUrl())){
            return true;
        }
        return false;
    }

    private boolean indexingPage(String url){
        for (Site site: sites){
            if (siteContainsPage(url, site)) {
                Parser parser = new Parser(url, pageRepository, lemmaRepository, indexRepository, sites, siteRepository);
                parsers.add(parser);
                this.parser = parser;
                pool2.execute(parser);
                return true;
            }
        }
        return false;
    }

    private void indexingSites() {
        for (Site site: sites)
        {
            try{
                Parser parser = new Parser(site.getUrl(), pageRepository, lemmaRepository, indexRepository, sites,siteRepository);
                this.parser = parser;
                parsers.add(this.parser);
                pool.execute(parser);
                site.setStatus(SiteStatus.INDEXING.toString());
                site.setStatus_time(new Date());
                siteRepository.save(site);
                if (marker == 0){
                    marker = 1;
                    fieldRepository.saveAndFlush(parser.getBody());
                    fieldRepository.saveAndFlush(parser.getTitle());
                }
            }catch (Exception ex){
                site.setStatus(SiteStatus.FAILED.toString());
                site.setStatus_time(new Date());
                site.setLast_error("Ошибка индексации.");
                siteRepository.saveAndFlush(site);
            }
        }
    }
}
