package cz.cas.lib.arclib.config;


import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static cz.cas.lib.core.util.Utils.asList;

@Configuration
public class SolrConfig {

    @Value("${solr.endpoint}")
    private String endpoint;

    @Bean
    public SolrClient solrClient() {
        return new CloudSolrClient.Builder(asList(endpoint)).build();
    }
}