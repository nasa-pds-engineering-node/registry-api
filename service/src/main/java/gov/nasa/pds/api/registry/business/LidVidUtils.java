package gov.nasa.pds.api.registry.business;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.common.unit.TimeValue;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.SearchHit;
import org.opensearch.search.aggregations.Aggregation;
import org.opensearch.search.aggregations.AggregationBuilders;
import org.opensearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.opensearch.search.aggregations.bucket.terms.Terms.Bucket;
import org.opensearch.search.aggregations.metrics.ParsedTopHits;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.sort.FieldSortBuilder;
import org.opensearch.search.sort.SortOrder;

import gov.nasa.pds.api.registry.opensearch.OpenSearchRegistryConnection;


/**
 * Methods to get latest versions of LIDs
 * @author karpenko
 */
public class LidVidUtils
{
    /**
     * Extract lid from lidvid.
     * @param identifier LIDVID or LID.
     * @return LID
     */
    public static String extractLidFromLidVid(String identifier)
    {
        if(identifier == null) return null;
        
        // If this is a LIDVID, extract LID. Otherwise return as is.
        int idx = identifier.indexOf("::");
        if(idx > 0)
        {
            return identifier.substring(0, idx);
        }

        return identifier;
    }
    
    
    /**
     * Get latest versions of LIDs
     * @param esConnection opensearch connection
     * @param lids list of LIDs
     * @return list of LIDVIDs
     * @throws IOException
     */
    public static List<String> getLatestLidVidsByLids(OpenSearchRegistryConnection esConnection, 
            Collection<String> lids) throws IOException
    {
        // Create request
        SearchSourceBuilder src = buildGetLatestLidVidsRequest(lids);
        src.timeout(new TimeValue(esConnection.getTimeOutSeconds(), TimeUnit.SECONDS));
        
        SearchRequest esRequest = new SearchRequest(esConnection.getRegistryIndex()).source(src);
        
        // Call opensearch
        RestHighLevelClient client = esConnection.getRestHighLevelClient();
        SearchResponse esResp = client.search(esRequest, RequestOptions.DEFAULT);

        // Parse response
        // (1) Terms aggregation (top level)
        Aggregation agg = esResp.getAggregations().get("lids");
        if(agg == null) return null;
        ParsedStringTerms terms = (ParsedStringTerms)agg;

        List<String> lidvids = new ArrayList<>(lids.size());
        
        for(Bucket buk: terms.getBuckets())
        {
            // (2) Top Hits aggregation (sub-aggregation)
            agg = buk.getAggregations().get("latest");
            if(agg == null) continue;
            
            ParsedTopHits topHits = (ParsedTopHits)agg;
            SearchHit[] hits = topHits.getHits().getHits();
            if(hits != null && hits.length > 0)
            {
                String lidvid = hits[0].getId();
                lidvids.add(lidvid);
            }
        }
        
        return lidvids;
    }
    
    
    /**
     * Get all LIDVIDs by LIDs
     * @param esConnection opensearch connection
     * @param lids list of LIDs
     * @return a list of LIDVIDs
     * @throws IOException an exception
     */
    public static List<String> getAllLidVidsByLids(OpenSearchRegistryConnection esConnection, 
            Collection<String> lids) throws IOException
    {
        // Create request
        SearchSourceBuilder src = buildGetAllLidVidsRequest(lids);
        src.timeout(new TimeValue(esConnection.getTimeOutSeconds(), TimeUnit.SECONDS));
        
        SearchRequest esRequest = new SearchRequest(esConnection.getRegistryIndex()).source(src);
        
        // Call opensearch
        RestHighLevelClient client = esConnection.getRestHighLevelClient();
        SearchResponse esResp = client.search(esRequest, RequestOptions.DEFAULT);
        
        // Parse response
        List<String> lidvids = new ArrayList<>();
        esResp.getHits().forEach((hit) -> { lidvids.add(hit.getId()); });
        return lidvids;
    }

    
    /**
     * Build aggregation query to select latest versions of lids
     * @param lids list of LIDs
     * @return opensearch query
     */
    public static SearchSourceBuilder buildGetLatestLidVidsRequest(Collection<String> lids)
    {
        if(lids == null || lids.isEmpty()) return null;
        
        SearchSourceBuilder src = new SearchSourceBuilder();
        
        // Query
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        ProductQueryBuilderUtil.addArchiveStatusFilter(boolQuery);
        boolQuery.must(QueryBuilders.termsQuery("lid", lids));        
        src.query(boolQuery).fetchSource(false).size(0);

        // Aggregations
        src.aggregation(AggregationBuilders.terms("lids").field("lid").size(lids.size())
            .subAggregation(AggregationBuilders.topHits("latest").sort(new FieldSortBuilder("vid").order(SortOrder.DESC))
                    .fetchSource(false).size(1))
        );

        return src;
    }

    
    /**
     * Build terms query to select all document ids by a list of LIDs.
     * @param lids a list of LIDS
     * @return opensearch query
     */
    public static SearchSourceBuilder buildGetAllLidVidsRequest(Collection<String> lids)
    {
        if(lids == null || lids.isEmpty()) return null;

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        ProductQueryBuilderUtil.addArchiveStatusFilter(boolQuery);
        boolQuery.must(QueryBuilders.termsQuery("lid", lids));        

        SearchSourceBuilder src = new SearchSourceBuilder();
        src.query(boolQuery).fetchSource(false).size(5000);
        return src;
    }
}