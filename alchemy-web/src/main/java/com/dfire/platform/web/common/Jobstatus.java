package com.dfire.platform.web.common;

/**
 * @author congbai
 * @date 2018/6/8
 */
public enum Jobstatus {
    /*
    状态，0是待完善 1是待审核 2是审核通过  3是审核失败 4是已提交 5是运行 6是失败 7是取消 8是完成
     */
    UN_FIX(0), UN_AUDIT(1), AUDIT(2), AUDIT_FAIL(3), COMMIT(4), RUNNING(5), FAILED(6), CANCELED(7), FINISHED(8);
    private int status;

    Jobstatus(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
