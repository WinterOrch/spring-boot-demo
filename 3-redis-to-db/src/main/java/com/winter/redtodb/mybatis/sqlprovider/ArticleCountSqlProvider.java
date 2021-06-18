package com.winter.redtodb.mybatis.sqlprovider;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

public class ArticleCountSqlProvider {
    private static final MessageFormat mf = new MessageFormat(
            "(#'{'list[{0}].buNo},#'{'list[{0}].customerId},#'{'list[{0}].articleNo},#'{'list[{0}].readTime})");

    public String batchedInsertSql(Map<String, Object> map) {
        int listSize = ((List)map.get("list")).size();

        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("INSERT INTO zh_article_count ");
        sqlBuilder.append("(bu_no, customer_id, article_no, read_time) ");
        sqlBuilder.append("VALUES ");
        for (int i = 0; i < listSize; ++i) {
            sqlBuilder.append(mf.format(new Object[]{i}));
            if (i < listSize - 1) {
                sqlBuilder.append(",");
            }
        }
        return sqlBuilder.toString().trim();
    }
}
