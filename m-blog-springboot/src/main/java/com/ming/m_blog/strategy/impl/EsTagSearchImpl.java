package com.ming.m_blog.strategy.impl;

import com.ming.m_blog.constant.CommonConst;
import com.ming.m_blog.dto.search.CategorySearchDTO;
import com.ming.m_blog.dto.search.TagSearchDTO;
import com.ming.m_blog.pojo.Tag;
import com.ming.m_blog.vo.SearchVO;
import lombok.extern.log4j.Log4j2;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * es搜索标签
 */
@Log4j2
@Service("esTagSearchImpl")
public class EsTagSearchImpl extends AbstractSearchStrategy{

    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Override
    public SearchVO search(String keyword) {
        if (StringUtils.isEmpty(keyword)){
            return new SearchVO();
        }
        return SearchVO.builder()
                .tagList(doSearch(buildQuery(keyword)))
                .build();
    }

    /**
     * 构造es条件构造器
     * @return 条件构造器
     */
    private NativeSearchQueryBuilder buildQuery(String keyword){
        NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder();
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        // 标签不分词
        boolQueryBuilder.must(QueryBuilders.termQuery("tagName", keyword));

        nativeSearchQueryBuilder.withQuery(boolQueryBuilder);
        return nativeSearchQueryBuilder;
    }

    /**
     * 执行搜索操作
     * @param nativeSearchQueryBuilder 条件构造器
     * @return                         搜索结果
     */
    private List<TagSearchDTO> doSearch(NativeSearchQueryBuilder nativeSearchQueryBuilder){
        // 设置分类名称高亮
        HighlightBuilder.Field categoryNameField = new HighlightBuilder.Field("tagName");
        categoryNameField.preTags(CommonConst.PRE_TAG);
        categoryNameField.postTags(CommonConst.POST_TAG);
        // 绑定条件构造器
        nativeSearchQueryBuilder.withHighlightFields(categoryNameField);

        // 进行搜索
        try {
            return elasticsearchRestTemplate.search(nativeSearchQueryBuilder.build(), TagSearchDTO.class)
                    .stream().map(hit -> {
                        TagSearchDTO tagSearchDTO = hit.getContent();
                        List<String> tagNameHighList = hit.getHighlightFields().get("tagName");
                        tagSearchDTO.setTagName(tagNameHighList.get(0));
                        return tagSearchDTO;
                    }).collect(Collectors.toList());
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        return new ArrayList<>();
    }

}
