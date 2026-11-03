package com.example.studio.task;

/** 处理阶段，声明顺序即执行顺序。 */
public enum TaskStage {
    MATERIAL_CHECK("素材检查", "素材检查未通过：图片质量不符合要求（模拟失败）"),
    GENERATION("模拟生成", "生成服务暂时不可用（模拟失败）"),
    RESULT_ARRANGE("结果整理", "结果保存失败（模拟失败）");

    private final String label;
    private final String simulatedFailureMessage;

    TaskStage(String label, String simulatedFailureMessage) {
        this.label = label;
        this.simulatedFailureMessage = simulatedFailureMessage;
    }

    public String label() {
        return label;
    }

    public String simulatedFailureMessage() {
        return simulatedFailureMessage;
    }
}
