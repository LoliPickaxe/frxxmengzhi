package com.frxx.mengzhi.elixir;

public enum ElixirRealm {
    QI_REFINING(1, "lianqi", "\u7ec3\u6c14\u671f"),
    FOUNDATION(2, "zhiji", "\u7b51\u57fa\u671f"),
    GOLDEN_CORE(3, "jiedan", "\u7ed3\u4e39\u671f"),
    NASCENT_SOUL(4, "yuanying", "\u5143\u5a74\u671f"),
    SPIRIT_TRANSFORMATION(5, "huashen", "\u5316\u795e\u671f");

    private final int level;
    private final String shortName;
    private final String fullName;

    ElixirRealm(int level, String shortName, String fullName) {
        this.level = level;
        this.shortName = shortName;
        this.fullName = fullName;
    }

    public int getLevel() { return level; }
    public String getShortName() { return shortName; }
    public String getFullName() { return fullName; }

    public static ElixirRealm fromLevel(int level) {
        for (ElixirRealm realm : values()) {
            if (realm.level == level) return realm;
        }
        return QI_REFINING;
    }

    public static ElixirRealm fromJingJie(double jingJie) {
        int lvl = (int) Math.floor(jingJie);
        return fromLevel(Math.max(1, Math.min(5, lvl)));
    }
}