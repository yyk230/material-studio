import type { StepStatus, TaskDto } from "@/lib/api/types";
import { STAGE_LABELS, STAGES } from "@/lib/labels";
import styles from "./tasks.module.css";

type StageView = StepStatus | "NOT_STARTED" | "SKIPPED";

const VIEW_TEXT: Record<StageView, string> = {
  RUNNING: "进行中",
  SUCCEEDED: "完成",
  FAILED: "失败",
  NOT_STARTED: "未开始",
  SKIPPED: "未执行",
};

/** 三个阶段的执行情况，全部来自服务端 steps 记录。 */
export function StageProgress({ task }: { task: TaskDto }) {
  const failedIndex = STAGES.findIndex((stage) => task.steps.some((s) => s.stage === stage && s.status === "FAILED"));
  const terminalFailed = task.status === "FAILED";

  return (
    <ol className={styles.stages} aria-label="处理阶段">
      {STAGES.map((stage, index) => {
        const step = task.steps.find((s) => s.stage === stage);
        let view: StageView = step ? step.status : "NOT_STARTED";
        if (!step && terminalFailed && (failedIndex === -1 || index > failedIndex)) view = "SKIPPED";
        return (
          <li key={stage} className={`${styles.stage} ${styles[`stage_${view}`] ?? ""}`}>
            <span className={styles.stageBar} aria-hidden="true" />
            <span className={styles.stageName}>{STAGE_LABELS[stage]}</span>
            <span className={styles.stageState}>{VIEW_TEXT[view]}</span>
          </li>
        );
      })}
    </ol>
  );
}
