package com.game.buildingstory.web;

import com.game.buildingstory.domain.CompanyNewsArticle;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CompanyReportingModelAssemblerTests {
    @Test
    void visibleNewsIsCappedEvenWhenAllOldArticlesAreUnread() {
        List<CompanyNewsArticle> articles = new ArrayList<>();
        for (int index = 0; index < 50; index++) {
            CompanyNewsArticle article = mock(CompanyNewsArticle.class);
            when(article.isUnread()).thenReturn(true);
            articles.add(article);
        }

        List<CompanyNewsArticle> visible = CompanyReportingModelAssembler.visibleNews(articles);

        assertThat(visible).hasSize(30);
        assertThat(visible).containsExactlyElementsOf(articles.subList(0, 30));
    }
}
