package com.dfire.platform.alchemy.web.rest.vm;

import javax.validation.constraints.NotNull;

import com.dfire.platform.alchemy.web.common.Content;

/**
 * @author congbai
 * @date 2018/6/8
 */
public class JobConfVM {

    private Long id;

    @NotNull
    private Long acJobId;

    @NotNull
    private Content content;

    @NotNull
    private int type;

    @NotNull
    private boolean isAudit;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAcJobId() {
        return acJobId;
    }

    public void setAcJobId(Long acJobId) {
        this.acJobId = acJobId;
    }

    public Content getContent() {
        return content;
    }

    public void setContent(Content content) {
        this.content = content;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public boolean isAudit() {
        return isAudit;
    }

    public void setAudit(boolean audit) {
        isAudit = audit;
    }

    @Override
    public String toString() {
        return "JobConfVM{" + "id=" + id + ", acJobId=" + acJobId + ", content='" + content + '\'' + ", type=" + type
            + ", isAudit=" + isAudit + '}';
    }
}