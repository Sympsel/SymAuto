package com.betterbundle.sort.plan;

/** 规划结果：成功携带 SortPlan，失败携带中文原因。 */
public final class PlanResult {

    private final SortPlan plan;
    private final String error;

    private PlanResult(SortPlan plan, String error) {
        this.plan = plan;
        this.error = error;
    }

    public static PlanResult ok(SortPlan plan) {
        return new PlanResult(plan, null);
    }

    public static PlanResult fail(String error) {
        return new PlanResult(null, error);
    }

    public boolean isOk() {
        return plan != null;
    }

    public SortPlan plan() {
        return plan;
    }

    public String error() {
        return error;
    }
}
