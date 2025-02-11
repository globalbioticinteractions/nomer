package org.eol.globi.taxon;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.tool.TermRequestImpl;
import org.eol.globi.util.CSVTSVUtil;
import org.eol.globi.util.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class GlobalNamesService2 extends PropertyEnricherSimple implements TermMatcher {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalNamesService2.class);

    private final List<GlobalNamesSources2> sources;

    public GlobalNamesService2() {
        this(GlobalNamesSources2.ITIS);
    }

    public GlobalNamesService2(GlobalNamesSources2 source) {
        this(Collections.singletonList(source));
    }

    public GlobalNamesService2(List<GlobalNamesSources2> sources) {
        super();
        this.sources = sources;
    }

    @Override
    public Map<String, String> enrich(Map<String, String> properties) throws PropertyEnricherException {
        Map<String, String> enrichedProperties = new HashMap<String, String>();
        final List<Taxon> exactMatches = new ArrayList<Taxon>();
        final List<Taxon> synonyms = new ArrayList<Taxon>();
        TermImpl termRequested = new TermImpl(
                properties.get(PropertyAndValueDictionary.EXTERNAL_ID),
                properties.get(PropertyAndValueDictionary.NAME)
        );

        findTermsForNames(Collections.singletonList(termRequested), new TermMatchListener() {
            @Override
            public void foundTaxonForTerm(Long nodeId,
                                          Term termRequested,
                                          NameType nameType,
                                          Taxon taxon) {
                if (Arrays.asList(NameType.HAS_ACCEPTED_NAME, NameType.SAME_AS).contains(nameType)) {
                    exactMatches.add(taxon);
                } else if (NameType.SYNONYM_OF.equals(nameType)) {
                    synonyms.add(taxon);
                }
            }
        });

        if (exactMatches.size() > 0) {
            enrichedProperties.putAll(TaxonUtil.taxonToMap(exactMatches.get(0)));
        } else if (synonyms.size() == 1) {
            enrichedProperties.putAll(TaxonUtil.taxonToMap(synonyms.get(0)));
        }

        return Collections.unmodifiableMap(enrichedProperties);
    }

    @Override
    public void match(List<Term> terms, TermMatchListener termMatchListener) throws PropertyEnricherException {
        if (terms.size() == 0) {
            throw new IllegalArgumentException("need non-empty list of names");
        }
        findTermsForNames(terms, termMatchListener);
    }

    private void findTermsForNames(List<Term> terms, TermMatchListener termMatchListener) throws PropertyEnricherException {
        if (terms.size() == 0) {
            throw new IllegalArgumentException("need non-empty list of names");
        }

        try {
            URI uri = buildPostRequestURI();
            try {
                parseResult(termMatchListener, executeQuery(terms, sources, uri), new RequestedTermService() {
                    private Map<Long, TermRequestImpl> requestMap = null;

                    @Override
                    public Term termForRequestId(Long requestId) {
                        if (requestMap == null) {
                            final Map<Long, TermRequestImpl> tmpRequestMap = new TreeMap<>();
                            terms.stream().filter(x -> x instanceof TermRequestImpl)
                                    .map(x -> (TermRequestImpl) x)
                                    .forEach(x -> tmpRequestMap.put(x.getNodeId(), x));
                            requestMap = new TreeMap<>(tmpRequestMap);
                        }

                        return requestMap.get(requestId);
                    }
                });
            } catch (IOException e) {
                if (terms.size() > 1) {
                    LOG.warn("retrying names query one name at a time: failed to perform batch query", e);
                    List<String> namesFailed = new ArrayList<>();
                    for (Term term : terms) {
                        try {
                            final List<Term> singleTermRequest = Collections.singletonList(term);
                            parseResult(termMatchListener, executeQuery(singleTermRequest, sources, uri), new RequestedTermService() {
                                private Term term1 = singleTermRequest.get(0);

                                @Override
                                public Term termForRequestId(Long requestId) {
                                    return term1 instanceof TermRequestImpl
                                            ? ((TermRequestImpl) term1).getNodeId().equals(requestId) ? term1 : null
                                            : null;
                                }
                            });
                        } catch (IOException e1) {
                            namesFailed.add(term.getName());
                        }
                    }
                    if (namesFailed.size() > 0) {
                        throw new PropertyEnricherException("Failed to execute individual name queries for [" + StringUtils.join(namesFailed, "|") + "]");
                    }
                }
            }
        } catch (URISyntaxException e) {
            throw new PropertyEnricherException("Failed to query", e);
        }

    }

    private String executeQuery(List<Term> terms, List<GlobalNamesSources2> sources, URI uri) throws IOException {
        HttpClient httpClient = HttpUtil.getHttpClient();
        HttpPost post = new HttpPost(uri);
        HttpUtil.addJsonHeaders(post);

        ObjectNode request = new ObjectMapper().createObjectNode();
        ArrayNode arrayNode = new ObjectMapper().createArrayNode();
        terms.stream().map(Term::getName).forEach(arrayNode::add);
        request.set("nameStrings", arrayNode);
        ArrayNode preferredSources = new ObjectMapper().createArrayNode();
        sources.stream().map(GlobalNamesSources2::getId).forEach(preferredSources::add);
        request.set("dataSources", preferredSources);
        request.put("withAllMatches", true);
        post.setEntity(new StringEntity(request.toString(), StandardCharsets.UTF_8));

        return httpClient.execute(post, new BasicResponseHandler());
    }

    private URI buildPostRequestURI() throws URISyntaxException {
        return new URI("https", "verifier.globalnames.org"
                , "/api/v1/verifications"
                , null
                , null);
    }

    static void parseResult(TermMatchListener termMatchListener, String result, RequestedTermService termService) throws PropertyEnricherException {
        try {
            parseResultNode(termMatchListener, new ObjectMapper().readTree(result), termService);
        } catch (IOException ex) {
            throw new PropertyEnricherException("failed to parse json string [" + result + "]", ex);
        }
    }

    public void setIncludeCommonNames(boolean includeCommonNames) {

    }

    interface RequestedTermService {
        Term termForRequestId(Long requestId);
    }

    static void parseResultNode(TermMatchListener termMatchListener, JsonNode jsonNode, RequestedTermService termService) {

        JsonNode nameMatches = jsonNode.has("names") ? jsonNode.get("names") : jsonNode;

        for (JsonNode nameMatch : nameMatches) {
            JsonNode suppliedNameNode = nameMatch.at("/name");
            JsonNode suppliedNameNodeId = nameMatch.at("/id");
            JsonNode matchTypeNode = nameMatch.at("/matchType");
            if (!suppliedNameNode.isMissingNode()
                    && !suppliedNameNodeId.isMissingNode()
                    && !matchTypeNode.isMissingNode()) {
                String suppliedNameString = suppliedNameNode.asText();
                String matchType = matchTypeNode.asText();
                Map<String, NameType> nameTypeMap = new TreeMap<String, NameType>() {{
                    put("NoMatch", NameType.NONE);
                    put("Fuzzy", NameType.SIMILAR_TO);
                    put("Exact", NameType.SAME_AS);
                }};

                NameType nameType = nameTypeMap.get(matchType);

                if (NameType.NONE.equals(nameType)) {
                    Term termRequested = new TermImpl(null, suppliedNameString);
                    termMatchListener.foundTaxonForTerm(
                            null,
                            termRequested,
                            nameType,
                            new TaxonImpl(suppliedNameString)
                    );
                } else if (Arrays.asList(NameType.SIMILAR_TO, NameType.SAME_AS).contains(nameType)) {
                    if (nameMatch.has("results")) {
                        for (JsonNode result : nameMatch.get("results")) {
                            handleResult(termMatchListener, suppliedNameString, nameType, result);
                        }
                    }
                    if (nameMatch.has("bestResult")) {
                        handleResult(termMatchListener, suppliedNameString, nameType, nameMatch.get("bestResult"));
                    }

                }
            }
        }


    }

    private static NameType handleResult(
            TermMatchListener termMatchListener,
            String suppliedNameString,
            final NameType nameType,
            JsonNode result) {

        NameType resolvedNameType = nameType;
        Term termRequested = new TermImpl(null, suppliedNameString);
        String resolvedName = result.at("/currentCanonicalFull").asText();
        resolvedName = StringUtils.isBlank(resolvedName) ? result.at("/matchedName").asText() : resolvedName;
        String path = result.at("/classificationPath").asText();
        String ranks = result.at("/classificationRanks").asText();
        String ids = result.at("/classificationIds").asText();
        String id = result.at("/currentRecordId").asText();
        id = StringUtils.isBlank(id) ? result.at("/recordId").asText() : id;
        TaxonImpl resolved = new TaxonImpl(resolvedName);
        resolved.setPathNames(parsePathIds(ranks));
        String[] nameRanks = CSVTSVUtil.splitPipes(ranks);
        if (nameRanks.length > 0) {
            String rank = nameRanks[nameRanks.length - 1];
            resolved.setRank(StringUtils.trim(rank));
        }

        resolved.setPath(parsePathIds(path));
        int dataSourceId = result.at("/dataSourceId").asInt();
        String taxonomicStatus = result.at("/taxonomicStatus").asText();
        if (NameType.SAME_AS.equals(resolvedNameType)) {
            if ("Accepted".equalsIgnoreCase(taxonomicStatus)) {
                resolvedNameType = NameType.HAS_ACCEPTED_NAME;
            } else if ("Synonym".equalsIgnoreCase(taxonomicStatus)) {
                resolvedNameType = NameType.SYNONYM_OF;
            }
        }
        TaxonomyProvider taxonomyProvider = getTaxonomyProvider(dataSourceId);

        // related to https://github.com/GlobalNamesArchitecture/gni/issues/48
        if (taxonomyProvider != null
                && !pathTailRepetitions(resolved)) {
            resolved.setPathIds(parsePathIds(ids, taxonomyProvider.getIdPrefix()));
            resolved.setExternalId(taxonomyProvider.getIdPrefix() + scrubIds(id));
            termMatchListener.foundTaxonForTerm(
                    null,
                    termRequested,
                    resolvedNameType,
                    resolved
            );
        } else {
            termMatchListener.foundTaxonForTerm(
                    null,
                    termRequested,
                    NameType.NONE,
                    new TaxonImpl(termRequested.getName(), termRequested.getId())
            );
        }
        return nameType;
    }


    private static Term createTermRequested(RequestedTermService termService, String suppliedNameString, Long requestId) {
        TermImpl termRequested = new TermImpl(null, suppliedNameString);
        if (requestId != null) {
            Term term = termService.termForRequestId(requestId);
            if (term != null) {
                termRequested.setId(term.getId());
            }
        }
        return termRequested;
    }

    private static String parsePathIds(String list) {
        return parsePathIds(list, null);
    }

    private static String parsePathIds(String list, String prefix) {
        String[] split = StringUtils.splitPreserveAllTokens(list, "|");
        List<String> ids = Collections.emptyList();
        if (split != null) {
            ids = Arrays.asList(split);
        }
        if (StringUtils.isNotBlank(prefix)) {
            List<String> prefixed = new ArrayList<String>();
            for (String id : ids) {
                if (StringUtils.startsWith(id, "gn:")) {
                    prefixed.add("");
                } else {
                    prefixed.add(prefix + scrubIds(id));
                }
            }
            ids = prefixed;
        }
        return StringUtils.join(ids, CharsetConstant.SEPARATOR);
    }

    private static String scrubIds(String s) {
        return StringUtils.replace(s, "urn:lsid:marinespecies.org:taxname:", "");
    }

    public static boolean pathTailRepetitions(Taxon taxon) {
        boolean repetitions = false;
        if (StringUtils.isNotBlank(taxon.getPath())) {
            String[] split = StringUtils.split(taxon.getPath(), CharsetConstant.SEPARATOR_CHAR);
            if (split.length > 2
                    && repeatInTail(split)) {
                repetitions = true;
            }
        }
        return repetitions;
    }

    private static boolean repeatInTail(String[] split) {
        String last = StringUtils.trim(split[split.length - 1]);
        String secondToLast = StringUtils.trim(split[split.length - 2]);
        return StringUtils.equals(last, secondToLast);
    }

    private static String getSuppliedNameString(JsonNode data) {
        return data.get("supplied_name_string").asText();
    }

    private static Long requestId(JsonNode data) {
        return data.has("supplied_id") ? data.get("supplied_id").asLong() : null;
    }

    private static TaxonomyProvider getTaxonomyProvider(JsonNode aResult) {
        TaxonomyProvider provider = null;
        if (aResult.has("data_source_id")) {
            int sourceId = aResult.get("data_source_id").asInt();

            provider = getTaxonomyProvider(sourceId);
        }
        return provider;
    }

    private static TaxonomyProvider getTaxonomyProvider(int sourceId) {
        TaxonomyProvider provider = null;
        GlobalNamesSources2[] values = GlobalNamesSources2.values();
        for (GlobalNamesSources2 value : values) {
            if (value.getId() == sourceId) {
                provider = value.getProvider();
                break;
            }
        }
        return provider;
    }

    public List<GlobalNamesSources2> getSources() {
        return sources;
    }

    public void shutdown() {

    }
}
