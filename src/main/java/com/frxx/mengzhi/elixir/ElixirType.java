package com.frxx.mengzhi.elixir;

public enum ElixirType {
    SHIELD_MAX("shield_max", "\u62a4\u76d6\u4e0a\u9650", "\u6c38\u4e45\u63d0\u5347\u62a4\u76d6\u6700\u5927\u503c"),
    SHIELD_REGEN("shield_regen", "\u62a4\u76d6\u56de\u590d", "\u6c38\u4e45\u63d0\u5347\u62a4\u76d6\u6bcf\u79d2\u56de\u590d"),
    SHIELD_ABSORPTION("shield_absorption", "\u4f24\u5bb3\u5438\u6536\u6bd4", "\u6c38\u4e45\u63d0\u5347\u62a4\u76d6\u5438\u6536\u6bd4\uff081\u70b9\u62a4\u76d6\u5bf9\u5e94\u591f\u5c11\u70b9\u4f24\u5bb3\uff09");

    private final String id;
    private final String displayName;
    private final String description;

    ElixirType(String id, String displayName, String description) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}