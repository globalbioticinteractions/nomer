package org.eol.globi.service;

import org.eol.globi.domain.TaxonImpl;

public class TaxonRequestImpl extends TaxonImpl {
    private final Long requestId;

    public TaxonRequestImpl(String lastId, String lastName, Long requestId) {
        super(lastName, lastId);
        this.requestId = requestId;
    }

    public Long getRequestId() {
        return requestId;
    }
}
