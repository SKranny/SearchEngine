package main.model;

import main.Lemmitisator;
import main.Parser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Query {
    private String text;
    private Lemmitisator lemmitisator = new Lemmitisator();
    private List<Lemma> lemmaList = new ArrayList<>();
    private List<Page> pageList = new ArrayList<>();
    private List<Lemma> lemmasWithDifferentId = new ArrayList<>();
    private List<FoundPage> foundPages = new ArrayList<>();
    private Parser parser;
    public Query(String text, Parser parser) {
        this.text = text;
        this.parser = parser;
    }

    public List<Page> getPageList() {
        return pageList;
    }

    public void textToLemma() throws IOException {
        List<String> words = new ArrayList<>();
        words.addAll(lemmitisator.getRusLemm(text));
        words.addAll(lemmitisator.getEngLemm(text));
        for (String s : words){
            Lemma lemma = new Lemma();
            lemma.setLemma(s);
            lemma.setFrequency(1);
            if (lemmaList.isEmpty()){
                lemmaList.add(lemma);
                continue;
            }
            if (lemmaContains(lemma, lemmaList)){
                updateFrequency(lemma,lemmaList);
            }else {
                lemmaList.add(lemma);
            }
        }
        sort();
    }

    public void sort(){
        Collections.sort(lemmaList, Comparator.comparing(Lemma::getFrequency).thenComparing(Lemma::getLemma));
    }

    public List<Page> getPagesList(){
        List<Lemma> lemmasFromParser = parser.getLemmaList();
        List<Integer> pagesId = new ArrayList<>();
        for (Lemma lemma: lemmaList){
            List<Lemma> lemmas = new ArrayList<>();
            if (pagesId.isEmpty()){
                if (lemmaContains(lemma, lemmasFromParser)){
                    getOneLemmaWithDifferentId(lemma,lemmasFromParser, lemmas);
                    for (Lemma l: lemmas){
                        pagesId.addAll(getPagesId(l,parser.getSearch_indexList()));
                    }
                    findPagesByIdInParser(pagesId);
                    continue;
                }
            }
            if (lemmaContains(lemma, lemmasFromParser)){
                getOneLemmaWithDifferentId(lemma,lemmasFromParser, lemmas);
                updatePageList(pageList, parser.getSearch_indexList(), lemmas);
            }
        }
        return pageList;
    }

    public void relevancy(){
        if (!pageList.isEmpty()){
            List<ExtendedPage> exPages = new ArrayList<>();
            List <Float> relevance = new ArrayList<>();
            for (Page page : pageList){
                List<Float> ranks = new ArrayList<>();
                float rel = 0;
                for (Lemma lemma: lemmasWithDifferentId){
                    float rank = findLemmaRank(page, lemma);
                    ranks.add(rank);
                }
                for (float rank : ranks){
                    rel += rank;
                }
                relevance.add(rel);
                ExtendedPage extendedPage = new ExtendedPage(page,rel);
                exPages.add(extendedPage);
            }

            Collections.sort(exPages, Comparator.comparing(ExtendedPage::getAbsoluteRelevancy).reversed());
            float temp = -1;
            for (ExtendedPage extendedPage : exPages){
                if (temp == -1){
                    temp = extendedPage.getAbsoluteRelevancy();
                    extendedPage.setRelativeRelevance(extendedPage.getAbsoluteRelevancy() / temp);
                    continue;
                }
                extendedPage.setRelativeRelevance(extendedPage.getAbsoluteRelevancy()/temp);
            }
            Collections.sort(exPages, Comparator.comparing(ExtendedPage::getRelativeRelevance).reversed());
            for (ExtendedPage extendedPage : exPages){
                String content = extendedPage.getPage().getContent();
                FoundPage foundPage = new FoundPage();
                foundPage.setUri(extendedPage.getPage().getPath());
                foundPage.setRelevancy(extendedPage.getRelativeRelevance());
                foundPage.setTitle(searchTitle(content));
                foundPage.setSnippet(searchSnippet());
                foundPages.add(foundPage);
            }
        }
    }

    public String searchSnippet(){
        String snippet = "";
        Elements elements = null;
        List<String> contents = new ArrayList<>();
        for (FoundPage foundPage : foundPages){
            contents.add(searchPageContent(foundPage));
        }
        for (String con : contents){
             elements = Jsoup.parse(con).head().getElementsByTag("meta");
        }
        if (elements != null){
            for (Element element : elements){
                if (element.toString().contains("name=\"description\"")){
                    snippet = element.attr("content");
                }
            }
        }
        for (Lemma lemma : lemmaList){
                String[] words = snippet.split("[^A-я]");
                for (String s : words){
                    if (s.contains(lemma.getLemma())){
                        snippet = snippet.replaceAll(s, "<b>" + s + "</b>");
                    }
                }
        }
        return snippet;
    }

    public String searchPageContent(FoundPage foundPage){
        for (Page page : parser.getPages()){
            if (page.getPath().contains(foundPage.getUri())){
                return page.getContent();
            }
        }
        return "";
    }

    public String searchTitle(String content){
        return Jsoup.parse(content).title();
    }

    public float findLemmaRank(Page page, Lemma lemma){
        for (Search_index index : parser.getSearch_indexList()){
            if (index.getPage_id() == page.getId() && lemma.getId() == index.getLemma_id()){
                return index.getRating();
            }
        }
        return 0;
    }


    public void getOneLemmaWithDifferentId(Lemma lemma, List<Lemma> list, List<Lemma> lemmas){
        for (Lemma l : list){
            if (l.getLemma().contains(lemma.getLemma())){
                Lemma newId = new Lemma();
                newId.setLemma(lemma.getLemma());
                newId.setFrequency(lemma.getFrequency());
                newId.setId(l.getId());
                lemmas.add(newId);
                lemmasWithDifferentId.add(newId);
            }
        }
    }

    public void updatePageList(List<Page> pageList, List<Search_index> indexList, List<Lemma> lemmas){
        List<Page> tempPageList = new ArrayList<>();
        for (Search_index index : indexList){
            for (Page page : pageList){
                for (Lemma l : lemmas){
                    if (index.getPage_id() == page.getId() && index.getLemma_id() == l.getId()){
                        tempPageList.add(page);
                    }
                }
            }
        }
        pageList.clear();
        pageList.addAll(tempPageList);
    }

    public void findPagesByIdInParser(List<Integer> pagesId){
        List<Page> pages = parser.getPages();
        for (Integer id: pagesId){
            for (Page page : pages){
                if (page.getId() == id){
                    pageList.add(page);
                }
            }
        }
    }

    public List<FoundPage> getFoundPages() {
        return foundPages;
    }

    public void setFoundPages(List<FoundPage> foundPages) {
        this.foundPages = foundPages;
    }

    public int idFromParser(Lemma lemma, List<Lemma> lemmasFromParser){
        for (Lemma l : lemmasFromParser){
            if (l.getLemma().contains(lemma.getLemma())){
                return l.getId();
            }
        }
        return 0;
    }

    private List<Integer> getPagesId(Lemma lemma, List<Search_index> indexList){
        List<Integer> pagesId = new ArrayList<>();
        for (Search_index index : indexList){
            if (index.getLemma_id() == lemma.getId()){
                pagesId.add(index.getPage_id());
            }
        }
        return pagesId;
    }

    private boolean lemmaContains(Lemma lemma, List<Lemma> list){
        for (Lemma l : list){
            if (l.getLemma().contains(lemma.getLemma())){
                return true;
            }
        }
        return false;
    }

    private void updateFrequency(Lemma lemma, List<Lemma> list){
        for (Lemma l : list){
            if (l.getLemma().contains(lemma.getLemma())){
                l.setFrequency(l.getFrequency() + 1);
            }
        }
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<Lemma> getLemmaList() {
        return lemmaList;
    }

    public void setLemmaList(List<Lemma> lemmaList) {
        this.lemmaList = lemmaList;
    }
}
