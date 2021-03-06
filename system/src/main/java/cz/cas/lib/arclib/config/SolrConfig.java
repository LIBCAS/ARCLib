package cz.cas.lib.arclib.config;


import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.schema.SolrPersistentEntitySchemaCreator;
import org.springframework.data.solr.repository.config.EnableSolrRepositories;

import java.util.Collections;

import static cz.cas.lib.core.util.Utils.asList;

@Configuration
@EnableSolrRepositories(basePackages = "cz.cas.lib.arclib.index.solr")
public class SolrConfig {

    @Value("${solr.endpoint}")
    private String endpoint;

    @Bean
    public SolrClient solrClient() {
        return new CloudSolrClient.Builder(asList(endpoint)).build();
    }

    @Bean
    @Primary
    public SolrTemplate solrTemplate(SolrClient client) throws Exception {
        SolrTemplate template = new SolrTemplate(client);
        template.setSchemaCreationFeatures(Collections.singletonList(SolrPersistentEntitySchemaCreator.Feature.CREATE_MISSING_FIELDS));
        //todo: this converter classhes with MappingSolrConverter which adds functionality
        //only purpose of this converter are child elements and solr references which are in progress anyway so for now it is commented
        //MySolrJConverter mySolrJConverter = new MySolrJConverter();
        //template.setSolrConverter(mySolrJConverter);
        //template.afterPropertiesSet();
        return template;
    }

    @Bean(name = "ArclibXmlSolrTemplate")
    public SolrTemplate arclibXmlSolrTemplate(SolrClient client) throws Exception {
        SolrTemplate template = new SolrTemplate(client);
        return template;
    }
}