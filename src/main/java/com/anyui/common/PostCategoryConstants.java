package com.anyui.common;

/**
 * 帖子分类常量定义
 * 对应数据库 tb_post 表的 category 字段 (varchar 20)
 */
public class PostCategoryConstants {
    public static final String CONFESSION = "confession";   // 表白墙
    public static final String CAMPUS_LIFE = "campus_life"; // 校园趣事
    public static final String LOST_FOUND = "lost_found";   // 失物招领
    public static final String FLEA_MARKET = "flea_market"; // 闲置交易
    public static final String ACADEMIC = "academic";       // 学业互助

    // 校验分类是否合法的工具方法
    public static boolean isValid(String category) {
        return CONFESSION.equals(category) ||
                CAMPUS_LIFE.equals(category) ||
                LOST_FOUND.equals(category) ||
                FLEA_MARKET.equals(category) ||
                ACADEMIC.equals(category);
    }
}