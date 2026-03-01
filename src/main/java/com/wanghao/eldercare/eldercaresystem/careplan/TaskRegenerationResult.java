package com.wanghao.eldercare.eldercaresystem.careplan;

class TaskRegenerationResult {
    private final int deletedCount;
    private final int generatedCount;

    TaskRegenerationResult(int deletedCount, int generatedCount) {
        this.deletedCount = deletedCount;
        this.generatedCount = generatedCount;
    }

    int getDeletedCount() {
        return deletedCount;
    }

    int getGeneratedCount() {
        return generatedCount;
    }
}
