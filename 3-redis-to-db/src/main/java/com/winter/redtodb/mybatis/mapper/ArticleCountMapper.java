package com.winter.redtodb.mybatis.mapper;

import com.winter.redtodb.dto.ArticleCountDto;
import com.winter.redtodb.mybatis.sqlprovider.ArticleCountSqlProvider;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface ArticleCountMapper {
    @InsertProvider(type = ArticleCountSqlProvider.class, method = "batchedInsertSql")
    boolean batchedInsert(@Param("list")List<ArticleCountDto> recordList);
}
