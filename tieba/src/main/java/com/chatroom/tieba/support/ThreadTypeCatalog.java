package com.chatroom.tieba.support;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ThreadTypeCatalog {

    private static final Map<String, ThreadTypeOption> OPTIONS = new LinkedHashMap<>();

    static {
        register("DISCUSSION", "讨论", "适合观点交流、经验分享和日常讨论");
        register("HELP", "求助", "适合提问、排障、寻求建议");
        register("ACTIVITY", "活动", "适合发布校园活动、社团安排和聚会信息");
        register("RECRUIT", "招募", "适合组队、招新、项目或比赛招募");
        register("MARKET", "闲置", "适合二手转让、互助交换和拼单");
    }

    private ThreadTypeCatalog() {
    }

    public static List<ThreadTypeOption> options() {
        return List.copyOf(OPTIONS.values());
    }

    public static boolean isSupported(String code) {
        if (code == null) {
            return false;
        }
        return OPTIONS.containsKey(code.trim().toUpperCase(Locale.ROOT));
    }

    public static String normalize(String code) {
        if (code == null || code.isBlank()) {
            return "DISCUSSION";
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }

    public static String labelOf(String code) {
        ThreadTypeOption option = OPTIONS.get(normalize(code));
        return option == null ? "讨论" : option.getLabel();
    }

    private static void register(String code, String label, String description) {
        OPTIONS.put(code, new ThreadTypeOption(code, label, description));
    }

    public static final class ThreadTypeOption {
        private final String code;
        private final String label;
        private final String description;

        public ThreadTypeOption(String code, String label, String description) {
            this.code = code;
            this.label = label;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getLabel() {
            return label;
        }

        public String getDescription() {
            return description;
        }
    }
}
