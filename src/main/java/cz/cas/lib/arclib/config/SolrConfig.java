package cz.cas.lib.arclib.config;


import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.repository.config.EnableSolrRepositories;

@Configuration
@EnableSolrRepositories(basePackages = "cz.cas.lib.arclib.index.solr", multicoreSupport = true)
public class SolrConfig {

    @Value("${solr.endpoint}")
    private String endpoint;

    @Bean
    public SolrClient solrClient() {
        return new HttpSolrClient(endpoint);
    }

    @Bean
    public SolrTemplate solrTemplate(SolrClient client) throws Exception {
        return new SolrTemplate(client);
    }
}