package org.eol.globi.service;

import java.util.List;

import org.eol.globi.domain.Term;
import org.eol.globi.taxon.TermMatchListener;
import org.eol.globi.taxon.TermMatcher;

public class MultipleTermMatcher implements TermMatcher {

	
	private List<? extends TermMatcher> matchers;

	public MultipleTermMatcher(List<? extends TermMatcher> matchers) {
		this.matchers = matchers;
	}
	
	@Override
	public void match(List<Term> terms, TermMatchListener termMatchListener) throws PropertyEnricherException {		
		for (TermMatcher matcher : this.matchers) {
			matcher.match(terms, termMatchListener);			
			
		}		
	}

}
