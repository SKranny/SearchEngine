package main;

import main.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;


import static main.Application.parser;

@RestController
public class SearchController {

    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private FieldRepository fieldRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private IndexRepository indexRepository;

    @GetMapping("/initDB")
    public void getInit(){
        init();
    }

    @PostMapping("/initDB")
    public boolean init(){
        pageRepository.saveAll(parser.getPages());
        fieldRepository.save(parser.getBody());
        fieldRepository.save(parser.getTitle());
        lemmaRepository.saveAll(parser.getLemmaList());
        indexRepository.saveAll(parser.getSearch_indexList());
        return true;
    }

}
