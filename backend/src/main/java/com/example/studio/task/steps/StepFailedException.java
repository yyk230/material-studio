package com.example.studio.task.steps;

/** 步骤业务失败，message 直接作为面向用户的失败原因。 */
public class StepFailedException extends Exception {

    public StepFailedException(String message) {
        super(message);
    }
}
