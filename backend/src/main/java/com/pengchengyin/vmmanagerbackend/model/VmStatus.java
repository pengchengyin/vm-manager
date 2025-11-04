package com.pengchengyin.vmmanagerbackend.model;

/**
 * 虚拟机状态枚举
 */
public enum VmStatus {
    /**
     * 未定义状态
     */
    NOSTATE(0, "未定义"),
    /**
     * 运行中
     */
    RUNNING(1, "运行中"),
    /**
     * 阻塞中（等待IO）
     */
    BLOCKED(2, "阻塞中"),
    /**
     * 暂停中
     */
    PAUSED(3, "暂停中"),
    /**
     * 关闭中
     */
    SHUTDOWN(4, "关闭中"),
    /**
     * 已关闭
     */
    SHUTOFF(5, "已关闭"),
    /**
     * 崩溃
     */
    CRASHED(6, "崩溃"),
    /**
     * 暂停（由于迁移）
     */
    PMSUSPENDED(7, "暂停（迁移中）");

    private final int code;
    private final String description;

    VmStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static VmStatus fromCode(int code) {
        for (VmStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return NOSTATE;
    }
}



