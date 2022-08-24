package com.github.jklasd.test.core.common.methodann.mock.h2;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Maps;

/**
 * @author one.xu
 * @version v1.0
 * @description
 * @date 2022/8/15 16:46
 */
public class TransformMysqlToH2 {
    private static final AtomicInteger INDEX = new AtomicInteger();

    /**
     * h2的索引名必须全局唯一
     *
     * @param content sql建表脚本
     * @return 替换索引名为全局唯一
     */
    private static String uniqueKey(String content) {
        Pattern pattern = Pattern.compile("(?<=KEY )(.*?)(?= \\()");
        Matcher matcher = pattern.matcher(content);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            int inc = INDEX.getAndIncrement();
            matcher.appendReplacement(sb, matcher.group() + inc);
        }
        matcher.appendTail(sb);
        content = sb.toString();
        return content;
    }

    /**
     * @param content
     * @return
     */
    public static String transformCreateTable(String content) {
        content = "SET MODE MYSQL;\n\n" + content;

        String[] keys = "`key`,`value`,`group`".split(",");
        Map<String,String> replaceKeyMap = Maps.newHashMap();
        String keywords = "key_expression_junit";
        int i=1;
        for(String key:keys) {
        	String replaceKey = keywords+i++;
        	content = content.replace(key, replaceKey);
        	replaceKeyMap.put(replaceKey, key);
        	
        }
        
        content = content.replaceAll("`", "");
        
        Iterator<Entry<String,String>> ite = replaceKeyMap.entrySet().iterator();
        while(ite.hasNext()) {
        	Entry<String,String> tmp = ite.next();
        	content = content.replace(tmp.getKey(), tmp.getValue());
        }
        
        content = content.replaceAll("COLLATE.*(?=D)", "");
        content = content.replaceAll("COMMENT.*'(?=,)", "");
        content = content.replaceAll("COMMENT.*'(?=)", "");
        content = content.replaceAll("\\).*ENGINE.*(?=;)", ")");
        content = content.replace("DEFAULT b", "DEFAULT ");

        String[] cullStr = {"USING BTREE","CHARACTER SET utf8mb4","CHARACTER SET utf8","COLLATE utf8_bin","COLLATE utf8mb4_bin"};
        
        for(String str : cullStr) {
        	content = content.replace(str, "");
        }
        
        
        /*
         * 对修改时间插入值有问题
         */
//        content = content.replaceAll("DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP", " AS CURRENT_TIMESTAMP");

        content = uniqueKey(content);

        return content;
    }

    public static void main(String[] args) {
        String content = "DROP TABLE IF EXISTS maucash_activity;\r\n"
        		+ "\r\n"
        		+ "CREATE TABLE `maucash_activity` (\r\n"
        		+ "  `id` int(10) NOT NULL AUTO_INCREMENT,\r\n"
        		+ "  `title` varchar(100) DEFAULT NULL COMMENT '活动标题',\r\n"
        		+ "  `activity_desc` varchar(500) DEFAULT NULL COMMENT '活动描述',\r\n"
        		+ "  `activity_status` tinyint(4) DEFAULT NULL COMMENT '状态值:0-正常状态由起始时间和结束时间控制，1-立即终止',\r\n"
        		+ "  `start_time` datetime NOT NULL COMMENT '起始时间',\r\n"
        		+ "  `end_time` datetime NOT NULL COMMENT '结束时间',\r\n"
        		+ "  `product_code` varchar(32) DEFAULT 'YN-MAUCASH' COMMENT '产品类型:MAUCASH/SPEKTRA/……',\r\n"
        		+ "  `create_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,\r\n"
        		+ "  `update_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,\r\n"
        		+ "  `create_manager` bigint(20) DEFAULT NULL,\r\n"
        		+ "  `update_manager` bigint(20) DEFAULT NULL,\r\n"
        		+ "  PRIMARY KEY (`id`)\r\n"
        		+ ");\r\n"
        		+ "\r\n"
        		+ "DROP TABLE IF EXISTS activitycode_use_record;\r\n"
        		+ "\r\n"
        		+ "CREATE TABLE `activitycode_use_record` (\r\n"
        		+ "  `id` bigint(20) NOT NULL AUTO_INCREMENT,\r\n"
        		+ "  `activity_code` varchar(64) NOT NULL COMMENT '活动code',\r\n"
        		+ "  `loan_number` varchar(32) DEFAULT NULL COMMENT '进件单号',\r\n"
        		+ "  `is_multiple` tinyint(1) DEFAULT '0' COMMENT '是否是多单,0不是，1是',\r\n"
        		+ "  `create_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,\r\n"
        		+ "  `activity_rule_id` int(11) DEFAULT NULL COMMENT '活动规则id',\r\n"
        		+ "  `rule_data` varchar(1024) DEFAULT NULL COMMENT '优惠数据',\r\n"
        		+ "  `user_id` bigint(20) DEFAULT NULL COMMENT '用户id',\r\n"
        		+ "  `updated_at` datetime DEFAULT NULL,\r\n"
        		+ "  `deleted` bit(1) DEFAULT '0' COMMENT '软删除',\r\n"
        		+ "  `type` int(4) DEFAULT NULL COMMENT '1-放款 2-还款',\r\n"
        		+ "  `status` int(4) DEFAULT NULL COMMENT '1-使用中 2-使用成功',\r\n"
        		+ "  PRIMARY KEY (`id`),\r\n"
        		+ "  KEY `idx_loan_number` (`loan_number`),\r\n"
        		+ "  KEY `idx_activity_code` (`activity_code`),\r\n"
        		+ "  KEY `idx_user_id` (`user_id`)\r\n"
        		+ ") ENGINE=InnoDB AUTO_INCREMENT=2859 DEFAULT CHARSET=utf8 COMMENT='优惠码使用记录';\r\n"
        		+ "\r\n"
        		+ "CREATE TABLE `backstage_configs` (\r\n"
        		+ "  `id` bigint(20) NOT NULL AUTO_INCREMENT,\r\n"
        		+ "  `config_key` varchar(64) NOT NULL,\r\n"
        		+ "  `config_value` text,\r\n"
        		+ "  `remark` varchar(64) DEFAULT NULL,\r\n"
        		+ "  `operator` bigint(20) DEFAULT NULL COMMENT '操作员id',\r\n"
        		+ "  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,\r\n"
        		+ "  `updated_at` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,\r\n"
        		+ "  PRIMARY KEY (`id`),\r\n"
        		+ "  UNIQUE KEY `uqk_config_key` (`config_key`) USING BTREE\r\n"
        		+ ") ;";


        System.out.println(transformCreateTable(content));
    }
}
