package com.asset.registry.common;

/**
 * 运行状态与状态流转规则（对应 SRS 表 3-5）。
 * active 启用 / stopped 停用 / scrapped 报废
 */
public enum RunStatus {

    ACTIVE("active", "启用"),
    STOPPED("stopped", "停用"),
    SCRAPPED("scrapped", "报废");

    private final String code;
    private final String label;

    RunStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static RunStatus of(String code) {
        for (RunStatus s : values()) {
            if (s.code.equalsIgnoreCase(code)) {
                return s;
            }
        }
        throw BizException.badRequest("非法的运行状态：" + code);
    }

    /** 是否允许从当前状态流转到目标状态 */
    public boolean canTransitTo(RunStatus target) {
        if (target == this) {
            return false;
        }
        return switch (this) {
            case ACTIVE -> target == STOPPED || target == SCRAPPED;
            case STOPPED -> target == ACTIVE || target == SCRAPPED;
            case SCRAPPED -> target == ACTIVE;
        };
    }
}
